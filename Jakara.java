import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;

/**
 * Jakara — the Arcane Mage character.
 *
 * Sprite folder: images/jakara/
 *
 * walkForward_f01.png   … walkForward_f28.png   — sliced from 4-col × 7-row sheet,
 *                                                  black background, stripped at load time.
 * walkRight_f01.png     … walkRight_f28.png     — sliced from 4-col × 7-row sheet,
 *                                                  pre-processed transparent PNGs.
 *
 * Other walk directions fall back to forward frames until dedicated sheets are added.
 */
public class Jakara extends Player {

    private static final String DIR = "images/jakara/";

    public Jakara(GameScreen gp, KeyHandler keyH) {
        super(gp, keyH);
    }

    @Override
    protected void loadSprites() {

        // ── Standing — all 8 directions ───────────────────────────────────────────
        standingForward           = loadDirect(DIR + "standingForward.png");
        standingBackward          = loadDirect(DIR + "standingBackward.png");
        standingLeft              = loadDirect(DIR + "standingLeft.png");
        standingRight             = loadDirect(DIR + "standingRight.png");
        standingDiagonalUpLeft    = loadDirect(DIR + "standingDiagonalUpLeft.png");
        standingDiagonalUpRight   = loadDirect(DIR + "standingDiagonalUpRight.png");
        standingDiagonalDownLeft  = loadDirect(DIR + "standingDiagonalDownLeft.png");
        standingDiagonalDownRight = loadDirect(DIR + "standingDiagonalDownRight.png");

        // ── Walking forward — 28 frames (sliced from 4-col × 7-row black-bg sheet) ─
        walkingForwardFrames = new BufferedImage[28];
        for (int i = 1; i <= 28; i++)
            walkingForwardFrames[i - 1] = loadDirect(
                String.format(DIR + "walkForward_f%02d.png", i));

        // ── Walking right — 28 dedicated frames (pre-processed transparent PNGs) ──
        // Sliced from the 4-col × 7-row walking-right sprite sheet.
        // Place walkRight_f01.png … walkRight_f28.png inside images/jakara/.
        walkingRightFrames = new BufferedImage[28];
        for (int i = 1; i <= 28; i++)
            walkingRightFrames[i - 1] = loadDirect(
                String.format(DIR + "walkRight_f%02d.png", i));

        // ── Walking left — 28 dedicated frames (black-bg sheet, stripped at load) ──
        walkingLeftFrames = new BufferedImage[28];
        for (int i = 1; i <= 28; i++)
            walkingLeftFrames[i - 1] = loadDirect(
                String.format(DIR + "walkLeft_f%02d.png", i));

        // ── Walking backward — 28 dedicated frames (black-bg sheet, stripped at load) ──
        walkingBackwardFrames = new BufferedImage[28];
        for (int i = 1; i <= 28; i++)
            walkingBackwardFrames[i - 1] = loadDirect(
                String.format(DIR + "walkBackward_f%02d.png", i));
    }

    /**
     * Loads a PNG, strips its background via edge flood-fill, then scales to
     * SPRITE_W × SPRITE_H. Supports both white/near-white backgrounds (old sprites),
     * pure black backgrounds (new walkForward sheet), and pre-transparent PNGs
     * (walkRight frames — the flood-fill simply finds no background to remove).
     * Returns a blank transparent sprite on failure so the game never crashes.
     */
    private BufferedImage loadDirect(String path) {
        BufferedImage out = new BufferedImage(SPRITE_W, SPRITE_H, BufferedImage.TYPE_INT_ARGB);
        try {
            BufferedImage src = ImageIO.read(new File(path));
            if (src != null) {
                // Convert to ARGB so we can manipulate alpha
                BufferedImage argb = new BufferedImage(
                        src.getWidth(), src.getHeight(), BufferedImage.TYPE_INT_ARGB);
                Graphics2D tmp = argb.createGraphics();
                tmp.drawImage(src, 0, 0, null);
                tmp.dispose();

                // Auto-detect background type and strip accordingly.
                // Pre-transparent PNGs (walkRight frames) are handled gracefully:
                // their edge pixels are already alpha=0, so the flood-fill exits
                // immediately without touching any character pixels.
                stripBackground(argb);

                // Scale to sprite dimensions
                Graphics2D g2 = out.createGraphics();
                g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION,
                                    RenderingHints.VALUE_INTERPOLATION_BILINEAR);
                g2.drawImage(argb, 0, 0, SPRITE_W, SPRITE_H, null);
                g2.dispose();
            }
        } catch (IOException e) {
            System.err.println("Jakara: could not load " + path);
        }
        return out;
    }

    /** Tries to load a PNG (with background stripping); returns fallback if the file is missing. */
    private BufferedImage tryLoad(String path, BufferedImage fallback) {
        try {
            if (new File(path).exists()) return loadDirect(path);
        } catch (Exception e) { /* ignore */ }
        return fallback;
    }

    /**
     * Detects whether the image has a white or black background by sampling the
     * four corners, then flood-fills from every edge pixel to make the background
     * fully transparent.
     *
     * White/near-white bg: pixels that are light (all channels > 190) AND near-neutral
     *   (channels within 30 of each other) — preserves warm skin tones and coloured armour.
     *
     * Black/near-black bg: pixels where all channels < 30 — preserves dark armour
     *   shadows which have a colour tint the pure black background does not.
     *
     * Pre-transparent bg (walkRight PNGs): edge pixels have alpha=0, so they are
     *   enqueued and cleared immediately — no opaque character pixels are touched.
     */
    private void stripBackground(BufferedImage img) {
        int w = img.getWidth(), h = img.getHeight();

        // Sample the four corners to decide background type
        int cornerBrightness = 0;
        int[] corners = {
            img.getRGB(0, 0), img.getRGB(w - 1, 0),
            img.getRGB(0, h - 1), img.getRGB(w - 1, h - 1)
        };
        for (int c : corners) {
            cornerBrightness += ((c >> 16) & 0xFF);
            cornerBrightness += ((c >>  8) & 0xFF);
            cornerBrightness +=  (c        & 0xFF);
        }
        boolean darkBackground = (cornerBrightness / 12) < 80; // avg channel value < 80 → dark bg

        boolean[][] visited = new boolean[w][h];
        java.util.Queue<int[]> queue = new java.util.LinkedList<>();

        for (int x = 0; x < w; x++) {
            enqueue(img, x, 0,     visited, queue, darkBackground);
            enqueue(img, x, h - 1, visited, queue, darkBackground);
        }
        for (int y = 1; y < h - 1; y++) {
            enqueue(img, 0,     y, visited, queue, darkBackground);
            enqueue(img, w - 1, y, visited, queue, darkBackground);
        }

        while (!queue.isEmpty()) {
            int[] px = queue.poll();
            int cx = px[0], cy = px[1];
            img.setRGB(cx, cy, 0x00000000);
            int[][] nb = {{cx-1,cy},{cx+1,cy},{cx,cy-1},{cx,cy+1}};
            for (int[] n : nb) enqueue(img, n[0], n[1], visited, queue, darkBackground);
        }
    }

    private void enqueue(BufferedImage img, int x, int y,
                         boolean[][] visited, java.util.Queue<int[]> queue,
                         boolean darkBackground) {
        if (x < 0 || y < 0 || x >= img.getWidth() || y >= img.getHeight()) return;
        if (visited[x][y]) return;
        visited[x][y] = true;
        int argb = img.getRGB(x, y);
        int a = (argb >> 24) & 0xFF;
        int r = (argb >> 16) & 0xFF;
        int g = (argb >>  8) & 0xFF;
        int b =  argb        & 0xFF;

        // Already transparent — treat as background so the fill can pass through.
        // This is the path taken by pre-transparent walkRight PNGs.
        if (a == 0) { queue.add(new int[]{x, y}); return; }

        boolean isBg;
        if (darkBackground) {
            // Pure black background — all channels near zero
            isBg = r < 30 && g < 30 && b < 30;
        } else {
            // White/dotted background — light AND near-neutral
            boolean isLight   = r > 190 && g > 190 && b > 190;
            boolean isNeutral = Math.abs(r - g) < 30
                             && Math.abs(r - b) < 30
                             && Math.abs(g - b) < 30;
            isBg = isLight && isNeutral;
        }

        if (isBg) queue.add(new int[]{x, y});
    }
}