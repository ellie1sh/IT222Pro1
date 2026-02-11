package client.views;

import javax.swing.*;
import javax.swing.border.*;
import java.awt.*;

/**
 * Login and Registration view with clean, centered layout.
 * Follows Law of UX: clear visual hierarchy, accessible buttons.
 */
public class LoginView extends JPanel {
    private final MainFrame mainFrame;
    private JTextField txtUsername, txtRegUsername, txtFullName, txtEmail;
    private JPasswordField txtPassword, txtRegPassword;
    private JPanel loginPanel, registerPanel;
    private CardLayout cardLayout;

    // Color scheme
    private static final Color PRIMARY = new Color(25, 118, 210);
    private static final Color PRIMARY_DARK = new Color(21, 101, 192);
    private static final Color BG_COLOR = new Color(245, 245, 245);
    private static final Color CARD_BG = Color.WHITE;

    public LoginView(MainFrame mainFrame) {
        this.mainFrame = mainFrame;
        initComponents();
    }

    private void initComponents() {
        cardLayout = new CardLayout();
        setLayout(cardLayout);
        setBackground(BG_COLOR);

        loginPanel = createLoginPanel();
        registerPanel = createRegisterPanel();

        add(loginPanel, "LOGIN");
        add(registerPanel, "REGISTER");
    }

    private JPanel createLoginPanel() {
        JPanel outer = new JPanel(new GridBagLayout());
        outer.setBackground(BG_COLOR);

        JPanel card = new JPanel();
        card.setLayout(new BoxLayout(card, BoxLayout.Y_AXIS));
        card.setBackground(CARD_BG);
        card.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(new Color(220, 220, 220), 1),
            new EmptyBorder(40, 50, 40, 50)));
        card.setPreferredSize(new Dimension(420, 420));

        // Title
        JLabel lblTitle = new JLabel("Pharmacy Reservation System");
        lblTitle.setFont(new Font("SansSerif", Font.BOLD, 18));
        lblTitle.setForeground(PRIMARY);
        lblTitle.setAlignmentX(Component.CENTER_ALIGNMENT);
        card.add(lblTitle);
        card.add(Box.createVerticalStrut(5));

        JLabel lblSubtitle = new JLabel("SDG 3: Good Health and Well-Being");
        lblSubtitle.setFont(new Font("SansSerif", Font.PLAIN, 11));
        lblSubtitle.setForeground(Color.GRAY);
        lblSubtitle.setAlignmentX(Component.CENTER_ALIGNMENT);
        card.add(lblSubtitle);
        card.add(Box.createVerticalStrut(30));

        // Form
        JPanel form = new JPanel(new GridBagLayout());
        form.setBackground(CARD_BG);
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(5, 5, 5, 5);
        gbc.fill = GridBagConstraints.HORIZONTAL;

        gbc.gridx = 0; gbc.gridy = 0; gbc.weightx = 0;
        form.add(createLabel("Username"), gbc);
        gbc.gridx = 1; gbc.weightx = 1;
        txtUsername = createTextField();
        form.add(txtUsername, gbc);

        gbc.gridx = 0; gbc.gridy = 1; gbc.weightx = 0;
        form.add(createLabel("Password"), gbc);
        gbc.gridx = 1; gbc.weightx = 1;
        txtPassword = new JPasswordField(18);
        txtPassword.setFont(new Font("SansSerif", Font.PLAIN, 13));
        txtPassword.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(new Color(200, 200, 200)),
            new EmptyBorder(6, 8, 6, 8)));
        form.add(txtPassword, gbc);

        form.setAlignmentX(Component.CENTER_ALIGNMENT);
        card.add(form);
        card.add(Box.createVerticalStrut(20));

        // Buttons
        JPanel btnPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 10, 0));
        btnPanel.setBackground(CARD_BG);

        JButton btnLogin = createPrimaryButton("Login");
        btnLogin.addActionListener(e -> handleLogin());
        btnPanel.add(btnLogin);

        JButton btnRegister = createSecondaryButton("Create Account");
        btnRegister.addActionListener(e -> cardLayout.show(this, "REGISTER"));
        btnPanel.add(btnRegister);

        btnPanel.setAlignmentX(Component.CENTER_ALIGNMENT);
        card.add(btnPanel);
        card.add(Box.createVerticalStrut(25));

        // Demo credentials
        JPanel demoPanel = new JPanel();
        demoPanel.setLayout(new BoxLayout(demoPanel, BoxLayout.Y_AXIS));
        demoPanel.setBackground(new Color(237, 247, 255));
        demoPanel.setBorder(new EmptyBorder(10, 15, 10, 15));

        JLabel lblDemo = new JLabel("Demo Credentials:");
        lblDemo.setFont(new Font("SansSerif", Font.BOLD, 11));
        lblDemo.setAlignmentX(Component.LEFT_ALIGNMENT);
        demoPanel.add(lblDemo);

        demoPanel.add(createDemoLabel("Admin:  admin / admin123"));
        demoPanel.add(createDemoLabel("Pharmacist:  mercury_pharm / pharm123"));
        demoPanel.add(createDemoLabel("Customer:  resident / user123"));

        demoPanel.setAlignmentX(Component.CENTER_ALIGNMENT);
        card.add(demoPanel);

        txtPassword.addActionListener(e -> handleLogin());

        outer.add(card);
        return outer;
    }

    private JPanel createRegisterPanel() {
        JPanel outer = new JPanel(new GridBagLayout());
        outer.setBackground(BG_COLOR);

        JPanel card = new JPanel();
        card.setLayout(new BoxLayout(card, BoxLayout.Y_AXIS));
        card.setBackground(CARD_BG);
        card.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(new Color(220, 220, 220), 1),
            new EmptyBorder(40, 50, 40, 50)));
        card.setPreferredSize(new Dimension(420, 420));

        JLabel lblTitle = new JLabel("Create New Account");
        lblTitle.setFont(new Font("SansSerif", Font.BOLD, 18));
        lblTitle.setForeground(PRIMARY);
        lblTitle.setAlignmentX(Component.CENTER_ALIGNMENT);
        card.add(lblTitle);
        card.add(Box.createVerticalStrut(25));

        JPanel form = new JPanel(new GridBagLayout());
        form.setBackground(CARD_BG);
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(5, 5, 5, 5);
        gbc.fill = GridBagConstraints.HORIZONTAL;

        gbc.gridx = 0; gbc.gridy = 0; gbc.weightx = 0;
        form.add(createLabel("Full Name"), gbc);
        gbc.gridx = 1; gbc.weightx = 1;
        txtFullName = createTextField();
        form.add(txtFullName, gbc);

        gbc.gridx = 0; gbc.gridy = 1; gbc.weightx = 0;
        form.add(createLabel("Email"), gbc);
        gbc.gridx = 1; gbc.weightx = 1;
        txtEmail = createTextField();
        form.add(txtEmail, gbc);

        gbc.gridx = 0; gbc.gridy = 2; gbc.weightx = 0;
        form.add(createLabel("Username"), gbc);
        gbc.gridx = 1; gbc.weightx = 1;
        txtRegUsername = createTextField();
        form.add(txtRegUsername, gbc);

        gbc.gridx = 0; gbc.gridy = 3; gbc.weightx = 0;
        form.add(createLabel("Password"), gbc);
        gbc.gridx = 1; gbc.weightx = 1;
        txtRegPassword = new JPasswordField(18);
        txtRegPassword.setFont(new Font("SansSerif", Font.PLAIN, 13));
        txtRegPassword.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(new Color(200, 200, 200)),
            new EmptyBorder(6, 8, 6, 8)));
        form.add(txtRegPassword, gbc);

        form.setAlignmentX(Component.CENTER_ALIGNMENT);
        card.add(form);
        card.add(Box.createVerticalStrut(20));

        JPanel btnPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 10, 0));
        btnPanel.setBackground(CARD_BG);

        JButton btnCreate = createPrimaryButton("Register");
        btnCreate.addActionListener(e -> handleRegister());
        btnPanel.add(btnCreate);

        JButton btnBack = createSecondaryButton("Back to Login");
        btnBack.addActionListener(e -> cardLayout.show(this, "LOGIN"));
        btnPanel.add(btnBack);

        btnPanel.setAlignmentX(Component.CENTER_ALIGNMENT);
        card.add(btnPanel);

        outer.add(card);
        return outer;
    }

    private void handleLogin() {
        String username = txtUsername.getText().trim();
        String password = new String(txtPassword.getPassword());
        if (username.isEmpty() || password.isEmpty()) {
            JOptionPane.showMessageDialog(mainFrame, "Please enter username and password!",
                "Validation", JOptionPane.WARNING_MESSAGE);
            return;
        }
        mainFrame.handleLogin(username, password);
    }

    private void handleRegister() {
        String fullName = txtFullName.getText().trim();
        String email = txtEmail.getText().trim();
        String username = txtRegUsername.getText().trim();
        String password = new String(txtRegPassword.getPassword());
        if (fullName.isEmpty() || email.isEmpty() || username.isEmpty() || password.isEmpty()) {
            JOptionPane.showMessageDialog(mainFrame, "Please fill all fields!",
                "Validation", JOptionPane.WARNING_MESSAGE);
            return;
        }
        mainFrame.handleRegister(username, password, fullName, email);
        cardLayout.show(this, "LOGIN");
        txtFullName.setText(""); txtEmail.setText("");
        txtRegUsername.setText(""); txtRegPassword.setText("");
    }

    public void clearFields() {
        txtUsername.setText(""); txtPassword.setText("");
    }

    // ==================== UI HELPERS ====================

    private JLabel createLabel(String text) {
        JLabel lbl = new JLabel(text);
        lbl.setFont(new Font("SansSerif", Font.PLAIN, 13));
        return lbl;
    }

    private JTextField createTextField() {
        JTextField tf = new JTextField(18);
        tf.setFont(new Font("SansSerif", Font.PLAIN, 13));
        tf.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(new Color(200, 200, 200)),
            new EmptyBorder(6, 8, 6, 8)));
        return tf;
    }

    private JLabel createDemoLabel(String text) {
        JLabel lbl = new JLabel(text);
        lbl.setFont(new Font("Monospaced", Font.PLAIN, 11));
        lbl.setAlignmentX(Component.LEFT_ALIGNMENT);
        return lbl;
    }

    static JButton createPrimaryButton(String text) {
        JButton btn = new JButton(text);
        btn.setFont(new Font("SansSerif", Font.BOLD, 12));
        btn.setBackground(PRIMARY);
        btn.setForeground(Color.WHITE);
        btn.setFocusPainted(false);
        btn.setBorder(new EmptyBorder(8, 20, 8, 20));
        btn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        btn.setOpaque(true);
        return btn;
    }

    static JButton createSecondaryButton(String text) {
        JButton btn = new JButton(text);
        btn.setFont(new Font("SansSerif", Font.PLAIN, 12));
        btn.setBackground(new Color(240, 240, 240));
        btn.setForeground(new Color(66, 66, 66));
        btn.setFocusPainted(false);
        btn.setBorder(new EmptyBorder(8, 20, 8, 20));
        btn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        btn.setOpaque(true);
        return btn;
    }

    static JButton createSuccessButton(String text) {
        JButton btn = new JButton(text);
        btn.setFont(new Font("SansSerif", Font.BOLD, 12));
        btn.setBackground(new Color(46, 125, 50));
        btn.setForeground(Color.WHITE);
        btn.setFocusPainted(false);
        btn.setBorder(new EmptyBorder(8, 20, 8, 20));
        btn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        btn.setOpaque(true);
        return btn;
    }

    static JButton createDangerButton(String text) {
        JButton btn = new JButton(text);
        btn.setFont(new Font("SansSerif", Font.BOLD, 12));
        btn.setBackground(new Color(198, 40, 40));
        btn.setForeground(Color.WHITE);
        btn.setFocusPainted(false);
        btn.setBorder(new EmptyBorder(8, 20, 8, 20));
        btn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        btn.setOpaque(true);
        return btn;
    }

    static JButton createWarningButton(String text) {
        JButton btn = new JButton(text);
        btn.setFont(new Font("SansSerif", Font.BOLD, 12));
        btn.setBackground(new Color(245, 124, 0));
        btn.setForeground(Color.WHITE);
        btn.setFocusPainted(false);
        btn.setBorder(new EmptyBorder(8, 20, 8, 20));
        btn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        btn.setOpaque(true);
        return btn;
    }
}
