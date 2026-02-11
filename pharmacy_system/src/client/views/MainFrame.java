package client.views;

import server.DataStore;
import client.controllers.*;

import javax.swing.*;
import java.awt.*;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

/**
 * Main application frame using CardLayout to switch between views.
 * Implements MVC pattern: views communicate through controllers.
 */
public class MainFrame extends JFrame {
    private final CardLayout cardLayout;
    private final JPanel mainPanel;

    private final DataStore dataStore;
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

    public MainFrame(DataStore dataStore) {
        this.dataStore = dataStore;
        this.authController = new AuthController(dataStore);
        this.adminController = new AdminController(dataStore);
        this.pharmacistController = new PharmacistController(dataStore);
        this.userController = new UserController(dataStore);

        setTitle("Pharmacy Reservation System - SDG 3: Good Health and Well-Being");
        setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
        setSize(1200, 800);
        setMinimumSize(new Dimension(1000, 700));
        setLocationRelativeTo(null);

        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                dataStore.saveAllData();
                dispose();
                System.exit(0);
            }
        });

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

    public void handleRegister(String username, String password, String fullName, String email, String contactNumber, String address, String accountType) {
        String msg = authController.register(username, password, fullName, email, contactNumber, address, accountType);
        if (msg.contains("successful") || msg.contains("pending")) {
            JOptionPane.showMessageDialog(this, msg,
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
