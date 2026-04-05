import javax.imageio.ImageIO;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;

public class TileManager {

    GameScreen gs;
    public Tile[] tile;
    public int[][] mapTileNum;
    boolean drawPath = false;

    ArrayList<String> fileNames = new ArrayList<>();
    ArrayList<String> collisionStatus = new ArrayList<>();

    public TileManager(GameScreen gs) {
        this.gs = gs;

        loadTileData();

        int tileArraySize = Math.max(10, fileNames.size());
        tile = new Tile[tileArraySize];
        mapTileNum = new int[gs.maxWorldCol][gs.maxWorldRow];

        getTileImage();
        loadMap("/maps/map.txt");
    }

    private void loadTileData() {
        try (InputStream is = getClass().getResourceAsStream("/maps/tiledata.txt")) {

            if (is == null) {
                System.out.println("Could not find /maps/tiledata.txt");
                createFallbackTiles();
                return;
            }

            try (BufferedReader br = new BufferedReader(new InputStreamReader(is))) {
                String line;

                while ((line = br.readLine()) != null) {
                    line = line.trim();

                    if (line.isEmpty()) {
                        continue;
                    }

                    fileNames.add(line);

                    String collisionLine = br.readLine();
                    if (collisionLine == null) {
                        collisionStatus.add("false");
                    } else {
                        collisionStatus.add(collisionLine.trim());
                    }
                }
            }

        } catch (IOException e) {
            e.printStackTrace();
            createFallbackTiles();
        }
    }

    public void getTileImage() {
        for (int i = 0; i < fileNames.size(); i++) {
            String fileName = fileNames.get(i);
            boolean collision = collisionStatus.get(i).equalsIgnoreCase("true");
            setup(i, fileName, collision);
        }
    }

    private void setup(int i, String fileName, boolean collision) {
        try {
            tile[i] = new Tile();
            tile[i].image = loadTile(fileName);
            tile[i].collision = collision;
        } catch (IOException e) {
            System.out.println("Could not load tile: " + fileName);
            tile[i] = new Tile();
            tile[i].image = solidTile(Color.MAGENTA);
            tile[i].collision = collision;
        }
    }

    private BufferedImage loadTile(String fileName) throws IOException {
        BufferedImage raw = loadTileAsset(fileName);
        if (raw != null) {
            return scaleTile(raw);
        }

        String alternateFileName = getAlternateExtension(fileName);
        if (alternateFileName != null) {
            raw = loadTileAsset(alternateFileName);
            if (raw != null) {
                return scaleTile(raw);
            }
        }

        throw new IOException("Missing tile file: " + fileName + (alternateFileName != null ? " or " + alternateFileName : ""));
    }

    private BufferedImage loadTileAsset(String fileName) throws IOException {
        String resourcePath = "/tiles/world1/" + fileName;

        try (InputStream is = getClass().getResourceAsStream(resourcePath)) {
            if (is != null) {
                BufferedImage raw = ImageIO.read(is);
                if (raw == null) {
                    throw new IOException("Could not decode resource image: " + resourcePath);
                }
                return raw;
            }
        }

        File f = new File("tiles/world1", fileName);
        if (f.exists()) {
            BufferedImage raw = ImageIO.read(f);
            if (raw == null) {
                throw new IOException("Could not decode image: " + f.getPath());
            }
            return raw;
        }

        return null;
    }

    private String getAlternateExtension(String fileName) {
        if (fileName.toLowerCase().endsWith(".png")) {
            return fileName.substring(0, fileName.length() - 4) + ".jpg";
        }
        if (fileName.toLowerCase().endsWith(".jpg")) {
            return fileName.substring(0, fileName.length() - 4) + ".png";
        }
        return null;
    }

    private BufferedImage scaleTile(BufferedImage raw) {
        BufferedImage scaled = new BufferedImage(gs.tileSize, gs.tileSize, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2 = scaled.createGraphics();
        g2.drawImage(raw, 0, 0, gs.tileSize, gs.tileSize, null);
        g2.dispose();
        return scaled;
    }

    private void createFallbackTiles() {
        for (int i = 0; i < tile.length; i++) {
            if (tile[i] == null) tile[i] = new Tile();
        }

        tile[0].image = solidTile(new Color(70, 140, 70));   // grass
        tile[1].image = solidTile(new Color(110, 110, 110)); // wall
        tile[2].image = solidTile(new Color(70, 110, 180));  // water
        tile[3].image = solidTile(new Color(120, 90, 60));   // earth
        tile[4].image = solidTile(new Color(40, 90, 40));    // tree
        tile[5].image = solidTile(new Color(194, 178, 128)); // sand

        tile[1].collision = true;
        tile[2].collision = true;
        tile[4].collision = true;
    }

    private BufferedImage solidTile(Color color) {
        BufferedImage img = new BufferedImage(gs.tileSize, gs.tileSize, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2 = img.createGraphics();
        g2.setColor(color);
        g2.fillRect(0, 0, gs.tileSize, gs.tileSize);
        g2.dispose();
        return img;
    }

    public void loadMap(String filePath) {
        try (InputStream is = getClass().getResourceAsStream(filePath)) {

            if (is == null) {
                System.out.println("Could not find map file: " + filePath);
                loadMockMap();
                return;
            }

            try (BufferedReader br = new BufferedReader(new InputStreamReader(is))) {
                int row = 0;

                while (row < gs.maxWorldRow) {
                    String line = br.readLine();

                    if (line == null) {
                        break;
                    }

                    line = line.trim();
                    if (line.isEmpty()) {
                        continue;
                    }

                    String[] numbers = line.split("\\s+");

                    for (int col = 0; col < gs.maxWorldCol && col < numbers.length; col++) {
                        mapTileNum[col][row] = Integer.parseInt(numbers[col]);
                    }

                    row++;
                }
            }

        } catch (Exception e) {
            e.printStackTrace();
            loadMockMap();
        }
    }

    private void loadMockMap() {
        for (int col = 0; col < gs.maxWorldCol; col++) {
            for (int row = 0; row < gs.maxWorldRow; row++) {

                if (row == 0 || col == 0 || row == gs.maxWorldRow - 1 || col == gs.maxWorldCol - 1) {
                    mapTileNum[col][row] = 1;
                } else {
                    mapTileNum[col][row] = 0;
                }
            }
        }

        for (int col = 8; col <= 11 && col < gs.maxWorldCol; col++) {
            for (int row = 6; row <= 8 && row < gs.maxWorldRow; row++) {
                mapTileNum[col][row] = 2;
            }
        }
    }

    public void draw(Graphics2D g2, Camera camera) {
        int col = 0;
        int row = 0;

        while (col < gs.maxWorldCol && row < gs.maxWorldRow) {
            int tileNum = mapTileNum[col][row];

            int worldX = col * gs.tileSize;
            int worldY = row * gs.tileSize;
            int screenX = worldX - camera.offsetX();
            int screenY = worldY - camera.offsetY();

            if (worldX + gs.tileSize > camera.offsetX() &&
                worldX - gs.tileSize < camera.offsetX() + GameScreen.SCREEN_WIDTH &&
                worldY + gs.tileSize > camera.offsetY() &&
                worldY - gs.tileSize < camera.offsetY() + GameScreen.SCREEN_HEIGHT) {

                if (tileNum >= 0 && tileNum < tile.length && tile[tileNum] != null && tile[tileNum].image != null) {
                    g2.drawImage(tile[tileNum].image, screenX, screenY, gs.tileSize, gs.tileSize, null);
                }
            }

            col++;

            if (col == gs.maxWorldCol) {
                col = 0;
                row++;
            }
        }
    }
}