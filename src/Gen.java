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

    /**
     * Core shaders are copies of 26.3's own and won't compile on any other version, which makes the
     * whole pack fail to load there (FAILED_RELOAD). They live in an overlay applied only to 26.3's
     * pack format (97.x), so other clients load the pack without them.
     */
    static final String SHADERS = "overlay_26_3/";

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
        // the world border: prism texture, and the vanilla 26.3 shader with the border's
        // blue/red tint taken out (its alpha, the fade with distance, stays) so the design keeps its colours
        // the zone wall: item displays of kitpvp:zone_panel, a flat plane whose two faces differ: the
        // inside (toward the safe zone) a prism diamond lattice, the outside red hazard stripes. Both are
        // 32-frame interpolated animations that wrap exactly, so they loop without a seam.
        for (String side : new String[]{"in", "out"}) {
            save(zonePanel(side.equals("in")), out.resolve("assets/kitpvp/textures/item/zone_" + side + ".png"));
            write(out.resolve("assets/kitpvp/textures/item/zone_" + side + ".png.mcmeta"), """
                    {"animation": {"frametime": 2, "interpolate": true}}
                    """);
        }
        write(out.resolve("assets/kitpvp/items/zone_panel.json"), """
                {"model": {"type": "minecraft:model", "model": "kitpvp:item/zone_panel"}}
                """);
        // south face (+z) = inside: the plugin turns each panel so +z points into the zone
        write(out.resolve("assets/kitpvp/models/item/zone_panel.json"), """
                {
                  "textures": {"in": "kitpvp:item/zone_in", "out": "kitpvp:item/zone_out", "particle": "kitpvp:item/zone_in"},
                  "elements": [
                    {"from": [0, 0, 8], "to": [16, 16, 8], "shade": false, "light_emission": 15,
                     "faces": {"south": {"uv": [0, 0, 16, 16], "texture": "#in"}, "north": {"uv": [16, 0, 0, 16], "texture": "#out"}}}
                  ]
                }
                """);
        // the supply drop crate: an item model (kitpvp:supply_drop) the plugin shows on an item display
        save(crateSide(), out.resolve("assets/kitpvp/textures/item/supply_side.png"));
        save(crateTop(), out.resolve("assets/kitpvp/textures/item/supply_top.png"));
        save(crateLight(), out.resolve("assets/kitpvp/textures/item/supply_light.png"));
        write(out.resolve("assets/kitpvp/items/supply_drop.json"), """
                {"model": {"type": "minecraft:model", "model": "kitpvp:item/supply_drop"}}
                """);
        write(out.resolve("assets/kitpvp/models/item/supply_drop.json"), """
                {
                  "textures": {"side": "kitpvp:item/supply_side", "top": "kitpvp:item/supply_top", "light": "kitpvp:item/supply_light",
                               "particle": "kitpvp:item/supply_side"},
                  "elements": [
                    {"from": [1, 0, 1], "to": [15, 13, 15],
                     "faces": {"north": {"uv": [0, 0, 16, 16], "texture": "#side"}, "south": {"uv": [0, 0, 16, 16], "texture": "#side"},
                               "east": {"uv": [0, 0, 16, 16], "texture": "#side"}, "west": {"uv": [0, 0, 16, 16], "texture": "#side"},
                               "up": {"uv": [0, 0, 16, 16], "texture": "#top"}, "down": {"uv": [0, 0, 16, 16], "texture": "#top"}}},
                    {"from": [6, 13, 6], "to": [10, 15, 10], "light_emission": 15,
                     "faces": {"north": {"uv": [0, 0, 16, 8], "texture": "#light"}, "south": {"uv": [0, 0, 16, 8], "texture": "#light"},
                               "east": {"uv": [0, 0, 16, 8], "texture": "#light"}, "west": {"uv": [0, 0, 16, 8], "texture": "#light"},
                               "up": {"uv": [0, 8, 16, 16], "texture": "#light"}}},
                    {"from": [12, 13, 7.5], "to": [13, 19, 8.5],
                     "faces": {"north": {"uv": [0, 0, 2, 16], "texture": "#top"}, "south": {"uv": [0, 0, 2, 16], "texture": "#top"},
                               "east": {"uv": [0, 0, 2, 16], "texture": "#top"}, "west": {"uv": [0, 0, 2, 16], "texture": "#top"},
                               "up": {"uv": [0, 0, 2, 2], "texture": "#light"}}}
                  ]
                }
                """);
        // the border players see is KitPvP's own wall; the vanilla one (also used for the red screen
        // warning near the edge) is drawn fully transparent
        save(new BufferedImage(16, 16, BufferedImage.TYPE_INT_ARGB), out.resolve("assets/minecraft/textures/misc/forcefield.png"));
        write(out.resolve(SHADERS + "assets/minecraft/shaders/core/world_border.fsh"), """
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
        // a map with data (the radar) is drawn on the checkerboard background in 26.x, so both get the frame
        save(mapFrame(), out.resolve("assets/minecraft/textures/map/map_background.png"));
        save(mapFrame(), out.resolve("assets/minecraft/textures/map/map_background_checkerboard.png"));
        save(arrowMarker(new Color(0xFFFFFF), PINK), out.resolve("assets/minecraft/textures/map/decorations/player.png"));
        save(arrowMarker(new Color(0xFF3B4E), new Color(0x2A0008)), out.resolve("assets/minecraft/textures/map/decorations/red_marker.png"));
        save(dropMarker(), out.resolve("assets/minecraft/textures/map/decorations/target_x.png"));
        // strike and supply-drop sounds: the .ogg files come from sounds.sh (src/sounds), build.sh copies them
        String[][] sfx = {{"strike.charge", "strike/charge", "Strike charging"}, {"strike.siren", "strike/siren", "Strike siren"},
                {"strike.impact", "strike/impact", "Strike impact"}, {"drop.incoming", "drop/incoming", "Supply drop incoming"},
                {"drop.land", "drop/land", "Supply drop lands"}, {"music.lobby1", "music/lobby1", "Lobby music"}, {"music.lobby2", "music/lobby2", "Lobby music"}, {"strike.wither", "strike/wither", "Wither skulls launch"},
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
            // music is long: streamed from disk instead of loaded whole
            boolean stream = sfx[i][0].startsWith("music.");
            sj.append("  \"").append(sfx[i][0]).append("\": {\"sounds\": [{\"name\": \"kitpvp:").append(sfx[i][1])
                    .append(preload ? "\", \"preload\": true" : stream ? "\", \"stream\": true" : "\"").append("}], \"subtitle\": \"").append(sfx[i][2]).append("\"}")
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
        radarHud(out);
        TabGen.build(out); // the custom tab list (src/TabGen.java)
        // the panel draws its own INVENTORY label; vanilla's dark grey one would vanish on it
        String lang = "{\n  \"container.inventory\": \"\"\n}\n";
        for (String l : new String[]{"en_us", "en_gb", "en_ca", "en_au", "en_nz"}) write(out.resolve("assets/minecraft/lang/" + l + ".json"), lang);
        write(out.resolve("pack.mcmeta"), """
                {
                  "pack": {
                    "description": "§dUnstableMC §fKitPvP",
                    "min_format": [97, 0],
                    "max_format": [120, 0]
                  },
                  "overlays": {
                    "entries": [
                      {"directory": "overlay_26_3", "min_format": [97, 0], "max_format": [97, 1]}
                    ]
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
     * The world border, the prism design: diagonal rainbow bands (pink, orange, gold, green, cyan,
     * purple) at low opacity with a thin white highlight, a seamless 32x32 tile (about 16 px per block,
     * like vanilla). The border scrolls it, so the bands drift.
     */
    static BufferedImage forcefield() {
        int n = 32;
        BufferedImage g = new BufferedImage(n, n, BufferedImage.TYPE_INT_ARGB);
        int[] cols = {0xFF2E63, 0xFF7518, 0xFFD900, 0x3CFF6A, 0x29D4FF, 0x8A4BFF, 0xFF2E9A, 0xFF2E63};
        for (int x = 0; x < n; x++) for (int y = 0; y < n; y++) {
            int c = cols[Math.floorMod(x + y, n) * 8 / n];
            g.setRGB(x, y, new Color((c >> 16) & 255, (c >> 8) & 255, c & 255, 110).getRGB());
        }
        for (int x = 0; x < n; x++) blend(g, x, Math.floorMod(-x, n), new Color(255, 255, 255, 120));
        return g;
    }

    // ------------------------------------------------------------------ the radar HUD

    /**
     * The corner radar HUD (radar-hud-spec.md): the title of an invisible boss bar, made of glyphs from
     * kitpvp:radar. text.vsh spots the HUD's #FEFEFE text and moves it from top centre to the top-left
     * corner. Glyph tops sit 16 px below the text line (ascent -9 - row), every image is opaque in its
     * leftmost and rightmost column so the advances are exact.
     */
    static void radarHud(Path out) throws IOException {
        Path tex = out.resolve("assets/kitpvp/textures/gui/radar");
        save(radarBg(), tex.resolve("bg.png"));
        Color outline = new Color(0x15131F), gold = GOLD, brown = new Color(0x5A3A00);
        BufferedImage enemy = new BufferedImage(3, 3, BufferedImage.TYPE_INT_ARGB);
        fill(enemy, 0, 0, 3, 3, PINK);
        save(enemy, tex.resolve("enemy.png"));
        BufferedImage self = new BufferedImage(5, 5, BufferedImage.TYPE_INT_ARGB);
        fill(self, 0, 0, 5, 5, outline);
        fill(self, 1, 1, 3, 3, Color.WHITE);
        save(self, tex.resolve("self.png"));
        BufferedImage drop = new BufferedImage(5, 5, BufferedImage.TYPE_INT_ARGB);
        for (int i = 0; i < 5; i++) {
            drop.setRGB(i, i, gold.getRGB());
            drop.setRGB(4 - i, i, gold.getRGB());
        }
        drop.setRGB(2, 2, brown.getRGB());
        save(drop, tex.resolve("drop.png"));
        // corners: an L of one row and one column; the row's far end covers the other side column
        String[] corners = {"tl", "tr", "bl", "br"};
        for (String k : corners) {
            BufferedImage c = new BufferedImage(4, 4, BufferedImage.TYPE_INT_ARGB);
            int row = k.charAt(0) == 't' ? 0 : 3, col = k.charAt(1) == 'l' ? 0 : 3;
            fill(c, 0, row, 4, 1, PINK);
            fill(c, col, 0, 1, 4, PINK);
            save(c, tex.resolve("corner_" + k + ".png"));
            // the same in purple: where the border will end up
            BufferedImage e = new BufferedImage(4, 4, BufferedImage.TYPE_INT_ARGB);
            fill(e, 0, row, 4, 1, PURPLE);
            fill(e, col, 0, 1, 4, PURPLE);
            save(e, tex.resolve("end_" + k + ".png"));
        }

        StringBuilder f = new StringBuilder("{\n  \"providers\": [\n");
        f.append(provider("bg", 0xE100, 88, -9));
        for (int r = 0; r < 88; r++) f.append(provider("enemy", 0xE200 + r, 3, -8 - r));
        for (int r = 0; r < 88; r++) f.append(provider("self", 0xE300 + r, 5, -7 - r));
        for (int r = 0; r < 88; r++) f.append(provider("drop", 0xE400 + r, 5, -7 - r));
        for (int k = 0; k < 4; k++) for (int t = 0; t < 85; t++) f.append(provider("corner_" + corners[k], 0xE500 + k * 0x100 + t, 4, -9 - t));
        for (int k = 0; k < 4; k++) for (int t = 0; t < 85; t++) f.append(provider("end_" + corners[k], 0xE900 + k * 0x100 + t, 4, -9 - t));
        f.append("    {\"type\": \"space\", \"advances\": {");
        for (int b = 0; b < 8; b++) f.append(String.format("\"\\u%04x\": %d, ", 0xF801 + b, -(1 << b)));
        for (int b = 0; b < 8; b++) f.append(String.format("\"\\u%04x\": %d%s", 0xF811 + b, 1 << b, b < 7 ? ", " : ""));
        f.append("}}\n  ]\n}\n");
        write(out.resolve("assets/kitpvp/font/radar.json"), f.toString());

        // the boss bar carrying the HUD is invisible: transparent white sprites (vanilla 182x5)
        for (String s : new String[]{"white_background", "white_progress"})
            save(new BufferedImage(182, 5, BufferedImage.TYPE_INT_ARGB), out.resolve("assets/minecraft/textures/gui/sprites/boss_bar/" + s + ".png"));

        // vanilla 26.3 text.vsh, plus the HUD move for GUI text (IS_GUI); world text is untouched
        write(out.resolve(SHADERS + "assets/minecraft/shaders/core/text.vsh"), """
                #version 330
                #extension GL_ARB_separate_shader_objects : require

                #if !defined(IS_GUI) && !defined(IS_SEE_THROUGH)
                #include <minecraft:fog.glsl>
                #include <minecraft:sample_lightmap.glsl>
                #endif

                #include <minecraft:dynamictransforms.glsl>
                #include <minecraft:projection.glsl>

                layout(location = 0) in vec3 Position;
                layout(location = 1) in vec4 Color;
                layout(location = 2) in vec2 UV0;
                #if !defined(IS_GUI) && !defined(IS_SEE_THROUGH)
                layout(location = 3) in ivec2 UV2;
                #endif

                #if !defined(IS_GUI) && !defined(IS_SEE_THROUGH)
                uniform sampler2D Sampler2;
                layout(location = 0) out float sphericalVertexDistance;
                layout(location = 1) out float cylindricalVertexDistance;
                #endif

                layout(location = 2) out vec4 vertexColor;
                layout(location = 3) out vec2 texCoord0;

                void main() {
                #ifdef IS_GUI
                    // KitPvP radar HUD: the invisible boss bar's #FEFEFE title moves from top centre to the
                    // top-left corner (89 wide, 4 px in from the left edge, top at y = 4)
                    bool radarHud = all(lessThan(abs(Color.rgb * 255.0 - vec3(254.0)), vec3(0.6)));
                    vec3 pos = Position;
                    if (radarHud) {
                        float guiW = 2.0 / ProjMat[0][0];
                        // the title starts at floor(guiW / 2) - 44; move it to the top-left corner, 4 px in
                        float w = floor(guiW + 0.5);
                        pos = Position + vec3(4.0 - (floor(w / 2.0) - 44.0), -15.0, 0.0);
                    }
                    gl_Position = ProjMat * ModelViewMat * vec4(pos, 1.0);
                #else
                    gl_Position = ProjMat * ModelViewMat * vec4(Position, 1.0);
                #endif

                #if !defined(IS_GUI) && !defined(IS_SEE_THROUGH)
                    sphericalVertexDistance = fog_spherical_distance(Position);
                    cylindricalVertexDistance = fog_cylindrical_distance(Position);
                    vertexColor = Color * sample_lightmap(Sampler2, UV2);
                #else
                    vertexColor = Color;
                #endif
                #ifdef IS_GUI
                    if (radarHud) vertexColor = vec4(1.0);
                #endif
                    texCoord0 = UV0;
                }
                """);
    }

    static String provider(String image, int ch, int height, int ascent) {
        return String.format("    {\"type\": \"bitmap\", \"file\": \"kitpvp:gui/radar/%s.png\", \"ascent\": %d, \"height\": %d, \"chars\": [\"\\u%04x\"]},%n",
                image, ascent, height, ch);
    }

    /**
     * The radar's background: a top-down map of the starting arena (x 140..900, z 220..980, north up)
     * from src/radar-heights.txt (tools/radar_bg.py), coloured like the plugin's radar, in a 2 px
     * pink->purple frame with a pink N at the top. Fully opaque.
     */
    static BufferedImage radarBg() throws IOException {
        int n = 88;
        java.util.List<String> lines = Files.readAllLines(Path.of("src/radar-heights.txt"));
        int[][] h = new int[n][n];
        for (int j = 0; j < n; j++) {
            String[] v = lines.get(j).trim().split(" ");
            for (int i = 0; i < n; i++) h[j][i] = Integer.parseInt(v[i]);
        }
        Color street = new Color(0x262534), ring = new Color(0x363448), plaza = new Color(0x46425C),
                low = new Color(0x70628C), tall = new Color(0xD6C4EC);
        BufferedImage g = new BufferedImage(n, n, BufferedImage.TYPE_INT_ARGB);
        for (int j = 0; j < n; j++) for (int i = 0; i < n; i++) {
            int y = h[j][i];
            Color c = y <= -8 ? street : y <= 2 ? ring : y <= 11 ? plaza : mix(low, tall, (y - 11) / 70.0);
            int up = j > 0 ? h[j - 1][i] : y, left = i > 0 ? h[j][i - 1] : y;
            if (y - Math.min(up, left) > 2) c = shade(c, 1.25);          // lit edge facing north-west
            else if (Math.max(up, left) - y > 2) c = shade(c, 0.6);      // shadow under a taller neighbour
            g.setRGB(i, j, c.getRGB());
        }
        // frame: the outer pixel in the gradient, the inner one the gradient dimmed
        for (int i = 0; i < n; i++) for (int j = 0; j < n; j++) {
            int d = Math.min(Math.min(i, j), Math.min(n - 1 - i, n - 1 - j));
            if (d > 1) continue;
            Color c = mix(PINK, PURPLE, (i + j) / (double) (2 * n - 2));
            g.setRGB(i, j, (d == 0 ? c : mix(c, new Color(0x15131F), 0.45)).getRGB());
        }
        // N: a 4x5 pink letter on a dark tab hanging from the top frame's centre
        fill(g, 41, 2, 6, 7, new Color(0x15131F));
        String[] letter = {"#..#", "##.#", "#.##", "#..#", "#..#"};
        for (int j = 0; j < 5; j++) for (int i = 0; i < 4; i++) if (letter[j].charAt(i) == '#') g.setRGB(42 + i, 3 + j, PINK.getRGB());
        fill(g, 42, 0, 4, 2, PINK);
        return g;
    }

    static Color shade(Color c, double k) {
        return new Color((int) Math.min(255, c.getRed() * k), (int) Math.min(255, c.getGreen() * k), (int) Math.min(255, c.getBlue() * k));
    }

    /**
     * A zone wall face: 64x64 frames (2 px per block on a 32-block panel) stacked 16 high, each one
     * step along so frame 16 wraps onto frame 1. Mostly clear, so the wall reads as glowing lines, not
     * a sheet of colour (and it fades out with distance instead of fogging the view).
     * Inside: a diamond lattice (16 px cells) in a pink-purple blend with a soft halo and a thin white
     * scanline sweeping down. Outside: thin red hazard stripes sliding sideways. Every pattern repeats
     * within 64 px, so panels line up edge to edge.
     */
    static BufferedImage zonePanel(boolean inside) {
        int n = 64, frames = 16;
        BufferedImage g = new BufferedImage(n, n * frames, BufferedImage.TYPE_INT_ARGB);
        for (int f = 0; f < frames; f++) for (int x = 0; x < n; x++) for (int y = 0; y < n; y++) {
            Color col = new Color(0, 0, 0, 0);
            if (inside) {
                Color line = mix(PINK, PURPLE, 0.5 + 0.5 * Math.sin(2 * Math.PI * (x + y + f * 4) / n));
                int d1 = Math.floorMod(x + y, 16), d2 = Math.floorMod(x - y, 16);
                if (d1 == 0 || d2 == 0) col = alpha(line, 230);
                else if (d1 == 1 || d2 == 1 || d1 == 15 || d2 == 15) col = alpha(line, 70);
                else col = alpha(PURPLE, 14);
                int scan = Math.floorMod(y - f * 4, n);
                if (scan == 0) col = new Color(255, 255, 255, 170);
                else if (scan == 1) col = new Color(255, 255, 255, 60);
            } else {
                int st = Math.floorMod(x + y - f, 16);
                if (st < 3) col = new Color(0xE0, 0x18, 0x30, 200);
                else if (st == 3) col = new Color(0xE0, 0x18, 0x30, 70);
                else col = new Color(0x40, 0x04, 0x0A, 18);
            }
            g.setRGB(x, f * n + y, col.getRGB());
        }
        return g;
    }

    /**
     * Supply crate side (32x32): dark plated panel in a pink-to-purple frame, gold corner brackets,
     * rivets, two hazard bands, and a glowing gold comet emblem in the middle.
     */
    static BufferedImage crateSide() {
        int n = 32;
        BufferedImage g = new BufferedImage(n, n, BufferedImage.TYPE_INT_ARGB);
        Color plate = new Color(0x221E33), plate2 = new Color(0x2C2742), dark = new Color(0x0F0E16);
        for (int x = 0; x < n; x++) for (int y = 0; y < n; y++) g.setRGB(x, y, ((x / 4 + y / 4) % 2 == 0 ? plate : plate2).getRGB());
        // frame: 2 px pink to purple, dark inner line
        for (int i = 0; i < n; i++) for (int w = 0; w < 2; w++) {
            Color c = mix(PINK, PURPLE, i / (double) n);
            g.setRGB(i, w, c.getRGB()); g.setRGB(i, n - 1 - w, mix(PURPLE, PINK, i / (double) n).getRGB());
            g.setRGB(w, i, c.getRGB()); g.setRGB(n - 1 - w, i, mix(PURPLE, PINK, i / (double) n).getRGB());
        }
        for (int i = 2; i < n - 2; i++) { g.setRGB(i, 2, dark.getRGB()); g.setRGB(i, n - 3, dark.getRGB()); g.setRGB(2, i, dark.getRGB()); g.setRGB(n - 3, i, dark.getRGB()); }
        // gold corner brackets and rivets
        int[][] cs = {{3, 3, 1, 1}, {n - 4, 3, -1, 1}, {3, n - 4, 1, -1}, {n - 4, n - 4, -1, -1}};
        for (int[] c : cs) {
            for (int k = 0; k < 6; k++) { g.setRGB(c[0] + k * c[2], c[1], GOLD.getRGB()); g.setRGB(c[0], c[1] + k * c[3], GOLD.getRGB()); }
            g.setRGB(c[0] + 2 * c[2], c[1] + 2 * c[3], new Color(0xFFF2A0).getRGB());
        }
        // hazard bands top and bottom
        for (int x = 5; x < n - 5; x++) for (int y : new int[]{6, 7, 24, 25}) g.setRGB(x, y, (Math.floorMod(x + y, 4) < 2 ? PINK : dark).getRGB());
        // comet emblem: a gold star with a tail
        int cx = 17, cy = 15;
        for (int dx = -2; dx <= 2; dx++) for (int dy = -2; dy <= 2; dy++) if (Math.abs(dx) + Math.abs(dy) <= 2) g.setRGB(cx + dx, cy + dy, GOLD.getRGB());
        g.setRGB(cx, cy, new Color(0xFFFFFF).getRGB());
        for (int k = 1; k <= 6; k++) {
            blend(g, cx - 2 - k, cy + k / 2 + 1, alpha(GOLD, 220 - k * 30));
            blend(g, cx - 2 - k, cy + k / 2, alpha(new Color(0xFF7518), 200 - k * 28));
        }
        return g;
    }

    /** Supply crate lid (32x32): plated, gold cross straps, a hatch in the middle. */
    static BufferedImage crateTop() {
        int n = 32;
        BufferedImage g = new BufferedImage(n, n, BufferedImage.TYPE_INT_ARGB);
        Color plate = new Color(0x26223A), plate2 = new Color(0x2F2A47), dark = new Color(0x0F0E16);
        for (int x = 0; x < n; x++) for (int y = 0; y < n; y++) g.setRGB(x, y, ((x / 4 + y / 4) % 2 == 0 ? plate : plate2).getRGB());
        for (int i = 0; i < n; i++) for (int w = 0; w < 2; w++) {
            Color c = mix(PINK, PURPLE, i / (double) n);
            g.setRGB(i, w, c.getRGB()); g.setRGB(i, n - 1 - w, c.getRGB()); g.setRGB(w, i, c.getRGB()); g.setRGB(n - 1 - w, i, c.getRGB());
        }
        for (int i = 2; i < n - 2; i++) for (int w = -1; w <= 1; w++) { g.setRGB(i, n / 2 + w, (w == 0 ? GOLD : new Color(0x8A6A00)).getRGB()); g.setRGB(n / 2 + w, i, (w == 0 ? GOLD : new Color(0x8A6A00)).getRGB()); }
        for (int x = 11; x < 21; x++) for (int y = 11; y < 21; y++) {
            boolean edge = x == 11 || x == 20 || y == 11 || y == 20;
            g.setRGB(x, y, (edge ? dark : new Color(0x3A3456)).getRGB());
        }
        return g;
    }

    /** The beacon light on top (16x16): top half the sides (pink glow with a white core), bottom half its top. */
    static BufferedImage crateLight() {
        BufferedImage g = new BufferedImage(16, 16, BufferedImage.TYPE_INT_ARGB);
        for (int x = 0; x < 16; x++) for (int y = 0; y < 16; y++) {
            double d = Math.abs(x - 7.5) / 8.0;
            Color c = y < 8 ? mix(new Color(0xFFFFFF), PINK, d * 1.4 + (y < 2 || y > 5 ? 0.3 : 0)) : mix(new Color(0xFFFFFF), PINK, Math.hypot(x - 7.5, y - 11.5) / 6.0);
            g.setRGB(x, y, c.getRGB());
        }
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
