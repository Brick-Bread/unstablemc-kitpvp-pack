import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Random;

/**
 * Synthesises the strike and supply-drop sounds as mono 44.1 kHz WAVs (mono, so Minecraft plays them
 * positionally). sounds.sh turns them into the .ogg files the pack ships.
 *
 * usage: java Sfx.java <out dir>
 */
public class Sfx {

    static final int RATE = 44100;
    static final Random RNG = new Random(20261029L);

    public static void main(String[] a) throws IOException {
        Path out = Path.of(a[0]);
        Files.createDirectories(out);
        wav(out.resolve("strike_charge.wav"), charge());
        wav(out.resolve("strike_siren.wav"), siren());
        wav(out.resolve("strike_impact.wav"), impact());
        wav(out.resolve("drop_incoming.wav"), whistle());
        wav(out.resolve("drop_land.wav"), land());
    }

    /** Charge-up: an exponential sweep from 60 to 600 Hz with harmonics, tremolo and rising hiss. */
    static double[] charge() {
        double len = 2.4;
        double[] s = new double[(int) (RATE * len)];
        double ph = 0, lp = 0;
        for (int i = 0; i < s.length; i++) {
            double t = i / (double) RATE, k = t / len;
            double f = 60 * Math.pow(10, k);
            ph += 2 * Math.PI * f / RATE;
            double trem = 1 - 0.3 * (0.5 + 0.5 * Math.sin(2 * Math.PI * (12 + 18 * k) * t));
            double tone = Math.sin(ph) + 0.5 * Math.sin(2 * ph) + 0.25 * Math.sin(3 * ph + 0.3);
            lp += 0.15 * ((RNG.nextDouble() * 2 - 1) - lp);
            s[i] = (tone * 0.55 * trem + lp * 0.5 * k) * (0.15 + 0.85 * k);
        }
        return fade(s, 0.02, 0.06);
    }

    /** Air-raid wail: 550 Hz rising to 900 and back, twice, sine with a little square bite and vibrato. */
    static double[] siren() {
        double len = 2.0;
        double[] s = new double[(int) (RATE * len)];
        double ph = 0;
        for (int i = 0; i < s.length; i++) {
            double t = i / (double) RATE;
            double f = 725 - 175 * Math.cos(2 * Math.PI * t) + 6 * Math.sin(2 * Math.PI * 7 * t);
            ph += 2 * Math.PI * f / RATE;
            double sq = Math.signum(Math.sin(ph));
            s[i] = 0.75 * Math.sin(ph) + 0.18 * sq + 0.12 * Math.sin(2 * ph);
        }
        return fade(s, 0.05, 0.25);
    }

    /** Impact: a falling sub thump, a low-passed noise blast and crackle that dies away. */
    static double[] impact() {
        double len = 3.0;
        double[] s = new double[(int) (RATE * len)];
        double ph = 0, lp = 0, lp2 = 0;
        for (int i = 0; i < s.length; i++) {
            double t = i / (double) RATE;
            double f = 30 + 35 * Math.exp(-t * 3);
            ph += 2 * Math.PI * f / RATE;
            double thump = Math.sin(ph) * Math.exp(-t / 0.6);
            double cut = 0.02 + 0.35 * Math.exp(-t * 2.5);
            lp += cut * ((RNG.nextDouble() * 2 - 1) - lp);
            lp2 += cut * (lp - lp2);
            double blast = lp2 * 2.2 * Math.exp(-t / 0.9);
            double crackle = RNG.nextDouble() < 0.004 * Math.exp(-t / 0.8) ? (RNG.nextDouble() * 2 - 1) * 0.8 : 0;
            double click = t < 0.004 ? (RNG.nextDouble() * 2 - 1) * (1 - t / 0.004) : 0;
            s[i] = thump * 0.9 + blast + crackle + click * 0.6;
        }
        return fade(s, 0.0, 0.3);
    }

    /** Falling-bomb whistle: 1800 Hz down to 500 Hz, getting louder, over soft wind. */
    static double[] whistle() {
        double len = 2.0;
        double[] s = new double[(int) (RATE * len)];
        double ph = 0, lp = 0;
        for (int i = 0; i < s.length; i++) {
            double t = i / (double) RATE, k = t / len;
            double f = 1800 - 1300 * k + 15 * Math.sin(2 * Math.PI * 5 * t);
            ph += 2 * Math.PI * f / RATE;
            lp += 0.05 * ((RNG.nextDouble() * 2 - 1) - lp);
            s[i] = (Math.sin(ph) * 0.6 + 0.08 * Math.sin(2 * ph)) * (0.2 + 0.8 * k) + lp * 0.6;
        }
        return fade(s, 0.1, 0.12);
    }

    /** Landing: a heavy thud and a metallic crate ring. */
    static double[] land() {
        double len = 1.2;
        double[] s = new double[(int) (RATE * len)];
        double[] partials = {310, 467, 702, 941, 1270};
        double lp = 0;
        for (int i = 0; i < s.length; i++) {
            double t = i / (double) RATE;
            double thud = Math.sin(2 * Math.PI * (70 + 40 * Math.exp(-t * 20)) * t) * Math.exp(-t / 0.15);
            double ring = 0;
            for (int k = 0; k < partials.length; k++) ring += Math.sin(2 * Math.PI * partials[k] * t + k) * Math.exp(-t / (0.5 - k * 0.07)) / (k + 1.5);
            lp += 0.3 * ((RNG.nextDouble() * 2 - 1) - lp);
            double hit = t < 0.03 ? lp * (1 - t / 0.03) : 0;
            s[i] = thud + ring * 0.7 + hit * 0.8;
        }
        return fade(s, 0.0, 0.1);
    }

    /** Linear fade in and out (seconds), then normalised to -1 dBFS. */
    static double[] fade(double[] s, double in, double out) {
        int ni = (int) (in * RATE), no = (int) (out * RATE);
        for (int i = 0; i < ni && i < s.length; i++) s[i] *= i / (double) ni;
        for (int i = 0; i < no && i < s.length; i++) s[s.length - 1 - i] *= i / (double) no;
        double peak = 1e-9;
        for (double v : s) peak = Math.max(peak, Math.abs(v));
        double g = 0.89 / peak;
        for (int i = 0; i < s.length; i++) s[i] *= g;
        return s;
    }

    static void wav(Path p, double[] s) throws IOException {
        ByteBuffer pcm = ByteBuffer.allocate(s.length * 2).order(ByteOrder.LITTLE_ENDIAN);
        for (double v : s) pcm.putShort((short) Math.round(Math.max(-1, Math.min(1, v)) * 32767));
        ByteBuffer h = ByteBuffer.allocate(44).order(ByteOrder.LITTLE_ENDIAN);
        h.put("RIFF".getBytes()).putInt(36 + pcm.capacity()).put("WAVE".getBytes());
        h.put("fmt ".getBytes()).putInt(16).putShort((short) 1).putShort((short) 1).putInt(RATE).putInt(RATE * 2).putShort((short) 2).putShort((short) 16);
        h.put("data".getBytes()).putInt(pcm.capacity());
        ByteArrayOutputStream o = new ByteArrayOutputStream();
        o.write(h.array());
        o.write(pcm.array());
        Files.write(p, o.toByteArray());
    }
}
