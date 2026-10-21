package chronorunner.gfx;

import java.util.Arrays;

/**
 * Builds a humanoid character sprite from a shared body template, so every
 * character gets the same anatomy and only the build, the clothing and the
 * palette change.
 *
 * <p>Proportions are measured off the reference art rather than guessed: the
 * head is about a quarter of the total height, the arms hang from a shoulder
 * line a little below the chin, and the legs are a bit under half. Given those
 * bands the template predicts the silhouette - head, neck, tapering torso, two
 * arms, hips, two legs, two feet - and the caller only chooses how wide each
 * band is and what colour it is.
 *
 * <p>The whole silhouette is outlined in a single pass at the end, so the
 * outline is always exactly one pixel thick and no limb can be left without
 * one. Art is authored facing right, like the rest of the sprite set.
 */
public final class Humanoid {

    /** Where the hair sits and how far it hangs. */
    public enum Hair { SHORT, SPIKY, BOWL, CAP, BALD }

    /** How much of the arm the garment covers. */
    public enum Sleeves { NONE, SHORT, LONG }

    /** What the character wears below the belt. */
    public enum Lower { SHORTS, TROUSERS }

    // ----- glyphs, resolved by the palette the caller supplies ---------------

    public static final char EMPTY = '.';
    public static final char OUTLINE = 'K';
    public static final char SKIN = 's';
    public static final char SKIN_SHADE = 'S';
    public static final char HAIR = 'h';
    public static final char HAIR_HI = 'H';
    public static final char CLOTH = 'b';
    public static final char CLOTH_HI = 'B';
    public static final char CLOTH_SHADE = 'n';
    public static final char LOWER = 'p';
    public static final char LOWER_HI = 'P';
    public static final char BOOT = 'w';
    public static final char SOLE = 'g';
    public static final char ACCENT = 'y';
    public static final char EYE = 'o';
    public static final char EYE_WHITE = 'e';

    /** Four-connected neighbours, used by the single outline pass. */
    private static final int[][] NEIGH = { {0, -1}, {0, 1}, {-1, 0}, {1, 0} };

    /**
     * One character's build and clothing. Defaults describe an average adult on
     * a 44x62 canvas, which at the game's hitbox size renders at 1x.
     */
    public static final class Spec {

        public int width = 44;
        public int height = 62;

        /** Rows the head occupies, measured from the top of the canvas. */
        public int headTop = 2;
        public int headHeight = 15;
        public int headWidth = 20;

        public int neckWidth = 8;

        /** Widest point of the torso, at the shoulder line. */
        public int shoulderWidth = 24;
        /** Narrowest point, just above the hips. */
        public int waistWidth = 21;
        public int hipWidth = 24;
        public int armWidth = 5;
        public int legWidth = 8;
        /** Transparent pixels between the two legs. */
        public int legGap = 3;

        public Hair hair = Hair.SHORT;
        public Sleeves sleeves = Sleeves.SHORT;
        public Lower lower = Lower.SHORTS;
        /** Draws a hood bunched behind the neck. */
        public boolean hood = false;
        /** 0 stands still; 1 swings the legs apart for a stride. */
        public int pose = 0;

        public int headLeft() {
            return (width - headWidth) / 2;
        }

        public int headBottom() {
            return headTop + headHeight - 1;
        }

        public int shoulderTop() {
            return headBottom() + 2;
        }

        public int hipTop() {
            return (int) (height * 0.62);
        }

        public int legTop() {
            return hipTop() + 4;
        }

        public int ankleTop() {
            return height - 6;
        }
    }

    private Humanoid() {
    }

    /** Renders the template to rows of palette keys, ready for {@link PixelSprite#of}. */
    public static String[] render(Spec spec) {
        int w = spec.width;
        int h = spec.height;
        boolean[] solid = new boolean[w * h];
        char[] ink = new char[w * h];
        Arrays.fill(ink, EMPTY);

        drawBody(spec, solid, ink, w, h);
        outline(solid, ink, w, h);
        drawFace(spec, solid, ink, w, h);
        drawGarmentDetails(spec, solid, ink, w, h);

        String[] rows = new String[h];
        for (int y = 0; y < h; y++) {
            rows[y] = new String(ink, y * w, w);
        }
        return rows;
    }

    // ----- body ------------------------------------------------------------

    private static void drawBody(Spec s, boolean[] solid, char[] ink, int w, int h) {
        int cx = w / 2;
        int headLeft = s.headLeft();
        // A stride swings the two legs apart, one forward and one back, which
        // reads as a walk without a second set of authored art. Only the legs
        // move: the arms already run close to the canvas edge on the broadest
        // build, so swinging them too would clip against it.
        int backShift = s.pose == 1 ? -2 : 0;
        int frontShift = s.pose == 1 ? 2 : 0;

        // Legs first, so the torso and hips paint over their tops.
        int legSpan = s.legWidth * 2 + s.legGap;
        int legLeft = cx - legSpan / 2;
        int[] legX = { legLeft + backShift, legLeft + s.legWidth + s.legGap + frontShift };
        for (int i = 0; i < 2; i++) {
            fill(solid, ink, legX[i], s.legTop(), s.legWidth, s.ankleTop() - s.legTop(),
                    s.lower == Lower.TROUSERS ? LOWER : SKIN, w, h);
        }

        // Feet: pushed forward, since the art faces right.
        for (int i = 0; i < 2; i++) {
            int depth = s.height - s.ankleTop();
            fill(solid, ink, legX[i], s.ankleTop(), s.legWidth, depth - 1, BOOT, w, h);
            fill(solid, ink, legX[i], s.height - 2, s.legWidth + 2, 1, SOLE, w, h);
        }

        // Hips: always the lower garment, never the top, since shorts and
        // trousers are a separate piece of clothing from the jersey or hoodie.
        fill(solid, ink, cx - s.hipWidth / 2, s.hipTop(), s.hipWidth, s.legTop() - s.hipTop(),
                LOWER, w, h);
        // Shorts stop mid-thigh; trousers already cover the whole leg because
        // the legs above were drawn in LOWER.
        if (s.lower == Lower.SHORTS) {
            int hem = Math.max(2, s.legWidth / 2);
            for (int x : legX) {
                fill(solid, ink, x, s.legTop(), s.legWidth, hem, LOWER, w, h);
            }
        }

        // Torso: shoulders taper to the waist, one row at a time. It stops one
        // row short of the hips so the hip block keeps its own width.
        int torsoTop = s.shoulderTop();
        int torsoBottom = s.hipTop() - 1;
        for (int y = torsoTop; y <= torsoBottom; y++) {
            double t = (y - torsoTop) / (double) Math.max(1, torsoBottom - torsoTop);
            int width = (int) Math.round(s.shoulderWidth + (s.waistWidth - s.shoulderWidth) * t);
            int left = cx - width / 2;
            fill(solid, ink, left, y, width, 1, CLOTH, w, h);
        }

        // Arms hang just outside the torso, hands at hip level. Both are held
        // one pixel clear of the shoulder line so the outline pass separates
        // them from the body; without that gap the arm and torso merge into a
        // single blob on whichever side happens to line up.
        int torsoLeft = cx - s.shoulderWidth / 2;
        int torsoRight = torsoLeft + s.shoulderWidth - 1;
        int armTop = torsoTop + 1;
        int armBottom = s.hipTop() - 1;
        int leftArm = torsoLeft - 1 - s.armWidth;
        int rightArm = torsoRight + 2;
        char lowerArm = s.sleeves == Sleeves.LONG ? CLOTH : SKIN;
        int cuff = armTop + (armBottom - armTop) * (s.sleeves == Sleeves.SHORT ? 2 : 3) / 5;
        for (int x : new int[] { leftArm, rightArm }) {
            fill(solid, ink, x + 1, armTop, s.armWidth - 2, armBottom - armTop + 1, CLOTH, w, h);
            fill(solid, ink, x, armTop + 1, s.armWidth, armBottom - armTop - 1, CLOTH, w, h);
            if (s.sleeves != Sleeves.LONG) {
                fill(solid, ink, x, cuff, s.armWidth, armBottom - cuff + 1, lowerArm, w, h);
            }
        }

        if (s.hood) {
            fill(solid, ink, cx - s.shoulderWidth / 2 + 1, torsoTop - 2, s.shoulderWidth - 2, 3,
                    CLOTH, w, h);
        }

        // Neck last so it sits behind the chin but over the collar.
        fill(solid, ink, cx - s.neckWidth / 2, s.headBottom() - 2, s.neckWidth, 5, SKIN, w, h);

        // Head: a rectangle with the corners cut, which reads as a rounded skull.
        fillRound(solid, ink, headLeft, s.headTop, s.headWidth, s.headHeight, SKIN, w, h);

        drawHair(s, solid, ink, w, h);
    }

    private static void drawHair(Spec s, boolean[] solid, char[] ink, int w, int h) {
        int left = s.headLeft();
        int top = s.headTop;
        switch (s.hair) {
            case BALD:
                break;
            case SPIKY:
                for (int i = 0; i < s.headWidth; i += 3) {
                    fill(solid, ink, left + i, top - 2, 2, 2, HAIR, w, h);
                }
                fill(solid, ink, left, top, s.headWidth, 5, HAIR, w, h);
                fill(solid, ink, left, top + 5, 2, 4, HAIR, w, h);
                fill(solid, ink, left + s.headWidth - 2, top + 5, 2, 4, HAIR, w, h);
                break;
            case BOWL:
                fill(solid, ink, left - 1, top + 1, s.headWidth + 2, 6, HAIR, w, h);
                fill(solid, ink, left - 1, top + 7, 3, 5, HAIR, w, h);
                fill(solid, ink, left + s.headWidth - 2, top + 7, 3, 5, HAIR, w, h);
                break;
            case CAP:
                fill(solid, ink, left, top, s.headWidth, 5, CLOTH, w, h);
                fill(solid, ink, left + s.headWidth, top + 3, 6, 2, CLOTH, w, h);
                fill(solid, ink, left, top + 5, s.headWidth, 1, CLOTH_SHADE, w, h);
                break;
            default:
                fill(solid, ink, left, top, s.headWidth, 5, HAIR, w, h);
                fill(solid, ink, left, top + 5, 2, 5, HAIR, w, h);
                fill(solid, ink, left + s.headWidth - 2, top + 5, 2, 5, HAIR, w, h);
                break;
        }
        // A highlight along the crown keeps a flat mass of hair from reading as
        // a hole in the silhouette.
        if (s.hair != Hair.BALD) {
            fill(solid, ink, left + 4, top + 1, Math.max(2, s.headWidth - 10), 1, HAIR_HI, w, h);
        }
    }

    private static void drawFace(Spec s, boolean[] solid, char[] ink, int w, int h) {
        int left = s.headLeft();
        int eyeRow = s.headTop + s.headHeight / 2 - 1;
        int eyeY = eyeRow + (s.hair == Hair.BALD ? 1 : 0);
        // Eyes sit a third of the way in from each edge so the face has a front.
        put(solid, ink, left + 4, eyeY, EYE_WHITE, w, h);
        put(solid, ink, left + 5, eyeY, EYE, w, h);
        put(solid, ink, left + 6, eyeY, EYE, w, h);
        put(solid, ink, left + s.headWidth - 5, eyeY, EYE, w, h);
        put(solid, ink, left + s.headWidth - 6, eyeY, EYE, w, h);
        put(solid, ink, left + s.headWidth - 7, eyeY, EYE_WHITE, w, h);

        int mouthY = s.headBottom() - 2;
        int mouth = left + Math.max(3, s.headWidth / 2 - 4);
        put(solid, ink, mouth, mouthY, SKIN_SHADE, w, h);
        put(solid, ink, mouth + 1, mouthY, EYE, w, h);
        put(solid, ink, mouth + 2, mouthY, EYE, w, h);
        put(solid, ink, mouth + 3, mouthY, SKIN_SHADE, w, h);
        put(solid, ink, left + 3, s.headBottom() - 1, SKIN_SHADE, w, h);
        put(solid, ink, left + s.headWidth - 4, s.headBottom() - 1, SKIN_SHADE, w, h);
    }

    private static void drawGarmentDetails(Spec s, boolean[] solid, char[] ink, int w, int h) {
        int cx = w / 2;
        // Collar.
        fillIfInk(solid, ink, cx - s.neckWidth / 2 - 2, s.shoulderTop(), s.neckWidth + 4, 1,
                CLOTH_SHADE, w, h);
        // Highlight down the chest and along the shoulders.
        fillIfInk(solid, ink, cx - 1, s.shoulderTop() + 2, 2, s.hipTop() - s.shoulderTop() - 1,
                CLOTH_HI, w, h);
        fillIfInk(solid, ink, cx - s.shoulderWidth / 2 + 2, s.shoulderTop() + 1,
                s.shoulderWidth - 4, 1, CLOTH_HI, w, h);
        // Belt: neutral, so it reads against any kit colour.
        fillIfInk(solid, ink, cx - s.hipWidth / 2, s.hipTop(), s.hipWidth, 1, SOLE, w, h);
        fillIfInk(solid, ink, cx - 1, s.hipTop(), 2, 1, ACCENT, w, h);
    }

    // ----- rasteriser ------------------------------------------------------

    /** Writes cells, marking them solid. Existing ink is overwritten. */
    private static void fill(boolean[] solid, char[] ink, int x, int y, int bw, int bh, char c,
                             int w, int h) {
        for (int j = y; j < y + bh; j++) {
            if (j < 0 || j >= h) continue;
            for (int i = x; i < x + bw; i++) {
                if (i < 0 || i >= w) continue;
                int idx = j * w + i;
                solid[idx] = true;
                ink[idx] = c;
            }
        }
    }

    /** Fills a rectangle with its four corners cut, so a head reads as rounded. */
    private static void fillRound(boolean[] solid, char[] ink, int x, int y, int bw, int bh, char c,
                                  int w, int h) {
        int r = Math.max(1, Math.min(bw, bh) / 4);
        for (int j = 0; j < bh; j++) {
            for (int i = 0; i < bw; i++) {
                int dx = i < r ? r - i : (i >= bw - r ? i - (bw - r - 1) : 0);
                int dy = j < r ? r - j : (j >= bh - r ? j - (bh - r - 1) : 0);
                if (dx > 0 && dy > 0 && dx + dy > r + 1) continue;
                fill(solid, ink, x + i, y + j, 1, 1, c, w, h);
            }
        }
    }

    /** Writes one cell, but only where the body is already solid. */
    private static void put(boolean[] solid, char[] ink, int x, int y, char c, int w, int h) {
        if (x < 0 || y < 0 || x >= w || y >= h) return;
        int idx = y * w + x;
        if (solid[idx]) ink[idx] = c;
    }

    /** Recolours cells that are already solid, leaving the outline untouched. */
    private static void fillIfInk(boolean[] solid, char[] ink, int x, int y, int bw, int bh, char c,
                                  int w, int h) {
        for (int j = y; j < y + bh; j++) {
            if (j < 0 || j >= h) continue;
            for (int i = x; i < x + bw; i++) {
                if (i < 0 || i >= w) continue;
                int idx = j * w + i;
                if (solid[idx]) ink[idx] = c;
            }
        }
    }

    /**
     * Rings the silhouette in one pass: every empty cell touching a solid one
     * becomes outline. Doing it here rather than per-limb is what guarantees the
     * outline is a uniform one pixel and never doubles up where limbs meet.
     */
    private static void outline(boolean[] solid, char[] ink, int w, int h) {
        boolean[] edge = new boolean[w * h];
        for (int y = 0; y < h; y++) {
            for (int x = 0; x < w; x++) {
                int idx = y * w + x;
                if (solid[idx]) continue;
                for (int[] d : NEIGH) {
                    int nx = x + d[0];
                    int ny = y + d[1];
                    if (nx < 0 || ny < 0 || nx >= w || ny >= h) continue;
                    if (solid[ny * w + nx]) {
                        edge[idx] = true;
                        break;
                    }
                }
            }
        }
        for (int i = 0; i < edge.length; i++) {
            if (edge[i]) ink[i] = OUTLINE;
        }
    }
}
