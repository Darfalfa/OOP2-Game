import javax.imageio.ImageIO;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.File;

public class MapBackground {

    private BufferedImage mapImage;

    public int worldWidth;
    public int worldHeight;

    public void loadMap(String path) {
        try {
            mapImage = ImageIO.read(new File(path));

            worldWidth = mapImage.getWidth();
            worldHeight = mapImage.getHeight();

            System.out.println("Map loaded: " + worldWidth + " x " + worldHeight);

        } catch (Exception e) {
            System.out.println("Could not load " + path);
            e.printStackTrace();
        }
    }

    public void draw(Graphics2D g2, Camera camera) {
        if (mapImage == null) return;

        g2.drawImage(
                mapImage,
                -camera.offsetX(),
                -camera.offsetY(),
                null
        );
    }
}