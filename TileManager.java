import javax.imageio.ImageIO;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;

public class TileManager {

    GameScreen gs;
    public Tile[] tile;
    public int[][] mapTileNum;

    public TileManager(GameScreen gs) {
        this.gs = gs;

        tile = new Tile[10];
        mapTileNum = new int[gs.maxWorldCol][gs.maxWorldRow];

        getTileImage();
        loadMockMap(); // mock map only so the project runs without text map files
    }

    public void getTileImage() {
        try {
            tile[0] = new Tile();
            tile[0].image = loadTile("images/tiles/grass.png");

            tile[1] = new Tile();
            tile[1].image = loadTile("images/tiles/wall.png");
            tile[1].collision = true;

            tile[2] = new Tile();
            tile[2].image = loadTile("images/tiles/water.png");
            tile[2].collision = true;

            tile[3] = new Tile();
            tile[3].image = loadTile("images/tiles/earth.png");

            tile[4] = new Tile();
            tile[4].image = loadTile("images/tiles/tree.png");
            tile[4].collision = true;

            tile[5] = new Tile();
            tile[5].image = loadTile("images/tiles/sand.png");

        } catch (Exception e) {
            e.printStackTrace();
            createFallbackTiles();
        }
    }

    private BufferedImage loadTile(String path) throws IOException {
        File f = new File(path);
        if (!f.exists()) {
            throw new IOException("Missing tile file: " + path);
        }
        return ImageIO.read(f);
    }

    private void createFallbackTiles() {
        for (int i = 0; i < tile.length; i++) {
            if (tile[i] == null) tile[i] = new Tile();
        }

        tile[0].image = solidTile(new java.awt.Color(70, 140, 70));   // grass
        tile[1].image = solidTile(new java.awt.Color(110, 110, 110)); // wall
        tile[2].image = solidTile(new java.awt.Color(70, 110, 180));  // water
        tile[3].image = solidTile(new java.awt.Color(120, 90, 60));   // earth
        tile[4].image = solidTile(new java.awt.Color(40, 90, 40));    // tree
        tile[5].image = solidTile(new java.awt.Color(194, 178, 128)); // sand

        tile[1].collision = true;
        tile[2].collision = true;
        tile[4].collision = true;
    }

    private BufferedImage solidTile(java.awt.Color color) {
        BufferedImage img = new BufferedImage(gs.tileSize, gs.tileSize, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2 = img.createGraphics();
        g2.setColor(color);
        g2.fillRect(0, 0, gs.tileSize, gs.tileSize);
        g2.dispose();
        return img;
    }

    private void loadMockMap() {
        for (int col = 0; col < gs.maxWorldCol; col++) {
            for (int row = 0; row < gs.maxWorldRow; row++) {

                if (row == 0 || col == 0 || row == gs.maxWorldRow - 1 || col == gs.maxWorldCol - 1) {
                    mapTileNum[col][row] = 1; // wall border
                } else {
                    mapTileNum[col][row] = 0; // grass
                }
            }
        }

        // small water patch
        for (int col = 8; col <= 11; col++) {
            for (int row = 6; row <= 8; row++) {
                mapTileNum[col][row] = 2;
            }
        }

        // small sand area
        for (int col = 15; col <= 19; col++) {
            for (int row = 10; row <= 12; row++) {
                mapTileNum[col][row] = 5;
            }
        }

        // a few trees
        mapTileNum[6][5] = 4;
        mapTileNum[7][5] = 4;
        mapTileNum[6][6] = 4;
        mapTileNum[22][9] = 4;
        mapTileNum[23][9] = 4;
        mapTileNum[22][10] = 4;
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