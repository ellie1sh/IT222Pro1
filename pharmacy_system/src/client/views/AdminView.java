package client.views;

import client.controllers.AdminController;
import javax.swing.*;
import javax.swing.border.*;
import javax.swing.table.*;
import java.awt.*;

/**
 * Admin dashboard: manage users, pharmacies, approve accounts, view details.
 */
public class AdminView extends JPanel {
    private final MainFrame mainFrame;
    private final AdminController controller;

    // Notifications
    private JTable pendingUsersTable;
    private DefaultTableModel pendingUsersTableModel;
    private JTable pendingPharmaciesTable;
    private DefaultTableModel pendingPharmaciesTableModel;
    private JLabel lblNotificationCount;

    // User Management
    private JTextField txtUserSearch;
    private JTable usersTable;
    private DefaultTableModel usersTableModel;
    private JTable userTransactionsTable;
    private DefaultTableModel userTransactionsTableModel;

    // Pharmacy Management
    private JTextField txtPharmacySearch;
    private JTable pharmaciesTable;
    private DefaultTableModel pharmaciesTableModel;
    private JTable pharmacyMedicinesTable;
    private DefaultTableModel pharmacyMedicinesTableModel;

    private static final Color HEADER_BG = new Color(25, 118, 210);

    public AdminView(MainFrame mainFrame) {
        this.mainFrame = mainFrame;
        this.controller = mainFrame.getAdminController();
        initComponents();
    }

    private void initComponents() {
        setLayout(new BorderLayout());
        setBackground(new Color(245, 245, 245));

        // Top bar
        JPanel topBar = new JPanel(new BorderLayout());
        topBar.setBackground(HEADER_BG);
        topBar.setBorder(new EmptyBorder(12, 20, 12, 20));

        JPanel leftPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 15, 0));
        leftPanel.setOpaque(false);
        
        JLabel lblTitle = new JLabel("Admin Dashboard");
        lblTitle.setFont(new Font("SansSerif", Font.BOLD, 18));
        lblTitle.setForeground(Color.WHITE);
        leftPanel.add(lblTitle);

        lblNotificationCount = new JLabel("0 Pending");
        lblNotificationCount.setFont(new Font("SansSerif", Font.BOLD, 12));
        lblNotificationCount.setForeground(new Color(255, 193, 7));
        lblNotificationCount.setBorder(new EmptyBorder(4, 10, 4, 10));
        leftPanel.add(lblNotificationCount);

        topBar.add(leftPanel, BorderLayout.WEST);

        JButton btnLogout = LoginView.createDangerButton("Logout");
        btnLogout.addActionListener(e -> mainFrame.handleLogout());
        topBar.add(btnLogout, BorderLayout.EAST);

        add(topBar, BorderLayout.NORTH);

        // Tabs
        JTabbedPane tabs = new JTabbedPane();
        tabs.setFont(new Font("SansSerif", Font.PLAIN, 13));
        tabs.setBorder(new EmptyBorder(10, 10, 10, 10));

        tabs.addTab("Notifications", createNotificationsPanel());
        tabs.addTab("Users", createUserPanel());
        tabs.addTab("Pharmacies", createPharmacyPanel());

        add(tabs, BorderLayout.CENTER);
    }

    // ==================== NOTIFICATIONS ====================

    private JPanel createNotificationsPanel() {
        JPanel panel = new JPanel(new GridLayout(2, 1, 10, 10));
        panel.setBorder(new EmptyBorder(15, 15, 15, 15));

        // Pending Users
        JPanel usersPanel = new JPanel(new BorderLayout(10, 10));
        usersPanel.setBorder(BorderFactory.createTitledBorder(
            BorderFactory.createLineBorder(new Color(200, 200, 200)),
            "Pending User Accounts", TitledBorder.LEFT, TitledBorder.TOP,
            new Font("SansSerif", Font.BOLD, 13)));

        JPanel userBtnPanel = new JPanel(new BorderLayout());
        JPanel leftBtns = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        JButton btnRefreshUsers = LoginView.createSecondaryButton("Refresh");
        btnRefreshUsers.addActionListener(e -> refreshNotifications());
        leftBtns.add(btnRefreshUsers);
        
        JPanel rightBtns = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        JButton btnAcceptAllUsers = LoginView.createSuccessButton("Accept All Users");
        btnAcceptAllUsers.addActionListener(e -> acceptAllUsers());
        rightBtns.add(btnAcceptAllUsers);
        
        userBtnPanel.add(leftBtns, BorderLayout.WEST);
        userBtnPanel.add(rightBtns, BorderLayout.EAST);
        usersPanel.add(userBtnPanel, BorderLayout.NORTH);

        String[] userCols = {"ID", "Username", "Full Name", "Email", "Type", "Status"};
        pendingUsersTableModel = new DefaultTableModel(userCols, 0) {
            public boolean isCellEditable(int r, int c) { return false; }
        };
        pendingUsersTable = createStyledTable(pendingUsersTableModel);
        pendingUsersTable.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseClicked(java.awt.event.MouseEvent e) {
                int row = pendingUsersTable.getSelectedRow();
                if (row >= 0) {
                    showUserApprovalDialog(row);
                }
            }
        });
        JScrollPane userScroll = new JScrollPane(pendingUsersTable);
        userScroll.setBorder(BorderFactory.createLineBorder(new Color(200, 200, 200)));
        usersPanel.add(userScroll, BorderLayout.CENTER);

        // Pending Pharmacies
        JPanel pharmaciesPanel = new JPanel(new BorderLayout(10, 10));
        pharmaciesPanel.setBorder(BorderFactory.createTitledBorder(
            BorderFactory.createLineBorder(new Color(200, 200, 200)),
            "Pending Pharmacy Accounts", TitledBorder.LEFT, TitledBorder.TOP,
            new Font("SansSerif", Font.BOLD, 13)));

        JPanel pharmBtnPanel = new JPanel(new BorderLayout());
        JPanel leftPharmBtns = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        JButton btnRefreshPharm = LoginView.createSecondaryButton("Refresh");
        btnRefreshPharm.addActionListener(e -> refreshNotifications());
        leftPharmBtns.add(btnRefreshPharm);
        
        JPanel rightPharmBtns = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        JButton btnAcceptAllPharmacies = LoginView.createSuccessButton("Accept All Pharmacies");
        btnAcceptAllPharmacies.addActionListener(e -> acceptAllPharmacies());
        rightPharmBtns.add(btnAcceptAllPharmacies);
        
        pharmBtnPanel.add(leftPharmBtns, BorderLayout.WEST);
        pharmBtnPanel.add(rightPharmBtns, BorderLayout.EAST);
        pharmaciesPanel.add(pharmBtnPanel, BorderLayout.NORTH);

        String[] pharmCols = {"ID", "Name", "Address", "Contact", "Email", "Status"};
        pendingPharmaciesTableModel = new DefaultTableModel(pharmCols, 0) {
            public boolean isCellEditable(int r, int c) { return false; }
        };
        pendingPharmaciesTable = createStyledTable(pendingPharmaciesTableModel);
        pendingPharmaciesTable.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseClicked(java.awt.event.MouseEvent e) {
                int row = pendingPharmaciesTable.getSelectedRow();
                if (row >= 0) {
                    showPharmacyApprovalDialog(row);
                }
            }
        });
        JScrollPane pharmScroll = new JScrollPane(pendingPharmaciesTable);
        pharmScroll.setBorder(BorderFactory.createLineBorder(new Color(200, 200, 200)));
        pharmaciesPanel.add(pharmScroll, BorderLayout.CENTER);

        panel.add(usersPanel);
        panel.add(pharmaciesPanel);

        return panel;
    }

    // ==================== USER MANAGEMENT ====================

    private JPanel createUserPanel() {
        JPanel panel = new JPanel(new BorderLayout(10, 10));
        panel.setBorder(new EmptyBorder(15, 15, 15, 15));

        // Split: left = user table, right = transactions
        JSplitPane splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT);
        splitPane.setResizeWeight(0.6);

        // Left panel: Search and Users table
        JPanel leftPanel = new JPanel(new BorderLayout(10, 10));

        // Search bar and buttons
        JPanel topPanel = new JPanel(new BorderLayout(10, 0));
        
        JPanel searchPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 0));
        searchPanel.add(new JLabel("Search:"));
        txtUserSearch = new JTextField(25);
        txtUserSearch.addKeyListener(new java.awt.event.KeyAdapter() {
            public void keyReleased(java.awt.event.KeyEvent e) {
                filterUsers();
            }
        });
        searchPanel.add(txtUserSearch);
        topPanel.add(searchPanel, BorderLayout.WEST);

        JPanel btnPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        JButton btnAdd = LoginView.createPrimaryButton("Add User");
        JButton btnRefresh = LoginView.createSecondaryButton("Refresh");
        btnAdd.addActionListener(e -> showAddUserDialog());
        btnRefresh.addActionListener(e -> refreshUsers());
        btnPanel.add(btnAdd);
        btnPanel.add(btnRefresh);
        topPanel.add(btnPanel, BorderLayout.EAST);

        leftPanel.add(topPanel, BorderLayout.NORTH);

        // Table
        String[] cols = {"ID", "Username", "Password", "Full Name", "Email", "Type", "Status"};
        usersTableModel = new DefaultTableModel(cols, 0) {
            public boolean isCellEditable(int r, int c) { return false; }
        };
        usersTable = createStyledTable(usersTableModel);
        usersTable.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseClicked(java.awt.event.MouseEvent e) {
                int row = usersTable.getSelectedRow();
                if (row >= 0) {
                    int userId = (int) usersTableModel.getValueAt(row, 0);
                    refreshUserTransactions(userId);
                    if (e.getClickCount() == 2) {
                        showEditUserDialog(row);
                    }
                }
            }
        });

        JScrollPane scroll = new JScrollPane(usersTable);
        scroll.setBorder(BorderFactory.createLineBorder(new Color(200, 200, 200)));
        leftPanel.add(scroll, BorderLayout.CENTER);

        // Right panel: User Transactions
        JPanel rightPanel = new JPanel(new BorderLayout(10, 10));
        rightPanel.setBorder(BorderFactory.createTitledBorder(
            BorderFactory.createLineBorder(new Color(200, 200, 200)),
            "User Transaction History", TitledBorder.LEFT, TitledBorder.TOP,
            new Font("SansSerif", Font.BOLD, 13)));

        String[] transCols = {"ID", "Ref", "Medicine", "Pharmacy", "Qty", "Total", "Payment", "Status"};
        userTransactionsTableModel = new DefaultTableModel(transCols, 0) {
            public boolean isCellEditable(int r, int c) { return false; }
        };
        userTransactionsTable = createStyledTable(userTransactionsTableModel);
        JScrollPane transScroll = new JScrollPane(userTransactionsTable);
        transScroll.setBorder(BorderFactory.createLineBorder(new Color(200, 200, 200)));
        rightPanel.add(transScroll, BorderLayout.CENTER);

        splitPane.setLeftComponent(leftPanel);
        splitPane.setRightComponent(rightPanel);

        panel.add(splitPane, BorderLayout.CENTER);

        return panel;
    }

    // ==================== PHARMACY MANAGEMENT ====================

    private JPanel createPharmacyPanel() {
        JPanel panel = new JPanel(new BorderLayout(10, 10));
        panel.setBorder(new EmptyBorder(15, 15, 15, 15));

        // Split: left = pharmacy table, right = medicines
        JSplitPane splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT);
        splitPane.setResizeWeight(0.6);

        // Left panel: Search and Pharmacies table
        JPanel leftPanel = new JPanel(new BorderLayout(10, 10));

        // Search bar and buttons
        JPanel topPanel = new JPanel(new BorderLayout(10, 0));
        
        JPanel searchPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 0));
        searchPanel.add(new JLabel("Search:"));
        txtPharmacySearch = new JTextField(25);
        txtPharmacySearch.addKeyListener(new java.awt.event.KeyAdapter() {
            public void keyReleased(java.awt.event.KeyEvent e) {
                filterPharmacies();
            }
        });
        searchPanel.add(txtPharmacySearch);
        topPanel.add(searchPanel, BorderLayout.WEST);

        JPanel btnPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        JButton btnAddPharmacy = LoginView.createPrimaryButton("Add Pharmacy");
        JButton btnRefresh = LoginView.createSecondaryButton("Refresh");
        btnAddPharmacy.addActionListener(e -> showAddPharmacyDialog());
        btnRefresh.addActionListener(e -> refreshPharmacies());
        btnPanel.add(btnAddPharmacy);
        btnPanel.add(btnRefresh);
        topPanel.add(btnPanel, BorderLayout.EAST);

        leftPanel.add(topPanel, BorderLayout.NORTH);

        // Table
        String[] cols = {"ID", "Name", "Username", "Password", "Address", "Contact", "Email", "Status"};
        pharmaciesTableModel = new DefaultTableModel(cols, 0) {
            public boolean isCellEditable(int r, int c) { return false; }
        };
        pharmaciesTable = createStyledTable(pharmaciesTableModel);
        pharmaciesTable.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseClicked(java.awt.event.MouseEvent e) {
                int row = pharmaciesTable.getSelectedRow();
                if (row >= 0) {
                    int pharmId = (int) pharmaciesTableModel.getValueAt(row, 0);
                    refreshPharmacyMedicines(pharmId);
                    if (e.getClickCount() == 2) {
                        showEditPharmacyDialog(row);
                    }
                }
            }
        });

        JScrollPane scroll = new JScrollPane(pharmaciesTable);
        scroll.setBorder(BorderFactory.createLineBorder(new Color(200, 200, 200)));
        leftPanel.add(scroll, BorderLayout.CENTER);

        // Right panel: Pharmacy Medicines
        JPanel rightPanel = new JPanel(new BorderLayout(10, 10));
        rightPanel.setBorder(BorderFactory.createTitledBorder(
            BorderFactory.createLineBorder(new Color(200, 200, 200)),
            "Pharmacy Medicine List", TitledBorder.LEFT, TitledBorder.TOP,
            new Font("SansSerif", Font.BOLD, 13)));

        String[] medCols = {"ID", "Brand", "Generic", "Dosage", "Form", "Price", "Qty", "Category", "Status"};
        pharmacyMedicinesTableModel = new DefaultTableModel(medCols, 0) {
            public boolean isCellEditable(int r, int c) { return false; }
        };
        pharmacyMedicinesTable = createStyledTable(pharmacyMedicinesTableModel);
        JScrollPane medScroll = new JScrollPane(pharmacyMedicinesTable);
        medScroll.setBorder(BorderFactory.createLineBorder(new Color(200, 200, 200)));
        rightPanel.add(medScroll, BorderLayout.CENTER);

        splitPane.setLeftComponent(leftPanel);
        splitPane.setRightComponent(rightPanel);

        panel.add(splitPane, BorderLayout.CENTER);

        return panel;
    }

    // ==================== USER DIALOGS ====================

    private void showAddUserDialog() {
        JDialog dialog = new JDialog((Frame) SwingUtilities.getWindowAncestor(this), "Add Resident User", true);
        dialog.setLayout(new BorderLayout(10, 10));
        dialog.setSize(500, 400);
        dialog.setLocationRelativeTo(this);

        JPanel formPanel = new JPanel(new GridBagLayout());
        formPanel.setBorder(new EmptyBorder(20, 20, 20, 20));
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(8, 8, 8, 8);
        gbc.fill = GridBagConstraints.HORIZONTAL;

        JTextField txtUsername = new JTextField(20);
        JPasswordField txtPassword = new JPasswordField(20);
        JTextField txtFullName = new JTextField(20);
        JTextField txtEmail = new JTextField(20);
        JTextField txtContactNumber = new JTextField(20);
        JTextField txtAddress = new JTextField(20);

        gbc.gridx = 0; gbc.gridy = 0; gbc.weightx = 0;
        formPanel.add(new JLabel("Username:"), gbc);
        gbc.gridx = 1; gbc.weightx = 1;
        formPanel.add(txtUsername, gbc);

        gbc.gridx = 0; gbc.gridy = 1; gbc.weightx = 0;
        formPanel.add(new JLabel("Password:"), gbc);
        gbc.gridx = 1; gbc.weightx = 1;
        formPanel.add(txtPassword, gbc);

        gbc.gridx = 0; gbc.gridy = 2; gbc.weightx = 0;
        formPanel.add(new JLabel("Full Name:"), gbc);
        gbc.gridx = 1; gbc.weightx = 1;
        formPanel.add(txtFullName, gbc);

        gbc.gridx = 0; gbc.gridy = 3; gbc.weightx = 0;
        formPanel.add(new JLabel("Email:"), gbc);
        gbc.gridx = 1; gbc.weightx = 1;
        formPanel.add(txtEmail, gbc);

        gbc.gridx = 0; gbc.gridy = 4; gbc.weightx = 0;
        formPanel.add(new JLabel("Contact Number:"), gbc);
        gbc.gridx = 1; gbc.weightx = 1;
        formPanel.add(txtContactNumber, gbc);

        gbc.gridx = 0; gbc.gridy = 5; gbc.weightx = 0;
        formPanel.add(new JLabel("Address:"), gbc);
        gbc.gridx = 1; gbc.weightx = 1;
        formPanel.add(txtAddress, gbc);

        dialog.add(formPanel, BorderLayout.CENTER);

        // Buttons
        JPanel btnPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 10));
        JButton btnSave = LoginView.createPrimaryButton("Save");
        JButton btnCancel = LoginView.createSecondaryButton("Cancel");
        
        btnSave.addActionListener(e -> {
            String username = txtUsername.getText().trim();
            String password = new String(txtPassword.getPassword());
            String fullName = txtFullName.getText().trim();
            String email = txtEmail.getText().trim();
            String contactNumber = txtContactNumber.getText().trim();
            String address = txtAddress.getText().trim();
            if (username.isEmpty() || password.isEmpty() || fullName.isEmpty() || email.isEmpty() || contactNumber.isEmpty()) {
                JOptionPane.showMessageDialog(dialog, "Please fill all required fields!");
                return;
            }
            // Always create RESIDENT users from Users tab
            String msg = controller.createUser(username, password, fullName, email, contactNumber, address, "RESIDENT", -1);
            JOptionPane.showMessageDialog(dialog, msg);
            if (msg.contains("success")) {
                refreshUsers();
                dialog.dispose();
            }
        });
        
        btnCancel.addActionListener(e -> dialog.dispose());
        
        btnPanel.add(btnSave);
        btnPanel.add(btnCancel);
        dialog.add(btnPanel, BorderLayout.SOUTH);

        dialog.setVisible(true);
    }

    private void showEditUserDialog(int row) {
        int userId = (int) usersTableModel.getValueAt(row, 0);
        String username = usersTableModel.getValueAt(row, 1).toString();
        String currentPassword = usersTableModel.getValueAt(row, 2).toString();
        String fullName = usersTableModel.getValueAt(row, 3).toString();
        String email = usersTableModel.getValueAt(row, 4).toString();
        String userType = usersTableModel.getValueAt(row, 5).toString();

        JDialog dialog = new JDialog((Frame) SwingUtilities.getWindowAncestor(this), "Edit User", true);
        dialog.setLayout(new BorderLayout(10, 10));
        dialog.setSize(500, 400);
        dialog.setLocationRelativeTo(this);

        JPanel formPanel = new JPanel(new GridBagLayout());
        formPanel.setBorder(new EmptyBorder(20, 20, 20, 20));
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(8, 8, 8, 8);
        gbc.fill = GridBagConstraints.HORIZONTAL;

        JTextField txtUsername = new JTextField(username, 20);
        JTextField txtFullName = new JTextField(fullName, 20);
        JTextField txtEmail = new JTextField(email, 20);
        JTextField txtCurrentPassword = new JTextField(currentPassword, 20);
        JPasswordField txtNewPassword = new JPasswordField(20);

        gbc.gridx = 0; gbc.gridy = 0; gbc.weightx = 0;
        formPanel.add(new JLabel("Username:"), gbc);
        gbc.gridx = 1; gbc.weightx = 1;
        formPanel.add(txtUsername, gbc);

        gbc.gridx = 0; gbc.gridy = 1; gbc.weightx = 0;
        formPanel.add(new JLabel("Current Password:"), gbc);
        gbc.gridx = 1; gbc.weightx = 1;
        formPanel.add(txtCurrentPassword, gbc);

        gbc.gridx = 0; gbc.gridy = 2; gbc.weightx = 0;
        formPanel.add(new JLabel("Full Name:"), gbc);
        gbc.gridx = 1; gbc.weightx = 1;
        formPanel.add(txtFullName, gbc);

        gbc.gridx = 0; gbc.gridy = 3; gbc.weightx = 0;
        formPanel.add(new JLabel("Email:"), gbc);
        gbc.gridx = 1; gbc.weightx = 1;
        formPanel.add(txtEmail, gbc);

        gbc.gridx = 0; gbc.gridy = 4; gbc.weightx = 0;
        formPanel.add(new JLabel("New Password:"), gbc);
        gbc.gridx = 1; gbc.weightx = 1;
        formPanel.add(txtNewPassword, gbc);

        gbc.gridx = 0; gbc.gridy = 5; gbc.weightx = 0;
        formPanel.add(new JLabel("(Leave blank to keep current)"), gbc);

        dialog.add(formPanel, BorderLayout.CENTER);

        // Buttons
        JPanel btnPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 10));
        JButton btnUpdate = LoginView.createWarningButton("Update");
        JButton btnDelete = LoginView.createDangerButton("Delete");
        JButton btnCancel = LoginView.createSecondaryButton("Cancel");
        
        btnUpdate.addActionListener(e -> {
            // Use new password if provided, otherwise use current password
            String newPwd = new String(txtNewPassword.getPassword());
            String finalPassword = newPwd.isEmpty() ? txtCurrentPassword.getText().trim() : newPwd;
            
            String msg = controller.updateUser(userId, txtUsername.getText().trim(),
                txtFullName.getText().trim(), txtEmail.getText().trim(), 
                finalPassword);
            JOptionPane.showMessageDialog(dialog, msg);
            if (msg.contains("success")) {
                refreshUsers();
                dialog.dispose();
            }
        });
        
        btnDelete.addActionListener(e -> {
            if ("ADMIN".equals(userType)) {
                JOptionPane.showMessageDialog(dialog, "Cannot delete admin users!");
                return;
            }
            int confirm = JOptionPane.showConfirmDialog(dialog, "Delete this user?",
                "Confirm", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
            if (confirm == JOptionPane.YES_OPTION) {
                JOptionPane.showMessageDialog(dialog, controller.deleteUser(userId));
                refreshUsers();
                dialog.dispose();
            }
        });
        
        btnCancel.addActionListener(e -> dialog.dispose());
        
        btnPanel.add(btnUpdate);
        btnPanel.add(btnDelete);
        btnPanel.add(btnCancel);
        dialog.add(btnPanel, BorderLayout.SOUTH);

        dialog.setVisible(true);
    }

    // ==================== PHARMACY DIALOGS ====================

    private void showAddPharmacyDialog() {
        JDialog dialog = new JDialog((Frame) SwingUtilities.getWindowAncestor(this), "Add Pharmacy", true);
        dialog.setLayout(new BorderLayout(10, 10));
        dialog.setSize(550, 500);
        dialog.setLocationRelativeTo(this);

        JPanel formPanel = new JPanel(new GridBagLayout());
        formPanel.setBorder(new EmptyBorder(20, 20, 20, 20));
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(8, 8, 8, 8);
        gbc.fill = GridBagConstraints.HORIZONTAL;

        JTextField txtName = new JTextField(25);
        JTextField txtAddress = new JTextField(25);
        JTextField txtContact = new JTextField(25);
        JTextField txtEmail = new JTextField(25);
        JTextField txtDesc = new JTextField(25);
        JTextField txtUsername = new JTextField(25);
        JPasswordField txtPassword = new JPasswordField(25);

        gbc.gridx = 0; gbc.gridy = 0; gbc.weightx = 0;
        formPanel.add(new JLabel("Name:"), gbc);
        gbc.gridx = 1; gbc.weightx = 1;
        formPanel.add(txtName, gbc);

        gbc.gridx = 0; gbc.gridy = 1; gbc.weightx = 0;
        formPanel.add(new JLabel("Address:"), gbc);
        gbc.gridx = 1; gbc.weightx = 1;
        formPanel.add(txtAddress, gbc);

        gbc.gridx = 0; gbc.gridy = 2; gbc.weightx = 0;
        formPanel.add(new JLabel("Contact:"), gbc);
        gbc.gridx = 1; gbc.weightx = 1;
        formPanel.add(txtContact, gbc);

        gbc.gridx = 0; gbc.gridy = 3; gbc.weightx = 0;
        formPanel.add(new JLabel("Email:"), gbc);
        gbc.gridx = 1; gbc.weightx = 1;
        formPanel.add(txtEmail, gbc);

        gbc.gridx = 0; gbc.gridy = 4; gbc.weightx = 0;
        formPanel.add(new JLabel("Description:"), gbc);
        gbc.gridx = 1; gbc.weightx = 1;
        formPanel.add(txtDesc, gbc);

        // Login Credentials Section
        gbc.gridx = 0; gbc.gridy = 5; gbc.gridwidth = 2;
        formPanel.add(new JSeparator(), gbc);
        gbc.gridwidth = 1;

        gbc.gridx = 0; gbc.gridy = 6; gbc.gridwidth = 2;
        JLabel lblLogin = new JLabel("Login Credentials");
        lblLogin.setFont(new Font("SansSerif", Font.BOLD, 13));
        formPanel.add(lblLogin, gbc);
        gbc.gridwidth = 1;

        gbc.gridx = 0; gbc.gridy = 7; gbc.weightx = 0;
        formPanel.add(new JLabel("Username:"), gbc);
        gbc.gridx = 1; gbc.weightx = 1;
        formPanel.add(txtUsername, gbc);

        gbc.gridx = 0; gbc.gridy = 8; gbc.weightx = 0;
        formPanel.add(new JLabel("Password:"), gbc);
        gbc.gridx = 1; gbc.weightx = 1;
        formPanel.add(txtPassword, gbc);

        dialog.add(formPanel, BorderLayout.CENTER);

        // Buttons
        JPanel btnPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 10));
        JButton btnSave = LoginView.createPrimaryButton("Save");
        JButton btnCancel = LoginView.createSecondaryButton("Cancel");
        
        btnSave.addActionListener(e -> {
            String name = txtName.getText().trim();
            String address = txtAddress.getText().trim();
            String contact = txtContact.getText().trim();
            String email = txtEmail.getText().trim();
            String desc = txtDesc.getText().trim();
            String username = txtUsername.getText().trim();
            String password = new String(txtPassword.getPassword());
            
            if (name.isEmpty() || address.isEmpty() || email.isEmpty() || username.isEmpty() || password.isEmpty()) {
                JOptionPane.showMessageDialog(dialog, "Please fill all required fields (Name, Address, Email, Username, Password)!");
                return;
            }
            
            String msg = controller.createPharmacy(name, address, contact, email, desc, username, password);
            JOptionPane.showMessageDialog(dialog, msg);
            if (msg.contains("success")) {
                refreshPharmacies();
                dialog.dispose();
            }
        });
        
        btnCancel.addActionListener(e -> dialog.dispose());
        
        btnPanel.add(btnSave);
        btnPanel.add(btnCancel);
        dialog.add(btnPanel, BorderLayout.SOUTH);

        dialog.setVisible(true);
    }

    private void showEditPharmacyDialog(int row) {
        int pharmacyId = (int) pharmaciesTableModel.getValueAt(row, 0);
        String name = pharmaciesTableModel.getValueAt(row, 1).toString();
        String username = pharmaciesTableModel.getValueAt(row, 2).toString();
        String currentPassword = pharmaciesTableModel.getValueAt(row, 3).toString();
        String address = pharmaciesTableModel.getValueAt(row, 4).toString();
        String contact = pharmaciesTableModel.getValueAt(row, 5).toString();
        String email = pharmaciesTableModel.getValueAt(row, 6).toString();

        // Get linked pharmacist user ID
        Object[][] linkedUser = controller.getPharmacistUserByPharmacyId(pharmacyId);
        int linkedUserId = -1;
        if (linkedUser != null && linkedUser.length > 0) {
            linkedUserId = (int) linkedUser[0][0];
        }

        JDialog dialog = new JDialog((Frame) SwingUtilities.getWindowAncestor(this), "Edit Pharmacy & Login", true);
        dialog.setLayout(new BorderLayout(10, 10));
        dialog.setSize(550, 570);
        dialog.setLocationRelativeTo(this);

        JPanel formPanel = new JPanel(new GridBagLayout());
        formPanel.setBorder(new EmptyBorder(20, 20, 20, 20));
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(8, 8, 8, 8);
        gbc.fill = GridBagConstraints.HORIZONTAL;

        JTextField txtName = new JTextField(name, 25);
        JTextField txtAddress = new JTextField(address, 25);
        JTextField txtContact = new JTextField(contact, 25);
        JTextField txtEmail = new JTextField(email, 25);
        JTextField txtDesc = new JTextField(25);
        JTextField txtUsername = new JTextField(username, 25);
        JTextField txtCurrentPassword = new JTextField(currentPassword, 25);
        JPasswordField txtNewPassword = new JPasswordField(25);

        gbc.gridx = 0; gbc.gridy = 0; gbc.weightx = 0;
        formPanel.add(new JLabel("Name:"), gbc);
        gbc.gridx = 1; gbc.weightx = 1;
        formPanel.add(txtName, gbc);

        gbc.gridx = 0; gbc.gridy = 1; gbc.weightx = 0;
        formPanel.add(new JLabel("Address:"), gbc);
        gbc.gridx = 1; gbc.weightx = 1;
        formPanel.add(txtAddress, gbc);

        gbc.gridx = 0; gbc.gridy = 2; gbc.weightx = 0;
        formPanel.add(new JLabel("Contact:"), gbc);
        gbc.gridx = 1; gbc.weightx = 1;
        formPanel.add(txtContact, gbc);

        gbc.gridx = 0; gbc.gridy = 3; gbc.weightx = 0;
        formPanel.add(new JLabel("Email:"), gbc);
        gbc.gridx = 1; gbc.weightx = 1;
        formPanel.add(txtEmail, gbc);

        gbc.gridx = 0; gbc.gridy = 4; gbc.weightx = 0;
        formPanel.add(new JLabel("Description:"), gbc);
        gbc.gridx = 1; gbc.weightx = 1;
        formPanel.add(txtDesc, gbc);

        // Login Credentials Section
        gbc.gridx = 0; gbc.gridy = 5; gbc.gridwidth = 2;
        formPanel.add(new JSeparator(), gbc);
        gbc.gridwidth = 1;

        gbc.gridx = 0; gbc.gridy = 6; gbc.gridwidth = 2;
        JLabel lblLogin = new JLabel("Login Credentials");
        lblLogin.setFont(new Font("SansSerif", Font.BOLD, 13));
        formPanel.add(lblLogin, gbc);
        gbc.gridwidth = 1;

        gbc.gridx = 0; gbc.gridy = 7; gbc.weightx = 0;
        formPanel.add(new JLabel("Username:"), gbc);
        gbc.gridx = 1; gbc.weightx = 1;
        formPanel.add(txtUsername, gbc);

        gbc.gridx = 0; gbc.gridy = 8; gbc.weightx = 0;
        formPanel.add(new JLabel("Current Password:"), gbc);
        gbc.gridx = 1; gbc.weightx = 1;
        formPanel.add(txtCurrentPassword, gbc);

        gbc.gridx = 0; gbc.gridy = 9; gbc.weightx = 0;
        formPanel.add(new JLabel("New Password:"), gbc);
        gbc.gridx = 1; gbc.weightx = 1;
        formPanel.add(txtNewPassword, gbc);

        gbc.gridx = 0; gbc.gridy = 10; gbc.weightx = 0;
        formPanel.add(new JLabel("(Leave blank to keep current)"), gbc);

        dialog.add(formPanel, BorderLayout.CENTER);

        final int finalUserId = linkedUserId;

        // Buttons
        JPanel btnPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 10));
        JButton btnUpdate = LoginView.createWarningButton("Update");
        JButton btnDelete = LoginView.createDangerButton("Delete");
        JButton btnCancel = LoginView.createSecondaryButton("Cancel");
        
        btnUpdate.addActionListener(e -> {
            // Update pharmacy info
            String msg = controller.updatePharmacy(pharmacyId, 
                txtName.getText().trim(),
                txtAddress.getText().trim(),
                txtContact.getText().trim(),
                txtEmail.getText().trim(),
                txtDesc.getText().trim());
            
            // Update linked pharmacist user credentials if user exists
            if (finalUserId > 0) {
                // Use new password if provided, otherwise use current password
                String newPwd = new String(txtNewPassword.getPassword());
                String finalPassword = newPwd.isEmpty() ? txtCurrentPassword.getText().trim() : newPwd;
                
                String userMsg = controller.updateUser(finalUserId,
                    txtUsername.getText().trim(), // Can update username
                    txtName.getText().trim(), // Use pharmacy name as full name
                    txtEmail.getText().trim(),
                    finalPassword);
                if (!userMsg.contains("success")) {
                    msg += "\nWarning: " + userMsg;
                }
            }
            
            JOptionPane.showMessageDialog(dialog, msg);
            if (msg.contains("success")) {
                refreshPharmacies();
                dialog.dispose();
            }
        });
        
        btnDelete.addActionListener(e -> {
            int confirm = JOptionPane.showConfirmDialog(dialog, "Delete this pharmacy?",
                "Confirm", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
            if (confirm == JOptionPane.YES_OPTION) {
                JOptionPane.showMessageDialog(dialog, controller.deletePharmacy(pharmacyId));
                refreshPharmacies();
                dialog.dispose();
            }
        });
        
        btnCancel.addActionListener(e -> dialog.dispose());
        
        btnPanel.add(btnUpdate);
        btnPanel.add(btnDelete);
        btnPanel.add(btnCancel);
        dialog.add(btnPanel, BorderLayout.SOUTH);

        dialog.setVisible(true);
    }

    // ==================== ACTIONS ====================

    private void showUserApprovalDialog(int row) {
        int userId = (int) pendingUsersTableModel.getValueAt(row, 0);
        String username = pendingUsersTableModel.getValueAt(row, 1).toString();
        String fullName = pendingUsersTableModel.getValueAt(row, 2).toString();
        String email = pendingUsersTableModel.getValueAt(row, 3).toString();

        JDialog dialog = new JDialog((Frame) SwingUtilities.getWindowAncestor(this), "User Approval Request", true);
        dialog.setLayout(new BorderLayout(15, 15));
        dialog.setSize(450, 280);
        dialog.setLocationRelativeTo(this);

        JPanel infoPanel = new JPanel(new GridBagLayout());
        infoPanel.setBorder(new EmptyBorder(20, 20, 15, 20));
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(5, 5, 5, 5);
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.anchor = GridBagConstraints.WEST;

        gbc.gridx = 0; gbc.gridy = 0;
        JLabel lblTitle = new JLabel("Review User Account Request");
        lblTitle.setFont(new Font("SansSerif", Font.BOLD, 14));
        lblTitle.setForeground(new Color(25, 118, 210));
        infoPanel.add(lblTitle, gbc);

        gbc.gridy = 1;
        infoPanel.add(new JSeparator(), gbc);

        gbc.gridy = 2;
        infoPanel.add(new JLabel("<html><b>Full Name:</b> " + fullName + "</html>"), gbc);
        
        gbc.gridy = 3;
        infoPanel.add(new JLabel("<html><b>Username:</b> " + username + "</html>"), gbc);
        
        gbc.gridy = 4;
        infoPanel.add(new JLabel("<html><b>Email:</b> " + email + "</html>"), gbc);

        dialog.add(infoPanel, BorderLayout.CENTER);

        JPanel btnPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 15, 15));
        JButton btnAccept = LoginView.createSuccessButton("Accept Request");
        JButton btnReject = LoginView.createDangerButton("Reject Request");
        JButton btnCancel = LoginView.createSecondaryButton("Cancel");

        btnAccept.addActionListener(e -> {
            JOptionPane.showMessageDialog(dialog, controller.approveUser(userId));
            refreshNotifications();
            refreshUsers();
            dialog.dispose();
        });

        btnReject.addActionListener(e -> {
            int confirm = JOptionPane.showConfirmDialog(dialog, "Are you sure you want to reject this user?",
                "Confirm Rejection", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
            if (confirm == JOptionPane.YES_OPTION) {
                JOptionPane.showMessageDialog(dialog, controller.rejectUser(userId));
                refreshNotifications();
                dialog.dispose();
            }
        });

        btnCancel.addActionListener(e -> dialog.dispose());

        btnPanel.add(btnAccept);
        btnPanel.add(btnReject);
        btnPanel.add(btnCancel);
        dialog.add(btnPanel, BorderLayout.SOUTH);

        dialog.setVisible(true);
    }

    private void showPharmacyApprovalDialog(int row) {
        int pharmacyId = (int) pendingPharmaciesTableModel.getValueAt(row, 0);
        String name = pendingPharmaciesTableModel.getValueAt(row, 1).toString();
        String address = pendingPharmaciesTableModel.getValueAt(row, 2).toString();
        String contact = pendingPharmaciesTableModel.getValueAt(row, 3).toString();
        String email = pendingPharmaciesTableModel.getValueAt(row, 4).toString();

        JDialog dialog = new JDialog((Frame) SwingUtilities.getWindowAncestor(this), "Pharmacy Approval Request", true);
        dialog.setLayout(new BorderLayout(15, 15));
        dialog.setSize(500, 320);
        dialog.setLocationRelativeTo(this);

        JPanel infoPanel = new JPanel(new GridBagLayout());
        infoPanel.setBorder(new EmptyBorder(20, 20, 15, 20));
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(5, 5, 5, 5);
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.anchor = GridBagConstraints.WEST;

        gbc.gridx = 0; gbc.gridy = 0;
        JLabel lblTitle = new JLabel("Review Pharmacy Account Request");
        lblTitle.setFont(new Font("SansSerif", Font.BOLD, 14));
        lblTitle.setForeground(new Color(25, 118, 210));
        infoPanel.add(lblTitle, gbc);

        gbc.gridy = 1;
        infoPanel.add(new JSeparator(), gbc);

        gbc.gridy = 2;
        infoPanel.add(new JLabel("<html><b>Pharmacy Name:</b> " + name + "</html>"), gbc);
        
        gbc.gridy = 3;
        infoPanel.add(new JLabel("<html><b>Address:</b> " + address + "</html>"), gbc);
        
        gbc.gridy = 4;
        infoPanel.add(new JLabel("<html><b>Contact:</b> " + (contact.isEmpty() ? "Not provided" : contact) + "</html>"), gbc);
        
        gbc.gridy = 5;
        infoPanel.add(new JLabel("<html><b>Email:</b> " + email + "</html>"), gbc);

        dialog.add(infoPanel, BorderLayout.CENTER);

        JPanel btnPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 15, 15));
        JButton btnAccept = LoginView.createSuccessButton("Accept Request");
        JButton btnReject = LoginView.createDangerButton("Reject Request");
        JButton btnCancel = LoginView.createSecondaryButton("Cancel");

        btnAccept.addActionListener(e -> {
            JOptionPane.showMessageDialog(dialog, controller.approvePharmacy(pharmacyId));
            refreshNotifications();
            refreshPharmacies();
            dialog.dispose();
        });

        btnReject.addActionListener(e -> {
            int confirm = JOptionPane.showConfirmDialog(dialog, "Are you sure you want to reject this pharmacy?",
                "Confirm Rejection", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
            if (confirm == JOptionPane.YES_OPTION) {
                JOptionPane.showMessageDialog(dialog, controller.rejectPharmacy(pharmacyId));
                refreshNotifications();
                dialog.dispose();
            }
        });

        btnCancel.addActionListener(e -> dialog.dispose());

        btnPanel.add(btnAccept);
        btnPanel.add(btnReject);
        btnPanel.add(btnCancel);
        dialog.add(btnPanel, BorderLayout.SOUTH);

        dialog.setVisible(true);
    }

    private void acceptAllUsers() {
        int count = pendingUsersTableModel.getRowCount();
        if (count == 0) {
            JOptionPane.showMessageDialog(this, "No pending users to approve.");
            return;
        }
        
        int confirm = JOptionPane.showConfirmDialog(this, 
            "Are you sure you want to approve all " + count + " pending user(s)?",
            "Confirm Accept All", JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE);
        
        if (confirm == JOptionPane.YES_OPTION) {
            int approved = 0;
            for (int i = 0; i < count; i++) {
                int userId = (int) pendingUsersTableModel.getValueAt(i, 0);
                if (controller.approveUser(userId).contains("success")) {
                    approved++;
                }
            }
            JOptionPane.showMessageDialog(this, "Approved " + approved + " user(s) successfully!");
            refreshNotifications();
            refreshUsers();
        }
    }

    private void acceptAllPharmacies() {
        int count = pendingPharmaciesTableModel.getRowCount();
        if (count == 0) {
            JOptionPane.showMessageDialog(this, "No pending pharmacies to approve.");
            return;
        }
        
        int confirm = JOptionPane.showConfirmDialog(this, 
            "Are you sure you want to approve all " + count + " pending pharmac(ies)?",
            "Confirm Accept All", JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE);
        
        if (confirm == JOptionPane.YES_OPTION) {
            int approved = 0;
            for (int i = 0; i < count; i++) {
                int pharmacyId = (int) pendingPharmaciesTableModel.getValueAt(i, 0);
                if (controller.approvePharmacy(pharmacyId).contains("success")) {
                    approved++;
                }
            }
            JOptionPane.showMessageDialog(this, "Approved " + approved + " pharmac(ies) successfully!");
            refreshNotifications();
            refreshPharmacies();
        }
    }

    private void filterUsers() {
        String searchText = txtUserSearch.getText().toLowerCase();
        TableRowSorter<DefaultTableModel> sorter = new TableRowSorter<>(usersTableModel);
        usersTable.setRowSorter(sorter);
        if (searchText.trim().isEmpty()) {
            sorter.setRowFilter(null);
        } else {
            sorter.setRowFilter(RowFilter.regexFilter("(?i)" + searchText));
        }
    }

    private void filterPharmacies() {
        String searchText = txtPharmacySearch.getText().toLowerCase();
        TableRowSorter<DefaultTableModel> sorter = new TableRowSorter<>(pharmaciesTableModel);
        pharmaciesTable.setRowSorter(sorter);
        if (searchText.trim().isEmpty()) {
            sorter.setRowFilter(null);
        } else {
            sorter.setRowFilter(RowFilter.regexFilter("(?i)" + searchText));
        }
    }

    // ==================== REFRESH ====================

    private void refreshNotifications() {
        pendingUsersTableModel.setRowCount(0);
        for (Object[] row : controller.getPendingUsers()) pendingUsersTableModel.addRow(row);

        pendingPharmaciesTableModel.setRowCount(0);
        for (Object[] row : controller.getPendingPharmacies()) pendingPharmaciesTableModel.addRow(row);

        int totalPending = pendingUsersTableModel.getRowCount() + pendingPharmaciesTableModel.getRowCount();
        lblNotificationCount.setText(totalPending + " Pending");
    }

    private void refreshUsers() {
        usersTableModel.setRowCount(0);
        for (Object[] row : controller.getAllUsers()) usersTableModel.addRow(row);
    }

    private void refreshPharmacies() {
        pharmaciesTableModel.setRowCount(0);
        Object[][] data = controller.getAllPharmacies();
        for (Object[] row : data) pharmaciesTableModel.addRow(row);
    }

    private void refreshUserTransactions(int userId) {
        userTransactionsTableModel.setRowCount(0);
        for (Object[] row : controller.getUserReservations(userId)) {
            userTransactionsTableModel.addRow(row);
        }
    }

    private void refreshPharmacyMedicines(int pharmacyId) {
        pharmacyMedicinesTableModel.setRowCount(0);
        for (Object[] row : controller.getPharmacyMedicines(pharmacyId)) {
            pharmacyMedicinesTableModel.addRow(row);
        }
    }

    public void refresh() {
        refreshNotifications();
        refreshUsers();
        refreshPharmacies();
    }

    // ==================== TABLE HELPER ====================

    static JTable createStyledTable(DefaultTableModel model) {
        JTable table = new JTable(model);
        table.setRowHeight(28);
        table.setFont(new Font("SansSerif", Font.PLAIN, 12));
        table.setGridColor(new Color(230, 230, 230));
        table.setSelectionBackground(new Color(187, 222, 251));
        table.setSelectionForeground(Color.BLACK);
        table.getTableHeader().setFont(new Font("SansSerif", Font.BOLD, 12));
        table.getTableHeader().setBackground(new Color(236, 239, 241));
        table.getTableHeader().setForeground(new Color(55, 71, 79));
        table.getTableHeader().setBorder(BorderFactory.createMatteBorder(0, 0, 1, 0, new Color(200, 200, 200)));
        table.getTableHeader().setReorderingAllowed(false); // Disable column reordering
        table.setAutoResizeMode(JTable.AUTO_RESIZE_ALL_COLUMNS);
        return table;
    }
}
