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

    public DungeonData(String mapPath, String collisionPath,
                       int enterX, int enterY,
                       int exitX, int exitY, int exitW, int exitH,
                       int returnX, int returnY,
                       int entranceX, int entranceY, int entranceW, int entranceH) {

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
    }
}