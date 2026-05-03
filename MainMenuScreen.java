import java.awt.*;
import java.awt.event.*;
import java.awt.geom.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import javax.imageio.ImageIO;
import javax.swing.*;

public class MainMenuScreen extends JPanel {

    private GameWindow window;
    private BufferedImage background;

    // Animation
    private Timer animTimer;
    private float flickerAlpha = 1f;
    private float flickerDir   = -0.008f;
    private float titleGlow    = 0f;
    private float titleGlowDir = 0.02f;

    private static final Color GOLD       = new Color(201, 150, 58);
    private static final Color GOLD_LIGHT = new Color(240, 192, 96);
    private static final Color GOLD_DARK  = new Color(120, 80, 20);
    private static final Color CRIMSON    = new Color(139, 26, 26);
    private static final Color EMBER      = new Color(200, 80, 20);
    private static final Color SMOKE_BG   = new Color(15, 8, 5);

    // Menu items
    private static final String[] LABELS = { "START GAME", "SETTINGS", "LEADERBOARD", "EXIT GAME" };
    private int hoveredIndex = -1;
    private Rectangle[] buttonRects = new Rectangle[LABELS.length];

    public MainMenuScreen(GameWindow window) {
        this.window = window;
        setPreferredSize(new Dimension(GameWindow.WIDTH, GameWindow.HEIGHT));
        setBackground(SMOKE_BG);

        // Load background image
        try {
            background = ImageIO.read(new File("images/titleBG.png"));
        } catch (IOException e) {
            background = null; // will fall back to painted background
        }

        setupAnimation();
        setupMouseListeners();
    }

    private void setupAnimation() {
        animTimer = new Timer(16, e -> {
            flickerAlpha += flickerDir;
            if (flickerAlpha <= 0.6f || flickerAlpha >= 1f) flickerDir = -flickerDir;
            titleGlow += titleGlowDir;
            if (titleGlow >= 1f || titleGlow <= 0f) titleGlowDir = -titleGlowDir;
            repaint();
        });
        animTimer.start();
    }

    private void setupMouseListeners() {
        addMouseMotionListener(new MouseMotionAdapter() {
            @Override
            public void mouseMoved(MouseEvent e) {
                int prev = hoveredIndex;
                hoveredIndex = -1;
                for (int i = 0; i < buttonRects.length; i++) {
                    if (buttonRects[i] != null && buttonRects[i].contains(e.getPoint())) {
                        hoveredIndex = i;
                        break;
                    }
                }
                if (hoveredIndex != prev) repaint();
                setCursor(hoveredIndex >= 0
                    ? Cursor.getPredefinedCursor(Cursor.HAND_CURSOR)
                    : Cursor.getDefaultCursor());
            }
        });

        addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                for (int i = 0; i < buttonRects.length; i++) {
                    if (buttonRects[i] != null && buttonRects[i].contains(e.getPoint())) {
                        handleAction(i);
                        return;
                    }
                }
            }
        });
    }

    private void handleAction(int index) {
        switch (index) {
            case 0 -> {
                if (window.getPlayerName().trim().isEmpty()) {
                    window.showNameEntry();
                } else {
                    window.showCharacterSelect();
                }
            }

            case 1 -> window.showSettings();

            case 2 -> window.showLeaderboard();

            case 3 -> {
                int choice = JOptionPane.showConfirmDialog(
                    this, "Are you sure you want to exit?",
                    "Exit Game", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
                if (choice == JOptionPane.YES_OPTION) System.exit(0);
            }
        }
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,      RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

        int W = getWidth(), H = getHeight();

        // ── Background ──
        if (background != null) {
            g2.drawImage(background, 0, 0, W, H, null);
            // Darken lower half so buttons are readable
            g2.setColor(new Color(0, 0, 0, 100));
            g2.fillRect(0, (int)(H * 0.55), W, (int)(H * 0.45));
            // Cover the "Press +" prompt area with a dark blend
            int pressY = (int)(H * 0.62);
            int pressH = (int)(H * 0.10);
            g2.setColor(new Color(10, 5, 3, 180));
            g2.fillRect(0, pressY, W, pressH);
        } else {
            drawFallbackBackground(g2, W, H);
        }

        // ── Dark vignette ──
        drawVignette(g2, W, H);

        // Title is already in the background image — no painted title needed

        // ── Menu buttons ──
        drawMenuButtons(g2, W, H);

        g2.dispose();
    }

    private void drawFallbackBackground(Graphics2D g2, int W, int H) {
        GradientPaint sky = new GradientPaint(
            0, 0, new Color(30, 5, 5),
            0, H, new Color(8, 3, 2));
        g2.setPaint(sky);
        g2.fillRect(0, 0, W, H);

        // Ember glow at bottom
        RadialGradientPaint fire = new RadialGradientPaint(
            new Point2D.Float(W * 0.28f, H * 0.85f), H * 0.4f,
            new float[]{0f, 1f},
            new Color[]{new Color(180, 60, 10, 80), new Color(0,0,0,0)});
        g2.setPaint(fire);
        g2.fillRect(0, 0, W, H);
    }

    private void drawVignette(Graphics2D g2, int W, int H) {
        RadialGradientPaint vignette = new RadialGradientPaint(
            new Point2D.Float(W / 2f, H / 2f),
            Math.max(W, H) * 0.75f,
            new float[]{0f, 1f},
            new Color[]{new Color(0,0,0,0), new Color(0,0,0,200)});
        g2.setPaint(vignette);
        g2.fillRect(0, 0, W, H);
    }

    private void drawTitle(Graphics2D g2, int W, int H) {
        // Glow pulse
        int glowSize = (int)(8 + titleGlow * 14);
        int glowAlpha = (int)(60 + titleGlow * 80);

        // "GREAT RUINS OF" — smaller line
        Font topFont = new Font("Serif", Font.BOLD | Font.ITALIC, 54);
        g2.setFont(topFont);
        FontMetrics fm = g2.getFontMetrics();
        String topText = "GREAT RUINS OF";
        int topX = (W - fm.stringWidth(topText)) / 2;
        int topY = H / 2 - 120;

        // Shadow
        g2.setColor(new Color(0, 0, 0, 180));
        g2.drawString(topText, topX + 3, topY + 3);
        // Glow
        g2.setColor(new Color(GOLD.getRed(), GOLD.getGreen(), GOLD.getBlue(), glowAlpha));
        for (int dx = -glowSize/2; dx <= glowSize/2; dx += 3)
            for (int dy = -glowSize/2; dy <= glowSize/2; dy += 3)
                g2.drawString(topText, topX + dx, topY + dy);
        // Main
        g2.setColor(GOLD_LIGHT);
        g2.drawString(topText, topX, topY);

        // "KHAI" — huge line
        Font bigFont = new Font("Serif", Font.BOLD, 160);
        g2.setFont(bigFont);
        fm = g2.getFontMetrics();
        String bigText = "KHAI";
        int bigX = (W - fm.stringWidth(bigText)) / 2;
        int bigY = H / 2 - 10;

        // Shadow layers
        g2.setColor(new Color(80, 20, 0, 200));
        g2.drawString(bigText, bigX + 6, bigY + 6);
        g2.setColor(new Color(0, 0, 0, 160));
        g2.drawString(bigText, bigX + 3, bigY + 3);
        // Glow
        g2.setColor(new Color(GOLD.getRed(), GOLD.getGreen(), GOLD.getBlue(), glowAlpha / 2));
        for (int dx = -glowSize; dx <= glowSize; dx += 5)
            for (int dy = -glowSize; dy <= glowSize; dy += 5)
                g2.drawString(bigText, bigX + dx, bigY + dy);
        // Gradient paint for gold cracked-stone effect
        GradientPaint goldGrad = new GradientPaint(bigX, bigY - 160, GOLD_LIGHT, bigX, bigY, GOLD_DARK);
        g2.setPaint(goldGrad);
        g2.drawString(bigText, bigX, bigY);
    }

    private void drawDivider(Graphics2D g2, int W, int H) {
        int cy = H / 2 + 55;
        int lx1 = W / 2 - 260, lx2 = W / 2 - 30;
        int rx1 = W / 2 + 30,  rx2 = W / 2 + 260;

        // Left line
        GradientPaint lp = new GradientPaint(lx1, cy, new Color(0,0,0,0), lx2, cy, GOLD_DARK);
        g2.setPaint(lp);
        g2.setStroke(new BasicStroke(1.5f));
        g2.drawLine(lx1, cy, lx2, cy);

        // Right line
        GradientPaint rp = new GradientPaint(rx1, cy, GOLD_DARK, rx2, cy, new Color(0,0,0,0));
        g2.setPaint(rp);
        g2.drawLine(rx1, cy, rx2, cy);

        // Center diamond
        int ds = 6;
        int[] xp = {W/2, W/2+ds, W/2, W/2-ds};
        int[] yp = {cy-ds, cy, cy+ds, cy};
        g2.setColor(GOLD);
        g2.fillPolygon(xp, yp, 4);
        g2.setColor(GOLD_LIGHT);
        g2.setStroke(new BasicStroke(1f));
        g2.drawPolygon(xp, yp, 4);
    }

    private void drawMenuButtons(Graphics2D g2, int W, int H) {
        int btnW = 500, btnH = 56;
        int gap = 14;
        int totalH = LABELS.length * btnH + (LABELS.length - 1) * gap;
        int startY = Math.min((int)(H * 0.67), H - totalH - 32);

        for (int i = 0; i < LABELS.length; i++) {
            int bx = (W - btnW) / 2;
            int by = startY + i * (btnH + gap);
            buttonRects[i] = new Rectangle(bx, by, btnW, btnH);
            drawButton(g2, LABELS[i], bx, by, btnW, btnH, hoveredIndex == i, i);
        }
    }

    private void drawButton(Graphics2D g2, String label, int x, int y, int w, int h,
                             boolean hovered, int index) {
        // Outer glow when hovered
        if (hovered) {
            int glow = 12;
            g2.setColor(new Color(GOLD.getRed(), GOLD.getGreen(), GOLD.getBlue(), 40));
            g2.fillRoundRect(x - glow, y - glow, w + glow*2, h + glow*2, 6, 6);
        }

        // Background panel
        Color bgColor = hovered
            ? new Color(80, 40, 10, 200)
            : new Color(20, 10, 5, 180);
        g2.setColor(bgColor);
        g2.fillRect(x, y, w, h);

        // Border
        Color borderColor = hovered ? GOLD_LIGHT : GOLD_DARK;
        g2.setColor(borderColor);
        g2.setStroke(new BasicStroke(hovered ? 2f : 1f));
        g2.drawRect(x, y, w, h);

        // Corner accents
        int cs = 8;
        g2.setColor(hovered ? GOLD_LIGHT : GOLD);
        g2.setStroke(new BasicStroke(2f));
        // TL
        g2.drawLine(x, y, x + cs, y);
        g2.drawLine(x, y, x, y + cs);
        // TR
        g2.drawLine(x + w, y, x + w - cs, y);
        g2.drawLine(x + w, y, x + w, y + cs);
        // BL
        g2.drawLine(x, y + h, x + cs, y + h);
        g2.drawLine(x, y + h, x, y + h - cs);
        // BR
        g2.drawLine(x + w, y + h, x + w - cs, y + h);
        g2.drawLine(x + w, y + h, x + w, y + h - cs);

        // Left accent mark (► style)
        if (hovered) {
            int mx = x + 16, my = y + h / 2;
            int[] xp = {mx, mx + 8, mx};
            int[] yp = {my - 6, my, my + 6};
            g2.setColor(GOLD_LIGHT);
            g2.fillPolygon(xp, yp, 3);
        }

        // Text
        Font btnFont = new Font("Serif", Font.BOLD, 18);
        g2.setFont(btnFont);
        FontMetrics fm = g2.getFontMetrics();
        int tx = x + (w - fm.stringWidth(label)) / 2;
        int ty = y + (h + fm.getAscent() - fm.getDescent()) / 2;

        // Shadow
        g2.setColor(new Color(0, 0, 0, 200));
        g2.drawString(label, tx + 2, ty + 2);
        // Main
        g2.setColor(hovered ? GOLD_LIGHT : GOLD);
        g2.drawString(label, tx, ty);
    }

}
