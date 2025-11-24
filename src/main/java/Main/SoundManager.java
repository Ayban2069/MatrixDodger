package Main;

import javax.sound.sampled.*;
import java.io.IOException;
import java.net.URL;
import java.util.HashMap;

public class SoundManager {

    private HashMap<String, Clip> soundMap = new HashMap<>();

    public SoundManager() {
        load("jump", "/sfx/retro-jump-1-236684.wav");
        load("hit", "/sfx/retro-explode-1-236678.wav");
        load("finalHit", "/sfx/retro-explode-2-236688.wav");
        load("select", "/sfx/retro-select-236670.wav");
        load("blink", "/sfx/teleport.wav");
        load("sandevistan", "/sfx/sandevistan.wav");
        load("timestop", "/sfx/timestop.wav");
    }

    private void load(String key, String path) {
        try {
            URL resource = getClass().getResource(path);
            if (resource == null) {
                System.err.println("❌ Sound not found: " + path);
                return;
            }

            AudioInputStream audioIn = AudioSystem.getAudioInputStream(resource);
            Clip clip = AudioSystem.getClip();
            clip.open(audioIn);

            soundMap.put(key, clip);

        } catch (UnsupportedAudioFileException | IOException | LineUnavailableException e) {
            System.err.println("❌ Error loading sound: " + path);
            e.printStackTrace();
        }
    }

    public void playSound(String key) {
        Clip clip = soundMap.get(key);
        if (clip == null) return;

        if (clip.isRunning()) {
            clip.stop();     // restart
        }
        clip.setFramePosition(0);
        clip.start();
    }
}
