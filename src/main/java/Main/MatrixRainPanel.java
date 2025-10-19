package Main;

import javax.swing.*;
import java.awt.*;
import java.util.Random;

public class MatrixRainPanel extends JPanel {
    private final int width, height;
    private final int fontSize = 18;
    private final int columns;
    private final int[] drops;
    private final Random random = new Random();
    private final char[] chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789@#$%&".toCharArray();

    public MatrixRainPanel(int width, int height) {
        this.width = width;
        this.height = height;
        setPreferredSize(new Dimension(width, height));
        setBackground(Color.BLACK);

        columns = width / fontSize;
        drops = new int[columns];
        for (int i = 0; i < columns; i++) {
            drops[i] = random.nextInt(height / fontSize);
        }

        Timer timer = new Timer(50, e -> repaint()); // ~20 FPS
        timer.start();
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2d = (Graphics2D) g;

        // Slight transparent black overlay to create trailing effect
        g2d.setColor(new Color(0, 0, 0, 25));
        g2d.fillRect(0, 0, width, height);

        g2d.setFont(new Font("Monospaced", Font.BOLD, fontSize));

        for (int i = 0; i < columns; i++) {
            // Random green color for "digital rain" effect
            g2d.setColor(new Color(0, 255, 70, 200));

            // Random character
            char c = chars[random.nextInt(chars.length)];

            // Draw character
            int x = i * fontSize;
            int y = drops[i] * fontSize;
            g2d.drawString(String.valueOf(c), x, y);

            // Move the character down
            if (y > height && random.nextDouble() > 0.975) {
                drops[i] = 0;
            }
            drops[i]++;
        }
    }
}
