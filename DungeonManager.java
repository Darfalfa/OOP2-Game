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
        // WORLD 1 - DUNGEON 1 (ShadowSprite x5) - Level 1+ required
        dungeons.put("1-1", new DungeonData(
                "tiles/world1/World1_D1.png", "maps/World1_D1.tmx",
                950, 764, 902, 880, 120, 40, 420, 732, 420, 672, 40, 40,
                1,
                "ShadowSprite", 5, minionPositions(950, 450),
                null, 0, 0, false  // no boss
        ));
        
        // WORLD 1 - DUNGEON 2 (ShadowSpriteLv2 x5) - Level 2+ required
        dungeons.put("1-2", new DungeonData(
                "tiles/world1/W1_D2.png", "maps/W1_D2.tmx",
                950, 764, 950, 900, 100, 50, 1848, 340, 1848, 228, 40, 40,
                2,
                "ShadowSpriteLv2", 5, minionPositions(950, 420),
                null, 0, 0, false  // no boss
        ));
        
        // WORLD 1 - DUNGEON 3 (Noctyx boss) - Level 3+ required, gives 1st puzzle piece
        dungeons.put("1-3", new DungeonData(
                "tiles/world1/World1_D3.png", "maps/World1_D3.tmx",
                1046, 1100, 1046, 1250, 100, 50, 396, 1644, 396, 1560, 40, 40,
                3,
                null, 0, null,
                "Noctyx", 1070, 488, true  // boss config, gives puzzle piece
        ));
        
        // WORLD 2 - DUNGEON 1 (ArmoredGhostSprite x5) - Level 4+ required
        dungeons.put("2-1", new DungeonData(
                "tiles/world2/World2_D1.png", "maps/World2_D1.tmx",
                926, 690, 926, 
                880, 100, 50, 
                460, 562, 
                460, 466, 80, 80,
                4,
                "ArmoredGhostSprite", 5, minionPositions(938, 420),
                null, 0, 0, false  // no boss
        ));
        
        // WORLD 2 - DUNGEON 2 (ArmoredGhostSpriteLv2 x5) - Level 5+ required, gives 2nd puzzle piece after all 5 die
        dungeons.put("2-2", new DungeonData(
                "tiles/world2/World2_D2.png", "maps/World2_D2.tmx",
                938, 738, 926, 880, 100, 50, 2296, 670, 2296, 562, 80, 80,
                5,
                "ArmoredGhostSpriteLv2", 5, minionPositions(938, 460),
                null, 0, 0, false  // no boss (minion level dungeon)
        ));
        
        // WORLD 2 - DUNGEON 3 (Bloodmancer boss) - Level 6+ required, gives 3rd puzzle piece
        dungeons.put("2-3", new DungeonData(
                "tiles/world2/World2_D3.png", "maps/World2_D3.tmx",
                1142, 1226, 1046, 1400, 100, 50, 1348, 202, 1348, 106, 80, 80,
                6,
                null, 0, null,
                "Bloodmancer", 1202,722, true  // boss config, gives puzzle piece
        ));
        
        // WORLD 3 - DUNGEON 1 (CultistSprite x5) - Level 7+ required
        dungeons.put("3-1", new DungeonData(
                "tiles/world3/World3_D1.png", "maps/World3_D1.tmx",
                926, 1158, 
                926, 1302, 
                100, 50, 796, 665, 
                808, 560, 80, 80,
                7,
                "CultistSprite", 5, minionPositions(926, 750),
                null, 0, 0, false  // no boss
        ));
        
        // WORLD 3 - DUNGEON 2 (CultistSpriteLv2 x5) - Level 8+ required, gives 4th puzzle piece after all 5 die
        dungeons.put("3-2", new DungeonData(
                "tiles/world3/World3_D2.png", "maps/World3_D2.tmx",
                826, 714, 826, 850, 100, 50, 2140, 689, 2140, 605, 80, 80,
                8,
                "CultistSpriteLv2", 5, minionPositions(838, 438),
                null, 0, 0, false  // no boss (minion level dungeon)
        ));
        
        // WORLD 3 - DUNGEON 3 (Khai boss) - Level 9+ required
        dungeons.put("3-3", new DungeonData(
                "tiles/world3/World3_D3.png", "maps/World3_D3.tmx",
                1035, 750, 1046, 1250, 100, 50, 1456, 377, 1456, 269, 80, 80,
                9,
                null, 0, null,
                "Khai", 1035, 438, false  // boss config (no puzzle piece for final boss)
        ));
    }

    private int[][] minionPositions(int baseX, int baseY) {
        return new int[][] {
                {baseX, baseY},
                {baseX + 120, baseY},
                {baseX - 120, baseY},
                {baseX + 60, baseY + 120},
                {baseX - 60, baseY + 120}
        };
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

    /**
     * Check if player level allows entry to this dungeon
     */
    public boolean canEnterDungeon(int world, int dungeonNumber, int playerLevel) {
        DungeonData data = getDungeon(world, dungeonNumber);
        if (data == null) return false;
        return playerLevel >= data.requiredLevel;
    }

    /**
     * Get spawn configuration for dungeon
     */
    public String getMinionsType(int world, int dungeonNumber) {
        DungeonData data = getDungeon(world, dungeonNumber);
        return data != null ? data.minionsType : null;
    }

    public int getMinionsCount(int world, int dungeonNumber) {
        DungeonData data = getDungeon(world, dungeonNumber);
        return data != null ? data.minionsCount : 0;
    }

    public int[][] getMinionSpawnPositions(int world, int dungeonNumber) {
        DungeonData data = getDungeon(world, dungeonNumber);
        return data != null ? data.minionSpawnPositions : null;
    }

    /**
     * Get boss configuration for dungeon
     */
    public String getBossType(int world, int dungeonNumber) {
        DungeonData data = getDungeon(world, dungeonNumber);
        return data != null ? data.bossType : null;
    }

    public int[] getBossSpawnPos(int world, int dungeonNumber) {
        DungeonData data = getDungeon(world, dungeonNumber);
        if (data == null) return new int[]{0, 0};
        return new int[]{data.bossSpawnX, data.bossSpawnY};
    }

    public boolean doesBossGivePuzzlePiece(int world, int dungeonNumber) {
        DungeonData data = getDungeon(world, dungeonNumber);
        return data != null && data.givesPuzzlePiece;
    }
}
