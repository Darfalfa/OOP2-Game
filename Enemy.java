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

    public int x, y;
    public static final int W = 48, H = 48;

    public Character character;

    // Simple roam AI
    private double vx = 0, vy = 0;
    private int roamTimer = 0;
    private final int ROAM_CHANGE = 90; // frames before changing direction

    // Trigger distance (pixels)
    public static final int DETECT_RANGE = 80;

    public boolean defeated = false;

    public int respawnTimer = 0;
    public int respawnDelay = 300; 

    // Sprite (optional — will draw a fallback shape if null)
    private BufferedImage sprite;

    public Enemy(int x, int y, Character character) {
        this.x = x;
        this.y = y;
        this.character = character;
        pickNewDirection();

        try {
            sprite = ImageIO.read(new File("images/enemy_shadow.png"));
        } catch (Exception e) {
            sprite = null; // fallback to drawn shape
        }
    }

    private void pickNewDirection() {
        double angle = Math.random() * Math.PI * 2;
        double spd   = 0.6 + Math.random() * 0.8;
        vx = Math.cos(angle) * spd;
        vy = Math.sin(angle) * spd;
    }

    public void update(int worldW, int worldH) {
        if (defeated) return;

        x += (int) vx;
        y += (int) vy;

        // Bounce off world edges
        if (x < 0)          { x = 0;          vx =  Math.abs(vx); }
        if (y < 0)          { y = 0;           vy =  Math.abs(vy); }
        if (x > worldW - W) { x = worldW - W; vx = -Math.abs(vx); }
        if (y > worldH - H) { y = worldH - H; vy = -Math.abs(vy); }

        // Periodically pick a new random direction
        if (++roamTimer >= ROAM_CHANGE) {
            roamTimer = 0;
            pickNewDirection();
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

        if (sprite != null) {
            g2.drawImage(sprite, sx, sy, W, H, null);
        } else {
            // Fallback: draw a spooky dark blob
            g2.setColor(new Color(20, 10, 30, 220));
            g2.fillOval(sx, sy, W, H);
            g2.setColor(new Color(120, 80, 200, 180));
            g2.setStroke(new BasicStroke(2f));
            g2.drawOval(sx, sy, W, H);
            // Eyes
            g2.setColor(new Color(255, 255, 255, 200));
            g2.fillOval(sx + 10, sy + 14, 8, 8);
            g2.fillOval(sx + 28, sy + 14, 8, 8);
            g2.setColor(new Color(180, 100, 255));
            g2.fillOval(sx + 12, sy + 16, 4, 4);
            g2.fillOval(sx + 30, sy + 16, 4, 4);
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