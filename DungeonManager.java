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
        for (int i = 1; i <= 2; i++) {
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
                "tiles/world1/W1_D1.png", "maps/W1_D1.tmx",
                1500, 1256,
                1450, 1370, 120, 40,
                660, 1430,
                660, 1380, 40, 40   // entrance
        ));

        dungeons.put("1-2", new DungeonData(
                "tiles/world1/W1_D2.png", "maps/W1_D2.tmx",

                1380, 1246,
                1516, 1334, 100, 50,  // Exit area

                2914, 496,  // Correct return position in the world map
                2968, 450, 40, 40   // Entrance in world
        ));
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