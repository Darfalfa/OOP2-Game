import javax.imageio.ImageIO;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

public class TileManager {

    GameScreen gs;
    public Tile[] tile;
    public int[][] mapTileNum;
    public boolean[][] collisionMap;
    private boolean[] tileCollidable;
    boolean drawPath = false;
    private boolean showCollision = false;

    ArrayList<String> fileNames = new ArrayList<>();
    ArrayList<String> collisionStatus = new ArrayList<>();

    public TileManager(GameScreen gs) {
        this(gs, "/maps/Map_World2_Collision.tmx");
    }

    public TileManager(GameScreen gs, String mapFilePath) {
        this.gs = gs;

        if (!mapFilePath.toLowerCase().endsWith(".tmx") || !loadTmx(mapFilePath)) {
            loadTileData();

            int tileArraySize = Math.max(10, fileNames.size());
            tile = new Tile[tileArraySize];
            mapTileNum = new int[gs.maxWorldCol][gs.maxWorldRow];

            getTileImage();
            loadMap("/maps/map.txt");
        }
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
        if (filePath.toLowerCase().endsWith(".tmx")) {
            if (!loadTmx(filePath)) {
                System.out.println("Failed to load TMX map: " + filePath);
                loadMockMap();
            }
            return;
        }

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

    private boolean loadTmx(String filePath) {
        InputStream is = getClass().getResourceAsStream(filePath);
        if (is == null) {
            File tmxFile = new File("." + filePath);
            if (tmxFile.exists()) {
                try {
                    is = new FileInputStream(tmxFile);
                } catch (IOException ignored) {
                }
            }
        }

        if (is == null) {
            System.out.println("Could not find TMX file: " + filePath);
            return false;
        }

        try (InputStream resourceStream = is) {
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            factory.setIgnoringComments(true);
            factory.setIgnoringElementContentWhitespace(true);
            DocumentBuilder builder = factory.newDocumentBuilder();
            Document document = builder.parse(is);
            Element mapElement = document.getDocumentElement();

            int mapCols = Integer.parseInt(mapElement.getAttribute("width"));
            int mapRows = Integer.parseInt(mapElement.getAttribute("height"));
            int mapTileW = Integer.parseInt(mapElement.getAttribute("tilewidth"));
            int mapTileH = Integer.parseInt(mapElement.getAttribute("tileheight"));

            gs.maxWorldCol = mapCols;
            gs.maxWorldRow = mapRows;
            mapTileNum = new int[gs.maxWorldCol][gs.maxWorldRow];
            collisionMap = new boolean[gs.maxWorldCol][gs.maxWorldRow];

            for (int col = 0; col < gs.maxWorldCol; col++) {
                for (int row = 0; row < gs.maxWorldRow; row++) {
                    mapTileNum[col][row] = -1;
                }
            }

            NodeList tilesetNodes = mapElement.getElementsByTagName("tileset");
            List<TilesetInfo> tilesetInfos = new ArrayList<>();
            List<Integer> collisionGids = new ArrayList<>();
            int totalTiles = 0;

            for (int i = 0; i < tilesetNodes.getLength(); i++) {
                Element tilesetElem = (Element) tilesetNodes.item(i);
                int firstgid = Integer.parseInt(tilesetElem.getAttribute("firstgid"));
                int tilecount = Integer.parseInt(tilesetElem.getAttribute("tilecount"));
                int tilewidth = tilesetElem.hasAttribute("tilewidth") ? Integer.parseInt(tilesetElem.getAttribute("tilewidth")) : mapTileW;
                int tileheight = tilesetElem.hasAttribute("tileheight") ? Integer.parseInt(tilesetElem.getAttribute("tileheight")) : mapTileH;
                int columns = tilesetElem.hasAttribute("columns") ? Integer.parseInt(tilesetElem.getAttribute("columns")) : 0;

                Element imageElem = (Element) tilesetElem.getElementsByTagName("image").item(0);
                String imageSource = imageElem.getAttribute("source");

                NodeList tileNodes = tilesetElem.getElementsByTagName("tile");
                for (int j = 0; j < tileNodes.getLength(); j++) {
                    Element tileElem = (Element) tileNodes.item(j);
                    int localId = Integer.parseInt(tileElem.getAttribute("id"));
                    int gid = firstgid + localId;
                    if (gid > 0) {
                        boolean hasCollision = tileElem.getElementsByTagName("objectgroup").getLength() > 0
                                             || tileElem.getElementsByTagName("properties").getLength() > 0;
                        if (hasCollision) {
                            collisionGids.add(gid);
                        }
                    }
                }

                tilesetInfos.add(new TilesetInfo(firstgid, tilecount, columns, tilewidth, tileheight, imageSource));
                totalTiles = Math.max(totalTiles, firstgid + tilecount - 1);
            }

            tile = new Tile[totalTiles];
            tileCollidable = new boolean[totalTiles];
            for (int gid : collisionGids) {
                if (gid > 0 && gid <= tileCollidable.length) {
                    tileCollidable[gid - 1] = true;
                }
            }
            for (TilesetInfo tilesetInfo : tilesetInfos) {
                BufferedImage tilesetImage = loadTilesetImage(tilesetInfo.imageSource, filePath);
                if (tilesetImage == null) {
                    System.out.println("Could not load tileset image: " + tilesetInfo.imageSource);
                    continue;
                }

                int cols = tilesetInfo.columns;
                if (cols <= 0) {
                    cols = tilesetImage.getWidth() / tilesetInfo.tileWidth;
                }

                for (int tileIndex = 0; tileIndex < tilesetInfo.tileCount; tileIndex++) {
                    int gid = tilesetInfo.firstgid + tileIndex;
                    int sx = (tileIndex % cols) * tilesetInfo.tileWidth;
                    int sy = (tileIndex / cols) * tilesetInfo.tileHeight;

                    if (sx + tilesetInfo.tileWidth > tilesetImage.getWidth() || sy + tilesetInfo.tileHeight > tilesetImage.getHeight()) {
                        continue;
                    }

                    BufferedImage rawTile = tilesetImage.getSubimage(sx, sy, tilesetInfo.tileWidth, tilesetInfo.tileHeight);
                    Tile tileEntry = new Tile();
                    tileEntry.image = scaleTile(rawTile);
                    tileEntry.collision = gid > 0 && gid <= tileCollidable.length && tileCollidable[gid - 1];
                    tile[gid - 1] = tileEntry;
                }
            }

            NodeList layerNodes = mapElement.getElementsByTagName("layer");
            for (int i = 0; i < layerNodes.getLength(); i++) {
                Element layerElem = (Element) layerNodes.item(i);
                String layerName = layerElem.getAttribute("name");
                int layerWidth = Integer.parseInt(layerElem.getAttribute("width"));
                int layerHeight = Integer.parseInt(layerElem.getAttribute("height"));

                Element dataElem = (Element) layerElem.getElementsByTagName("data").item(0);
                if (dataElem == null) {
                    continue;
                }

                String dataText = dataElem.getTextContent().trim();
                String[] tileValues = dataText.replaceAll("[\r\n]+", "").split("\\s*,\\s*");

                for (int row = 0; row < layerHeight; row++) {
                    for (int col = 0; col < layerWidth; col++) {
                        int index = row * layerWidth + col;
                        if (index >= tileValues.length) {
                            continue;
                        }
                        int gid = Integer.parseInt(tileValues[index]);
                        if (gid <= 0) {
                            continue;
                        }

                        if (layerName.equalsIgnoreCase("collision") || layerName.toLowerCase().contains("collision")) {
                            if (col < gs.maxWorldCol && row < gs.maxWorldRow) {
                                collisionMap[col][row] = (gid > 0);
                            }
                        } else if (col < gs.maxWorldCol && row < gs.maxWorldRow) {
                            mapTileNum[col][row] = gid - 1;
                        }
                    }
                }
            }

            return true;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    private BufferedImage loadTilesetImage(String imageSource, String mapFilePath) throws IOException {
        String basePath = mapFilePath;
        if (basePath.contains("/")) {
            basePath = basePath.substring(0, basePath.lastIndexOf('/'));
        }

        String candidate = basePath + "/" + imageSource;
        candidate = candidate.replace("\\", "/");
        try {
            candidate = new File(candidate).toPath().normalize().toString().replace("\\", "/");
        } catch (Exception ignored) {
        }

        InputStream is = getClass().getResourceAsStream(candidate);
        if (is == null && !candidate.startsWith("/")) {
            is = getClass().getResourceAsStream("/" + candidate);
        }
        if (is != null) {
            BufferedImage img = ImageIO.read(is);
            if (img != null) {
                return img;
            }
        }

        File file = new File(".", candidate);
        if (!file.exists()) {
            file = new File("maps", imageSource);
        }
        if (!file.exists()) {
            file = findFileInTiles(imageSource);
        }
        if (!file.exists()) {
            file = findFileInTiles(new File(imageSource).getName());
        }
        if (file.exists()) {
            BufferedImage img = ImageIO.read(file);
            if (img != null) {
                return img;
            }
        }

        return null;
    }

    private File findFileInTiles(String fileName) {
        File tilesFolder = new File("tiles");
        if (!tilesFolder.exists() || !tilesFolder.isDirectory()) {
            return new File("", fileName);
        }
        return findFileRecursively(tilesFolder, fileName);
    }

    private File findFileRecursively(File folder, String fileName) {
        File candidate = new File(folder, fileName);
        if (candidate.exists()) {
            return candidate;
        }

        File[] children = folder.listFiles();
        if (children == null) {
            return candidate;
        }

        for (File child : children) {
            if (child.isDirectory()) {
                File result = findFileRecursively(child, fileName);
                if (result.exists()) {
                    return result;
                }
            }
        }

        return candidate;
    }

    private static class TilesetInfo {
        final int firstgid;
        final int tileCount;
        final int columns;
        final int tileWidth;
        final int tileHeight;
        final String imageSource;

        TilesetInfo(int firstgid, int tileCount, int columns, int tileWidth, int tileHeight, String imageSource) {
            this.firstgid = firstgid;
            this.tileCount = tileCount;
            this.columns = columns;
            this.tileWidth = tileWidth;
            this.tileHeight = tileHeight;
            this.imageSource = imageSource;
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

        if (showCollision) {
            col = 0;
            row = 0;
            while (col < gs.maxWorldCol && row < gs.maxWorldRow) {
                if (collisionMap[col][row]) {
                    int worldX = col * gs.tileSize;
                    int worldY = row * gs.tileSize;
                    int screenX = worldX - camera.offsetX();
                    int screenY = worldY - camera.offsetY();

                    if (worldX + gs.tileSize > camera.offsetX() &&
                        worldX - gs.tileSize < camera.offsetX() + GameScreen.SCREEN_WIDTH &&
                        worldY + gs.tileSize > camera.offsetY() &&
                        worldY - gs.tileSize < camera.offsetY() + GameScreen.SCREEN_HEIGHT) {

                        g2.setColor(Color.RED);
                        g2.drawRect(screenX, screenY, gs.tileSize, gs.tileSize);
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

    public void toggleCollisionVisibility() {
        showCollision = !showCollision;
    }
}
