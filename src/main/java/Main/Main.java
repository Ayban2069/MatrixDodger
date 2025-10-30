package Main;

import javax.swing.*;
import java.awt.*;

public class Main {
    private static SoundManager soundManager = new SoundManager();

    public static void main(String[] args) {
        // Step 1: Show the splash before starting Swing UI
        showSplashScreen();

        // Step 2: Then run your main UI logic
        SwingUtilities.invokeLater(() -> {
            JFrame frame = new JFrame("Matrix Dodger");
            frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            frame.setResizable(false);

            // Card layout for switching screens
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

            // When the title screen says "start game"
            titleScreen.setOnGameStart(() -> {
                cardLayout.show(mainPanel, "home");
                soundManager.playSound("select");
                // Optionally: soundManager.playBackgroundMusic("sounds/home_theme.wav");
            });

            // Key listener for starting game from title screen
            frame.addKeyListener(new java.awt.event.KeyAdapter() {
                @Override
                public void keyPressed(java.awt.event.KeyEvent e) {
                    titleScreen.startGame();
                }
            });
            frame.setFocusable(true);
        });
    }

    // --------------------------
    // SPLASH SCREEN METHOD
    // --------------------------
private static void showSplashScreen() {
    JWindow splash = new JWindow();

    // Make the window background fully transparent
    splash.setBackground(new Color(0, 0, 0, 0));

    // Create the text label
    JLabel label = new JLabel("Matrix Dodger", SwingConstants.CENTER);
    label.setFont(new Font("VT323", Font.BOLD, 60));
    label.setForeground(Color.GREEN);
    label.setOpaque(false);

    // Transparent panel to hold the label (no background)
    JPanel panel = new JPanel(new BorderLayout());
    panel.setOpaque(false);
    panel.add(label, BorderLayout.CENTER);

    splash.getContentPane().add(panel);
    splash.setSize(600, 200);
    splash.setLocationRelativeTo(null);
    splash.setVisible(true);

    try {
        Thread.sleep(2500); // show for 5 seconds
    } catch (InterruptedException e) {
        e.printStackTrace();
    }

    splash.dispose(); // close splash
}



    private static void launchMainWindow() {
    JFrame frame = new JFrame("Matrix Dodger");
    frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
    frame.setResizable(false);

    // Card layout for switching screens
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

    // Game start callback for title screen
    titleScreen.setOnGameStart(() -> {
        cardLayout.show(mainPanel, "home");
        soundManager.playSound("select");
        // Optionally: soundManager.playBackgroundMusic("sounds/home_theme.wav");
    });

    // Key listener to start game from title screen
    frame.addKeyListener(new java.awt.event.KeyAdapter() {
        @Override
        public void keyPressed(java.awt.event.KeyEvent e) {
            titleScreen.startGame();
        }
    });

    frame.setFocusable(true);
}

}
