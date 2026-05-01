import org.w3c.dom.*;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.File;
import java.util.HashSet;
import java.util.Set;

public class CollisionManager {

    private final Set<Integer> blockedTileIds = new HashSet<>();
    private boolean[][] blocked;

    private int mapCols;
    private int mapRows;
    private final int tileSize = 72;

    private String mapPath;

    public CollisionManager(String mapPath) {
        this.mapPath = mapPath;
        loadCollision();
    }

    private void loadCollision() {
        try {
            File tmxFile = new File(mapPath);

            Document doc = DocumentBuilderFactory.newInstance()
                    .newDocumentBuilder()
                    .parse(tmxFile);

            doc.getDocumentElement().normalize();

            Element mapElement = (Element) doc.getElementsByTagName("map").item(0);

            mapCols = Integer.parseInt(mapElement.getAttribute("width"));
            mapRows = Integer.parseInt(mapElement.getAttribute("height"));

            blocked = new boolean[mapCols][mapRows];

            blockedTileIds.clear();

            // Read external TSX tilesets
            NodeList tilesets = doc.getElementsByTagName("tileset");

            for (int i = 0; i < tilesets.getLength(); i++) {
                Element tileset = (Element) tilesets.item(i);

                int firstgid = Integer.parseInt(tileset.getAttribute("firstgid"));

                // If it's an external TSX (like dungeon)
                if (tileset.hasAttribute("source")) {
                    String source = tileset.getAttribute("source");
                    File tsxFile = new File(tmxFile.getParentFile(), source);

                    if (!tsxFile.exists()) {
                        tsxFile = new File("maps/" + new File(source).getName());
                    }

                    loadTilesetCollision(tsxFile, firstgid);
                }
                // If it's embedded (like your world map)
                else {
                    loadEmbeddedTilesetCollision(tileset, firstgid);
                }
            }

            NodeList layerList = doc.getElementsByTagName("layer");

            for (int l = 0; l < layerList.getLength(); l++) {
                Element layer = (Element) layerList.item(l);

                NodeList dataList = layer.getElementsByTagName("data");
                if (dataList.getLength() == 0) continue;

                String csv = dataList.item(0).getTextContent().trim();
                String[] nums = csv.split(",");

                int index = 0;

                for (int row = 0; row < mapRows; row++) {
                    for (int col = 0; col < mapCols; col++) {
                        int gid = Integer.parseInt(nums[index].trim());

                        if (blockedTileIds.contains(gid)) {
                            blocked[col][row] = true;
                        }

                        index++;
                    }
                }
            }

            System.out.println("Collision loaded: " + mapCols + " x " + mapRows);
            System.out.println("Blocked tile IDs: " + blockedTileIds.size());

        } catch (Exception e) {
            System.out.println("Could not load collision TMX");
            e.printStackTrace();
        }
    }

    private void loadTilesetCollision(File tsxFile, int firstgid) {
        try {
            Document tsxDoc = DocumentBuilderFactory.newInstance()
                    .newDocumentBuilder()
                    .parse(tsxFile);

            tsxDoc.getDocumentElement().normalize();

            NodeList tiles = tsxDoc.getElementsByTagName("tile");

            for (int i = 0; i < tiles.getLength(); i++) {
                Element tile = (Element) tiles.item(i);

                int localId = Integer.parseInt(tile.getAttribute("id"));

                NodeList objectGroups = tile.getElementsByTagName("objectgroup");

                if (objectGroups.getLength() > 0) {
                    blockedTileIds.add(firstgid + localId);
                }
            }

            System.out.println("Loaded TSX collision: " + tsxFile.getPath());

        } catch (Exception e) {
            System.out.println("Could not load TSX collision: " + tsxFile.getPath());
            e.printStackTrace();
        }
    }

    public boolean isColliding(int x, int y, int width, int height) {

        if (x < 0 || y < 0 ||
                x + width > mapCols * tileSize ||
                y + height > mapRows * tileSize) {
            return true;
        }

        int leftCol = x / tileSize;
        int rightCol = (x + width - 1) / tileSize;
        int topRow = y / tileSize;
        int bottomRow = (y + height - 1) / tileSize;

        for (int col = leftCol; col <= rightCol; col++) {
            for (int row = topRow; row <= bottomRow; row++) {
                if (blocked[col][row]) {
                    return true;
                }
            }
        }

        return false;
    }

    private void loadEmbeddedTilesetCollision(Element tileset, int firstgid) {
        NodeList tiles = tileset.getElementsByTagName("tile");

        for (int i = 0; i < tiles.getLength(); i++) {
            Element tile = (Element) tiles.item(i);

            int localId = Integer.parseInt(tile.getAttribute("id"));

            NodeList objectGroups = tile.getElementsByTagName("objectgroup");

            if (objectGroups.getLength() > 0) {
                blockedTileIds.add(firstgid + localId);
            }
        }

        System.out.println("Loaded embedded collision tiles");
    }
}