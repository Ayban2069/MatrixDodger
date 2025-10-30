package Main;

import java.awt.*;

public class Projectile {
    private double x, y;
    private double speedX, speedY;
    private int size;
    private int bouncesLeft = 3;

    private boolean slowed = false; // for Sandevistan effect

    public Projectile(double x, double y, double speedX, double speedY, int size) {
        this.x = x;
        this.y = y;
        this.speedX = speedX;
        this.speedY = speedY;
        this.size = size;
    }

    public void update(int arenaWidth, int arenaHeight) {
        double factor = slowed ? 0.3 : 1.0;

        x += speedX * factor;
        y += speedY * factor;

        // Bounce against walls
        if (x <= 20 || x + size >= arenaWidth - 20) {
            speedX *= -1;
            bouncesLeft--;
        }
        if (y <= 20 || y + size >= arenaHeight - 20) {
            speedY *= -1;
            bouncesLeft--;
        }
    }

    public void draw(Graphics g) {
        g.setColor(Color.RED);
        g.fillOval((int) x, (int) y, size, size);
    }

    public boolean shouldRemove() {
        return bouncesLeft <= 0;
    }

    // --- Sandevistan control ---
    public void setSlowed(boolean slowed) {
        this.slowed = slowed;
    }

    // --- Getters ---
    public int getX() { return (int) x; }
    public int getY() { return (int) y; }
    public int getSize() { return size; }
}
