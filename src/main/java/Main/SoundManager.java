package Main;

import javax.sound.sampled.*;
import java.io.*;
import java.util.HashMap;
import java.util.Map;

public class SoundManager {
    private Map<String, Clip> soundCache;
    private Clip backgroundMusic;
    
    public SoundManager() {
        soundCache = new HashMap<>();
        preloadSounds();
    }
    
    private void preloadSounds() {
        // Preload frequently used sounds
        preloadSound("select", "sounds/select.wav");
        preloadSound("explosion", "sounds/explosion.wav");
        preloadSound("powerup", "sounds/powerup.wav");
    }
    
    private void preloadSound(String name, String filename) {
        try {
            File soundFile = new File(filename);
            AudioInputStream audioIn = AudioSystem.getAudioInputStream(soundFile);
            Clip clip = AudioSystem.getClip();
            clip.open(audioIn);
            soundCache.put(name, clip);
        } catch (Exception e) {
            System.err.println("Could not load sound: " + filename);
        }
    }
    
    public void playSound(String name) {
        Clip clip = soundCache.get(name);
        if (clip != null) {
            // Reset clip to beginning if it's still playing
            if (clip.isRunning()) {
                clip.stop();
            }
            clip.setFramePosition(0);
            clip.start();
        }
    }
    
    public void playBackgroundMusic(String filename) {
        try {
            if (backgroundMusic != null && backgroundMusic.isRunning()) {
                backgroundMusic.stop();
            }
            
            File musicFile = new File(filename);
            AudioInputStream audioIn = AudioSystem.getAudioInputStream(musicFile);
            backgroundMusic = AudioSystem.getClip();
            backgroundMusic.open(audioIn);
            backgroundMusic.loop(Clip.LOOP_CONTINUOUSLY);
        } catch (Exception e) {
            System.err.println("Could not load background music: " + filename);
        }
    }
    
    public void stopBackgroundMusic() {
        if (backgroundMusic != null && backgroundMusic.isRunning()) {
            backgroundMusic.stop();
        }
    }
    
    public void setVolume(String name, float volume) {
        Clip clip = soundCache.get(name);
        if (clip != null) {
            FloatControl gainControl = (FloatControl) clip.getControl(FloatControl.Type.MASTER_GAIN);
            gainControl.setValue(volume); // Reduce volume by 10 decibels
        }
    }
}