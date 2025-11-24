package Main;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.*;
import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.net.URL;
import java.util.List;
import javax.swing.Timer;

public class HomeScreen extends JPanel implements ActionListener {
    private JButton playButton;
    private JButton skillsButton;
    private JLabel currencyLabel;
    private JLabel characterLabel;
    private Timer idleAnimationTimer;
    private int animationFrame = 0;
    private int playerCurrency = 100000; // Starting currency

    // Available skills with costs
    private Map<String, Integer> availableSkills = new LinkedHashMap<>();
    private Map<String, Boolean> unlockedSkills = new HashMap<>();
    private Map<String, Boolean> equippedSkills = new HashMap<>();
    private List<String> equippedOrder = new ArrayList<>(); // order => R, F, V

    private SoundManager soundManager;

    public HomeScreen(SoundManager soundManager) {
        this.soundManager = soundManager;
        setBackground(Color.BLACK);
        setLayout(new BorderLayout());

        // Layered panes like before
        JLayeredPane layered = new JLayeredPane();
        layered.setPreferredSize(new Dimension(800, 600));
        layered.setLayout(null);

        MatrixBackground rain = new MatrixBackground();
        rain.setBounds(0,0,800,600);

        JPanel ui = new JPanel(new BorderLayout());
        ui.setOpaque(false);
        ui.setBounds(0,0,800,600);

        layered.add(rain, Integer.valueOf(0));
        layered.add(ui, Integer.valueOf(1));

        add(layered, BorderLayout.CENTER);

        initializeSkills();
        setupUIInto(ui);
        startIdleAnimation();
    }

private void initializeSkills() {

    // Prices
    availableSkills.put("Sandevistan", 500);
    availableSkills.put("Time Stop", 800);
    availableSkills.put("Shield", 1200);
    availableSkills.put("Blink", 1500);
    availableSkills.put("Speed Blitz", 2000);
    availableSkills.put("Revive", 5000); // NEW revive skill

    // Unlocked
    unlockedSkills.put("Sandevistan", false);
    unlockedSkills.put("Time Stop", false);
    unlockedSkills.put("Shield", false);
    unlockedSkills.put("Blink", false);
    unlockedSkills.put("Speed Blitz", true); // free starter
    unlockedSkills.put("Revive", false);     // NEW revive skill

    // Equipped state
    for (String skill : availableSkills.keySet()) {
        equippedSkills.put(skill, false);
    }
}



    private void setupUIInto(JPanel ui) {
        ui.setLayout(new BorderLayout());

        JPanel topPanel = new JPanel(new BorderLayout());
        topPanel.setOpaque(false);
        topPanel.setBorder(BorderFactory.createEmptyBorder(20,20,20,20));

        currencyLabel = new JLabel("Credits: " + playerCurrency);
        currencyLabel.setForeground(Color.GREEN);
        currencyLabel.setFont(new Font("VT323", Font.BOLD, 18));
        topPanel.add(currencyLabel, BorderLayout.EAST);

        JLabel titleLabel = new JLabel("MATRIX DODGER", SwingConstants.CENTER);
        titleLabel.setForeground(Color.GREEN);
        titleLabel.setFont(new Font("VT323", Font.BOLD, 24));
        topPanel.add(titleLabel, BorderLayout.CENTER);

        ui.add(topPanel, BorderLayout.NORTH);

        JPanel centerPanel = new JPanel(new GridBagLayout());
        centerPanel.setOpaque(false);

        characterLabel = new JLabel();
        characterLabel.setHorizontalAlignment(SwingConstants.CENTER);
        updateCharacterAnimation();

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.gridx = 0; gbc.gridy = 0; gbc.insets = new Insets(20,20,20,20);
        centerPanel.add(characterLabel, gbc);

        ui.add(centerPanel, BorderLayout.CENTER);

        JPanel bottomPanel = new JPanel(new FlowLayout());
        bottomPanel.setOpaque(false);
        bottomPanel.setBorder(BorderFactory.createEmptyBorder(20,20,40,20));

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

        button.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseEntered(java.awt.event.MouseEvent evt) {
                button.setBackground(new Color(0,50,0));
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
        loadSpriteFrames();
        idleAnimationTimer = new Timer(100, e -> {
            animationFrame = (animationFrame + 1) % TOTAL_FRAMES;
            updateCharacterAnimation();
        });
        idleAnimationTimer.start();
    }

    private void loadSpriteFrames() {
        idleFrames = new BufferedImage[TOTAL_FRAMES];
        try {
            for (int i = 0; i < TOTAL_FRAMES; i++) {
                String path = "/sprites/GamerGabby/frame" + i + ".png";
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
            characterLabel.setText(null);
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

    // Equip with limit of 3; maintain equipped order for R,F,V mapping
    public void equipSkill(String skillName) {
        if (!unlockedSkills.getOrDefault(skillName, false)) return;
        if (equippedSkills.getOrDefault(skillName, false)) return; // already equipped
        if (equippedOrder.size() >= 3) {
            // refuse equip if already 3
            JOptionPane.showMessageDialog(this, "You can only equip 3 skills (R, F, V). Unequip another first.", "Equip limit", JOptionPane.INFORMATION_MESSAGE);
            return;
        }
        equippedSkills.put(skillName, true);
        equippedOrder.add(skillName);
    }

    public void unequipSkill(String skillName) {
        if (!equippedSkills.getOrDefault(skillName, false)) return;
        equippedSkills.put(skillName, false);
        equippedOrder.remove(skillName);
    }

    public Map<String, Boolean> getEquippedSkills() {
        return new HashMap<>(equippedSkills);
    }

    // Return the equipped order (first -> R, second -> F, third -> V)
    public List<String> getEquippedOrder() {
        return new ArrayList<>(equippedOrder);
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
        // Create GameManager, map equipped skills into its R/F/V slots, then create GameArena(manager)
        GameManager gm = new GameManager();

        // map names -> SkillType
        Map<String, GameManager.SkillType> nameToType = new HashMap<>();
        nameToType.put("Sandevistan", GameManager.SkillType.SANDEVISTAN);
        nameToType.put("Time Stop", GameManager.SkillType.TIME_STOP);
        nameToType.put("Shield", GameManager.SkillType.SHIELD);
        nameToType.put("Blink", GameManager.SkillType.BLINK);
        nameToType.put("Speed Blitz", GameManager.SkillType.CLEAR_SCREEN);
        nameToType.put("Revive", GameManager.SkillType.REVIVE); 
        // others default to NONE

        List<String> order = getEquippedOrder();
        if (order.size() > 0) {
            GameManager.SkillType t = nameToType.getOrDefault(order.get(0), GameManager.SkillType.NONE);
            gm.equipSkillToR(t);
        }
        if (order.size() > 1) {
            GameManager.SkillType t = nameToType.getOrDefault(order.get(1), GameManager.SkillType.NONE);
            gm.equipSkillToF(t);
        }
        if (order.size() > 2) {
            GameManager.SkillType t = nameToType.getOrDefault(order.get(2), GameManager.SkillType.NONE);
            gm.equipSkillToV(t);
        }

        // Transition to game screen
        JFrame frame = (JFrame) SwingUtilities.getWindowAncestor(this);
        frame.getContentPane().removeAll();

        GameArena arena = new GameArena(gm); // <-- now GameArena accepts GameManager
        frame.getContentPane().add(arena);

        frame.revalidate();
        frame.repaint();

        SwingUtilities.invokeLater(arena::requestFocusInWindow);
    }

    private void showSkillsMenu() {
        JDialog skillsDialog = new JDialog((Frame) SwingUtilities.getWindowAncestor(this), "Skills & Upgrades", true);
        skillsDialog.setSize(600, 400);
        skillsDialog.setLocationRelativeTo(this);
        skillsDialog.getContentPane().setBackground(Color.BLACK);
        skillsDialog.setLayout(new BorderLayout());

        JPanel skillsPanel = new JPanel(new GridLayout(0, 2, 10, 10));
        skillsPanel.setBackground(Color.BLACK);
        skillsPanel.setBorder(BorderFactory.createEmptyBorder(20,20,20,20));

        for (String skill : availableSkills.keySet()) {
            JPanel skillPanel = createSkillPanel(skill);
            skillsPanel.add(skillPanel);
        }

        JScrollPane scrollPane = new JScrollPane(skillsPanel);
        scrollPane.setBorder(BorderFactory.createLineBorder(Color.GREEN));
        scrollPane.getViewport().setBackground(Color.BLACK);

        skillsDialog.add(scrollPane, BorderLayout.CENTER);

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
    panel.setPreferredSize(new Dimension(250,80));

    JLabel nameLabel = new JLabel(skillName);
    nameLabel.setForeground(Color.GREEN);
    nameLabel.setFont(new Font("Consolas", Font.BOLD, 14));

    int cost = availableSkills.get(skillName);
    JLabel costLabel = new JLabel("Cost: " + cost + " credits");

    // ⭐ FIXED COLOR LOGIC
    if (!unlockedSkills.getOrDefault(skillName, false)) {
        costLabel.setForeground(playerCurrency >= cost ? Color.GREEN : Color.RED);
    } else {
        costLabel.setForeground(Color.GREEN);
    }

    costLabel.setFont(new Font("Consolas", Font.PLAIN, 12));

    JButton actionButton = new JButton();
    actionButton.setFont(new Font("Consolas", Font.PLAIN, 12));
    actionButton.setFocusPainted(false);

    if (unlockedSkills.getOrDefault(skillName, false)) {
        if (equippedSkills.getOrDefault(skillName, false)) {
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

    JPanel infoPanel = new JPanel(new GridLayout(2,1));
    infoPanel.setBackground(Color.BLACK);
    infoPanel.add(nameLabel);
    infoPanel.add(costLabel);

    panel.add(infoPanel, BorderLayout.CENTER);
    panel.add(actionButton, BorderLayout.SOUTH);

    return panel;
}


    private void handleSkillAction(String skillName, JButton button) {
        if (!unlockedSkills.getOrDefault(skillName, false)) {
            if (purchaseSkill(skillName)) {
                button.setText("EQUIP");
                button.setBackground(Color.GREEN);
                SwingUtilities.getWindowAncestor(this).repaint();
            }
        } else {
            if (equippedSkills.getOrDefault(skillName, false)) {
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
