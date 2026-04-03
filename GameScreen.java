import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.util.ArrayList;
import java.util.List;

/**
 * GameScreen — the live game view embedded in the CardLayout window.
 * Contains the game loop, player, camera, world background, and enemies.
 */
public class GameScreen extends JPanel implements Runnable {

    private GameWindow window;

    // ── Screen constants ─────────────────────────────────────────────────────
    static final int SCREEN_WIDTH  = GameWindow.WIDTH;
    static final int SCREEN_HEIGHT = GameWindow.HEIGHT;

    // ── World size (3×2 background tiles) ───────────────────────────────────
    final int worldWidth  = WorldBackground.tileW() * 3;
    final int worldHeight = WorldBackground.tileH() * 2;

    // ── Systems ──────────────────────────────────────────────────────────────
    private Thread          gameThread;
    private KeyHandler      keyH = new KeyHandler();
    private Player          player;   // set to Aya / Ronnix / Jakara in startGame()
    private Camera          camera;
    private WorldBackground worldBG;

    // ── Enemies ──────────────────────────────────────────────────────────────
    private List<Enemy> enemies = new ArrayList<>();
    private static final int ENEMY_COUNT = 5;

    // ── Player persistent stats ───────────────────────────────────────────────
    public int playerHp    = 100;
    public int playerMaxHp = 100;
    public int playerMp    = 40;
    public int playerMaxMp = 40;
    public int playerAtk   = 15;
    public int playerDef   = 5;

    // Which character was chosen on the select screen
    private String selectedCharacter = "Archer";

    /** Called by GameWindow after the player picks a character. */
    public void setSelectedCharacter(String name) {
        this.selectedCharacter = name;
        applyCharacterStats();
    }

    private void applyCharacterStats() {
        if ("Ronnix".equalsIgnoreCase(selectedCharacter)) {
            // Ronnix — Fighter: high HP, high ATK, high DEF, low MP
            playerMaxHp = 140;  playerHp = 140;
            playerMaxMp = 20;   playerMp = 20;
            playerAtk   = 22;
            playerDef   = 10;
        } else if ("Jakara".equalsIgnoreCase(selectedCharacter)) {
            // Jakara — Arcane Mage: low HP, huge MP, very high ATK, very low DEF
            playerMaxHp = 75;   playerHp = 75;
            playerMaxMp = 100;  playerMp = 100;
            playerAtk   = 26;
            playerDef   = 2;
        } else {
            // Archer (default): balanced HP, good MP, moderate ATK/DEF
            playerMaxHp = 100;  playerHp = 100;
            playerMaxMp = 40;   playerMp = 40;
            playerAtk   = 15;
            playerDef   = 5;
        }
    }

    // ── HUD ──────────────────────────────────────────────────────────────────
    private static final Color GOLD       = new Color(201, 150, 58);
    private static final Color GOLD_LIGHT = new Color(240, 192, 96);
    private static final Color GOLD_DARK  = new Color(100, 65, 15);

    private Rectangle menuBtnRect;
    private boolean   menuBtnHovered = false;

    // ── Battle flag ──────────────────────────────────────────────────────────
    private boolean inBattle = false;

    public GameScreen(GameWindow window) {
        this.window = window;
        setBackground(Color.BLACK);
        setDoubleBuffered(true);
        setFocusable(true);
        addKeyListener(keyH);

        worldBG = new WorldBackground();
        // player is created in startGame() once selectedCharacter is known
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

    // ── Lifecycle ─────────────────────────────────────────────────────────────
    public void startGame() {
        // Load sprites on a background thread so the EDT (and loading screen)
        // never freezes during disk I/O. Once done, switch to the game on the EDT.
        new javax.swing.SwingWorker<Player, Void>() {
            @Override
            protected Player doInBackground() {
                // Heavy work runs off the EDT
                if ("Ronnix".equalsIgnoreCase(selectedCharacter)) {
                    return new Ronnix(GameScreen.this, keyH);
                } else if ("Jakara".equalsIgnoreCase(selectedCharacter)) {
                    return new Jakara(GameScreen.this, keyH);
                } else {
                    return new Aya(GameScreen.this, keyH);
                }
            }

            @Override
            protected void done() {
                // Back on the EDT — safe to update UI and start the game loop
                try {
                    player = get();
                } catch (Exception e) {
                    e.printStackTrace();
                    return;
                }
                player.x = worldWidth  / 2;
                player.y = worldHeight / 2;
                applyCharacterStats();
                spawnEnemies();
                window.showGameScreen();
                requestFocusInWindow();
                if (gameThread == null || !gameThread.isAlive()) {
                    gameThread = new Thread(GameScreen.this);
                    gameThread.setDaemon(true);
                    gameThread.start();
                }
            }
        }.execute();
    }

    public void stopGame() {
        gameThread = null;
    }

    private void spawnEnemies() {
        enemies.clear();
        for (int i = 0; i < ENEMY_COUNT; i++) {
            // Spawn away from player start position
            int ex, ey;
            do {
                ex = 200 + (int)(Math.random() * (worldWidth  - 400));
                ey = 200 + (int)(Math.random() * (worldHeight - 400));
            } while (Math.hypot(ex - player.x, ey - player.y) < 300);
            enemies.add(new Enemy(ex, ey));
        }
    }

    /** Called by BattleScreen when the battle finishes. */
    public void onBattleEnd(Enemy defeated, int newHp, int newMp, boolean won) {
        playerHp = newHp;
        playerMp = newMp;
        inBattle = false;

        if (won) {
            defeated.defeated = true;
        }

        // Return focus to game
        SwingUtilities.invokeLater(() -> {
            window.showGameScreen();
            requestFocusInWindow();
            // Resume loop
            if (gameThread == null || !gameThread.isAlive()) {
                gameThread = new Thread(this);
                gameThread.setDaemon(true);
                gameThread.start();
            }
        });
    }

    // ── Game loop ─────────────────────────────────────────────────────────────
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

        player.update();
        camera.update(player.x, player.y, Player.SPRITE_W, Player.SPRITE_H);

        // Update enemies and check for encounter
        for (Enemy enemy : enemies) {
            if (enemy.defeated) continue;
            enemy.update(worldWidth, worldHeight);

            if (enemy.isNearPlayer(player.x, player.y, Player.SPRITE_W, Player.SPRITE_H)) {
                triggerBattle(enemy);
                return;
            }
        }
    }

    private void triggerBattle(Enemy enemy) {
        inBattle = true;
        stopGame();

        SwingUtilities.invokeLater(() -> {
            window.showBattle(enemy, this);
        });
    }

    // ── Render ────────────────────────────────────────────────────────────────
    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2 = (Graphics2D) g;

        worldBG.draw(g2, camera, worldWidth, worldHeight);

        // Draw enemies
        for (Enemy enemy : enemies) {
            enemy.draw(g2, camera);
        }

        player.draw(g2, camera);
        drawHUD(g2);

        g2.dispose();
    }

    private void drawHUD(Graphics2D g2) {
        // Main Menu button
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

        // Player HP/MP bars (bottom-left HUD)
        drawHudBar(g2, 16, SCREEN_HEIGHT - 70, 200, "HP",
                   playerHp, playerMaxHp, new Color(60, 180, 80));
        drawHudBar(g2, 16, SCREEN_HEIGHT - 44, 200, "MP",
                   playerMp, playerMaxMp, new Color(80, 120, 220));
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

    // Expose world size so Player can reference via GameScreen
    public int getWorldWidth()  { return worldWidth;  }
    public int getWorldHeight() { return worldHeight; }
}