package Main;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.util.*;
import java.util.List;

public class GameManager {

    public enum SkillType {
        SANDEVISTAN,
        TIME_STOP,
        BLINK,
        SHIELD,
        CLEAR_SCREEN,
        REVIVE,
        NONE
    }

    // Player (GameManager owns the authoritative player)
    private Player player;

    // Game objects
    private List<Projectile> projectiles;
    private List<LifePack> lifePacks;

    // Equipped slots
    private SkillType slotR = SkillType.NONE;
    private SkillType slotF = SkillType.NONE;
    private SkillType slotV = SkillType.NONE;

    // Cooldowns - last used timestamps
    private long lastUsedSandevistan = 0;
    private long lastUsedTimeStop = 0;
    private long lastUsedBlink = 0;
    private long lastUsedShield = 0;
    private long lastUsedClearScreen = 0;
    private long lastUsedRevive = 0;

    // Cooldown durations (ms) - sensible defaults
    public long cdSandevistan = 10_000; // 10s
    public long cdTimeStop = 12_000; // 12s
    public long cdBlink = 7_000; // 7s
    public long cdShield = 8_000; // 8s
    public long cdClear = 20_000; // 20s
    public long cdRevive = 120_000; // 2 minutes

    // Skill active flags & timers (frames)
    private boolean sandevistanActive = false;
    private int sandevistanFramesLeft = 0;
    private int sandevistanDuration = 200;

    private boolean timeStopActive = false;
    private int timeStopFramesLeft = 0;
    private int timeStopDuration = 300;
    private int flashingFramesLeft = 0;
    private int flashingDuration = 50;

    private boolean shieldActive = false;
    private int shieldFramesLeft = 0;
    private int shieldDuration = 500;

    private boolean reviveAvailable = true;
    public boolean revivedThisFrame = false;

    // Revive temporary invincibility (frames)
    private final int REVIVE_INVINCIBLE_FRAMES = 180; // ~3s @60fps-ish
    private int reviveInvincibleFramesLeft = 0;

    // Clear (ultimate) cinematic state
    private boolean clearSkillActive = false;
    private int clearSkillSequenceStep = 0;
    private List<Projectile> targetsToClear = new ArrayList<>();
    private int clearSkillTargetIndex = 0;
    private long sequenceTimeElapsed = 0;
    private final int MOVEMENT_DURATION = 3;
    private final int DESTROY_DURATION = 2;
    private double currentTiltAngle = 0;
    private int zigzagAmplitude = 10;
    private float zigzagFrequency = 0.5f;
    private int currentTargetDestroyFramesLeft = 0;
    private int initialX, initialY; // used for cinematic movement

    // Blink ghosts (visual effect)
    private List<BlinkGhost> blinkGhosts = new ArrayList<>();

    // Trail for sandevistan (store sprite frames)
    private final int TRAIL_SIZE = 12;
    private BufferedImage[] trailFrames = new BufferedImage[TRAIL_SIZE];
    private int[][] trailPositions = new int[TRAIL_SIZE][2];
    private int trailIndex = 0;

    // Spawning and difficulty
    private Random random = new Random();
    private int spawnTimer = 0;
    private long startTime = System.currentTimeMillis();
    private int difficultyLevel = 1;

    SoundManager soundManager = new SoundManager();

    public GameManager() {
        // Instantiate a Player that matches the visual size used in the arena (64x64)
        player = new Player(250, 100, 64, 64, 5, 3);

        projectiles = new ArrayList<>();
        lifePacks = new ArrayList<>();
    }

    // ===============================
    // EQUIP SLOTS
    // ===============================
    public void equipSkillToR(SkillType skill) { slotR = skill; }
    public void equipSkillToF(SkillType skill) { slotF = skill; }
    public void equipSkillToV(SkillType skill) { slotV = skill; }

    public SkillType getSkillR() { return slotR; }
    public SkillType getSkillF() { return slotF; }
    public SkillType getSkillV() { return slotV; }

    // ===============================
    // CAN USE / MARK USED
    // ===============================
    public boolean canUse(SkillType s) {
        long now = System.currentTimeMillis();
        return switch (s) {
            case SANDEVISTAN -> now - lastUsedSandevistan > cdSandevistan;
            case TIME_STOP -> now - lastUsedTimeStop > cdTimeStop;
            case BLINK -> now - lastUsedBlink > cdBlink;
            case SHIELD -> now - lastUsedShield > cdShield;
            case CLEAR_SCREEN -> now - lastUsedClearScreen > cdClear;
            case REVIVE -> now - lastUsedRevive > cdRevive && reviveAvailable;
            default -> false;
        };
    }

    private void markUsed(SkillType s) {
        long now = System.currentTimeMillis();
        switch (s) {
            case SANDEVISTAN -> lastUsedSandevistan = now;
            case TIME_STOP -> lastUsedTimeStop = now;
            case BLINK -> lastUsedBlink = now;
            case SHIELD -> lastUsedShield = now;
            case CLEAR_SCREEN -> lastUsedClearScreen = now;
            case REVIVE -> { lastUsedRevive = now; reviveAvailable = false; }
        }
    }

    // Tries to use the skill; returns true if skill activated
    public boolean tryUseSkill(SkillType s) {
        if (s == SkillType.NONE) return false;
        if (!canUse(s)) return false;

        switch (s) {
            case SANDEVISTAN -> activateSandevistan();
            case TIME_STOP -> activateTimeStop();
            case BLINK -> blink();
            case SHIELD -> activateShield();
            case CLEAR_SCREEN -> activateClearSkill();
            default -> {}
        }
        markUsed(s);
        return true;
    }

    // ===============================
    // Skill implementations (moved from arena)
    // ===============================
    private void activateSandevistan() {
        sandevistanActive = true;
        sandevistanFramesLeft = sandevistanDuration;
        soundManager.playSound("sandevistan");
    }

    private void activateTimeStop() {
        timeStopActive = true;
        timeStopFramesLeft = timeStopDuration;
        flashingFramesLeft = flashingDuration;
        soundManager.playSound("timestop");
    }

    private void activateShield() {
        shieldActive = true;
        shieldFramesLeft = shieldDuration;
    }

    private void blink() {
        int startX = player.getX();
        int startY = player.getY();

        int dir = player.isFacingLeft() ? -1 : 1;
        player.setX(player.getX() + (dir * 500));
        player.setY(player.getY()); // keep Y
        player.setInvincible(false); // not invincible by default

        // create simple ghost frames (use player's current frame)
        BufferedImage sprite = player.getCurrentFrame();
        int steps = 10;
        for (int i = 0; i < steps; i++) {
            float ratio = (float) i / (float) (steps - 1);
            int ghostX = (int) (startX + (player.getX() - startX) * ratio);
            int ghostY = (int) (startY + (player.getY() - startY) * ratio);
            blinkGhosts.add(new BlinkGhost(ghostX, ghostY, sprite, 0.8f));
        }
        soundManager.playSound("blink");
    }

    private void activateClearSkill() {
        clearSkillActive = true;
        targetsToClear.clear();
        targetsToClear.addAll(projectiles);
        projectiles.clear();

        clearSkillSequenceStep = 1;
        clearSkillTargetIndex = 0;
        sequenceTimeElapsed = 0;
        initialX = player.getX();
        initialY = player.getY();

        if (!targetsToClear.isEmpty()) {
            // nothing extra needed here
        } else {
            clearSkillActive = false;
        }
    }

    // ===============================
    // Spawn / Update / Cleanup
    // ===============================
    public void spawnProjectile(int arenaW, int arenaH) {
        int left = 20, right = arenaW - 20;
        int top = 20, bottom = arenaH - 20;
        if (right - left < 10 || bottom - top < 10) return;

        int side = random.nextInt(3);
        double x = 0, y = 0, sx = 0, sy = 0;

        double baseSpeed = 3.0 + (double) difficultyLevel / 2.0;
        double speedX = baseSpeed * 0.75;
        double speedY = baseSpeed;

        switch (side) {
            case 0 -> {
                x = left + random.nextInt(Math.max(1, right - left));
                y = top;
                sx = random.nextBoolean() ? speedX : -speedX;
                sy = speedY;
            }
            case 1 -> {
                x = left;
                y = top + random.nextInt(Math.max(1, bottom - top));
                sx = speedY;
                sy = random.nextBoolean() ? speedX : -speedX;
            }
            default -> {
                x = right - 10;
                y = top + random.nextInt(Math.max(1, bottom - top));
                sx = -speedY;
                sy = random.nextBoolean() ? speedX : -speedX;
            }
        }
        projectiles.add(new Projectile(x,y,sx,sy,16));
    }

    /**
     * Update method called every tick from GameArena.
     * This updates difficulty, skill timers, projectiles, spawn logic, blink ghosts, etc.
     */
    public void update(int arenaW, int arenaH) {
        long elapsedSeconds = (System.currentTimeMillis() - startTime) / 1000;
        difficultyLevel = 1 + (int) (elapsedSeconds / 20);
        long now = System.currentTimeMillis();
        player.updateInvincibility();
        revivedThisFrame = false;



        if (!reviveAvailable && now - lastUsedRevive > cdRevive) {
            reviveAvailable = true;
        }

        // Revive invincibility countdown
        if (reviveInvincibleFramesLeft > 0) {
            reviveInvincibleFramesLeft--;
            if (reviveInvincibleFramesLeft <= 0) {
                player.setInvincible(false);
            }
        }

        // Skill timers
        if (timeStopActive) {
            timeStopFramesLeft--;
            if (timeStopFramesLeft <= 0) timeStopActive = false;
            if (flashingFramesLeft > 0) flashingFramesLeft--;
        }

        if (sandevistanActive) {
            sandevistanFramesLeft--;
            if (sandevistanFramesLeft <= 0) sandevistanActive = false;
        }

        if (shieldActive) {
            shieldFramesLeft--;
            if (shieldFramesLeft <= 0) shieldActive = false;
        }

        // Blink ghosts fade
        Iterator<BlinkGhost> git = blinkGhosts.iterator();
        while (git.hasNext()) {
            BlinkGhost g = git.next();
            g.alpha -= 0.03f;
            if (g.alpha <= 0) git.remove();
        }

        // Spawn projectiles (respect time stop)
        if (!timeStopActive) {
            spawnTimer++;
            int spawnInterval = Math.max(10, 60 - (difficultyLevel * 5));
            if (spawnTimer > spawnInterval) {
                spawnProjectile(arenaW, arenaH);
                spawnTimer = 0;
            }
        }



// Update projectiles and handle collisions with player (AUTO-REVIVE here)
Iterator<Projectile> it = projectiles.iterator();
while (it.hasNext()) {
    Projectile p = it.next();
    if (!timeStopActive) {
        p.setSlowed(sandevistanActive);
        p.update(arenaW, arenaH);

        // Shield intercepts first
        if (shieldActive && checkShieldCollision(p)) {
            it.remove();
            continue;
        }

        // If player is currently invincible, skip collision (projectile still updates)
        if (player.isInvincible()) {
            if (p.shouldRemove()) it.remove();
            continue;
        }

        // Collision with player
        if (player.collidesWith(p)) {
            // remove projectile
            it.remove();

            // Always take damage if lives > 0
            if (player.getLives() > 0) {
                player.loseLife();
                soundManager.playSound("hit");

                // Check if player just died (lives now 0) and try auto-revive
                if (player.getLives() == 0) {
                    boolean hasAutoReviveEquipped =
                            slotR == SkillType.REVIVE ||
                            slotF == SkillType.REVIVE ||
                            slotV == SkillType.REVIVE;

                    if (hasAutoReviveEquipped && canUse(SkillType.REVIVE)) {
                        // Trigger auto-revive: restore 1 life and give temporary invincibility
                        player.setLives(1);  // Assuming Player has setLives(int) method
                        player.setInvincible(true);
                        reviveInvincibleFramesLeft = REVIVE_INVINCIBLE_FRAMES;
                        markUsed(SkillType.REVIVE);
                        soundManager.playSound("revive");  // Use appropriate sound asset
                    } else {
                        // No revive available -> player dies
                        soundManager.playSound("final_hit");
                    }
                }
            }
            // continue to next projectile
            continue;
        }
    }
    if (p.shouldRemove()) it.remove();
}

// Remove the unused tryAutoRevive and useReviveSkill methods from GameManager


        // Clear-skill cinematic
        if (clearSkillActive) {
            runCinematicClearSequence();
        }

        // store trail frame for sandevistan visuals
        storeTrailFrame();
    }

    private void runCinematicClearSequence() {
        sequenceTimeElapsed++;
        if (clearSkillTargetIndex >= targetsToClear.size()) {
            clearSkillSequenceStep = 3;
        }

        switch (clearSkillSequenceStep) {
            case 1 -> {
                if (clearSkillTargetIndex < targetsToClear.size()) {
                    Projectile target = targetsToClear.get(clearSkillTargetIndex);

                    int startX = initialX;
                    int startY = initialY;
                    int endX = (int) target.getX() + target.getSize()/2 - player.getWidth()/2;
                    int endY = (int) target.getY() + target.getSize()/2 - player.getHeight()/2;

                    float ratio = Math.min(1.0f, (float)(sequenceTimeElapsed) / MOVEMENT_DURATION);

                    int straightX = (int) (startX + (endX - startX) * ratio);
                    int straightY = (int) (startY + (endY - startY) * ratio);

                    double deltaX = endX - startX;
                    double deltaY = endY - startY;
                    currentTiltAngle = Math.atan2(deltaY, deltaX);

                    double perpendicularAngle = currentTiltAngle + Math.PI/2;
                    double oscillation = Math.sin(sequenceTimeElapsed * zigzagFrequency * 2 * Math.PI / MOVEMENT_DURATION);
                    int offsetX = (int) (Math.cos(perpendicularAngle) * oscillation * zigzagAmplitude);
                    int offsetY = (int) (Math.sin(perpendicularAngle) * oscillation * zigzagAmplitude);

                    int currentX = straightX + offsetX;
                    int currentY = straightY + offsetY;

                    player.setX(currentX);
                    player.setY(currentY);

                    storeTrailFrame();

                    if (ratio >= 1.0f) {
                        clearSkillSequenceStep = 2;
                        sequenceTimeElapsed = 0;
                        currentTargetDestroyFramesLeft = DESTROY_DURATION;
                        currentTiltAngle = 0;
                    }
                } else {
                    clearSkillSequenceStep = 3;
                }
            }
            case 2 -> {
                currentTargetDestroyFramesLeft--;
                if (currentTargetDestroyFramesLeft <= 0) {
                    initialX = player.getX();
                    initialY = player.getY();
                    clearSkillTargetIndex++;
                    sequenceTimeElapsed = 0;
                    if (clearSkillTargetIndex < targetsToClear.size()) {
                        clearSkillSequenceStep = 1;
                    } else {
                        clearSkillSequenceStep = 3;
                    }
                }
            }
            case 3 -> {
                clearSkillActive = false;
                clearSkillSequenceStep = 0;
            }
        }
    }
    
    
    public boolean tryAutoRevive(Player p) {
    // Check if revive is equipped and ready
    if (slotR == SkillType.REVIVE || 
        slotF == SkillType.REVIVE || 
        slotV == SkillType.REVIVE) {

        if (canUse(SkillType.REVIVE)) {

            markUsed(SkillType.REVIVE);
            soundManager.playSound("revive");

            // revive with 1 life
            p.gainLife();
            p.setInvincible(true); 
            return true;
        }
    }
    return false;
}

    public void useReviveSkill(Player p) {
    System.out.println("⚡ AUTO REVIVE ACTIVATED!");

    p.setInvincible(true);
    markUsed(SkillType.REVIVE);
    soundManager.playSound("revive");

    // revive with 1 life
    p.gainLife();

    // blinking invincibility
    p.setInvincible(true);
    }


    // Shield collision helper (same maths as arena used previously)
    private boolean checkShieldCollision(Projectile p) {
        int shieldCenterX = player.getX() + player.getWidth()/2;
        int shieldCenterY = player.getY() + player.getHeight()/2;

        int shieldRadius = Math.max(player.getWidth(), player.getHeight())/2 + 15;
        double dist = Math.sqrt(Math.pow(p.getX() + p.getSize()/2 - shieldCenterX, 2)
                + Math.pow(p.getY() + p.getSize()/2 - shieldCenterY, 2));
        return dist < (shieldRadius + p.getSize()/2);
    }

    // Trail storage (for sandevistan)
    private void storeTrailFrame() {
        BufferedImage frame = player.getCurrentFrame();
        trailFrames[trailIndex] = frame;
        trailPositions[trailIndex][0] = player.getX();
        trailPositions[trailIndex][1] = player.getY();
        trailIndex = (trailIndex + 1) % TRAIL_SIZE;
    }
    
    private void triggerRevive() {
    player.setLives(player.getLives() + 1);
    revivedThisFrame = true;
    }

    // ===============================
    // Getters used by GameArena drawing
    // ===============================
    public Player getPlayer() { return player; }
    public List<Projectile> getProjectiles() { return projectiles; }
    public List<BlinkGhost> getBlinkGhosts() { return blinkGhosts; }
    public BufferedImage[] getTrailFrames() { return trailFrames; }
    public int[][] getTrailPositions() { return trailPositions; }
    public boolean isSandevistanActive() { return sandevistanActive; }
    public boolean isTimeStopActive() { return timeStopActive; }
    public boolean isShieldActive() { return shieldActive; }
    public boolean isClearSkillActive() { return clearSkillActive; }
    public int getSandevistanFramesLeft() { return sandevistanFramesLeft; }
    public int getTimeStopFramesLeft() { return timeStopFramesLeft; }
    public int getFlashingFramesLeft() { return flashingFramesLeft; }
    public double getCurrentTiltAngle() { return currentTiltAngle; }
    public int getClearSequenceStep() { return clearSkillSequenceStep; }
    public List<Projectile> getClearTargets() { return targetsToClear; }
    public int getClearTargetIndex() { return clearSkillTargetIndex; }
    public int getCurrentTargetDestroyFramesLeft() { return currentTargetDestroyFramesLeft; }
    public int getTrailSize() { return TRAIL_SIZE; }
    public boolean isReviveAvailable() { return reviveAvailable; }

    // Inner class used for blink ghosts
    public static class BlinkGhost {
        public int x, y;
        public BufferedImage image;
        public float alpha;
        public BlinkGhost(int x, int y, BufferedImage image, float alpha) {
            this.x = x; this.y = y; this.image = image; this.alpha = alpha;
        }
    }
}
