package client.views;

import client.network.ServerConnection;
import client.controllers.*;

import javax.swing.*;
import java.awt.*;

/**
 * Main application frame using CardLayout to switch between views.
 * Implements MVC pattern: views communicate through controllers.
 */
public class MainFrame extends JFrame {
    private final CardLayout cardLayout;
    private final JPanel mainPanel;

    private final ServerConnection connection;
    private final AuthController authController;
    private final AdminController adminController;
    private final PharmacistController pharmacistController;
    private final UserController userController;

    private LoginView loginView;
    private AdminView adminView;
    private PharmacistView pharmacistView;
    private UserView userView;

    public static final String LOGIN_VIEW = "LOGIN";
    public static final String ADMIN_VIEW = "ADMIN";
    public static final String PHARMACY_VIEW = "PHARMACY";
    public static final String USER_VIEW = "USER";

    public MainFrame(ServerConnection connection) {
        this.connection = connection;
        this.authController = new AuthController(connection);
        this.adminController = new AdminController(connection);
        this.pharmacistController = new PharmacistController(connection);
        this.userController = new UserController(connection);

        setTitle("Pharmacy Reservation System - SDG 3: Good Health and Well-Being");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(1200, 800);
        setMinimumSize(new Dimension(1000, 700));
        setLocationRelativeTo(null);

        cardLayout = new CardLayout();
        mainPanel = new JPanel(cardLayout);

        initializeViews();
        add(mainPanel);
        showView(LOGIN_VIEW);
    }

    private void initializeViews() {
        loginView = new LoginView(this);
        adminView = new AdminView(this);
        pharmacistView = new PharmacistView(this);
        userView = new UserView(this);

        mainPanel.add(loginView, LOGIN_VIEW);
        mainPanel.add(adminView, ADMIN_VIEW);
        mainPanel.add(pharmacistView, PHARMACY_VIEW);
        mainPanel.add(userView, USER_VIEW);
    }

    public void showView(String viewName) {
        cardLayout.show(mainPanel, viewName);
    }

    public void handleLogin(String username, String password) {
        boolean success = authController.login(username, password);
        if (!success) {
            JOptionPane.showMessageDialog(this, "Invalid username or password!",
                "Login Failed", JOptionPane.ERROR_MESSAGE);
            return;
        }

        String type = authController.getCurrentUserType();
        switch (type) {
            case "ADMIN":
                adminView.refresh();
                showView(ADMIN_VIEW);
                break;
            case "PHARMACIST":
                pharmacistView.setPharmacyId(authController.getCurrentPharmacyId());
                pharmacistView.refresh();
                showView(PHARMACY_VIEW);
                break;
            case "RESIDENT":
                userView.setUserId(authController.getCurrentUserId());
                userView.setUserName(authController.getCurrentFullName());
                userView.refresh();
                showView(USER_VIEW);
                break;
            default:
                JOptionPane.showMessageDialog(this, "Unknown user type: " + type,
                    "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    public void handleRegister(String username, String password, String fullName, String email) {
        String msg = authController.register(username, password, fullName, email);
        if (msg.contains("successful")) {
            JOptionPane.showMessageDialog(this, "Registration successful! You can now login.",
                "Success", JOptionPane.INFORMATION_MESSAGE);
        } else {
            JOptionPane.showMessageDialog(this, msg, "Registration Failed", JOptionPane.ERROR_MESSAGE);
        }
    }

    public void handleLogout() {
        authController.logout();
        loginView.clearFields();
        showView(LOGIN_VIEW);
    }

    public AuthController getAuthController() { return authController; }
    public AdminController getAdminController() { return adminController; }
    public PharmacistController getPharmacistController() { return pharmacistController; }
    public UserController getUserController() { return userController; }
}
