import Characters.Character;
import Characters.Ronnix;
import Characters.Shadow;
import java.awt.*;
import java.awt.event.*;
import java.util.ArrayList;
import java.util.List;
import javax.swing.*;

/**
 * GameScreen — the live game view embedded in the CardLayout window.
 * Contains the game loop, player, camera, world background, and enemies.
 */
public class GameScreen extends JPanel implements Runnable {

    private GameWindow window;

    static final int SCREEN_WIDTH  = GameWindow.WIDTH;
    static final int SCREEN_HEIGHT = GameWindow.HEIGHT;

    final int worldWidth  = WorldBackground.tileW() * 3;
    final int worldHeight = WorldBackground.tileH() * 2;

    private Thread gameThread;
    private KeyHandler keyH = new KeyHandler();
    private Player player;
    private Camera camera;
    private WorldBackground worldBG;

    private List<Enemy> enemies = new ArrayList<>();
    private static final int ENEMY_COUNT = 5;

    // Only store the selected playable battle character here
    private Character playerCharacter;

    private String selectedCharacter;

    private static final Color GOLD       = new Color(201, 150, 58);
    private static final Color GOLD_LIGHT = new Color(240, 192, 96);
    private static final Color GOLD_DARK  = new Color(100, 65, 15);

    private Rectangle menuBtnRect;
    private boolean menuBtnHovered = false;

    private boolean inBattle = false;
    private int postBattleCooldown = 0;

    public GameScreen(GameWindow window) {
        this.window = window;
        setBackground(Color.BLACK);
        setDoubleBuffered(true);
        setFocusable(true);
        addKeyListener(keyH);

        worldBG = new WorldBackground();
        player  = new Player(this, keyH);
        camera  = new Camera(SCREEN_WIDTH, SCREEN_HEIGHT, worldWidth, worldHeight);

        addMouseMotionListener(new MouseMotionAdapter() {
            @Override
            public void mouseMoved(MouseEvent e) {
                boolean prev = menuBtnHovered;
                menuBtnHovered = menuBtnRect != null && menuBtnRect.contains(e.getPoint());
                if (prev != menuBtnHovered) repaint();
                setCursor(menuBtnHovered
                    ? Cursor.getPredefinedCursor(Cursor.HAND_CURSOR)
                    : Cursor.getDefaultCursor());
            }
        });

        addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (menuBtnRect != null && menuBtnRect.contains(e.getPoint())) {
                    stopGame();
                    window.showMainMenu();
                }
            }
        });
    }

    public void setSelectedCharacter(String name) {
        this.selectedCharacter = name;

        switch (name.toLowerCase()) {
            case "ronnix":
                playerCharacter = new Ronnix();
                break;
            // add test case for the other characters
            default:
                playerCharacter = new Ronnix(); // fallback
                break;
        }
        repaint();
    }

    public Character getPlayerCharacter() {
        return playerCharacter;
    }

    public void startGame() {
        player.x = worldWidth / 2;
        player.y = worldHeight / 2;

        if (playerCharacter == null) {
            setSelectedCharacter(selectedCharacter);
        }

        spawnEnemies();

        requestFocusInWindow();
        if (gameThread == null || !gameThread.isAlive()) {
            gameThread = new Thread(this);
            gameThread.setDaemon(true);
            gameThread.start();
        }
    }

    public void stopGame() {
        gameThread = null;
    }

    private void spawnEnemies() {
        enemies.clear();
        for (int i = 0; i < ENEMY_COUNT; i++) {
            int ex, ey;
            do {
                ex = 200 + (int)(Math.random() * (worldWidth - 400));
                ey = 200 + (int)(Math.random() * (worldHeight - 400));
            } while (Math.hypot(ex - player.x, ey - player.y) < 300);

            enemies.add(new Enemy(ex, ey, new Shadow()));
        }
    }

    public void onBattleEnd(Enemy defeated, boolean won) {
        inBattle = false;
        postBattleCooldown = 120; // about 2 seconds

        if (won) {
            defeated.defeated = true;
        } else {
            // Push player away a bit after losing so battle doesn't instantly retrigger
            player.x = Math.max(0, player.x - 100);
            player.y = Math.max(0, player.y - 100);
        }

        SwingUtilities.invokeLater(() -> {
            window.showGameScreen();
            requestFocusInWindow();

            if (gameThread == null || !gameThread.isAlive()) {
                gameThread = new Thread(this);
                gameThread.setDaemon(true);
                gameThread.start();
            }
        });
    }

    @Override
    public void run() {
        final int FPS = 60;
        double drawInterval = 1_000_000_000.0 / FPS;
        double nextDrawTime = System.nanoTime() + drawInterval;

        while (gameThread != null && gameThread == Thread.currentThread()) {
            update();
            repaint();

            try {
                double remaining = (nextDrawTime - System.nanoTime()) / 1_000_000.0;
                if (remaining < 0) remaining = 0;
                Thread.sleep((long) remaining);
                nextDrawTime += drawInterval;
            } catch (InterruptedException ex) {
                Thread.currentThread().interrupt();
            }
        }
    }

    private void update() {
        if (inBattle) return;

        if (postBattleCooldown > 0) {
            postBattleCooldown--;
        }

        player.update();
        camera.update(player.x, player.y, Player.SPRITE_W, Player.SPRITE_H);

        for (Enemy enemy : enemies) {
            if (enemy.defeated) continue;
            enemy.update(worldWidth, worldHeight);

            if (postBattleCooldown == 0 &&
                enemy.isNearPlayer(player.x, player.y, Player.SPRITE_W, Player.SPRITE_H)) {
                triggerBattle(enemy);
                return;
            }
        }
    }

    private void triggerBattle(Enemy enemy) {
        inBattle = true;
        stopGame();

        Character battleEnemy = enemy.character;

        SwingUtilities.invokeLater(() -> window.showBattle(enemy, battleEnemy, this));
    }

    

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2 = (Graphics2D) g;

        worldBG.draw(g2, camera, worldWidth, worldHeight);

        for (Enemy enemy : enemies) {
            enemy.draw(g2, camera);
        }

        player.draw(g2, camera);
        drawHUD(g2);

        g2.dispose();

        
    }

    private void drawHUD(Graphics2D g2) {
        int bW = 160, bH = 36, bX = 16, bY = 16;
        menuBtnRect = new Rectangle(bX, bY, bW, bH);

        if (menuBtnHovered) {
            g2.setColor(new Color(GOLD.getRed(), GOLD.getGreen(), GOLD.getBlue(), 35));
            g2.fillRoundRect(bX - 5, bY - 5, bW + 10, bH + 10, 6, 6);
        }

        g2.setColor(menuBtnHovered ? new Color(80, 45, 10, 220) : new Color(18, 9, 5, 180));
        g2.fillRect(bX, bY, bW, bH);
        g2.setColor(menuBtnHovered ? GOLD_LIGHT : GOLD_DARK);
        g2.setStroke(new BasicStroke(menuBtnHovered ? 2f : 1.5f));
        g2.drawRect(bX, bY, bW, bH);

        g2.setFont(new Font("Serif", Font.BOLD, 13));
        FontMetrics fm = g2.getFontMetrics();
        String lbl = "◄  MAIN MENU";
        int tx = bX + (bW - fm.stringWidth(lbl)) / 2;
        int ty = bY + (bH + fm.getAscent() - fm.getDescent()) / 2;
        g2.setColor(new Color(0, 0, 0, 180));
        g2.drawString(lbl, tx + 2, ty + 2);
        g2.setColor(menuBtnHovered ? GOLD_LIGHT : GOLD);
        g2.drawString(lbl, tx, ty);

        int baseY = getHeight() - 120;

        if (playerCharacter != null) {

            // LEVEL
            g2.setFont(new Font("Serif", Font.BOLD, 14));
            g2.setColor(GOLD_LIGHT);
            g2.drawString("LV " + playerCharacter.getLevel(), 16, baseY);

            // HP
            drawHudBar(g2, 16, baseY + 10, 200, "HP",
                    playerCharacter.getHp(), playerCharacter.getMaxHp(),
                    new Color(60, 180, 80));

            // DEF
            drawHudBar(g2, 16, baseY + 36, 200, "DEF",
                    playerCharacter.getDefense(), playerCharacter.getMaxDefense(),
                    new Color(80, 120, 220));

            // EXP (NEW)
            drawHudBar(g2, 16, baseY + 62, 200, "EXP",
                    playerCharacter.getCurrentXp(),
                    playerCharacter.getNextLevelXp(),
                    new Color(220, 180, 60));
        }
    }

    private void drawHudBar(Graphics2D g2, int x, int y, int w,
                            String label, int cur, int max, Color fill) {
        int h = 20;
        double pct = max <= 0 ? 0 : (double) cur / max;

        g2.setColor(new Color(10, 5, 15, 200));
        g2.fillRoundRect(x, y, w, h, h, h);

        int fw = (int)(w * pct);
        if (fw > 0) {
            g2.setColor(fill);
            g2.fillRoundRect(x, y, fw, h, h, h);
        }

        g2.setColor(GOLD_DARK);
        g2.setStroke(new BasicStroke(1f));
        g2.drawRoundRect(x, y, w, h, h, h);

        g2.setFont(new Font("Serif", Font.BOLD, 12));
        g2.setColor(GOLD_LIGHT);
        g2.drawString(label + ": " + cur + "/" + max, x + 8, y + h - 4);
    }

    public int getWorldWidth()  { return worldWidth; }
    public int getWorldHeight() { return worldHeight; }
}