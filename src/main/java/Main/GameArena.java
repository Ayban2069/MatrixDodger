package Main;

import javax.swing.*;
import java.awt.*;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.Iterator;

public class GameArena extends JPanel {
    private Player player;

    // Physics
    private int gravity = 1;
    private int jumpStrength = -20;
    private int velocityY = 0;

    // Double jump
    private int maxJumps = 2;
    private int jumpsUsed = 0;

    // Dash
    private int dashDistance = 200;
    private int dashCooldown = 1000; // 1 sec
    private long lastDashTime = 0;
    private int dashVelocityX = 0;
    private int dashDuration = 8; // frames
    private int dashFramesLeft = 0;

    // Movement flags
    private boolean movingLeft = false;
    private boolean movingRight = false;
    private int moveSpeed = 5;

    // Sandevistan
    private boolean sandevistanActive = false;
    private int sandevistanDuration = 240; // ~2 sec
    private int sandevistanFramesLeft = 0;
    private int sandevistanCooldown = 5000; // 5 sec
    private long lastSandevistanTime = 0;

    // Time Stop
    private boolean timeStopActive = false;
    private int timeStopDuration = 300; // 5 sec
    private int timeStopFramesLeft = 0;
    private int timeStopCooldown = 60000; // 1 min
    private long lastTimeStopTime = 0;

    // Trail effect for Sandevistan
    private int[][] trailPositions = new int[10][2];
    private int trailIndex = 0;

    // Projectiles
    private List<Projectile> projectiles = new ArrayList<>();
    private Random random = new Random();
    private int spawnTimer = 0;

    public GameArena() {
        setFocusable(true);
        setBackground(Color.BLACK);
        player = new Player(250, 100, 32, 32, 5, 3);

        setupKeyListener();

        Timer gameLoop = new Timer(16, e -> update());
        gameLoop.start();
    }

    private void setupKeyListener() {
        addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                switch (e.getKeyCode()) {
                    case KeyEvent.VK_A -> movingLeft = true;
                    case KeyEvent.VK_D -> movingRight = true;
                    case KeyEvent.VK_SPACE -> {
                        if (jumpsUsed < maxJumps) {
                            velocityY = jumpStrength;
                            jumpsUsed++;
                        }
                    }
                    case KeyEvent.VK_Q -> dash();
                    case KeyEvent.VK_E -> activateSandevistan();
                    case KeyEvent.VK_R -> activateTimeStop();
                }
            }

            @Override
            public void keyReleased(KeyEvent e) {
                switch (e.getKeyCode()) {
                    case KeyEvent.VK_A -> movingLeft = false;
                    case KeyEvent.VK_D -> movingRight = false;
                }
            }
        });
    }

    // --- Dash ---
    private void dash() {
        long now = System.currentTimeMillis();
        if (now - lastDashTime > dashCooldown) {
            int dashDir = movingRight ? 1 : (movingLeft ? -1 : 1);
            dashVelocityX = dashDir * (dashDistance / dashDuration);
            dashFramesLeft = dashDuration;
            lastDashTime = now;
        }
    }

    // --- Sandevistan ---
    private void activateSandevistan() {
        long now = System.currentTimeMillis();
        if (!sandevistanActive && now - lastSandevistanTime > sandevistanCooldown) {
            sandevistanActive = true;
            sandevistanFramesLeft = sandevistanDuration;
            lastSandevistanTime = now;
        }
    }

    // --- Time Stop ---
    private void activateTimeStop() {
        long now = System.currentTimeMillis();
        if (!timeStopActive && now - lastTimeStopTime > timeStopCooldown) {
            timeStopActive = true;
            timeStopFramesLeft = timeStopDuration;
            lastTimeStopTime = now;
        }
    }

    private void update() {
        // --- Time Stop logic ---
        if (timeStopActive) {
            timeStopFramesLeft--;
            if (timeStopFramesLeft <= 0) {
                timeStopActive = false;
            }
        }

        // Gravity
        velocityY += gravity;
        playerFall();

        // Normal / Sandevistan movement
        if (!timeStopActive) {
            if (sandevistanActive) {
                if (sandevistanFramesLeft > 0) {
                    velocityY += gravity * 0.3;
                    if (movingLeft && dashFramesLeft == 0 && player.getX() > 20) {
                        setPlayerX(player.getX() - (int) (moveSpeed * 1.5));
                    }
                    if (movingRight && dashFramesLeft == 0 && player.getX() + player.getWidth() < getWidth() - 20) {
                        setPlayerX(player.getX() + (int) (moveSpeed * 1.5));
                    }
                    sandevistanFramesLeft--;
                } else {
                    sandevistanActive = false;
                }
            } else {
                if (movingLeft && dashFramesLeft == 0 && player.getX() > 20) {
                    setPlayerX(player.getX() - moveSpeed);
                }
                if (movingRight && dashFramesLeft == 0 && player.getX() + player.getWidth() < getWidth() - 20) {
                    setPlayerX(player.getX() + moveSpeed);
                }
            }
        }

        // Dash motion
        if (dashFramesLeft > 0) {
            int newX = player.getX() + dashVelocityX;
            if (newX < 20) newX = 20;
            if (newX + player.getWidth() > getWidth() - 20)
                newX = getWidth() - 20 - player.getWidth();
            setPlayerX(newX);
            dashFramesLeft--;
            if (dashFramesLeft == 0) dashVelocityX = 0;
        }

        // Spawn projectiles (disabled if Time Stop active)
        if (!timeStopActive) {
            spawnTimer++;
            if (spawnTimer > 60) {
                spawnProjectile();
                spawnTimer = 0;
            }
        }

        // Update projectiles (freeze if Time Stop)
        Iterator<Projectile> it = projectiles.iterator();
        while (it.hasNext()) {
            Projectile p = it.next();
            if (!timeStopActive) {
                p.setSlowed(sandevistanActive);
                p.update(getWidth(), getHeight());
            }
            if (p.shouldRemove()) {
                it.remove();
            }
        }

        // Trail (Sandevistan)
        trailPositions[trailIndex][0] = player.getX();
        trailPositions[trailIndex][1] = player.getY();
        trailIndex = (trailIndex + 1) % trailPositions.length;

        repaint();
    }

    private void playerFall() {
        int newY = player.getY() + velocityY;
        if (newY + player.getHeight() >= getHeight() - 20) {
            newY = getHeight() - 20 - player.getHeight();
            velocityY = 0;
            jumpsUsed = 0;
        }
        setPlayerY(newY);
    }

    private void setPlayerY(int y) {
        try {
            var fieldY = Player.class.getDeclaredField("y");
            fieldY.setAccessible(true);
            fieldY.setInt(player, y);
        } catch (Exception ignored) {}
    }

    private void setPlayerX(int x) {
        try {
            var fieldX = Player.class.getDeclaredField("x");
            fieldX.setAccessible(true);
            fieldX.setInt(player, x);
        } catch (Exception ignored) {}
    }

    private void spawnProjectile() {
        int arenaLeft = 20;
        int arenaRight = getWidth() - 20;
        int arenaTop = 20;
        int arenaBottom = getHeight() - 20;

        if (arenaRight - arenaLeft < 10 || arenaBottom - arenaTop < 10) return;

        int side = random.nextInt(3); // top, left, right
        double x = 0, y = 0;
        double speedX = 0, speedY = 0;

        switch (side) {
            case 0 -> { // top
                x = arenaLeft + random.nextInt(Math.max(1, arenaRight - arenaLeft - 10));
                y = arenaTop;
                speedX = random.nextBoolean() ? 2 : -2;
                speedY = 3;
            }
            case 1 -> { // left
                x = arenaLeft;
                y = arenaTop + random.nextInt(Math.max(1, arenaBottom - arenaTop - 10));
                speedX = 3;
                speedY = random.nextBoolean() ? 2 : -2;
            }
            default -> { // right
                x = arenaRight - 10;
                y = arenaTop + random.nextInt(Math.max(1, arenaBottom - arenaTop - 10));
                speedX = -3;
                speedY = random.nextBoolean() ? 2 : -2;
            }
        }

        projectiles.add(new Projectile(x, y, speedX, speedY, 16));
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);

        // Arena
        g.setColor(Color.GREEN);
        g.drawRect(20, 20, getWidth() - 40, getHeight() - 40);
        g.drawLine(20, getHeight() - 20, getWidth() - 20, getHeight() - 20);

        // Projectiles
        for (Projectile p : projectiles) {
            p.draw(g);
        }

        // Dash ghost
        if (dashFramesLeft > 0) {
            g.setColor(new Color(0, 255, 0, 100));
            g.fillRect(player.getX() - dashVelocityX, player.getY(), player.getWidth(), player.getHeight());
        }

        // Sandevistan trail
        if (sandevistanActive) {
            g.setColor(new Color(0, 200, 255, 120));
            for (int[] pos : trailPositions) {
                if (pos[0] != 0 || pos[1] != 0) {
                    g.fillRect(pos[0], pos[1], player.getWidth(), player.getHeight());
                }
            }
        }

        // Time Stop overlay (flashing)
        if (timeStopActive) {
            int alpha = (int) (128 + 127 * Math.sin(System.currentTimeMillis() / 100.0));
            g.setColor(new Color(255, 255, 0, alpha));
            g.fillRect(20, 20, getWidth() - 40, getHeight() - 40);
        }

        // Player
        player.draw(g);
    }
}
