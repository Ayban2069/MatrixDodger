package Main;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.HashMap;
import java.util.Map;
import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.net.URL;


public class HomeScreen extends JPanel implements ActionListener {
    private JButton playButton;
    private JButton skillsButton;
    private JLabel currencyLabel;
    private JLabel characterLabel;
    private Timer idleAnimationTimer;
    private int animationFrame = 0;
    private int playerCurrency = 1000; // Starting currency
    
    // Available skills with costs
    private Map<String, Integer> availableSkills = new HashMap<>();
    private Map<String, Boolean> unlockedSkills = new HashMap<>();
    private Map<String, Boolean> equippedSkills = new HashMap<>();
    
    private SoundManager soundManager;
    
public HomeScreen(SoundManager soundManager) {
    this.soundManager = soundManager;
    setBackground(Color.BLACK);
    setLayout(new BorderLayout());

    // 🔹 Step 1: Create a JLayeredPane
    JLayeredPane layered = new JLayeredPane();
    layered.setPreferredSize(new Dimension(800, 600));
    layered.setLayout(null); // Important! Use absolute positioning

    // 🔹 Step 2: Add the Matrix background
    MatrixBackground rain = new MatrixBackground();
    rain.setBounds(0, 0, 800, 600);

    // 🔹 Step 3: Your UI layer (transparent)
    JPanel ui = new JPanel(new BorderLayout());
    ui.setOpaque(false);
    ui.setBounds(0, 0, 800, 600);

    // your original top, center, bottom UI setup will go here
    // instead of adding directly to HomeScreen, we add to ui

    // 🔹 Step 4: Add both layers
    layered.add(rain, Integer.valueOf(0)); // background
    layered.add(ui, Integer.valueOf(1));   // foreground

    // 🔹 Step 5: Add layered pane to the main panel
    add(layered, BorderLayout.CENTER);

    // Now build your UI inside 'ui'
    initializeSkills();
    setupUIInto(ui); // ⬅ we'll change setupUI() slightly
    startIdleAnimation();
}

    
    private void initializeSkills() {
        // Define available skills and their costs
        availableSkills.put("Speed Blitz", 500);
        availableSkills.put("Time Stop", 800);
        availableSkills.put("Sandevistan", 1200);
        availableSkills.put("Bullet Time", 1500);
        availableSkills.put("Neo Reflexes", 2000);
        availableSkills.put("System Breach", 2500);
        
        // Start with some skills unlocked
        unlockedSkills.put("Speed Blitz", true);
        unlockedSkills.put("Time Stop", false);
        unlockedSkills.put("Sandevistan", false);
        unlockedSkills.put("Bullet Time", false);
        unlockedSkills.put("Neo Reflexes", false);
        unlockedSkills.put("System Breach", false);
        
        // Start with no skills equipped
        for (String skill : availableSkills.keySet()) {
            equippedSkills.put(skill, false);
        }
    }
    
private void setupUIInto(JPanel ui) {
    ui.setLayout(new BorderLayout());

    // Top panel for currency and title
    JPanel topPanel = new JPanel(new BorderLayout());
    topPanel.setOpaque(false);
    topPanel.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));

    // Currency display
    currencyLabel = new JLabel("Credits: " + playerCurrency);
    currencyLabel.setForeground(Color.GREEN);
    currencyLabel.setFont(new Font("VT323", Font.BOLD, 18));
    topPanel.add(currencyLabel, BorderLayout.EAST);

    // Game title
    JLabel titleLabel = new JLabel("MATRIX DODGER", SwingConstants.CENTER);
    titleLabel.setForeground(Color.GREEN);
    titleLabel.setFont(new Font("VT323", Font.BOLD, 24));
    topPanel.add(titleLabel, BorderLayout.CENTER);

    ui.add(topPanel, BorderLayout.NORTH);

    // Center panel for character
    JPanel centerPanel = new JPanel(new GridBagLayout());
    centerPanel.setOpaque(false);

    characterLabel = new JLabel();
    characterLabel.setHorizontalAlignment(SwingConstants.CENTER);
    updateCharacterAnimation();

    GridBagConstraints gbc = new GridBagConstraints();
    gbc.gridx = 0;
    gbc.gridy = 0;
    gbc.insets = new Insets(20, 20, 20, 20);
    centerPanel.add(characterLabel, gbc);

    ui.add(centerPanel, BorderLayout.CENTER);

    // Bottom panel for buttons
    JPanel bottomPanel = new JPanel(new FlowLayout());
    bottomPanel.setOpaque(false);
    bottomPanel.setBorder(BorderFactory.createEmptyBorder(20, 20, 40, 20));

    playButton = createMatrixButton("PLAY GAME");
    playButton.addActionListener(this);
    bottomPanel.add(playButton);

    skillsButton = createMatrixButton("SKILLS & UPGRADES");
    skillsButton.addActionListener(this);
    bottomPanel.add(skillsButton);

    ui.add(bottomPanel, BorderLayout.SOUTH);
}

    
    private JButton createMatrixButton(String text) {
        JButton button = new JButton(text);
        button.setFont(new Font("VT323", Font.BOLD, 16));
        button.setForeground(Color.GREEN);
        button.setBackground(Color.BLACK);
        button.setBorder(BorderFactory.createLineBorder(Color.GREEN, 2));
        button.setFocusPainted(false);
        button.setPreferredSize(new Dimension(200, 50));
        
        // Hover effects
        button.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseEntered(java.awt.event.MouseEvent evt) {
                button.setBackground(new Color(0, 50, 0));
                soundManager.playSound("select");
            }
            
            public void mouseExited(java.awt.event.MouseEvent evt) {
                button.setBackground(Color.BLACK);
            }
        });
        
        return button;
    }


// Sprite frames
private BufferedImage[] idleFrames;
private final int TOTAL_FRAMES = 7;

private void startIdleAnimation() {
    loadSpriteFrames(); // load your images first
    idleAnimationTimer = new Timer(50, e -> {
        animationFrame = (animationFrame + 1) % TOTAL_FRAMES;
        updateCharacterAnimation();
    });
    idleAnimationTimer.start();
}

private void loadSpriteFrames() {
    idleFrames = new BufferedImage[TOTAL_FRAMES];
    try {
        for (int i = 0; i < TOTAL_FRAMES; i++) {
            String path = "/sprites/GamerGabby/frame"+i+".png";
            URL resource = getClass().getResource(path);
            if (resource != null) {
                idleFrames[i] = ImageIO.read(resource);
            } else {
                System.err.println("❌ Missing sprite frame: " + path);
            }
        }
    } catch (IOException e) {
        e.printStackTrace();
    }
}


private void updateCharacterAnimation() {
    if (idleFrames != null && idleFrames[animationFrame] != null) {
        characterLabel.setIcon(new ImageIcon(idleFrames[animationFrame]));
        characterLabel.setText(null); // remove placeholder text
    }
}

    
    public void addCurrency(int amount) {
        playerCurrency += amount;
        currencyLabel.setText("Credits: " + playerCurrency);
    }
    
    public boolean purchaseSkill(String skillName) {
        if (unlockedSkills.containsKey(skillName) && !unlockedSkills.get(skillName)) {
            int cost = availableSkills.get(skillName);
            if (playerCurrency >= cost) {
                playerCurrency -= cost;
                unlockedSkills.put(skillName, true);
                currencyLabel.setText("Credits: " + playerCurrency);
                soundManager.playSound("powerup");
                return true;
            }
        }
        return false;
    }
    
    public void equipSkill(String skillName) {
        if (unlockedSkills.get(skillName)) {
            equippedSkills.put(skillName, true);
        }
    }
    
    public void unequipSkill(String skillName) {
        equippedSkills.put(skillName, false);
    }
    
    public Map<String, Boolean> getEquippedSkills() {
        return new HashMap<>(equippedSkills);
    }
    
    public int getPlayerCurrency() {
        return playerCurrency;
    }
    
    @Override
    public void actionPerformed(ActionEvent e) {
        if (e.getSource() == playButton) {
            soundManager.playSound("select");
            startGame();
        } else if (e.getSource() == skillsButton) {
            soundManager.playSound("select");
            showSkillsMenu();
        }
    }
    
private void startGame() {
    // Transition to game screen
    JFrame frame = (JFrame) SwingUtilities.getWindowAncestor(this);
    frame.getContentPane().removeAll();

    GameArena arena = new GameArena();
    frame.getContentPane().add(arena);

    frame.revalidate();
    frame.repaint();

    // Make sure arena gets focus for controls
    SwingUtilities.invokeLater(arena::requestFocusInWindow);
}

    
    private void showSkillsMenu() {
        // Create skills selection dialog
        JDialog skillsDialog = new JDialog((Frame) SwingUtilities.getWindowAncestor(this), "Skills & Upgrades", true);
        skillsDialog.setSize(600, 400);
        skillsDialog.setLocationRelativeTo(this);
        skillsDialog.getContentPane().setBackground(Color.BLACK);
        skillsDialog.setLayout(new BorderLayout());
        
        JPanel skillsPanel = new JPanel(new GridLayout(0, 2, 10, 10));
        skillsPanel.setBackground(Color.BLACK);
        skillsPanel.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));
        
        for (String skill : availableSkills.keySet()) {
            JPanel skillPanel = createSkillPanel(skill);
            skillsPanel.add(skillPanel);
        }
        
        JScrollPane scrollPane = new JScrollPane(skillsPanel);
        scrollPane.setBorder(BorderFactory.createLineBorder(Color.GREEN));
        scrollPane.getViewport().setBackground(Color.BLACK);
        
        skillsDialog.add(scrollPane, BorderLayout.CENTER);
        
        // Close button
        JButton closeButton = createMatrixButton("CLOSE");
        closeButton.addActionListener(e -> skillsDialog.dispose());
        
        JPanel buttonPanel = new JPanel();
        buttonPanel.setBackground(Color.BLACK);
        buttonPanel.add(closeButton);
        skillsDialog.add(buttonPanel, BorderLayout.SOUTH);
        
        skillsDialog.setVisible(true);
    }
    
    private JPanel createSkillPanel(String skillName) {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBorder(BorderFactory.createLineBorder(Color.GREEN, 1));
        panel.setBackground(Color.BLACK);
        panel.setPreferredSize(new Dimension(250, 80));
        
        JLabel nameLabel = new JLabel(skillName);
        nameLabel.setForeground(Color.GREEN);
        nameLabel.setFont(new Font("Consolas", Font.BOLD, 14));
        
        JLabel costLabel = new JLabel("Cost: " + availableSkills.get(skillName) + " credits");
        costLabel.setForeground(unlockedSkills.get(skillName) ? Color.GREEN : Color.RED);
        costLabel.setFont(new Font("Consolas", Font.PLAIN, 12));
        
        JButton actionButton = new JButton();
        actionButton.setFont(new Font("Consolas", Font.PLAIN, 12));
        actionButton.setFocusPainted(false);
        
        if (unlockedSkills.get(skillName)) {
            if (equippedSkills.get(skillName)) {
                actionButton.setText("UNEQUIP");
                actionButton.setBackground(Color.RED);
            } else {
                actionButton.setText("EQUIP");
                actionButton.setBackground(Color.GREEN);
            }
        } else {
            actionButton.setText("PURCHASE");
            actionButton.setBackground(Color.GRAY);
        }
        
        actionButton.addActionListener(e -> handleSkillAction(skillName, actionButton));
        
        JPanel infoPanel = new JPanel(new GridLayout(2, 1));
        infoPanel.setBackground(Color.BLACK);
        infoPanel.add(nameLabel);
        infoPanel.add(costLabel);
        
        panel.add(infoPanel, BorderLayout.CENTER);
        panel.add(actionButton, BorderLayout.SOUTH);
        
        return panel;
    }
    
    private void handleSkillAction(String skillName, JButton button) {
        if (!unlockedSkills.get(skillName)) {
            // Purchase skill
            if (purchaseSkill(skillName)) {
                button.setText("EQUIP");
                button.setBackground(Color.GREEN);
                // Refresh the skills menu
                SwingUtilities.getWindowAncestor(this).repaint();
            }
        } else {
            // Equip/Unequip skill
            if (equippedSkills.get(skillName)) {
                unequipSkill(skillName);
                button.setText("EQUIP");
                button.setBackground(Color.GREEN);
            } else {
                equipSkill(skillName);
                button.setText("UNEQUIP");
                button.setBackground(Color.RED);
            }
        }
    }
}
