import java.awt.Rectangle;
import java.util.HashMap;
import java.util.Map;

public class DungeonManager {

    private final Map<String, DungeonData> dungeons = new HashMap<>();

    private boolean inDungeon = false;
    private int currentDungeon = 0;

    public DungeonManager() {
        loadDungeons();
    }

    public int checkDungeonEntry(int world, Rectangle playerBox) {
        for (int i = 1; i <= 3; i++) {
            DungeonData d = getDungeon(world, i);
            if (d == null) continue;

            Rectangle entrance = new Rectangle(
                    d.entranceX,
                    d.entranceY,
                    d.entranceW,
                    d.entranceH
            );

            if (playerBox.intersects(entrance)) {
                return i;
            }
        }
        return 0;
    }

    private void loadDungeons() {
        dungeons.put("1-1", new DungeonData(
                "tiles/world1/World1_D1.png", "maps/World1_D1.tmx",
                950, 500,
                902, 880, 120, 40,
                660, 1430,
                660, 1380, 40, 40   // entrance
        ));

        dungeons.put("1-2", new DungeonData(
                "tiles/world1/W1_D2.png", "maps/W1_D2.tmx",

                950, 764,
                950, 900, 100, 50,  // Exit area

                2914, 496,  // Correct return position in the world map
                2968, 450, 40, 40   // Entrance in world
        ));

                dungeons.put("1-3", new DungeonData(
                "tiles/world1/World1_D3.png", "maps/World1_D3.tmx",

                1046, 1100,
                1046, 1250, 100, 50,  // Exit area

             684, 2649,  // spawned outside
            684, 2592, 40, 40   // Entrance
        ));

        // dungeons.put("2-1", new DungeonData(
        //         "tiles/world2/W2_D1.png", "maps/W2_D1.tmx",
        //         1526, 1172, //spawned inside
        //         1526, 1288, 100, 50,  // Exit
        //         684, 2649,  // spawned outside
        //         684, 2592, 40, 40   // Entrance
        // ));
    }

    public DungeonData getDungeon(int world, int dungeonNumber) {
        return dungeons.get(world + "-" + dungeonNumber);
    }

    public Rectangle getExitRect(int world, int dungeonNumber) {
        DungeonData data = getDungeon(world, dungeonNumber);

        if (data == null) return null;

        return new Rectangle(data.exitX, data.exitY, data.exitW, data.exitH);
    }

    public boolean isInDungeon() {
        return inDungeon;
    }

    public int getCurrentDungeon() {
        return currentDungeon;
    }

    public void enterDungeon(int dungeonNumber) {
        inDungeon = true;
        currentDungeon = dungeonNumber;
    }

    public void exitDungeon() {
        inDungeon = false;
        currentDungeon = 0;
    }
}