import javax.imageio.ImageIO;
import java.awt.Color;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Builds the KitPvP resource pack's art: a dark KitPvP restyle of the 6-row chest (all KitPvP menus
 * use it) and full-menu overlays drawn through a font glyph in the menu title (kit picker, kit
 * preview/editor, leaderboards). Text is drawn with the vanilla font sheet so it looks like the game.
 *
 * usage: java Gen.java <ascii.png from the vanilla client> <out dir>
 *
 * Panel geometry (generic_54): 176x222, menu slots from (7,17) 18 px apart, inventory label at y 128,
 * player inventory from y 139, hotbar at y 197. Overlays are 256x256 with the panel at (0,0).
 */
public class Gen {

    static BufferedImage font;
    static int[] widths = new int[256];

    static final Color BG = new Color(0x1A1824);
    static final Color INNER = new Color(0x221F30);
    static final Color SLOT_FILL = new Color(0x2C2940);
    static final Color SLOT_DARK = new Color(0x0F0E16);
    static final Color SLOT_LIGHT = new Color(0x4A4466);
    static final Color PINK = new Color(0xFF2E63);
    static final Color PURPLE = new Color(0x8A4BFF);
    static final Color GOLD = new Color(0xFFD900);
    static final Color TEXT = new Color(0xECE8F5);
    static final Color MUTED = new Color(0x9C95B8);

    public static void main(String[] a) throws IOException {
        font = ImageIO.read(new File(a[0]));
        for (int c = 0; c < 256; c++) {
            int cx = (c % 16) * 8, cy = (c / 16) * 8, w = 0;
            for (int x = 0; x < 8; x++) for (int y = 0; y < 8; y++) if ((font.getRGB(cx + x, cy + y) >>> 24) > 0) w = Math.max(w, x + 1);
            widths[c] = c == ' ' ? 3 : w;
        }
        Path out = Path.of(a[1]);
        save(base(), out.resolve("assets/minecraft/textures/gui/container/generic_54.png"));
        save(kits(), out.resolve("assets/kitpvp/textures/gui/kits.png"));
        save(loadout(), out.resolve("assets/kitpvp/textures/gui/loadout.png"));
        save(boards(), out.resolve("assets/kitpvp/textures/gui/boards.png"));
        save(logo(), out.resolve("pack.png"));
        write(out.resolve("assets/kitpvp/font/gui.json"), """
                {
                  "providers": [
                    {"type": "bitmap", "file": "kitpvp:gui/kits.png", "ascent": 13, "height": 256, "chars": ["\\ue000"]},
                    {"type": "bitmap", "file": "kitpvp:gui/loadout.png", "ascent": 13, "height": 256, "chars": ["\\ue001"]},
                    {"type": "bitmap", "file": "kitpvp:gui/boards.png", "ascent": 13, "height": 256, "chars": ["\\ue002"]},
                    {"type": "space", "advances": {"\\uf000": -8, "\\uf001": -249}}
                  ]
                }
                """);
        // the panel draws its own INVENTORY label; vanilla's dark grey one would vanish on it
        String lang = "{\n  \"container.inventory\": \"\"\n}\n";
        for (String l : new String[]{"en_us", "en_gb", "en_ca", "en_au", "en_nz"}) write(out.resolve("assets/minecraft/lang/" + l + ".json"), lang);
        write(out.resolve("pack.mcmeta"), """
                {
                  "pack": {
                    "description": "§dUnstableMC §fKitPvP",
                    "min_format": [97, 0],
                    "max_format": [120, 0]
                  }
                }
                """);
    }

    // ------------------------------------------------------------------ drawing helpers

    static void save(BufferedImage img, Path p) throws IOException {
        Files.createDirectories(p.getParent());
        ImageIO.write(img, "png", p.toFile());
    }

    static void write(Path p, String s) throws IOException {
        Files.createDirectories(p.getParent());
        Files.writeString(p, s);
    }

    static BufferedImage canvas() {
        return new BufferedImage(256, 256, BufferedImage.TYPE_INT_ARGB);
    }

    static void fill(BufferedImage g, int x, int y, int w, int h, Color c) {
        for (int i = x; i < x + w; i++) for (int j = y; j < y + h; j++) g.setRGB(i, j, c.getRGB());
    }

    static Color mix(Color a, Color b, double t) {
        t = Math.max(0, Math.min(1, t));
        return new Color((int) (a.getRed() + (b.getRed() - a.getRed()) * t), (int) (a.getGreen() + (b.getGreen() - a.getGreen()) * t),
                (int) (a.getBlue() + (b.getBlue() - a.getBlue()) * t), (int) (a.getAlpha() + (b.getAlpha() - a.getAlpha()) * t));
    }

    static Color alpha(Color c, int a) {
        return new Color(c.getRed(), c.getGreen(), c.getBlue(), a);
    }

    static void blend(BufferedImage g, int x, int y, Color c) {
        if (x < 0 || y < 0 || x >= g.getWidth() || y >= g.getHeight()) return;
        Color d = new Color(g.getRGB(x, y), true);
        double a = c.getAlpha() / 255.0, da = d.getAlpha() / 255.0;
        double oa = a + da * (1 - a);
        if (oa <= 0) return;
        int r = (int) ((c.getRed() * a + d.getRed() * da * (1 - a)) / oa);
        int gg = (int) ((c.getGreen() * a + d.getGreen() * da * (1 - a)) / oa);
        int b = (int) ((c.getBlue() * a + d.getBlue() * da * (1 - a)) / oa);
        g.setRGB(x, y, new Color(r, gg, b, (int) (oa * 255)).getRGB());
    }

    /** Horizontal gradient bar. */
    static void gradient(BufferedImage g, int x, int y, int w, int h, Color a, Color b) {
        for (int i = 0; i < w; i++) {
            Color c = mix(a, b, w == 1 ? 0 : i / (double) (w - 1));
            for (int j = 0; j < h; j++) g.setRGB(x + i, y + j, c.getRGB());
        }
    }

    static int textWidth(String s, int scale) {
        int w = 0;
        for (char c : s.toCharArray()) w += (widths[c & 0xFF] + 1) * scale;
        return w - scale;
    }

    /** Vanilla-font text with a drop shadow; colour per character from a gradient when b != null. */
    static void text(BufferedImage g, int x, int y, String s, int scale, Color a, Color b, boolean shadow) {
        int total = Math.max(1, s.length() - 1), cx = x;
        for (int k = 0; k < s.length(); k++) {
            int c = s.charAt(k) & 0xFF;
            Color col = b == null ? a : mix(a, b, k / (double) total);
            Color sh = new Color(col.getRed() / 4, col.getGreen() / 4, col.getBlue() / 4);
            int fx = (c % 16) * 8, fy = (c / 16) * 8;
            for (int px = 0; px < 8; px++) for (int py = 0; py < 8; py++) {
                if ((font.getRGB(fx + px, fy + py) >>> 24) == 0) continue;
                for (int sx = 0; sx < scale; sx++) for (int sy = 0; sy < scale; sy++) {
                    if (shadow) blend(g, cx + px * scale + sx + scale, y + py * scale + sy + scale, sh);
                    blend(g, cx + px * scale + sx, y + py * scale + sy, col);
                }
            }
            cx += (widths[c] + 1) * scale;
        }
    }

    static void centered(BufferedImage g, int cx, int y, String s, int scale, Color a, Color b) {
        text(g, cx - textWidth(s, scale) / 2, y, s, scale, a, b, true);
    }

    /** One slot frame at slot-origin (x, y): 18x18, dark top-left, light bottom-right. */
    static void slot(BufferedImage g, int x, int y) {
        fill(g, x, y, 18, 18, SLOT_FILL);
        fill(g, x, y, 17, 1, SLOT_DARK);
        fill(g, x, y, 1, 17, SLOT_DARK);
        fill(g, x + 1, y + 17, 17, 1, SLOT_LIGHT);
        fill(g, x + 17, y + 1, 1, 17, SLOT_LIGHT);
    }

    /** A rounded panel with a glowing pink->purple frame. */
    static void panel(BufferedImage g, int x, int y, int w, int h, Color fillC) {
        for (int i = 0; i < w; i++) for (int j = 0; j < h; j++) {
            boolean corner = (i < 2 || i >= w - 2) && (j < 2 || j >= h - 2) && !((i == 1 || i == w - 2) && (j == 1 || j == h - 2));
            if (corner) continue;
            boolean edge = i == 0 || j == 0 || i == w - 1 || j == h - 1;
            boolean edge2 = i == 1 || j == 1 || i == w - 2 || j == h - 2;
            double t = (i + j) / (double) (w + h);
            if (edge) g.setRGB(x + i, y + j, mix(PINK, PURPLE, t).getRGB());
            else if (edge2) g.setRGB(x + i, y + j, mix(mix(PINK, PURPLE, t), fillC, 0.55).getRGB());
            else g.setRGB(x + i, y + j, fillC.getRGB());
        }
    }

    // ------------------------------------------------------------------ the textures

    /** The 6-row chest for every KitPvP menu: dark panel, glowing frame, dark slots. */
    static BufferedImage base() {
        BufferedImage g = canvas();
        panel(g, 0, 0, 176, 222, BG);
        fill(g, 4, 4, 168, 123, INNER);
        for (int r = 0; r < 6; r++) for (int c = 0; c < 9; c++) slot(g, 7 + c * 18, 17 + r * 18);
        // divider with the INVENTORY label
        gradient(g, 7, 128, 162, 1, alpha(PINK, 160), alpha(PURPLE, 160));
        text(g, 8, 130, "INVENTORY", 1, MUTED, null, false);
        for (int r = 0; r < 3; r++) for (int c = 0; c < 9; c++) slot(g, 7 + c * 18, 139 + r * 18);
        for (int c = 0; c < 9; c++) slot(g, 7 + c * 18, 197);
        return g;
    }

    /** A banner covering the title strip and menu row 0 (y 4..34). */
    static void header(BufferedImage g, String title, String sub) {
        panel(g, 4, 4, 168, 31, new Color(0x151320));
        for (int j = 6; j < 33; j++) for (int i = 6; i < 170; i++) {
            double glow = 1 - Math.abs(i - 88) / 82.0;
            blend(g, i, j, alpha(mix(PINK, PURPLE, (i - 6) / 164.0), (int) (38 * glow)));
        }
        centered(g, 88, 8, title, 2, PINK, PURPLE);
        if (sub != null) centered(g, 88, 25, sub, 1, MUTED, null);
    }

    /** A role banner over three slots (54x18) at slot-origin (x, y), arrow-cut at the right end. */
    static void roleBanner(BufferedImage g, int x, int y, String label, Color c) {
        int w = 54;
        for (int i = 0; i < w; i++) for (int j = 0; j < 18; j++) {
            boolean tip = i >= w - 8 && Math.abs(j - 8.5) > (w - i) * 1.1;
            if (tip) continue;
            boolean edge = i == 0 || j == 0 || j == 17 || (i >= w - 8 && Math.abs(j - 8.5) > (w - i - 1) * 1.1);
            Color f = edge ? c : mix(new Color(0x14121C), c, 0.22 + 0.3 * (1 - i / (double) w));
            g.setRGB(x + i, y + j, f.getRGB());
        }
        text(g, x + 4, y + 5, label, 1, TEXT, null, true);
    }

    /** Kit picker: logo header, a banner per role row, glow ring on Random, hint strip. */
    static BufferedImage kits() {
        BufferedImage g = canvas();
        header(g, "KITPVP", "CHOOSE YOUR KIT");
        roleBanner(g, 7, 35, "MELEE", new Color(0xFF4040));
        roleBanner(g, 7, 53, "RANGED", new Color(0x4FC3FF));
        roleBanner(g, 7, 71, "TRICKS", new Color(0x7DFF6A));
        // row 4: divider with a ring for Random (slot 40 = column 4)
        gradient(g, 9, 97, 64, 1, alpha(PURPLE, 0), alpha(GOLD, 180));
        gradient(g, 103, 97, 64, 1, alpha(GOLD, 180), alpha(PURPLE, 0));
        ring(g, 7 + 4 * 18, 89, GOLD);
        // row 5: hint strip
        fill(g, 7, 107, 162, 18, new Color(0x151320));
        gradient(g, 7, 107, 162, 1, alpha(PINK, 140), alpha(PURPLE, 140));
        centered(g, 88, 112, "LEFT: PICK    RIGHT: PREVIEW", 1, MUTED, null);
        // the other row-4 slots aren't used: blend them into the panel
        for (int c = 0; c < 9; c++) if (c != 4) {
            fill(g, 7 + c * 18, 89, 18, 18, INNER);
        }
        gradient(g, 9, 97, 64, 1, alpha(PURPLE, 0), alpha(GOLD, 180));
        gradient(g, 103, 97, 64, 1, alpha(GOLD, 180), alpha(PURPLE, 0));
        return g;
    }

    /** A 1px glow ring just inside a slot, item area stays clear. */
    static void ring(BufferedImage g, int x, int y, Color c) {
        for (int i = 0; i < 18; i++) {
            blend(g, x + i, y, c);
            blend(g, x + i, y + 17, c);
            blend(g, x, y + i, c);
            blend(g, x + 17, y + i, c);
            blend(g, x + i, y - 1, alpha(c, 90));
            blend(g, x + i, y + 18, alpha(c, 90));
            blend(g, x - 1, y + i, alpha(c, 90));
            blend(g, x + 18, y + i, alpha(c, 90));
        }
    }

    /** Kit preview and editor: inventory and hotbar on top, armour + offhand row below a label strip. */
    static BufferedImage loadout() {
        BufferedImage g = canvas();
        fill(g, 4, 4, 168, 12, INNER);
        // hotbar row (row 3) gets a tinted frame
        for (int c = 0; c < 9; c++) ring(g, 7 + c * 18, 71, alpha(PURPLE, 200));
        // row 4: label strip (no items there)
        fill(g, 7, 89, 162, 18, new Color(0x151320));
        gradient(g, 7, 89, 162, 1, alpha(PINK, 160), alpha(PURPLE, 160));
        gradient(g, 7, 106, 162, 1, alpha(PURPLE, 160), alpha(PINK, 160));
        text(g, 10, 94, "^ HOTBAR", 1, MUTED, null, true);
        String right = "ARMOUR + OFFHAND v";
        text(g, 166 - textWidth(right, 1), 94, right, 1, TEXT, null, true);
        // equipment slots (45..49) framed gold
        for (int c = 0; c < 5; c++) ring(g, 7 + c * 18, 107, alpha(GOLD, 200));
        return g;
    }

    /** Leaderboards: tabs on row 0, a TOP 10 header on row 1, podium rings on #1-#3. */
    static BufferedImage boards() {
        BufferedImage g = canvas();
        fill(g, 4, 4, 168, 12, INNER);
        fill(g, 7, 35, 162, 18, new Color(0x151320));
        gradient(g, 7, 35, 162, 1, alpha(PINK, 160), alpha(PURPLE, 160));
        centered(g, 88, 40, "TOP 10", 1, GOLD, null);
        ring(g, 7 + 2 * 18, 53, GOLD);
        ring(g, 7 + 3 * 18, 53, new Color(0xD7D7D7));
        ring(g, 7 + 4 * 18, 53, new Color(0xCD7F32));
        return g;
    }

    /** pack.png: the KITPVP logo. */
    static BufferedImage logo() {
        BufferedImage g = new BufferedImage(64, 64, BufferedImage.TYPE_INT_ARGB);
        panel(g, 0, 0, 64, 64, new Color(0x151320));
        centered(g, 32, 18, "KIT", 2, PINK, PURPLE);
        centered(g, 32, 36, "PVP", 2, PURPLE, PINK);
        return g;
    }
}
