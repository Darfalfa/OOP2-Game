import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.geom.*;
import java.awt.image.BufferedImage;
import javax.imageio.ImageIO;
import java.io.File;
import java.util.ArrayList;
import java.util.List;

/**
 * BattleScreen — turn-based battle panel.
 * Triggered when the player walks into an enemy.
 *
 * Battle sprites expected at:
 *   images/ayaBattle.png    — Aya  (Archer)  fighting stance
 *   images/ronnixBattle.png — Ronnix (Fighter) fighting stance
 *   images/jakaraBattle.png — Jakara (Mage)  fighting stance
 */
public class BattleScreen extends JPanel {

    // ── Colours ──────────────────────────────────────────────────────────────
    private static final Color GOLD       = new Color(201, 150, 58);
    private static final Color GOLD_LIGHT = new Color(240, 192, 96);
    private static final Color GOLD_DARK  = new Color(100, 65, 15);
    private static final Color DARK_BG    = new Color(10, 5, 15);
    private static final Color RED_HP     = new Color(200, 50,  50);
    private static final Color GREEN_HP   = new Color(60,  180, 80);
    private static final Color PURPLE     = new Color(120, 80, 200);

    // ── References ───────────────────────────────────────────────────────────
    private GameWindow window;
    private GameScreen gameScreen;

    // ── Battle state ─────────────────────────────────────────────────────────
    private enum Phase { PLAYER_TURN, ENEMY_TURN, VICTORY, DEFEATED }
    private Phase phase = Phase.PLAYER_TURN;

    // Player stats (pulled fresh each battle)
    private int playerHp, playerMaxHp, playerMp, playerMaxMp;
    private int playerAtk, playerDef;

    // Enemy stats (copied from Enemy object)
    private int enemyHp, enemyMaxHp, enemyAtk, enemyDef;
    private String enemyName;

    // Battle log
    private List<String> log = new ArrayList<>();
    private static final int MAX_LOG = 5;

    // Buttons — label for index 1 (SKILL) changes per character
    private String[] getActionLabels() {
        String skillName;
        if ("Ronnix".equalsIgnoreCase(selectedCharacter))       skillName = "IRON CLEAVE";
        else if ("Jakara".equalsIgnoreCase(selectedCharacter))  skillName = "VOID BLAST";
        else                                                     skillName = "SWIFT SHOT";
        return new String[]{ "ATTACK", skillName, "ITEM", "RUN" };
    }
    private Rectangle[] btnRects = new Rectangle[4];
    private int hoveredBtn = -1;

    // Animation
    private Timer animTimer;
    private int   animTick    = 0;
    private float shakeX      = 0;
    private int   shakeTicks  = 0;
    private boolean enemyShake  = false;
    private boolean playerShake = false;

    // Sprites
    private BufferedImage enemySprite;

    // Per-character battle sprites (loaded once, swapped on character change)
    private BufferedImage ayaBattleSprite;
    private BufferedImage ronnixBattleSprite;
    private BufferedImage jakaraBattleSprite;

    // Flash effect
    private int flashTicks = 0;
    private Color flashColor = Color.WHITE;

    // Callback: called when battle ends (win or lose)
    private Runnable onBattleEnd;
    private boolean playerWon = false;

    // Which character the player chose
    private String selectedCharacter = "Archer";

    public BattleScreen(GameWindow window, GameScreen gameScreen) {
        this.window     = window;
        this.gameScreen = gameScreen;
        setBackground(DARK_BG);
        setFocusable(true);

        loadAllSprites();
        setupListeners();

        animTimer = new Timer(16, e -> {
            animTick++;
            if (shakeTicks > 0) { shakeTicks--; shakeX = (shakeTicks % 2 == 0) ? 6 : -6; }
            else shakeX = 0;
            if (flashTicks > 0) flashTicks--;
            repaint();
        });
    }

    // ── Sprite loading ────────────────────────────────────────────────────────

    /**
     * Loads all battle sprites upfront so there is no disk I/O mid-battle.
     * Each character has a dedicated "fighting stance" image stored in images/.
     */
    private void loadAllSprites() {
        enemySprite       = tryLoadImage("images/enemy_shadow.png");
        ayaBattleSprite    = tryLoadImage("images/aya/ayaBattle.png");
        ronnixBattleSprite = tryLoadImage("images/ronnix/ronnixBattle.png");
        jakaraBattleSprite = tryLoadImage("images/jakara/jakaraBattle.png");
    }

    /** Returns the loaded BufferedImage, or null if the file is missing. */
    private BufferedImage tryLoadImage(String path) {
        try {
            File f = new File(path);
            if (f.exists()) return ImageIO.read(f);
        } catch (Exception e) {
            System.err.println("BattleScreen: could not load " + path);
        }
        return null;
    }

    /** Returns the correct battle sprite for the currently selected character. */
    private BufferedImage currentPlayerSprite() {
        if ("Ronnix".equalsIgnoreCase(selectedCharacter)) return ronnixBattleSprite;
        if ("Jakara".equalsIgnoreCase(selectedCharacter)) return jakaraBattleSprite;
        return ayaBattleSprite; // default: Aya / Archer
    }

    public void setSelectedCharacter(String name) {
        this.selectedCharacter = name;
    }

    // ── Battle lifecycle ──────────────────────────────────────────────────────

    public void startBattle(Enemy enemy,
                             int pHp, int pMaxHp, int pMp, int pMaxMp,
                             int pAtk, int pDef, Runnable onEnd) {
        this.playerHp    = pHp;
        this.playerMaxHp = pMaxHp;
        this.playerMp    = pMp;
        this.playerMaxMp = pMaxMp;
        this.playerAtk   = pAtk;
        this.playerDef   = pDef;

        this.enemyHp     = enemy.hp;
        this.enemyMaxHp  = enemy.maxHp;
        this.enemyAtk    = enemy.attack;
        this.enemyDef    = enemy.defense;
        this.enemyName   = enemy.name;

        this.onBattleEnd = onEnd;
        this.phase       = Phase.PLAYER_TURN;
        this.playerWon   = false;

        log.clear();
        addLog("A " + enemyName + " appeared!");
        addLog("Your turn — choose an action.");

        animTimer.start();
        repaint();
    }

    public void stopBattle() { animTimer.stop(); }

    // ── Input ─────────────────────────────────────────────────────────────────
    private void setupListeners() {
        addMouseMotionListener(new MouseMotionAdapter() {
            @Override public void mouseMoved(MouseEvent e) {
                int prev = hoveredBtn;
                hoveredBtn = -1;
                for (int i = 0; i < btnRects.length; i++)
                    if (btnRects[i] != null && btnRects[i].contains(e.getPoint())) { hoveredBtn = i; break; }
                if (prev != hoveredBtn) repaint();
                setCursor(hoveredBtn >= 0
                    ? Cursor.getPredefinedCursor(Cursor.HAND_CURSOR)
                    : Cursor.getDefaultCursor());
            }
        });

        addMouseListener(new MouseAdapter() {
            @Override public void mouseClicked(MouseEvent e) {
                if (phase == Phase.VICTORY || phase == Phase.DEFEATED) { endBattle(); return; }
                if (phase != Phase.PLAYER_TURN) return;
                for (int i = 0; i < btnRects.length; i++)
                    if (btnRects[i] != null && btnRects[i].contains(e.getPoint())) { handleAction(i); break; }
            }
        });
    }

    private void handleAction(int index) {
        switch (index) {
            case 0 -> doAttack();
            case 1 -> doSkill();
            case 2 -> doItem();
            case 3 -> doRun();
        }
    }

    // ── Player actions ────────────────────────────────────────────────────────
    private void doAttack() {
        int dmg = Math.max(1, playerAtk - enemyDef + randomInt(-2, 3));
        enemyHp = Math.max(0, enemyHp - dmg);
        addLog("You attack for " + dmg + " damage!");
        triggerShake(true);
        triggerFlash(new Color(255, 80, 80, 180));
        afterPlayerAction();
    }

    private void doSkill() {
        if ("Ronnix".equalsIgnoreCase(selectedCharacter)) doIronCleave();
        else if ("Jakara".equalsIgnoreCase(selectedCharacter)) doVoidBlast();
        else doSwiftShot();
    }

    // Aya skill — rapid ranged burst
    private void doSwiftShot() {
        if (playerMp < 10) { addLog("Not enough MP!"); return; }
        playerMp -= 10;
        int dmg = Math.max(1, playerAtk * 2 - enemyDef + randomInt(-1, 4));
        enemyHp = Math.max(0, enemyHp - dmg);
        addLog("Swift Shot! " + dmg + " damage! (-10 MP)");
        triggerShake(true);
        triggerFlash(new Color(180, 80, 255, 180));
        afterPlayerAction();
    }

    // Ronnix skill — heavy melee cleave, ignores part of enemy defence
    private void doIronCleave() {
        if (playerMp < 6) { addLog("Not enough MP!"); return; }
        playerMp -= 6;
        int effDef = enemyDef / 2;
        int dmg = Math.max(1, (int)(playerAtk * 1.8) - effDef + randomInt(-1, 5));
        enemyHp = Math.max(0, enemyHp - dmg);
        addLog("Iron Cleave! " + dmg + " damage! (DEF pierced) (-6 MP)");
        triggerShake(true);
        triggerFlash(new Color(220, 80, 40, 200));
        afterPlayerAction();
    }

    // Jakara skill — massive arcane damage, ignores ALL defence
    private void doVoidBlast() {
        if (playerMp < 25) { addLog("Not enough MP! (Void Blast needs 25)"); return; }
        playerMp -= 25;
        int dmg = Math.max(1, playerAtk + 18 + randomInt(0, 10));
        enemyHp = Math.max(0, enemyHp - dmg);
        addLog("★ Void Blast! " + dmg + " arcane dmg! (DEF ignored, -25 MP)");
        triggerShake(true);
        triggerFlash(new Color(160, 80, 255, 210));
        afterPlayerAction();
    }

    private void doItem() {
        int heal = 20 + randomInt(0, 11);
        playerHp = Math.min(playerMaxHp, playerHp + heal);
        addLog("Used Potion! Recovered " + heal + " HP.");
        triggerFlash(new Color(80, 255, 120, 160));
        afterPlayerAction();
    }

    private void doRun() {
        boolean success = Math.random() < 0.5;
        if (success) {
            addLog("You escaped!");
            phase = Phase.DEFEATED;
            playerWon = false;
            scheduleEndScreen(1200);
        } else {
            addLog("Couldn't escape!");
            afterPlayerAction();
        }
    }

    private void afterPlayerAction() {
        if (enemyHp <= 0) {
            addLog(enemyName + " was defeated!");
            phase = Phase.VICTORY;
            playerWon = true;
            scheduleEndScreen(1800);
            return;
        }
        phase = Phase.ENEMY_TURN;
        addLog(enemyName + " is attacking...");
        Timer t = new Timer(900, e -> { enemyTurn(); ((Timer)e.getSource()).stop(); });
        t.setRepeats(false); t.start();
    }

    // ── Enemy turn ────────────────────────────────────────────────────────────
    private void enemyTurn() {
        int dmg = Math.max(1, enemyAtk - playerDef + randomInt(-2, 3));
        playerHp = Math.max(0, playerHp - dmg);
        addLog(enemyName + " attacks for " + dmg + " damage!");
        triggerShake(false);
        triggerFlash(new Color(255, 50, 50, 140));

        if (playerHp <= 0) {
            addLog("You were defeated...");
            phase = Phase.DEFEATED;
            playerWon = false;
            scheduleEndScreen(1800);
        } else {
            phase = Phase.PLAYER_TURN;
            addLog("Your turn — choose an action.");
        }
        repaint();
    }

    private void scheduleEndScreen(int delayMs) {
        Timer t = new Timer(delayMs, e -> { endBattle(); ((Timer)e.getSource()).stop(); });
        t.setRepeats(false); t.start();
    }

    private void endBattle() {
        stopBattle();
        if (onBattleEnd != null) onBattleEnd.run();
    }

    // ── Helpers ───────────────────────────────────────────────────────────────
    private void addLog(String msg) {
        log.add(msg);
        if (log.size() > MAX_LOG) log.remove(0);
        repaint();
    }

    private void triggerShake(boolean isEnemy) {
        shakeTicks  = 10;
        enemyShake  = isEnemy;
        playerShake = !isEnemy;
    }

    private void triggerFlash(Color c) { flashColor = c; flashTicks = 8; }

    private int randomInt(int min, int max) { return min + (int)(Math.random() * (max - min)); }

    // ── Paint ─────────────────────────────────────────────────────────────────
    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,      RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION,     RenderingHints.VALUE_INTERPOLATION_BILINEAR);

        int W = getWidth(), H = getHeight();

        drawBackground(g2, W, H);
        drawEnemyArea(g2, W, H);
        drawPlayerArea(g2, W, H);
        drawHpBars(g2, W, H);
        drawActionButtons(g2, W, H);
        drawBattleLog(g2, W, H);

        // Flash overlay
        if (flashTicks > 0) { g2.setColor(flashColor); g2.fillRect(0, 0, W, H); }

        // Victory / Defeat overlay
        if (phase == Phase.VICTORY || phase == Phase.DEFEATED) drawEndOverlay(g2, W, H);

        g2.dispose();
    }

    private void drawBackground(Graphics2D g2, int W, int H) {
        GradientPaint bg = new GradientPaint(0, 0, new Color(15, 5, 25), 0, H, new Color(5, 2, 10));
        g2.setPaint(bg);
        g2.fillRect(0, 0, W, H);

        // Animated fog
        g2.setColor(new Color(60, 30, 80, 15));
        for (int i = 0; i < 8; i++) {
            int yy = (int)((animTick * 0.3 + i * 90) % H);
            g2.fillRect(0, yy, W, 40);
        }

        // Ground
        int groundY = (int)(H * 0.62);
        GradientPaint ground = new GradientPaint(0, groundY, new Color(30, 15, 40), 0, H, new Color(10, 5, 15));
        g2.setPaint(ground);
        g2.fillRect(0, groundY, W, H - groundY);

        g2.setColor(new Color(80, 40, 100, 80));
        g2.setStroke(new BasicStroke(1.5f));
        g2.drawLine(0, groundY, W, groundY);

        // Vignette
        RadialGradientPaint vig = new RadialGradientPaint(
            new Point2D.Float(W/2f, H/2f), Math.max(W,H)*0.75f,
            new float[]{0.3f, 1f},
            new Color[]{new Color(0,0,0,0), new Color(0,0,0,200)});
        g2.setPaint(vig);
        g2.fillRect(0, 0, W, H);
    }

    private void drawEnemyArea(Graphics2D g2, int W, int H) {
        int groundY = (int)(H * 0.62);
        int centerX = (int)(W * 0.65);

        // Shadow beneath enemy
        g2.setColor(new Color(0, 0, 0, 80));
        g2.fillOval(centerX - 50, groundY - 10, 100, 20);

        double bounce = Math.sin(animTick * 0.05) * 6;
        int eW = 140, eH = 140;
        int ex = centerX - eW / 2;
        int ey = groundY - eH - 20 + (int)bounce;

        int offX = (enemyShake && shakeTicks > 0) ? (int)shakeX : 0;

        if (enemySprite != null) {
            g2.drawImage(enemySprite, ex + offX, ey, eW, eH, null);
        } else {
            // Fallback drawn enemy
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

        // Enemy name tag
        g2.setFont(new Font("Serif", Font.BOLD, 16));
        FontMetrics fm = g2.getFontMetrics();
        int tx = centerX - fm.stringWidth(enemyName) / 2;
        g2.setColor(new Color(0,0,0,160)); g2.drawString(enemyName, tx+2, ey - 10 + 2);
        g2.setColor(GOLD_LIGHT);           g2.drawString(enemyName, tx,   ey - 10);
    }

    /**
     * Draws the player's character in their fighting stance.
     * Each character has a unique battle sprite and thematic visual effects.
     */
    private void drawPlayerArea(Graphics2D g2, int W, int H) {
        int groundY = (int)(H * 0.62);
        int centerX = (int)(W * 0.28);

        boolean isRonnix = "Ronnix".equalsIgnoreCase(selectedCharacter);
        boolean isJakara = "Jakara".equalsIgnoreCase(selectedCharacter);

        // Ground shadow beneath player
        g2.setColor(new Color(0, 0, 0, 80));
        g2.fillOval(centerX - 45, groundY - 10, 90, 20);

        // ── Sprite dimensions — each character is sized to feel right ──────────
        // Ronnix is bulkier, Jakara is taller (staff), Aya is slender
        final int pW, pH;
        if (isRonnix) {
            pW = 130; pH = 160;
        } else if (isJakara) {
            pW = 120; pH = 170;
        } else {
            pW = 110; pH = 155;
        }

        int px = centerX - pW / 2;
        int py = groundY - pH - 5;

        int offX = (playerShake && shakeTicks > 0) ? (int)shakeX : 0;

        BufferedImage sprite = currentPlayerSprite();

        // ── Per-character ambient effects drawn BEHIND the sprite ─────────────
        if (isJakara) {
            // Pulsing arcane aura — violet glow
            float pulse = 0.4f + 0.3f * (float)Math.sin(animTick * 0.07);
            int auraAlpha = (int)(pulse * 100);
            // Outer soft glow
            g2.setColor(new Color(160, 80, 255, auraAlpha / 2));
            g2.fillOval(px + offX - 20, py - 15, pW + 40, pH + 30);
            // Inner brighter core
            g2.setColor(new Color(200, 130, 255, auraAlpha));
            g2.fillOval(px + offX - 8, py - 4, pW + 16, pH + 8);

        } else if (isRonnix) {
            // Smoldering ember glow — dark red/orange
            float pulse = 0.3f + 0.2f * (float)Math.sin(animTick * 0.05);
            int auraAlpha = (int)(pulse * 80);
            g2.setColor(new Color(200, 60, 20, auraAlpha));
            g2.fillOval(px + offX - 10, py + pH / 3, pW + 20, pH * 2 / 3 + 10);

        } else {
            // Aya — soft wind shimmer in teal/white
            float pulse = 0.3f + 0.2f * (float)Math.sin(animTick * 0.06);
            int auraAlpha = (int)(pulse * 60);
            g2.setColor(new Color(100, 200, 220, auraAlpha));
            g2.fillOval(px + offX - 8, py - 4, pW + 16, pH + 8);
        }

        // ── Draw the actual battle sprite ─────────────────────────────────────
        if (sprite != null) {
            // Strip near-white background if the source image has one
            BufferedImage cleaned = stripWhiteBackground(sprite);
            g2.drawImage(cleaned, px + offX, py, pW, pH, null);
        } else {
            // Fallback drawn silhouette when the sprite file is missing
            Color silColor  = isRonnix ? new Color(140, 40, 40, 200)
                            : isJakara ? new Color(80, 30, 160, 200)
                                       : new Color(60, 100, 140, 200);
            Color silBorder = isRonnix ? new Color(220, 80, 40)
                            : isJakara ? new Color(190, 130, 255)
                                       : new Color(100, 200, 220);
            g2.setColor(silColor);
            g2.fillRoundRect(px + offX, py, pW, pH, 20, 20);
            g2.setColor(silBorder);
            g2.setStroke(new BasicStroke(2f));
            g2.drawRoundRect(px + offX, py, pW, pH, 20, 20);

            // Simple weapon hint on fallback
            if (isJakara) {
                g2.setColor(new Color(160, 100, 255));
                g2.setStroke(new BasicStroke(3f));
                g2.drawLine(px + offX + pW - 14, py + pH - 10, px + offX + pW - 10, py + 14);
                g2.setColor(new Color(210, 160, 255, 220));
                g2.fillOval(px + offX + pW - 20, py + 4, 16, 16);
            } else if (isRonnix) {
                g2.setColor(new Color(180, 80, 40));
                g2.setStroke(new BasicStroke(4f));
                g2.drawLine(px + offX + pW/2, py + 20, px + offX + pW - 4, py + pH/2);
            }
        }

        // ── Character name tag below sprite ───────────────────────────────────
        String charLabel = isRonnix ? "RONNIX" : isJakara ? "JAKARA" : "AYA";
        Color nameColor  = isRonnix ? new Color(220, 100,  80)
                         : isJakara ? new Color(190, 130, 255)
                                    : new Color(100, 210, 230);
        g2.setFont(new Font("Serif", Font.BOLD, 14));
        FontMetrics fm = g2.getFontMetrics();
        int lx = centerX - fm.stringWidth(charLabel) / 2;
        g2.setColor(new Color(0, 0, 0, 150));
        g2.drawString(charLabel, lx + 1, groundY + 18);
        g2.setColor(nameColor);
        g2.drawString(charLabel, lx, groundY + 17);
    }

    /**
     * Removes near-white / lavender pixel-art backgrounds using a flood-fill
     * from every edge pixel. Returns a new ARGB image with the background
     * made transparent. Threshold of 195 keeps fringe aliasing pixels intact.
     */
    private BufferedImage stripWhiteBackground(BufferedImage src) {
        int w = src.getWidth(), h = src.getHeight();
        // Convert to ARGB
        BufferedImage img = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
        Graphics2D tmp = img.createGraphics();
        tmp.drawImage(src, 0, 0, null);
        tmp.dispose();

        boolean[][] visited = new boolean[w][h];
        java.util.Queue<int[]> queue = new java.util.LinkedList<>();

        // Seed from all four edges
        for (int x = 0; x < w; x++) { enqueueIfBg(img, x, 0,     visited, queue); enqueueIfBg(img, x, h-1, visited, queue); }
        for (int y = 1; y < h-1; y++) { enqueueIfBg(img, 0, y,   visited, queue); enqueueIfBg(img, w-1, y, visited, queue); }

        while (!queue.isEmpty()) {
            int[] px = queue.poll();
            int cx = px[0], cy = px[1];
            img.setRGB(cx, cy, 0x00000000);
            enqueueIfBg(img, cx-1, cy,   visited, queue);
            enqueueIfBg(img, cx+1, cy,   visited, queue);
            enqueueIfBg(img, cx,   cy-1, visited, queue);
            enqueueIfBg(img, cx,   cy+1, visited, queue);
        }
        return img;
    }

    private void enqueueIfBg(BufferedImage img, int x, int y,
                              boolean[][] visited, java.util.Queue<int[]> q) {
        if (x < 0 || y < 0 || x >= img.getWidth() || y >= img.getHeight()) return;
        if (visited[x][y]) return;
        visited[x][y] = true;
        int argb = img.getRGB(x, y);
        int a = (argb >> 24) & 0xFF;
        int r = (argb >> 16) & 0xFF;
        int g = (argb >>  8) & 0xFF;
        int b =  argb        & 0xFF;
        // Match white, near-white, and light lavender/grey pixel-art backgrounds
        if (a == 0 || (r > 195 && g > 195 && b > 195))
            q.add(new int[]{x, y});
    }

    // ── Stats bars ────────────────────────────────────────────────────────────
    private void drawHpBars(Graphics2D g2, int W, int H) {
        drawStatBar(g2, W - 340, 30, 300, "ENEMY HP", enemyHp, enemyMaxHp, RED_HP);
        drawStatBar(g2, 40, 30, 300, "HP", playerHp, playerMaxHp, GREEN_HP);

        Color mpColor = "Jakara".equalsIgnoreCase(selectedCharacter)
                      ? new Color(160, 80, 255)
                      : new Color(80, 120, 220);
        drawStatBar(g2, 40, 75, 300, "MP", playerMp, playerMaxMp, mpColor);
    }

    private void drawStatBar(Graphics2D g2, int x, int y, int barW,
                              String label, int cur, int max, Color fill) {
        int barH = 22;
        double pct = max <= 0 ? 0 : (double)cur / max;

        g2.setColor(new Color(10, 5, 15, 200));
        g2.fillRoundRect(x, y, barW, barH, barH, barH);

        int fillW = (int)(barW * pct);
        GradientPaint gp = new GradientPaint(x, y, fill.brighter(), x, y+barH, fill.darker());
        g2.setPaint(gp);
        if (fillW > 0) g2.fillRoundRect(x, y, fillW, barH, barH, barH);

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

    // ── Action buttons ────────────────────────────────────────────────────────
    private void drawActionButtons(Graphics2D g2, int W, int H) {
        if (phase == Phase.VICTORY || phase == Phase.DEFEATED) return;

        int btnW = 160, btnH = 50, gap = 20;
        int totalW = 4 * btnW + 3 * gap;
        int startX = (W - totalW) / 2;
        int btnY   = H - 90;

        boolean locked = (phase == Phase.ENEMY_TURN);
        String[] ACTIONS = getActionLabels();

        for (int i = 0; i < ACTIONS.length; i++) {
            int bx = startX + i * (btnW + gap);
            btnRects[i] = new Rectangle(bx, btnY, btnW, btnH);
            boolean hov = (hoveredBtn == i && !locked);
            drawButton(g2, ACTIONS[i], bx, btnY, btnW, btnH, hov, locked);
        }

        String turnStr = locked ? enemyName + "'s turn..." : "YOUR TURN";
        g2.setFont(new Font("Serif", Font.BOLD | Font.ITALIC, 14));
        FontMetrics fm = g2.getFontMetrics();
        g2.setColor(locked ? new Color(200, 150, 255) : GOLD_LIGHT);
        g2.drawString(turnStr, (W - fm.stringWidth(turnStr)) / 2, btnY - 10);
    }

    private void drawButton(Graphics2D g2, String label, int x, int y, int w, int h,
                             boolean hovered, boolean locked) {
        Color bg     = locked  ? new Color(20, 10, 30, 160)
                     : hovered ? new Color(70, 40, 10, 220)
                               : new Color(25, 12, 35, 200);
        Color border = locked  ? new Color(60, 40, 80)
                     : hovered ? GOLD_LIGHT
                               : GOLD_DARK;

        if (hovered) {
            g2.setColor(new Color(GOLD.getRed(), GOLD.getGreen(), GOLD.getBlue(), 30));
            g2.fillRoundRect(x-6, y-6, w+12, h+12, 8, 8);
        }
        g2.setColor(bg);
        g2.fillRoundRect(x, y, w, h, 8, 8);
        g2.setColor(border);
        g2.setStroke(new BasicStroke(hovered ? 2f : 1.5f));
        g2.drawRoundRect(x, y, w, h, 8, 8);

        int cs = 7;
        g2.setColor(hovered ? GOLD_LIGHT : GOLD_DARK);
        g2.setStroke(new BasicStroke(1.5f));
        g2.drawLine(x,   y,   x+cs, y);    g2.drawLine(x,   y,   x,   y+cs);
        g2.drawLine(x+w, y,   x+w-cs, y);  g2.drawLine(x+w, y,   x+w, y+cs);
        g2.drawLine(x,   y+h, x+cs, y+h);  g2.drawLine(x,   y+h, x,   y+h-cs);
        g2.drawLine(x+w, y+h, x+w-cs,y+h); g2.drawLine(x+w, y+h, x+w, y+h-cs);

        g2.setFont(new Font("Serif", Font.BOLD, 15));
        FontMetrics fm = g2.getFontMetrics();
        int tx = x + (w - fm.stringWidth(label)) / 2;
        int ty = y + (h + fm.getAscent() - fm.getDescent()) / 2;
        g2.setColor(new Color(0,0,0,160)); g2.drawString(label, tx+2, ty+2);
        g2.setColor(locked ? new Color(120,90,150) : hovered ? GOLD_LIGHT : GOLD);
        g2.drawString(label, tx, ty);
    }

    // ── Battle log ────────────────────────────────────────────────────────────
    private void drawBattleLog(Graphics2D g2, int W, int H) {
        int logW = W - 80, logH = 110;
        int logX = 40, logY = H - 90 - logH - 16;

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

    // ── End overlay ───────────────────────────────────────────────────────────
    private void drawEndOverlay(Graphics2D g2, int W, int H) {
        g2.setColor(new Color(0, 0, 0, 160));
        g2.fillRect(0, 0, W, H);

        boolean won  = (phase == Phase.VICTORY);
        String title = won ? "VICTORY!" : "DEFEATED";
        String sub   = won ? "Click anywhere to continue." : "Click anywhere to return.";
        Color  tc    = won ? GOLD_LIGHT : new Color(220, 80, 80);

        g2.setFont(new Font("Serif", Font.BOLD, 64));
        FontMetrics fm = g2.getFontMetrics();
        int tx = (W - fm.stringWidth(title)) / 2;
        g2.setColor(new Color(0,0,0,200)); g2.drawString(title, tx+4, H/2+4);
        g2.setColor(tc);                   g2.drawString(title, tx,   H/2);

        g2.setFont(new Font("Serif", Font.ITALIC, 18));
        fm = g2.getFontMetrics();
        g2.setColor(new Color(200, 180, 140));
        g2.drawString(sub, (W - fm.stringWidth(sub)) / 2, H/2 + 50);
    }

    // ── Getters ───────────────────────────────────────────────────────────────
    public int     getPlayerHp()  { return playerHp;  }
    public int     getPlayerMp()  { return playerMp;  }
    public int     getEnemyHp()   { return enemyHp;   }
    public boolean playerWon()    { return playerWon; }
}