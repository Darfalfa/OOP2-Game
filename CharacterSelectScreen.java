import java.awt.*;
import java.awt.event.*;
import java.awt.geom.*;
import java.awt.image.BufferedImage;
import java.io.File;
import javax.imageio.ImageIO;
import javax.swing.*;

public class CharacterSelectScreen extends JPanel {

    private GameWindow window;
    private BufferedImage archerSprite;
    private BufferedImage ronnixSprite;
    private BufferedImage jakaraSprite;

    private int cardWidth  = 200;
    private int cardHeight = 420;

    private Rectangle archerRect;
    private Rectangle ronnixRect;
    private Rectangle jakaraRect;
    private boolean   archerHovered = false;
    private boolean   ronnixHovered = false;
    private boolean   jakaraHovered = false;

    private static final Color GOLD        = new Color(201, 150,  58);
    private static final Color GOLD_LIGHT  = new Color(240, 192,  96);
    private static final Color GOLD_DARK   = new Color(100,  65,  15);
    private static final Color STEEL_LIGHT = new Color(180, 200, 220);
    private static final Color STEEL_DARK  = new Color( 60,  80, 110);
    // Jakara — arcane violet theme
    private static final Color MAGE_LIGHT  = new Color(190, 130, 255);
    private static final Color MAGE_DARK   = new Color( 60,  20, 110);

    private Rectangle backRect;
    private boolean   backHovered = false;

    // Character stat bars (HP, MP, ATK, DEF) out of 10
    private static final String[] CHAR_NAMES = { "ARCHER", "RONNIX", "JAKARA" };
    private static final String[] CHAR_CLASS = { "Ranger", "Fighter", "Arcane Mage" };
    private static final int[][] CHAR_STATS  = {
        { 7, 8, 6, 5 },   // Archer
        { 9, 4, 9, 8 },   // Ronnix
        { 5, 10, 9, 3 }   // Jakara
    };

    public CharacterSelectScreen(GameWindow window) {
        this.window = window;
        setBackground(Color.BLACK);

        // Aya — the Archer character
        try { archerSprite = preparePortrait(ImageIO.read(new File("images/aya.png"))); }
        catch (Exception e) { archerSprite = null; }

        // Ronnix — the Fighter character
        try { ronnixSprite = preparePortrait(ImageIO.read(new File("images/ronnix.png"))); }
        catch (Exception e) { ronnixSprite = null; }

        // Jakara — the Arcane Mage character
        try { jakaraSprite = preparePortrait(ImageIO.read(new File("images/jakara.png"))); }
        catch (Exception e) { jakaraSprite = null; }

        setupListeners();
    }

    private void setupListeners() {
        addMouseMotionListener(new MouseMotionAdapter() {
            @Override
            public void mouseMoved(MouseEvent e) {
                boolean pA = archerHovered, pR = ronnixHovered, pJ = jakaraHovered, pB = backHovered;
                archerHovered = archerRect != null && archerRect.contains(e.getPoint());
                ronnixHovered = ronnixRect != null && ronnixRect.contains(e.getPoint());
                jakaraHovered = jakaraRect != null && jakaraRect.contains(e.getPoint());
                backHovered   = backRect   != null && backRect.contains(e.getPoint());
                if (pA != archerHovered || pR != ronnixHovered || pJ != jakaraHovered || pB != backHovered) repaint();
                boolean any = archerHovered || ronnixHovered || jakaraHovered || backHovered;
                setCursor(any ? Cursor.getPredefinedCursor(Cursor.HAND_CURSOR)
                              : Cursor.getDefaultCursor());
            }
        });

        addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (archerRect != null && archerRect.contains(e.getPoint())) {
                    window.setSelectedCharacter("Archer");
                    window.showGame();
                    return;
                }
                if (ronnixRect != null && ronnixRect.contains(e.getPoint())) {
                    window.setSelectedCharacter("Ronnix");
                    window.showGame();
                    return;
                }
                if (jakaraRect != null && jakaraRect.contains(e.getPoint())) {
                    window.setSelectedCharacter("Jakara");
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
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,      RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

        int W = getWidth(), H = getHeight();

        // Background gradient
        GradientPaint bg = new GradientPaint(0, 0, new Color(18, 8, 5), 0, H, new Color(6, 3, 10));
        g2.setPaint(bg);
        g2.fillRect(0, 0, W, H);

        // Subtle grid
        g2.setColor(new Color(40, 20, 10, 20));
        g2.setStroke(new BasicStroke(1f));
        for (int x = 0; x < W; x += 50) g2.drawLine(x, 0, x, H);
        for (int y = 0; y < H; y += 50) g2.drawLine(0, y, W, y);

        // Vignette
        RadialGradientPaint vig = new RadialGradientPaint(
            new Point2D.Float(W / 2f, H / 2f), Math.max(W, H) * 0.75f,
            new float[]{0.3f, 1f},
            new Color[]{new Color(0,0,0,0), new Color(0,0,0,220)});
        g2.setPaint(vig);
        g2.fillRect(0, 0, W, H);

        // Title
        g2.setFont(new Font("Serif", Font.BOLD, 36));
        g2.setColor(GOLD_LIGHT);
        drawCenteredText(g2, "CHOOSE YOUR CHARACTER", W, 70);
        g2.setColor(GOLD_DARK);
        g2.setStroke(new BasicStroke(1f));
        g2.drawLine((W - 460) / 2, 82, (W + 460) / 2, 82);

        // Cards — three characters side by side, tighter gap to fit at 1280px
        int gap    = 40;
        int totalW = 3 * cardWidth + 2 * gap;
        int startX = (W - totalW) / 2;
        int cardY  = (H - cardHeight) / 2 - 10;

        archerRect = new Rectangle(startX,                       cardY, cardWidth, cardHeight);
        ronnixRect = new Rectangle(startX + cardWidth + gap,     cardY, cardWidth, cardHeight);
        jakaraRect = new Rectangle(startX + 2 * (cardWidth + gap), cardY, cardWidth, cardHeight);

        drawCharacterCard(g2, 0, archerRect, archerSprite, archerHovered, GOLD,       false, false);
        drawCharacterCard(g2, 1, ronnixRect, ronnixSprite, ronnixHovered, STEEL_LIGHT, true, false);
        drawCharacterCard(g2, 2, jakaraRect, jakaraSprite, jakaraHovered, MAGE_LIGHT, false, true);

        // Back button
        int bW = 140, bH = 40;
        int bX = (W - bW) / 2, bY = H - 70;
        backRect = new Rectangle(bX, bY, bW, bH);
        drawSmallButton(g2, "◄  BACK", bX, bY, bW, bH, backHovered);

        g2.dispose();
    }

    private void drawCharacterCard(Graphics2D g2, int idx, Rectangle r,
                                    BufferedImage sprite, boolean hovered,
                                    Color accent, boolean isRonnix, boolean isJakara) {
        int x = r.x, y = r.y, w = r.width, h = r.height;

        if (hovered) {
            g2.setColor(new Color(accent.getRed(), accent.getGreen(), accent.getBlue(), 35));
            g2.fillRoundRect(x - 12, y - 12, w + 24, h + 24, 24, 24);
        }

        // Background tint differs per class
        Color cardBg = isRonnix  ? new Color(15, 10, 25, 220)
                     : isJakara  ? new Color(10,  5, 22, 230)
                                 : new Color(30, 15,  5, 220);
        g2.setColor(cardBg);
        g2.fillRoundRect(x, y, w, h, 16, 16);

        // Accent top strip for Jakara
        if (isJakara) {
            GradientPaint strip = new GradientPaint(x, y, MAGE_DARK, x + w, y, MAGE_LIGHT);
            g2.setPaint(strip);
            g2.fillRoundRect(x, y, w, 10, 16, 16);
            g2.fillRect(x, y + 6, w, 4);
        }

        g2.setColor(hovered ? accent.brighter() : accent.darker());
        g2.setStroke(new BasicStroke(hovered ? 2.5f : 1.5f));
        g2.drawRoundRect(x, y, w, h, 16, 16);
        drawCorners(g2, x, y, w, h, hovered ? accent.brighter() : accent);

        // Sprite or silhouette
        // Portrait area: OUTPUT_W wide × OUTPUT_H tall, centred in card, top-padded by 10px
        int portraitAreaW = OUTPUT_W;
        int portraitAreaH = OUTPUT_H;
        int spriteX = x + (w - portraitAreaW) / 2;
        int spriteY = y + 10;
        if (sprite != null) {
            // Subtle glow behind portrait
            if (isJakara) {
                g2.setColor(new Color(160, 80, 255, 35));
                g2.fillOval(spriteX - 8, spriteY + portraitAreaH / 2, portraitAreaW + 16, portraitAreaH / 2 + 8);
            } else if (isRonnix) {
                g2.setColor(new Color(60, 80, 180, 25));
                g2.fillOval(spriteX - 8, spriteY + portraitAreaH / 2, portraitAreaW + 16, portraitAreaH / 2 + 8);
            } else {
                g2.setColor(new Color(180, 130, 40, 20));
                g2.fillOval(spriteX - 8, spriteY + portraitAreaH / 2, portraitAreaW + 16, portraitAreaH / 2 + 8);
            }
            // Draw portrait directly — background already stripped, no clip needed
            g2.drawImage(sprite, spriteX, spriteY, portraitAreaW, portraitAreaH, null);
        } else {
            drawSilhouette(g2, spriteX, spriteY, Math.min(portraitAreaW, portraitAreaH), isRonnix, isJakara, accent);
        }

        // Name
        g2.setFont(new Font("Serif", Font.BOLD, 20));
        g2.setColor(Color.WHITE);
        drawCenteredInRect(g2, CHAR_NAMES[idx], x, w, y + portraitAreaH + 26);

        // Class label
        g2.setFont(new Font("Serif", Font.ITALIC, 13));
        g2.setColor(accent);
        drawCenteredInRect(g2, CHAR_CLASS[idx], x, w, y + portraitAreaH + 44);

        // Stat bars
        String[] statLabels = { "HP", "MP", "ATK", "DEF" };
        int[] stats = CHAR_STATS[idx];
        int barAreaY = y + portraitAreaH + 58;
        int barW = w - 30, barH = 9, barX = x + 15;
        for (int i = 0; i < 4; i++) {
            int by = barAreaY + i * 20;
            g2.setFont(new Font("Monospaced", Font.BOLD, 10));
            g2.setColor(new Color(180, 160, 120));
            g2.drawString(statLabels[i], barX, by + barH - 1);
            int tX = barX + 32;
            int tW = barW - 32;
            g2.setColor(new Color(15, 8, 20));
            g2.fillRoundRect(tX, by, tW, barH, barH, barH);
            int fillW = (int)(tW * stats[i] / 10.0);
            Color fillCol = isRonnix  ? lerp(STEEL_DARK, STEEL_LIGHT, stats[i] / 10f)
                          : isJakara  ? lerp(MAGE_DARK,  MAGE_LIGHT,  stats[i] / 10f)
                                      : lerp(GOLD_DARK,  GOLD_LIGHT,  stats[i] / 10f);
            g2.setColor(fillCol);
            if (fillW > 0) g2.fillRoundRect(tX, by, fillW, barH, barH, barH);
            g2.setColor(accent.darker()); g2.setStroke(new BasicStroke(0.8f));
            g2.drawRoundRect(tX, by, tW, barH, barH, barH);
        }

        // Prompt
        g2.setFont(new Font("Serif", Font.ITALIC, 12));
        g2.setColor(hovered ? accent : new Color(120, 100, 60));
        drawCenteredInRect(g2, "Click to select", x, w, y + h - 10);
    }

    /**
     * Draws a stylised placeholder silhouette when no sprite image is found.
     * Ronnix gets a sword; Archer gets a bow; Jakara gets a staff with an orb.
     */
    private void drawSilhouette(Graphics2D g2, int x, int y, int size,
                                 boolean isRonnix, boolean isJakara, Color accent) {
        Color body = isRonnix  ? new Color(40, 30, 60)
                   : isJakara  ? new Color(25, 12, 45)
                               : new Color(50, 35, 15);
        g2.setColor(body);
        // Torso
        g2.fillRoundRect(x + size/4, y + size/4, size/2, (int)(size*0.55), 10, 10);
        // Head
        g2.fillOval(x + size/3, y + 4, size/3, size/3);
        // Legs
        g2.fillRect(x + size/4,          y + (int)(size*0.75), size/5, size/4);
        g2.fillRect(x + (int)(size*0.55), y + (int)(size*0.75), size/5, size/4);

        if (isRonnix) {
            // Sword blade
            g2.setColor(STEEL_LIGHT);
            g2.setStroke(new BasicStroke(3f));
            g2.drawLine(x + (int)(size*0.72), y + size/5,
                        x + (int)(size*0.52), y + (int)(size*0.72));
            // Crossguard
            g2.setColor(GOLD);
            g2.fillRect(x + (int)(size*0.56), y + (int)(size*0.36), 16, 4);
        } else if (isJakara) {
            // Staff shaft
            g2.setColor(new Color(120, 70, 200));
            g2.setStroke(new BasicStroke(3f));
            g2.drawLine(x + (int)(size*0.75), y + (int)(size*0.85),
                        x + (int)(size*0.72), y + size/8);
            // Orb at tip
            g2.setColor(new Color(200, 140, 255, 200));
            g2.fillOval(x + (int)(size*0.64), y + 2, 18, 18);
            g2.setColor(MAGE_LIGHT);
            g2.setStroke(new BasicStroke(1.5f));
            g2.drawOval(x + (int)(size*0.64), y + 2, 18, 18);
            // Arcane sparkles
            g2.setColor(new Color(220, 180, 255, 160));
            g2.fillOval(x + (int)(size*0.20), y + (int)(size*0.30), 6, 6);
            g2.fillOval(x + (int)(size*0.15), y + (int)(size*0.55), 4, 4);
        } else {
            // Bow
            g2.setColor(GOLD_DARK);
            g2.setStroke(new BasicStroke(2f));
            g2.drawArc(x + (int)(size*0.68), y + size/5,
                       (int)(size*0.22), (int)(size*0.55), -90, 180);
            g2.setColor(new Color(180, 140, 90));
            g2.setStroke(new BasicStroke(1f));
            g2.drawLine(x + (int)(size*0.79), y + size/5,
                        x + (int)(size*0.79), y + (int)(size*0.76));
        }

        g2.setColor(accent);
        g2.setStroke(new BasicStroke(1.5f));
        g2.drawRoundRect(x, y, size, size, 10, 10);

        g2.setFont(new Font("Serif", Font.BOLD | Font.ITALIC, 13));
        g2.setColor(accent);
        String tag = isRonnix ? "RONNIX" : isJakara ? "JAKARA" : "ARCHER";
        FontMetrics fm = g2.getFontMetrics();
        g2.drawString(tag, x + (size - fm.stringWidth(tag)) / 2, y + size - 8);
    }

    private void drawSmallButton(Graphics2D g2, String label, int x, int y,
                                  int w, int h, boolean hovered) {
        if (hovered) {
            g2.setColor(new Color(GOLD.getRed(), GOLD.getGreen(), GOLD.getBlue(), 35));
            g2.fillRoundRect(x - 6, y - 6, w + 12, h + 12, 6, 6);
        }
        g2.setColor(hovered ? new Color(80, 45, 10, 220) : new Color(18, 9, 5, 200));
        g2.fillRect(x, y, w, h);
        g2.setColor(hovered ? GOLD_LIGHT : GOLD_DARK);
        g2.setStroke(new BasicStroke(hovered ? 2f : 1.5f));
        g2.drawRect(x, y, w, h);
        g2.setFont(new Font("Serif", Font.BOLD, 14));
        FontMetrics fm = g2.getFontMetrics();
        int tx = x + (w - fm.stringWidth(label)) / 2;
        int ty = y + (h + fm.getAscent() - fm.getDescent()) / 2;
        g2.setColor(new Color(0, 0, 0, 180)); g2.drawString(label, tx + 2, ty + 2);
        g2.setColor(hovered ? GOLD_LIGHT : GOLD); g2.drawString(label, tx, ty);
    }

    private void drawCorners(Graphics2D g2, int x, int y, int w, int h, Color c) {
        int cs = 10;
        g2.setColor(c); g2.setStroke(new BasicStroke(2f));
        g2.drawLine(x, y, x+cs, y);         g2.drawLine(x, y, x, y+cs);
        g2.drawLine(x+w, y, x+w-cs, y);     g2.drawLine(x+w, y, x+w, y+cs);
        g2.drawLine(x, y+h, x+cs, y+h);     g2.drawLine(x, y+h, x, y+h-cs);
        g2.drawLine(x+w, y+h, x+w-cs, y+h); g2.drawLine(x+w, y+h, x+w, y+h-cs);
    }

    private void drawCenteredText(Graphics2D g2, String text, int W, int y) {
        FontMetrics fm = g2.getFontMetrics();
        g2.drawString(text, (W - fm.stringWidth(text)) / 2, y);
    }

    private void drawCenteredInRect(Graphics2D g2, String text, int rx, int rw, int y) {
        FontMetrics fm = g2.getFontMetrics();
        g2.drawString(text, rx + (rw - fm.stringWidth(text)) / 2, y);
    }

    // ── Portrait processing ───────────────────────────────────────────────────

    /**
     * Removes the white/light background via flood-fill, crops to the tight
     * content bounding box, then places the character anchored to the BOTTOM
     * of a fixed-size canvas. This ensures all three characters are drawn at
     * the same visual height and stand on the same baseline inside their card.
     *
     * Canvas size: OUTPUT_W × OUTPUT_H (wider than tall so full body fits).
     */
    private static final int OUTPUT_W = 160;
    private static final int OUTPUT_H = 200;

    private BufferedImage preparePortrait(BufferedImage src) {
        if (src == null) return null;

        // 1. Convert to ARGB
        BufferedImage argb = new BufferedImage(
                src.getWidth(), src.getHeight(), BufferedImage.TYPE_INT_ARGB);
        Graphics2D tmp = argb.createGraphics();
        tmp.drawImage(src, 0, 0, null);
        tmp.dispose();

        // 2. Remove white / near-white / fringe background
        removeWhiteBackground(argb);

        // 3. Tight-crop to visible content
        int[] bbox = contentBBox(argb);
        if (bbox == null) return null;
        int cx = bbox[0], cy = bbox[1], cw = bbox[2], ch = bbox[3];
        BufferedImage cropped = argb.getSubimage(cx, cy, cw, ch);

        // 4. Scale so the character fills OUTPUT_H in height (preserving aspect ratio)
        //    then centre horizontally in OUTPUT_W.
        double scale  = (double) OUTPUT_H / ch;
        int scaledW   = (int)(cw * scale);
        int scaledH   = OUTPUT_H;
        // If too wide, fit by width instead
        if (scaledW > OUTPUT_W) {
            scale   = (double) OUTPUT_W / cw;
            scaledW = OUTPUT_W;
            scaledH = (int)(ch * scale);
        }

        // 5. Place on canvas, anchored to the BOTTOM-CENTRE so all characters
        //    stand on the same line regardless of their different proportions.
        BufferedImage result = new BufferedImage(OUTPUT_W, OUTPUT_H, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2 = result.createGraphics();
        g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION,
                            RenderingHints.VALUE_INTERPOLATION_BILINEAR);
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                            RenderingHints.VALUE_ANTIALIAS_ON);
        int drawX = (OUTPUT_W - scaledW) / 2;
        int drawY = OUTPUT_H - scaledH;          // bottom-anchored
        g2.drawImage(cropped, drawX, drawY, scaledW, scaledH, null);
        g2.dispose();
        return result;
    }

    /** Removes white/near-white background in-place via edge flood-fill. */
    private void removeWhiteBackground(BufferedImage img) {
        int w = img.getWidth(), h = img.getHeight();
        boolean[][] visited = new boolean[w][h];
        java.util.Queue<int[]> queue = new java.util.LinkedList<>();

        for (int x = 0; x < w; x++) {
            enqueuePortrait(img, x, 0,     visited, queue);
            enqueuePortrait(img, x, h - 1, visited, queue);
        }
        for (int y = 1; y < h - 1; y++) {
            enqueuePortrait(img, 0,     y, visited, queue);
            enqueuePortrait(img, w - 1, y, visited, queue);
        }

        while (!queue.isEmpty()) {
            int[] px = queue.poll();
            int cx = px[0], cy = px[1];
            img.setRGB(cx, cy, 0x00000000);
            int[][] nb = { {cx-1,cy},{cx+1,cy},{cx,cy-1},{cx,cy+1} };
            for (int[] n : nb) enqueuePortrait(img, n[0], n[1], visited, queue);
        }
    }

    private void enqueuePortrait(BufferedImage img, int x, int y,
                                  boolean[][] visited, java.util.Queue<int[]> queue) {
        int w = img.getWidth(), h = img.getHeight();
        if (x < 0 || y < 0 || x >= w || y >= h || visited[x][y]) return;
        visited[x][y] = true;
        int argb = img.getRGB(x, y);
        int a = (argb >> 24) & 0xFF;
        int r = (argb >> 16) & 0xFF;
        int g = (argb >>  8) & 0xFF;
        int b =  argb        & 0xFF;
        // Transparent, pure white, near-white, and light grey all count as background
        boolean isBackground = (a < 20) || (r >= 190 && g >= 190 && b >= 190);
        if (isBackground) queue.add(new int[]{x, y});
    }

    /**
     * Returns [x, y, width, height] of the tight bounding box of non-transparent
     * pixels, or null if the image is completely transparent.
     */
    private int[] contentBBox(BufferedImage img) {
        int w = img.getWidth(), h = img.getHeight();
        int minX = w, minY = h, maxX = -1, maxY = -1;
        for (int y = 0; y < h; y++) {
            for (int x = 0; x < w; x++) {
                if (((img.getRGB(x, y) >> 24) & 0xFF) > 20) {
                    if (x < minX) minX = x;
                    if (x > maxX) maxX = x;
                    if (y < minY) minY = y;
                    if (y > maxY) maxY = y;
                }
            }
        }
        if (maxX < minX || maxY < minY) return null;
        return new int[]{minX, minY, maxX - minX + 1, maxY - minY + 1};
    }

    private Color lerp(Color a, Color b, float t) {
        int r  = Math.min(255, (int)(a.getRed()   + (b.getRed()   - a.getRed())   * t));
        int gr = Math.min(255, (int)(a.getGreen() + (b.getGreen() - a.getGreen()) * t));
        int bl = Math.min(255, (int)(a.getBlue()  + (b.getBlue()  - a.getBlue())  * t));
        return new Color(r, gr, bl);
    }
}