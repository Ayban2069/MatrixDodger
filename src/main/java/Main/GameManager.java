package Main;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class GameManager {
    private Player player;
    private List<Projectile> projectiles;
    private List<LifePack> lifePacks;
    private int score;
    private Random random;

    public GameManager() {
        player = new Player(250, 250, 32, 32, 5, 3); // 3 lives start
        projectiles = new ArrayList<>();
        lifePacks = new ArrayList<>();
        score = 0;
        random = new Random();
    }

    public void spawnProjectile() {
        int y = random.nextInt(500); // screen height = 500
        projectiles.add(new Projectile(0, y, 3, 0, 16)); // fixed constructor
    }

    public void spawnLifePack() {
        int x = random.nextInt(480); // inside box
        int y = random.nextInt(480);
        lifePacks.add(new LifePack(x, y, 20));
    }

    public void update() {
        List<Projectile> toRemove = new ArrayList<>();
        List<LifePack> lifeToRemove = new ArrayList<>();

        // Update projectiles
for (Projectile p : projectiles) {
    p.setSlowed(false);              // no slowdown here
    p.update(500, 500);              // move + bounce
    if (p.shouldRemove()) {
        toRemove.add(p);
    }
    if (player.collidesWith(p)) {
        player.loseLife();
        toRemove.add(p);
    }
}


        // Check for life pack collection
        for (LifePack lp : lifePacks) {
            if (player.collidesWith(lp)) {
                player.gainLife();
                lifeToRemove.add(lp);
            }
        }

        projectiles.removeAll(toRemove);
        lifePacks.removeAll(lifeToRemove);
    }

    // Getters
    public Player getPlayer() { return player; }
    public int getScore() { return score; }
    public List<Projectile> getProjectiles() { return projectiles; }
    public List<LifePack> getLifePacks() { return lifePacks; }
}
