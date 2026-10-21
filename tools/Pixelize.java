import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import javax.imageio.ImageIO;

/**
 * Offline photo-to-pixel-art converter for Chrono Runner.
 *
 * <p>Reads a picture, knocks out its background, isolates the subject, crops to
 * it, scales it onto an exact pixel grid, reduces it to a small palette and
 * writes a 1:1 PNG with alpha. Drop the result in {@code assets/<kind>/} and the
 * game picks it up with no recompile.
 *
 * <p>This lives outside {@code src/} on purpose: it is a build-time tool, not
 * part of the game. Run it as a single-file program:
 *
 * <pre>
 *   java tools/Pixelize.java "phan si kien.jpg" assets/players/phan_si_kien.png \
 *       -w 44 -h 62 --colors 16 --bg auto
 *   java tools/Pixelize.java photo.png out.png --bg none      (keep background)
 *   java tools/Pixelize.java photo.png out.png --bg "#FFFFFF" (force one key)
 * </pre>
 *
 * <p>Flags: {@code -w/-h} output size, {@code --colors} palette size (2-64),
 * {@code --bg} auto|none|#RRGGBB, {@code --tolerance} background match 0-255,
 * {@code --contrast}/{@code --saturate} multipliers applied before scaling,
 * {@code --no-isolate} keep every background remnant, {@code --no-crop} skip the
 * crop to the subject, {@code --preview} preview file (default
 * {@code <out>_preview.png}, "none" to skip), {@code --preview-scale} zoom.
 *
 * <p>{@code --bg auto} samples the border ring (inset slightly, so a picture
 * frame is stepped over) and keeps the few most common colours as background
 * keys, then floods inward from the border. Flooding rather than keying every
 * pixel is what preserves a white shirt against a white wall: only the region
 * connected to the edge is removed.
 */
public final class Pixelize {

    private int width = 44;
    private int height = 62;
    private int colors = 16;
    private String bg = "auto";
    private int tolerance = 48;
    private double contrast = 1.0;
    private double saturate = 1.0;
    private boolean isolate = true;
    private boolean crop = true;
    private String preview = null;
    private int previewScale = 8;
    private String input;
    private String output;

    public static void main(String[] args) throws IOException {
        Pixelize tool = new Pixelize();
        tool.parse(args);
        tool.run();
    }

    private void parse(String[] args) {
        List<String> positional = new ArrayList<>();
        for (int i = 0; i < args.length; i++) {
            switch (args[i]) {
                case "-w": case "--width": width = Integer.parseInt(next(args, ++i)); break;
                case "-h": case "--height": height = Integer.parseInt(next(args, ++i)); break;
                case "-c": case "--colors": colors = Integer.parseInt(next(args, ++i)); break;
                case "--bg": bg = next(args, ++i); break;
                case "--tolerance": tolerance = Integer.parseInt(next(args, ++i)); break;
                case "--contrast": contrast = Double.parseDouble(next(args, ++i)); break;
                case "--saturate": saturate = Double.parseDouble(next(args, ++i)); break;
                case "--preview": preview = next(args, ++i); break;
                case "--preview-scale": previewScale = Integer.parseInt(next(args, ++i)); break;
                case "--no-isolate": isolate = false; break;
                case "--isolate": isolate = true; break;
                case "--no-crop": crop = false; break;
                case "--crop": crop = true; break;
                case "--help": case "-?": usage(); System.exit(0); break;
                default: positional.add(args[i]); break;
            }
        }
        if (positional.size() != 2) {
            usage();
            throw new IllegalArgumentException("expected an input and an output path");
        }
        input = positional.get(0);
        output = positional.get(1);
    }

    private static String next(String[] args, int i) {
        if (i >= args.length) throw new IllegalArgumentException("missing value after " + args[i - 1]);
        return args[i];
    }

    private static void usage() {
        System.out.println("usage: java tools/Pixelize.java <input> <output.png> "
                + "[-w W] [-h H] [--colors N] [--bg auto|none|#RRGGBB] [--tolerance N] "
                + "[--contrast X] [--saturate X] [--no-isolate] [--no-crop] "
                + "[--preview FILE|none] [--preview-scale N]");
    }

    private void run() throws IOException {
        if (colors < 2 || colors > 64) throw new IllegalArgumentException("--colors must be 2-64");
        if (width < 1 || height < 1) throw new IllegalArgumentException("size must be positive");

        BufferedImage src = ImageIO.read(new File(input));
        if (src == null) {
            throw new IOException("cannot decode " + input
                    + " (this JDK has no reader for that format; convert it to PNG or JPG first)");
        }
        System.out.println("read      " + input + " " + src.getWidth() + "x" + src.getHeight());

        int sw = src.getWidth();
        int sh = src.getHeight();
        int[] px = toArgb(src);

        if (!"none".equalsIgnoreCase(bg)) {
            int[] keys = "auto".equalsIgnoreCase(bg)
                    ? borderKeys(px, sw, sh, tolerance)
                    : new int[] { parseColor(bg) };
            StringBuilder keyList = new StringBuilder();
            for (int k : keys) keyList.append(String.format(" #%06X", k & 0xFFFFFF));
            int removed = removeBackground(px, sw, sh, keys, tolerance, ringInset(sw, sh));
            System.out.printf("bg        keys%s tolerance %d -> %d px cleared (%.0f%%)%n",
                    keyList, tolerance, removed, 100.0 * removed / (sw * (double) sh));
        }

        if (isolate) {
            int kept = keepLargest(px, sw, sh);
            System.out.println("isolate   largest subject keeps " + kept + " px");
        }

        if (contrast != 1.0 || saturate != 1.0) {
            for (int i = 0; i < px.length; i++) {
                px[i] = tone(px[i], contrast, saturate);
            }
        }

        if (crop) {
            int[] box = opaqueBounds(px, sw, sh);
            if (box != null) {
                System.out.println("crop      " + (box[2] - box[0] + 1) + "x" + (box[3] - box[1] + 1)
                        + " at " + box[0] + "," + box[1]);
                px = cropTo(px, sw, sh, box);
                sw = box[2] - box[0] + 1;
                sh = box[3] - box[1] + 1;
            }
        }

        int[] small = fit(px, sw, sh, width, height);
        int[] palette = medianCut(small, colors);
        quantize(small, palette);
        System.out.println("grid      " + width + "x" + height + " -> " + palette.length + " colours");

        writePng(small, width, height, new File(output), 1);
        System.out.println("wrote     " + output);

        if (!"none".equalsIgnoreCase(preview)) {
            File pv = new File(preview != null ? preview : previewName(output));
            writePng(small, width, height, pv, previewScale);
            System.out.println("preview   " + pv + " (" + width * previewScale + "x"
                    + height * previewScale + ")");
        }
    }

    private static int parseColor(String value) {
        String hex = value.startsWith("#") ? value.substring(1) : value;
        return Integer.parseInt(hex, 16) & 0xFFFFFF;
    }

    // ----- image plumbing ---------------------------------------------------

    private static int[] toArgb(BufferedImage img) {
        BufferedImage argb = new BufferedImage(img.getWidth(), img.getHeight(),
                BufferedImage.TYPE_INT_ARGB);
        java.awt.Graphics2D g = argb.createGraphics();
        g.drawImage(img, 0, 0, null);
        g.dispose();
        return argb.getRGB(0, 0, img.getWidth(), img.getHeight(), null, 0, img.getWidth());
    }

    private static boolean near(int a, int b, int tol) {
        return Math.abs(((a >> 16) & 0xFF) - ((b >> 16) & 0xFF)) <= tol
                && Math.abs(((a >> 8) & 0xFF) - ((b >> 8) & 0xFF)) <= tol
                && Math.abs((a & 0xFF) - (b & 0xFF)) <= tol;
    }

    /**
     * Background colours sampled from a ring just inside the image edge. The
     * inset is what steps over a decorative frame: a photo of pixel art is often
     * bordered in black while the actual backdrop is white.
     */
    /**
     * Rings to sample, as insets from the edge. Two of them: a frame box is
     * usually drawn a few percent in, so a single shallow ring lands on the
     * frame's outer margin and then cannot get past it.
     */
    private static int[] ringInsets(int w, int h) {
        int min = Math.min(w, h);
        int a = Math.max(2, min / 50);
        int b = Math.max(2, min / 16);
        return a == b ? new int[] { a } : new int[] { a, b };
    }

    private static int[] borderKeys(int[] px, int w, int h, int tol) {
        List<Integer> ring = new ArrayList<>();
        for (int inset : ringInsets(w, h)) {
            collectRing(px, w, h, inset, ring);
        }
        List<Integer> guard = new ArrayList<>();
        collectCentre(px, w, h, guard);
        int centreFloor = Math.max(4, guard.size() * 2 / 5);
        for (int x = inset; x < w - inset; x += 2) {
            ring.add(px[inset * w + x]);
            ring.add(px[(h - 1 - inset) * w + x]);
        }
        for (int y = inset; y < h - inset; y += 2) {
            ring.add(px[y * w + inset]);
            ring.add(px[y * w + w - 1 - inset]);
        }
        int bucket = Math.max(8, tol / 2);
        List<int[]> clusters = new ArrayList<>();   // {rSum, gSum, bSum, count}
        for (int p : ring) {
            if ((p >>> 24) == 0) continue;
            int[] hit = null;
            for (int[] c : clusters) {
                int avg = (((int) (c[0] / c[3])) << 16)
                        | (((int) (c[1] / c[3])) << 8) | (int) (c[2] / c[3]);
                if (near(p, avg, bucket)) { hit = c; break; }
            }
            if (hit == null) {
                clusters.add(new int[] { p >> 16 & 0xFF, p >> 8 & 0xFF, p & 0xFF, 1 });
            } else {
                hit[0] += p >> 16 & 0xFF;
                hit[1] += p >> 8 & 0xFF;
                hit[2] += p & 0xFF;
                hit[3]++;
            }
        }
        clusters.sort((a, b) -> Integer.compare(b[3], a[3]));
        // Only prominent clusters count. Four keys sampled off a black frame and
        // a white backdrop span the entire grey range, which keys away so much
        // of the picture that the subject itself floods out.
        int floor = Math.max(2, ring.size() / 12);
        List<Integer> keys = new ArrayList<>();
        for (int[] c : clusters) {
            if (keys.size() == 3) break;
            if (c[3] < floor) break;
            keys.add(((int) (c[0] / c[3]) << 16) | ((int) (c[1] / c[3]) << 8) | (int) (c[2] / c[3]));
            System.out.printf("          ring key #%06X on %.0f%% of border%n",
                    keys.get(keys.size() - 1) & 0xFFFFFF, 100.0 * c[3] / ring.size());
        }
        if (keys.isEmpty()) {
            int[] c = clusters.get(0);
            keys.add(((int) (c[0] / c[3]) << 16) | ((int) (c[1] / c[3]) << 8) | (int) (c[2] / c[3]));
        }
        int[] arr = new int[keys.size()];
        for (int i = 0; i < arr.length; i++) arr[i] = keys.get(i);
        return arr;
    }

    /**
     * Clears background by flooding inward from the image border, so only the
     * connected outer region goes and a shirt matching the wall behind it stays.
     *
     * <p>Seeds come from the border <em>and</em> from the inset ring the keys were
     * sampled from. Without the ring seeds a picture framed in black never
     * connects to its own white backdrop, and nothing would be removed at all.
     */
    private static int removeBackground(int[] px, int w, int h, int[] keys, int tol, int inset) {
        boolean[] seen = new boolean[w * h];
        ArrayDeque<Integer> queue = new ArrayDeque<>();
        for (int x = 0; x < w; x++) {
            push(px, seen, queue, x, keys, tol);
            push(px, seen, queue, (h - 1) * w + x, keys, tol);
            if (x >= inset && x < w - inset) {
                push(px, seen, queue, inset * w + x, keys, tol);
                push(px, seen, queue, (h - 1 - inset) * w + x, keys, tol);
            }
        }
        for (int y = 0; y < h; y++) {
            push(px, seen, queue, y * w, keys, tol);
            push(px, seen, queue, y * w + w - 1, keys, tol);
            if (y >= inset && y < h - inset) {
                push(px, seen, queue, y * w + inset, keys, tol);
                push(px, seen, queue, y * w + w - 1 - inset, keys, tol);
            }
        }
        int cleared = 0;
        while (!queue.isEmpty()) {
            int i = queue.poll();
            px[i] = 0;
            cleared++;
            int x = i % w;
            int y = i / w;
            if (x > 0) push(px, seen, queue, i - 1, keys, tol);
            if (x < w - 1) push(px, seen, queue, i + 1, keys, tol);
            if (y > 0) push(px, seen, queue, i - w, keys, tol);
            if (y < h - 1) push(px, seen, queue, i + w, keys, tol);
        }
        return cleared;
    }

    private static void push(int[] px, boolean[] seen, ArrayDeque<Integer> queue,
                             int i, int[] keys, int tol) {
        if (seen[i] || (px[i] >>> 24) == 0) return;
        boolean match = false;
        for (int k : keys) {
            if (near(px[i], k, tol)) { match = true; break; }
        }
        if (!match) return;
        seen[i] = true;
        queue.add(i);
    }

    /**
     * Keeps only the largest 8-connected opaque blob and clears the rest. Drops
     * leftover frame edges, watermarks and speckle so the crop lands on the
     * character instead of the whole picture.
     */
    private static int keepLargest(int[] px, int w, int h) {
        boolean[] seen = new boolean[w * h];
        int[] best = null;
        int bestSize = 0;
        int[] stack = new int[w * h];
        for (int start = 0; start < px.length; start++) {
            if (seen[start] || (px[start] >>> 24) == 0) continue;
            int size = 0;
            int top = 0;
            stack[top++] = start;
            seen[start] = true;
            while (top > 0) {
                int i = stack[--top];
                size++;
                int x = i % w;
                int y = i / w;
                for (int dy = -1; dy <= 1; dy++) {
                    for (int dx = -1; dx <= 1; dx++) {
                        if (dx == 0 && dy == 0) continue;
                        int nx = x + dx, ny = y + dy;
                        if (nx < 0 || ny < 0 || nx >= w || ny >= h) continue;
                        int j = ny * w + nx;
                        if (seen[j] || (px[j] >>> 24) == 0) continue;
                        seen[j] = true;
                        stack[top++] = j;
                    }
                }
            }
            if (size > bestSize) { bestSize = size; best = new int[] { start }; }
        }
        if (best == null) return 0;
        // Re-walk the winning blob, clearing everything not reached.
        boolean[] keep = new boolean[w * h];
        int top = 0;
        stack[top++] = best[0];
        keep[best[0]] = true;
        while (top > 0) {
            int i = stack[--top];
            int x = i % w;
            int y = i / w;
            for (int dy = -1; dy <= 1; dy++) {
                for (int dx = -1; dx <= 1; dx++) {
                    if (dx == 0 && dy == 0) continue;
                    int nx = x + dx, ny = y + dy;
                    if (nx < 0 || ny < 0 || nx >= w || ny >= h) continue;
                    int j = ny * w + nx;
                    if (keep[j] || (px[j] >>> 24) == 0) continue;
                    keep[j] = true;
                    stack[top++] = j;
                }
            }
        }
        for (int i = 0; i < px.length; i++) {
            if (!keep[i]) px[i] = 0;
        }
        return bestSize;
    }

    /** Smallest opaque bounding box as {minX, minY, maxX, maxY}, or null. */
    private static int[] opaqueBounds(int[] px, int w, int h) {
        int minX = w, minY = h, maxX = -1, maxY = -1;
        for (int y = 0; y < h; y++) {
            for (int x = 0; x < w; x++) {
                if ((px[y * w + x] >>> 24) < 16) continue;
                if (x < minX) minX = x;
                if (x > maxX) maxX = x;
                if (y < minY) minY = y;
                if (y > maxY) maxY = y;
            }
        }
        return maxX < 0 ? null : new int[] { minX, minY, maxX, maxY };
    }

    private static int[] cropTo(int[] px, int w, int h, int[] box) {
        int cw = box[2] - box[0] + 1;
        int ch = box[3] - box[1] + 1;
        int[] out = new int[cw * ch];
        for (int y = 0; y < ch; y++) {
            System.arraycopy(px, (box[1] + y) * w + box[0], out, y * cw, cw);
        }
        return out;
    }

    /** Contrast around mid-grey, then a saturation scale in plain RGB terms. */
    private static int tone(int p, double contrast, double saturate) {
        int a = p >>> 24;
        if (a == 0) return 0;
        double r = clamp((((p >> 16) & 0xFF) - 128) * contrast + 128);
        double g = clamp((((p >> 8) & 0xFF) - 128) * contrast + 128);
        double b = clamp(((p & 0xFF) - 128) * contrast + 128);
        double luma = 0.299 * r + 0.587 * g + 0.114 * b;
        r = clamp(luma + (r - luma) * saturate);
        g = clamp(luma + (g - luma) * saturate);
        b = clamp(luma + (b - luma) * saturate);
        return (a << 24) | ((int) r << 16) | ((int) g << 8) | (int) b;
    }

    private static double clamp(double v) {
        return v < 0 ? 0 : (v > 255 ? 255 : v);
    }

    // ----- scaling ----------------------------------------------------------

    /**
     * Scales the subject onto exactly {@code dw}x{@code dh}, preserving its
     * aspect ratio and centring it in the transparent remainder. Downscaling
     * area-averages; upscaling picks nearest so no blur is invented.
     */
    private static int[] fit(int[] src, int sw, int sh, int dw, int dh) {
        double scale = Math.min(dw / (double) sw, dh / (double) sh);
        int nw = Math.max(1, Math.min(dw, (int) Math.round(sw * scale)));
        int nh = Math.max(1, Math.min(dh, (int) Math.round(sh * scale)));
        int[] resized = nw <= sw && nh <= sh
                ? downscale(src, sw, sh, nw, nh)
                : upscale(src, sw, sh, nw, nh);
        int[] out = new int[dw * dh];
        int offX = (dw - nw) / 2;
        int offY = (dh - nh) / 2;
        for (int y = 0; y < nh; y++) {
            System.arraycopy(resized, y * nw, out, (offY + y) * dw + offX, nw);
        }
        return out;
    }

    /**
     * Area-average downscale. Colour is accumulated premultiplied by alpha so
     * transparent neighbours (a knocked-out background) cannot darken the edge
     * pixels of the subject into a muddy fringe.
     */
    private static int[] downscale(int[] src, int sw, int sh, int dw, int dh) {
        int[] out = new int[dw * dh];
        for (int dy = 0; dy < dh; dy++) {
            int y0 = dy * sh / dh;
            int y1 = Math.max(y0 + 1, (dy + 1) * sh / dh);
            for (int dx = 0; dx < dw; dx++) {
                int x0 = dx * sw / dw;
                int x1 = Math.max(x0 + 1, (dx + 1) * sw / dw);
                long ra = 0, ga = 0, ba = 0, aa = 0;
                int n = 0;
                for (int y = y0; y < y1; y++) {
                    for (int x = x0; x < x1; x++) {
                        int p = src[y * sw + x];
                        int a = p >>> 24;
                        ra += ((p >> 16) & 0xFF) * a;
                        ga += ((p >> 8) & 0xFF) * a;
                        ba += (p & 0xFF) * a;
                        aa += a;
                        n++;
                    }
                }
                if (n == 0 || aa == 0) continue;
                int a = (int) (aa / n);
                int r = (int) (ra / aa);
                int g = (int) (ga / aa);
                int b = (int) (ba / aa);
                out[dy * dw + dx] = (a << 24) | (r << 16) | (g << 8) | b;
            }
        }
        return out;
    }

    private static int[] upscale(int[] src, int sw, int sh, int dw, int dh) {
        int[] out = new int[dw * dh];
        for (int y = 0; y < dh; y++) {
            int sy = Math.min(sh - 1, y * sh / dh);
            for (int x = 0; x < dw; x++) {
                out[y * dw + x] = src[sy * sw + Math.min(sw - 1, x * sw / dw)];
            }
        }
        return out;
    }

    // ----- palette ----------------------------------------------------------

    /** Median-cut palette over the opaque pixels of the scaled image. */
    private static int[] medianCut(int[] px, int colors) {
        List<int[]> samples = new ArrayList<>();
        for (int p : px) if ((p >>> 24) != 0) samples.add(new int[] { p });
        if (samples.isEmpty()) return new int[] { 0 };
        List<List<int[]>> boxes = new ArrayList<>();
        boxes.add(samples);
        while (boxes.size() < colors) {
            List<int[]> box = null;
            int widest = -1;
            for (List<int[]> b : boxes) {
                if (b.size() < 2) continue;
                int range = channelRange(b);
                if (range > widest) { widest = range; box = b; }
            }
            if (box == null || widest <= 0) break;
            boxes.remove(box);
            List<List<int[]>> split = split(box);
            boxes.add(split.get(0));
            boxes.add(split.get(1));
        }
        int[] palette = new int[boxes.size()];
        for (int i = 0; i < boxes.size(); i++) {
            long r = 0, g = 0, b = 0;
            for (int[] s : boxes.get(i)) {
                r += (s[0] >> 16) & 0xFF;
                g += (s[0] >> 8) & 0xFF;
                b += s[0] & 0xFF;
            }
            int n = boxes.get(i).size();
            palette[i] = ((int) (r / n) << 16) | ((int) (g / n) << 8) | (int) (b / n);
        }
        return palette;
    }

    private static int channelRange(List<int[]> box) {
        int rmin = 255, rmax = 0, gmin = 255, gmax = 0, bmin = 255, bmax = 0;
        for (int[] s : box) {
            int p = s[0];
            int r = (p >> 16) & 0xFF, g = (p >> 8) & 0xFF, b = p & 0xFF;
            rmin = Math.min(rmin, r); rmax = Math.max(rmax, r);
            gmin = Math.min(gmin, g); gmax = Math.max(gmax, g);
            bmin = Math.min(bmin, b); bmax = Math.max(bmax, b);
        }
        return Math.max(rmax - rmin, Math.max(gmax - gmin, bmax - bmin));
    }

    private static List<List<int[]>> split(List<int[]> box) {
        int rmin = 255, rmax = 0, gmin = 255, gmax = 0, bmin = 255, bmax = 0;
        for (int[] s : box) {
            int p = s[0];
            int r = (p >> 16) & 0xFF, g = (p >> 8) & 0xFF, b = p & 0xFF;
            rmin = Math.min(rmin, r); rmax = Math.max(rmax, r);
            gmin = Math.min(gmin, g); gmax = Math.max(gmax, g);
            bmin = Math.min(bmin, b); bmax = Math.max(bmax, b);
        }
        int dr = rmax - rmin, dg = gmax - gmin, db = bmax - bmin;
        final int shift = (dr >= dg && dr >= db) ? 16 : (dg >= db ? 8 : 0);
        box.sort((a, b) -> Integer.compare((a[0] >> shift) & 0xFF, (b[0] >> shift) & 0xFF));
        int mid = box.size() / 2;
        return Arrays.asList(box.subList(0, mid), box.subList(mid, box.size()));
    }

    private static void quantize(int[] px, int[] palette) {
        for (int i = 0; i < px.length; i++) {
            int a = px[i] >>> 24;
            if (a == 0) continue;
            int best = 0;
            long bestD = Long.MAX_VALUE;
            for (int j = 0; j < palette.length; j++) {
                long d = dist2(px[i], palette[j]);
                if (d < bestD) { bestD = d; best = j; }
            }
            px[i] = (a << 24) | palette[best];
        }
    }

    private static long dist2(int a, int b) {
        int dr = ((a >> 16) & 0xFF) - ((b >> 16) & 0xFF);
        int dg = ((a >> 8) & 0xFF) - ((b >> 8) & 0xFF);
        int db = (a & 0xFF) - (b & 0xFF);
        return (long) dr * dr + (long) dg * dg + (long) db * db;
    }

    // ----- output -----------------------------------------------------------

    private static String previewName(String output) {
        int dot = output.lastIndexOf('.');
        return dot < 0 ? output + "_preview.png" : output.substring(0, dot) + "_preview.png";
    }

    /**
     * Writes a PNG at {@code scale}x using hard pixel edges. At scale 1 this is
     * the exact sprite the game loads; larger scales are for eyeballing it.
     */
    private static void writePng(int[] px, int w, int h, File file, int scale) throws IOException {
        int dw = w * scale;
        int dh = h * scale;
        BufferedImage out = new BufferedImage(dw, dh, BufferedImage.TYPE_INT_ARGB);
        int[] scaled = new int[dw * dh];
        for (int y = 0; y < dh; y++) {
            for (int x = 0; x < dw; x++) {
                scaled[y * dw + x] = px[(y / scale) * w + (x / scale)];
            }
        }
        out.setRGB(0, 0, dw, dh, scaled, 0, dw);
        File parent = file.getParentFile();
        if (parent != null) parent.mkdirs();
        if (!ImageIO.write(out, "png", file)) {
            throw new IOException("no PNG writer available");
        }
    }
}
