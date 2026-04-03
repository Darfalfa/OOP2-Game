public class Camera {

    public double x, y;

    int screenWidth, screenHeight;
    int worldWidth, worldHeight;

    // Smoothing factor (0 = no movement, 1 = instant snap)
    private static final double SMOOTHING = 0.12;

    public Camera(int screenWidth, int screenHeight, int worldWidth, int worldHeight) {
        this.screenWidth  = screenWidth;
        this.screenHeight = screenHeight;
        this.worldWidth   = worldWidth;
        this.worldHeight  = worldHeight;
    }

    /**
     * Smoothly moves the camera so the player stays centred,
     * then clamps so we never show outside the world.
     */
    public void update(int playerX, int playerY, int playerWidth, int playerHeight) {

        // Target: centre the player on screen
        double targetX = playerX + playerWidth  / 2.0 - screenWidth  / 2.0;
        double targetY = playerY + playerHeight / 2.0 - screenHeight / 2.0;

        // Lerp towards target
        x += (targetX - x) * SMOOTHING;
        y += (targetY - y) * SMOOTHING;

        // Clamp to world bounds
        if (x < 0) x = 0;
        if (y < 0) y = 0;
        if (x > worldWidth  - screenWidth)  x = worldWidth  - screenWidth;
        if (y > worldHeight - screenHeight) y = worldHeight - screenHeight;
    }

    /** X offset to apply when drawing world objects */
    public int offsetX() { return (int) x; }

    /** Y offset to apply when drawing world objects */
    public int offsetY() { return (int) y; }
}