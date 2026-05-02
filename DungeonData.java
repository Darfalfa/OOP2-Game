public class DungeonData {
    public String mapPath;
    public String collisionPath;

    public int entranceX;
    public int entranceY;
    public int entranceW;
    public int entranceH;

    // where player spawns inside dungeon
    public int enterX;
    public int enterY;

    // exit area inside dungeon
    public int exitX;
    public int exitY;
    public int exitW;
    public int exitH;

    // where player returns in the world map
    public int returnX;
    public int returnY;

    // Dungeon access requirement
    public int requiredLevel;

    // Enemy spawn configuration
    public String minionsType;      // e.g., "ShadowSprite", "ArmoredGhost", "Cultist"
    public int minionsCount;        // e.g., 5 minions
    public int[][] minionSpawnPositions; // placeholder positions for each minion

    // Boss configuration
    public String bossType;         // e.g., "Noctyx", "Bloodmancer", "Khai"
    public int bossSpawnX;          // placeholder X for boss
    public int bossSpawnY;          // placeholder Y for boss
    public boolean givesPuzzlePiece; // whether beating the boss gives a puzzle piece

    public DungeonData(String mapPath, String collisionPath,
                       int enterX, int enterY,
                       int exitX, int exitY, int exitW, int exitH,
                       int returnX, int returnY,
                       int entranceX, int entranceY, int entranceW, int entranceH,
                       int requiredLevel,
                       String minionsType, int minionsCount, int[][] minionSpawnPositions,
                       String bossType, int bossSpawnX, int bossSpawnY, boolean givesPuzzlePiece) {

        this.mapPath = mapPath;
        this.collisionPath = collisionPath;

        this.enterX = enterX;
        this.enterY = enterY;

        this.exitX = exitX;
        this.exitY = exitY;
        this.exitW = exitW;
        this.exitH = exitH;

        this.returnX = returnX;
        this.returnY = returnY;

        this.entranceX = entranceX;
        this.entranceY = entranceY;
        this.entranceW = entranceW;
        this.entranceH = entranceH;

        this.requiredLevel = requiredLevel;
        
        this.minionsType = minionsType;
        this.minionsCount = minionsCount;
        this.minionSpawnPositions = minionSpawnPositions;

        this.bossType = bossType;
        this.bossSpawnX = bossSpawnX;
        this.bossSpawnY = bossSpawnY;
        this.givesPuzzlePiece = givesPuzzlePiece;
    }
}
