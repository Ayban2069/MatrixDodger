package Main;

import javax.swing.*;
import java.awt.*;

public class Main {
    private static SoundManager soundManager = new SoundManager();
    
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            JFrame frame = new JFrame("Matrix Dodger");
            frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            frame.setResizable(false);
            
            // Create card layout for screen switching
            CardLayout cardLayout = new CardLayout();
            JPanel mainPanel = new JPanel(cardLayout);
            
            MatrixDodgerTitleScreen titleScreen = new MatrixDodgerTitleScreen();
            HomeScreen homeScreen = new HomeScreen(soundManager);
            
            mainPanel.add(titleScreen, "title");
            mainPanel.add(homeScreen, "home");
            
            frame.add(mainPanel);
            frame.pack();
            frame.setLocationRelativeTo(null);
            frame.setVisible(true);
            
            // Set up the game start callback for title screen
            titleScreen.setOnGameStart(() -> {
                cardLayout.show(mainPanel, "home");
                soundManager.playSound("select");
                // You can add background music here if needed
                // soundManager.playBackgroundMusic("sounds/home_theme.wav");
            });
            
            // Add key listener to start game from title screen
            frame.addKeyListener(new java.awt.event.KeyAdapter() {
                @Override
                public void keyPressed(java.awt.event.KeyEvent e) {
                    titleScreen.startGame();
                }
            });
            frame.setFocusable(true);
        });
    }
}