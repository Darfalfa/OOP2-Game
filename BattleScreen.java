import Characters.Character;
import Characters.Ronnix;
import java.awt.*;
import java.awt.event.*;
import java.awt.geom.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.util.ArrayList;
import java.util.List;
import javax.imageio.ImageIO;
import javax.swing.*;

/**
 * BattleScreen — battle panel driven by real battle logic classes.
 */
public class BattleScreen extends JPanel {

    private static final Color GOLD       = new Color(201, 150, 58);
    private static final Color GOLD_LIGHT = new Color(240, 192, 96);
    private static final Color GOLD_DARK  = new Color(100, 65, 15);
    private static final Color DARK_BG    = new Color(10, 5, 15);
    private static final Color RED_HP     = new Color(200, 50, 50);
    private static final Color GREEN_HP   = new Color(60, 180, 80);
    private static final Color PURPLE     = new Color(120, 80, 200);

    private GameWindow window;
    private GameScreen gameScreen;

    private enum Phase { PLAYER_TURN, ENEMY_TURN, VICTORY, DEFEATED }
    private Phase phase = Phase.PLAYER_TURN;

    // Real battle logic objects
    private Character playerCharacter;
    private Character enemyCharacter;

    private List<String> log = new ArrayList<>();
    private static final int MAX_LOG = 6;

    // 3 skills + Items + Run
    private Rectangle[] btnRects = new Rectangle[4];
    private int hoveredBtn = -1;

    private Timer animTimer;
    private int animTick = 0;
    private float shakeX = 0;
    private int shakeTicks = 0;
    private boolean enemyShake = false;
    private boolean playerShake = false;

    private BufferedImage enemySprite;
    private BufferedImage playerSprite;

    private int flashTicks = 0;
    private Color flashColor = Color.WHITE;

    private Runnable onBattleEnd;
    private boolean playerWon = false;

    private String selectedCharacter;

    public BattleScreen(GameWindow window, GameScreen gameScreen) {
        this.window = window;
        this.gameScreen = gameScreen;
        setBackground(DARK_BG);
        setFocusable(true);

        loadSprites();
        setupListeners();

        animTimer = new Timer(16, e -> {
            animTick++;
            if (shakeTicks > 0) {
                shakeTicks--;
                shakeX = (shakeTicks % 2 == 0) ? 6 : -6;
            } else {
                shakeX = 0;
            }

            if (flashTicks > 0) flashTicks--;
            repaint();
        });
    }

    private void loadSprites() {
        try {
            enemySprite = ImageIO.read(new File("images/enemy_shadow.png"));
        } catch (Exception e) {
            enemySprite = null;
        }

        reloadPlayerSprite();
    }

    private void reloadPlayerSprite() {
        String path;
        if ("Ronnix".equalsIgnoreCase(selectedCharacter))      path = "images/ronnixForward.png";
        else if ("Jakara".equalsIgnoreCase(selectedCharacter)) path = "images/jakaraForward.png";
        else                                                   path = "images/standingForward.png";

        try {
            playerSprite = ImageIO.read(new File(path));
        } catch (Exception e) {
            try {
                playerSprite = ImageIO.read(new File("images/standingForward.png"));
            } catch (Exception ex) {
                playerSprite = null;
            }
        }
    }

    public void setSelectedCharacter(String name) {
        this.selectedCharacter = name;
        switch (name.toLowerCase()) {
            case "ronnix":
                playerCharacter = new Ronnix();
                break;
            // add cases for the other characters
            default:
                playerCharacter = new Ronnix(); // fallback
                break;
        }
        repaint();
    }

    public void startBattle(Character player, Character enemy, Runnable onEnd) {
        this.playerCharacter = player;
        this.enemyCharacter = enemy;
        this.onBattleEnd = onEnd;
        this.phase = Phase.PLAYER_TURN;
        this.playerWon = false;

        log.clear();
        addLog("A " + enemyCharacter.getName() + " appeared!");
        addLog("Your turn — choose an action.");

        animTimer.start();
        repaint();
    }

    public void stopBattle() {
        animTimer.stop();
    }

    private void setupListeners() {
        addMouseMotionListener(new MouseMotionAdapter() {
            @Override
            public void mouseMoved(MouseEvent e) {
                int prev = hoveredBtn;
                hoveredBtn = -1;

                for (int i = 0; i < btnRects.length; i++) {
                    if (btnRects[i] != null && btnRects[i].contains(e.getPoint())) {
                        hoveredBtn = i;
                        break;
                    }
                }

                if (prev != hoveredBtn) repaint();

                setCursor(hoveredBtn >= 0
                    ? Cursor.getPredefinedCursor(Cursor.HAND_CURSOR)
                    : Cursor.getDefaultCursor());
            }
        });

        addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (phase == Phase.VICTORY || phase == Phase.DEFEATED) {
                    endBattle();
                    return;
                }

                if (phase != Phase.PLAYER_TURN) return;

                for (int i = 0; i < btnRects.length; i++) {
                    if (btnRects[i] != null && btnRects[i].contains(e.getPoint())) {
                        handleAction(i);
                        break;
                    }
                }
            }
        });
    }

    private String[] getActionLabels() {
        String skill1 = playerCharacter.getSkillName(1).toUpperCase();
        String skill2 = playerCharacter.getSkillName(2).toUpperCase();
        String skill3 = playerCharacter.getSkillName(3).toUpperCase();

        return new String[]{ skill1, skill2, skill3, "ITEMS" };
    }

    private void handleAction(int index) {
        switch (index) {
            case 0 -> doSkill(1, new Color(255, 80, 80, 180));
            case 1 -> doSkill(2, new Color(180, 80, 255, 180));
            case 2 -> doSkill(3, new Color(255, 180, 80, 180));
            case 3 -> doItems();
        }
    }

    private void doSkill(int skillNumber, Color fxColor) {
        if (!playerCharacter.isSkillAvailable(skillNumber)) {
            addLog("Skill on cooldown! " + playerCharacter.getSkillCooldown(skillNumber) + " turns left.");
            return;
        }

        int dmg = playerCharacter.useSkill(skillNumber, enemyCharacter);

        addLog(playerCharacter.getName() + " used " + playerCharacter.getSkillName(skillNumber) + "!");

        addLog("Dealt " + dmg + " damage!");

        triggerShake(true);
        triggerFlash(fxColor);

        afterPlayerAction();
    }

    private void doItems() {
            addLog("No usable items yet.");
            repaint();
    }

    private void afterPlayerAction() {
        playerCharacter.reduceCooldowns();

        if (enemyCharacter.getHp() <= 0) {
            addLog(enemyCharacter.getName() + " was defeated!");

            int gainedXp = enemyCharacter.getXpReward();
            int gainedGold = enemyCharacter.getGoldReward();

            playerCharacter.receiveBattleRewards(gainedXp, gainedGold);
            playerCharacter.restoreStats();
            playerCharacter.resetCooldowns();
            enemyCharacter.restoreStats(); 

            addLog("Gained " + gainedXp + " XP and " + gainedGold + " gold!");
            addLog("HP restored and cooldowns reset.");

            phase = Phase.VICTORY;
            playerWon = true;
            scheduleEndScreen(6000);
            return;
        }

        phase = Phase.ENEMY_TURN;
        addLog(enemyCharacter.getName() + " is attacking...");

        Timer t = new Timer(900, e -> {
            enemyTurn();
            ((Timer)e.getSource()).stop();
        });
        t.setRepeats(false);
        t.start();
    }

    private void enemyTurn() {
        int skillNum = chooseAvailableEnemySkill();

        int dealt = enemyCharacter.useSkill(skillNum, playerCharacter);

        addLog(enemyCharacter.getName() + " used " + enemyCharacter.getSkillName(skillNum) + "!");
        addLog("Dealt " + dealt + " damage!");

        triggerShake(false);
        triggerFlash(new Color(255, 50, 50, 140));

        enemyCharacter.reduceCooldowns();

        if (playerCharacter.getHp() <= 0) {
            addLog(playerCharacter.getName() + " has been defeated!");
            
            int lost = playerCharacter.loseRandomExp();
            addLog("Lost " + lost + " EXP.");
            
            playerCharacter.restoreStats();
            playerCharacter.resetCooldowns();
            enemyCharacter.restoreStats(); 

            playerWon = false;
            stopBattle();

            Timer t = new Timer(3000, e -> {
                if (onBattleEnd != null) onBattleEnd.run();
                ((Timer)e.getSource()).stop();
            });
            t.setRepeats(false);
            t.start();
            return;
        }

        phase = Phase.PLAYER_TURN;
        addLog("Your turn — choose an action.");
        repaint();
    }

    private int chooseAvailableEnemySkill() {
        java.util.List<Integer> available = new java.util.ArrayList<>();

        for (int i = 1; i <= 3; i++) {
            if (enemyCharacter.isSkillAvailable(i)) {
                available.add(i);
            }
        }

        if (available.isEmpty()) {
            return 1; 
        }

        return available.get(enemyCharacter.random.nextInt(available.size()));
    }

    private void scheduleEndScreen(int delayMs) {
        Timer t = new Timer(delayMs, e -> {
            endBattle();
            ((Timer)e.getSource()).stop();
        });
        t.setRepeats(false);
        t.start();
    }

    private void endBattle() {
        stopBattle();
        if (onBattleEnd != null) onBattleEnd.run();
    }

    private void addLog(String msg) {
        log.add(msg);
        if (log.size() > MAX_LOG) log.remove(0);
        repaint();
    }

    private void triggerShake(boolean isEnemy) {
        shakeTicks = 10;
        enemyShake = isEnemy;
        playerShake = !isEnemy;
    }

    private void triggerFlash(Color c) {
        flashColor = c;
        flashTicks = 8;
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

        int W = getWidth();
        int H = getHeight();

        drawBackground(g2, W, H);
        drawEnemyArea(g2, W, H);
        drawPlayerArea(g2, W, H);
        drawHpBars(g2, W, H);
        drawActionButtons(g2, W, H);
        drawBattleLog(g2, W, H);

        if (flashTicks > 0) {
            g2.setColor(flashColor);
            g2.fillRect(0, 0, W, H);
        }

        if (phase == Phase.VICTORY || phase == Phase.DEFEATED) {
            drawEndOverlay(g2, W, H);
        }

        g2.dispose();
    }

    private void drawBackground(Graphics2D g2, int W, int H) {
        GradientPaint bg = new GradientPaint(0, 0, new Color(15, 5, 25), 0, H, new Color(5, 2, 10));
        g2.setPaint(bg);
        g2.fillRect(0, 0, W, H);

        g2.setColor(new Color(60, 30, 80, 15));
        for (int i = 0; i < 8; i++) {
            int yy = (int)((animTick * 0.3 + i * 90) % H);
            g2.fillRect(0, yy, W, 40);
        }

        int groundY = (int)(H * 0.62);
        GradientPaint ground = new GradientPaint(0, groundY, new Color(30, 15, 40), 0, H, new Color(10, 5, 15));
        g2.setPaint(ground);
        g2.fillRect(0, groundY, W, H - groundY);

        g2.setColor(new Color(80, 40, 100, 80));
        g2.setStroke(new BasicStroke(1.5f));
        g2.drawLine(0, groundY, W, groundY);

        RadialGradientPaint vig = new RadialGradientPaint(
            new Point2D.Float(W / 2f, H / 2f),
            Math.max(W, H) * 0.75f,
            new float[]{0.3f, 1f},
            new Color[]{new Color(0, 0, 0, 0), new Color(0, 0, 0, 200)}
        );
        g2.setPaint(vig);
        g2.fillRect(0, 0, W, H);
    }

    private void drawEnemyArea(Graphics2D g2, int W, int H) {
        int groundY = (int)(H * 0.62);
        int centerX = (int)(W * 0.65);

        g2.setColor(new Color(0, 0, 0, 80));
        g2.fillOval(centerX - 50, groundY - 10, 100, 20);

        double bounce = Math.sin(animTick * 0.05) * 6;
        int eW = 140, eH = 140;
        int ex = centerX - eW / 2;
        int ey = groundY - eH - 20 + (int) bounce;

        int offX = (enemyShake && shakeTicks > 0) ? (int) shakeX : 0;

        if (enemySprite != null) {
            g2.drawImage(enemySprite, ex + offX, ey, eW, eH, null);
        } else {
            g2.setColor(new Color(20, 10, 40, 230));
            g2.fillOval(ex + offX, ey, eW, eH);
            g2.setColor(new Color(100, 60, 180, 200));
            g2.setStroke(new BasicStroke(3f));
            g2.drawOval(ex + offX, ey, eW, eH);

            g2.setColor(new Color(255, 255, 255, 220));
            g2.fillOval(ex + offX + 28, ey + 40, 24, 24);
            g2.fillOval(ex + offX + 80, ey + 40, 24, 24);
            g2.setColor(PURPLE);
            g2.fillOval(ex + offX + 34, ey + 46, 12, 12);
            g2.fillOval(ex + offX + 86, ey + 46, 12, 12);
        }

        g2.setFont(new Font("Serif", Font.BOLD, 16));
        FontMetrics fm = g2.getFontMetrics();
        String tag = (enemyCharacter != null) ? enemyCharacter.getName() : "Enemy";
        int tx = centerX - fm.stringWidth(tag) / 2;
        g2.setColor(new Color(0, 0, 0, 160));
        g2.drawString(tag, tx + 2, ey - 10 + 2);
        g2.setColor(GOLD_LIGHT);
        g2.drawString(tag, tx, ey - 10);
    }

    private void drawPlayerArea(Graphics2D g2, int W, int H) {
        int groundY = (int)(H * 0.62);
        int centerX = (int)(W * 0.28);

        g2.setColor(new Color(0, 0, 0, 80));
        g2.fillOval(centerX - 40, groundY - 10, 80, 18);

        int pW = 110, pH = 140;
        int px = centerX - pW / 2;
        int py = groundY - pH - 10;

        int offX = (playerShake && shakeTicks > 0) ? (int) shakeX : 0;

        boolean isRonnix = "Ronnix".equalsIgnoreCase(selectedCharacter);
        boolean isJakara = "Jakara".equalsIgnoreCase(selectedCharacter);

        if (playerSprite != null) {
            if (isJakara) {
                float pulse = 0.4f + 0.3f * (float)Math.sin(animTick * 0.07);
                g2.setColor(new Color(160, 80, 255, (int)(pulse * 90)));
                g2.fillOval(px + offX - 14, py - 10, pW + 28, pH + 20);
            }

            g2.drawImage(playerSprite, px + offX, py, pW, pH, null);

            if (isRonnix) {
                g2.setColor(new Color(180, 20, 20, 55));
                g2.fillRect(px + offX, py, pW, pH);
            } else if (isJakara) {
                g2.setColor(new Color(120, 40, 220, 55));
                g2.fillRect(px + offX, py, pW, pH);
            }
        } else {
            Color silColor = isRonnix ? new Color(140, 40, 40, 200)
                           : isJakara ? new Color(80, 30, 160, 200)
                                      : new Color(80, 60, 120, 200);
            Color silBorder = isRonnix ? new Color(220, 80, 40)
                            : isJakara ? new Color(190, 130, 255)
                                       : GOLD_DARK;

            g2.setColor(silColor);
            g2.fillRoundRect(px + offX, py, pW, pH, 20, 20);
            g2.setColor(silBorder);
            g2.setStroke(new BasicStroke(2f));
            g2.drawRoundRect(px + offX, py, pW, pH, 20, 20);

            if (isJakara) {
                g2.setColor(new Color(160, 100, 255));
                g2.setStroke(new BasicStroke(3f));
                g2.drawLine(px + offX + pW - 14, py + pH - 10, px + offX + pW - 10, py + 14);
                g2.setColor(new Color(210, 160, 255, 220));
                g2.fillOval(px + offX + pW - 20, py + 4, 16, 16);
            }
        }

        String charLabel = (playerCharacter != null) ? playerCharacter.getName().toUpperCase() : "PLAYER";
        Color nameColor = isRonnix ? new Color(220, 100, 80)
                        : isJakara ? new Color(190, 130, 255)
                                   : GOLD_LIGHT;

        g2.setFont(new Font("Serif", Font.BOLD, 13));
        FontMetrics fm = g2.getFontMetrics();
        g2.setColor(new Color(0, 0, 0, 140));
        g2.drawString(charLabel, centerX - fm.stringWidth(charLabel) / 2 + 1, groundY + 16);
        g2.setColor(nameColor);
        g2.drawString(charLabel, centerX - fm.stringWidth(charLabel) / 2, groundY + 15);
    }

    private void drawHpBars(Graphics2D g2, int W, int H) {
        if (playerCharacter == null || enemyCharacter == null) return;

        drawStatBar(g2, W - 340, 30, 300, "ENEMY HP", enemyCharacter.getHp(), enemyCharacter.getMaxHp(), RED_HP);

        drawStatBar(g2, W - 340, 75, 300, "ENEMY DEF", enemyCharacter.getDefense(), enemyCharacter.getMaxDefense(), new Color(80, 120, 220));

        drawStatBar(g2, 40, 30, 300, "HP",
            playerCharacter.getHp(), playerCharacter.getMaxHp(), GREEN_HP);

        drawStatBar(g2, 40, 75, 300, "DEF", playerCharacter.getDefense(), playerCharacter.getMaxDefense(), new Color(80, 120, 220));

        
    }

    private void drawStatBar(Graphics2D g2, int x, int y, int barW,
                             String label, int cur, int max, Color fill) {
        int barH = 22;
        double pct = max <= 0 ? 0 : (double) cur / max;

        g2.setColor(new Color(10, 5, 15, 200));
        g2.fillRoundRect(x, y, barW, barH, barH, barH);

        int fillW = (int)(barW * pct);
        GradientPaint gp = new GradientPaint(x, y, fill.brighter(), x, y + barH, fill.darker());
        g2.setPaint(gp);
        if (fillW > 0) {
            g2.fillRoundRect(x, y, fillW, barH, barH, barH);
        }

        g2.setColor(GOLD_DARK);
        g2.setStroke(new BasicStroke(1.5f));
        g2.drawRoundRect(x, y, barW, barH, barH, barH);

        g2.setFont(new Font("Serif", Font.BOLD, 13));
        g2.setColor(GOLD_LIGHT);
        g2.drawString(label, x + 8, y + barH - 5);

        String valStr = cur + " / " + max;
        FontMetrics fm = g2.getFontMetrics();
        g2.drawString(valStr, x + barW - fm.stringWidth(valStr) - 8, y + barH - 5);
    }

    private void drawActionButtons(Graphics2D g2, int W, int H) {
        if (phase == Phase.VICTORY || phase == Phase.DEFEATED) return;

        int btnW = 160;
        int btnH = 50;
        int gap  = 20;
        int totalW = 4 * btnW + 3 * gap;
        int startX = (W - totalW) / 2;
        int btnY = H - 90;

        boolean locked = (phase == Phase.ENEMY_TURN);
        String[] actions = getActionLabels();

        for (int i = 0; i < actions.length; i++) {
            int bx = startX + i * (btnW + gap);
            btnRects[i] = new Rectangle(bx, btnY, btnW, btnH);
            boolean hov = (hoveredBtn == i && !locked);
            drawButton(g2, actions[i], bx, btnY, btnW, btnH, hov, locked);
        }

        String turnStr = locked
                ? enemyCharacter.getName() + "'s turn..."
                : "YOUR TURN";

        g2.setFont(new Font("Serif", Font.BOLD | Font.ITALIC, 14));
        FontMetrics fm = g2.getFontMetrics();
        g2.setColor(locked ? new Color(200, 150, 255) : GOLD_LIGHT);
        g2.drawString(turnStr, (W - fm.stringWidth(turnStr)) / 2, btnY - 10);
    }

    private void drawButton(Graphics2D g2, String label, int x, int y, int w, int h,
                            boolean hovered, boolean locked) {
        Color bg = locked ? new Color(20, 10, 30, 160)
                 : hovered ? new Color(70, 40, 10, 220)
                           : new Color(25, 12, 35, 200);

        Color border = locked ? new Color(60, 40, 80)
                     : hovered ? GOLD_LIGHT
                               : GOLD_DARK;

        if (hovered) {
            g2.setColor(new Color(GOLD.getRed(), GOLD.getGreen(), GOLD.getBlue(), 30));
            g2.fillRoundRect(x - 6, y - 6, w + 12, h + 12, 8, 8);
        }

        g2.setColor(bg);
        g2.fillRoundRect(x, y, w, h, 8, 8);
        g2.setColor(border);
        g2.setStroke(new BasicStroke(hovered ? 2f : 1.5f));
        g2.drawRoundRect(x, y, w, h, 8, 8);

        int cs = 7;
        g2.setColor(hovered ? GOLD_LIGHT : GOLD_DARK);
        g2.setStroke(new BasicStroke(1.5f));
        g2.drawLine(x, y, x + cs, y);
        g2.drawLine(x, y, x, y + cs);
        g2.drawLine(x + w, y, x + w - cs, y);
        g2.drawLine(x + w, y, x + w, y + cs);
        g2.drawLine(x, y + h, x + cs, y + h);
        g2.drawLine(x, y + h, x, y + h - cs);
        g2.drawLine(x + w, y + h, x + w - cs, y + h);
        g2.drawLine(x + w, y + h, x + w, y + h - cs);

        g2.setFont(new Font("Serif", Font.BOLD, 14));
        FontMetrics fm = g2.getFontMetrics();
        int tx = x + (w - fm.stringWidth(label)) / 2;
        int ty = y + (h + fm.getAscent() - fm.getDescent()) / 2;

        g2.setColor(new Color(0, 0, 0, 160));
        g2.drawString(label, tx + 2, ty + 2);
        g2.setColor(locked ? new Color(120, 90, 150) : hovered ? GOLD_LIGHT : GOLD);
        g2.drawString(label, tx, ty);
    }

    private void drawBattleLog(Graphics2D g2, int W, int H) {
        int logW = W - 80;
        int logH = 110;
        int logX = 40;
        int logY = H - 90 - logH - 16;

        g2.setColor(new Color(10, 5, 20, 210));
        g2.fillRoundRect(logX, logY, logW, logH, 10, 10);
        g2.setColor(GOLD_DARK);
        g2.setStroke(new BasicStroke(1.5f));
        g2.drawRoundRect(logX, logY, logW, logH, 10, 10);

        g2.setFont(new Font("Serif", Font.PLAIN, 14));
        FontMetrics fm = g2.getFontMetrics();
        int lineH = fm.getHeight() + 2;
        int textY = logY + 18;

        for (int i = 0; i < log.size(); i++) {
            float alpha = 0.5f + 0.5f * ((float)(i + 1) / log.size());
            g2.setColor(new Color(1f, 1f, 0.85f, alpha));
            g2.drawString(log.get(i), logX + 14, textY + i * lineH);
        }
    }

    private void drawEndOverlay(Graphics2D g2, int W, int H) {
        g2.setColor(new Color(0, 0, 0, 160));
        g2.fillRect(0, 0, W, H);

        boolean won = (phase == Phase.VICTORY);
        String title = won ? "VICTORY!" : "DEFEATED";
        String sub   = won ? "Click anywhere to continue." : "Click anywhere to return.";
        Color tc     = won ? GOLD_LIGHT : new Color(220, 80, 80);

        g2.setFont(new Font("Serif", Font.BOLD, 64));
        FontMetrics fm = g2.getFontMetrics();
        int tx = (W - fm.stringWidth(title)) / 2;
        g2.setColor(new Color(0, 0, 0, 200));
        g2.drawString(title, tx + 4, H / 2 + 4);
        g2.setColor(tc);
        g2.drawString(title, tx, H / 2);

        g2.setFont(new Font("Serif", Font.ITALIC, 18));
        fm = g2.getFontMetrics();
        g2.setColor(new Color(200, 180, 140));
        g2.drawString(sub, (W - fm.stringWidth(sub)) / 2, H / 2 + 50);
    }

    public boolean playerWon() {
        return playerWon;
    }
}