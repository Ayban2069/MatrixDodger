package main;

import javax.swing.*;
import java.awt.*;

public class SplashScreen extends JWindow {

    public SplashScreen() {
        // Create label for the splash text
        JLabel label = new JLabel("Matrix Dodger", SwingConstants.CENTER);
        label.setFont(new Font("Consolas", Font.BOLD, 48));
        label.setForeground(Color.GREEN);

        // Add it to the window
        getContentPane().add(label, BorderLayout.CENTER);
        getContentPane().setBackground(Color.BLACK); // black background or transparent (optional)
        setBounds(400, 200, 600, 200); // position and size
        setBackground(new Color(0, 0, 0, 0)); // Transparent background
    }

    public static void main(String[] args) {
        SplashScreen splash = new SplashScreen();
        splash.setVisible(true);

        // Display splash for 5 seconds
        try {
            Thread.sleep(5000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        splash.dispose(); // close splash

        // Then open your title screen (replace this with your actual main screen)
//SwingUtilities.invokeLater(Main::launchMainWindow);

    }
}
