import javax.imageio.ImageIO;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.File;

/**
 * WorldBackground
 * ---------------
 * Tiles backgroundGame.png across the entire game world.
 * Only draws tiles that are currently on screen (camera culling).
 */
public class WorldBackground {

    private BufferedImage tile;

    /**
     * How large one copy of the background image is rendered in world space.
     * This matches the original image's natural pixel size (960 × 640)
     * so it looks sharp and not stretched.
     */
    private static final int TILE_W = 960;
    private static final int TILE_H = 640;

    public WorldBackground() {
        try {
            tile = ImageIO.read(new File("images/yourNewMap.png"));
        } catch (Exception e) {
            System.err.println("Could not load yourNewMap.png");
            e.printStackTrace();
        }
    }

    /**
     * Draw the tiled background.
     *
     * @param g2     Graphics context
     * @param cam    active Camera
     * @param worldW total world width  in pixels
     * @param worldH total world height in pixels
     */
    public void draw(Graphics2D g2, Camera cam, int worldW, int worldH) {
        if (tile == null) return;

        int camX = cam.offsetX();
        int camY = cam.offsetY();

        // First and last tile indices visible on screen
        int colStart = camX / TILE_W;
        int rowStart = camY / TILE_H;
        int colEnd   = (camX + cam.screenWidth  - 1) / TILE_W;
        int rowEnd   = (camY + cam.screenHeight - 1) / TILE_H;

        for (int row = rowStart; row <= rowEnd; row++) {
            for (int col = colStart; col <= colEnd; col++) {

                int worldX = col * TILE_W;
                int worldY = row * TILE_H;

                // Don't draw tiles outside the world
                if (worldX >= worldW || worldY >= worldH) continue;

                g2.drawImage(tile,
                             worldX - camX,   // screen X
                             worldY - camY,   // screen Y
                             TILE_W, TILE_H,
                             null);
            }
        }
    }

    // Expose tile size so GamePanel can calculate world dimensions
    public static int tileW() { return TILE_W; }
    public static int tileH() { return TILE_H; }
}