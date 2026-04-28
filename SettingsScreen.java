import java.awt.*;
import java.awt.event.*;
import java.awt.geom.*;
import javax.swing.*;

public class SettingsScreen extends JPanel {

    private GameWindow window;

    private static final Color GOLD       = new Color(201, 150, 58);
    private static final Color GOLD_LIGHT = new Color(240, 192, 96);
    private static final Color GOLD_DARK  = new Color(100, 65, 15);
    private static final Color DARK_BG    = new Color(12, 6, 4);

    // Sliders
    private int musicVol  = 80;
    private int sfxVol    = 70;
    private boolean fullscreen = false;

    private Rectangle musicSlider, sfxSlider;
    private Rectangle musicTrack, sfxTrack;
    private Rectangle fullBtn;
    private Rectangle backRect;
    private Rectangle applyRect;

    private String dragging = null; // "music" or "sfx"
    private boolean backHovered  = false;
    private boolean applyHovered = false;
    

    public SettingsScreen(GameWindow window) {
        this.window = window;
        setBackground(DARK_BG);
        setupListeners();
    }

    private void setupListeners() {
        addMouseMotionListener(new MouseMotionAdapter() {
            @Override
            public void mouseMoved(MouseEvent e) {
                boolean prevB = backHovered, prevA = applyHovered;

                backHovered  = backRect  != null && backRect.contains(e.getPoint());
                applyHovered = applyRect != null && applyRect.contains(e.getPoint());

                if (prevB != backHovered || prevA != applyHovered) repaint();

                boolean anyHover = backHovered || applyHovered ||
                        (musicSlider != null && musicSlider.contains(e.getPoint())) ||
                        (sfxSlider != null && sfxSlider.contains(e.getPoint())) ||
                        (fullBtn != null && fullBtn.contains(e.getPoint()));

                setCursor(anyHover ? Cursor.getPredefinedCursor(Cursor.HAND_CURSOR) : Cursor.getDefaultCursor());
            }

            @Override
            public void mouseDragged(MouseEvent e) {
                if ("music".equals(dragging) && musicTrack != null) {
                    int val = (int)(100.0 * (e.getX() - musicTrack.x) / musicTrack.width);
                    musicVol = Math.max(0, Math.min(100, val));
                    SoundManager.setMusicVolume(musicVol / 100f);
                    repaint();

                } else if ("sfx".equals(dragging) && sfxTrack != null) {
                    int val = (int)(100.0 * (e.getX() - sfxTrack.x) / sfxTrack.width);
                    sfxVol = Math.max(0, Math.min(100, val));
                    SoundManager.setSfxVolume(sfxVol / 100f);
                    repaint();
                }
            }
        });

        addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                if (musicSlider != null && musicSlider.contains(e.getPoint())) dragging = "music";
                else if (sfxSlider != null && sfxSlider.contains(e.getPoint())) dragging = "sfx";
            }

            @Override
            public void mouseReleased(MouseEvent e) { dragging = null; }

            @Override
            public void mouseClicked(MouseEvent e) {
                if (fullBtn != null && fullBtn.contains(e.getPoint())) {
                    fullscreen = !fullscreen;
                    window.setFullscreen(fullscreen);
                    repaint();
                    return;
                }

                if (backRect != null && backRect.contains(e.getPoint())) {
                    window.showMainMenu();
                    return;
                }

                if (applyRect != null && applyRect.contains(e.getPoint())) {
                    SoundManager.setMusicVolume(musicVol / 100f);
                    SoundManager.setSfxVolume(sfxVol / 100f);

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

        // Background
        GradientPaint bg = new GradientPaint(0, 0, new Color(25, 10, 8), 0, H, new Color(8, 3, 2));
        g2.setPaint(bg);
        g2.fillRect(0, 0, W, H);
        g2.setColor(new Color(40, 20, 10, 30));
        g2.setStroke(new BasicStroke(1f));
        for (int x = 0; x < W; x += 40) g2.drawLine(x, 0, x, H);
        for (int y = 0; y < H; y += 40) g2.drawLine(0, y, W, y);

        // Vignette
        RadialGradientPaint vig = new RadialGradientPaint(
            new Point2D.Float(W/2f, H/2f), Math.max(W,H)*0.7f,
            new float[]{0.4f,1f}, new Color[]{new Color(0,0,0,0), new Color(0,0,0,210)});
        g2.setPaint(vig); g2.fillRect(0,0,W,H);

        // Panel
        int panelW = 620, panelH = 440;
        int panelX = (W - panelW)/2, panelY = (H - panelH)/2 - 20;
        g2.setColor(new Color(18, 9, 5, 210));
        g2.fillRoundRect(panelX, panelY, panelW, panelH, 12, 12);
        g2.setColor(GOLD_DARK);
        g2.setStroke(new BasicStroke(1.5f));
        g2.drawRoundRect(panelX, panelY, panelW, panelH, 12, 12);
        drawCorners(g2, panelX, panelY, panelW, panelH, GOLD);

        // Header
        Font hf = new Font("Serif", Font.BOLD, 34);
        g2.setFont(hf);
        String hdr = "SETTINGS";
        FontMetrics fm = g2.getFontMetrics();
        int hx = panelX + (panelW - fm.stringWidth(hdr))/2;
        g2.setColor(new Color(0,0,0,160)); g2.drawString(hdr, hx+2, panelY+44);
        GradientPaint gp = new GradientPaint(hx, panelY+10, GOLD_LIGHT, hx, panelY+46, GOLD_DARK);
        g2.setPaint(gp); g2.drawString(hdr, hx, panelY+42);

        // Divider
        g2.setColor(GOLD_DARK); g2.setStroke(new BasicStroke(1f));
        g2.drawLine(panelX+30, panelY+55, panelX+panelW-30, panelY+55);

        int rowX = panelX + 40, rowW = panelW - 80;
        int ry = panelY + 80;

        // Music volume
        ry = drawSliderRow(g2, "MUSIC VOLUME", musicVol, rowX, ry, rowW, true);
        musicTrack = new Rectangle(rowX + 160, ry - 32, rowW - 160, 16);
        musicSlider = new Rectangle((int)(musicTrack.x + musicTrack.width * musicVol / 100.0) - 8, ry - 40, 16, 32);

        // SFX volume
        ry += 10;
        ry = drawSliderRow(g2, "SFX VOLUME", sfxVol, rowX, ry, rowW, false);
        sfxTrack = new Rectangle(rowX + 160, ry - 32, rowW - 160, 16);
        sfxSlider = new Rectangle((int)(sfxTrack.x + sfxTrack.width * sfxVol / 100.0) - 8, ry - 40, 16, 32);

        // Fullscreen toggle
        drawLabel(g2, "FULLSCREEN", rowX, ry);
        int tbX = rowX + 160, tbY = ry - 24, tbW = 60, tbH = 28;
        fullBtn = new Rectangle(tbX, tbY, tbW, tbH);
        g2.setColor(fullscreen ? new Color(80,45,10,220) : new Color(20,10,5,180));
        g2.fillRoundRect(tbX, tbY, tbW, tbH, tbH, tbH);
        g2.setColor(fullscreen ? GOLD_LIGHT : GOLD_DARK);
        g2.setStroke(new BasicStroke(1.5f));
        g2.drawRoundRect(tbX, tbY, tbW, tbH, tbH, tbH);
        int knobX = fullscreen ? tbX + tbW - tbH + 3 : tbX + 3;
        g2.setColor(fullscreen ? GOLD_LIGHT : new Color(80,50,20));
        g2.fillOval(knobX, tbY + 3, tbH - 6, tbH - 6);
        Font tl = new Font("Serif", Font.BOLD, 12);
        g2.setFont(tl); g2.setColor(GOLD);
        g2.drawString(fullscreen ? "ON" : "OFF", tbX + tbW + 12, ry - 5);

        // Buttons
        int btnY = panelY + panelH - 60;
        int bkW = 140, bkH = 40;
        int bkX = panelX + 40;
        backRect = new Rectangle(bkX, btnY, bkW, bkH);
        drawActionButton(g2, "◄  BACK", bkX, btnY, bkW, bkH, backHovered);

        int apW = 160, apH = 40;
        int apX = panelX + panelW - 40 - apW;
        applyRect = new Rectangle(apX, btnY, apW, apH);
        drawActionButton(g2, "APPLY  ✓", apX, btnY, apW, apH, applyHovered);

        g2.dispose();
    }

    private int drawSliderRow(Graphics2D g2, String label, int value, int x, int y, int w, boolean isMusic) {
        drawLabel(g2, label, x, y);

        int trackX = x + 160, trackY = y - 14, trackW = w - 160, trackH = 16;
        int fillW = (int)(trackW * value / 100.0);
        int knobX = trackX + fillW - 8, knobY = trackY - 8, knobW = 16, knobH = 32;

        // Track bg
        g2.setColor(new Color(20, 10, 5));
        g2.fillRoundRect(trackX, trackY, trackW, trackH, trackH, trackH);

        // Fill
        GradientPaint fp = new GradientPaint(trackX, trackY, GOLD, trackX + fillW, trackY, GOLD_DARK);
        g2.setPaint(fp);
        g2.fillRoundRect(trackX, trackY, fillW, trackH, trackH, trackH);

        // Track border
        g2.setColor(GOLD_DARK);
        g2.setStroke(new BasicStroke(1f));
        g2.drawRoundRect(trackX, trackY, trackW, trackH, trackH, trackH);

        // Knob
        g2.setColor(new Color(50, 30, 8));
        g2.fillRoundRect(knobX, knobY, knobW, knobH, 4, 4);
        g2.setColor(GOLD_LIGHT);
        g2.setStroke(new BasicStroke(1.5f));
        g2.drawRoundRect(knobX, knobY, knobW, knobH, 4, 4);

        // Value text
        Font vf = new Font("Monospaced", Font.BOLD, 11);
        g2.setFont(vf);
        g2.setColor(GOLD);
        g2.drawString(value + "%", trackX + trackW + 10, y - 3);

        return y + 40;
    }

    private void drawLabel(Graphics2D g2, String text, int x, int y) {
        Font lf = new Font("Serif", Font.BOLD, 14);
        g2.setFont(lf);
        g2.setColor(new Color(180, 145, 90));
        g2.drawString(text, x, y - 4);
    }

    private void drawActionButton(Graphics2D g2, String label, int x, int y, int w, int h, boolean hovered) {
        if (hovered) {
            g2.setColor(new Color(GOLD.getRed(), GOLD.getGreen(), GOLD.getBlue(), 35));
            g2.fillRoundRect(x-6, y-6, w+12, h+12, 6, 6);
        }
        g2.setColor(hovered ? new Color(80,45,10,220) : new Color(18,9,5,200));
        g2.fillRect(x, y, w, h);
        g2.setColor(hovered ? GOLD_LIGHT : GOLD_DARK);
        g2.setStroke(new BasicStroke(hovered ? 2f : 1.5f));
        g2.drawRect(x, y, w, h);
        drawCorners(g2, x, y, w, h, hovered ? GOLD_LIGHT : GOLD_DARK);

        Font bf = new Font("Serif", Font.BOLD, 14);
        g2.setFont(bf);
        FontMetrics fm = g2.getFontMetrics();
        int tx = x + (w - fm.stringWidth(label))/2;
        int ty = y + (h + fm.getAscent() - fm.getDescent())/2;
        g2.setColor(new Color(0,0,0,180)); g2.drawString(label, tx+2, ty+2);
        g2.setColor(hovered ? GOLD_LIGHT : GOLD); g2.drawString(label, tx, ty);
    }

    private void drawCorners(Graphics2D g2, int x, int y, int w, int h, Color c) {
        int cs = 10;
        g2.setColor(c); g2.setStroke(new BasicStroke(1.5f));
        g2.drawLine(x,y,x+cs,y); g2.drawLine(x,y,x,y+cs);
        g2.drawLine(x+w,y,x+w-cs,y); g2.drawLine(x+w,y,x+w,y+cs);
        g2.drawLine(x,y+h,x+cs,y+h); g2.drawLine(x,y+h,x,y+h-cs);
        g2.drawLine(x+w,y+h,x+w-cs,y+h); g2.drawLine(x+w,y+h,x+w,y+h-cs);
    }
}
