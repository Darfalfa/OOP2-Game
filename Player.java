import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import javax.imageio.ImageIO;

/**
 * Player — abstract base class for all playable characters.
 * Handles movement, animation logic, sprite processing, and rendering.
 * Each character subclass (Aya, Ronnix, Jakara) only needs to
 * override loadSprites() to point to their own image files.
 */
public abstract class Player {

    // ─── Dependencies ────────────────────────────────────────────────────────────
    GameScreen gp;
    KeyHandler keyH;

    // ─── Constants ───────────────────────────────────────────────────────────────
    public static final int SPRITE_W         = 64;
    public static final int SPRITE_H         = 80;
    static final int        WHITE_THRESHOLD  = 200;

    // ─── Position & Movement ─────────────────────────────────────────────────────
    public int    x, y;
    public double speed;

    // ─── Standing Sprites ────────────────────────────────────────────────────────
    BufferedImage standingForward;
    BufferedImage standingBackward;
    BufferedImage standingLeft;
    BufferedImage standingRight;
    BufferedImage standingDiagonalUpLeft;
    BufferedImage standingDiagonalUpRight;
    BufferedImage standingDiagonalDownLeft;
    BufferedImage standingDiagonalDownRight;

    // ─── Walking Frame Arrays ────────────────────────────────────────────────────
    BufferedImage[] walkingForwardFrames           = new BufferedImage[21];
    BufferedImage[] walkingRightFrames             = new BufferedImage[28];
    BufferedImage[] walkingLeftFrames              = new BufferedImage[28];
    BufferedImage[] walkingBackwardFrames          = new BufferedImage[28];
    BufferedImage[] walkingDiagonalDownLeftFrames  = new BufferedImage[24];
    BufferedImage[] walkingDiagonalDownRightFrames = new BufferedImage[24];

    // ─── Current Sprite ──────────────────────────────────────────────────────────
    BufferedImage currentSprite;

    // ─── Animation Counters ──────────────────────────────────────────────────────
    int forwardAnimCounter = 0, forwardAnimDelay = 4, forwardAnimFrame = 0;
    int rightAnimCounter   = 0, rightAnimDelay   = 4, rightAnimFrame   = 0;
    int leftAnimCounter    = 0, leftAnimDelay    = 4, leftAnimFrame    = 0;
    int backAnimCounter    = 0, backAnimDelay    = 4, backAnimFrame    = 0;
    int diagDownLeftAnimCounter  = 0, diagDownLeftAnimDelay  = 4, diagDownLeftAnimFrame  = 0;
    int diagDownRightAnimCounter = 0, diagDownRightAnimDelay = 4, diagDownRightAnimFrame = 0;

    // ─── Direction Tracking ──────────────────────────────────────────────────────
    String lastDirection = "down";

    // ─────────────────────────────────────────────────────────────────────────────
    // Constructor
    // ─────────────────────────────────────────────────────────────────────────────
    public Player(GameScreen gp, KeyHandler keyH) {
        this.gp   = gp;
        this.keyH = keyH;
        x     = gp.getWorldWidth()  / 2;
        y     = gp.getWorldHeight() / 2;
        speed = 7;
        loadSprites();
        currentSprite = standingForward;
    }
    // ─────────────────────────────────────────────────────────────────────────────
    // Sprite Loading — each subclass fills in their own image paths
    // ─────────────────────────────────────────────────────────────────────────────
    protected abstract void loadSprites();

    // ─────────────────────────────────────────────────────────────────────────────
    // Sprite Processing — shared utilities available to all subclasses
    // ─────────────────────────────────────────────────────────────────────────────

    /**
     * Removes the background, crops to content, and scales to SPRITE_W × SPRITE_H.
     */
    protected BufferedImage normalizeSprite(BufferedImage img) {
        BufferedImage stripped = removeBackgroundFloodFill(img);

        int minX = stripped.getWidth(),  maxX = 0;
        int minY = stripped.getHeight(), maxY = 0;

        for (int y = 0; y < stripped.getHeight(); y++) {
            for (int x = 0; x < stripped.getWidth(); x++) {
                int alpha = (stripped.getRGB(x, y) >> 24) & 0xFF;
                if (alpha > 0) {
                    if (x < minX) minX = x;
                    if (x > maxX) maxX = x;
                    if (y < minY) minY = y;
                    if (y > maxY) maxY = y;
                }
            }
        }

        if (maxX < minX || maxY < minY)
            return new BufferedImage(SPRITE_W, SPRITE_H, BufferedImage.TYPE_INT_ARGB);

        BufferedImage cropped = stripped.getSubimage(minX, minY, maxX - minX + 1, maxY - minY + 1);
        BufferedImage out = new BufferedImage(SPRITE_W, SPRITE_H, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2 = out.createGraphics();
        g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION,
                RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR);
        g2.drawImage(cropped, 0, 0, SPRITE_W, SPRITE_H, null);
        g2.dispose();
        return out;
    }

    protected BufferedImage loadCharacterSprite(String path) {
        return loadCharacterSprite(path, null);
    }

    protected BufferedImage loadCharacterSprite(String path, BufferedImage fallback) {
        File file = new File(path);
        if (!file.exists()) {
            return fallback != null ? fallback : blankSprite();
        }

        try {
            BufferedImage src = ImageIO.read(file);
            if (src == null) {
                return fallback != null ? fallback : blankSprite();
            }

            BufferedImage argb = new BufferedImage(
                    src.getWidth(), src.getHeight(), BufferedImage.TYPE_INT_ARGB);
            Graphics2D tmp = argb.createGraphics();
            tmp.drawImage(src, 0, 0, null);
            tmp.dispose();

            BufferedImage stripped = removeAutoBackground(argb);
            Rectangle bounds = findOpaqueBounds(stripped);
            if (bounds == null) {
                return fallback != null ? fallback : blankSprite();
            }

            BufferedImage cropped = stripped.getSubimage(
                    bounds.x, bounds.y, bounds.width, bounds.height);

            BufferedImage out = blankSprite();
            Graphics2D g2 = out.createGraphics();
            g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION,
                    RenderingHints.VALUE_INTERPOLATION_BILINEAR);

            int maxW = (int)(SPRITE_W * 0.90);
            int maxH = (int)(SPRITE_H * 0.94);
            double ratio = Math.min((double) maxW / cropped.getWidth(),
                    (double) maxH / cropped.getHeight());
            int drawW = Math.max(1, (int)Math.round(cropped.getWidth() * ratio));
            int drawH = Math.max(1, (int)Math.round(cropped.getHeight() * ratio));
            int drawX = (SPRITE_W - drawW) / 2;
            int drawY = SPRITE_H - drawH - 2;

            g2.drawImage(cropped, drawX, drawY, drawW, drawH, null);
            g2.dispose();
            return out;

        } catch (IOException e) {
            return fallback != null ? fallback : blankSprite();
        }
    }

    private BufferedImage blankSprite() {
        return new BufferedImage(SPRITE_W, SPRITE_H, BufferedImage.TYPE_INT_ARGB);
    }

    private Rectangle findOpaqueBounds(BufferedImage img) {
        int minX = img.getWidth();
        int minY = img.getHeight();
        int maxX = -1;
        int maxY = -1;

        for (int y = 0; y < img.getHeight(); y++) {
            for (int x = 0; x < img.getWidth(); x++) {
                int alpha = (img.getRGB(x, y) >> 24) & 0xFF;
                if (alpha > 8) {
                    if (x < minX) minX = x;
                    if (x > maxX) maxX = x;
                    if (y < minY) minY = y;
                    if (y > maxY) maxY = y;
                }
            }
        }

        if (maxX < minX || maxY < minY) return null;
        return new Rectangle(minX, minY, maxX - minX + 1, maxY - minY + 1);
    }

    private BufferedImage removeAutoBackground(BufferedImage src) {
        int w = src.getWidth();
        int h = src.getHeight();
        BufferedImage dst = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2 = dst.createGraphics();
        g2.drawImage(src, 0, 0, null);
        g2.dispose();

        boolean[][] visited = new boolean[w][h];
        java.util.Queue<int[]> queue = new java.util.LinkedList<>();

        for (int x = 0; x < w; x++) {
            enqueueAutoBackground(dst, x, 0, visited, queue);
            enqueueAutoBackground(dst, x, h - 1, visited, queue);
        }
        for (int y = 1; y < h - 1; y++) {
            enqueueAutoBackground(dst, 0, y, visited, queue);
            enqueueAutoBackground(dst, w - 1, y, visited, queue);
        }

        while (!queue.isEmpty()) {
            int[] px = queue.poll();
            int cx = px[0];
            int cy = px[1];
            dst.setRGB(cx, cy, 0x00000000);

            enqueueAutoBackground(dst, cx - 1, cy, visited, queue);
            enqueueAutoBackground(dst, cx + 1, cy, visited, queue);
            enqueueAutoBackground(dst, cx, cy - 1, visited, queue);
            enqueueAutoBackground(dst, cx, cy + 1, visited, queue);
        }

        return dst;
    }

    private void enqueueAutoBackground(BufferedImage img, int x, int y,
                                       boolean[][] visited,
                                       java.util.Queue<int[]> queue) {
        int w = img.getWidth();
        int h = img.getHeight();
        if (x < 0 || y < 0 || x >= w || y >= h) return;
        if (visited[x][y]) return;
        visited[x][y] = true;

        int argb = img.getRGB(x, y);
        int a = (argb >> 24) & 0xFF;
        int r = (argb >> 16) & 0xFF;
        int g = (argb >> 8) & 0xFF;
        int b = argb & 0xFF;

        boolean transparent = a <= 8;
        boolean nearWhite = r > 190 && g > 190 && b > 190
                && Math.abs(r - g) < 45
                && Math.abs(r - b) < 45
                && Math.abs(g - b) < 45;
        boolean nearBlack = r < 28 && g < 28 && b < 28;
        boolean lavenderDots = b > 170 && r > 100 && g < 150;

        if (transparent || nearWhite || nearBlack || lavenderDots) {
            queue.add(new int[]{x, y});
        }
    }

    protected BufferedImage removeBackgroundFloodFill(BufferedImage src) {
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
            int[][] neighbours = { {cx-1, cy}, {cx+1, cy}, {cx, cy-1}, {cx, cy+1} };
            for (int[] n : neighbours)
                enqueue(dst, n[0], n[1], visited, queue);
        }
        return dst;
    }

    protected void enqueue(BufferedImage img, int x, int y,
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

        boolean isWhite  = (r >= WHITE_THRESHOLD && g >= WHITE_THRESHOLD && b >= WHITE_THRESHOLD);
        boolean isPurple = (b > 180 && r > 100 && g < 120);

        if (a == 0 || isWhite || isPurple)
            queue.add(new int[]{x, y});
    }

    // ─────────────────────────────────────────────────────────────────────────────
    // Update
    // ─────────────────────────────────────────────────────────────────────────────
    public void update() {
        boolean up    = keyH.upPressed;
        boolean down  = keyH.downPressed;
        boolean left  = keyH.leftPressed;
        boolean right = keyH.rightPressed;
        boolean moving = up || down || left || right;

        // ── Movement ──────────────────────────────────────────────────────────────
        int nextX = x;
        int nextY = y;

        if (up)    nextY -= (int) speed;
        if (down)  nextY += (int) speed;
        if (left)  nextX -= (int) speed;
        if (right) nextX += (int) speed;

        if (!gp.isTileCollision(nextX, nextY, SPRITE_W, SPRITE_H)) {
            x = nextX;
            y = nextY;
        } else {
            if (!gp.isTileCollision(nextX, y, SPRITE_W, SPRITE_H)) {
                x = nextX;
            }
            if (!gp.isTileCollision(x, nextY, SPRITE_W, SPRITE_H)) {
                y = nextY;
            }
        }

        // ── World Boundary Clamping ───────────────────────────────────────────────
        if (x < 0) x = 0;
        if (y < 0) y = 0;
        if (x > gp.getWorldWidth()  - SPRITE_W) x = gp.getWorldWidth()  - SPRITE_W;
        if (y > gp.getWorldHeight() - SPRITE_H) y = gp.getWorldHeight() - SPRITE_H;

        // ── Direction Tracking ────────────────────────────────────────────────────
        if      (up   && left)  lastDirection = "upLeft";
        else if (up   && right) lastDirection = "upRight";
        else if (down && left)  lastDirection = "downLeft";
        else if (down && right) lastDirection = "downRight";
        else if (up)            lastDirection = "up";
        else if (down)          lastDirection = "down";
        else if (left)          lastDirection = "left";
        else if (right)         lastDirection = "right";

        // ── Direction Flags ───────────────────────────────────────────────────────
        boolean goingUp        = lastDirection.equals("up");
        boolean goingUpLeft    = lastDirection.equals("upLeft");
        boolean goingUpRight   = lastDirection.equals("upRight");
        boolean goingBackward  = goingUp || goingUpLeft || goingUpRight;
        boolean goingRight     = lastDirection.equals("right");
        boolean goingLeft      = lastDirection.equals("left");

        // ── Animation Counters ────────────────────────────────────────────────────
        if (!moving) {
            forwardAnimCounter = 0; forwardAnimFrame = 0;
            backAnimCounter    = 0; backAnimFrame    = 0;
            rightAnimCounter   = 0; rightAnimFrame   = 0;
            leftAnimCounter    = 0; leftAnimFrame    = 0;
            diagDownLeftAnimCounter  = 0; diagDownLeftAnimFrame  = 0;
            diagDownRightAnimCounter = 0; diagDownRightAnimFrame = 0;
        } else {
            // Reset counters for directions NOT currently active so stale frame
            // indices never exceed the new array's length when direction changes.
            if (!lastDirection.equals("down")) {
                forwardAnimCounter = 0; forwardAnimFrame = 0;
            }
            if (!goingBackward) {
                backAnimCounter = 0; backAnimFrame = 0;
            }
            if (!goingRight) {
                rightAnimCounter = 0; rightAnimFrame = 0;
            }
            if (!goingLeft) {
                leftAnimCounter = 0; leftAnimFrame = 0;
            }
            if (!lastDirection.equals("downLeft")) {
                diagDownLeftAnimCounter = 0; diagDownLeftAnimFrame = 0;
            }
            if (!lastDirection.equals("downRight")) {
                diagDownRightAnimCounter = 0; diagDownRightAnimFrame = 0;
            }

            if (lastDirection.equals("down")) {
                if (++forwardAnimCounter >= forwardAnimDelay) {
                    forwardAnimCounter = 0;
                    forwardAnimFrame = (forwardAnimFrame + 1) % walkingForwardFrames.length;
                }
            }
            if (goingBackward) {
                if (++backAnimCounter >= backAnimDelay) {
                    backAnimCounter = 0;
                    backAnimFrame = (backAnimFrame + 1) % walkingBackwardFrames.length;
                }
            }
            if (goingRight) {
                if (++rightAnimCounter >= rightAnimDelay) {
                    rightAnimCounter = 0;
                    rightAnimFrame = (rightAnimFrame + 1) % walkingRightFrames.length;
                }
            }
            if (goingLeft) {
                if (++leftAnimCounter >= leftAnimDelay) {
                    leftAnimCounter = 0;
                    leftAnimFrame = (leftAnimFrame + 1) % walkingLeftFrames.length;
                }
            }
            if (lastDirection.equals("downLeft") && walkingDiagonalDownLeftFrames != null
                    && walkingDiagonalDownLeftFrames[0] != null) {
                if (++diagDownLeftAnimCounter >= diagDownLeftAnimDelay) {
                    diagDownLeftAnimCounter = 0;
                    diagDownLeftAnimFrame = (diagDownLeftAnimFrame + 1) % walkingDiagonalDownLeftFrames.length;
                }
            }
            if (lastDirection.equals("downRight") && walkingDiagonalDownRightFrames != null
                    && walkingDiagonalDownRightFrames[0] != null) {
                if (++diagDownRightAnimCounter >= diagDownRightAnimDelay) {
                    diagDownRightAnimCounter = 0;
                    diagDownRightAnimFrame = (diagDownRightAnimFrame + 1) % walkingDiagonalDownRightFrames.length;
                }
            }
        }

        // ── Sprite Selection ──────────────────────────────────────────────────────
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
                case "upRight":   currentSprite = walkingBackwardFrames[backAnimFrame];   break;
                case "down":      currentSprite = walkingForwardFrames[forwardAnimFrame]; break;
                case "right":     currentSprite = walkingRightFrames[rightAnimFrame];     break;
                case "left":      currentSprite = walkingLeftFrames[leftAnimFrame];       break;
                case "downLeft":
                    currentSprite = (walkingDiagonalDownLeftFrames != null
                            && walkingDiagonalDownLeftFrames[0] != null)
                            ? walkingDiagonalDownLeftFrames[diagDownLeftAnimFrame]
                            : walkingLeftFrames[leftAnimFrame];
                    break;
                case "downRight":
                    currentSprite = (walkingDiagonalDownRightFrames != null
                            && walkingDiagonalDownRightFrames[0] != null)
                            ? walkingDiagonalDownRightFrames[diagDownRightAnimFrame]
                            : walkingRightFrames[rightAnimFrame];
                    break;
            }
        }
    }

    // ─────────────────────────────────────────────────────────────────────────────
    // Draw
    // ─────────────────────────────────────────────────────────────────────────────
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
