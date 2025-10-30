package Main;

import javax.swing.*;
import java.awt.*;
import java.io.File;
import java.util.*;
import java.util.List;
import javax.swing.Timer;

public class SpriteAnimator extends JPanel {
    private List<ImageIcon> frames = new ArrayList<>();
    private int currentFrame = 0;
    private Timer timer;
    private boolean loop = true;

    public SpriteAnimator(String folderPath, int frameDelay, boolean autoStart) {
        setOpaque(false);
        loadFrames(folderPath);

        timer = new Timer(frameDelay, e -> {
            if (!frames.isEmpty()) {
                currentFrame++;
                if (currentFrame >= frames.size()) {
                    if (loop) currentFrame = 0;
                    else {
                        currentFrame = frames.size() - 1;
                        timer.stop();
                    }
                }
                repaint();
            }
        });

        if (autoStart) timer.start();
    }

    // 🧩 Load all frames from folder
    private void loadFrames(String folderPath) {
        frames.clear();
        File folder = new File(folderPath);
        File[] files = folder.listFiles((dir, name) ->
                name.toLowerCase().endsWith(".png") ||
                name.toLowerCase().endsWith(".jpg") ||
                name.toLowerCase().endsWith(".gif"));

        if (files != null) {
            Arrays.sort(files); // ensure correct order
            for (File f : files) {
                frames.add(new ImageIcon(f.getAbsolutePath()));
            }
        } else {
            System.err.println("No images found in " + folderPath);
        }
    }

    // 🔄 Change skin on the fly
    public void setSkin(String folderPath) {
        loadFrames(folderPath);
        currentFrame = 0;
        repaint();
    }

    // ▶️ / ⏸ Controls
    public void start() { timer.start(); }
    public void stop() { timer.stop(); }
    public void setLoop(boolean loop) { this.loop = loop; }
    public void setSpeed(int delay) { timer.setDelay(delay); }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        if (!frames.isEmpty()) {
            Image img = frames.get(currentFrame).getImage();
            g.drawImage(img, 0, 0, getWidth(), getHeight(), this);
        }
    }
}