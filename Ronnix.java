import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;

/**
 * Ronnix — the Fighter character.
 *
 * Sprite folder: images/ronnix/
 *
 * walkForward_f01.png  … walkForward_f28.png   — pre-sliced, already transparent
 * walkBackward_f01.png … walkBackward_f28.png  — sliced from 4-col × 7-row sheet
 * walkLeft_f01.png     … walkLeft_f28.png      — sliced from 4-col × 7-row sheet
 * walkRight_f01.png    … walkRight_f28.png     — sliced from 4-col × 7-row sheet
 *
 * Diagonal directions fall back to forward frames until dedicated sheets are added.
 */
public class Ronnix extends Player {

    private static final String DIR = "images/ronnix/";

    public Ronnix(GameScreen gp, KeyHandler keyH) {
        super(gp, keyH);
        // Diagonal sheets have 24 frames vs 28 for forward walk.
        // Delay of 5 keeps the cycle duration the same as forward (28 * 4 = 112 ticks ≈ 24 * 5 = 120).
        diagDownLeftAnimDelay  = 5;
        diagDownRightAnimDelay = 5;
    }

    @Override
    protected void loadSprites() {

        // ── Walking forward — 28 pre-sliced transparent frames ────────────────────
        walkingForwardFrames = new BufferedImage[28];
        for (int i = 1; i <= 28; i++) {
            walkingForwardFrames[i - 1] = loadDirect(
                String.format(DIR + "walkForward_f%02d.png", i));
        }

        // ── Standing — dedicated idle sprites for all 8 directions ───────────────
        standingForward           = loadDirect(DIR + "standingForward.png");
        standingBackward          = loadDirect(DIR + "standingBackward.png");
        standingLeft              = loadDirect(DIR + "standingLeft.png");
        standingRight             = loadDirect(DIR + "standingRight.png");
        standingDiagonalUpLeft    = loadDirect(DIR + "standingDiagonalUpLeft.png");
        standingDiagonalUpRight   = loadDirect(DIR + "standingDiagonalUpRight.png");
        standingDiagonalDownLeft  = loadDirect(DIR + "standingDiagonalDownLeft.png");
        standingDiagonalDownRight = loadDirect(DIR + "standingDiagonalDownRight.png");

        // ── Walking backward — 28 dedicated frames (sliced from sprite sheet) ─────
        walkingBackwardFrames = new BufferedImage[28];
        for (int i = 1; i <= 28; i++) {
            walkingBackwardFrames[i - 1] = loadDirect(
                String.format(DIR + "walkBackward_f%02d.png", i));
        }

        // ── Walking left — 28 dedicated frames (sliced from 4-col × 7-row sheet) ──
        // Uses loadWithBgStrip() because the source PNGs have a white/dotted background.
        walkingLeftFrames = new BufferedImage[28];
        for (int i = 1; i <= 28; i++) {
            walkingLeftFrames[i - 1] = loadWithBgStrip(
                String.format(DIR + "walkLeft_f%02d.png", i),
                walkingForwardFrames[(i - 1) % walkingForwardFrames.length]);
        }

        // ── Walking right — 28 dedicated frames (sliced from 4-col × 7-row sheet) ──
        // Uses loadWithBgStrip() because the source PNGs have a white/dotted background.
        walkingRightFrames = new BufferedImage[28];
        for (int i = 1; i <= 28; i++) {
            walkingRightFrames[i - 1] = loadWithBgStrip(
                String.format(DIR + "walkRight_f%02d.png", i),
                walkingForwardFrames[(i - 1) % walkingForwardFrames.length]);
        }

        // ── Diagonal down-left — 24 dedicated frames ──────────────────────────────
        walkingDiagonalDownLeftFrames = new BufferedImage[24];
        for (int i = 1; i <= 24; i++) {
            walkingDiagonalDownLeftFrames[i - 1] = tryLoad(
                String.format(DIR + "walkDiagonalDownLeft_f%02d.png", i),
                walkingForwardFrames[(i - 1) % walkingForwardFrames.length]);
        }

        // ── Diagonal down-right — 24 dedicated frames ─────────────────────────────
        walkingDiagonalDownRightFrames = new BufferedImage[24];
        for (int i = 1; i <= 24; i++) {
            walkingDiagonalDownRightFrames[i - 1] = tryLoad(
                String.format(DIR + "walkDiagonalDownRight_f%02d.png", i),
                walkingForwardFrames[(i - 1) % walkingForwardFrames.length]);
        }
    }

    /**
     * Loads a pre-processed transparent PNG and scales it to SPRITE_W × SPRITE_H.
     * Returns a blank transparent sprite on failure so the game never crashes.
     */
    private BufferedImage loadDirect(String path) {
        BufferedImage out = new BufferedImage(SPRITE_W, SPRITE_H, BufferedImage.TYPE_INT_ARGB);
        try {
            BufferedImage src = ImageIO.read(new File(path));
            if (src != null) {
                Graphics2D g2 = out.createGraphics();
                g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION,
                                    RenderingHints.VALUE_INTERPOLATION_BILINEAR);
                g2.drawImage(src, 0, 0, SPRITE_W, SPRITE_H, null);
                g2.dispose();
            }
        } catch (IOException e) {
            System.err.println("Ronnix: could not load " + path);
        }
        return out;
    }

    /**
     * Loads a PNG that has a white/near-white background, strips it via edge
     * flood-fill (preserving all character colours), then scales to SPRITE_W × SPRITE_H.
     * Falls back to the provided fallback image if the file is missing.
     */
    private BufferedImage loadWithBgStrip(String path, BufferedImage fallback) {
        File f = new File(path);
        if (!f.exists()) return fallback;

        BufferedImage out = new BufferedImage(SPRITE_W, SPRITE_H, BufferedImage.TYPE_INT_ARGB);
        try {
            BufferedImage src = ImageIO.read(f);
            if (src == null) return fallback;

            // Convert to ARGB so we can manipulate alpha
            BufferedImage argb = new BufferedImage(
                    src.getWidth(), src.getHeight(), BufferedImage.TYPE_INT_ARGB);
            Graphics2D tmp = argb.createGraphics();
            tmp.drawImage(src, 0, 0, null);
            tmp.dispose();

            // Strip white/near-white/dotted background without touching character colours
            stripBackground(argb);

            // Scale to sprite dimensions
            Graphics2D g2 = out.createGraphics();
            g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION,
                                RenderingHints.VALUE_INTERPOLATION_BILINEAR);
            g2.drawImage(argb, 0, 0, SPRITE_W, SPRITE_H, null);
            g2.dispose();

        } catch (IOException e) {
            System.err.println("Ronnix: could not load " + path);
            return fallback;
        }
        return out;
    }

    /**
     * Flood-fills from every edge pixel and makes background pixels fully transparent.
     *
     * A pixel qualifies as background only if it is:
     *   - light (all channels > 190), AND
     *   - near-neutral (no channel differs from another by more than 30)
     *
     * This safely strips the white/gray dotted background while leaving warm
     * skin-tone highlights and purple armour highlights completely intact.
     */
    private void stripBackground(BufferedImage img) {
        int w = img.getWidth(), h = img.getHeight();
        boolean[][] visited = new boolean[w][h];
        java.util.Queue<int[]> queue = new java.util.LinkedList<>();

        for (int x = 0; x < w; x++) {
            enqueueBackground(img, x, 0,     visited, queue);
            enqueueBackground(img, x, h - 1, visited, queue);
        }
        for (int y = 1; y < h - 1; y++) {
            enqueueBackground(img, 0,     y, visited, queue);
            enqueueBackground(img, w - 1, y, visited, queue);
        }

        while (!queue.isEmpty()) {
            int[] px = queue.poll();
            int cx = px[0], cy = px[1];
            img.setRGB(cx, cy, 0x00000000);
            int[][] nb = {{cx-1,cy},{cx+1,cy},{cx,cy-1},{cx,cy+1}};
            for (int[] n : nb) enqueueBackground(img, n[0], n[1], visited, queue);
        }
    }

    private void enqueueBackground(BufferedImage img, int x, int y,
                                    boolean[][] visited, java.util.Queue<int[]> queue) {
        if (x < 0 || y < 0 || x >= img.getWidth() || y >= img.getHeight()) return;
        if (visited[x][y]) return;
        visited[x][y] = true;
        int argb = img.getRGB(x, y);
        int a = (argb >> 24) & 0xFF;
        int r = (argb >> 16) & 0xFF;
        int g = (argb >>  8) & 0xFF;
        int b =  argb        & 0xFF;

        // Already transparent — treat as background so the fill can pass through
        if (a == 0) { queue.add(new int[]{x, y}); return; }

        // Light + near-neutral = background dot/white pixel
        boolean isLight   = r > 190 && g > 190 && b > 190;
        boolean isNeutral = Math.abs(r - g) < 30 && Math.abs(r - b) < 30 && Math.abs(g - b) < 30;
        if (isLight && isNeutral)
            queue.add(new int[]{x, y});
    }

    /**
     * Tries to load a pre-processed PNG; returns fallback if the file is missing.
     */
    private BufferedImage tryLoad(String path, BufferedImage fallback) {
        try {
            if (new File(path).exists()) return loadDirect(path);
        } catch (Exception e) { /* ignore */ }
        return fallback;
    }
}