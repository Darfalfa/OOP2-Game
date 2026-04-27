import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.util.ArrayList;
import java.util.List;

/**
 * GameScreen — gameplay panel adapted to your current project structure.
 * Uses map.txt + tiledata.txt + tile images from your tiles folder.
 */
public class GameScreen extends JPanel implements Runnable {

    private GameWindow window;

    public static final int SCREEN_WIDTH  = GameWindow.WIDTH;
    public static final int SCREEN_HEIGHT = GameWindow.HEIGHT;

    // tile settings
    final int originalTileSize = 16;
    final int scale = 3;
    public final int tileSize = originalTileSize * scale; // 48

    // world settings
    public int maxWorldCol = 50;
    public int maxWorldRow = 50;
    int worldWidth;
    int worldHeight;

    private Thread gameThread;
    private final KeyHandler keyH = new KeyHandler();
    private Player player;
    private Camera camera;
    private TileManager tileM;

    // enemies
    private final List<Enemy> enemies = new ArrayList<>();
    private static final int ENEMY_COUNT = 5;

    // player stats
    public int playerHp    = 100;
    public int playerMaxHp = 100;
    public int playerMp    = 40;
    public int playerMaxMp = 40;
    public int playerAtk   = 15;
    public int playerDef   = 5;

    private String selectedCharacter = "Ronnix";

    // hud
    private static final Color GOLD       = new Color(201, 150, 58);
    private static final Color GOLD_LIGHT = new Color(240, 192, 96);
    private static final Color GOLD_DARK  = new Color(100, 65, 15);

    private Rectangle menuBtnRect;
    private boolean menuBtnHovered = false;

    private boolean inBattle = false;

    public GameScreen(GameWindow window) {
        this.window = window;
        setBackground(Color.BLACK);
        setDoubleBuffered(true);
        setFocusable(true);
        addKeyListener(keyH);

        tileM = new TileManager(this, "/maps/World_2.tmx");
        keyH.setTileManager(tileM);
        worldWidth = tileSize * maxWorldCol;
        worldHeight = tileSize * maxWorldRow;
        camera = new Camera(SCREEN_WIDTH, SCREEN_HEIGHT, worldWidth, worldHeight);

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
        applyCharacterStats();
    }

    private void applyCharacterStats() {
        if ("Ronnix".equalsIgnoreCase(selectedCharacter)) {
            playerMaxHp = 140;
            playerHp = 140;
            playerMaxMp = 20;
            playerMp = 20;
            playerAtk = 22;
            playerDef = 10;
        } else if ("Jakara".equalsIgnoreCase(selectedCharacter)) {
            playerMaxHp = 75;
            playerHp = 75;
            playerMaxMp = 100;
            playerMp = 100;
            playerAtk = 26;
            playerDef = 2;
        } else if ("Aya".equalsIgnoreCase(selectedCharacter) || "Archer".equalsIgnoreCase(selectedCharacter)) {
            playerMaxHp = 100;
            playerHp = 100;
            playerMaxMp = 45;
            playerMp = 45;
            playerAtk = 18;
            playerDef = 5;
        } else {
            playerMaxHp = 140;
            playerHp = 140;
            playerMaxMp = 20;
            playerMp = 20;
            playerAtk = 22;
            playerDef = 10;
            selectedCharacter = "Ronnix";
        }
    }

    public boolean isTileCollision(int x, int y, int width, int height) {
        int leftCol   = Math.max(0, x / tileSize);
        int rightCol  = Math.min(maxWorldCol - 1, (x + width - 1) / tileSize);
        int topRow    = Math.max(0, y / tileSize);
        int bottomRow = Math.min(maxWorldRow - 1, (y + height - 1) / tileSize);

        for (int col = leftCol; col <= rightCol; col++) {
            for (int row = topRow; row <= bottomRow; row++) {
                int tileNum = tileM.mapTileNum[col][row];
                if (tileNum >= 0 && tileNum < tileM.tile.length) {
                    Tile tile = tileM.tile[tileNum];
                    if (tile != null && tile.collision) {
                        return true;
                    }
                }

                if (tileM.collisionMap != null && tileM.collisionMap[col][row]) {
                    return true;
                }
            }
        }

        return false;
    }

    public void startGame() {
        new SwingWorker<Player, Void>() {
            @Override
            protected Player doInBackground() {
                if ("Jakara".equalsIgnoreCase(selectedCharacter)) {
                    return new Jakara(GameScreen.this, keyH);
                } else if ("Aya".equalsIgnoreCase(selectedCharacter) || "Archer".equalsIgnoreCase(selectedCharacter)) {
                    return new Aya(GameScreen.this, keyH);
                }
                return new Ronnix(GameScreen.this, keyH);
            }

            @Override
            protected void done() {
                try {
                    player = get();
                } catch (Exception e) {
                    e.printStackTrace();
                    return;
                }

                player.x = worldWidth / 2;
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
            int ex, ey;
            do {
                ex = 80 + (int)(Math.random() * Math.max(1, worldWidth - 160));
                ey = 80 + (int)(Math.random() * Math.max(1, worldHeight - 160));
            } while (player != null && Math.hypot(ex - player.x, ey - player.y) < 200);

            enemies.add(new Enemy(ex, ey));
        }
    }

    public void onBattleEnd(Enemy defeated, int newHp, int newMp, boolean won) {
        playerHp = newHp;
        playerMp = newMp;
        inBattle = false;

        if (won) {
            defeated.defeated = true;
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
        if (inBattle || player == null) return;

        player.update();
        camera.update(player.x, player.y, Player.SPRITE_W, Player.SPRITE_H);

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

        SwingUtilities.invokeLater(() -> window.showBattle(enemy, this));
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2 = (Graphics2D) g;

        tileM.draw(g2, camera);

        for (Enemy enemy : enemies) {
            enemy.draw(g2, camera);
        }

        if (player != null) {
            player.draw(g2, camera);
        }

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

        drawHudBar(g2, 16, SCREEN_HEIGHT - 70, 200, "HP", playerHp, playerMaxHp, new Color(60, 180, 80));
        drawHudBar(g2, 16, SCREEN_HEIGHT - 44, 200, "MP", playerMp, playerMaxMp, new Color(80, 120, 220));
    }

    private void drawHudBar(Graphics2D g2, int x, int y, int w, String label, int cur, int max, Color fill) {
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

    public int getWorldWidth() {
        return worldWidth;
    }

    public int getWorldHeight() {
        return worldHeight;
    }
}