import Characters.*;
import Characters.Character;
import java.awt.*;
import java.awt.event.*;
import java.awt.geom.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.imageio.ImageIO;
import javax.sound.sampled.*;
import javax.swing.*;

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

    // battle logic objects
    private Character playerCharacter;
    private Character enemyCharacter;

    // Battle log
    private List<String> log = new ArrayList<>();
    private static final int MAX_LOG = 6;

    private JTextArea logArea;
    private JScrollPane logScroll;

    //Battle timer
    private Timer idleTimer;
    private Timer countdownTimer;
    private int timeLeft = 20;

    // Buttons — label for index 1 (SKILL) changes per character
    private String[] getActionLabels() {
        String skill1 = playerCharacter.getSkillName(1).toUpperCase();
        String skill2 = playerCharacter.getSkillName(2).toUpperCase();
        String skill3 = playerCharacter.getSkillName(3).toUpperCase();

        return new String[]{ skill1, skill2, skill3, "POTION" };
    }

    // 3 skills + Potion
    private Rectangle[] btnRects = new Rectangle[4];
    private int hoveredBtn = -1;

    // Animation
    private Timer animTimer;
    private int   animTick    = 0;
    private float shakeX      = 0;
    private int   shakeTicks  = 0;
    private boolean enemyShake  = false;
    private boolean playerShake = false;

    private boolean isHealingPhase = false;

    private float displayPlayerHp = 0;
    private float displayEnemyHp = 0;

    // Enemy sprite
    private BufferedImage enemySpriteSheet;

    private int enemyFrame = 0;

    private int enemyFrameTick = 0;
    private int enemyFrameSpeed = 6;

    private int enemyMaxFrames = 1;

    private int frameWidth;
    private int frameHeight;

    private BufferedImage skillSpriteSheet;
    private int skillFrame = 0;
    private int skillFrameTick = 0;
    private int skillFrameSpeed = 2;
    private int skillMaxFrames = 1;
    private int skillFrameWidth;
    private int skillFrameHeight;
    private int skillAnimTicks = 0;
    private int activeSkillNumber = 0;
    private boolean skillOnEnemy = false;
    private boolean skillByEnemy = false;
    private BufferedImage cachedBattleSprite;
    private BufferedImage cachedCleanedBattleSprite;
    private final Map<String, BufferedImage> imageCache = new HashMap<>();


    // Snapshot of the enemy's draw position at the moment a skill fires.
    // Used so the skill animation stays locked to the same spot as the idle sprite.
    private int frozenEnemyX = 0;
    private int frozenEnemyY = 0;
    private int frozenEnemyW = 0;
    private int frozenEnemyH = 0;
    private int frozenEnemyVisibleX = 0;
    private int frozenEnemyVisibleY = 0;
    private int frozenEnemyVisibleW = 0;
    private int frozenEnemyVisibleH = 0;

    private int frozenPlayerX = 0;
    private int frozenPlayerY = 0;
    private int frozenPlayerW = 0;
    private int frozenPlayerH = 0;
    private int frozenPlayerVisibleX = 0;
    private int frozenPlayerVisibleY = 0;
    private int frozenPlayerVisibleW = 0;
    private int frozenPlayerVisibleH = 0;

    // Per-character battle sprites (loaded once, swapped on character change)
    private BufferedImage ayaBattleSprite;
    private BufferedImage ronnixBattleSprite;
    private BufferedImage jakaraBattleSprite;
    private BufferedImage wensSprite;

    //companion-battle
    private int wensFadeTicks = 0;
    private static final int WENS_FADE_MAX = 60;

    // Flash effect
    private int flashTicks = 0;
    private Color flashColor = Color.WHITE;

    // Callback: called when battle ends (win or lose)
    private Runnable onBattleEnd;
    private boolean playerWon = false;

    private boolean usedPotionThisTurn = false;

    // Which character the player chose
    private String selectedCharacter;

    public BattleScreen(GameWindow window, GameScreen gameScreen) {
        this.window = window;
        this.gameScreen = gameScreen;
        setBackground(DARK_BG);
        setFocusable(true);

        // === SCROLLABLE LOG ===
        logArea = new JTextArea();
        logArea.setEditable(false);
        logArea.setFont(new Font("Serif", Font.PLAIN, 14));
        logArea.setForeground(new Color(240, 230, 200));
        logArea.setBackground(new Color(10, 5, 20));
        logArea.setLineWrap(true);
        logArea.setWrapStyleWord(true);

        logArea.setBorder(BorderFactory.createEmptyBorder(10, 12, 10, 12));

        logScroll = new JScrollPane(logArea);
        logScroll.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_ALWAYS);

        logScroll.setBorder(BorderFactory.createLineBorder(new Color(100, 65, 15), 2));
        logScroll.setViewportBorder(null);
        logScroll.getViewport().setBackground(new Color(10, 5, 20));

        setLayout(null);
        add(logScroll);

        loadAllSprites();
        setupListeners();

        animTimer = new Timer(16, e -> {
            enemyFrameTick++;

            if (enemyFrameTick >= enemyFrameSpeed) {

                enemyFrameTick = 0;

                enemyFrame =
                        (enemyFrame + 1) % enemyMaxFrames;

            }

            animTick++;
            if (wensFadeTicks > 0) {
                wensFadeTicks--;
            }

            if (shakeTicks > 0) {
                shakeTicks--;
                shakeX = (shakeTicks % 2 == 0) ? 6 : -6;
            } else {
                shakeX = 0;
            }

            if (flashTicks > 0) flashTicks--;

            displayPlayerHp += (playerCharacter.getHp() - displayPlayerHp) * 0.15f;
            if (Math.abs(displayPlayerHp - playerCharacter.getHp()) < 0.5f) {
                displayPlayerHp = playerCharacter.getHp();
            }

            displayEnemyHp += (enemyCharacter.getHp() - displayEnemyHp) * 0.15f;

            if (skillAnimTicks > 0) {
                skillAnimTicks--;

                skillFrameTick++;

                if (skillFrameTick >= skillFrameSpeed) {
                    skillFrameTick = 0;
                    skillFrame = (skillFrame + 1) % skillMaxFrames;
                }
            }

            repaint();
        });
    }

    @Override
    public void doLayout() {
        super.doLayout();
        int btnAreaHeight = 90;
        int logHeight = 110;
        int gap = 20;

        logScroll.setBounds(
                40,
                getHeight() - btnAreaHeight - logHeight - gap,
                getWidth() - 80,
                logHeight
        );
    }

    // ── Sprite loading ────────────────────────────────────────────────────────

    /**
     * Loads all battle sprites upfront so there is no disk I/O mid-battle.
     * Each character has a dedicated "fighting stance" image stored in images/.
     */
    private void loadAllSprites() {
        ayaBattleSprite    = tryLoadImage("images/aya/ayaBattle.png");
        ronnixBattleSprite = tryLoadImage("images/ronnix/ronnixBattle.png");
        jakaraBattleSprite = tryLoadImage("images/jakara/jakaraBattle.png");
        wensSprite = tryLoadImage("images/Wens.png");

        String[] skillSheets = {
                "images/Aya_ChasingArrow.png",
                "images/Aya_MetalRain.png",
                "images/Aya_SagittariusPunishment.png",
                "images/Ronnix_SwordSlash.png",
                "images/Ronnix_DeathThrust.png",
                "images/Ronnix_DivineStrike.png",
                "images/Bloodmancer_CurseWave.png",
                "images/Bloodmancer_Hemoburst.png",
                "images/Bloodmancer_SoulDrain.png",
                "images/Noctyx_DarkSlash.png",
                "images/Noctyx_ShadowBlink.png",
                "images/Noctyx_VoidBurst.png",
                "images/Khai_Codechum.png",
                "images/Khai_Execute.png",
                "images/Khai_SuddenQuiz.png"
        };

        for (String sheet : skillSheets) {
            tryLoadImage(sheet);
        }
    }

    /** Returns the loaded BufferedImage, or null if the file is missing. */
    private BufferedImage tryLoadImage(String path) {
        if (imageCache.containsKey(path)) {
            return imageCache.get(path);
        }

        try {
            File f = new File(path);
            if (f.exists()) {
                BufferedImage img = ImageIO.read(f);
                imageCache.put(path, img);
                return img;
            }
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
        // Do NOT create a new playerCharacter here — the real one is passed in via startBattle().
        // This method only stores the name so the correct battle sprite is displayed.
        repaint();
    }

    // ── Battle lifecycle ──────────────────────────────────────────────────────

    private void startIdleTimer() {
        stopIdleTimer();

        timeLeft = 20;

        countdownTimer = new Timer(1000, e -> {
            timeLeft--;
            repaint();

            if (timeLeft <= 0) {
                ((Timer)e.getSource()).stop();
            }
        });

        countdownTimer.start();

        idleTimer = new Timer(20000, e -> {
            if (phase == Phase.PLAYER_TURN) {
                addLog("Time's up! Enemy attacks!");

                phase = Phase.ENEMY_TURN;
                enemyTurn();
            }
        });

        idleTimer.setRepeats(false);
        idleTimer.start();
    }

    private void stopIdleTimer() {
        if (idleTimer != null) {
            idleTimer.stop();
        }

        if (countdownTimer != null) {
            countdownTimer.stop();
        }
    }

    public void startBattle(Character player, Character enemy, Runnable onEnd) {
        // Stop any previously running timers before starting fresh
        stopBattle();
        stopIdleTimer();

        this.usedPotionThisTurn = false;
        this.hoveredBtn = -1;
        this.shakeTicks = 0;
        this.flashTicks = 0;
        this.skillSpriteSheet = null;
        this.skillAnimTicks = 0;
        this.skillFrame = 0;
        this.skillFrameTick = 0;
        this.skillMaxFrames = 1;
        this.skillFrameWidth = 0;
        this.skillFrameHeight = 0;
        this.activeSkillNumber = 0;
        this.skillOnEnemy = false;
        this.cachedBattleSprite = null;
        this.cachedCleanedBattleSprite = null;
        this.frozenEnemyX = 0;
        this.frozenEnemyY = 0;
        this.frozenEnemyW = 0;
        this.frozenEnemyH = 0;
        this.frozenEnemyVisibleX = 0;
        this.frozenEnemyVisibleY = 0;
        this.frozenEnemyVisibleW = 0;
        this.frozenEnemyVisibleH = 0;
        this.frozenPlayerVisibleX = 0;
        this.frozenPlayerVisibleY = 0;
        this.frozenPlayerVisibleW = 0;
        this.frozenPlayerVisibleH = 0;

        this.playerCharacter = player;
        this.enemyCharacter = enemy;

        enemySpriteSheet =
                tryLoadImage(enemyCharacter.getSpritePath());

        frameWidth  = enemyCharacter.getFrameWidth();
        frameHeight = enemyCharacter.getFrameHeight();

        if (enemySpriteSheet != null) {
            enemyMaxFrames = enemySpriteSheet.getWidth() / frameWidth;
            enemyFrame = 0;
        }

        this.enemyCharacter.setLevel(playerCharacter.getLevel());
        this.enemyCharacter.restoreStats();

        playerCharacter.resetTurnCounter();

        this.onBattleEnd = onEnd;
        this.phase = Phase.PLAYER_TURN;
        this.playerWon = false;

        displayPlayerHp = player.getHp();
        displayEnemyHp = enemy.getHp();

        logArea.setText("");
        addLog("A " + enemyCharacter.getName() + " appeared!");
        addLog("Your turn — choose an action.");
        startIdleTimer();

        animTimer.start();
        repaint();
    }

    public void stopBattle() { animTimer.stop(); }

    private void playSkillAnimation(Character attacker, int skillNumber, boolean attackerIsEnemy) {
        String path = attacker.getSkillSprite(skillNumber);

        if (path == null) return;

        skillSpriteSheet = tryLoadImage(path);
        if (skillSpriteSheet == null) return;

        // Boss/enemy skill animations always play on the enemy side (right),
        // centred on the enemy sprite — they are visual effects emanating FROM
        // the boss, not a projectile hitting the player.
        skillByEnemy = attackerIsEnemy;
        skillOnEnemy = attackerIsEnemy;

        // Always reset to frame 0 so every skill starts from the beginning.
        skillFrame     = 0;
        skillFrameTick = 0;

        skillFrameWidth  = attacker.getSkillFrameWidth(skillNumber);
        skillFrameHeight = attacker.getSkillFrameHeight(skillNumber);
        skillMaxFrames   = attacker.getSkillMaxFrames(skillNumber);
        activeSkillNumber = skillNumber;
        // Give enough ticks so every frame is shown at the chosen speed.
        skillAnimTicks = skillMaxFrames * skillFrameSpeed + skillFrameSpeed;

        // frozenEnemyX/Y/W/H are kept up-to-date every paint frame (drawEnemyArea),
        // so they are already correct at this point — no extra snapshot call needed.
    }

    /**
     * Calculates and stores the enemy's current draw rect (without bounce)
     * so skill animations can be pinned to the same spot.
     * Uses the same sizing logic as drawEnemyArea so the snapshot matches exactly.
     */
    private void snapshotEnemyPosition() {
        int W = getWidth(), H = getHeight();
        if (W <= 0 || H <= 0) return; // not yet laid out — skip
        int groundY = getEnemyGroundY(H);
        int centerX = (int)(W * 0.65);

        int[] size = computeEnemyDrawSize(groundY);
        int eW = size[0], eH = size[1];

        int ex = centerX - eW / 2;
        int ey = groundY - eH + enemyCharacter.getVerticalOffset();

        frozenEnemyX = ex;
        frozenEnemyY = ey;
        frozenEnemyW = eW;
        frozenEnemyH = eH;
    }

    /**
     * Computes the draw size [width, height] for the enemy sprite.
     * Bosses (isFinalBoss) are rendered at 3× the average player sprite size
     * (~120×160 px), giving a target of ~360×480 px scaled to fit the screen.
     * Regular enemies use the character's getScale() value.
     */
    private int[] computeEnemyDrawSize(int groundY) {
        int eW, eH;

        if (enemyCharacter.isFinalBoss()) {
            // Use the character's own height fraction so mid-tier bosses like
            // Bloodmancer can be smaller than the true final bosses (Khai, Noctyx).
            int targetH = (int)(groundY * enemyCharacter.getBossHeightFraction());
            double aspect = (double) frameWidth / frameHeight;
            eH = targetH;
            eW = (int)(eH * aspect);
        } else {
            double scale = enemyCharacter.getScale();
            eW = (int)(frameWidth  * scale);
            eH = (int)(frameHeight * scale);

            // Regular enemies should be readable and stay clear of the battle log.
            int minAllowedH = 170;
            if (eH < minAllowedH) {
                double ratio = (double) minAllowedH / eH;
                eH = minAllowedH;
                eW = (int)(eW * ratio);
            }

            int maxAllowedH = (int)(groundY * 0.48);
            if (eH > maxAllowedH) {
                double ratio = (double) maxAllowedH / eH;
                eH = maxAllowedH;
                eW = (int)(eW * ratio);
            }
        }

        return new int[]{eW, eH};
    }

    private int getEnemyGroundY(int H) {
        if (isBloodmancer()) {
            int liftedBossLine = (int)(H * 0.62);
            if (logScroll != null && logScroll.getY() > 0) {
                liftedBossLine = Math.min(liftedBossLine, logScroll.getY() - 44);
            }
            return liftedBossLine;
        }

        if (enemyCharacter != null && enemyCharacter.isFinalBoss()) {
            return (int)(H * 0.72);
        }

        int liftedBattleLine = (int)(H * 0.60);
        if (logScroll != null && logScroll.getY() > 0) {
            liftedBattleLine = Math.min(liftedBattleLine, logScroll.getY() - 32);
        }
        return liftedBattleLine;
    }

    private boolean isBloodmancer() {
        return enemyCharacter != null && "Bloodmancer".equalsIgnoreCase(enemyCharacter.getName());
    }

    // ── Input ─────────────────────────────────────────────────────────────────
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

                if (phase != Phase.PLAYER_TURN || isHealingPhase) return;

                stopIdleTimer();

                for (int i = 0; i < btnRects.length; i++) {
                    if (btnRects[i] != null && btnRects[i].contains(e.getPoint())) {
                        handleAction(i);
                        break;
                    }
                }
            }
        });
    }

    private void handleAction(int index) {
        switch (index) {
            case 0 -> doSkill(1, new Color(255, 80, 80, 180));
            case 1 -> doSkill(2, new Color(180, 80, 255, 180));
            case 2 -> doSkill(3, new Color(255, 180, 80, 180));
            case 3 -> doItems();
        }
    }

    // ── Player actions ────────────────────────────────────────────────────────
    private void playSfx(String fileName) {
        try {
            File soundFile = new File("sounds/" + fileName);
            AudioInputStream audio = AudioSystem.getAudioInputStream(soundFile);

            Clip clip = AudioSystem.getClip();
            clip.open(audio);
            clip.start();

        } catch (Exception e) {
            System.err.println("Error playing sound: " + fileName);
            e.printStackTrace();
        }
    }

    public void doSkill(int skillNumber, Color fxColor) {
        if (!playerCharacter.isSkillAvailable(skillNumber)) {
            addLog("Skill on cooldown! " + playerCharacter.getSkillCooldown(skillNumber) + " turns left.");
            return;
        }

        String sfx = playerCharacter.getSkillSfx(skillNumber);
        if (sfx != null) {
            SoundManager.playSfx(sfx);
        }

        int dmg = playerCharacter.useSkill(skillNumber, enemyCharacter);

        playSkillAnimation(playerCharacter, skillNumber, false);

        addLog(playerCharacter.getName() + " used " + playerCharacter.getSkillName(skillNumber) + "!");
        addLog("Dealt " + dmg + " damage!");

        triggerShake(true);
        triggerFlash(fxColor);

        afterPlayerAction();
    }

    private void doItems() {

        // Prevent multiple uses in one turn
        if (usedPotionThisTurn) {
            addLog("You already used a potion this turn!");
            return;
        }

        // Check if player has potion
        if (playerCharacter.getHealthPotion() <= 0) {
            addLog("No HP potions left!");
            return;
        }

        // Use potion
        String result = playerCharacter.useHealthPotion();
        displayPlayerHp = playerCharacter.getHp();

        // Display result in log
        for (String line : result.split("\n")) {
            addLog(line);
        }

        repaint();

        usedPotionThisTurn = true;

    }

    /*
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
    */


    private void afterPlayerAction() {
        usedPotionThisTurn = false;
        playerCharacter.reduceCooldowns();

        if (enemyCharacter.getHp() <= 0) {
            addLog(enemyCharacter.getName() + " was defeated!");

            int gainedXp;

            if (enemyCharacter.isFinalBoss()) {
                gainedXp = playerCharacter.getNextLevelXp() - playerCharacter.getCurrentXp();
            } else {
                gainedXp = enemyCharacter.getXpReward();
            }

            int gainedGold = enemyCharacter.getGoldReward();

            playerCharacter.receiveBattleRewards(gainedXp, gainedGold);
            playerCharacter.restoreStats();
            playerCharacter.resetCooldowns();
            enemyCharacter.restoreStats();

            addLog("Gained " + gainedXp + " XP and " + gainedGold + " gold!");
            addLog("HP restored and cooldowns reset.");

            phase = Phase.VICTORY;
            playerWon = true;
            stopIdleTimer();
            repaint();
            return;
        }

        int waitForPlayerSkill = (skillAnimTicks > 0 && !skillByEnemy)
                ? skillAnimTicks * 16 + 80
                : 0;

        isHealingPhase = true;
        Timer wait = new Timer(waitForPlayerSkill, e -> {
            isHealingPhase = false;
            beginEnemyTurn();
            ((Timer)e.getSource()).stop();
        });
        wait.setRepeats(false);
        wait.start();
    }

    private void beginEnemyTurn() {
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

        String sfx = enemyCharacter.getSkillSfx(skillNum);
        if (sfx != null) {
            SoundManager.playSfx(sfx);
        }

        int dealt = enemyCharacter.useSkill(skillNum, playerCharacter);

        playSkillAnimation(enemyCharacter, skillNum, true);

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

        Timer healDelay = new Timer(900, e -> {

            isHealingPhase = true;

            if (playerCharacter.getLevel() >= 4) {

                String healMsg = playerCharacter.handleAutoHeal();

                if (healMsg != null) {
                    addLog(healMsg);
                    showWensHealEffect();
                }

            }

            // Wait for Wens animation before unlocking
            Timer unlock = new Timer(1200, ev -> {
                isHealingPhase = false;

                phase = Phase.PLAYER_TURN;
                addLog("Your turn — choose an action.");
                startIdleTimer();
                repaint();

                ((Timer)ev.getSource()).stop();
            });

            unlock.setRepeats(false);
            unlock.start();

            ((Timer)e.getSource()).stop();
        });

        healDelay.setRepeats(false);
        healDelay.start();
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

    // ── Helpers ───────────────────────────────────────────────────────────────
    private void addLog(String msg) {
        logArea.append(msg + "\n");

        // auto scroll to bottom
        logArea.setCaretPosition(logArea.getDocument().getLength());
    }

    private void triggerShake(boolean isEnemy) {
        shakeTicks  = 10;
        enemyShake  = isEnemy;
        playerShake = !isEnemy;
    }

    private void triggerFlash(Color c) {
        flashColor = c;
        flashTicks = 8;
    }


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
        drawWensEffect(g2, W, H);
        drawSkillAnimation(g2, W, H);
        drawHpBars(g2, W, H);
        drawTurnTimer(g2, W, H);
        drawActionButtons(g2, W, H);

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

        // Ground line — visual midpoint between player stand (65%) and boss stand (82%)
        int groundY = getEnemyGroundY(H);
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
        int groundY = getEnemyGroundY(H);
        int centerX = (int)(W * 0.65);

        // Freeze bounce while the skill animation is playing so the sprite
        // reappears exactly where it was when the skill fired.
        boolean enemyUsingSkill2 = skillAnimTicks > 0 && skillOnEnemy;
        double bounce = enemyUsingSkill2 ? 0 : Math.sin(animTick * 0.05) * 6;

        int[] size = computeEnemyDrawSize(groundY);
        int eW = size[0], eH = size[1];

        int ex = centerX - eW / 2;
        int ey = groundY - eH + enemyCharacter.getVerticalOffset() + (int)bounce;

        // Keep the frozen snapshot up-to-date every frame when idle.
        // This guarantees that when a skill fires, snapshotEnemyPosition()
        // returns values that exactly match what was just painted.
        if (!enemyUsingSkill2) {
            frozenEnemyX = ex;
            frozenEnemyY = (int)(groundY - eH + enemyCharacter.getVerticalOffset()); // no bounce
            frozenEnemyW = eW;
            frozenEnemyH = eH;

            if (enemySpriteSheet != null && frameWidth > 0 && frameHeight > 0) {
                int sx = enemyFrame * frameWidth;
                Rectangle visibleSource = findVisibleBounds(enemySpriteSheet, sx, 0, frameWidth, frameHeight);
                Rectangle visibleDest = mapSourceBoundsToDest(
                        visibleSource,
                        sx, 0,
                        frameWidth, frameHeight,
                        frozenEnemyX, frozenEnemyY,
                        frozenEnemyW, frozenEnemyH
                );
                frozenEnemyVisibleX = visibleDest.x;
                frozenEnemyVisibleY = visibleDest.y;
                frozenEnemyVisibleW = visibleDest.width;
                frozenEnemyVisibleH = visibleDest.height;
            } else {
                frozenEnemyVisibleX = frozenEnemyX;
                frozenEnemyVisibleY = frozenEnemyY;
                frozenEnemyVisibleW = frozenEnemyW;
                frozenEnemyVisibleH = frozenEnemyH;
            }
        }

        // Scale the ground shadow to match the enemy footprint
        int shadowW = Math.max(60, eW / 2);
        g2.setColor(new Color(0, 0, 0, 80));
        g2.fillOval(centerX - shadowW / 2, groundY - 10, shadowW, 20);

        int offX = (enemyShake && shakeTicks > 0) ? (int)shakeX : 0;

        // Hide the enemy sprite while its own skill animation is playing —
        // the skill sheet acts as a full replacement visual for that duration.
        boolean enemyUsingSkill = enemyUsingSkill2;

        if (enemySpriteSheet != null && !enemyUsingSkill) {

            int sx = enemyFrame * frameWidth;

            g2.drawImage(
                    enemySpriteSheet,
                    ex + offX, ey,
                    ex + offX + eW, ey + eH,
                    sx, 0,
                    sx + frameWidth, frameHeight,
                    null
            );

        }

        // Enemy name tag
        g2.setFont(new Font("Serif", Font.BOLD, 16));
        FontMetrics fm = g2.getFontMetrics();
        String tag = (enemyCharacter != null) ? enemyCharacter.getName() : "Enemy";
        int tx = centerX - fm.stringWidth(tag) / 2;
        g2.setColor(new Color(0, 0, 0, 160));
        g2.drawString(tag, tx + 2, ey - 10 + 2);
        g2.setColor(GOLD_LIGHT);
        g2.drawString(tag, tx, ey - 10);
    }

    /**
     * Draws the player's character in their fighting stance.
     * Each character has a unique battle sprite and thematic visual effects.
     */
    private void drawPlayerArea(Graphics2D g2, int W, int H) {
        // Player always stands at 65% of screen height — independent of where
        // the boss ground line is — so the character is never cut off.
        int groundY = (int)(H * 0.65);
        int centerX = (int)(W * 0.28);

        boolean isRonnix = "Ronnix".equalsIgnoreCase(selectedCharacter);
        boolean isJakara = "Jakara".equalsIgnoreCase(selectedCharacter);
        boolean isAya = "Aya".equalsIgnoreCase(selectedCharacter);

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

        boolean playerUsingSkill = skillAnimTicks > 0 && !skillByEnemy;

        if (!playerUsingSkill) {
            frozenPlayerX = px;
            frozenPlayerY = py;
            frozenPlayerW = pW;
            frozenPlayerH = pH;
        }

        int offX = (playerShake && shakeTicks > 0) ? (int)shakeX : 0;

        BufferedImage sprite = currentPlayerSprite();

        if (!playerUsingSkill && sprite != null) {
            BufferedImage cleaned = getCleanedBattleSprite(sprite);
            Rectangle visibleSource = findVisibleBounds(cleaned, 0, 0, cleaned.getWidth(), cleaned.getHeight());
            Rectangle visibleDest = mapSourceBoundsToDest(
                    visibleSource,
                    0, 0,
                    cleaned.getWidth(), cleaned.getHeight(),
                    px, py,
                    pW, pH
            );
            frozenPlayerVisibleX = visibleDest.x;
            frozenPlayerVisibleY = visibleDest.y;
            frozenPlayerVisibleW = visibleDest.width;
            frozenPlayerVisibleH = visibleDest.height;
        } else if (!playerUsingSkill) {
            frozenPlayerVisibleX = frozenPlayerX;
            frozenPlayerVisibleY = frozenPlayerY;
            frozenPlayerVisibleW = frozenPlayerW;
            frozenPlayerVisibleH = frozenPlayerH;
        }

        // ── Per-character ambient effects drawn BEHIND the sprite ─────────────
        if (!playerUsingSkill && isJakara) {
            // Pulsing arcane aura — violet glow
            float pulse = 0.4f + 0.3f * (float)Math.sin(animTick * 0.07);
            int auraAlpha = (int)(pulse * 100);
            // Outer soft glow
            g2.setColor(new Color(160, 80, 255, auraAlpha / 2));
            g2.fillOval(px + offX - 20, py - 15, pW + 40, pH + 30);
            // Inner brighter core
            g2.setColor(new Color(200, 130, 255, auraAlpha));
            g2.fillOval(px + offX - 8, py - 4, pW + 16, pH + 8);

        } else if (!playerUsingSkill && isRonnix) {
            // Smoldering ember glow — dark red/orange
            float pulse = 0.3f + 0.2f * (float)Math.sin(animTick * 0.05);
            int auraAlpha = (int)(pulse * 80);
            g2.setColor(new Color(200, 60, 20, auraAlpha));
            g2.fillOval(px + offX - 10, py + pH / 3, pW + 20, pH * 2 / 3 + 10);

        } else if (!playerUsingSkill) {
            // Aya — soft wind shimmer in teal/white
            float pulse = 0.3f + 0.2f * (float)Math.sin(animTick * 0.06);
            int auraAlpha = (int)(pulse * 60);
            g2.setColor(new Color(100, 200, 220, auraAlpha));
            g2.fillOval(px + offX - 8, py - 4, pW + 16, pH + 8);
        }

        // ── Draw the actual battle sprite ─────────────────────────────────────
        if (sprite != null && !playerUsingSkill) {
            // Strip near-white background if the source image has one
            BufferedImage cleaned = getCleanedBattleSprite(sprite);
            g2.drawImage(cleaned, px + offX, py, pW, pH, null);
        } else if (!playerUsingSkill) {
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
        // Match white, near-white, and the purple/lavender battle-sprite matte.
        if (a == 0 || isBattleSpriteBackground(r, g, b))
            q.add(new int[]{x, y});
    }

    private boolean isBattleSpriteBackground(int r, int g, int b) {
        boolean nearWhite = r > 195 && g > 195 && b > 195;
        boolean lavenderEdge = b > 170 && r > 80 && g > 45 && b > r + 25;
        return nearWhite || lavenderEdge;
    }

    private BufferedImage getCleanedBattleSprite(BufferedImage sprite) {
        if (sprite != cachedBattleSprite) {
            cachedBattleSprite = sprite;
            cachedCleanedBattleSprite = stripWhiteBackground(sprite);
        }
        return cachedCleanedBattleSprite;
    }

    //Timer
    private void drawTurnTimer(Graphics2D g2, int W, int H) {
        if (phase != Phase.PLAYER_TURN) return;

        String timerText = "Time Left: " + timeLeft + "s";

        g2.setFont(new Font("Serif", Font.BOLD, 22));
        FontMetrics fm = g2.getFontMetrics();

        int boxW = 180;
        int boxH = 40;
        int x = (W - boxW) / 2;
        int y = 25;

        g2.setColor(new Color(10, 5, 20, 220));
        g2.fillRoundRect(x, y, boxW, boxH, 12, 12);

        g2.setColor(GOLD_LIGHT);
        g2.drawRoundRect(x, y, boxW, boxH, 12, 12);

        int textX = x + (boxW - fm.stringWidth(timerText)) / 2;
        int textY = y + 27;

        g2.drawString(timerText, textX, textY);
    }

    // ── Stats bars ────────────────────────────────────────────────────────────
    private void drawHpBars(Graphics2D g2, int W, int H) {
        if (playerCharacter == null || enemyCharacter == null) return;

        drawStatBar(g2, W - 340, 30, 300, "ENEMY HP",
                (int)displayEnemyHp, enemyCharacter.getMaxHp(), RED_HP);

        drawStatBar(g2, W - 340, 75, 300, "ENEMY DEF", enemyCharacter.getDefense(), enemyCharacter.getMaxDefense(), new Color(80, 120, 220));

        drawStatBar(g2, 40, 30, 300, "HP",
                (int)displayPlayerHp, playerCharacter.getMaxHp(), GREEN_HP);

        drawStatBar(g2, 40, 75, 300, "DEF", playerCharacter.getDefense(), playerCharacter.getMaxDefense(), new Color(80, 120, 220));


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

        boolean locked = (phase == Phase.ENEMY_TURN || isHealingPhase);
        String[] ACTIONS = getActionLabels();

        for (int i = 0; i < ACTIONS.length; i++) {
            int bx = startX + i * (btnW + gap);
            btnRects[i] = new Rectangle(bx, btnY, btnW, btnH);
            boolean hov = (hoveredBtn == i && !locked);
            drawButton(g2, ACTIONS[i], bx, btnY, btnW, btnH, hov, locked);
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
        Color bg     = locked  ? new Color(50, 50, 60, 180)   // gray
                : hovered ? new Color(70, 40, 10, 220)
                  : new Color(25, 12, 35, 200);

        Color border = locked  ? new Color(120, 120, 130)     // light gray border
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
        String[] words = label.split(" ");

        if (words.length >= 2) {
            String line1 = words[0];
            String line2 = label.substring(line1.length()).trim();

            int ty = y + h / 2 - 2;

            // Line 1
            int tx1 = x + (w - fm.stringWidth(line1)) / 2;
            g2.drawString(line1, tx1, ty - 5);

            // Line 2
            int tx2 = x + (w - fm.stringWidth(line2)) / 2;
            g2.drawString(line2, tx2, ty + 15);

        } else {
            int tx = x + (w - fm.stringWidth(label)) / 2;
            int ty = y + (h + fm.getAscent() - fm.getDescent()) / 2;
            g2.setColor(locked ? new Color(180, 180, 180) : GOLD_LIGHT);
            g2.drawString(label, tx, ty);
        }
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

    //Companion
    private void showWensHealEffect() {
        SoundManager.playSfx("Wens_heal.wav");
        wensFadeTicks = WENS_FADE_MAX;
    }

    private void drawWensEffect(Graphics2D g2, int W, int H) {
        if (wensFadeTicks <= 0 || wensSprite == null) return;

        int elapsed = WENS_FADE_MAX - wensFadeTicks;

        int wensW = 500;
        int wensH = 300;

        int startX = -wensW;
        int targetX = 40;

        int slideDuration = 20;     // slide time
        int visibleDuration = 10;   // stay fully visible

        int x;
        float alpha = 1f;

        if (elapsed < slideDuration) {
            //SLIDE PHASE
            float progress = elapsed / (float) slideDuration;
            x = startX + (int)((targetX - startX) * progress);
            alpha = 1f; // no fade yet

        } else if (elapsed < slideDuration + visibleDuration) {
            // STAY PHASE
            x = targetX;
            alpha = 1f;

        } else {
            // FADE PHASE
            x = targetX;

            int fadeElapsed = elapsed - (slideDuration + visibleDuration);
            int fadeDuration = WENS_FADE_MAX - (slideDuration + visibleDuration);

            alpha = 1f - (fadeElapsed / (float) fadeDuration);
        }

        Composite old = g2.getComposite();
        g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, Math.max(0, alpha)));

        int y = (int)(H * 0.20);
        g2.drawImage(wensSprite, x, y, wensW, wensH, null);

        g2.setComposite(old);
    }

    private void drawSkillAnimation(Graphics2D g2, int W, int H) {
        if (skillAnimTicks <= 0 || skillSpriteSheet == null) return;

        int safeFrame = skillFrame % skillMaxFrames;
        int sx = safeFrame * skillFrameWidth;

        int baseX = skillByEnemy ? frozenEnemyX : frozenPlayerX;
        int baseY = skillByEnemy ? frozenEnemyY : frozenPlayerY;
        int baseW = skillByEnemy ? frozenEnemyW : frozenPlayerW;
        int baseH = skillByEnemy ? frozenEnemyH : frozenPlayerH;

        Rectangle sourceBounds = new Rectangle(sx, 0, skillFrameWidth, skillFrameHeight);
        drawImageSameHeight(g2, skillSpriteSheet, sourceBounds, baseX, baseY, baseW, baseH);
    }

    private void drawImageSameHeight(Graphics2D g2, BufferedImage img, Rectangle src,
                                     int destX, int destY, int destW, int destH) {
        if (destW <= 0 || destH <= 0 || src.width <= 0 || src.height <= 0) return;

        double scale = (double)destH / src.height;
        int drawW = Math.max(1, (int)Math.round(src.width * scale));
        int drawH = destH;
        int drawX = destX + (destW - drawW) / 2;
        int drawY = destY;

        g2.drawImage(
                img,
                drawX, drawY,
                drawX + drawW, drawY + drawH,
                src.x, src.y,
                src.x + src.width,
                src.y + src.height,
                null
        );
    }

    private Rectangle findVisibleBounds(BufferedImage img, int startX, int startY, int w, int h) {
        int minX = startX + w;
        int minY = startY + h;
        int maxX = startX;
        int maxY = startY;

        for (int y = startY; y < startY + h; y++) {
            for (int x = startX; x < startX + w; x++) {
                int alpha = (img.getRGB(x, y) >> 24) & 0xFF;
                if (alpha > 20) {
                    if (x < minX) minX = x;
                    if (y < minY) minY = y;
                    if (x > maxX) maxX = x;
                    if (y > maxY) maxY = y;
                }
            }
        }

        if (maxX < minX || maxY < minY) {
            return new Rectangle(startX, startY, w, h);
        }

        return new Rectangle(minX, minY, maxX - minX + 1, maxY - minY + 1);
    }

    private Rectangle mapSourceBoundsToDest(Rectangle sourceBounds,
                                            int sourceX, int sourceY,
                                            int sourceW, int sourceH,
                                            int destX, int destY,
                                            int destW, int destH) {
        double scaleX = (double)destW / Math.max(1, sourceW);
        double scaleY = (double)destH / Math.max(1, sourceH);

        int x = destX + (int)Math.round((sourceBounds.x - sourceX) * scaleX);
        int y = destY + (int)Math.round((sourceBounds.y - sourceY) * scaleY);
        int w = Math.max(1, (int)Math.round(sourceBounds.width * scaleX));
        int h = Math.max(1, (int)Math.round(sourceBounds.height * scaleY));

        return new Rectangle(x, y, w, h);
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

    public boolean playerWon()    { return playerWon; }
}
