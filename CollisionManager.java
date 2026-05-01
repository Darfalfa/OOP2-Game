import org.w3c.dom.*;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.File;
import java.util.*;
import java.awt.Color;
import java.awt.Shape;
import java.awt.geom.*;

public class CollisionManager {

    private final Set<Integer> blockedTileIds = new HashSet<>();
    private boolean[][] blocked;
    
    // Maps tile GID to list of collision shapes for that tile
    private Map<Integer, List<Shape>> tileCollisionShapes = new HashMap<>();
    // Maps world tile position to tile GID
    private Map<String, Integer> worldTileGids = new HashMap<>();

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
                        
                        // Store the GID at this world position
                        worldTileGids.put(col + "," + row, gid);

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
                int gid = firstgid + localId;

                NodeList objectGroups = tile.getElementsByTagName("objectgroup");

                if (objectGroups.getLength() > 0) {
                    blockedTileIds.add(gid);
                    
                    // Parse actual collision shapes from objectgroup
                    List<Shape> shapes = new ArrayList<>();
                    Element objectGroup = (Element) objectGroups.item(0);
                    NodeList objects = objectGroup.getElementsByTagName("object");
                    
                    for (int j = 0; j < objects.getLength(); j++) {
                        Element obj = (Element) objects.item(j);
                        Shape shape = parseCollisionObject(obj);
                        if (shape != null) {
                            shapes.add(shape);
                        }
                    }
                    
                    if (!shapes.isEmpty()) {
                        tileCollisionShapes.put(gid, shapes);
                    }
                }
            }

            System.out.println("Loaded TSX collision: " + tsxFile.getPath());

        } catch (Exception e) {
            System.out.println("Could not load TSX collision: " + tsxFile.getPath());
            e.printStackTrace();
        }
    }

    private Shape parseCollisionObject(Element obj) {
        try {
            // Get base position and size
            float x = obj.hasAttribute("x") ? Float.parseFloat(obj.getAttribute("x")) : 0;
            float y = obj.hasAttribute("y") ? Float.parseFloat(obj.getAttribute("y")) : 0;
            float width = obj.hasAttribute("width") ? Float.parseFloat(obj.getAttribute("width")) : 0;
            float height = obj.hasAttribute("height") ? Float.parseFloat(obj.getAttribute("height")) : 0;
            
            // Check for polygon
            NodeList polygons = obj.getElementsByTagName("polygon");
            if (polygons.getLength() > 0) {
                String points = polygons.item(0).getAttributes().getNamedItem("points").getTextContent();
                return parsePolygon(points, x, y);
            }
            
            // Check for polyline
            NodeList polylines = obj.getElementsByTagName("polyline");
            if (polylines.getLength() > 0) {
                String points = polylines.item(0).getAttributes().getNamedItem("points").getTextContent();
                return parsePolyline(points, x, y);
            }
            
            // Default to rectangle
            if (width > 0 && height > 0) {
                return new Rectangle2D.Float(x, y, width, height);
            }
            
        } catch (Exception e) {
            System.out.println("Error parsing collision object: " + e.getMessage());
        }
        
        return null;
    }

    private Shape parsePolygon(String pointsStr, float offsetX, float offsetY) {
        String[] points = pointsStr.split(" ");
        Path2D.Float path = new Path2D.Float();
        
        for (int i = 0; i < points.length; i++) {
            String[] coords = points[i].split(",");
            float x = Float.parseFloat(coords[0]) + offsetX;
            float y = Float.parseFloat(coords[1]) + offsetY;
            
            if (i == 0) {
                path.moveTo(x, y);
            } else {
                path.lineTo(x, y);
            }
        }
        
        path.closePath();
        return path;
    }

    private Shape parsePolyline(String pointsStr, float offsetX, float offsetY) {
        // For now, treat polyline as polygon
        return parsePolygon(pointsStr, offsetX, offsetY);
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

        Rectangle2D.Float playerRect = new Rectangle2D.Float(x, y, width, height);

        for (int col = leftCol; col <= rightCol; col++) {
            for (int row = topRow; row <= bottomRow; row++) {
                if (blocked[col][row]) {
                    // Try to get custom collision shapes for this tile
                    List<Shape> shapes = getCollisionShapesAtTile(col, row);
                    
                    if (shapes != null && !shapes.isEmpty()) {
                        // Check if player rect intersects any custom shape
                        int tileWorldX = col * tileSize;
                        int tileWorldY = row * tileSize;
                        
                        for (Shape shape : shapes) {
                            // Translate shape to world coordinates
                            Area shapeArea = new Area(shape);
                            shapeArea.transform(AffineTransform.getTranslateInstance(tileWorldX, tileWorldY));
                            
                            // Check if player intersects this shape
                            Area playerArea = new Area(playerRect);
                            shapeArea.intersect(playerArea);
                            
                            if (!shapeArea.isEmpty()) {
                                return true;
                            }
                        }
                    } else {
                        // Legacy: no custom shapes, treat entire tile as blocked
                        return true;
                    }
                }
            }
        }

        return false;
    }

    public boolean[][] getBlockedTiles() {
        return blocked;
    }

    public int getTileSize() {
        return tileSize;
    }

    public int getMapCols() {
        return mapCols;
    }

    public int getMapRows() {
        return mapRows;
    }

    private void loadEmbeddedTilesetCollision(Element tileset, int firstgid) {
        NodeList tiles = tileset.getElementsByTagName("tile");

        for (int i = 0; i < tiles.getLength(); i++) {
            Element tile = (Element) tiles.item(i);

            int localId = Integer.parseInt(tile.getAttribute("id"));
            int gid = firstgid + localId;

            NodeList objectGroups = tile.getElementsByTagName("objectgroup");

            if (objectGroups.getLength() > 0) {
                blockedTileIds.add(gid);
                
                // Parse actual collision shapes from objectgroup
                List<Shape> shapes = new ArrayList<>();
                Element objectGroup = (Element) objectGroups.item(0);
                NodeList objects = objectGroup.getElementsByTagName("object");
                
                for (int j = 0; j < objects.getLength(); j++) {
                    Element obj = (Element) objects.item(j);
                    Shape shape = parseCollisionObject(obj);
                    if (shape != null) {
                        shapes.add(shape);
                    }
                }
                
                if (!shapes.isEmpty()) {
                    tileCollisionShapes.put(gid, shapes);
                }
            }
        }

        System.out.println("Loaded embedded collision tiles");
    }

    public List<Shape> getCollisionShapesAtTile(int col, int row) {
        String key = col + "," + row;
        Integer gid = worldTileGids.get(key);
        if (gid != null && tileCollisionShapes.containsKey(gid)) {
            return tileCollisionShapes.get(gid);
        }
        return null;
    }
}