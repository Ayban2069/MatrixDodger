package Main;

import java.awt.Graphics;
import java.awt.Color;

public class Player {
    private int x, y;
    private int width, height;
    private int speed;
    private int lives;             // number of lives
    private boolean isInvincible;

    public Player(int x, int y, int width, int height, int speed, int lives) {
        this.x = x;
        this.y = y;
        this.width = width;
        this.height = height;
        this.speed = speed;
        this.lives = lives;
        this.isInvincible = false;
    }

    // Movement
    public void moveLeft() { x -= speed; }
    public void moveRight() { x += speed; }
    public void moveUp() { y -= speed; }
    public void moveDown() { y += speed; }

    // Collision check
    public boolean collidesWith(Projectile p) {
        return (x < p.getX() + p.getSize() &&
                x + width > p.getX() &&
                y < p.getY() + p.getSize() &&
                y + height > p.getY());
    }

    // Collision with LifePack
    public boolean collidesWith(LifePack lp) {
        return (x < lp.getX() + lp.getSize() &&
                x + width > lp.getX() &&
                y < lp.getY() + lp.getSize() &&
                y + height > lp.getY());
    }

    // Life management
    public void loseLife() {
        if (!isInvincible && lives > 0) {
            lives--;
        }
    }

    public void gainLife() {
        lives++;
    }

    // 🔹 Draw placeholder (blue box)
    public void draw(Graphics g) {
        g.setColor(Color.BLUE);
        g.fillRect(x, y, width, height);
    }

    // Getters/Setters
    public int getX() { return x; }
    public int getY() { return y; }
    public int getWidth() { return width; }
    public int getHeight() { return height; }
    public int getLives() { return lives; }

    public boolean isInvincible() { return isInvincible; }
    public void setInvincible(boolean invincible) { this.isInvincible = invincible; }
}
