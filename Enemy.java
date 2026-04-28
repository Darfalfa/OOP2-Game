import Characters.Character;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import javax.imageio.ImageIO;

/**
 * Enemy — a shadow creature that roams the world.
 * When the player gets close enough, it triggers a battle.
 */
public class Enemy {

    private GameScreen gp;

    public int x, y;
    public final int W = 48, H = 48;

    public Character character;

    private BufferedImage spriteSheet;

    private int frame = 0;
    private final int maxFrames = 12;

    private int frameTick = 0;
    private int frameSpeed = 6;

    // Simple roam AI
    private double vx = 0, vy = 0;
    private int roamTimer = 0;
    private final int ROAM_CHANGE = 90; // frames before changing direction

    // Trigger distance (pixels)
    public static final int DETECT_RANGE = 80;

    public boolean defeated = false;

    public int respawnTimer = 0;

    public Enemy(int x, int y, Character character, GameScreen gp) {
        this.x = x;
        this.y = y;
        this.character = character;
        this.gp = gp;
        pickNewDirection();

        try {
            spriteSheet = ImageIO.read(new File(character.getSpritePath()));
        } catch (Exception e) {
            spriteSheet = null;
        }
    }

    private void pickNewDirection() {
        double angle = Math.random() * Math.PI * 2;
        double spd   = 0.6 + Math.random() * 0.8;
        vx = Math.cos(angle) * spd;
        vy = Math.sin(angle) * spd;
    }

    public void update() {
        if (defeated) return;

        int nextX = x + (int) vx;
        int nextY = y + (int) vy;

        // horizontal collision
        if (!gp.isTileCollision(nextX, y, W, H)) {
            x = nextX;
        } else {
            vx *= -1;
            x -= (int) vx;
        }

        // vertical collision
        if (!gp.isTileCollision(x, nextY, W, H)) {
            y = nextY;
        } else {
            vy *= -1;
            y -= (int) vy;
        }

        // Periodically pick a new random direction
        if (++roamTimer >= ROAM_CHANGE) {
            roamTimer = 0;
            pickNewDirection();
        }

        frameTick++;

        if (frameTick >= frameSpeed) {
            frameTick = 0;
            frame = (frame + 1) % maxFrames;
        }
    }

    /** Returns true if the player is within detection range. */
    public boolean isNearPlayer(int px, int py, int pw, int ph) {
        int ex = x + W / 2, ey = y + H / 2;
        int plx = px + pw / 2, ply = py + ph / 2;
        double dist = Math.hypot(ex - plx, ey - ply);
        return dist < DETECT_RANGE;
    }

    public void draw(Graphics2D g2, Camera cam) {
        if (defeated) return;
        int sx = x - cam.offsetX();
        int sy = y - cam.offsetY();

        if (spriteSheet != null) {

            int frameWidth = character.getFrameWidth();
            int frameHeight = character.getFrameHeight();

            int maxFrames = character.getMaxFrames();
            frame %= maxFrames;

            int sxFrame = frame * frameWidth;

            g2.drawImage(
                    spriteSheet,
                    sx, sy, sx + W, sy + H,
                    sxFrame, 0, sxFrame + frameWidth, frameHeight,
                    null
            );
        }

        // HP bar above enemy
        int barW = W, barH = 5;
        int barX = sx, barY = sy - 10;
        g2.setColor(new Color(60, 0, 0));
        g2.fillRect(barX, barY, barW, barH);
        g2.setColor(new Color(200, 50, 50));
        g2.fillRect(barX, barY, (int)(barW * (double)character.getHp() / character.getMaxHp()), barH);
        g2.setColor(Color.BLACK);
        g2.setStroke(new BasicStroke(1f));
        g2.drawRect(barX, barY, barW, barH);
    }
}