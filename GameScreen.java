import Characters.*;
import Characters.Character;
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
    private KeyHandler keyH;
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
    private Rectangle buyHealthBtn = null;
    private Rectangle sellHealthBtn = null;
    private Rectangle buyExpBtn = null;
    private Rectangle sellExpBtn = null;
    private Rectangle exitBtn;
    private Rectangle hoveredBtn = null;

    // SHOP IMAGES
    private Image shopBG;
    private Image healthImg;
    private Image expImg;
    private Image coinImg;
    private Image makoImg;
    private Image makoBlinkImg;
    private Image itemCardImg;
    // BUTTON IMAGES
    private Image buyBtnImg;
    private Image buyBtnHoverImg;
    private Image exitBtnImg;


    private boolean inBattle = false;
    private int postBattleCooldown = 0;

    // SHOP SYSTEM
    private boolean shopDialogueOpen = false;
    private boolean shopOpen = false;
    private boolean makoBlink = false;
    private int blinkTimer = 0;

    private int selectedOption = 0; // 0 = Shop, 1 = Close

    // Prices
    private final int HEALTH_PRICE = 5;
    private final int EXP_PRICE = 5;

    public GameScreen(GameWindow window) {
        this.window = window;
        setBackground(Color.BLACK);
        setDoubleBuffered(true);
        setFocusable(true);

        keyH = new KeyHandler(this);
        addKeyListener(keyH);

        worldBG = new WorldBackground();
        player  = new Player(this, keyH);
        camera  = new Camera(SCREEN_WIDTH, SCREEN_HEIGHT, worldWidth, worldHeight);

        try {
            shopBG = new ImageIcon("images/Shop_UI.png").getImage();
            healthImg = new ImageIcon("images/Health_potion.png").getImage();
            expImg = new ImageIcon("images/EXP_potion.png").getImage();
            coinImg = new ImageIcon("images/Coin.png").getImage();
            makoImg = new ImageIcon("images/Mako.png").getImage();
            makoBlinkImg = new ImageIcon("images/MakoBlink.png").getImage();
            buyBtnImg = new ImageIcon("images/Shop_Button_Buy_Placeholder.png").getImage();
            buyBtnHoverImg = new ImageIcon("images/Shop_Button_Clicked_Buy_Placeholder.png").getImage();
            exitBtnImg = new ImageIcon("images/shop_button_exit_placeholder.png").getImage();
            itemCardImg = new ImageIcon("images/Shop_Item_Placeholder.png").getImage();
        } catch (Exception e) {
            e.printStackTrace();
        }

        addMouseMotionListener(new MouseMotionAdapter() {
            @Override
            public void mouseMoved(MouseEvent e) {
                boolean prev = menuBtnHovered;
                menuBtnHovered = menuBtnRect != null && menuBtnRect.contains(e.getPoint());
                if (prev != menuBtnHovered) repaint();
                setCursor(menuBtnHovered
                    ? Cursor.getPredefinedCursor(Cursor.HAND_CURSOR)
                    : Cursor.getDefaultCursor());

                if (shopOpen) {
                    Point p = e.getPoint();
                    hoveredBtn = null;

                    if (buyHealthBtn != null && buyHealthBtn.contains(p)) hoveredBtn = buyHealthBtn;
                    else if (sellHealthBtn != null && sellHealthBtn.contains(p)) hoveredBtn = sellHealthBtn;
                    else if (buyExpBtn != null && buyExpBtn.contains(p)) hoveredBtn = buyExpBtn;
                    else if (sellExpBtn != null && sellExpBtn.contains(p)) hoveredBtn = sellExpBtn;
                    else if (exitBtn != null && exitBtn.contains(p)) hoveredBtn = exitBtn;

                    repaint();
                }
            }
        });

        addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (menuBtnRect != null && menuBtnRect.contains(e.getPoint())) {
                    stopGame();
                    window.showMainMenu();
                }

                if (shopOpen) {

                    Point p = e.getPoint();

                    // BUY HEALTH
                    if (buyHealthBtn != null && buyHealthBtn.contains(p)) {
                        if (playerCharacter.getGold() >= HEALTH_PRICE) {
                            playerCharacter.addGold(-HEALTH_PRICE);
                            playerCharacter.setHealthPotion(
                                    playerCharacter.getHealthPotion() + 1
                            );
                        }
                    }

                    // SELL HEALTH
                    if (sellHealthBtn != null && sellHealthBtn.contains(p)) {
                        if (playerCharacter.getHealthPotion() > 0) {
                            playerCharacter.setHealthPotion(
                                    playerCharacter.getHealthPotion() - 1
                            );
                            playerCharacter.addGold(HEALTH_PRICE);
                        }
                    }

                    // BUY EXP
                    if (buyExpBtn != null && buyExpBtn.contains(p)) {
                        if (playerCharacter.getGold() >= EXP_PRICE) {
                            playerCharacter.addGold(-EXP_PRICE);
                            playerCharacter.setExpPotion(
                                    playerCharacter.getExpPotion() + 1
                            );
                        }
                    }

                    // SELL EXP
                    if (sellExpBtn != null && sellExpBtn.contains(p)) {
                        if (playerCharacter.getExpPotion() > 0) {
                            playerCharacter.setExpPotion(
                                    playerCharacter.getExpPotion() - 1
                            );
                            playerCharacter.addGold(EXP_PRICE);
                        }
                    }

                    // EXIT
                    if (exitBtn != null && exitBtn.contains(p)) {
                        shopOpen = false;
                    }

                    repaint();
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
            case "aya":
                playerCharacter = new Aya();
                break;
            case "jakara":
                playerCharacter = new Jakara();
                break;
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

        blinkTimer++;

        if (blinkTimer > 120) { // every ~2 seconds
            makoBlink = !makoBlink;
            blinkTimer = 0;
        }
    }

    private void triggerBattle(Enemy enemy) {
        inBattle = true;
        stopGame();

        Character battleEnemy = enemy.character;

        SwingUtilities.invokeLater(() -> window.showBattle(enemy, battleEnemy, this));
    }

    public void openShopDialogue() {
        if (!shopOpen && !shopDialogueOpen) {
            shopDialogueOpen = true;
            selectedOption = 0;
            repaint();
        }
    }

    public void handleShopInput(int keyCode) {
        if (playerCharacter == null) return;
        // ===== DIALOGUE =====
        if (shopDialogueOpen) {
            if (keyCode == KeyEvent.VK_UP || keyCode == KeyEvent.VK_DOWN) {
                selectedOption = 1 - selectedOption;
            }
            if (keyCode == KeyEvent.VK_ENTER) {
                if (selectedOption == 0) {
                    shopDialogueOpen = false;
                    shopOpen = true;
                } else {
                    shopDialogueOpen = false;
                }
            }
        }
        // ===== SHOP =====
        else if (shopOpen) {
            // BUY HEALTH
            if (keyCode == KeyEvent.VK_1) {
                if (playerCharacter.getGold() >= HEALTH_PRICE) {
                    playerCharacter.addGold(-HEALTH_PRICE);
                    playerCharacter.setHealthPotion(
                            playerCharacter.getHealthPotion() + 1
                    );
                }
            }
            // BUY EXP
            if (keyCode == KeyEvent.VK_2) {
                if (playerCharacter.getGold() >= EXP_PRICE) {
                    playerCharacter.addGold(-EXP_PRICE);
                    playerCharacter.setExpPotion(
                            playerCharacter.getExpPotion() + 1
                    );
                }
            }
            // SELL HEALTH
            if (keyCode == KeyEvent.VK_Q) {
                if (playerCharacter.getHealthPotion() > 0) {
                    playerCharacter.setHealthPotion(
                            playerCharacter.getHealthPotion() - 1
                    );
                    playerCharacter.addGold(HEALTH_PRICE);
                }
            }
            // SELL EXP
            if (keyCode == KeyEvent.VK_W) {
                if (playerCharacter.getExpPotion() > 0) {
                    playerCharacter.setExpPotion(
                            playerCharacter.getExpPotion() - 1
                    );
                    playerCharacter.addGold(EXP_PRICE);
                }
            }
            // EXIT SHOP
            if (keyCode == KeyEvent.VK_ESCAPE) {
                shopOpen = false;
            }
        }
        repaint();
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

        // DRAW SHOP DIALOGUE
        if (shopDialogueOpen) {
            drawShopDialogue(g2);
        }

        // DRAW SHOP
        if (shopOpen) {
            drawShop(g2);
        }

        g2.dispose();

        
    }

    private void drawShopDialogue(Graphics2D g2) {
        int w = 600;
        int h = 200;

        int x = (getWidth() - w) / 2;
        int y = getHeight() - h - 40;

        g2.setColor(new Color(20, 20, 40, 230));
        g2.fillRoundRect(x, y, w, h, 20, 20);

        g2.setColor(Color.WHITE);
        g2.drawRoundRect(x, y, w, h, 20, 20);

        g2.setFont(new Font("Arial", Font.PLAIN, 16));

        g2.drawString("*Yawn* Yeah ... welcome to the tavern.", x + 20, y + 40);
        g2.drawString("If you're here for potions,", x + 20, y + 70);
        g2.drawString("they're on the table. *yawn*", x + 20, y + 100);

        g2.setFont(new Font("Arial", Font.BOLD, 18));

        g2.setColor(selectedOption == 0 ? Color.YELLOW : Color.WHITE);
        g2.drawString("Shop", x + 120, y + 150);

        g2.setColor(selectedOption == 1 ? Color.YELLOW : Color.WHITE);
        g2.drawString("Close", x + 240, y + 150);
    }

    private void drawShop(Graphics2D g2) {

        if (playerCharacter == null) return;

        int screenW = getWidth();
        int screenH = getHeight();

        // ===== BACKGROUND =====
        g2.drawImage(shopBG, 0, 0, screenW, screenH, null);

        // ===== MAKO =====
        Image makoToDraw = makoBlink ? makoBlinkImg : makoImg;
        g2.drawImage(makoToDraw, 60, screenH - 510, 350, 350, null);

        // ===== GOLD =====
        g2.setColor(Color.WHITE);
        g2.setFont(new Font("Arial", Font.BOLD, 20));
        g2.drawImage(coinImg, 20, 20, 30, 30, null);
        g2.drawString(String.valueOf(playerCharacter.getGold()), 60, 45);

        // ===== ITEM CARDS =====
        int cardY = 120;

        // CARD SETTINGS
        int cardWidth = 260;
        int spacing = 40;

        // TOTAL WIDTH of both cards
        int totalWidth = (cardWidth * 2) + spacing;

        // START POSITION (adjust this to move left/right)
        int startX = (screenW - totalWidth) / 2 + 120;

        // FINAL POSITIONS
        int leftX = startX;
        int rightX = startX + cardWidth + spacing;

        drawItemCard(g2, leftX, cardY, true);
        drawItemCard(g2, rightX, cardY, false);

        // ===== EXIT BUTTON =====
        exitBtn = new Rectangle(160, screenH - 100, 150, 60);
        g2.drawImage(exitBtnImg, exitBtn.x, exitBtn.y, exitBtn.width, exitBtn.height, null);

        g2.setFont(new Font("Arial", Font.BOLD, 16));
        g2.setColor(Color.WHITE);
        g2.drawString("EXIT", exitBtn.x + 50, exitBtn.y + 35);
    }

    private void drawItemCard(Graphics2D g2, int x, int y, boolean isHealth) {

        int w = 260;
        int h = 360;

        // Card background
        g2.drawImage(itemCardImg, x, y, w, h, null);

        Image icon = isHealth ? healthImg : expImg;

        int owned = isHealth
                ? playerCharacter.getHealthPotion()
                : playerCharacter.getExpPotion();

        int price = isHealth ? HEALTH_PRICE : EXP_PRICE;

        String name = isHealth ? "Health Potion" : "EXP Potion";
        String desc = isHealth
                ? "Restores 50 HP\nPerfect for survival."
                : "Grants 50 EXP\nBoost progression.";

        // ICON
        g2.drawImage(icon, x + 80, y + 50, 80, 80, null);

        // NAME
        g2.setFont(new Font("Arial", Font.BOLD, 16));
        g2.setColor(Color.WHITE);
        FontMetrics fm = g2.getFontMetrics();
        int nameX = x + (w - fm.stringWidth(name)) / 2;
        g2.drawString(name, nameX, y + 155);

        // DESCRIPTION
        g2.setFont(new Font("Arial", Font.PLAIN, 12));
        drawMultiline(g2, desc, x + 40, y + 180);

        // Price (left side)
        g2.drawImage(coinImg, x + 40, y + 300, 20, 20, null);
        g2.drawString(String.valueOf(price), x + 70, y + 315);

        // Owned (right side)
        g2.drawString("Owned: " + owned, x + 165, y + 315);

        // BUTTONS
        Rectangle buy = new Rectangle(x + 20, y + 400, 220, 45);
        Rectangle sell = new Rectangle(x + 20, y + 450, 220, 45);

        if (isHealth) {
            buyHealthBtn = buy;
            sellHealthBtn = sell;
        } else {
            buyExpBtn = buy;
            sellExpBtn = sell;
        }

        drawButton(g2, buy, "BUY");
        drawButton(g2, sell, "SELL");
    }

    private void drawMultiline(Graphics2D g2, String text, int x, int y) {
        for (String line : text.split("\n")) {
            g2.drawString(line, x, y);
            y += 15;
        }
    }

    private void drawButton(Graphics2D g2, Rectangle rect, String text) {

        Image img = (rect == hoveredBtn) ? buyBtnHoverImg : buyBtnImg;

        g2.drawImage(img, rect.x, rect.y, rect.width, rect.height, null);

        g2.setColor(Color.WHITE);
        g2.setFont(new Font("Arial", Font.BOLD, 14));

        FontMetrics fm = g2.getFontMetrics();
        int tx = rect.x + (rect.width - fm.stringWidth(text)) / 2;
        int ty = rect.y + (rect.height + fm.getAscent()) / 2 - 4;

        g2.drawString(text, tx, ty);
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