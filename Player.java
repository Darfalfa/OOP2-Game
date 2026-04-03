import javax.imageio.ImageIO;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;

public class Player {

    GameScreen gp;
    KeyHandler keyH;

    public static final int SPRITE_W = 64;
    public static final int SPRITE_H = 80;

    public int x, y;
    public double speed;

    // Standing sprites
    BufferedImage standingForward;
    BufferedImage standingBackward;
    BufferedImage standingLeft;
    BufferedImage standingRight;
    BufferedImage standingDiagonalUpLeft;
    BufferedImage standingDiagonalUpRight;
    BufferedImage standingDiagonalDownLeft;
    BufferedImage standingDiagonalDownRight;

    // Walking frames
    BufferedImage[] walkingForwardFrames  = new BufferedImage[21];
    BufferedImage[] walkingRightFrames    = new BufferedImage[28];
    BufferedImage[] walkingLeftFrames     = new BufferedImage[28];
    BufferedImage[] walkingBackwardFrames = new BufferedImage[24];

    BufferedImage currentSprite;

    // Animation counters
    int forwardAnimCounter = 0, forwardAnimDelay = 5, forwardAnimFrame = 0;
    int rightAnimCounter   = 0, rightAnimDelay   = 5, rightAnimFrame   = 0;
    int leftAnimCounter    = 0, leftAnimDelay     = 5, leftAnimFrame    = 0;
    int backAnimCounter    = 0, backAnimDelay     = 5, backAnimFrame    = 0;

    // Direction tracking
    String lastDirection = "down";

    public Player(GameScreen gp, KeyHandler keyH) {
        this.gp   = gp;
        this.keyH = keyH;
        x     = gp.getWorldWidth()  / 1;
        y     = gp.getWorldHeight() / 1;
        speed = 2;
        loadSprites();
        currentSprite = standingForward;
    }

    private static final int WHITE_THRESHOLD = 200;

    private BufferedImage normalizeSprite(BufferedImage img) {
        BufferedImage stripped = removeBackgroundFloodFill(img);
        BufferedImage out = new BufferedImage(SPRITE_W, SPRITE_H, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2 = out.createGraphics();
        g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION,
                            RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR);
        g2.drawImage(stripped, 0, 0, SPRITE_W, SPRITE_H, null);
        g2.dispose();
        return out;
    }

    private BufferedImage removeBackgroundFloodFill(BufferedImage src) {
        int w = src.getWidth();
        int h = src.getHeight();
        BufferedImage dst = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
        dst.getGraphics().drawImage(src, 0, 0, null);
        boolean[][] visited = new boolean[w][h];
        java.util.Queue<int[]> queue = new java.util.LinkedList<>();
        for (int x = 0; x < w; x++) {
            enqueue(dst, x, 0,     visited, queue);
            enqueue(dst, x, h - 1, visited, queue);
        }
        for (int y = 1; y < h - 1; y++) {
            enqueue(dst, 0,     y, visited, queue);
            enqueue(dst, w - 1, y, visited, queue);
        }
        while (!queue.isEmpty()) {
            int[] px = queue.poll();
            int cx = px[0], cy = px[1];
            dst.setRGB(cx, cy, 0x00000000);
            int[][] neighbours = {{cx-1,cy},{cx+1,cy},{cx,cy-1},{cx,cy+1}};
            for (int[] n : neighbours) {
                enqueue(dst, n[0], n[1], visited, queue);
            }
        }
        return dst;
    }

    private void enqueue(BufferedImage img, int x, int y,
                         boolean[][] visited, java.util.Queue<int[]> queue) {
        int w = img.getWidth(), h = img.getHeight();
        if (x < 0 || y < 0 || x >= w || y >= h) return;
        if (visited[x][y]) return;
        visited[x][y] = true;
        int argb = img.getRGB(x, y);
        int a = (argb >> 24) & 0xFF;
        int r = (argb >> 16) & 0xFF;
        int g = (argb >>  8) & 0xFF;
        int b =  argb        & 0xFF;
        if (a == 0 || (r >= WHITE_THRESHOLD && g >= WHITE_THRESHOLD && b >= WHITE_THRESHOLD)) {
            queue.add(new int[]{x, y});
        }
    }

    private void loadSprites() {
        try {
            standingForward           = normalizeSprite(ImageIO.read(new File("images/standingForward.png")));
            standingBackward          = normalizeSprite(ImageIO.read(new File("images/standingBackward.png")));
            standingLeft              = normalizeSprite(ImageIO.read(new File("images/standingLeft.png")));
            standingRight             = normalizeSprite(ImageIO.read(new File("images/standingRight.png")));
            standingDiagonalUpLeft    = normalizeSprite(ImageIO.read(new File("images/standingDiagonalUpLeft.png")));
            standingDiagonalUpRight   = normalizeSprite(ImageIO.read(new File("images/standingDiagonalUpRight.png")));
            standingDiagonalDownLeft  = normalizeSprite(ImageIO.read(new File("images/standingDiagonalDownLeft.png")));
            standingDiagonalDownRight = normalizeSprite(ImageIO.read(new File("images/standingDiagonalDownRight.png")));

            for (int i = 1; i <= 21; i++)
                walkingForwardFrames[i - 1] = normalizeSprite(
                    ImageIO.read(new File(String.format("images/walkForward_f%02d.png", i))));

            for (int i = 1; i <= 28; i++)
                walkingRightFrames[i - 1] = normalizeSprite(
                    ImageIO.read(new File(String.format("images/walkRight_f%02d.png", i))));

            for (int i = 1; i <= 28; i++)
                walkingLeftFrames[i - 1] = normalizeSprite(
                    ImageIO.read(new File(String.format("images/walkLeft_f%02d.png", i))));

            for (int i = 1; i <= 24; i++)
                walkingBackwardFrames[i - 1] = normalizeSprite(
                    ImageIO.read(new File(String.format("images/walkBackward_f%02d.png", i))));

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void update() {

        boolean up    = keyH.upPressed;
        boolean down  = keyH.downPressed;
        boolean left  = keyH.leftPressed;
        boolean right = keyH.rightPressed;

        boolean moving = up || down || left || right;

        if (up)    y -= speed;
        if (down)  y += speed;
        if (left)  x -= speed;
        if (right) x += speed;

        if (x < 0)                               x = 0;
        if (y < 0)                               y = 0;
        if (x > gp.getWorldWidth()  - SPRITE_W)  x = gp.getWorldWidth()  - SPRITE_W;
        if (y > gp.getWorldHeight() - SPRITE_H)  y = gp.getWorldHeight() - SPRITE_H;

        if      (up && left)    lastDirection = "upLeft";
        else if (up && right)   lastDirection = "upRight";
        else if (down && left)  lastDirection = "downLeft";
        else if (down && right) lastDirection = "downRight";
        else if (up)            lastDirection = "up";
        else if (down)          lastDirection = "down";
        else if (left)          lastDirection = "left";
        else if (right)         lastDirection = "right";

        boolean goingUp    = lastDirection.equals("up")
                          || lastDirection.equals("upLeft")
                          || lastDirection.equals("upRight");
        boolean goingRight = lastDirection.equals("right")
                          || lastDirection.equals("upRight")
                          || lastDirection.equals("downRight");
        boolean goingLeft  = lastDirection.equals("left")
                          || lastDirection.equals("upLeft")
                          || lastDirection.equals("downLeft");

        // Forward animation
        if (moving && lastDirection.equals("down")) {
            if (++forwardAnimCounter >= forwardAnimDelay) {
                forwardAnimCounter = 0;
                forwardAnimFrame = (forwardAnimFrame + 1) % 21;
            }
        } else if (!moving) { forwardAnimCounter = 0; forwardAnimFrame = 0; }

        // Backward animation
        if (moving && goingUp) {
            if (++backAnimCounter >= backAnimDelay) {
                backAnimCounter = 0;
                backAnimFrame = (backAnimFrame + 1) % 24;
            }
        } else if (!moving) { backAnimCounter = 0; backAnimFrame = 0; }

        // Right animation
        if (moving && goingRight) {
            if (++rightAnimCounter >= rightAnimDelay) {
                rightAnimCounter = 0;
                rightAnimFrame = (rightAnimFrame + 1) % 28;
            }
        } else if (!moving) { rightAnimCounter = 0; rightAnimFrame = 0; }

        // Left animation
        if (moving && goingLeft) {
            if (++leftAnimCounter >= leftAnimDelay) {
                leftAnimCounter = 0;
                leftAnimFrame = (leftAnimFrame + 1) % 28;
            }
        } else if (!moving) { leftAnimCounter = 0; leftAnimFrame = 0; }

        // Sprite selection
        if (!moving) {
            switch (lastDirection) {
                case "up":        currentSprite = standingBackward;          break;
                case "down":      currentSprite = standingForward;           break;
                case "left":      currentSprite = standingLeft;              break;
                case "right":     currentSprite = standingRight;             break;
                case "upLeft":    currentSprite = standingDiagonalUpLeft;    break;
                case "upRight":   currentSprite = standingDiagonalUpRight;   break;
                case "downLeft":  currentSprite = standingDiagonalDownLeft;  break;
                case "downRight": currentSprite = standingDiagonalDownRight; break;
            }
        } else {
            switch (lastDirection) {
                case "up":
                case "upLeft":
                case "upRight":
                    currentSprite = walkingBackwardFrames[backAnimFrame];   break;
                case "down":
                    currentSprite = walkingForwardFrames[forwardAnimFrame]; break;
                case "right":
                case "downRight":
                    currentSprite = walkingRightFrames[rightAnimFrame];     break;
                case "left":
                case "downLeft":
                    currentSprite = walkingLeftFrames[leftAnimFrame];       break;
            }
        }
    }

    public void draw(Graphics2D g2, Camera cam) {
        g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION,
                            RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR);
        g2.drawImage(currentSprite,
                     x - cam.offsetX(),
                     y - cam.offsetY(),
                     SPRITE_W, SPRITE_H, null);
    }

    public void draw(Graphics2D g2) {
        g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION,
                            RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR);
        g2.drawImage(currentSprite, x, y, SPRITE_W, SPRITE_H, null);
    }
}