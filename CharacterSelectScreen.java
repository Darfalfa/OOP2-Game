import Characters.AyaLogic;
import Characters.Character;
import Characters.JakaraLogic;
import Characters.RonnixLogic;

import java.awt.*;
import java.awt.event.*;
import java.awt.geom.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.util.Arrays;
import javax.imageio.ImageIO;
import javax.swing.*;

public class CharacterSelectScreen extends JPanel {

    private final GameWindow window;

    private static final Color BG_TOP = new Color(28, 6, 6);
    private static final Color BG_BOTTOM = new Color(7, 3, 2);
    private static final Color GOLD = new Color(201, 150, 58);
    private static final Color GOLD_LIGHT = new Color(240, 192, 96);
    private static final Color GOLD_DARK = new Color(100, 65, 15);
    private static final Color PANEL = new Color(18, 9, 5, 218);
    private static final Color EMBER = new Color(200, 80, 20);
    private static final Color TEXT = new Color(242, 221, 174);

    private static final String[] NAMES = { "Aya", "Ronnix", "Jakara" };
    private static final String[] DISPLAY_NAMES = { "AYA", "RONNIX", "JAKARA" };
    private static final String[] CLASSES = { "Archer", "Fighter", "Arcane Mage" };

    private static final Character[] CHARACTERS = {
            new AyaLogic(),
            new RonnixLogic(),
            new JakaraLogic()
    };

    private static final String[] PORTRAIT_PATHS = {
            "images/aya.png",
            "images/ronnix.png",
            "images/jakara.png"
    };

    private static final String[] PREVIEW_PATHS = {
            "images/aya/AyaCharacterSelection.png",
            "images/ronnix/RonnixCharacterSelection.png",
            "images/jakara/JakaraCharacterSelection.png"
    };

    private static final double[] PORTRAIT_SCALE_MULTIPLIERS = { 1.35, 1.35, 1.35 };
    private static final int[] PORTRAIT_X_OFFSETS = { 0, 0, -12 };
    private static final int[] PORTRAIT_Y_OFFSETS = { 0, 26, -6 };
    private static final int[] PREVIEW_ROTATIONS = { 0, 0, 90 };

    private final BufferedImage[] portraits = new BufferedImage[3];
    private final BufferedImage[] previews = new BufferedImage[3];
    private final BufferedImage[][] idleFrames = new BufferedImage[3][];
    private final Rectangle[] characterRects = new Rectangle[3];
    private Timer idleTimer;
    private int idleFrameIndex = 0;

    private Rectangle playRect;
    private Rectangle backRect;
    private int selectedIndex = 1;
    private int hoveredIndex = -1;
    private boolean playHovered;
    private boolean backHovered;

    public CharacterSelectScreen(GameWindow window) {
        this.window = window;
        setBackground(Color.BLACK);
        loadImages();
        setupListeners();
        idleTimer = new Timer(110, e -> {
            idleFrameIndex++;
            repaint();
        });
        idleTimer.start();
    }

    private void loadImages() {
        for (int i = 0; i < 3; i++) {
            portraits[i] = loadPortrait(PORTRAIT_PATHS[i], 148, 148, i);
            previews[i] = loadPrepared(PREVIEW_PATHS[i], 330, 430, false, PREVIEW_ROTATIONS[i]);
            if (previews[i] == null) {
                previews[i] = loadPrepared(PORTRAIT_PATHS[i], 330, 430, false);
            }
            idleFrames[i] = loadIdleFrames("images/" + NAMES[i].toLowerCase() + "/Idle");
        }
    }

    private BufferedImage[] loadIdleFrames(String folderPath) {
        File folder = new File(folderPath);
        File[] files = folder.listFiles((dir, name) -> name.toLowerCase().endsWith(".png"));
        if (files == null || files.length == 0) {
            return new BufferedImage[0];
        }

        Arrays.sort(files, (a, b) -> a.getName().compareToIgnoreCase(b.getName()));
        BufferedImage[] frames = new BufferedImage[files.length];
        for (int i = 0; i < files.length; i++) {
            try {
                frames[i] = ImageIO.read(files[i]);
            } catch (Exception e) {
                frames[i] = null;
            }
        }
        return frames;
    }

    private void setupListeners() {
        addMouseMotionListener(new MouseMotionAdapter() {
            @Override
            public void mouseMoved(MouseEvent e) {
                int oldHover = hoveredIndex;
                boolean oldPlay = playHovered;
                boolean oldBack = backHovered;

                hoveredIndex = -1;
                for (int i = 0; i < characterRects.length; i++) {
                    if (characterRects[i] != null && characterRects[i].contains(e.getPoint())) {
                        hoveredIndex = i;
                        selectedIndex = i;
                        break;
                    }
                }

                playHovered = playRect != null && playRect.contains(e.getPoint());
                backHovered = backRect != null && backRect.contains(e.getPoint());

                boolean hand = hoveredIndex >= 0 || playHovered || backHovered;
                setCursor(hand ? Cursor.getPredefinedCursor(Cursor.HAND_CURSOR) : Cursor.getDefaultCursor());

                if (oldHover != hoveredIndex || oldPlay != playHovered || oldBack != backHovered) {
                    repaint();
                }
            }
        });

        addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                for (int i = 0; i < characterRects.length; i++) {
                    if (characterRects[i] != null && characterRects[i].contains(e.getPoint())) {
                        selectedIndex = i;
                        repaint();
                        return;
                    }
                }

                if (playRect != null && playRect.contains(e.getPoint())) {
                    window.setSelectedCharacter(NAMES[selectedIndex]);
                    SaveManager.savePlayer(window.getPlayerName(), NAMES[selectedIndex], 1, 0);
                    window.showGame();
                    return;
                }

                if (backRect != null && backRect.contains(e.getPoint())) {
                    window.showMainMenu();
                }
            }
        });
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);

        int W = getWidth();
        int H = getHeight();

        drawBackground(g2, W, H);
        drawHeader(g2, W);

        int leftW = Math.max(390, (int)(W * 0.38));
        int rightX = leftW + 34;
        int rightW = W - rightX - 46;
        int top = 90;
        int bottom = H - 42;

        drawSelectedPreview(g2, 42, top, leftW - 78, bottom - top);
        drawCharacterGrid(g2, rightX, top + 10, rightW, bottom - top - 90);

        int buttonY = H - 82;
        backRect = new Rectangle(28, 24, 138, 42);
        playRect = new Rectangle(W - 210, buttonY, 164, 54);

        Character c = CHARACTERS[selectedIndex];
        Color accent = accentFor(selectedIndex);
        drawStatBars(g2, c, W - 530, buttonY + 4, 300, accent);

        drawBackButton(g2, backRect, backHovered);
        drawPlayButton(g2, playRect, playHovered);

        g2.dispose();
    }

    private void drawBackground(Graphics2D g2, int W, int H) {
        GradientPaint bg = new GradientPaint(0, 0, BG_TOP, 0, H, BG_BOTTOM);
        g2.setPaint(bg);
        g2.fillRect(0, 0, W, H);

        g2.setColor(new Color(EMBER.getRed(), EMBER.getGreen(), EMBER.getBlue(), 28));
        for (int i = 0; i < 120; i++) {
            int x = (i * 97) % Math.max(W, 1);
            int y = (i * 53) % Math.max(H, 1);
            int s = 2 + (i % 4);
            g2.fillOval(x, y, s, s);
        }

        RadialGradientPaint emberGlow = new RadialGradientPaint(
                new Point2D.Float(W * 0.18f, H * 0.78f),
                Math.max(W, H) * 0.42f,
                new float[]{0f, 1f},
                new Color[]{new Color(180, 60, 10, 82), new Color(0, 0, 0, 0)}
        );
        g2.setPaint(emberGlow);
        g2.fillRect(0, 0, W, H);

        RadialGradientPaint vignette = new RadialGradientPaint(
                new Point2D.Float(W / 2f, H / 2f),
                Math.max(W, H) * 0.75f,
                new float[]{0.2f, 1f},
                new Color[]{new Color(0, 0, 0, 0), new Color(0, 0, 0, 205)}
        );
        g2.setPaint(vignette);
        g2.fillRect(0, 0, W, H);

        g2.setColor(new Color(0, 0, 0, 115));
        g2.fillRect(0, 0, W, H);
    }

    private void drawHeader(Graphics2D g2, int W) {
        g2.setFont(new Font("Serif", Font.BOLD, 38));
        String title = "CHOOSE YOUR CHARACTER";
        FontMetrics fm = g2.getFontMetrics();
        int x = (W - fm.stringWidth(title)) / 2;
        g2.setColor(new Color(0, 0, 0, 180));
        g2.drawString(title, x + 3, 62);
        g2.setColor(GOLD_LIGHT);
        g2.drawString(title, x, 60);

        g2.setColor(new Color(GOLD.getRed(), GOLD.getGreen(), GOLD.getBlue(), 130));
        g2.drawLine(x + 28, 74, x + fm.stringWidth(title) - 28, 74);
    }

    private void drawSelectedPreview(Graphics2D g2, int x, int y, int w, int h) {
        Character c = CHARACTERS[selectedIndex];
        Color accent = accentFor(selectedIndex);

        int platformH = 90;
        int platformY = y + h - platformH - 92;
        int platformW = Math.min(w - 34, 430);
        int platformX = x + (w - platformW) / 2;

        drawPlatform(g2, platformX, platformY, platformW, platformH, accent);

        BufferedImage img = currentIdleFrame(selectedIndex);
        if (img == null) {
            img = previews[selectedIndex];
        }
        if (img != null) {
            int imgW = Math.min(w, 360);
            int imgH = Math.min(430, platformY - y + 74);
            int imgX = x + (w - imgW) / 2;
            int imgY = platformY - imgH + 38;
            g2.drawImage(img, imgX, imgY, imgW, imgH, null);
        }

        int panelX = x + 18;
        int panelY = platformY + platformH - 8;
        int panelW = w - 36;
        int panelH = Math.min(170, y + h - panelY - 4);
        drawGlassPanel(g2, panelX, panelY, panelW, panelH);

        g2.setFont(new Font("Serif", Font.BOLD, 34));
        g2.setColor(TEXT);
        g2.drawString(DISPLAY_NAMES[selectedIndex], panelX + 22, panelY + 42);

        g2.setFont(new Font("Serif", Font.ITALIC, 17));
        g2.setColor(accent);
        g2.drawString(CLASSES[selectedIndex], panelX + 24, panelY + 66);

    }

    private void drawCharacterGrid(Graphics2D g2, int x, int y, int w, int h) {
        g2.setFont(new Font("Serif", Font.BOLD, 22));
        g2.setColor(GOLD_LIGHT);

        int cols = 3;
        int gap = 14;
        int tile = Math.min(176, Math.max(118, (w - gap * (cols - 1)) / cols));
        int gridW = cols * tile + (cols - 1) * gap;
        int startX = x + Math.max(0, (w - gridW) / 2);
        int startY = y + 18;

        for (int i = 0; i < 3; i++) {
            int col = i % cols;
            int row = i / cols;
            Rectangle r = new Rectangle(startX + col * (tile + gap), startY + row * (tile + gap), tile, tile);
            characterRects[i] = r;
            drawCharacterTile(g2, i, r, i == selectedIndex, i == hoveredIndex);
        }

        int infoY = startY + tile + 36;
        drawAbilityPanel(g2, x + 8, infoY, w - 16, Math.max(170, h - (infoY - y)), selectedIndex);
    }

    private void drawCharacterTile(Graphics2D g2, int idx, Rectangle r, boolean selected, boolean hovered) {
        Color accent = accentFor(idx);
        int arc = 14;

        if (selected) {
            g2.setColor(new Color(accent.getRed(), accent.getGreen(), accent.getBlue(), 70));
            g2.fillRoundRect(r.x - 8, r.y - 8, r.width + 16, r.height + 16, arc + 8, arc + 8);
        }

        GradientPaint fill = new GradientPaint(
                r.x, r.y, selected ? new Color(82, 36, 12) : new Color(31, 14, 8),
                r.x, r.y + r.height, selected ? new Color(30, 10, 4) : new Color(13, 7, 4)
        );
        g2.setPaint(fill);
        g2.fillRoundRect(r.x, r.y, r.width, r.height, arc, arc);

        g2.setColor(selected ? GOLD_LIGHT : hovered ? GOLD : GOLD_DARK);
        g2.setStroke(new BasicStroke(selected ? 4f : 2f));
        g2.drawRoundRect(r.x, r.y, r.width, r.height, arc, arc);

        BufferedImage portrait = currentIdleFrame(idx);
        if (portrait == null) {
            portrait = portraits[idx];
        }
        if (portrait != null) {
            int pad = 12;
            Shape oldClip = g2.getClip();
            g2.setClip(new RoundRectangle2D.Double(r.x + pad, r.y + pad, r.width - pad * 2, r.height - pad * 2 - 30, 10, 10));
            g2.drawImage(portrait, r.x + pad, r.y + pad, r.width - pad * 2, r.height - pad * 2 - 30, null);
            g2.setClip(oldClip);
        }

        g2.setFont(new Font("Serif", Font.BOLD, 16));
        FontMetrics fm = g2.getFontMetrics();
        int tx = r.x + (r.width - fm.stringWidth(DISPLAY_NAMES[idx])) / 2;
        int ty = r.y + r.height - 12;
        g2.setColor(new Color(0, 0, 0, 180));
        g2.drawString(DISPLAY_NAMES[idx], tx + 2, ty + 2);
        g2.setColor(TEXT);
        g2.drawString(DISPLAY_NAMES[idx], tx, ty);
    }

    private BufferedImage currentIdleFrame(int idx) {
        BufferedImage[] frames = idleFrames[idx];
        if (frames == null || frames.length == 0) {
            return null;
        }
        return frames[idleFrameIndex % frames.length];
    }

    private void drawAbilityPanel(Graphics2D g2, int x, int y, int w, int h, int idx) {
        drawGlassPanel(g2, x, y, w, h);

        Character c = CHARACTERS[idx];
        Color accent = accentFor(idx);

        g2.setFont(new Font("Serif", Font.BOLD, 24));
        g2.setColor(TEXT);
        g2.drawString(DISPLAY_NAMES[idx], x + 24, y + 36);

        g2.setFont(new Font("Serif", Font.ITALIC, 15));
        g2.setColor(accent);
        g2.drawString(CLASSES[idx], x + 24, y + 58);

        int statX = x + 24;
        int statY = y + 88;
        drawValueLine(g2, "HP", String.valueOf(c.getMaxHp()), statX, statY, accent);
        drawValueLine(g2, "DEF", String.valueOf(c.getMaxDefense()), statX, statY + 24, accent);
        drawValueLine(g2, c.getSkillName(1), c.getSkillDamageRange(1), statX, statY + 58, accent);
        drawValueLine(g2, c.getSkillName(2), c.getSkillDamageRange(2), statX, statY + 82, accent);
        drawValueLine(g2, c.getSkillName(3), c.getSkillDamageRange(3), statX, statY + 110, accent);
    }

    private void drawValueLine(Graphics2D g2, String label, String value, int x, int y, Color accent) {
        g2.setFont(new Font("Monospaced", Font.BOLD, 14));
        FontMetrics fm = g2.getFontMetrics();

        int valueX = x + 250; // was 190, gives more space

        g2.setColor(new Color(178, 140, 84));
        g2.drawString(label, x, y);

        g2.setColor(accent);
        g2.drawString(value, valueX, y);
    }

    private void drawStatBars(Graphics2D g2, Character c, int x, int y, int w, Color accent) {
        int hpScore = Math.min(10, Math.max(1, c.getMaxHp() / 10));
        int defScore = Math.min(10, Math.max(1, c.getMaxDefense()));
        drawMiniBar(g2, "HP", hpScore, x, y, w, accent);
        drawMiniBar(g2, "DEF", defScore, x, y + 28, w, accent);
    }

    private void drawMiniBar(Graphics2D g2, String label, int score, int x, int y, int w, Color accent) {
        g2.setFont(new Font("Monospaced", Font.BOLD, 13));
        g2.setColor(new Color(186, 146, 84));
        g2.drawString(label, x, y + 14);

        int blocks = 10;
        int blockW = Math.max(12, (w - 72) / blocks);
        int bx = x + 58;
        for (int i = 0; i < blocks; i++) {
            g2.setColor(i < score ? accent : new Color(52, 30, 14));
            g2.fillRoundRect(bx + i * blockW, y + 3, blockW - 3, 14, 4, 4);
            g2.setColor(new Color(0, 0, 0, 80));
            g2.drawRoundRect(bx + i * blockW, y + 3, blockW - 3, 14, 4, 4);
        }
    }

    private void drawPlatform(Graphics2D g2, int x, int y, int w, int h, Color accent) {
        g2.setColor(new Color(0, 0, 0, 85));
        g2.fillOval(x + 18, y + h - 20, w - 36, 30);

        GradientPaint base = new GradientPaint(x, y, new Color(112, 66, 24), x, y + h, new Color(42, 22, 10));
        g2.setPaint(base);
        g2.fillRoundRect(x, y + 22, w, h - 22, 18, 18);

        g2.setColor(new Color(166, 118, 58));
        g2.fillRoundRect(x + 12, y + 8, w - 24, 34, 18, 18);

        g2.setColor(new Color(accent.getRed(), accent.getGreen(), accent.getBlue(), 165));
        g2.setStroke(new BasicStroke(4f));
        g2.drawOval(x + 46, y + 10, w - 92, 34);
        g2.setStroke(new BasicStroke(1f));
    }

    private void drawGlassPanel(Graphics2D g2, int x, int y, int w, int h) {
        g2.setColor(PANEL);
        g2.fillRoundRect(x, y, w, h, 10, 10);
        g2.setColor(GOLD_DARK);
        g2.setStroke(new BasicStroke(1.5f));
        g2.drawRoundRect(x, y, w, h, 10, 10);
        drawCorners(g2, x, y, w, h, GOLD);
        g2.setColor(new Color(GOLD_LIGHT.getRed(), GOLD_LIGHT.getGreen(), GOLD_LIGHT.getBlue(), 28));
        g2.drawLine(x + 12, y + 9, x + w - 12, y + 9);
    }

    private void drawBackButton(Graphics2D g2, Rectangle r, boolean hovered) {
        drawActionButton(g2, "<  BACK", r, hovered);
    }

    private void drawPlayButton(Graphics2D g2, Rectangle r, boolean hovered) {
        drawActionButton(g2, "SELECT", r, hovered);
    }

    private void drawActionButton(Graphics2D g2, String label, Rectangle r, boolean hovered) {
        if (hovered) {
            g2.setColor(new Color(GOLD.getRed(), GOLD.getGreen(), GOLD.getBlue(), 38));
            g2.fillRoundRect(r.x - 7, r.y - 7, r.width + 14, r.height + 14, 8, 8);
        }

        g2.setColor(hovered ? new Color(80, 40, 10, 220) : new Color(20, 10, 5, 190));
        g2.fillRect(r.x, r.y, r.width, r.height);
        g2.setColor(hovered ? GOLD_LIGHT : GOLD_DARK);
        g2.setStroke(new BasicStroke(hovered ? 2f : 1.5f));
        g2.drawRect(r.x, r.y, r.width, r.height);
        drawCorners(g2, r.x, r.y, r.width, r.height, hovered ? GOLD_LIGHT : GOLD);

        g2.setFont(new Font("Serif", Font.BOLD, 16));
        FontMetrics fm = g2.getFontMetrics();
        int tx = r.x + (r.width - fm.stringWidth(label)) / 2;
        int ty = r.y + (r.height + fm.getAscent() - fm.getDescent()) / 2;
        g2.setColor(new Color(0, 0, 0, 185));
        g2.drawString(label, tx + 2, ty + 2);
        g2.setColor(hovered ? GOLD_LIGHT : GOLD);
        g2.drawString(label, tx, ty);
    }

    private void drawCorners(Graphics2D g2, int x, int y, int w, int h, Color c) {
        int cs = 10;
        g2.setColor(c);
        g2.setStroke(new BasicStroke(1.5f));
        g2.drawLine(x, y, x + cs, y);
        g2.drawLine(x, y, x, y + cs);
        g2.drawLine(x + w, y, x + w - cs, y);
        g2.drawLine(x + w, y, x + w, y + cs);
        g2.drawLine(x, y + h, x + cs, y + h);
        g2.drawLine(x, y + h, x, y + h - cs);
        g2.drawLine(x + w, y + h, x + w - cs, y + h);
        g2.drawLine(x + w, y + h, x + w, y + h - cs);
    }

    private Color accentFor(int idx) {
        return switch (idx) {
            case 0 -> new Color(255, 190, 70);
            case 1 -> new Color(225, 86, 42);
            case 2 -> new Color(221, 157, 72);
            default -> GOLD;
        };
    }

    private BufferedImage loadPrepared(String path, int outW, int outH, boolean fillSquare) {
        return loadPrepared(path, outW, outH, fillSquare, 0);
    }

    private BufferedImage loadPrepared(String path, int outW, int outH, boolean fillSquare, int rotationDegrees) {
        try {
            BufferedImage src = ImageIO.read(new File(path));
            if (src == null) return null;
            if (rotationDegrees == 90) {
                src = rotateClockwise(src);
            }
            return prepareImage(src, outW, outH, fillSquare);
        } catch (Exception e) {
            return null;
        }
    }

    private BufferedImage rotateClockwise(BufferedImage src) {
        BufferedImage rotated = new BufferedImage(src.getHeight(), src.getWidth(), BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2 = rotated.createGraphics();
        g2.translate(src.getHeight(), 0);
        g2.rotate(Math.toRadians(90));
        g2.drawImage(src, 0, 0, null);
        g2.dispose();
        return rotated;
    }

    private BufferedImage loadPortrait(String path, int outW, int outH, int characterIndex) {
        try {
            BufferedImage src = ImageIO.read(new File(path));
            if (src == null) return null;
            return preparePortrait(src, outW, outH, characterIndex);
        } catch (Exception e) {
            return null;
        }
    }

    private BufferedImage preparePortrait(BufferedImage src, int outW, int outH, int characterIndex) {
        BufferedImage argb = new BufferedImage(src.getWidth(), src.getHeight(), BufferedImage.TYPE_INT_ARGB);
        Graphics2D tmp = argb.createGraphics();
        tmp.drawImage(src, 0, 0, null);
        tmp.dispose();

        removeBackground(argb);
        Rectangle bounds = contentBounds(argb);
        if (bounds == null) return null;

        BufferedImage cropped = argb.getSubimage(bounds.x, bounds.y, bounds.width, bounds.height);
        BufferedImage out = new BufferedImage(outW, outH, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2 = out.createGraphics();
        g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);

        double portraitScale = PORTRAIT_SCALE_MULTIPLIERS[Math.max(0, Math.min(characterIndex, PORTRAIT_SCALE_MULTIPLIERS.length - 1))];
        int offsetX = PORTRAIT_X_OFFSETS[Math.max(0, Math.min(characterIndex, PORTRAIT_X_OFFSETS.length - 1))];
        int offsetY = PORTRAIT_Y_OFFSETS[Math.max(0, Math.min(characterIndex, PORTRAIT_Y_OFFSETS.length - 1))];

        double scale = Math.max((double)outW / cropped.getWidth(), (double)outH / cropped.getHeight()) * portraitScale;
        int drawW = Math.max(1, (int)Math.round(cropped.getWidth() * scale));
        int drawH = Math.max(1, (int)Math.round(cropped.getHeight() * scale));
        int drawX = (outW - drawW) / 2 + offsetX;
        int drawY = (int)Math.round(outH * 0.58 - drawH * 0.42) + offsetY;
        g2.drawImage(cropped, drawX, drawY, drawW, drawH, null);
        g2.dispose();
        return out;
    }

    private BufferedImage prepareImage(BufferedImage src, int outW, int outH, boolean fillSquare) {
        BufferedImage argb = new BufferedImage(src.getWidth(), src.getHeight(), BufferedImage.TYPE_INT_ARGB);
        Graphics2D tmp = argb.createGraphics();
        tmp.drawImage(src, 0, 0, null);
        tmp.dispose();

        removeBackground(argb);
        Rectangle bounds = contentBounds(argb);
        if (bounds == null) return null;

        BufferedImage cropped = argb.getSubimage(bounds.x, bounds.y, bounds.width, bounds.height);
        BufferedImage out = new BufferedImage(outW, outH, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2 = out.createGraphics();
        g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);

        double scale = fillSquare
                ? Math.min((double)(outW - 12) / cropped.getWidth(), (double)(outH - 28) / cropped.getHeight())
                : Math.min((double)outW / cropped.getWidth(), (double)outH / cropped.getHeight());
        int drawW = Math.max(1, (int)Math.round(cropped.getWidth() * scale));
        int drawH = Math.max(1, (int)Math.round(cropped.getHeight() * scale));
        int drawX = (outW - drawW) / 2;
        int drawY = fillSquare ? outH - drawH - 10 : outH - drawH;
        g2.drawImage(cropped, drawX, drawY, drawW, drawH, null);
        g2.dispose();
        return out;
    }

    private void removeBackground(BufferedImage img) {
        int w = img.getWidth();
        int h = img.getHeight();
        boolean[][] visited = new boolean[w][h];
        java.util.Queue<Point> queue = new java.util.LinkedList<>();

        for (int x = 0; x < w; x++) {
            enqueueBackground(img, x, 0, visited, queue);
            enqueueBackground(img, x, h - 1, visited, queue);
        }
        for (int y = 1; y < h - 1; y++) {
            enqueueBackground(img, 0, y, visited, queue);
            enqueueBackground(img, w - 1, y, visited, queue);
        }

        while (!queue.isEmpty()) {
            Point p = queue.poll();
            img.setRGB(p.x, p.y, 0x00000000);
            enqueueBackground(img, p.x - 1, p.y, visited, queue);
            enqueueBackground(img, p.x + 1, p.y, visited, queue);
            enqueueBackground(img, p.x, p.y - 1, visited, queue);
            enqueueBackground(img, p.x, p.y + 1, visited, queue);
        }
    }

    private void enqueueBackground(BufferedImage img, int x, int y, boolean[][] visited, java.util.Queue<Point> queue) {
        if (x < 0 || y < 0 || x >= img.getWidth() || y >= img.getHeight()) return;
        if (visited[x][y]) return;
        visited[x][y] = true;

        int argb = img.getRGB(x, y);
        int a = (argb >> 24) & 0xFF;
        int r = (argb >> 16) & 0xFF;
        int g = (argb >> 8) & 0xFF;
        int b = argb & 0xFF;

        boolean transparent = a < 20;
        boolean light = r > 188 && g > 188 && b > 188;
        boolean dark = r < 20 && g < 20 && b < 20;
        boolean lavender = b > 170 && r > 100 && g < 145;

        if (transparent || light || dark || lavender) {
            queue.add(new Point(x, y));
        }
    }

    private Rectangle contentBounds(BufferedImage img) {
        int minX = img.getWidth();
        int minY = img.getHeight();
        int maxX = -1;
        int maxY = -1;

        for (int y = 0; y < img.getHeight(); y++) {
            for (int x = 0; x < img.getWidth(); x++) {
                int alpha = (img.getRGB(x, y) >> 24) & 0xFF;
                if (alpha > 20) {
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
}
