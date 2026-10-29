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
 * player inventory from y 139, hotbar at y 197. Overlays are 176x222 glyphs aligned with the panel.
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
        save(crop(kits()), out.resolve("assets/kitpvp/textures/gui/kits.png"));
        save(crop(loadout()), out.resolve("assets/kitpvp/textures/gui/loadout.png"));
        save(crop(boards()), out.resolve("assets/kitpvp/textures/gui/boards.png"));
        save(logo(), out.resolve("pack.png"));
        save(strike(), out.resolve("assets/kitpvp/textures/gui/strike.png"));
        // the world border: exploit-glitch texture, and the vanilla 26.3 shader with the border's
        // blue/red tint taken out (its alpha, the fade with distance, stays) so the design keeps its colours
        save(forcefield(), out.resolve("assets/minecraft/textures/misc/forcefield.png"));
        write(out.resolve("assets/minecraft/shaders/core/world_border.fsh"), """
                #version 330
                #extension GL_ARB_separate_shader_objects : require

                #include <minecraft:dynamictransforms.glsl>
                #include <minecraft:oit.glsl>

                uniform sampler2D Sampler0;

                layout(location = 0) in vec2 texCoord0;

                #ifndef OIT_ALPHA_ONLY
                layout(location = 0) out vec4 fragColor;
                #endif

                vec4 calculateFinalColor(vec4 color) {
                    // KitPvP: keep the texture's own colours, only fade with the border's alpha
                    color.a *= ColorModulator.a;
                    #ifdef OIT_ACCUMULATE
                    color = sampleColorForAccumulation(color);
                    #endif
                    return color;
                }

                void main() {
                    vec4 color = texture(Sampler0, texCoord0);
                    if (color.a == 0.0) {
                        discard;
                    }

                    #ifdef OIT_ALPHA_ONLY
                    executeAlphaOnlyPhase(gl_FragCoord.z, color.a * ColorModulator.a);
                    #else
                    fragColor = calculateFinalColor(color);
                    #endif
                }
                """);
        // the radar (a held map): frame with compass letters, and its markers
        save(mapFrame(), out.resolve("assets/minecraft/textures/map/map_background.png"));
        save(arrowMarker(new Color(0xFFFFFF), PINK), out.resolve("assets/minecraft/textures/map/decorations/player.png"));
        save(arrowMarker(new Color(0xFF3B4E), new Color(0x2A0008)), out.resolve("assets/minecraft/textures/map/decorations/red_marker.png"));
        save(dropMarker(), out.resolve("assets/minecraft/textures/map/decorations/target_x.png"));
        // strike and supply-drop sounds: the .ogg files come from sounds.sh (src/sounds), build.sh copies them
        String[][] sfx = {{"strike.charge", "strike/charge", "Strike charging"}, {"strike.siren", "strike/siren", "Strike siren"},
                {"strike.impact", "strike/impact", "Strike impact"}, {"drop.incoming", "drop/incoming", "Supply drop incoming"},
                {"drop.land", "drop/land", "Supply drop lands"}, {"strike.wither", "strike/wither", "Wither skulls launch"},
                {"strike.mob", "strike/mob", "Monsters roar"}, {"drop.pickup", "drop/pickup", "Supply drop opened"},
                {"gadget.ready", "gadget/ready", "Gadget ready"}, {"gadget.use", "gadget/use", "Gadget used"},
                {"kill.confirm", "kill/confirm", "Kill confirmed"}, {"kill.streak", "kill/streak", "Kill streak"},
                {"kill.shutdown", "kill/shutdown", "Streak shut down"}, {"round.start", "round/start", "Round starts"},
                {"round.border", "round/border", "Border closing"}, {"round.sudden", "round/sudden", "Sudden death"},
                {"round.win", "round/win", "Round won"}, {"ui.tick", "ui/tick", "Countdown ticks"},
                {"ui.click", "ui/click", "Click"}, {"ui.confirm", "ui/confirm", "Confirmed"}};
        StringBuilder sj = new StringBuilder("{\n");
        for (int i = 0; i < sfx.length; i++) {
            // tiny sounds that play all the time are kept in memory so the first one doesn't stutter
            boolean preload = java.util.Set.of("ui.click", "ui.tick", "ui.confirm", "kill.confirm").contains(sfx[i][0]);
            sj.append("  \"").append(sfx[i][0]).append("\": {\"sounds\": [{\"name\": \"kitpvp:").append(sfx[i][1])
                    .append(preload ? "\", \"preload\": true" : "\"").append("}], \"subtitle\": \"").append(sfx[i][2]).append("\"}")
                    .append(i < sfx.length - 1 ? ",\n" : "\n");
        }
        write(out.resolve("assets/kitpvp/sounds.json"), sj.append("}\n").toString());
        write(out.resolve("assets/kitpvp/font/gui.json"), """
                {
                  "providers": [
                    {"type": "bitmap", "file": "kitpvp:gui/kits.png", "ascent": 13, "height": 222, "chars": ["\\ue000"]},
                    {"type": "bitmap", "file": "kitpvp:gui/loadout.png", "ascent": 13, "height": 222, "chars": ["\\ue001"]},
                    {"type": "bitmap", "file": "kitpvp:gui/boards.png", "ascent": 13, "height": 222, "chars": ["\\ue002"]},
                    {"type": "bitmap", "file": "kitpvp:gui/strike.png", "ascent": 16, "height": 24, "chars": ["\\ue003"]},
                    {"type": "space", "advances": {"\\uf000": -8, "\\uf001": -169}}
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

    /** Overlay glyphs are exactly the panel size: 176x222 fits the client's 256 px font pages. */
    static BufferedImage crop(BufferedImage g) {
        BufferedImage out = new BufferedImage(176, 222, BufferedImage.TYPE_INT_ARGB);
        for (int x = 0; x < 176; x++) for (int y = 0; y < 222; y++) out.setRGB(x, y, g.getRGB(x, y));
        return out;
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
        int w = 11 + textWidth("PICK", 1) + 14 + 11 + textWidth("PREVIEW", 1), x = 88 - w / 2;
        mouse(g, x, 111, true);
        text(g, x + 11, 112, "PICK", 1, MUTED, null, true);
        x += 11 + textWidth("PICK", 1) + 14;
        mouse(g, x, 111, false);
        text(g, x + 11, 112, "PREVIEW", 1, MUTED, null, true);
        // the other row-4 slots aren't used: blend them into the panel
        for (int c = 0; c < 9; c++) if (c != 4) {
            fill(g, 7 + c * 18, 89, 18, 18, INNER);
        }
        gradient(g, 9, 97, 64, 1, alpha(PURPLE, 0), alpha(GOLD, 180));
        gradient(g, 103, 97, 64, 1, alpha(GOLD, 180), alpha(PURPLE, 0));
        return g;
    }

    /** A 7x10 mouse at (x, y) with the left or right button lit, for click hints. */
    static void mouse(BufferedImage g, int x, int y, boolean left) {
        String[] art = {
                ".#####.",
                "#ll#rr#",
                "#ll#rr#",
                "#ll#rr#",
                "#######",
                "#.....#",
                "#.....#",
                "#.....#",
                "#.....#",
                ".#####.",
        };
        Color body = new Color(0x2C2940), lit = PINK;
        for (int j = 0; j < art.length; j++) for (int i = 0; i < 7; i++) {
            char c = art[j].charAt(i);
            Color col = c == '#' ? MUTED : c == 'l' ? (left ? lit : body) : c == 'r' ? (left ? body : lit) : body;
            if (c == '.' && (j == 0 || j == art.length - 1)) continue; // rounded corners
            blend(g, x + i + 1, y + j + 1, new Color(col.getRed() / 4, col.getGreen() / 4, col.getBlue() / 4));
            g.setRGB(x + i, y + j, col.getRGB());
        }
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

    /**
     * The INCOMING STRIKE overlay (title glyph ): a red targeting reticle with corner
     * brackets and a soft glow, 96x96, drawn at height 24 so the 4x title scale makes it large.
     */
    static BufferedImage strike() {
        int n = 96;
        BufferedImage g = new BufferedImage(n, n, BufferedImage.TYPE_INT_ARGB);
        double c = (n - 1) / 2.0;
        Color red = new Color(0xFF2E3A), hot = new Color(0xFFD0D0);
        for (int x = 0; x < n; x++) for (int y = 0; y < n; y++) {
            double dx = x - c, dy = y - c, d = Math.sqrt(dx * dx + dy * dy);
            // glow behind the ring
            double glow = Math.max(0, 1 - Math.abs(d - 30) / 9);
            if (glow > 0) blend(g, x, y, alpha(red, (int) (70 * glow * glow)));
            // the ring, broken at the four ticks
            boolean gap = Math.abs(dx) < 4 || Math.abs(dy) < 4;
            if (Math.abs(d - 30) < 1.6 && !gap) blend(g, x, y, mix(red, hot, 0.25));
            // inner ring
            if (Math.abs(d - 12) < 1.0) blend(g, x, y, alpha(red, 200));
            // ticks pointing in
            boolean tick = (Math.abs(dx) < 1.2 && Math.abs(dy) > 16 && Math.abs(dy) < 40)
                    || (Math.abs(dy) < 1.2 && Math.abs(dx) > 16 && Math.abs(dx) < 40);
            if (tick) blend(g, x, y, red);
            // centre diamond
            if (Math.abs(dx) + Math.abs(dy) < 3.5) blend(g, x, y, hot);
        }
        // corner brackets
        int a = 6, len = 14;
        int[][] corners = {{a, a, 1, 1}, {n - 1 - a, a, -1, 1}, {a, n - 1 - a, 1, -1}, {n - 1 - a, n - 1 - a, -1, -1}};
        for (int[] k : corners) for (int i = 0; i < len; i++) for (int w = 0; w < 2; w++) {
            blend(g, k[0] + i * k[2], k[1] + w * k[3], red);
            blend(g, k[0] + w * k[2], k[1] + i * k[3], red);
        }
        return g;
    }

    /**
     * The held-map background, which shows as the border around the radar. The client draws it from
     * -7 to 135 around the 128 px map, so at 284 px the border is 14 px: room for N, E, S, W.
     */
    static BufferedImage mapFrame() {
        int n = 284, b = 14;
        BufferedImage g = new BufferedImage(n, n, BufferedImage.TYPE_INT_ARGB);
        panel(g, 0, 0, n, n, new Color(0x151320));
        // inner bevel where the map sits
        for (int i = b - 2; i < n - b + 2; i++) {
            for (int w = 0; w < 2; w++) {
                g.setRGB(i, b - 2 + w, SLOT_DARK.getRGB());
                g.setRGB(b - 2 + w, i, SLOT_DARK.getRGB());
                g.setRGB(i, n - b + w, SLOT_LIGHT.getRGB());
                g.setRGB(n - b + w, i, SLOT_LIGHT.getRGB());
            }
        }
        // ticks every 1/8 along each edge
        for (int k = 1; k < 8; k++) {
            if (k == 4) continue; // the letters sit there
            int p = b + (n - 2 * b) * k / 8;
            for (int t = 0; t < 4; t++) {
                blend(g, p, b - 6 + t, alpha(MUTED, 160));
                blend(g, p, n - b + 2 + t, alpha(MUTED, 160));
                blend(g, b - 6 + t, p, alpha(MUTED, 160));
                blend(g, n - b + 2 + t, p, alpha(MUTED, 160));
            }
        }
        // compass letters in the border (vanilla font, 8 px)
        centered(g, n / 2, 3, "N", 1, PINK, null);
        centered(g, n / 2, n - 11, "S", 1, TEXT, null);
        centered(g, 7, n / 2 - 4, "W", 1, TEXT, null);
        centered(g, n - 7, n / 2 - 4, "E", 1, TEXT, null);
        return g;
    }

    /** A 16x16 map arrow (a chevron) pointing up; the map flips it and turns it with the player. */
    static BufferedImage arrowMarker(Color fill, Color edge) {
        BufferedImage g = new BufferedImage(16, 16, BufferedImage.TYPE_INT_ARGB);
        // inside(x, y, grow): the chevron's head widens down to row 14, with a notch cut up into the tail
        java.util.function.BiPredicate<Double, Double> in = (x, y) -> {
            double d = Math.abs(x - 7.5);
            return y >= 1 && y <= 14 && d <= (y - 1) * 0.5 + 0.5 && !(y > 8 && d < (y - 8) * 0.9);
        };
        for (int y = 0; y < 16; y++) for (int x = 0; x < 16; x++) {
            boolean core = in.test((double) x, (double) y);
            boolean near = core || in.test(x - 1.0, (double) y) || in.test(x + 1.0, (double) y) || in.test((double) x, y - 1.0) || in.test((double) x, y + 1.0);
            if (near && !core) g.setRGB(x, y, edge.getRGB());
            else if (core) g.setRGB(x, y, fill.getRGB());
        }
        return g;
    }

    /** A 16x16 gold supply-drop chest with a glow, symmetric so the map's flip and turn don't show. */
    static BufferedImage dropMarker() {
        BufferedImage g = new BufferedImage(16, 16, BufferedImage.TYPE_INT_ARGB);
        for (int x = 0; x < 16; x++) for (int y = 0; y < 16; y++) {
            double d = Math.max(Math.abs(x - 7.5), Math.abs(y - 7.5));
            if (d > 6.5 && d < 8) blend(g, x, y, alpha(GOLD, 110));
        }
        Color dark = new Color(0x5A3A00), gold = GOLD, light = new Color(0xFFF2A0);
        for (int x = 2; x < 14; x++) for (int y = 3; y < 13; y++) {
            boolean edge = x == 2 || x == 13 || y == 3 || y == 12 || y == 7 || y == 8;
            g.setRGB(x, y, (edge ? dark : (y < 7 ? light : gold)).getRGB());
        }
        for (int y = 6; y < 10; y++) for (int x = 7; x < 9; x++) g.setRGB(x, y, new Color(0xFF2E63).getRGB()); // the latch
        return g;
    }

    /**
     * The world border, in Spoke's exploit-glitch look: a seamless 32x32 tile (about 16 px per block,
     * like vanilla) of dark-navy haze, faint cyan scanlines, and cyan and red glitch tears with a few
     * white sparks. The border scrolls it, which makes the tears drift.
     */
    static BufferedImage forcefield() {
        int n = 32;
        BufferedImage g = new BufferedImage(n, n, BufferedImage.TYPE_INT_ARGB);
        java.util.Random r = new java.util.Random(20261029L);
        Color haze = new Color(0x0E, 0x1A, 0x2B, 70), scan = new Color(0x29, 0xD4, 0xFF, 75);
        Color cyan = new Color(0x29, 0xD4, 0xFF, 220), red = new Color(0xFF, 0x18, 0x2B, 220), spark = new Color(255, 255, 255, 210);
        for (int x = 0; x < n; x++) for (int y = 0; y < n; y++) g.setRGB(x, y, (y % 3 == 0 ? mix(haze, scan, 1) : haze).getRGB());
        int[][] tears = {{2, 3, 11, 1}, {18, 8, 9, 2}, {5, 15, 16, 1}, {24, 20, 12, 1}, {9, 26, 7, 2}, {27, 12, 6, 1}};
        for (int k = 0; k < tears.length; k++) {
            int[] t = tears[k];
            Color c = k % 3 == 1 ? red : cyan;
            for (int x = t[0]; x < t[0] + t[2]; x++) for (int y = t[1]; y < t[1] + t[3]; y++) g.setRGB(Math.floorMod(x, n), Math.floorMod(y, n), c.getRGB());
            // a red/cyan offset on the cyan tears (chromatic split)
            if (c == cyan) g.setRGB(Math.floorMod(t[0] + t[2], n), t[1], red.getRGB());
        }
        for (int k = 0; k < 6; k++) g.setRGB(r.nextInt(n), r.nextInt(n), spark.getRGB());
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
