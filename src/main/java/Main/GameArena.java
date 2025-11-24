package Main;

import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * GameArena with:
 *  - Camera shake
 *  - Revive particle sparks
 *  - Slow-motion effect (by update throttling while slowMotionFrames > 0)
 *  - Glowing wings on revive
 *  - Proper paint order (transform applied to everything)
 *
 * Replace your current GameArena with this class.
 */
public class GameArena extends JPanel {

    private GameManager manager;
    private Player player; // reference to manager.getPlayer()

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

    // Movement State
    private boolean movingLeft = false;
    private boolean movingRight = false;
    private int moveSpeed = 5;

    // Pause System
    private boolean gamePaused = false;
    private Timer gameLoop;

    // UI
    private int menuButtonXOffset = 65;
    private int menuButtonYOffset = 25;
    private JButton menuButton;
    private JPanel pauseOverlay;

    // Difficulty Scaling
    private long startTime = System.currentTimeMillis();
    private int difficultyLevel = 1; // Used to scale speed and spawn rate
    private int spawnTimer = 0;

    private BufferedImage heartIcon;

    // Heart loss animation
    private int previousLives = 0; // Will be set in constructor
    private int heartLossAnimationFrames = 0;

    // Revive / VFX state
    private int reviveVFXFrames = 0;         // governs beam + explosion visuals
    private int cameraShakeFrames = 0;       // camera shake countdown
    private int cameraShakeIntensity = 12;   // pixels
    private int slowMotionFrames = 0;        // slow-mo countdown (throttles updates)
    private int updateTickCounter = 0;       // used for throttling when slow-motion active

    SoundManager soundManager = new SoundManager();

    // Particles & wings
    private final List<ReviveParticle> reviveParticles = new ArrayList<>();
    private final List<SparkParticle> sparkParticles = new ArrayList<>();

    public GameArena(GameManager gm) {
        this.manager = gm;
        this.player = manager.getPlayer();
        this.previousLives = player.getLives(); // Initialize previousLives

        // load heart safely
        try {
            heartIcon = ImageIO.read(getClass().getResource("/assets/heart.png"));
        } catch (Exception ex) {
            heartIcon = null;
            System.out.println("Failed to load heart.png (it's optional) — continuing without it.");
        }

        setPreferredSize(new Dimension(1024, 768));
        setFocusable(true);
        setBackground(Color.BLACK);
        setLayout(null);

        setupKeyListener();

        // 60 FPS-ish loop
        gameLoop = new Timer(16, e -> {
            if (!gamePaused) update();
            else repaint();
        });
        gameLoop.start();

        setupMenuButton();

        addComponentListener(new ComponentAdapter() {
            @Override
            public void componentResized(ComponentEvent e) {
                positionMenuButton();
                if (pauseOverlay != null) positionPauseOverlay();
            }
            @Override
            public void componentShown(ComponentEvent e) {
                requestFocusInWindow();
            }
        });

        addHierarchyListener(e -> {
            if ((e.getChangeFlags() & HierarchyEvent.SHOWING_CHANGED) != 0 && isShowing()) {
                requestFocusInWindow();
            }
        });
    }

    private void setupKeyListener() {
        addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                if (gamePaused) {
                    if (e.getKeyCode() == KeyEvent.VK_ESCAPE) closePauseOverlay();
                    return;
                }

                int key = e.getKeyCode();

                // If cinematic ultimate active, allow only pause
                if (manager.isClearSkillActive()) {
                    if (key == KeyEvent.VK_ESCAPE) openPauseOverlay();
                    return;
                }

                if (key == KeyEvent.VK_A || key == KeyEvent.VK_LEFT) {
                    movingLeft = true;
                    player.setFacingLeft(true);
                }
                else if (key == KeyEvent.VK_D || key == KeyEvent.VK_RIGHT) {
                    movingRight = true;
                    player.setFacingLeft(false);
                }
                else if (key == KeyEvent.VK_SPACE || key == KeyEvent.VK_UP) {
                    soundManager.playSound("jump");
                    if (jumpsUsed < maxJumps) {
                        velocityY = jumpStrength;
                        jumpsUsed++;
                    }
                }
                else if (key == KeyEvent.VK_Q) dash();
                else if (key == KeyEvent.VK_R) manager.tryUseSkill(manager.getSkillR());
                else if (key == KeyEvent.VK_F) manager.tryUseSkill(manager.getSkillF());
                else if (key == KeyEvent.VK_V) manager.tryUseSkill(manager.getSkillV());
                else if (key == KeyEvent.VK_ESCAPE) openPauseOverlay();
            }

            @Override
            public void keyReleased(KeyEvent e) {
                int key = e.getKeyCode();
                if (key == KeyEvent.VK_A || key == KeyEvent.VK_LEFT) movingLeft = false;
                if (key == KeyEvent.VK_D || key == KeyEvent.VK_RIGHT) movingRight = false;
                if (!movingLeft && !movingRight) player.stopRunning();
            }
        });
    }

    // ================================
    // DASH
    // ================================
    private void dash() {
        long now = System.currentTimeMillis();
        if (now - lastDashTime > dashCooldown) {
            int dir = movingRight ? 1 : (movingLeft ? -1 : (player.isFacingLeft() ? -1 : 1));
            dashVelocityX = dir * (dashDistance / dashDuration);
            dashFramesLeft = dashDuration;
            lastDashTime = now;
        }
    }

    // ================================
    // UPDATE LOOP
    // ================================
    private void update() {
        updateTickCounter++;

        long elapsedSeconds = (System.currentTimeMillis() - startTime) / 1000;
        difficultyLevel = 1 + (int) (elapsedSeconds / 20);

        player.updateAnimation();

        // handle revive trigger from manager (manager should set revivedThisFrame true for one frame)
        if (manager.revivedThisFrame) {
            // spawn revive VFX
            reviveVFXFrames = 60;            // full VFX time
            cameraShakeFrames = 20;          // intense shake for a bit
            slowMotionFrames = 40;           // slow-mo for a short duration (throttling)
            // spawn lots of particles
            spawnReviveParticles(40);
            spawnSparks(24);
            soundManager.playSound("finalhit"); // or a revive-specific sfx if you have one

            // IMPORTANT: assume GameManager marks this true only for one frame.
            // If not, manager should reset it — else we'll prevent repeated triggers by clearing it here:
            // try to be safe: if manager exposes the flag publicly we can clear it (not ideal if private)
            try {
                manager.revivedThisFrame = false;
            } catch (Exception ignore) {}
        }

        // throttled manager.update for slow-motion: when slowMotionFrames > 0, call update every other tick
        boolean shouldUpdateManager = true;
        if (slowMotionFrames > 0) {
            shouldUpdateManager = (updateTickCounter % 2 == 0);
            slowMotionFrames--;
        }
        if (shouldUpdateManager) manager.update(getWidth(), getHeight());

        // Check for heart loss
        int currentLives = player.getLives();
        if (currentLives < previousLives) {
            heartLossAnimationFrames = 30; // Start animation
            // small camera nudge on hit
            cameraShakeFrames = Math.max(cameraShakeFrames, 8);
            // short invincibility flash handled by player.setInvincible possibly
        }
        previousLives = currentLives;

        // Decrement other animations
        if (heartLossAnimationFrames > 0) heartLossAnimationFrames--;
        if (cameraShakeFrames > 0) cameraShakeFrames--;

        // Update particles
        Iterator<ReviveParticle> rit = reviveParticles.iterator();
        while (rit.hasNext()) {
            ReviveParticle rp = rit.next();
            rp.update();
            if (rp.life <= 0) rit.remove();
        }

        Iterator<SparkParticle> sit = sparkParticles.iterator();
        while (sit.hasNext()) {
            SparkParticle sp = sit.next();
            sp.update();
            if (sp.life <= 0) sit.remove();
        }

        // Physics & Movement
        velocityY += gravity;
        applyGravity();

        // Movement - sandevistan speed handled by manager state (we replicate the same movement style)
        if (manager.isSandevistanActive()) {
            if (manager.getSandevistanFramesLeft() > 0) {
                if (movingLeft && dashFramesLeft == 0) {
                    player.moveLeft();
                    player.setFacingLeft(true);
                    player.setX(player.getX() - (int) (moveSpeed * 0.5));
                }
                if (movingRight && dashFramesLeft == 0) {
                    player.moveRight();
                    player.setFacingLeft(false);
                    player.setX(player.getX() + (int) (moveSpeed * 0.5));
                }
            }
        } else {
            if (movingLeft && dashFramesLeft == 0) player.moveLeft();
            if (movingRight && dashFramesLeft == 0) player.moveRight();
        }

        // Dash physics
        if (dashFramesLeft > 0) {
            player.setX(player.getX() + dashVelocityX);
            dashFramesLeft--;
            if (dashFramesLeft == 0) dashVelocityX = 0;
        }

        // clamp player
        clampPlayerPosition();

        repaint();
    }

    // spawn revive particles around player
    private void spawnReviveParticles(int amount) {
        int cx = player.getX() + player.getWidth() / 2;
        int cy = player.getY() + player.getHeight() / 2;
        for (int i = 0; i < amount; i++) {
            reviveParticles.add(new ReviveParticle(cx, cy));
        }
    }

    // spawn spark particles (brighter, faster)
    private void spawnSparks(int amount) {
        int cx = player.getX() + player.getWidth() / 2;
        int cy = player.getY() + player.getHeight() / 2;
        for (int i = 0; i < amount; i++) {
            sparkParticles.add(new SparkParticle(cx, cy));
        }
    }

    // ================================
    // DRAWING (uses manager state for skills)
    // layered & transform-safe
    // ================================
    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);

        // camera shake offsets
        int shakeX = 0, shakeY = 0;
        if (cameraShakeFrames > 0) {
            shakeX = (int) (Math.random() * cameraShakeIntensity) - cameraShakeIntensity / 2;
            shakeY = (int) (Math.random() * cameraShakeIntensity) - cameraShakeIntensity / 2;
        }

        Graphics2D g2 = (Graphics2D) g.create();
        // apply shake to everything
        g2.translate(shakeX, shakeY);

        // ================================
        // 1. Arena Border
        // ================================
        g2.setColor(Color.GREEN);
        g2.drawRect(20, 20, getWidth() - 40, getHeight() - 40);
        g2.drawLine(20, getHeight() - 20, getWidth() - 20, getHeight() - 20);

        // ================================
        // 2. Projectiles OR Clear Skill Cinematic
        // ================================
        if (manager.isClearSkillActive()) {
            Graphics2D gC = (Graphics2D) g2.create();
            gC.setColor(new Color(255, 0, 255, 150));

            List<Projectile> clearTargets = manager.getClearTargets();
            int startIndex = manager.getClearTargetIndex();

            for (int i = startIndex; i < clearTargets.size(); i++) {
                Projectile t = clearTargets.get(i);
                int size = t.getSize();
                gC.drawRect((int) t.getX(), (int) t.getY(), size, size);
                gC.drawLine((int) t.getX(), (int) t.getY(), (int) t.getX() + size, (int) t.getY() + size);
            }
            gC.dispose();

            // explosion pulse
            if (manager.getClearSequenceStep() == 2 && manager.getCurrentTargetDestroyFramesLeft() > 0) {
                Graphics2D g3 = (Graphics2D) g2.create();
                float alpha = (float) manager.getCurrentTargetDestroyFramesLeft() / 2f;
                int pulseSize = 30;
                g3.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, alpha));
                g3.setColor(Color.WHITE);
                g3.fillOval(player.getX() + player.getWidth() / 2 - pulseSize / 2,
                        player.getY() + player.getHeight() / 2 - pulseSize / 2,
                        pulseSize, pulseSize);
                g3.dispose();
            }

        } else {
            for (Projectile p : manager.getProjectiles()) p.draw(g2);
        }

        // ================================
        // 3. Blink Ghosts
        // ================================
        if (!manager.getBlinkGhosts().isEmpty()) {
            Graphics2D gB = (Graphics2D) g2.create();
            for (GameManager.BlinkGhost ghost : manager.getBlinkGhosts()) {
                if (ghost.image != null) {
                    BufferedImage tinted = new BufferedImage(ghost.image.getWidth(), ghost.image.getHeight(), BufferedImage.TYPE_INT_ARGB);
                    Graphics2D tg = tinted.createGraphics();
                    tg.drawImage(ghost.image, 0, 0, null);

                    tg.setComposite(AlphaComposite.SrcAtop);
                    tg.setColor(Color.CYAN);
                    tg.fillRect(0, 0, tinted.getWidth(), tinted.getHeight());
                    tg.dispose();

                    gB.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, ghost.alpha));
                    gB.drawImage(tinted, ghost.x, ghost.y, player.getWidth(), player.getHeight(), null);
                }
            }
            gB.dispose();
        }

        // ================================
        // 4. Dash Ghost
        // ================================
        if (dashFramesLeft > 0 && !manager.isClearSkillActive()) {
            Graphics2D gD = (Graphics2D) g2.create();
            gD.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.35f));
            gD.drawImage(player.getCurrentFrame(),
                    player.getX() - dashVelocityX,
                    player.getY(),
                    player.getWidth(),
                    player.getHeight(),
                    null);
            gD.dispose();
        }

        // ================================
        // 5. Sandevistan Trail
        // ================================
        if (manager.isSandevistanActive() ||
                (manager.isClearSkillActive() && manager.getClearSequenceStep() == 1)) {

            Graphics2D gTrail = (Graphics2D) g2.create();
            float baseAlpha = manager.isClearSkillActive() ? 0.7f : 0.5f;
            gTrail.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, baseAlpha));

            BufferedImage[] trailFrames = manager.getTrailFrames();
            int[][] trailPositions = manager.getTrailPositions();
            int TRAIL_SIZE = manager.getTrailSize();

            for (int i = 0; i < TRAIL_SIZE; i++) {
                if (trailFrames[i] != null) {
                    BufferedImage tinted = new BufferedImage(trailFrames[i].getWidth(), trailFrames[i].getHeight(), BufferedImage.TYPE_INT_ARGB);
                    Graphics2D tg = tinted.createGraphics();

                    tg.drawImage(trailFrames[i], 0, 0, null);
                    tg.setComposite(AlphaComposite.SrcAtop.derive(0.7f));

                    Color tintColor = manager.isClearSkillActive() ? Color.MAGENTA : getRainbowColor(i);
                    tg.setColor(tintColor);
                    tg.fillRect(0, 0, tinted.getWidth(), tinted.getHeight());
                    tg.dispose();

                    gTrail.drawImage(tinted,
                            trailPositions[i][0],
                            trailPositions[i][1],
                            player.getWidth(),
                            player.getHeight(),
                            null);
                }
            }
            gTrail.dispose();
        }

        // ================================
        // 6. Shield
        // ================================
        if (manager.isShieldActive()) {
            Graphics2D gSh = (Graphics2D) g2.create();
            float shieldAlpha = 0.5f;

            gSh.setColor(new Color(0, 100, 255, (int) (255 * shieldAlpha)));
            gSh.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, shieldAlpha));
            gSh.setStroke(new BasicStroke(3));

            int shieldRadius = Math.max(player.getWidth(), player.getHeight()) / 2 + 15;
            int diameter = shieldRadius * 2;

            int sx = player.getX() + player.getWidth() / 2 - shieldRadius;
            int sy = player.getY() + player.getHeight() / 2 - shieldRadius;

            gSh.fillOval(sx, sy, diameter, diameter);
            gSh.setColor(new Color(0, 150, 255));
            gSh.drawOval(sx, sy, diameter, diameter);

            gSh.dispose();
        }

        // ================================
        // 7. Time Stop Flash
        // ================================
        if (manager.isTimeStopActive() && manager.getFlashingFramesLeft() > 0) {
            Graphics2D gTS = (Graphics2D) g2.create();
            float flashAlpha = (float) (0.5 + 0.5 * Math.sin(System.currentTimeMillis() / 20.0));

            gTS.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, flashAlpha));
            gTS.setColor(getRainbowColor(0));
            gTS.fillRect(20, 20, getWidth() - 40, getHeight() - 40);

            gTS.dispose();
        }

        // ================================
        // 8. GLOWING WINGS (draw behind player but after trails)
        // ================================
        if (reviveVFXFrames > 0) {
            Graphics2D gW = (Graphics2D) g2.create();
            float wingAlpha = Math.min(1f, reviveVFXFrames / 60f);
            gW.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.5f * wingAlpha));
            int cx = player.getX() + player.getWidth() / 2;
            int cy = player.getY() + player.getHeight() / 2;
            int wingW = player.getWidth() + 40;
            int wingH = player.getHeight() + 30;

            // Left wing
            gW.setColor(new Color(200, 100, 255, (int) (220 * wingAlpha)));
            int[] lx = new int[]{cx - 10, cx - wingW / 2, cx - wingW / 2 - 20};
            int[] ly = new int[]{cy - 10, cy - wingH / 2, cy + wingH / 3};
            gW.fillPolygon(lx, ly, 3);

            // Right wing
            gW.setColor(new Color(100, 200, 255, (int) (200 * wingAlpha)));
            int[] rx = new int[]{cx + 10, cx + wingW / 2, cx + wingW / 2 + 20};
            int[] ry = new int[]{cy - 10, cy - wingH / 2, cy + wingH / 3};
            gW.fillPolygon(rx, ry, 3);

            // soft glow overlay
            gW.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.2f * wingAlpha));
            gW.setColor(new Color(255, 255, 255, 60));
            gW.fillOval(cx - wingW, cy - wingH / 2, wingW * 2, wingH);

            gW.dispose();
        }

        // ================================
        // 9. PLAYER (ALWAYS ON TOP)
        // ================================
        Graphics2D gPlayer = (Graphics2D) g2.create();

        if (manager.isClearSkillActive()) {
            gPlayer.translate(player.getX() + player.getWidth() / 2,
                    player.getY() + player.getHeight() / 2);
            gPlayer.rotate(manager.getCurrentTiltAngle() + Math.PI / 2);
            gPlayer.translate(-(player.getX() + player.getWidth() / 2),
                    -(player.getY() + player.getHeight() / 2));
        }

        player.draw(gPlayer);
        gPlayer.dispose();

        // ================================
        // 10. PARTICLES (sparks + revive particles)
        // ================================
        // draw revive particles (soft, larger)
        Graphics2D gP = (Graphics2D) g2.create();
        for (ReviveParticle p : reviveParticles) {
            float alpha = Math.max(0f, p.life / 30f);
            gP.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, alpha));
            int s = 6 + (int) (6 * (1f - alpha));
            gP.setColor(new Color(180, 120, 255, (int) (200 * alpha)));
            gP.fillOval(p.x - s / 2, p.y - s / 2, s, s);
            // soft trail
            gP.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, alpha * 0.6f));
            gP.setColor(new Color(255, 255, 255, (int) (100 * alpha)));
            gP.fillOval(p.x - s, p.y - s, s * 2, s * 2);
        }

        // draw spark particles (bright, tiny)
        for (SparkParticle sp : sparkParticles) {
            float alpha = Math.max(0f, sp.life / 20f);
            gP.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, alpha));
            int s = 2 + (int) (3 * (1f - alpha));
            gP.setColor(sp.colorWithAlpha(alpha));
            gP.fillOval(sp.x - s / 2, sp.y - s / 2, s, s);
        }
        gP.dispose();

        // ================================
        // 11. REVIVE BEAM + EXPLOSION (overlay effect)
        // ================================
        if (reviveVFXFrames > 0) {
            Graphics2D gVFX = (Graphics2D) g2.create();
            float progress = (float) reviveVFXFrames / 60f; // 1 -> 0
            // Beam
            int beamX = player.getX() + player.getWidth() / 2;
            int beamTop = 20;
            int beamBottom = player.getY() + player.getHeight() / 2;
            gVFX.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.7f * progress));
            GradientPaint gp = new GradientPaint(beamX, beamTop, new Color(255, 255, 255, (int) (255 * progress)),
                    beamX, beamBottom, new Color(150, 100, 255, 0));
            gVFX.setPaint(gp);
            gVFX.fillRect(beamX - 8, beamTop, 16, beamBottom - beamTop);

            // pulsing circle around player
            int maxR = 120;
            int r = (int) (maxR * (1f - progress));
            gVFX.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, (1f - progress) * 0.9f));
            gVFX.setColor(new Color(200, 200, 255, (int) (180 * (1f - progress))));
            gVFX.fillOval(player.getX() + player.getWidth() / 2 - r / 2, player.getY() + player.getHeight() / 2 - r / 2, r, r);

            gVFX.dispose();

            reviveVFXFrames--;
            // ensure slowMotionFrames already decremented in update()
        }

        // ================================
        // 12. HEARTS (UI ALWAYS ABOVE PLAYER)
        // ================================
        if (heartIcon != null) {
            int hearts = player.getLives();
            int size = 35;
            int spacing = 8;
            int x = 25;
            int y = 25;

            // Draw current hearts
            for (int i = 0; i < hearts; i++) {
                g2.drawImage(heartIcon,
                        x + i * (size + spacing),
                        y,
                        size,
                        size,
                        null);
            }

            // Draw animating lost heart if animation is active
            if (heartLossAnimationFrames > 0) {
                int lostHeartIndex = hearts; // The heart that was lost is at the position after current hearts
                int hx = x + lostHeartIndex * (size + spacing);
                int hy = y;

                Graphics2D gInv = (Graphics2D) g2.create();
                gInv.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.3f));
                gInv.setColor(Color.YELLOW);
                int glowSize = 10;
                gInv.fillOval(player.getX() - glowSize, player.getY() - glowSize, player.getWidth() + 2 * glowSize, player.getHeight() + 2 * glowSize);
                gInv.dispose();

                // Create a tinted version for flashing effect
                BufferedImage tintedHeart = new BufferedImage(heartIcon.getWidth(), heartIcon.getHeight(), BufferedImage.TYPE_INT_ARGB);
                Graphics2D tg = tintedHeart.createGraphics();
                tg.drawImage(heartIcon, 0, 0, null);
                tg.setComposite(AlphaComposite.SrcAtop);
                tg.setColor(Color.RED); // Tint red for damage effect
                tg.fillRect(0, 0, tintedHeart.getWidth(), tintedHeart.getHeight());
                tg.dispose();

                // Flash effect: alternate between normal and tinted every few frames
                if ((heartLossAnimationFrames / 5) % 2 == 0) {
                    g2.drawImage(tintedHeart, hx, hy, size, size, null);
                } else {
                    g2.drawImage(heartIcon, hx, hy, size, size, null);
                }
            }

            // Draw reviving heart if animation is active (a small green flash)
            if (reviveVFXFrames > 0) {
                int reviveHeartIndex = Math.max(0, player.getLives() - 1);
                int hx = x + reviveHeartIndex * (size + spacing);
                int hy = y;

                BufferedImage tintedHeart = new BufferedImage(heartIcon.getWidth(), heartIcon.getHeight(), BufferedImage.TYPE_INT_ARGB);
                Graphics2D tg = tintedHeart.createGraphics();
                tg.drawImage(heartIcon, 0, 0, null);
                tg.setComposite(AlphaComposite.SrcAtop);
                tg.setColor(new Color(120, 255, 120)); // green tint
                tg.fillRect(0, 0, tintedHeart.getWidth(), tintedHeart.getHeight());
                tg.dispose();

                if ((reviveVFXFrames / 5) % 2 == 0) {
                    g2.drawImage(tintedHeart, hx, hy, size, size, null);
                } else {
                    g2.drawImage(heartIcon, hx, hy, size, size, null);
                }
            }
        }

        // dispose the top-level g2 copy
        g2.dispose();
    }

    // ================================
    // Helpers & inner particle classes
    // ================================
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
        if (player.getX() + player.getWidth() > arenaRight) player.setX(arenaRight - player.getWidth());
        if (player.getY() < arenaTop) player.setY(arenaTop);
        if (player.getY() + player.getHeight() > arenaBottom) player.setY(arenaBottom - player.getHeight());
    }

    // --- UI Setup (same as before) ---
    private void setupMenuButton() {
        menuButton = new JButton() {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                Graphics2D g2 = (Graphics2D) g;
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_OFF);
                g2.setColor(Color.GREEN);
                int w = getWidth(), h = getHeight();
                g2.fillRect(5, h / 4, w - 10, 4);
                g2.fillRect(5, h / 2 - 2, w - 10, 4);
                g2.fillRect(5, (h / 4) * 3 - 4, w - 10, 4);
            }
        };
        menuButton.setBorder(BorderFactory.createLineBorder(Color.GREEN));
        menuButton.setFocusPainted(false);
        menuButton.setContentAreaFilled(false);
        menuButton.setOpaque(false);
        menuButton.setFocusable(false);
        menuButton.addActionListener(e -> { if (gamePaused) closePauseOverlay(); else openPauseOverlay(); });
        add(menuButton);
        positionMenuButton();
    }

    private void positionMenuButton() {
        int bx = getWidth() - menuButtonXOffset;
        if (bx < 30) bx = getWidth() - 40;
        int by = menuButtonYOffset;
        menuButton.setBounds(bx, by, 40, 40);
        menuButton.revalidate();
        menuButton.repaint();
        setComponentZOrder(menuButton, 0);
    }

    private void openPauseOverlay() {
        if (pauseOverlay != null) return;
        gamePaused = true;
        pauseOverlay = new JPanel(null);
        pauseOverlay.setBackground(new Color(0, 0, 0, 180));
        pauseOverlay.setOpaque(true);

        JButton resumeBtn = new JButton("RESUME");
        resumeBtn.setFont(new Font("VT323", Font.BOLD, 28));
        resumeBtn.setForeground(Color.GREEN);
        resumeBtn.setBackground(Color.BLACK);
        resumeBtn.setBorder(BorderFactory.createLineBorder(Color.GREEN));
        resumeBtn.setFocusPainted(false);
        resumeBtn.setContentAreaFilled(false);
        resumeBtn.addActionListener(e -> closePauseOverlay());

        JButton quitBtn = new JButton("QUIT");
        quitBtn.setFont(new Font("VT323", Font.BOLD, 28));
        quitBtn.setForeground(Color.RED);
        quitBtn.setBackground(Color.BLACK);
        quitBtn.setBorder(BorderFactory.createLineBorder(Color.GREEN));
        quitBtn.setFocusPainted(false);
        quitBtn.setContentAreaFilled(false);
        quitBtn.addActionListener(e -> { closePauseOverlay(); goToHomeScreen(); });

        pauseOverlay.add(resumeBtn);
        pauseOverlay.add(quitBtn);

        add(pauseOverlay);
        setComponentZOrder(pauseOverlay, 0);

        positionPauseOverlay();

        repaint();

        SwingUtilities.invokeLater(() -> {
            positionPauseOverlay();
            requestFocusInWindow();
        });
    }

    private void positionPauseOverlay() {
        if (pauseOverlay == null) return;
        int x = 19, y = 19;
        int w = getWidth() - 38;
        int h = getHeight() - 38;
        pauseOverlay.setBounds(x,y,w,h);
        Component[] comps = pauseOverlay.getComponents();
        if (comps.length >= 2) {
            comps[0].setBounds((pauseOverlay.getWidth() - 200) / 2, 180, 200, 50);
            comps[1].setBounds((pauseOverlay.getWidth() - 200) / 2, 260, 200, 50);
        }
        pauseOverlay.revalidate();
        pauseOverlay.repaint();
    }

    private void closePauseOverlay() {
        if (pauseOverlay != null) { remove(pauseOverlay); pauseOverlay = null; }
        gamePaused = false;
        SwingUtilities.invokeLater(() -> { requestFocusInWindow(); repaint(); });
    }

    private void goToHomeScreen() {
        JFrame frame = (JFrame) SwingUtilities.getWindowAncestor(this);
        frame.getContentPane().removeAll();
        frame.getContentPane().add(new HomeScreen(new SoundManager()));
        frame.revalidate();
        frame.repaint();
    }

    // ---------------------------
    // Particle classes
    // ---------------------------
    private static class ReviveParticle {
        int x, y;
        double vx, vy;
        int life = 28;

        ReviveParticle(int startX, int startY) {
            x = startX;
            y = startY;
            double angle = Math.random() * Math.PI * 2;
            double speed = 1.5 + Math.random() * 2.8;
            vx = Math.cos(angle) * speed;
            vy = Math.sin(angle) * speed - (0.5 + Math.random()); // little upward bias
        }

        void update() {
            x += vx;
            y += vy;
            // subtle gravity
            vy += 0.06;
            life--;
        }
    }

    private static class SparkParticle {
        int x, y;
        double vx, vy;
        int life = 20;
        Color base;

        SparkParticle(int startX, int startY) {
            x = startX;
            y = startY;
            double angle = Math.random() * Math.PI * 2;
            double speed = 2 + Math.random() * 4.5;
            vx = Math.cos(angle) * speed;
            vy = Math.sin(angle) * speed;
            base = randomSparkColor();
        }

        void update() {
            x += vx;
            y += vy;
            // friction
            vx *= 0.96;
            vy *= 0.96;
            // gravity
            vy += 0.08;
            life--;
        }

        Color colorWithAlpha(float alpha) {
            int a = Math.max(0, Math.min(255, (int) (255 * alpha)));
            return new Color(base.getRed(), base.getGreen(), base.getBlue(), a);
        }

        private static Color randomSparkColor() {
            int pick = (int) (Math.random() * 3);
            return switch (pick) {
                case 0 -> new Color(255, 200, 60);
                case 1 -> new Color(255, 120, 255);
                default -> new Color(120, 220, 255);
            };
        }
    }
}
