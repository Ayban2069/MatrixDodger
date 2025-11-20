package Main;

import java.awt.*;
import java.awt.image.BufferedImage;
import javax.imageio.ImageIO;
import java.net.URL;

public class Player {
    private int x, y;
    private int width, height;
    private int speed;
    private int lives;
    private boolean isInvincible;

    // Sprite animation
    private BufferedImage[] idleFrames;
    private int frameIndex = 0;
    private boolean facingLeft = false;
    private boolean facingRight = true;
    private boolean isRunning = false;
    private int animationDelay = 5; 
    private int animationCounter = 0;

    public Player(int x, int y, int width, int height, int speed, int lives) {
        this.x = x;
        this.y = y;
        this.width = width;
        this.height = height;
        this.speed = speed;
        this.lives = lives;
        this.isInvincible = false;

        loadIdleFrames();
    }

    // -----------------------------------------------------
    // LOAD SPRITE FRAMES
    // -----------------------------------------------------
    private void loadIdleFrames() {
        try {
            idleFrames = new BufferedImage[7]; // number of frames you said
            for (int i = 0; i < 7; i++) {
                String path = "/sprites/GamerGabby/frame"+i+".png";
                URL url = getClass().getResource(path);
                idleFrames[i] = ImageIO.read(url);
            }
        } catch (Exception e) {
            System.err.println("⚠ Failed to load sprite frames!");
            e.printStackTrace();
        }
    }

    // -----------------------------------------------------
    // ANIMATION UPDATE (call every tick)
    // -----------------------------------------------------
    public void updateAnimation() {
           animationCounter++;
    if (animationCounter >= animationDelay) {
        frameIndex = (frameIndex + 1) % idleFrames.length;
        animationCounter = 0;
    }
    }

    // -----------------------------------------------------
    // MOVEMENT (GamArena uses reflection, so no change here)
    // -----------------------------------------------------
    public void moveLeft() { 
        x -= speed;
        facingRight = false;
        isRunning = true;
    }

    public void moveRight() { 
        x += speed; 
        facingRight = true;
        isRunning = true;
    }

    public void moveUp() { y -= speed; }
    public void moveDown() { y += speed; }

    public void stopRunning() { isRunning = false; }

    // -----------------------------------------------------
    // COLLISION SYSTEM
    // -----------------------------------------------------
    public boolean collidesWith(Projectile p) {
        return (x < p.getX() + p.getSize() &&
                x + width > p.getX() &&
                y < p.getY() + p.getSize() &&
                y + height > p.getY());
    }

    public boolean collidesWith(LifePack lp) {
        return (x < lp.getX() + lp.getSize() &&
                x + width > lp.getX() &&
                y < lp.getY() + lp.getSize() &&
                y + height > lp.getY());
    }

    // -----------------------------------------------------
    // LIVES
    // -----------------------------------------------------
    public void loseLife() {
        if (!isInvincible && lives > 0) {
            lives--;
        }
    }

    public void gainLife() { lives++; }

    // -----------------------------------------------------
    // DRAW SPRITE
    // -----------------------------------------------------
    public void draw(Graphics g) {
        if (idleFrames == null) {
            // fallback in case images fail
            g.setColor(Color.BLUE);
            g.fillRect(x, y, width, height);
            return;
        }

        BufferedImage frame = idleFrames[frameIndex];
        if (!facingRight) frame = flip(frame);

        Graphics2D g2 = (Graphics2D) g.create();

        // Tilt when running
        if (isRunning) {
            double angle = Math.toRadians(facingRight ? 10 : -10);
            g2.rotate(angle, x + width / 2.0, y + height / 2.0);
        }

        g2.drawImage(frame, x, y, width, height, null);
        g2.dispose();
    }

    // -----------------------------------------------------
    // Sprite flip util for left movement
    // -----------------------------------------------------
    private BufferedImage flip(BufferedImage img) {
        int w = img.getWidth();
        int h = img.getHeight();
        BufferedImage flipped = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);

        Graphics2D g = flipped.createGraphics();
        g.drawImage(img, 0, 0, w, h, w, 0, 0, h, null);
        g.dispose();

        return flipped;
    }

    // -----------------------------------------------------
    // Needed for Sandevistan Ghost Trail
    // -----------------------------------------------------
    public BufferedImage getCurrentFrame() {
        BufferedImage frame = idleFrames[frameIndex];
        if (!facingRight) frame = flip(frame);
        return frame;
    }

    // -----------------------------------------------------
    // Getters & Setters
    // -----------------------------------------------------
    public int getX() { return x; }
    public int getY() { return y; }
    public int getWidth() { return width; }
    public int getHeight() { return height; }
    public int getLives() { return lives; }
    
    public boolean isFacingLeft() {
        return facingLeft;
    }
    public void setFacingLeft(boolean facingLeft) {
        this.facingLeft = facingLeft;
    }

    public void setX(int v) { this.x = v; }
    public void setY(int v) { this.y = v; }

    public boolean isInvincible() { return isInvincible; }
    public void setInvincible(boolean invincible) { this.isInvincible = invincible; }
}
