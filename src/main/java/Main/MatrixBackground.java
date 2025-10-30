package Main;

import javax.swing.*;
import java.awt.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * MatrixBackground
 * A reusable animated panel that draws the signature green "Matrix rain".
 *
 * ✅ How to use:
 *   JPanel screen = new HomeScreen();
 *   setLayout(new BorderLayout());
 *   add(new MatrixBackground(), BorderLayout.CENTER);
 *   // add other UI components *after*, set them opaque(false) so the rain shows through
 *
 * ✅ Features:
 * - Independent animation timer
 * - Adjustable column density and speed
 * - Can be added behind any layout or layered pane
 */
public class MatrixBackground extends JPanel {

    private final List<MatrixColumn> columns = new ArrayList<>();
    private final Random random = new Random();
    private Timer rainTimer;

    private static final int PREF_W = 800;
    private static final int PREF_H = 600;

    public MatrixBackground() {
        setOpaque(false); // allows underlying background to show through
        setPreferredSize(new Dimension(PREF_W, PREF_H));
        setBackground(Color.BLACK);

        // generate random columns across the width
        for (int i = 0; i < 40; i++) {
            columns.add(new MatrixColumn(random.nextInt(PREF_W)));
        }

        // start the rain animation after safe initialization
        SwingUtilities.invokeLater(this::startRain);
    }

    private void startRain() {
        rainTimer = new Timer(50, e -> {
            for (MatrixColumn c : columns) c.update(getHeight());
            repaint();
        });
        rainTimer.start();
    }

    public void stopRain() {
        if (rainTimer != null) rainTimer.stop();
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2 = (Graphics2D) g.create();

        // draw all columns
        g2.setFont(new Font("VT323", Font.BOLD, 18));
        for (MatrixColumn col : columns) {
            col.draw(g2);
        }

        g2.dispose();
    }

    // -------------------------
    // Inner class: MatrixColumn
    // -------------------------
    private class MatrixColumn {
        private final int x;
        private int y;
        private final int speed;
        private final char[] chars = "01".toCharArray();

        MatrixColumn(int x) {
            this.x = x;
            this.y = random.nextInt(getHeight() + 600) - 600; // randomize start
            this.speed = 4 + random.nextInt(6); // varied falling speeds
        }

        void update(int panelHeight) {
            y += speed;
            if (y > panelHeight + 400) {
                y = -random.nextInt(300);
            }
        }

        void draw(Graphics2D g) {
            int density = 22; // vertical character count per strip
            int charHeight = 18;
            for (int i = 0; i < density; i++) {
                int alpha = Math.max(0, 255 - i * 10);
                g.setColor(new Color(0, 255, 0, alpha));
                char c = chars[random.nextInt(chars.length)];
                g.drawString(String.valueOf(c), x, y - i * charHeight);
            }
        }
    }
}
