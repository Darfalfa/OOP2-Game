import Characters.*;
import Characters.Character;
import java.awt.*;
import java.awt.event.*;
import java.util.ArrayList;
import java.util.List;
import javax.swing.*;

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
    final int worldWidth = tileSize * maxWorldCol;
    final int worldHeight = tileSize * maxWorldRow;

    private Thread gameThread;
    private KeyHandler keyH;
    private Player player;
    private Camera camera;
    private TileManager tileM;

    // enemies
    private final List<Enemy> enemies = new ArrayList<>();
    private static final int ENEMY_COUNT = 10;

    private Character playerCharacter;

    private String selectedCharacter;


    // hud
    private static final Color GOLD       = new Color(201, 150, 58);
    private static final Color GOLD_LIGHT = new Color(240, 192, 96);
    private static final Color GOLD_DARK  = new Color(100, 65, 15);

    private Rectangle menuBtnRect;
    private boolean menuBtnHovered = false;

    private boolean infoOpen = false;

    private Rectangle buyHealthBtn = null;
    private Rectangle sellHealthBtn = null;
    private Rectangle buyExpBtn = null;
    private Rectangle sellExpBtn = null;
    private Rectangle exitBtn;
    private Rectangle hoveredBtn = null;
    private Rectangle expPotionBtn;

    // SHOP IMAGES
    private Image shopBG;
    private Image makoChatboxImg;
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
    private boolean wensDialogueOpen = false;
    private boolean wensDialogueSeen = false;
    private Image wensDialogueImg;

    private boolean khaiDialogueOpen = false;
    private Image khaiDialogueImg;
    
    // Feedback message shown at the top of the shop (not blocking buttons)
    private String shopFeedback = "";
    private int shopFeedbackTimer = 0;
    private static final int FEEDBACK_DURATION = 120; // 2 seconds at 60 FPS

    private int selectedOption = 0; // 0 = Shop, 1 = Close

    // Dialogue mouse hit-rects and hover state
    private Rectangle dialogueShopBtn  = null;
    private Rectangle dialogueCloseBtn = null;
    private int dialogueHovered = -1; // -1=none, 0=Shop, 1=Close

    // Prices
    private final int HEALTH_PRICE = 5;
    private final int EXP_PRICE = 5;

    private String shopMessage = "";
    private int shopMessageTimer = 0;
    private static final int MESSAGE_DURATION = 120;
    private boolean shopMessageIsError = true;

    

    public GameScreen(GameWindow window) {
        this.window = window;
        setBackground(Color.BLACK);
        setDoubleBuffered(true);
        setFocusable(true);
        
        keyH = new KeyHandler(this);
        addKeyListener(keyH);

        camera = new Camera(SCREEN_WIDTH, SCREEN_HEIGHT, worldWidth, worldHeight);
        tileM = new TileManager(this);

        try {
            shopBG = new ImageIcon("images/Shop_UI.png").getImage();
            makoChatboxImg = new ImageIcon("images/Mako_Dialogue_Box.png").getImage();
            healthImg = new ImageIcon("images/Health_potion.png").getImage();
            expImg = new ImageIcon("images/EXP_potion.png").getImage();
            coinImg = new ImageIcon("images/Coin.png").getImage();
            makoImg = new ImageIcon("images/Mako.png").getImage();
            makoBlinkImg = new ImageIcon("images/MakoBlink.png").getImage();
            buyBtnImg = new ImageIcon("images/Shop_Button_Buy_Placeholder.png").getImage();
            buyBtnHoverImg = new ImageIcon("images/Shop_Button_Clicked_Buy_Placeholder.png").getImage();
            exitBtnImg = new ImageIcon("images/shop_button_exit_placeholder.png").getImage();
            itemCardImg = new ImageIcon("images/Shop_Item_Placeholder.png").getImage();
            wensDialogueImg = new ImageIcon("images/Wens_dialogue.png").getImage();
            khaiDialogueImg = new ImageIcon("images/Khai_dialogue.png").getImage();
        } catch (Exception e) {
            e.printStackTrace();
        }

        addMouseMotionListener(new MouseMotionAdapter() {
            @Override
            public void mouseMoved(MouseEvent e) {
                Point p = e.getPoint();

                // ── Dialogue hover ────────────────────────────────────────────
                if (shopDialogueOpen) {
                    int prevDH = dialogueHovered;
                    dialogueHovered = -1;
                    if (dialogueShopBtn  != null && dialogueShopBtn.contains(p))  dialogueHovered = 0;
                    else if (dialogueCloseBtn != null && dialogueCloseBtn.contains(p)) dialogueHovered = 1;
                    if (prevDH != dialogueHovered) repaint();
                    setCursor(dialogueHovered >= 0
                        ? Cursor.getPredefinedCursor(Cursor.HAND_CURSOR)
                        : Cursor.getDefaultCursor());
                    return;
                }

                // ── Menu button hover ─────────────────────────────────────────
                boolean prev = menuBtnHovered;
                menuBtnHovered = menuBtnRect != null && menuBtnRect.contains(p);
                if (prev != menuBtnHovered) repaint();

                // ── Shop button hover ─────────────────────────────────────────
                if (shopOpen) {
                    Rectangle prevHov = hoveredBtn;
                    hoveredBtn = null;
                    if      (buyHealthBtn  != null && buyHealthBtn.contains(p))  hoveredBtn = buyHealthBtn;
                    else if (sellHealthBtn != null && sellHealthBtn.contains(p)) hoveredBtn = sellHealthBtn;
                    else if (buyExpBtn     != null && buyExpBtn.contains(p))     hoveredBtn = buyExpBtn;
                    else if (sellExpBtn    != null && sellExpBtn.contains(p))    hoveredBtn = sellExpBtn;
                    else if (exitBtn       != null && exitBtn.contains(p))       hoveredBtn = exitBtn;
                    if (prevHov != hoveredBtn) repaint();
                    setCursor(hoveredBtn != null
                        ? Cursor.getPredefinedCursor(Cursor.HAND_CURSOR)
                        : Cursor.getDefaultCursor());
                    return;
                }

                setCursor(menuBtnHovered
                    ? Cursor.getPredefinedCursor(Cursor.HAND_CURSOR)
                    : Cursor.getDefaultCursor());
            }
        });

        addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
            Point p = e.getPoint();
    if (wensDialogueOpen) {
        wensDialogueOpen = false;
        return;
    }
    if (khaiDialogueOpen) {
        khaiDialogueOpen = false;
        return;
    }

    if (menuBtnRect != null && menuBtnRect.contains(e.getPoint())) {
        stopGame();
        window.showMainMenu();
    }

                // ── Dialogue clicks ───────────────────────────────────────────
                if (shopDialogueOpen) {
                    if (dialogueShopBtn != null && dialogueShopBtn.contains(p)) {
                        SoundManager.stopSfx(); 
                        shopDialogueOpen = false;
                        shopOpen = true;
                        dialogueHovered = -1;
                        repaint();
                        return;
                    }
                    if (dialogueCloseBtn != null && dialogueCloseBtn.contains(p)) {
                        SoundManager.stopSfx(); 
                        shopDialogueOpen = false;
                        dialogueHovered = -1;
                        repaint();
                        return;
                    }
                }

                if (shopOpen) {

// BUY HEALTH
if (buyHealthBtn != null && buyHealthBtn.contains(p)) {
    if (playerCharacter.getGold() >= HEALTH_PRICE) {
        playerCharacter.addGold(-HEALTH_PRICE);
        playerCharacter.setHealthPotion(playerCharacter.getHealthPotion() + 1);
        showShopFeedback("Health Potion purchased!");
    } else {
        showShopFeedback("Not enough coins!");
    }
}

// SELL HEALTH
if (sellHealthBtn != null && sellHealthBtn.contains(p)) {
    if (playerCharacter.getHealthPotion() > 0) {
        playerCharacter.setHealthPotion(playerCharacter.getHealthPotion() - 1);
        playerCharacter.addGold(HEALTH_PRICE);
        showShopFeedback("Health Potion sold!");
    } else {
        showShopFeedback("No potions to sell!");
    }
}

// BUY EXP
if (buyExpBtn != null && buyExpBtn.contains(p)) {
    if (playerCharacter.getGold() >= EXP_PRICE) {
        playerCharacter.addGold(-EXP_PRICE);
        playerCharacter.setExpPotion(playerCharacter.getExpPotion() + 1);
        showShopFeedback("EXP Potion purchased!");
    } else {
        showShopFeedback("Not enough coins!");
    }
}

// SELL EXP
if (sellExpBtn != null && sellExpBtn.contains(p)) {
    if (playerCharacter.getExpPotion() > 0) {
        playerCharacter.setExpPotion(playerCharacter.getExpPotion() - 1);
        playerCharacter.addGold(EXP_PRICE);
        showShopFeedback("EXP Potion sold!");
    } else {
        showShopFeedback("No potions to sell!");
    }
}

                    // EXIT
                    if (exitBtn != null && exitBtn.contains(p)) {
                        SoundManager.stopSfx();
                        shopOpen = false;
                        hoveredBtn = null;
                    }
                    repaint();
                }

                // ── EXP Potion HUD button ─────────────────────────────────────
                if (expPotionBtn != null && expPotionBtn.contains(p)) {
                    if (playerCharacter.getExpPotion() > 0) {
                        String result = playerCharacter.useExpPotion();
                        System.out.println(result);
                    } else {
                        System.out.println("No EXP potions!");
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
                playerCharacter = new RonnixLogic();
                player = new Ronnix(this, keyH);
                break;
            case "aya":
                playerCharacter = new AyaLogic();
                player = new Aya(this, keyH);
                break;
            case "jakara":
                playerCharacter = new JakaraLogic();
                player = new Jakara(this, keyH);
                break;
        }

        repaint();
    }

    public Character getPlayerCharacter() {
        return playerCharacter;
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
            }
        }

        return false;
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

            enemies.add(new Enemy(ex, ey, new ShadowLogic()));
        }
    }

public void onBattleEnd(Enemy defeated, boolean won) {
        inBattle = false;
        postBattleCooldown = 120; // about 2 seconds

        // Reset all held keys so the player doesn't auto-walk after returning
        keyH.upPressed    = false;
        keyH.downPressed  = false;
        keyH.leftPressed  = false;
        keyH.rightPressed = false;

        if (won) {
    defeated.defeated = true;
    defeated.respawnTimer = 300; // ~5 seconds

    if (!wensDialogueSeen && playerCharacter != null && playerCharacter.getLevel() >= 4) {
        wensDialogueOpen = true;
        wensDialogueSeen = true;
    }
} else {
    // Push player away a bit after losing so battle doesn't instantly retrigger
    player.x = Math.max(0, player.x - 100);
    player.y = Math.max(0, player.y - 100);
}

        SwingUtilities.invokeLater(() -> {
            window.showGameScreen();

            GameScreen.this.requestFocusInWindow();

            GameScreen.this.requestFocus();

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

            // ===== RESPAWN LOGIC =====
            if (enemy.defeated) {

                enemy.respawnTimer--;

                if (enemy.respawnTimer <= 0) {

                    enemy.defeated = false;

                    // restore enemy stats
                    enemy.character.restoreStats();

                    // respawn in random location (not near player)
                    do {
                        enemy.x = 200 + (int)(Math.random() * (worldWidth - 400));
                        enemy.y = 200 + (int)(Math.random() * (worldHeight - 400));
                    } while (Math.hypot(enemy.x - player.x, enemy.y - player.y) < 250);
                }

                continue;
            }

            // ===== NORMAL BEHAVIOR =====
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
        if (shopFeedbackTimer > 0) shopFeedbackTimer--;
    }

    private void triggerBattle(Enemy enemy) {
        inBattle = true;
        stopGame();

        Character battleEnemy = enemy.character;

        SwingUtilities.invokeLater(() -> window.showBattle(enemy, battleEnemy, this));
    }

    private void showShopFeedback(String msg) {
    shopFeedback = msg;
    shopFeedbackTimer = FEEDBACK_DURATION;
}

    //public void openShopDialogue() {
    //    if (!shopOpen && !shopDialogueOpen) {
    //        shopDialogueOpen = true;
    //        selectedOption = 0;
    //        repaint();
    //    }
    //}

public void openShopDialogue() {
    if (!shopOpen && !shopDialogueOpen && !wensDialogueOpen && !khaiDialogueOpen) {
        shopDialogueOpen = true;
        selectedOption = 0;

        SoundManager.playSfx("Mako.wav");

        repaint();
    }
}

public void openWensDialogue() {
    if (!shopDialogueOpen && !shopOpen && !khaiDialogueOpen) {
        wensDialogueOpen = true;
        repaint();
    }
}

public void openKhaiDialogue() {
    if (!shopDialogueOpen && !shopOpen && !wensDialogueOpen) {
        khaiDialogueOpen = true;
        repaint();
    }
}

    public void handleShopInput(int keyCode) {
        if (playerCharacter == null) return;
        // ===== DIALOGUE — only Escape closes it (mouse handles Shop/Close) =====
        if (shopDialogueOpen) {
            if (keyCode == KeyEvent.VK_ESCAPE) {
                shopDialogueOpen = false;
                repaint();
            }
        }
        // ===== SHOP =====
        else if (shopOpen) {
            // BUY HEALTH
// BUY HEALTH
if (keyCode == KeyEvent.VK_1) {
    if (playerCharacter.getGold() >= HEALTH_PRICE) {
        playerCharacter.addGold(-HEALTH_PRICE);
        playerCharacter.setHealthPotion(playerCharacter.getHealthPotion() + 1);
        showShopFeedback("Health Potion purchased!");
    } else {
        showShopFeedback("Not enough coins!");
    }
}
// BUY EXP
if (keyCode == KeyEvent.VK_2) {
    if (playerCharacter.getGold() >= EXP_PRICE) {
        playerCharacter.addGold(-EXP_PRICE);
        playerCharacter.setExpPotion(playerCharacter.getExpPotion() + 1);
        showShopFeedback("EXP Potion purchased!");
    } else {
        showShopFeedback("Not enough coins!");
    }
}
// SELL HEALTH
if (keyCode == KeyEvent.VK_Q) {
    if (playerCharacter.getHealthPotion() > 0) {
        playerCharacter.setHealthPotion(playerCharacter.getHealthPotion() - 1);
        playerCharacter.addGold(HEALTH_PRICE);
        showShopFeedback("Health Potion sold!");
    } else {
        showShopFeedback("No potions to sell!");
    }
}
// SELL EXP
if (keyCode == KeyEvent.VK_W) {
    if (playerCharacter.getExpPotion() > 0) {
        playerCharacter.setExpPotion(playerCharacter.getExpPotion() - 1);
        playerCharacter.addGold(EXP_PRICE);
        showShopFeedback("EXP Potion sold!");
    } else {
        showShopFeedback("No potions to sell!");
    }
}
// EXIT SHOP
if (keyCode == KeyEvent.VK_ESCAPE) {
    shopOpen = false;
}
        }
        repaint();
    }

public void handleInfoInput(int keyCode) {
        if (keyCode == KeyEvent.VK_I) {
            infoOpen = !infoOpen;
        }
        
        if (keyCode == KeyEvent.VK_R) {
            openKhaiDialogue();
        }
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

        if (shopDialogueOpen) {
    drawShopDialogue(g2);
}

if (wensDialogueOpen) {
    drawWensDialogue(g2);
}

if (khaiDialogueOpen) {
    drawKhaiDialogue(g2);
}

// DRAW SHOP
if (shopOpen) {
    drawShop(g2);
}

        if (infoOpen) {
            drawInfoWindow(g2);
        }

        g2.dispose();
    }

    private void drawShopDialogue(Graphics2D g2) {
        int screenW = getWidth();
        int screenH = getHeight();

        // ── Render the chatbox image at a fixed comfortable size ──────────────
        // Natural image ratio: 1108 × 468.
        // We render at 760px wide so it fits on 1280px screens without crowding.
        int imgW = 760;
        int imgH = (int)(imgW * 468.0 / 1108.0); // ≈ 321px

        // Centre horizontally, anchor near the bottom of the screen
        int imgX = (screenW - imgW) / 2;
        int imgY = screenH - imgH - 40;

        // Draw the chatbox image
        if (makoChatboxImg != null) {
            g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION,
                                RenderingHints.VALUE_INTERPOLATION_BILINEAR);
            g2.drawImage(makoChatboxImg, imgX, imgY, imgW, imgH, null);
        } else {
            // Fallback plain box
            g2.setColor(new Color(20, 16, 42, 235));
            g2.fillRoundRect(imgX, imgY, imgW, imgH, 14, 14);
            g2.setColor(new Color(90, 100, 150));
            g2.setStroke(new BasicStroke(2f));
            g2.drawRoundRect(imgX, imgY, imgW, imgH, 14, 14);
        }

        // ── Text area coordinates (derived from pixel analysis of image) ──────
        // Portrait occupies left 30.69% of image width  → x=0..340 of 1108
        // Text box starts at x≈340, top y≈148, bottom y≈460 (of 468)
        // In rendered space:
        int portraitEndX = imgX + (int)(imgW * 340.0 / 1108.0); // ~233px from imgX
        int boxTopY      = imgY + (int)(imgH * 148.0 / 468.0);  // ~101px from imgY
        int boxBotY      = imgY + (int)(imgH * 460.0 / 468.0);  // ~315px from imgY
        int boxH         = boxBotY - boxTopY;                    // ~214px

        int textX    = portraitEndX + 18;
        int textMaxW = imgX + imgW - textX - 16;

        // ── Dialogue text ─────────────────────────────────────────────────────
        // Sits in the top ~55% of the box, below the name-tag area (~20% of boxH)
        int textStartY = boxTopY + (int)(boxH * 0.20);
        int lineGap    = (int)(boxH * 0.14);

        g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING,
                            RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        g2.setFont(new Font("Serif", Font.PLAIN, 15));
        g2.setColor(new Color(215, 210, 235));
        g2.drawString("*Yawn* Yeah ... welcome to the tavern.", textX, textStartY);
        g2.drawString("If you're here for potions,",            textX, textStartY + lineGap);
        g2.drawString("they're on the table. *yawn*",           textX, textStartY + lineGap * 2);

        // ── Option buttons — bottom 28% of the box ───────────────────────────
        int btnW = 130;
        int btnH = (int)(boxH * 0.24);
        int btnY = boxBotY - btnH - (int)(boxH * 0.06);

        // Spread buttons inside the text area
        int textAreaW  = imgX + imgW - textX - 16;
        int shopBtnX   = textX + (int)(textAreaW * 0.04);
        int closeBtnX  = textX + (int)(textAreaW * 0.36);

        // Register hit-rects for mouse clicks
        dialogueShopBtn  = new Rectangle(shopBtnX,  btnY, btnW, btnH);
        dialogueCloseBtn = new Rectangle(closeBtnX, btnY, btnW, btnH);

        drawDialogueButton(g2, "Shop",  shopBtnX,  btnY, btnW, btnH, dialogueHovered == 0,
                           new Color(210, 170, 60));
        drawDialogueButton(g2, "Close", closeBtnX, btnY, btnW, btnH, dialogueHovered == 1,
                           new Color(200, 195, 220));
    }

    private void drawDialogueButton(Graphics2D g2, String label,
                                     int x, int y, int w, int h,
                                     boolean hovered, Color accent) {
        // Only show design when actually hovered — no default highlight
        if (hovered) {
            // Outer glow
            g2.setColor(new Color(accent.getRed(), accent.getGreen(), accent.getBlue(), 60));
            g2.fillRoundRect(x - 6, y - 6, w + 12, h + 12, 12, 12);

            // Button background
            g2.setColor(new Color(50, 38, 12, 210));
            g2.fillRoundRect(x, y, w, h, 8, 8);

            // Border
            g2.setColor(accent);
            g2.setStroke(new BasicStroke(2f));
            g2.drawRoundRect(x, y, w, h, 8, 8);

            // Arrow indicator
            int ax = x + 10, ay = y + h / 2;
            int[] xp = {ax, ax + 7, ax};
            int[] yp = {ay - 5, ay, ay + 5};
            g2.setColor(accent);
            g2.fillPolygon(xp, yp, 3);
        } else {
            // Not hovered — just transparent dark backing so text is readable
            g2.setColor(new Color(15, 12, 30, 140));
            g2.fillRoundRect(x, y, w, h, 8, 8);
        }

        // Label
        g2.setFont(new Font("Serif", Font.BOLD, 15));
        FontMetrics fm = g2.getFontMetrics();
        int tx = x + (w - fm.stringWidth(label)) / 2;
        int ty = y + (h + fm.getAscent() - fm.getDescent()) / 2;

        // Shadow
        g2.setColor(new Color(0, 0, 0, 160));
        g2.drawString(label, tx + 1, ty + 1);
        // Main text — gold when hovered, soft white otherwise
        g2.setColor(hovered ? accent : new Color(200, 195, 220));
        g2.drawString(label, tx, ty);
    }

    /*private void drawWensDialogue(Graphics2D g2) {
    int screenW = getWidth();
    int screenH = getHeight();

    int boxW = (int)(screenW * 0.80);
    int boxH = (int)(screenH * 0.38);
    int boxX = (screenW - boxW) / 2;
    int boxY = screenH - boxH - 30;

    if (wensDialogueImg != null) {
        g2.drawImage(wensDialogueImg, boxX, boxY, boxW, boxH, null);
    }

    int textX = boxX + (int)(boxW * 0.33);
    int textY = boxY + (int)(boxH * 0.30);
    int textMaxW = (int)(boxW * 0.63);

    String line = "Traveler, you've proven your strength. From here on, you won't walk alone. "
                + "I am your companion now\u2014ready to face whatever trials await us.";

    g2.setFont(new Font("Arial", Font.PLAIN, 16));
    g2.setColor(new Color(220, 210, 190));
    drawWensWrappedText(g2, line, textX, textY, textMaxW);

    g2.setFont(new Font("Arial", Font.ITALIC, 12));
    g2.setColor(new Color(180, 180, 180, 200));
    g2.drawString("Click to continue", boxX + boxW - 130, boxY + boxH - 14);
}
*/

private void drawWensDialogue(Graphics2D g2) {
    int screenW = getWidth();
    int screenH = getHeight();

    // Exact same dimensions as Mako's dialogue box
    int imgW = 760;
    int imgH = (int)(imgW * 468.0 / 1108.0); // ~321px — same ratio as makoChatboxImg

    int imgX = (screenW - imgW) / 2;
    int imgY = screenH - imgH - 40;

    // Draw Wens' dialogue image at the same size as Mako's
    if (wensDialogueImg != null) {
        g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION,
                            RenderingHints.VALUE_INTERPOLATION_BILINEAR);
        g2.drawImage(wensDialogueImg, imgX, imgY, imgW, imgH, null);
    } else {
        // Fallback plain box (same as Mako's fallback)
        g2.setColor(new Color(20, 16, 42, 235));
        g2.fillRoundRect(imgX, imgY, imgW, imgH, 14, 14);
        g2.setColor(new Color(90, 100, 150));
        g2.setStroke(new BasicStroke(2f));
        g2.drawRoundRect(imgX, imgY, imgW, imgH, 14, 14);
    }

// Wens' image: portrait takes left ~38%, text box starts after that
    int portraitEndX = imgX + (int)(imgW * 0.38);
    int boxTopY      = imgY + (int)(imgH * 148.0 / 468.0);
    int boxBotY      = imgY + (int)(imgH * 460.0 / 468.0);
    int boxH         = boxBotY - boxTopY;

    int textX    = portraitEndX + 12;
    int textMaxW = imgX + imgW - textX - 16;

    // Same font as Mako's dialogue text
    int textStartY = boxTopY + (int)(boxH * 0.20);
    int lineGap    = (int)(boxH * 0.14);

    g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING,
                        RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
    g2.setFont(new Font("Serif", Font.PLAIN, 15));
    g2.setColor(new Color(215, 210, 235));
    g2.drawString("Traveler, you've proven your strength.", textX, textStartY);
    g2.drawString("From here on, you won't walk alone.",   textX, textStartY + lineGap);
    g2.drawString("I am your companion now\u2014ready to face whatever trials await us.", textX, textStartY + lineGap * 2);

    // Same "Click to continue" style as Mako's box
    g2.setFont(new Font("Serif", Font.ITALIC, 13));
    g2.setColor(new Color(180, 170, 200, 200));
    g2.drawString("Click to continue", imgX + imgW - 148, boxBotY - 6);
}

private void drawKhaiDialogue(Graphics2D g2) {
    int screenW = getWidth();
    int screenH = getHeight();

    int imgW = 760;
    int imgH = (int)(imgW * 468.0 / 1108.0);

    int imgX = (screenW - imgW) / 2;
    int imgY = screenH - imgH - 40;

    if (khaiDialogueImg != null) {
        g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION,
                            RenderingHints.VALUE_INTERPOLATION_BILINEAR);
        g2.drawImage(khaiDialogueImg, imgX, imgY, imgW, imgH, null);
    } else {
        g2.setColor(new Color(20, 16, 42, 235));
        g2.fillRoundRect(imgX, imgY, imgW, imgH, 14, 14);
        g2.setColor(new Color(90, 100, 150));
        g2.setStroke(new BasicStroke(2f));
        g2.drawRoundRect(imgX, imgY, imgW, imgH, 14, 14);
    }

    int portraitEndX = imgX + (int)(imgW * 0.38);
    int boxTopY      = imgY + (int)(imgH * 148.0 / 468.0);
    int boxBotY      = imgY + (int)(imgH * 460.0 / 468.0);
    int boxH         = boxBotY - boxTopY;

    int textX    = portraitEndX + 12;
    int lineGap  = (int)(boxH * 0.14);
    int textStartY = boxTopY + (int)(boxH * 0.18);

    g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING,
                        RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
    g2.setFont(new Font("Serif", Font.PLAIN, 15));
    g2.setColor(new Color(215, 210, 235));
    g2.drawString("So\u2026 you\u2019ve finally made it this far.",          textX, textStartY);
    g2.drawString("You\u2019re stronger now\u2014strong enough to face what lies ahead.", textX, textStartY + lineGap);
    g2.drawString("The Boss Dungeon is open to you.",                         textX, textStartY + lineGap * 2);
    g2.drawString("\u2026Go on. Step inside.",                                textX, textStartY + lineGap * 3);
    g2.drawString("I\u2019ll be waiting.",                                    textX, textStartY + lineGap * 4);

    g2.setFont(new Font("Serif", Font.ITALIC, 13));
    g2.setColor(new Color(180, 170, 200, 200));
    g2.drawString("Click to continue", imgX + imgW - 148, boxBotY - 6);
}

private void drawWensWrappedText(Graphics2D g2, String text, int x, int y, int maxWidth) {
    FontMetrics fm = g2.getFontMetrics();
    int lineHeight = fm.getHeight() + 2;
    String[] words = text.split(" ");
    String line = "";

    for (String word : words) {
        String test = line + word + " ";
        if (fm.stringWidth(test) > maxWidth && !line.isEmpty()) {
            g2.drawString(line.trim(), x, y);
            line = word + " ";
            y += lineHeight;
        } else {
            line = test;
        }
    }
    if (!line.isEmpty()) g2.drawString(line.trim(), x, y);
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
        // ===== FEEDBACK MESSAGE (top-centre, never overlaps buttons) =====
if (shopFeedbackTimer > 0 && !shopFeedback.isEmpty()) {
    float alpha = Math.min(1f, shopFeedbackTimer / 30f);
    boolean isError = shopFeedback.contains("Not enough") || shopFeedback.contains("No potion");
    Color bgColor  = isError ? new Color(180, 30, 30, (int)(200 * alpha))
                             : new Color(30, 130, 60,  (int)(200 * alpha));
    Color txtColor = new Color(255, 255, 255, (int)(255 * alpha));

    g2.setFont(new Font("Arial", Font.BOLD, 16));
    FontMetrics fm2 = g2.getFontMetrics();
    int msgW = fm2.stringWidth(shopFeedback) + 32;
    int msgH = 34;
    int msgX = (screenW - msgW) / 2;
    int msgY = 14;

    g2.setColor(bgColor);
    g2.fillRoundRect(msgX, msgY, msgW, msgH, msgH, msgH);
    g2.setColor(txtColor);
    g2.drawString(shopFeedback,
            msgX + 16,
            msgY + (msgH + fm2.getAscent() - fm2.getDescent()) / 2);
}

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
        // ===== FEEDBACK MESSAGE =====
if (shopMessageTimer > 0 && !shopMessage.isEmpty()) {
    float alpha = Math.min(1f, shopMessageTimer / 30f);
    int a = (int)(alpha * 255);

    g2.setFont(new Font("Arial", Font.BOLD, 16));
    FontMetrics fmMsg = g2.getFontMetrics();
    int msgW = fmMsg.stringWidth(shopMessage) + 32;
    int msgH = 38;
    int msgX = (screenW - msgW) / 2;
    int msgY = screenH - 160;

    Color bgCol = shopMessageIsError
        ? new Color(120, 20, 20, Math.min(a, 210))
        : new Color(20, 90, 20, Math.min(a, 210));
    g2.setColor(bgCol);
    g2.fillRoundRect(msgX, msgY, msgW, msgH, msgH, msgH);

    Color borderCol = shopMessageIsError
        ? new Color(220, 80, 80, a)
        : new Color(80, 200, 80, a);
    g2.setColor(borderCol);
    g2.setStroke(new BasicStroke(1.5f));
    g2.drawRoundRect(msgX, msgY, msgW, msgH, msgH, msgH);
    g2.setStroke(new BasicStroke(1f));

    g2.setColor(new Color(0, 0, 0, Math.min(a, 160)));
    g2.drawString(shopMessage, msgX + 17, msgY + (msgH + fmMsg.getAscent() - fmMsg.getDescent()) / 2 + 1);
    g2.setColor(new Color(255, 255, 255, a));
    g2.drawString(shopMessage, msgX + 16, msgY + (msgH + fmMsg.getAscent() - fmMsg.getDescent()) / 2);
}
        // ===== EXIT BUTTON =====
exitBtn = new Rectangle(160, screenH - 100, 150, 60);
boolean exitHov = hoveredBtn != null && hoveredBtn.equals(exitBtn);

// Outer glow on hover
if (exitHov) {
    g2.setColor(new Color(160, 80, 220, 75));
    g2.fillRoundRect(exitBtn.x - 6, exitBtn.y - 6,
                     exitBtn.width + 12, exitBtn.height + 12, 12, 12);
}

g2.drawImage(exitBtnImg, exitBtn.x, exitBtn.y, exitBtn.width, exitBtn.height, null);

// Bright overlay + border on hover
if (exitHov) {
    g2.setColor(new Color(255, 255, 255, 40));
    g2.fillRoundRect(exitBtn.x, exitBtn.y, exitBtn.width, exitBtn.height, 6, 6);
    g2.setColor(new Color(210, 160, 255, 220));
    g2.setStroke(new BasicStroke(2f));
    g2.drawRoundRect(exitBtn.x, exitBtn.y, exitBtn.width, exitBtn.height, 6, 6);
    g2.setStroke(new BasicStroke(1f));
}

// Text with drop shadow
g2.setFont(new Font("Arial", Font.BOLD, 16));
FontMetrics fmExit = g2.getFontMetrics();
int etx = exitBtn.x + (exitBtn.width - fmExit.stringWidth("EXIT")) / 2;
int ety = exitBtn.y + (exitBtn.height + fmExit.getAscent()) / 2 - 4;
g2.setColor(new Color(0, 0, 0, 160));
g2.drawString("EXIT", etx + 1, ety + 1);
g2.setColor(exitHov ? new Color(255, 238, 180) : Color.WHITE);
g2.drawString("EXIT", etx, ety);
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
                ? "Restores 30% of the missing HP\nPerfect for survival."
                : "Grants 15%–25% EXP needed for next level.\nSpeeds up leveling.";

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
        drawWrappedText(g2, desc, x + 40, y + 180, 180);

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

    private int drawWrappedText(Graphics2D g2, String text, int x, int y, int maxWidth) {

        FontMetrics fm = g2.getFontMetrics();
        int lineHeight = fm.getHeight();
        int startY = y;

        String[] lines = text.split("\n");

        for (String rawLine : lines) {

            String[] words = rawLine.split(" ");
            String line = "";

            for (String word : words) {
                String testLine = line + word + " ";
                int testWidth = fm.stringWidth(testLine);

                if (testWidth > maxWidth) {
                    g2.drawString(line, x, y);
                    line = word + " ";
                    y += lineHeight;
                } else {
                    line = testLine;
                }
            }

            if (!line.isEmpty()) {
                g2.drawString(line, x, y);
                y += lineHeight;
            }
        }

        return y - startY;
    }

    private void drawInfoWindow(Graphics2D g2) {

        if (playerCharacter == null) return;

int w = 600;
        int h = 420;
        int padding = 20;
        int maxTextWidth = w - padding * 2;

        int x = (getWidth() - w) / 2;
        int y = (getHeight() - h) / 2;

        // ===== DRAW BOX =====
        g2.setColor(new Color(20, 20, 40, 230));
        g2.fillRoundRect(x, y, w, h, 20, 20);

        g2.setColor(Color.WHITE);
        g2.drawRoundRect(x, y, w, h, 20, 20);

// ===== TITLE =====
        g2.setFont(new Font("Arial", Font.BOLD, 18));
        g2.drawString(playerCharacter.getName(), x + padding, y + 32);

        // ===== BACKGROUND =====
        int bgX = x + padding;
        int bgY = y + 55;

        g2.setFont(new Font("Arial", Font.BOLD, 13));
        g2.drawString("Background", bgX, bgY);

        g2.setFont(new Font("Arial", Font.PLAIN, 13));
        int textHeight = drawWrappedText(
                g2,
                playerCharacter.getBackgroundInfo(),
                bgX,
                bgY + 16,
                maxTextWidth
        );

        // ===== SKILLS =====
        int skillX = x + padding;
        int skillY = bgY + 16 + textHeight + 12;

        g2.setFont(new Font("Arial", Font.BOLD, 13));
        g2.drawString("Skills", skillX, skillY);

        g2.setFont(new Font("Arial", Font.PLAIN, 12));

        int yOffset = skillY + 18;

        for (int i = 1; i <= 3; i++) {
            String name = playerCharacter.getSkillName(i);
            String dmg  = playerCharacter.getSkillDamageRange(i);

            g2.drawString(i + ": " + name + "   DMG: " + dmg, skillX, yOffset);
            yOffset += 20;
        }

        // ===== ITEMS =====
        int itemY = yOffset + 8;

        g2.setFont(new Font("Arial", Font.BOLD, 13));
        g2.drawString("Items", x + padding, itemY);

        g2.setFont(new Font("Arial", Font.PLAIN, 12));
        g2.drawString("HP Potion: " + playerCharacter.getHealthPotion() +
                      "   EXP Potion: " + playerCharacter.getExpPotion(),
                      x + padding, itemY + 18);

        // ===== CLOSE =====
        g2.setFont(new Font("Arial", Font.ITALIC, 11));
        g2.drawString("Press I to close", x + w - 130, y + h - 12);
    }

    private int measureWrappedText(Graphics2D g2, String text, int maxWidth) {

        FontMetrics fm = g2.getFontMetrics();
        int lineHeight = fm.getHeight();
        int totalHeight = 0;

        String[] lines = text.split("\n");

        for (String rawLine : lines) {

            String[] words = rawLine.split(" ");
            String line = "";

            for (String word : words) {
                String testLine = line + word + " ";
                int testWidth = fm.stringWidth(testLine);

                if (testWidth > maxWidth) {
                    totalHeight += lineHeight;
                    line = word + " ";
                } else {
                    line = testLine;
                }
            }

            if (!line.isEmpty()) {
                totalHeight += lineHeight;
            }
        }

        return totalHeight;
    }

    private void drawButton(Graphics2D g2, Rectangle rect, String text) {
    // Use .equals() — rect is a new object every frame so == always fails
    boolean hovered = hoveredBtn != null && hoveredBtn.equals(rect);

    // Outer glow on hover
    if (hovered) {
        g2.setColor(new Color(160, 80, 220, 75));
        g2.fillRoundRect(rect.x - 6, rect.y - 6,
                         rect.width + 12, rect.height + 12, 12, 12);
    }

    // Base image (hover variant if available)
    Image img = hovered ? buyBtnHoverImg : buyBtnImg;
    g2.drawImage(img, rect.x, rect.y, rect.width, rect.height, null);

    // Bright overlay + border on hover
    if (hovered) {
        g2.setColor(new Color(255, 255, 255, 40));
        g2.fillRoundRect(rect.x, rect.y, rect.width, rect.height, 6, 6);
        g2.setColor(new Color(210, 160, 255, 220));
        g2.setStroke(new BasicStroke(2f));
        g2.drawRoundRect(rect.x, rect.y, rect.width, rect.height, 6, 6);
        g2.setStroke(new BasicStroke(1f));
    }

    // Text with drop shadow
    g2.setFont(new Font("Arial", Font.BOLD, 14));
    FontMetrics fm = g2.getFontMetrics();
    int tx = rect.x + (rect.width - fm.stringWidth(text)) / 2;
    int ty = rect.y + (rect.height + fm.getAscent()) / 2 - 4;

    g2.setColor(new Color(0, 0, 0, 160));
    g2.drawString(text, tx + 1, ty + 1);
    g2.setColor(hovered ? new Color(255, 238, 180) : Color.WHITE);
    g2.drawString(text, tx, ty);
}
    private void showShopMessage(String msg, boolean isError) {
    shopMessage = msg;
    shopMessageTimer = MESSAGE_DURATION;
    shopMessageIsError = isError;
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

            // EXP
            drawHudBar(g2, 16, baseY + 62, 200, "EXP",
                    playerCharacter.getCurrentXp(),
                    playerCharacter.getNextLevelXp(),
                    new Color(220, 180, 60));

            int btnSize = 22;
            int btnX = 16 + 200 + 10;
            int btnY = baseY + 62;

            expPotionBtn = new Rectangle(btnX, btnY, btnSize, btnSize);

            g2.setColor(new Color(50, 50, 70));
            g2.fillRoundRect(btnX, btnY, btnSize, btnSize, 6, 6);

            g2.setColor(Color.WHITE);
            g2.drawRoundRect(btnX, btnY, btnSize, btnSize, 6, 6);

            g2.setFont(new Font("Arial", Font.BOLD, 16));
            g2.drawString("+", btnX + 7, btnY + 16);
        }
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