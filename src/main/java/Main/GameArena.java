package Main;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Random;
import java.awt.image.BufferedImage;

/**
 * GameArena - cleaned & fixed:
 * - hamburger/menu button reliably positioned and visible
 * - borderless pause overlay that freezes gameLoop
 * - resume restores keyboard focus
 * - quit returns to HomeScreen
 * - preserved Sandevistan / TimeStop / trails / projectiles behavior
 */
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
    private int dashCooldown = 1000;
    private long lastDashTime = 0;
    private int dashVelocityX = 0;
    private int dashDuration = 8;
    private int dashFramesLeft = 0;

    private boolean movingLeft = false;
    private boolean movingRight = false;
    private int moveSpeed = 5;

    // Pause System (single source of truth)
    private boolean gamePaused = false;
    private Timer gameLoop;
    
    // Adjustable offsets for menu button position (tweak these values as needed)
    private int menuButtonXOffset = 65;  // Distance from right edge (higher = more left)
    private int menuButtonYOffset = 25;  // Distance from top edge (higher = lower)

    // UI
    private JButton menuButton;
    private JPanel pauseOverlay;

    // Sandevistan
    private boolean sandevistanActive = false;
    private int sandevistanDuration = 240;
    private int sandevistanFramesLeft = 0;
    private int sandevistanCooldown = 10000;
    private long lastSandevistanTime = 0;

    // Time Stop
    private boolean timeStopActive = false;
    private int timeStopDuration = 300;
    private int timeStopFramesLeft = 0;
    private int timeStopCooldown = 30000;
    private long lastTimeStopTime = 0;
    private int flashingDuration = 50;
    private int flashingFramesLeft = 0;

    // TRAIL — stores sprite frames
    private final int TRAIL_SIZE = 12;
    private BufferedImage[] trailFrames = new BufferedImage[TRAIL_SIZE];
    private int[][] trailPositions = new int[TRAIL_SIZE][2];
    private int trailIndex = 0;

    // Projectiles
    private List<Projectile> projectiles = new ArrayList<>();
    private Random random = new Random();
    private int spawnTimer = 0;

    public GameArena() {
        setFocusable(true);
        setBackground(Color.BLACK);
        setLayout(null); // we use absolute positioning for menu/overlay

        player = new Player(250, 100, 64, 64, 5, 3);

        setupKeyListener();

        gameLoop = new Timer(16, e -> {
            if (!gamePaused) update();
            else repaint(); // still repaint so overlay & UI remain responsive
        });
        gameLoop.start();

        setupMenuButton();

        // Ensure components get proper bounds after being shown/resized
        addComponentListener(new ComponentAdapter() {
            @Override
            public void componentResized(ComponentEvent e) {
                positionMenuButton();
                if (pauseOverlay != null) {
                    positionPauseOverlay();
                }
            }

            @Override
            public void componentShown(ComponentEvent e) {
                requestFocusInWindow();
            }
        });

        // get focus when added to frame
        addHierarchyListener(e -> {
            if ((e.getChangeFlags() & HierarchyEvent.SHOWING_CHANGED) != 0 && isShowing()) {
                requestFocusInWindow();
            }
        });
    }

    private void setupKeyListener() {
        // Using anonymous listener so key events go to this panel (must be focused)
        addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                // If paused, ignore game controls (but allow Escape to resume)
                if (gamePaused) {
                    if (e.getKeyCode() == KeyEvent.VK_ESCAPE) closePauseOverlay();
                    return;
                }

                switch (e.getKeyCode()) {
                    case KeyEvent.VK_A -> {
                        movingLeft = true;
                        player.setFacingLeft(true);
                        movingLeft = true;
                    }
                    case KeyEvent.VK_D -> {
                        movingRight = true;
                        player.setFacingLeft(false);
                        movingRight = true;
                    }
                    case KeyEvent.VK_SPACE -> {
                        if (jumpsUsed < maxJumps) {
                            velocityY = jumpStrength;
                            jumpsUsed++;
                        }
                    }
                    case KeyEvent.VK_Q -> dash();
                    case KeyEvent.VK_E -> activateSandevistan();
                    case KeyEvent.VK_R -> activateTimeStop();
                    case KeyEvent.VK_ESCAPE -> openPauseOverlay();
                }
            }

            @Override
            public void keyReleased(KeyEvent e) {
                if (e.getKeyCode() == KeyEvent.VK_A) movingLeft = false;
                if (e.getKeyCode() == KeyEvent.VK_D) movingRight = false;

                if (!movingLeft && !movingRight) {
                    player.stopRunning();
                }
            }
        });
    }

    // ================================
    // DASH
    // ================================
    private void dash() {
        long now = System.currentTimeMillis();
        if (now - lastDashTime > dashCooldown) {
            int dir = movingRight ? 1 : (movingLeft ? -1 : 1);
            dashVelocityX = dir * (dashDistance / dashDuration);
            dashFramesLeft = dashDuration;
            lastDashTime = now;
        }
    }

    // ================================
    // SANDEVISTAN
    // ================================
    private void activateSandevistan() {
        long now = System.currentTimeMillis();
        if (!sandevistanActive && now - lastSandevistanTime > sandevistanCooldown) {
            sandevistanActive = true;
            sandevistanFramesLeft = sandevistanDuration;
            lastSandevistanTime = now;
        }
    }

    // ================================
    // TIME STOP
    // ================================
    private void activateTimeStop() {
        long now = System.currentTimeMillis();
        if (!timeStopActive && now - lastTimeStopTime > timeStopCooldown) {
            timeStopActive = true;
            timeStopFramesLeft = timeStopDuration;
            flashingFramesLeft = flashingDuration;
            lastTimeStopTime = now;
        }
    }

    private void update() {
        // Update animation EVERY frame
        player.updateAnimation();

        // TIME STOP TIMER
        if (timeStopActive) {
            timeStopFramesLeft--;
            if (timeStopFramesLeft <= 0) timeStopActive = false;
            if (flashingFramesLeft > 0) flashingFramesLeft--;
        }

        // Gravity
        velocityY += gravity;
        applyGravity();

        // Movement
        if (sandevistanActive) {
            if (sandevistanFramesLeft > 0) {
                // move and ensure facing updates
                if (movingLeft && dashFramesLeft == 0) {
                    player.moveLeft();
                    player.setFacingLeft(true);
                    // small extra boost while sandevistan (optional)
                    player.setX(player.getX() - (int) (moveSpeed * 0.5));
                }
                if (movingRight && dashFramesLeft == 0) {
                    player.moveRight();
                    player.setFacingLeft(false);
                    player.setX(player.getX() + (int) (moveSpeed * 0.5));
                }
                sandevistanFramesLeft--;
            } else {
                sandevistanActive = false;
            }
        } else {
            if (movingLeft && dashFramesLeft == 0) player.moveLeft();
            if (movingRight && dashFramesLeft == 0) player.moveRight();
        }

        // DASH
        if (dashFramesLeft > 0) {
            player.setX(player.getX() + dashVelocityX);
            dashFramesLeft--;
            if (dashFramesLeft == 0) dashVelocityX = 0;
        }

        // TRAIL
        storeTrailFrame();

        // Projectiles spawn
        if (!timeStopActive) {
            spawnTimer++;
            if (spawnTimer > 60) {
                spawnProjectile();
                spawnTimer = 0;
            }
        }

        // Update projectiles
        Iterator<Projectile> it = projectiles.iterator();
        while (it.hasNext()) {
            Projectile p = it.next();
            if (!timeStopActive) {
                p.setSlowed(sandevistanActive);
                p.update(getWidth(), getHeight());
            }
            if (p.shouldRemove()) it.remove();
        }

        clampPlayerPosition();
        repaint();
    }

    private void applyGravity() {
        int ny = player.getY() + velocityY;
        int floor = getHeight() - 20 - player.getHeight();
        if (ny >= floor) {
            ny = floor;
            velocityY = 0;
            jumpsUsed = 0;
        }
        player.setY(ny);
    }

    // Store sprite frame in trail buffer
    private void storeTrailFrame() {
        trailFrames[trailIndex] = player.getCurrentFrame();
        trailPositions[trailIndex][0] = player.getX();
        trailPositions[trailIndex][1] = player.getY();
        trailIndex = (trailIndex + 1) % TRAIL_SIZE;
    }

    private void spawnProjectile() {
        int left = 20, right = getWidth() - 20;
        int top = 20, bottom = getHeight() - 20;

        if (right - left < 10 || bottom - top < 10) return;

        int side = random.nextInt(3);
        double x = 0, y = 0, sx = 0, sy = 0;

        switch (side) {
            case 0 -> { // top
                x = left + random.nextInt(Math.max(1, right - left));
                y = top;
                sx = random.nextBoolean() ? 2 : -2;
                sy = 3;
            }
            case 1 -> { // left
                x = left;
                y = top + random.nextInt(Math.max(1, bottom - top));
                sx = 3;
                sy = random.nextBoolean() ? 2 : -2;
            }
            default -> { // right
                x = right - 10;
                y = top + random.nextInt(Math.max(1, bottom - top));
                sx = -3;
                sy = random.nextBoolean() ? 2 : -2;
            }
        }

        projectiles.add(new Projectile(x, y, sx, sy, 16));
    }

    private Color getRainbowColor(int index) {
        float hue = (System.currentTimeMillis() % 2000) / 2000f;
        hue += (index * 0.05f);
        return Color.getHSBColor(hue % 1f, 1f, 1f);
    }

    private void clampPlayerPosition() {
        int arenaLeft = 20;
        int arenaTop = 20;
        int arenaRight = getWidth() - 20;
        int arenaBottom = getHeight() - 20;

        if (player.getX() < arenaLeft) player.setX(arenaLeft);
        if (player.getX() + player.getWidth() > arenaRight)
            player.setX(arenaRight - player.getWidth());
        if (player.getY() < arenaTop) player.setY(arenaTop);
        if (player.getY() + player.getHeight() > arenaBottom)
            player.setY(arenaBottom - player.getHeight());
    }

    // ----------------------------
    // Menu button + pause overlay
    // ----------------------------
    private void setupMenuButton() {
        menuButton = new JButton("☰");  // Use HTML to create three horizontal lines
        menuButton.setFont(new Font("VT323", Font.BOLD, 22));
        menuButton.setForeground(Color.GREEN);
        menuButton.setBackground(Color.BLACK);
        menuButton.setBorder(BorderFactory.createLineBorder(Color.GREEN));
        menuButton.setFocusPainted(false);

        // Make it visually borderless and non-opaque so it feels "matrixy"
        menuButton.setContentAreaFilled(false);
        menuButton.setOpaque(false);

        // Don't let the button steal keyboard focus
        menuButton.setFocusable(false);

        menuButton.addActionListener(e -> {
            // toggle overlay: open if closed, close if already open
            if (gamePaused) closePauseOverlay();
            else openPauseOverlay();
        });

        add(menuButton);
        positionMenuButton();
    }




    private void positionMenuButton() {
        // Position within arena border and account for component size
        int bx = getWidth() - menuButtonXOffset;
        if (bx < 30) bx = getWidth() - 40; // fallback
        int by = menuButtonYOffset;
        menuButton.setBounds(bx, by, 40, 40);
        menuButton.revalidate();
        menuButton.repaint();
        // ensure button is on top
        setComponentZOrder(menuButton, 0);
    }

    private void openPauseOverlay() {
        if (pauseOverlay != null) return; // already open
        gamePaused = true;

        pauseOverlay = new JPanel(null);
        pauseOverlay.setBackground(new Color(0, 0, 0, 180));
        pauseOverlay.setOpaque(true);
        positionPauseOverlay();

        // Resume button
        JButton resumeBtn = new JButton("RESUME");
        resumeBtn.setFont(new Font("VT323", Font.BOLD, 28));
        resumeBtn.setForeground(Color.GREEN);
        resumeBtn.setBackground(Color.BLACK);
        resumeBtn.setBorder(BorderFactory.createLineBorder(Color.GREEN));
        resumeBtn.setBorderPainted(true);
        resumeBtn.setFocusPainted(false);
        resumeBtn.setContentAreaFilled(false);
        resumeBtn.setBounds((pauseOverlay.getWidth() - 200) / 2, 180, 200, 50);
        resumeBtn.addActionListener(e -> closePauseOverlay());

        // Quit button
        JButton quitBtn = new JButton("QUIT");
        quitBtn.setFont(new Font("VT323", Font.BOLD, 28));
        quitBtn.setForeground(Color.RED);
        quitBtn.setBackground(Color.BLACK);
        quitBtn.setBorder(BorderFactory.createLineBorder(Color.GREEN));
        quitBtn.setBorderPainted(true);
        quitBtn.setFocusPainted(false);
        quitBtn.setContentAreaFilled(false);
        quitBtn.setBounds((pauseOverlay.getWidth() - 200) / 2, 260, 200, 50);
        quitBtn.addActionListener(e -> {
            closePauseOverlay();
            goToHomeScreen();
        });

        pauseOverlay.add(resumeBtn);
        pauseOverlay.add(quitBtn);

        // Add overlay above everything
        add(pauseOverlay);
        setComponentZOrder(pauseOverlay, 0); // ensure it paints above
        repaint();

        // ensure keyboard focus returns to this panel after overlay is opened, so ESC works
        SwingUtilities.invokeLater(() -> requestFocusInWindow());
    }


    private void positionPauseOverlay() {
        if (pauseOverlay == null) return;
        pauseOverlay.setBounds(20, 20, getWidth() - 40, getHeight() - 40);
        // reposition child buttons if added earlier
        for (Component c : pauseOverlay.getComponents()) {
            if (c instanceof JButton) {
                // reposition by name bounds (we used fixed bounds above)
                // keep them centered
                JButton b = (JButton) c;
                if ("RESUME".equals(b.getText())) {
                    b.setBounds((pauseOverlay.getWidth() - 200) / 2, 180, 200, 50);
                } else if ("QUIT".equals(b.getText())) {
                    b.setBounds((pauseOverlay.getWidth() - 200) / 2, 260, 200, 50);
                }
            }
        }
        pauseOverlay.revalidate();
        pauseOverlay.repaint();
    }

    private void closePauseOverlay() {
        if (pauseOverlay != null) {
            remove(pauseOverlay);
            pauseOverlay = null;
        }
        gamePaused = false;

        // request focus back so KeyListener works again
        SwingUtilities.invokeLater(() -> {
            requestFocusInWindow();
            repaint();
        });
    }

    private void goToHomeScreen() {
        JFrame frame = (JFrame) SwingUtilities.getWindowAncestor(this);
        frame.getContentPane().removeAll();
        frame.getContentPane().add(new HomeScreen(new SoundManager()));
        frame.revalidate();
        frame.repaint();
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);

        // Arena
        g.setColor(Color.GREEN);
        g.drawRect(20, 20, getWidth() - 40, getHeight() - 40);
        g.drawLine(20, getHeight() - 20, getWidth() - 20, getHeight() - 20);

        // Projectiles
        for (Projectile p : projectiles) p.draw(g);

        // Dash ghost — real sprite now
        if (dashFramesLeft > 0) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.35f));
            g2.drawImage(player.getCurrentFrame(),
                    player.getX() - dashVelocityX,
                    player.getY(),
                    player.getWidth(),
                    player.getHeight(),
                    null);
            g2.dispose();
        }

        // Sandevistan trail — rainbow-tinted PNG-like sprites
        if (sandevistanActive) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.5f)); // base alpha

            for (int i = 0; i < TRAIL_SIZE; i++) {
                if (trailFrames[i] != null) {
                    BufferedImage tinted = new BufferedImage(trailFrames[i].getWidth(),
                            trailFrames[i].getHeight(), BufferedImage.TYPE_INT_ARGB);
                    Graphics2D tg = tinted.createGraphics();
                    tg.drawImage(trailFrames[i], 0, 0, null);
                    tg.setComposite(AlphaComposite.SrcAtop.derive(0.7f));
                    tg.setColor(getRainbowColor(i));
                    tg.fillRect(0, 0, tinted.getWidth(), tinted.getHeight());
                    tg.dispose();

                    g2.drawImage(tinted,
                            trailPositions[i][0],
                            trailPositions[i][1],
                            player.getWidth(),
                            player.getHeight(),
                            null);
                }
            }
            g2.dispose();
        }

        // Time Stop flashing effect — only for the first flashingFramesLeft frames
        if (timeStopActive && flashingFramesLeft > 0) {
            Graphics2D g2 = (Graphics2D) g.create();
            float flashAlpha = (float) (0.5 + 0.5 * Math.sin(System.currentTimeMillis() / 20.0));
            g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, flashAlpha));
            g2.setColor(getRainbowColor(0));
            g2.fillRect(20, 20, getWidth() - 40, getHeight() - 40);
            g2.dispose();
        }

        // Player
        player.draw(g);
    }
}
