import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Preview of the radar HUD (radar-hud-spec.md) from a built pack: builds a title the way the plugin
 * does, lays it out with build/assets/kitpvp/font/radar.json like the client (bitmap advance = rightmost
 * opaque column + 2, glyph top = line y + 7 - ascent), applies text.vsh's move and draws it on a fake
 * 320x180 GUI screen, scaled up. Also writes the background map scaled up.
 *
 * usage: java tools/RadarMock.java <build dir> <out dir>
 */
public class RadarMock {

    record Glyph(BufferedImage img, int ascent, int advance) {}

    static final Map<Character, Glyph> glyphs = new HashMap<>();
    static final Map<Character, Integer> spaces = new HashMap<>();

    public static void main(String[] a) throws Exception {
        Path build = Path.of(a[0]), out = Path.of(a[1]);
        Files.createDirectories(out);
        String json = Files.readString(build.resolve("assets/kitpvp/font/radar.json"));
        Matcher m = Pattern.compile("\"file\": \"kitpvp:([^\"]+)\", \"ascent\": (-?\\d+), \"height\": (\\d+), \"chars\": \\[\"\\\\u([0-9a-f]{4})\"\\]").matcher(json);
        Map<String, BufferedImage> cache = new HashMap<>();
        while (m.find()) {
            BufferedImage img = cache.computeIfAbsent(m.group(1), f -> read(build.resolve("assets/kitpvp/textures/" + f).toFile()));
            if (img.getHeight() != Integer.parseInt(m.group(3))) throw new IllegalStateException("height " + m.group(1));
            int right = 0;
            for (int x = 0; x < img.getWidth(); x++) for (int y = 0; y < img.getHeight(); y++) if ((img.getRGB(x, y) >>> 24) > 0) right = x + 1;
            glyphs.put((char) Integer.parseInt(m.group(4), 16), new Glyph(img, Integer.parseInt(m.group(2)), right + 1));
        }
        Matcher s = Pattern.compile("\"\\\\u(f8[0-9a-f]{2})\": (-?\\d+)").matcher(json);
        while (s.find()) spaces.put((char) Integer.parseInt(s.group(1), 16), Integer.parseInt(s.group(2)));
        System.out.println(glyphs.size() + " glyphs, " + spaces.size() + " spaces");

        // a sample round: the border drifted off centre and closed to 520 blocks, four enemies (one past
        // the map's edge, clamped), a drop, you
        StringBuilder t = new StringBuilder().append('\ue100');
        double bx = 560, bz = 560, size = 520;
        int c0 = col(bx - size / 2), c1 = col(bx + size / 2), r0 = row(bz - size / 2), r1 = row(bz + size / 2);
        element(t, 0xE500, clamp(c0, 0, 84), clamp(r0, 0, 84), 5);
        element(t, 0xE600, clamp(c1 - 3, 0, 84), clamp(r0, 0, 84), 5);
        element(t, 0xE700, clamp(c0, 0, 84), clamp(r1 - 3, 0, 84), 5);
        element(t, 0xE800, clamp(c1 - 3, 0, 84), clamp(r1 - 3, 0, 84), 5);
        marker(t, 0xE400, 610, 480, 5);
        for (double[] e : new double[][]{{400, 700}, {455, 520}, {720, 760}, {950, 300}}) marker(t, 0xE200, e[0], e[1], 3);
        marker(t, 0xE300, 520, 600, 5);

        // lay out like the client: the boss bar title at (guiWidth/2 - width/2, 12 - 9)
        int gw = 320, gh = 180, k = 4;
        int width = 0;
        for (char ch : t.toString().toCharArray()) width += advance(ch);
        System.out.println("title width " + width + " (spec: 89), chars " + t.length());
        int tx = gw / 2 - width / 2, ty = 12 - 9;
        double moveX = gw / 2.0 - 4.0 - 89.0 / 2.0 - 1.0, moveY = -15.0; // text.vsh, guiW = 2 / ProjMat[0][0]
        BufferedImage screen = new BufferedImage(gw * k, gh * k, BufferedImage.TYPE_INT_RGB);
        for (int y = 0; y < gh * k; y++) for (int x = 0; x < gw * k; x++)
            screen.setRGB(x, y, ((x / k / 16 + y / k / 16) & 1) == 0 ? 0x5B7A3C : 0x6B8A4A); // grass-ish backdrop
        double pen = tx;
        for (char ch : t.toString().toCharArray()) {
            Glyph g = glyphs.get(ch);
            if (g != null) {
                double gx = pen + moveX, gy = ty + 7 - g.ascent + moveY;
                for (int y = 0; y < g.img.getHeight(); y++) for (int x = 0; x < g.img.getWidth(); x++) {
                    int argb = g.img.getRGB(x, y);
                    if ((argb >>> 24) == 0) continue;
                    int sx = (int) Math.round((gx + x) * k), sy = (int) Math.round((gy + y) * k);
                    for (int i = 0; i < k; i++) for (int j = 0; j < k; j++) {
                        int px = sx + i, py = sy + j;
                        if (px >= 0 && py >= 0 && px < gw * k && py < gh * k) screen.setRGB(px, py, argb & 0xFFFFFF);
                    }
                }
            }
            pen += advance(ch);
        }
        double left = tx + moveX;
        System.out.printf("HUD left %.1f right %.1f (screen %d, gap %.1f), top %.1f%n", left, left + 89, gw, gw - left - 89, ty + 7 + 9 + moveY);
        ImageIO.write(screen, "png", out.resolve("hud.png").toFile());

        BufferedImage bg = glyphs.get('\ue100').img, big = new BufferedImage(88 * 6, 88 * 6, BufferedImage.TYPE_INT_ARGB);
        for (int y = 0; y < 88 * 6; y++) for (int x = 0; x < 88 * 6; x++) big.setRGB(x, y, bg.getRGB(x / 6, y / 6));
        ImageIO.write(big, "png", out.resolve("bg.png").toFile());
        int transparent = 0;
        for (int y = 0; y < 88; y++) for (int x = 0; x < 88; x++) if ((bg.getRGB(x, y) >>> 24) != 255) transparent++;
        System.out.println("bg pixels not fully opaque: " + transparent);
    }

    static BufferedImage read(File f) {
        try { return ImageIO.read(f); } catch (Exception e) { throw new RuntimeException(e); }
    }

    static int advance(char ch) {
        Glyph g = glyphs.get(ch);
        if (g != null) return g.advance;
        Integer s = spaces.get(ch);
        if (s == null) throw new IllegalStateException("no glyph " + Integer.toHexString(ch));
        return s;
    }

    static int col(double x) { return clamp((int) Math.floor((x - 140) / 760 * 88), 0, 87); }
    static int row(double z) { return clamp((int) Math.floor((z - 220) / 760 * 88), 0, 87); }
    static int clamp(int v, int lo, int hi) { return Math.max(lo, Math.min(hi, v)); }

    /** enemy (3 wide) / self, drop (5 wide) centred on the world position */
    static void marker(StringBuilder t, int base, double x, double z, int w) {
        int c = col(x), r = row(z), half = w / 2;
        int left = clamp(c - half, 0, 88 - w);
        r = clamp(r, half, 87 - half); // keep the image inside the map vertically too
        element(t, base + r, left, -1, w + 1);
    }

    /** move (x - 89), the glyph, move (89 - x - a): net advance 0 */
    static void element(StringBuilder t, int ch, int x, int top, int advance) {
        if (top >= 0) ch += top;
        move(t, x - 89);
        t.append((char) ch);
        move(t, 89 - x - advance);
    }

    static void move(StringBuilder t, int d) {
        int base = d < 0 ? 0xF801 : 0xF811, v = Math.abs(d);
        for (int b = 0; b < 8; b++) if ((v >> b & 1) != 0) t.append((char) (base + b));
    }
}
