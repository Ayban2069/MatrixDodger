package Main;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * MatrixDodgerTitleScreen
 * Self-contained title screen with Matrix rain, flash intro, and "press any key".
 *
 * - Drop this panel into any JFrame, or run its main() to test standalone.
 * - setOnGameStart(Runnable) -> callback when user presses a key to start the game.
 */
public class MatrixDodgerTitleScreen extends JPanel {

    // ---- Intro flash timers ----
    private Timer flashTimer;   // toggles the big title during intro flashes
    private Timer mainBlinkTimer; // toggles the "press any key" text after intro
    private boolean visible = false;   // used for blinking text
    private int flashCount = 0;
    private final int MAX_FLASHES = 3;

    // ---- Matrix rain data ----
    private final List<MatrixColumn> columns = new ArrayList<>();
    private final Random random = new Random();
    private Timer matrixTimer; // repaints matrix rain (animation)

    // callback invoked when player presses any key
    private Runnable onGameStart;

    // recommended size
    private static final int PREF_W = 800;
    private static final int PREF_H = 600;

    public MatrixDodgerTitleScreen() {
        // panel setup
        setPreferredSize(new Dimension(PREF_W, PREF_H));
        setBackground(Color.BLACK);
        setFocusable(true);

        // create a set of random columns for the matrix rain
        for (int i = 0; i < 40; i++) {
            columns.add(new MatrixColumn(random.nextInt(PREF_W)));
        }

        // start the matrix rain AFTER construction to avoid leaking 'this'
        SwingUtilities.invokeLater(this::startMatrixRain);

        // intro flash sequence: toggle visible every 150ms
        flashTimer = new Timer(150, e -> {
            visible = !visible;
            if (!visible) {
                flashCount++;
                if (flashCount >= MAX_FLASHES) {
                    // stop intro flashes and begin slower blinking for "press any key"
                    flashTimer.stop();
                    startMainBlink();
                }
            }
            repaint();
        });
        // start intro
        flashTimer.start();

        // key listener: any key starts game
        addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                // stop timers, call callback
                startGame();
            }
        });

        // request focus once added to a top-level container (safe)
        addHierarchyListener(e -> {
            if ((e.getChangeFlags() & HierarchyEvent.SHOWING_CHANGED) != 0 && isShowing()) {
                // request focus when the panel is actually shown
                requestFocusInWindow();
            }
        });
    }

    /**
     * Start the background matrix animation (safe to call via invokeLater).
     */
    private void startMatrixRain() {
        // repaint timer drives the animation
        matrixTimer = new Timer(50, e -> {
            // update columns (positions) before repaint
            for (MatrixColumn c : columns) c.update(getHeight());
            repaint();
        });
        matrixTimer.start();
    }

    /**
     * Start the slower blink of the "press any key" prompt.
     */
    private void startMainBlink() {
        mainBlinkTimer = new Timer(500, e -> {
            visible = !visible;
            repaint();
        });
        mainBlinkTimer.start();
    }

    /**
     * Register a callback that runs when user presses any key to start the game.
     */
    public void setOnGameStart(Runnable callback) {
        this.onGameStart = callback;
    }

    /**
     * Stop timers and call callback.
     */
    public void startGame() {
        if (flashTimer != null) flashTimer.stop();
        if (mainBlinkTimer != null) mainBlinkTimer.stop();
        if (matrixTimer != null) matrixTimer.stop();

        if (onGameStart != null) {
            onGameStart.run();
        }
    }

    // -------------------------
    // Painting
    // -------------------------
    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2 = (Graphics2D) g.create();

        // draw matrix rain first (background)
        drawMatrixRain(g2);

        // draw title or flash text on top
        if (flashCount < MAX_FLASHES) {
            if (visible) drawFlashText(g2);
        } else {
            drawMainTitle(g2);
            // draw press-any-key prompt
            if (visible) drawPressPrompt(g2);
        }

        g2.dispose();
    }

    // draw flashing title used during the intro
    private void drawFlashText(Graphics2D g) {
        g.setColor(Color.WHITE);
        g.setFont(new Font("VT323", Font.BOLD, 64)); // pixel font (fallback to default if not present)
        String text = "MATRIX DODGER";
        drawCenteredString(g, text, getWidth(), getHeight() / 2);
    }

    // draw main title and subtitle
    private void drawMainTitle(Graphics2D g) {
        g.setColor(new Color(0, 255, 0));
        g.setFont(new Font("VT323", Font.BOLD, 60));
        String main = "MATRIX DODGER";
        int yMain = getHeight() / 2 - 50;
        drawCenteredString(g, main, getWidth(), yMain);

        g.setFont(new Font("VT323", Font.PLAIN, 24));
        String subtitle = "Dodge the Digital Storm";
        drawCenteredString(g, subtitle, getWidth(), yMain + 40);
    }

    private void drawPressPrompt(Graphics2D g) {
        g.setColor(Color.WHITE);
        g.setFont(new Font("VT323", Font.PLAIN, 20));
        String prompt = "Press any key to start...";
        drawCenteredString(g, prompt, getWidth(), getHeight() / 2 + 100);
    }

    // helper to center text at y
    private void drawCenteredString(Graphics2D g, String text, int containerWidth, int y) {
        FontMetrics fm = g.getFontMetrics();
        int textWidth = fm.stringWidth(text);
        int x = (containerWidth - textWidth) / 2;
        g.drawString(text, x, y);
    }

    // Draw matrix rain columns
    private void drawMatrixRain(Graphics2D g) {
        // use a mono/pixel-ish font for look (will fallback if font not present)
        g.setFont(new Font("VT323", Font.BOLD, 18));

        for (MatrixColumn col : columns) {
            col.draw(g);
        }
    }

    // -------------------------
    // Matrix column inner class
    // -------------------------
    private class MatrixColumn {
        private final int x;
        private int y;
        private final int speed;
        private final char[] chars = "01".toCharArray();

        MatrixColumn(int x) {
            this.x = x;
            this.y = random.nextInt(getHeight() + 600) - 600; // spread start positions
            this.speed = 4 + random.nextInt(6);
        }

        void update(int panelHeight) {
            // move downward
            y += speed;
            if (y > panelHeight + 400) {
                y = -random.nextInt(300);
            }
        }

        void draw(Graphics2D g) {
            int density = 22; // number of characters in the strip
            int charHeight = 18;
            for (int i = 0; i < density; i++) {
                // head bright, tail faded
                int alpha = Math.max(0, 255 - i * 10);
                g.setColor(new Color(0, 255, 0, alpha));
                char c = chars[random.nextInt(chars.length)];
                g.drawString(String.valueOf(c), x, y - i * charHeight);
            }
        }
    }

    // -------------------------
    // Quick test runner
    // -------------------------
    public static void main(String[] args) {
        // quick test frame for the title screen only
        SwingUtilities.invokeLater(() -> {
            JFrame f = new JFrame("Title Test");
            MatrixDodgerTitleScreen title = new MatrixDodgerTitleScreen();
            // set callback to print and close window (simulate opening HomeScreen)
            title.setOnGameStart(() -> {
                System.out.println("Start requested!");
                JOptionPane.showMessageDialog(f, "Would open HomeScreen here.");
                f.dispose();
            });

            f.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            f.setContentPane(title);
            f.pack();
            f.setLocationRelativeTo(null);
            f.setVisible(true);

            // ensure the panel gets focus for keys
            title.requestFocusInWindow();
        });
    }
}
