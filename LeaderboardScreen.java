import java.awt.*;
import java.awt.event.*;
import java.awt.geom.*;
import java.util.List;
import javax.swing.*;

public class LeaderboardScreen extends JPanel {

    private final GameWindow window;
    private Rectangle backRect;
    private boolean backHovered;

    private static final Color GOLD = new Color(201, 150, 58);
    private static final Color GOLD_LIGHT = new Color(240, 192, 96);
    private static final Color GOLD_DARK = new Color(100, 65, 15);
    private static final Color TEXT = new Color(242, 221, 174);

    public LeaderboardScreen(GameWindow window) {
        this.window = window;
        setBackground(Color.BLACK);
        setupListeners();
    }

    private void setupListeners() {
        addMouseMotionListener(new MouseMotionAdapter() {
            @Override
            public void mouseMoved(MouseEvent e) {
                boolean oldBack = backHovered;
                backHovered = backRect != null && backRect.contains(e.getPoint());
                setCursor(backHovered ? Cursor.getPredefinedCursor(Cursor.HAND_CURSOR) : Cursor.getDefaultCursor());
                if (oldBack != backHovered) repaint();
            }
        });

        addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
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

        int W = getWidth();
        int H = getHeight();

        GradientPaint bg = new GradientPaint(0, 0, new Color(25, 10, 8), 0, H, new Color(8, 3, 2));
        g2.setPaint(bg);
        g2.fillRect(0, 0, W, H);

        RadialGradientPaint vignette = new RadialGradientPaint(
                new Point2D.Float(W / 2f, H / 2f),
                Math.max(W, H) * 0.75f,
                new float[]{0f, 1f},
                new Color[]{new Color(0, 0, 0, 0), new Color(0, 0, 0, 215)}
        );
        g2.setPaint(vignette);
        g2.fillRect(0, 0, W, H);

        g2.setFont(new Font("Serif", Font.BOLD, 42));
        String title = "LEADERBOARD";
        FontMetrics fm = g2.getFontMetrics();
        int titleX = (W - fm.stringWidth(title)) / 2;
        g2.setColor(new Color(0, 0, 0, 180));
        g2.drawString(title, titleX + 3, 82);
        g2.setColor(GOLD_LIGHT);
        g2.drawString(title, titleX, 80);

        int tableX = 90;
        int tableY = 132;
        int tableW = W - tableX * 2;
        int rowH = 42;

        g2.setColor(new Color(18, 9, 5, 220));
        g2.fillRect(tableX, tableY, tableW, 430);
        g2.setColor(GOLD_DARK);
        g2.setStroke(new BasicStroke(1.5f));
        g2.drawRect(tableX, tableY, tableW, 430);

        drawRow(g2, tableX, tableY, tableW, rowH, "PLAYER NAME", "CHARACTER SELECTED", "TIME FINISHED", "LEVEL", "MONSTER KILLED", true);

        List<SaveManager.SaveRecord> records = SaveManager.loadLeaderboard();
        if (records.isEmpty()) {
            g2.setFont(new Font("Serif", Font.ITALIC, 20));
            String empty = "No save records yet.";
            fm = g2.getFontMetrics();
            g2.setColor(TEXT);
            g2.drawString(empty, tableX + (tableW - fm.stringWidth(empty)) / 2, tableY + 230);
        } else {
            int maxRows = Math.min(9, records.size());
            for (int i = 0; i < maxRows; i++) {
                SaveManager.SaveRecord record = records.get(i);
                drawRow(g2, tableX, tableY + rowH * (i + 1), tableW, rowH,
                        record.name,
                        record.character,
                        SaveManager.formatTime(record.timeFinishedMillis),
                        String.valueOf(record.level),
                        String.valueOf(record.monstersKilled),
                        false);
            }
        }

        backRect = new Rectangle(W / 2 - 140, H - 92, 280, 48);
        drawButton(g2, backRect, "BACK", backHovered);

        g2.dispose();
    }

    private void drawRow(Graphics2D g2, int x, int y, int w, int h,
                         String name, String character, String timeFinished, String level, String kills, boolean header) {
        g2.setColor(header ? new Color(80, 40, 10, 220) : new Color(0, 0, 0, 60));
        g2.fillRect(x, y, w, h);
        g2.setColor(GOLD_DARK);
        g2.drawLine(x, y + h, x + w, y + h);

        g2.setFont(new Font("Serif", header ? Font.BOLD : Font.PLAIN, header ? 17 : 18));
        g2.setColor(header ? GOLD_LIGHT : TEXT);

        int nameX = x + 28;
        int characterX = x + (int)(w * 0.27);
        int timeX = x + (int)(w * 0.52);
        int levelX = x + (int)(w * 0.72);
        int killsX = x + (int)(w * 0.84);
        int textY = y + 27;

        g2.drawString(name, nameX, textY);
        g2.drawString(character, characterX, textY);
        g2.drawString(timeFinished, timeX, textY);
        g2.drawString(level, levelX, textY);
        g2.drawString(kills, killsX, textY);
    }

    private void drawButton(Graphics2D g2, Rectangle rect, String label, boolean hovered) {
        g2.setColor(hovered ? new Color(80, 40, 10, 220) : new Color(20, 10, 5, 190));
        g2.fillRect(rect.x, rect.y, rect.width, rect.height);
        g2.setColor(hovered ? GOLD_LIGHT : GOLD_DARK);
        g2.drawRect(rect.x, rect.y, rect.width, rect.height);

        g2.setFont(new Font("Serif", Font.BOLD, 16));
        FontMetrics fm = g2.getFontMetrics();
        int tx = rect.x + (rect.width - fm.stringWidth(label)) / 2;
        int ty = rect.y + (rect.height + fm.getAscent() - fm.getDescent()) / 2;
        g2.setColor(hovered ? GOLD_LIGHT : GOLD);
        g2.drawString(label, tx, ty);
    }
}
