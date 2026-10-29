import java.awt.Color;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.file.Path;

/**
 * The custom tab list (font kitpvp:tab): the panel background, the own-card highlight, small icons,
 * health bars and space characters. The plugin (TabText.java / TabPanel.java) lays the tab header out
 * with these, so the sizes and code points here and there must match.
 * <p>
 * The header is lines 9 px apart, each centred. Every line is padded to W = 300 px, so they all start
 * at the same x; Y below is relative to the top of the header's first line. A glyph on line i whose
 * top should be at Y has ascent 7 - (Y - 9 i). Font pages are 256 px, so the 302 px panel is two
 * 151 px halves. Every image is opaque (alpha > 0) in its last column, so its advance is width + 1.
 *
 * | code point | what | size | ascent | advance |
 * |---|---|---|---|---|
 * | U+E000 + 2k, U+E001 + 2k | panel layout k, left and right half; top at Y -9, line 0 | 151 x 9(n+1) | 16 | 152 |
 * | U+E020, U+E021 | own-card highlight, beside the map / below it; top 1 px above the line | 198 x 9, 145 x 9 | 8 | 199, 146 |
 * | U+E030..E038 | heart, empty heart, sword, skull, clock, crate, border, people, eye | 7 x 7 (hearts 7 x 6) | 7 (hearts 6) | 8 |
 * | U+E039 | dot, white (tinted by the text colour) | 5 x 5 (+1 clear row) | 6 | 6 |
 * | U+E03A | separator dot, white | 2 x 2 (+2 clear rows) | 4 | 3 |
 * | U+E040 + f (f = 0..16) | health bar with f px filled | 18 x 5 (+1 clear row) | 6 | 19 |
 * | U+F801..F808, U+F811..F818 | spaces -1..-128, +1..+128 | | | |
 *
 * Layouts (n = lines): k = 0 the compact panel outside rounds (n = 6); k = 1..5 the round panel with
 * R = 0, 2, 4, 6, 9 rows of cards below the map (n = 15 + R).
 */
final class TabGen {

    static final int[] ROWS_BELOW = {0, 2, 4, 6, 9};
    static final int PW = 302;

    static void build(Path out) throws IOException {
        Path tex = out.resolve("assets/kitpvp/textures/gui/tab");
        StringBuilder f = new StringBuilder("{\n  \"providers\": [\n");
        for (int k = 0; k <= ROWS_BELOW.length; k++) {
            int n = k == 0 ? 6 : 15 + ROWS_BELOW[k - 1];
            BufferedImage bg = k == 0 ? compact(n) : full(n, ROWS_BELOW[k - 1]);
            save(tex, "bg" + k + "_l", sub(bg, 0, 151), f, 0xE000 + 2 * k, 16);
            save(tex, "bg" + k + "_r", sub(bg, 151, 151), f, 0xE001 + 2 * k, 16);
        }
        save(tex, "hl_wide", highlight(198), f, 0xE020, 8);
        save(tex, "hl_compact", highlight(145), f, 0xE021, 8);
        String[] names = {"heart", "heart_empty", "sword", "skull", "clock", "crate", "border", "people", "eye"};
        for (int i = 0; i < names.length; i++) save(tex, names[i], icon(names[i]), f, 0xE030 + i, i < 2 ? 6 : 7);
        // the client wants ascent <= height: these get transparent rows at the bottom
        save(tex, "dot", art(new String[]{".www.", "wwwww", "wwwww", "wwwww", ".www.", "....."}), f, 0xE039, 6);
        save(tex, "sep", art(new String[]{"ww", "ww", "..", ".."}), f, 0xE03A, 4);
        for (int i = 0; i <= 16; i++) save(tex, "hp" + i, hpBar(i), f, 0xE040 + i, 6);
        f.append("    {\"type\": \"space\", \"advances\": {");
        for (int b = 0; b < 8; b++) f.append(String.format("\"\\u%04x\": %d, ", 0xF801 + b, -(1 << b)));
        for (int b = 0; b < 8; b++) f.append(String.format("\"\\u%04x\": %d%s", 0xF811 + b, 1 << b, b < 7 ? ", " : ""));
        f.append("}}\n  ]\n}\n");
        Gen.write(out.resolve("assets/kitpvp/font/tab.json"), f.toString());
    }

    /** Saves the image and adds its bitmap provider; refuses images whose advance wouldn't be width + 1. */
    private static void save(Path tex, String name, BufferedImage img, StringBuilder f, int cp, int ascent) throws IOException {
        int last = img.getWidth() - 1;
        boolean opaque = false;
        for (int y = 0; y < img.getHeight(); y++) opaque |= (img.getRGB(last, y) >>> 24) > 0;
        if (!opaque) throw new IllegalStateException(name + ": last column is empty, the advance would be wrong");
        if (img.getWidth() > 254 || img.getHeight() > 254) throw new IllegalStateException(name + ": too big for a 256 px font page");
        if (ascent > img.getHeight()) throw new IllegalStateException(name + ": the client refuses an ascent above the height");
        Gen.save(img, tex.resolve(name + ".png"));
        f.append(String.format("    {\"type\": \"bitmap\", \"file\": \"kitpvp:gui/tab/%s.png\", \"ascent\": %d, \"height\": %d, \"chars\": [\"\\u%04x\"]},%n",
                name, ascent, img.getHeight(), cp));
    }

    private static BufferedImage sub(BufferedImage g, int x, int w) {
        BufferedImage out = new BufferedImage(w, g.getHeight(), BufferedImage.TYPE_INT_ARGB);
        for (int i = 0; i < w; i++) for (int j = 0; j < g.getHeight(); j++) out.setRGB(i, j, g.getRGB(x + i, j));
        return out;
    }

    // ------------------------------------------------------------------ the panel

    // panel image (x', y') = header (x + 1, Y + 9): the panel covers x -1..300 and Y -9..9n-1, the
    // vanilla tab's dark box behind the header exactly (W + 2 wide, down to the last line's bottom)
    private static void fillY(BufferedImage g, int x0, int x1, int y0, int y1, Color c) {
        for (int x = x0; x <= x1; x++) for (int y = y0; y <= y1; y++) Gen.blend(g, x + 1, y + 9, c);
    }

    private static void lineY(BufferedImage g, int x0, int x1, int y) {
        for (int x = x0; x <= x1; x++) {
            double t = (x - x0) / (double) (x1 - x0);
            double fade = Math.min(1, Math.min(x - x0, x1 - x) / 24.0);
            Gen.blend(g, x + 1, y + 9, Gen.alpha(Gen.mix(Gen.PINK, Gen.PURPLE, t), (int) (40 + 150 * fade)));
        }
    }

    private static BufferedImage base(int n) {
        BufferedImage g = new BufferedImage(PW, 9 * (n + 1), BufferedImage.TYPE_INT_ARGB);
        Gen.panel(g, 0, 0, PW, g.getHeight(), Gen.BG);
        // title and info rows on a lighter band, with a soft glow in the middle
        fillY(g, 1, 298, -7, 17, Gen.INNER);
        for (int x = 1; x <= 298; x++) for (int y = -7; y <= 17; y++) {
            double glow = Math.max(0, 1 - Math.abs(x - 150) / 150.0) * (1 - (y + 7) / 25.0);
            Gen.blend(g, x + 1, y + 9, Gen.alpha(Gen.mix(Gen.PINK, Gen.PURPLE, x / 300.0), (int) (26 * glow)));
        }
        lineY(g, 4, 295, 18);
        return g;
    }

    /** Outside rounds: title, info row, divider, two lines of names. */
    private static BufferedImage compact(int n) {
        return base(n);
    }

    /** A round: the map well at x 5..94, Y 24..113, card stripes, and a bottom band for the spectators. */
    private static BufferedImage full(int n, int below) {
        BufferedImage g = base(n);
        Color stripe = Gen.alpha(Color.WHITE, 9), well = new Color(0x0F0E16);
        fillY(g, 5, 94, 24, 113, well);
        fillY(g, 4, 95, 23, 114, Gen.alpha(Gen.PURPLE, 40));
        fillY(g, 5, 94, 24, 113, well);
        for (int i = 3; i <= 12; i++) if (i % 2 == 1) fillY(g, 98, 295, 9 * i - 1, 9 * i + 7, stripe);
        for (int i = 13; i < 13 + below; i++) if (i % 2 == 1) {
            fillY(g, 4, 148, 9 * i - 1, 9 * i + 7, stripe);
            fillY(g, 151, 295, 9 * i - 1, 9 * i + 7, stripe);
        }
        int s = 9 * (13 + below);
        fillY(g, 1, 298, s - 1, 9 * n - 3, Gen.INNER);
        lineY(g, 4, 295, s - 1);
        return g;
    }

    /** The viewer's own card: a pink wash with a bright left edge. */
    private static BufferedImage highlight(int w) {
        BufferedImage g = new BufferedImage(w, 9, BufferedImage.TYPE_INT_ARGB);
        for (int x = 0; x < w; x++) for (int y = 0; y < 9; y++) {
            double t = x / (double) (w - 1);
            g.setRGB(x, y, Gen.alpha(Gen.mix(Gen.PINK, Gen.PURPLE, t), (int) (95 - 45 * t)).getRGB());
        }
        for (int y = 0; y < 9; y++) g.setRGB(0, y, Gen.PINK.getRGB());
        return g;
    }

    // ------------------------------------------------------------------ icons

    private static BufferedImage icon(String name) {
        return switch (name) {
            case "heart" -> art(new String[]{".rr.rr.", "rlrrrrr", "rrrrrrr", ".rrrrr.", "..rrr..", "...r..."});
            case "heart_empty" -> art(new String[]{".mm.mm.", "mddmddm", "mdddddm", ".mdddm.", "..mdm..", "...m..."});
            case "sword" -> art(new String[]{".....ss", "....sws", "...sws.", "g.sws..", ".gss...", ".hg....", "h..g..."});
            case "skull" -> art(new String[]{".wwwww.", "wwwwwww", "wddwddw", "wddwddw", "wwwdwww", ".wwwww.", ".w.w.w."});
            case "clock" -> art(new String[]{".ggggg.", "gddwddg", "gddwddg", "gddwwdg", "gdddddg", "gdddddg", ".ggggg."});
            case "crate" -> art(new String[]{"ggggggg", "gbbgbbg", "gbbgbbg", "gggpggg", "gbbgbbg", "gbbgbbg", "ggggggg"});
            case "border" -> art(new String[]{"pp...pp", "p.....p", ".......", "...w...", ".......", "p.....p", "pp...pp"});
            case "people" -> art(new String[]{"..ttt..", ".ttttt.", "..ttt..", ".......", ".ttttt.", "ttttttt", "ttttttt"});
            case "eye" -> art(new String[]{".......", "..ppp..", ".pdddp.", "pddwddp", ".pdddp.", "..ppp..", "......."});
            default -> throw new IllegalArgumentException(name);
        };
    }

    /** Pixel art from rows of letters: w white, r red, l light red, m muted, d dark, s steel, g gold, b brown, h handle, p pink, t text. */
    private static BufferedImage art(String[] rows) {
        BufferedImage g = new BufferedImage(rows[0].length(), rows.length, BufferedImage.TYPE_INT_ARGB);
        for (int y = 0; y < rows.length; y++) for (int x = 0; x < rows[y].length(); x++) {
            Color c = switch (rows[y].charAt(x)) {
                case 'w' -> Color.WHITE;
                case 'r' -> new Color(0xFF3B4E);
                case 'l' -> new Color(0xFFB3BC);
                case 'm' -> new Color(0x6A6388);
                case 'd' -> new Color(0x15131F);
                case 'e' -> new Color(0x15131F);
                case 's' -> new Color(0xB8B4D0);
                case 'g' -> Gen.GOLD;
                case 'b' -> new Color(0x8A5A10);
                case 'h' -> new Color(0x7A4A20);
                case 'p' -> Gen.PINK;
                case 't' -> Gen.TEXT;
                default -> null;
            };
            if (c != null) g.setRGB(x, y, c.getRGB());
        }
        return g;
    }

    /** An 18x5 health bar (and a clear row under it), f of its 16 inner columns filled: green, then gold below half, red below a quarter. */
    private static BufferedImage hpBar(int f) {
        BufferedImage g = new BufferedImage(18, 6, BufferedImage.TYPE_INT_ARGB);
        Gen.fill(g, 0, 0, 18, 5, new Color(0x0F0E16));
        Gen.fill(g, 1, 1, 16, 3, new Color(0x2C2940));
        Color c = f > 8 ? new Color(0x3CFF6A) : f > 4 ? Gen.GOLD : new Color(0xFF2E3A);
        Gen.fill(g, 1, 1, f, 3, c);
        for (int x = 1; x <= f; x++) g.setRGB(x, 1, Gen.mix(c, Color.WHITE, 0.45).getRGB());
        return g;
    }
}
