package Main;

import javax.swing.*;
import java.awt.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * MatrixRainPanel
 * ----------------------------
 * A reusable animated background that renders falling green "Matrix-style"
 * code rain. Can be added behind any UI components for a cyber effect.
 *
 * Usage Example:
 *   MatrixRainPanel rain = new MatrixRainPanel();
 *   frame.add(rain); // Add first (so it's behind)
 *   frame.add(otherUIComponents);
 */
public class MatrixRainPanel extends JPanel {
    private final List<Column> columns = new ArrayList<>();
    private final Timer timer;
    private final Random random = new Random();

    // Matrix code characters
    private static final String CHARACTERS = "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789@#$%^&*";

    // Adjust these constants to change appearance
    private static final int COLUMN_WIDTH = 20;  // Width of each vertical strip
    private static final int FONT_SIZE = 18;     // Character size
    private static final int FALL_SPEED_MIN = 4; // Min speed of rain
    private static final int FALL_SPEED_MAX = 10; // Max speed of rain
    private static final int CHAR_DENSITY = 20;  // Number of characters per strip

    public MatrixRainPanel() {
        setOpaque(false); // Transparent background so other components show above
        setDoubleBuffered(true);

        // Start the animation timer (every 50ms)
        timer = new Timer(50, e -> updateRain());
        timer.start();
    }

    @Override
    public void setBounds(int x, int y, int width, int height) {
        super.setBounds(x, y, width, height);
        initializeColumns(width, height);
    }

    /**
     * Initializes the falling columns when panel is resized or first added.
     */
    private void initializeColumns(int width, int height) {
        columns.clear();
        int numColumns = width / COLUMN_WIDTH;

        for (int i = 0; i < numColumns; i++) {
            columns.add(new Column(
                i * COLUMN_WIDTH,
                random.nextInt(height),
                random.nextInt(FALL_SPEED_MAX - FALL_SPEED_MIN) + FALL_SPEED_MIN
            ));
        }
    }

    /**
     * Updates position and visibility of all falling columns.
     */
    private void updateRain() {
        for (Column c : columns) {
            c.update(getHeight());
        }
        repaint();
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        drawMatrixRain((Graphics2D) g);
    }

    /**
     * Draws all vertical columns of green characters.
     */
    private void drawMatrixRain(Graphics2D g2d) {
        g2d.setFont(new Font("VT323", Font.BOLD, FONT_SIZE));

        for (Column c : columns) {
            int y = c.y;
            for (int i = 0; i < CHAR_DENSITY; i++) {
                // Compute color gradient — head is bright, trail fades
                float brightness = 1.0f - (float) i / CHAR_DENSITY;
                brightness = Math.max(brightness, 0.1f);
                g2d.setColor(new Color(0f, brightness, 0f));

                // Random character per frame
                char ch = CHARACTERS.charAt(random.nextInt(CHARACTERS.length()));
                g2d.drawString(String.valueOf(ch), c.x, y - (i * FONT_SIZE));
            }
        }
    }

    /**
     * Represents a single vertical stream of falling text.
     */
    private static class Column {
        int x, y, speed;

        public Column(int x, int startY, int speed) {
            this.x = x;
            this.y = startY;
            this.speed = speed;
        }

        public void update(int panelHeight) {
            y += speed;
            if (y > panelHeight + 100) {
                y = -100; // reset position when it leaves the screen
            }
        }
    }
}
