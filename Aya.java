import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;

/**
 * Aya — the Archer character.
 *
 * Sprite folder: images/aya/
 * All PNGs are pre-processed: transparent background, padded to the same
 * aspect ratio as SPRITE_W × SPRITE_H so every animation looks the same size.
 *
 * Backward walk: walkBackward_f01.png … walkBackward_f28.png
 *   (cut from 4-col × 7-row sheet, padded to 64×80 before saving)
 */
public class Aya extends Player {

    private static final String DIR = "images/aya/";

    public Aya(GameScreen gp, KeyHandler keyH) {
        super(gp, keyH);
    }

    @Override
    protected void loadSprites() {

        // ── Standing sprites ──────────────────────────────────────────────────────
        standingForward           = loadDirect(DIR + "standingForward.png");
        standingBackward          = tryLoad(DIR + "standingBackward.png",          standingForward);
        standingLeft              = tryLoad(DIR + "standingLeft.png",              standingForward);
        standingRight             = tryLoad(DIR + "standingRight.png",             standingForward);
        standingDiagonalUpLeft    = tryLoad(DIR + "standingDiagonalUpLeft.png",    standingForward);
        standingDiagonalUpRight   = tryLoad(DIR + "standingDiagonalUpRight.png",   standingForward);
        standingDiagonalDownLeft  = tryLoad(DIR + "standingDiagonalDownLeft.png",  standingForward);
        standingDiagonalDownRight = tryLoad(DIR + "standingDiagonalDownRight.png", standingForward);

        // ── Walking forward (21 frames) ───────────────────────────────────────────
        walkingForwardFrames = new BufferedImage[21];
        for (int i = 1; i <= 21; i++)
            walkingForwardFrames[i - 1] = loadDirect(
                String.format(DIR + "walkForward_f%02d.png", i));

        // ── Walking right (28 frames) ─────────────────────────────────────────────
        walkingRightFrames = new BufferedImage[28];
        for (int i = 1; i <= 28; i++)
            walkingRightFrames[i - 1] = loadDirect(
                String.format(DIR + "walkRight_f%02d.png", i));

        // ── Walking left (28 frames) ──────────────────────────────────────────────
        walkingLeftFrames = new BufferedImage[28];
        for (int i = 1; i <= 28; i++)
            walkingLeftFrames[i - 1] = loadDirect(
                String.format(DIR + "walkLeft_f%02d.png", i));

        // ── Walking backward (28 frames) ──────────────────────────────────────────
        // PNGs are pre-padded to SPRITE_W × SPRITE_H aspect ratio so the character
        // renders at the same visual size as all other animations.
        walkingBackwardFrames = new BufferedImage[28];
        for (int i = 1; i <= 28; i++)
            walkingBackwardFrames[i - 1] = loadDirect(
                String.format(DIR + "walkBackward_f%02d.png", i));
    }

    /**
     * Loads a pre-processed PNG and scales it to exactly SPRITE_W × SPRITE_H.
     * Because the PNGs are already padded to the correct aspect ratio, a simple
     * stretch-to-fill produces no distortion and keeps all animations the same size.
     */
    private BufferedImage loadDirect(String path) {
        BufferedImage out = new BufferedImage(SPRITE_W, SPRITE_H, BufferedImage.TYPE_INT_ARGB);
        try {
            BufferedImage src = ImageIO.read(new File(path));
            if (src != null) {
                // Convert to ARGB
                BufferedImage argb = new BufferedImage(
                        src.getWidth(), src.getHeight(), BufferedImage.TYPE_INT_ARGB);
                Graphics2D tmp = argb.createGraphics();
                tmp.drawImage(src, 0, 0, null);
                tmp.dispose();

                // Strip near-white / lavender-dot background
                stripBackground(argb);

                // Stretch to fill the canvas — no letterboxing needed
                Graphics2D g2 = out.createGraphics();
                g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION,
                                    RenderingHints.VALUE_INTERPOLATION_BILINEAR);
                g2.drawImage(argb, 0, 0, SPRITE_W, SPRITE_H, null);
                g2.dispose();
            }
        } catch (IOException e) {
            System.err.println("Aya: could not load " + path);
        }
        return out;
    }

    /**
     * Flood-fills from every edge pixel and makes near-white background pixels
     * fully transparent. Threshold 210 catches pure white and aliasing fringe.
     */
    private void stripBackground(BufferedImage img) {
        int w = img.getWidth(), h = img.getHeight();
        boolean[][] visited = new boolean[w][h];
        java.util.Queue<int[]> queue = new java.util.LinkedList<>();

        for (int x = 0; x < w; x++) {
            enqueueWhite(img, x, 0,     visited, queue);
            enqueueWhite(img, x, h - 1, visited, queue);
        }
        for (int y = 1; y < h - 1; y++) {
            enqueueWhite(img, 0,     y, visited, queue);
            enqueueWhite(img, w - 1, y, visited, queue);
        }

        while (!queue.isEmpty()) {
            int[] px = queue.poll();
            int cx = px[0], cy = px[1];
            img.setRGB(cx, cy, 0x00000000);
            int[][] nb = {{cx-1,cy},{cx+1,cy},{cx,cy-1},{cx,cy+1}};
            for (int[] n : nb) enqueueWhite(img, n[0], n[1], visited, queue);
        }
    }

    private void enqueueWhite(BufferedImage img, int x, int y,
                               boolean[][] visited, java.util.Queue<int[]> queue) {
        if (x < 0 || y < 0 || x >= img.getWidth() || y >= img.getHeight()) return;
        if (visited[x][y]) return;
        visited[x][y] = true;
        int argb = img.getRGB(x, y);
        int a = (argb >> 24) & 0xFF;
        int r = (argb >> 16) & 0xFF;
        int g = (argb >>  8) & 0xFF;
        int b =  argb        & 0xFF;
        if (a == 0 || (r > 200 && g > 200 && b > 200))
            queue.add(new int[]{x, y});
    }

    private BufferedImage tryLoad(String path, BufferedImage fallback) {
        try {
            if (new File(path).exists()) return loadDirect(path);
        } catch (Exception e) { /* ignore */ }
        return fallback;
    }
}