import java.awt.*;
import java.awt.event.*;
import java.awt.geom.*;
import javax.swing.*;

public class NameEntryScreen extends JPanel {

    private final GameWindow window;
    private final JTextField nameField;
    private Rectangle continueRect;
    private Rectangle backRect;
    private boolean continueHovered;
    private boolean backHovered;
    private String errorMessage = "";

    private static final Color GOLD = new Color(201, 150, 58);
    private static final Color GOLD_LIGHT = new Color(240, 192, 96);
    private static final Color GOLD_DARK = new Color(100, 65, 15);
    private static final Color TEXT = new Color(242, 221, 174);

    public NameEntryScreen(GameWindow window) {
        this.window = window;
        setLayout(null);
        setBackground(Color.BLACK);

        nameField = new JTextField();
        nameField.setFont(new Font("Serif", Font.BOLD, 24));
        nameField.setHorizontalAlignment(JTextField.CENTER);
        nameField.setForeground(TEXT);
        nameField.setCaretColor(GOLD_LIGHT);
        nameField.setBackground(new Color(18, 9, 5));
        nameField.setBorder(BorderFactory.createLineBorder(GOLD_DARK, 2));
        add(nameField);

        nameField.addActionListener(e -> submitName());
        setupListeners();
    }

    private void setupListeners() {
        addMouseMotionListener(new MouseMotionAdapter() {
            @Override
            public void mouseMoved(MouseEvent e) {
                boolean oldContinue = continueHovered;
                boolean oldBack = backHovered;

                continueHovered = continueRect != null && continueRect.contains(e.getPoint());
                backHovered = backRect != null && backRect.contains(e.getPoint());

                setCursor(continueHovered || backHovered
                        ? Cursor.getPredefinedCursor(Cursor.HAND_CURSOR)
                        : Cursor.getDefaultCursor());

                if (oldContinue != continueHovered || oldBack != backHovered) {
                    repaint();
                }
            }
        });

        addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (continueRect != null && continueRect.contains(e.getPoint())) {
                    submitName();
                } else if (backRect != null && backRect.contains(e.getPoint())) {
                    window.showMainMenu();
                }
            }
        });
    }

    @Override
    public void addNotify() {
        super.addNotify();
        SwingUtilities.invokeLater(() -> {
            nameField.setText(window.getPlayerName());
            nameField.requestFocusInWindow();
        });
    }

    private void submitName() {
        String name = SaveManager.cleanName(nameField.getText());
        if (SaveManager.nameExists(name)) {
            errorMessage = "Name has been taken";
            repaint();
            return;
        }

        errorMessage = "";
        window.setPlayerName(name);
        window.setMonstersKilled(0);
        window.showCharacterSelect();
    }

    @Override
    public void doLayout() {
        super.doLayout();
        int fieldW = 420;
        int fieldH = 54;
        nameField.setBounds((getWidth() - fieldW) / 2, 302, fieldW, fieldH);
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

        int W = getWidth();
        int H = getHeight();

        GradientPaint bg = new GradientPaint(0, 0, new Color(28, 6, 6), 0, H, new Color(7, 3, 2));
        g2.setPaint(bg);
        g2.fillRect(0, 0, W, H);

        RadialGradientPaint glow = new RadialGradientPaint(
                new Point2D.Float(W / 2f, H * 0.45f),
                Math.max(W, H) * 0.55f,
                new float[]{0f, 1f},
                new Color[]{new Color(170, 70, 12, 80), new Color(0, 0, 0, 0)}
        );
        g2.setPaint(glow);
        g2.fillRect(0, 0, W, H);

        g2.setColor(new Color(0, 0, 0, 170));
        g2.fillRect(0, 0, W, H);

        g2.setFont(new Font("Serif", Font.BOLD, 42));
        String title = "ENTER YOUR NAME";
        FontMetrics fm = g2.getFontMetrics();
        int titleX = (W - fm.stringWidth(title)) / 2;
        g2.setColor(new Color(0, 0, 0, 180));
        g2.drawString(title, titleX + 3, 232);
        g2.setColor(GOLD_LIGHT);
        g2.drawString(title, titleX, 230);

        g2.setFont(new Font("Serif", Font.PLAIN, 18));
        String hint = "This name will be used for your save file and leaderboard record.";
        fm = g2.getFontMetrics();
        g2.setColor(TEXT);
        g2.drawString(hint, (W - fm.stringWidth(hint)) / 2, 272);

        if (!errorMessage.isEmpty()) {
            g2.setFont(new Font("Serif", Font.BOLD, 18));
            fm = g2.getFontMetrics();
            g2.setColor(new Color(0, 0, 0, 180));
            g2.drawString(errorMessage, (W - fm.stringWidth(errorMessage)) / 2 + 2, 376);
            g2.setColor(new Color(235, 90, 80));
            g2.drawString(errorMessage, (W - fm.stringWidth(errorMessage)) / 2, 374);
        }

        continueRect = new Rectangle(W / 2 - 210, 386, 420, 52);
        backRect = new Rectangle(W / 2 - 210, 456, 420, 52);

        drawButton(g2, continueRect, "START GAME", continueHovered);
        drawButton(g2, backRect, "BACK", backHovered);

        g2.dispose();
    }

    private void drawButton(Graphics2D g2, Rectangle rect, String label, boolean hovered) {
        if (hovered) {
            g2.setColor(new Color(GOLD.getRed(), GOLD.getGreen(), GOLD.getBlue(), 38));
            g2.fillRect(rect.x - 7, rect.y - 7, rect.width + 14, rect.height + 14);
        }

        g2.setColor(hovered ? new Color(80, 40, 10, 220) : new Color(20, 10, 5, 190));
        g2.fillRect(rect.x, rect.y, rect.width, rect.height);
        g2.setColor(hovered ? GOLD_LIGHT : GOLD_DARK);
        g2.setStroke(new BasicStroke(hovered ? 2f : 1.5f));
        g2.drawRect(rect.x, rect.y, rect.width, rect.height);

        g2.setFont(new Font("Serif", Font.BOLD, 17));
        FontMetrics fm = g2.getFontMetrics();
        int tx = rect.x + (rect.width - fm.stringWidth(label)) / 2;
        int ty = rect.y + (rect.height + fm.getAscent() - fm.getDescent()) / 2;
        g2.setColor(new Color(0, 0, 0, 180));
        g2.drawString(label, tx + 2, ty + 2);
        g2.setColor(hovered ? GOLD_LIGHT : GOLD);
        g2.drawString(label, tx, ty);
    }
}
