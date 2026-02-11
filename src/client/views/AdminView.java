package client.views;

import client.controllers.AdminController;
import javax.swing.*;
import javax.swing.border.*;
import javax.swing.table.*;
import java.awt.*;

/**
 * Admin dashboard: manage users, pharmacies, view all medicines & reservations.
 */
public class AdminView extends JPanel {
    private final MainFrame mainFrame;
    private final AdminController controller;

    // User Management
    private JTextField txtUsername, txtFullName, txtEmail;
    private JPasswordField txtPassword;
    private JComboBox<String> cmbUserType, cmbPharmacy;
    private JTable usersTable;
    private DefaultTableModel usersTableModel;

    // Pharmacy Management
    private JTable pharmaciesTable;
    private DefaultTableModel pharmaciesTableModel;

    // All Medicines
    private JTable medicinesTable;
    private DefaultTableModel medicinesTableModel;

    // All Reservations
    private JTable reservationsTable;
    private DefaultTableModel reservationsTableModel;

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

        JLabel lblTitle = new JLabel("Admin Dashboard");
        lblTitle.setFont(new Font("SansSerif", Font.BOLD, 18));
        lblTitle.setForeground(Color.WHITE);
        topBar.add(lblTitle, BorderLayout.WEST);

        JButton btnLogout = LoginView.createDangerButton("Logout");
        btnLogout.addActionListener(e -> mainFrame.handleLogout());
        topBar.add(btnLogout, BorderLayout.EAST);

        add(topBar, BorderLayout.NORTH);

        // Tabs
        JTabbedPane tabs = new JTabbedPane();
        tabs.setFont(new Font("SansSerif", Font.PLAIN, 13));
        tabs.setBorder(new EmptyBorder(10, 10, 10, 10));

        tabs.addTab("Users", createUserPanel());
        tabs.addTab("Pharmacies", createPharmacyPanel());
        tabs.addTab("All Medicines", createMedicinesPanel());
        tabs.addTab("All Reservations", createReservationsPanel());

        add(tabs, BorderLayout.CENTER);
    }

    // ==================== USER MANAGEMENT ====================

    private JPanel createUserPanel() {
        JPanel panel = new JPanel(new BorderLayout(10, 10));
        panel.setBorder(new EmptyBorder(15, 15, 15, 15));

        // Form
        JPanel formPanel = new JPanel(new GridBagLayout());
        formPanel.setBorder(BorderFactory.createTitledBorder(
            BorderFactory.createLineBorder(new Color(200, 200, 200)),
            "Add / Edit User", TitledBorder.LEFT, TitledBorder.TOP,
            new Font("SansSerif", Font.BOLD, 13)));
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(4, 8, 4, 8);
        gbc.fill = GridBagConstraints.HORIZONTAL;

        gbc.gridx = 0; gbc.gridy = 0;
        formPanel.add(new JLabel("Username:"), gbc);
        gbc.gridx = 1; gbc.weightx = 1;
        txtUsername = new JTextField(15);
        formPanel.add(txtUsername, gbc);

        gbc.gridx = 2; gbc.weightx = 0;
        formPanel.add(new JLabel("Password:"), gbc);
        gbc.gridx = 3; gbc.weightx = 1;
        txtPassword = new JPasswordField(15);
        formPanel.add(txtPassword, gbc);

        gbc.gridx = 0; gbc.gridy = 1; gbc.weightx = 0;
        formPanel.add(new JLabel("Full Name:"), gbc);
        gbc.gridx = 1; gbc.weightx = 1;
        txtFullName = new JTextField(15);
        formPanel.add(txtFullName, gbc);

        gbc.gridx = 2; gbc.weightx = 0;
        formPanel.add(new JLabel("Email:"), gbc);
        gbc.gridx = 3; gbc.weightx = 1;
        txtEmail = new JTextField(15);
        formPanel.add(txtEmail, gbc);

        gbc.gridx = 0; gbc.gridy = 2; gbc.weightx = 0;
        formPanel.add(new JLabel("User Type:"), gbc);
        gbc.gridx = 1;
        cmbUserType = new JComboBox<>(new String[]{"RESIDENT", "PHARMACIST"});
        formPanel.add(cmbUserType, gbc);

        gbc.gridx = 2;
        formPanel.add(new JLabel("Pharmacy:"), gbc);
        gbc.gridx = 3;
        cmbPharmacy = new JComboBox<>();
        cmbPharmacy.setEnabled(false);
        formPanel.add(cmbPharmacy, gbc);

        cmbUserType.addActionListener(e ->
            cmbPharmacy.setEnabled("PHARMACIST".equals(cmbUserType.getSelectedItem())));

        // Buttons
        gbc.gridx = 0; gbc.gridy = 3; gbc.gridwidth = 4;
        gbc.fill = GridBagConstraints.NONE;
        gbc.anchor = GridBagConstraints.WEST;
        JPanel btnPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        JButton btnAdd = LoginView.createPrimaryButton("Add User");
        JButton btnUpdate = LoginView.createWarningButton("Update");
        JButton btnDelete = LoginView.createDangerButton("Delete");
        JButton btnClear = LoginView.createSecondaryButton("Clear");
        btnAdd.addActionListener(e -> addUser());
        btnUpdate.addActionListener(e -> updateUser());
        btnDelete.addActionListener(e -> deleteUser());
        btnClear.addActionListener(e -> clearUserFields());
        btnPanel.add(btnAdd); btnPanel.add(btnUpdate);
        btnPanel.add(btnDelete); btnPanel.add(btnClear);
        formPanel.add(btnPanel, gbc);

        panel.add(formPanel, BorderLayout.NORTH);

        // Table
        String[] cols = {"ID", "Username", "Full Name", "Email", "Type", "Status"};
        usersTableModel = new DefaultTableModel(cols, 0) {
            public boolean isCellEditable(int r, int c) { return false; }
        };
        usersTable = createStyledTable(usersTableModel);
        usersTable.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseClicked(java.awt.event.MouseEvent e) {
                int row = usersTable.getSelectedRow();
                if (row >= 0) {
                    txtUsername.setText(usersTableModel.getValueAt(row, 1).toString());
                    txtFullName.setText(usersTableModel.getValueAt(row, 2).toString());
                    txtEmail.setText(usersTableModel.getValueAt(row, 3).toString());
                    txtPassword.setText("");
                }
            }
        });

        JScrollPane scroll = new JScrollPane(usersTable);
        scroll.setBorder(BorderFactory.createLineBorder(new Color(200, 200, 200)));
        panel.add(scroll, BorderLayout.CENTER);

        return panel;
    }

    // ==================== PHARMACY MANAGEMENT ====================

    private JPanel createPharmacyPanel() {
        JPanel panel = new JPanel(new BorderLayout(10, 10));
        panel.setBorder(new EmptyBorder(15, 15, 15, 15));

        JPanel btnPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        JButton btnApprove = LoginView.createSuccessButton("Approve");
        JButton btnReject = LoginView.createDangerButton("Reject");
        JButton btnRefresh = LoginView.createSecondaryButton("Refresh");
        btnApprove.addActionListener(e -> approvePharmacy());
        btnReject.addActionListener(e -> rejectPharmacy());
        btnRefresh.addActionListener(e -> refreshPharmacies());
        btnPanel.add(btnApprove); btnPanel.add(btnReject); btnPanel.add(btnRefresh);
        panel.add(btnPanel, BorderLayout.NORTH);

        String[] cols = {"ID", "Name", "Address", "Contact", "Email", "Status"};
        pharmaciesTableModel = new DefaultTableModel(cols, 0) {
            public boolean isCellEditable(int r, int c) { return false; }
        };
        pharmaciesTable = createStyledTable(pharmaciesTableModel);
        JScrollPane scroll = new JScrollPane(pharmaciesTable);
        scroll.setBorder(BorderFactory.createLineBorder(new Color(200, 200, 200)));
        panel.add(scroll, BorderLayout.CENTER);

        return panel;
    }

    // ==================== ALL MEDICINES ====================

    private JPanel createMedicinesPanel() {
        JPanel panel = new JPanel(new BorderLayout(10, 10));
        panel.setBorder(new EmptyBorder(15, 15, 15, 15));

        JPanel btnPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        JButton btnRefresh = LoginView.createSecondaryButton("Refresh");
        btnRefresh.addActionListener(e -> refreshMedicines());
        btnPanel.add(btnRefresh);
        panel.add(btnPanel, BorderLayout.NORTH);

        String[] cols = {"ID", "Brand Name", "Generic Name", "Dosage", "Form", "Price", "Qty", "Category", "Pharmacy", "Status"};
        medicinesTableModel = new DefaultTableModel(cols, 0) {
            public boolean isCellEditable(int r, int c) { return false; }
        };
        medicinesTable = createStyledTable(medicinesTableModel);
        JScrollPane scroll = new JScrollPane(medicinesTable);
        scroll.setBorder(BorderFactory.createLineBorder(new Color(200, 200, 200)));
        panel.add(scroll, BorderLayout.CENTER);

        return panel;
    }

    // ==================== ALL RESERVATIONS ====================

    private JPanel createReservationsPanel() {
        JPanel panel = new JPanel(new BorderLayout(10, 10));
        panel.setBorder(new EmptyBorder(15, 15, 15, 15));

        JPanel btnPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        JButton btnRefresh = LoginView.createSecondaryButton("Refresh");
        btnRefresh.addActionListener(e -> refreshReservations());
        btnPanel.add(btnRefresh);
        panel.add(btnPanel, BorderLayout.NORTH);

        String[] cols = {"ID", "Customer", "Medicine", "Pharmacy", "Qty", "Total", "Payment", "Pay Status", "Status"};
        reservationsTableModel = new DefaultTableModel(cols, 0) {
            public boolean isCellEditable(int r, int c) { return false; }
        };
        reservationsTable = createStyledTable(reservationsTableModel);
        JScrollPane scroll = new JScrollPane(reservationsTable);
        scroll.setBorder(BorderFactory.createLineBorder(new Color(200, 200, 200)));
        panel.add(scroll, BorderLayout.CENTER);

        return panel;
    }

    // ==================== ACTIONS ====================

    private void addUser() {
        String username = txtUsername.getText().trim();
        String password = new String(txtPassword.getPassword());
        String fullName = txtFullName.getText().trim();
        String email = txtEmail.getText().trim();
        if (username.isEmpty() || password.isEmpty() || fullName.isEmpty() || email.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Please fill all fields!"); return;
        }
        String userType = (String) cmbUserType.getSelectedItem();
        int pharmacyId = -1;
        if ("PHARMACIST".equals(userType) && cmbPharmacy.getSelectedItem() != null) {
            String item = cmbPharmacy.getSelectedItem().toString();
            try { pharmacyId = Integer.parseInt(item.split(":")[0].trim()); } catch (Exception ignored) {}
        }
        String msg = controller.createUser(username, password, fullName, email, userType, pharmacyId);
        JOptionPane.showMessageDialog(this, msg);
        clearUserFields();
        refreshUsers();
    }

    private void updateUser() {
        int row = usersTable.getSelectedRow();
        if (row < 0) { JOptionPane.showMessageDialog(this, "Select a user first!"); return; }
        int userId = (int) usersTableModel.getValueAt(row, 0);
        String msg = controller.updateUser(userId, txtFullName.getText().trim(),
            txtEmail.getText().trim(), new String(txtPassword.getPassword()));
        JOptionPane.showMessageDialog(this, msg);
        clearUserFields();
        refreshUsers();
    }

    private void deleteUser() {
        int row = usersTable.getSelectedRow();
        if (row < 0) { JOptionPane.showMessageDialog(this, "Select a user first!"); return; }
        if ("ADMIN".equals(usersTableModel.getValueAt(row, 4))) {
            JOptionPane.showMessageDialog(this, "Cannot delete admin users!"); return;
        }
        int confirm = JOptionPane.showConfirmDialog(this, "Are you sure you want to delete this user?",
            "Confirm Delete", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
        if (confirm == JOptionPane.YES_OPTION) {
            int userId = (int) usersTableModel.getValueAt(row, 0);
            JOptionPane.showMessageDialog(this, controller.deleteUser(userId));
            clearUserFields();
            refreshUsers();
        }
    }

    private void clearUserFields() {
        txtUsername.setText(""); txtPassword.setText("");
        txtFullName.setText(""); txtEmail.setText("");
        usersTable.clearSelection();
    }

    private void approvePharmacy() {
        int row = pharmaciesTable.getSelectedRow();
        if (row < 0) { JOptionPane.showMessageDialog(this, "Select a pharmacy!"); return; }
        int id = (int) pharmaciesTableModel.getValueAt(row, 0);
        JOptionPane.showMessageDialog(this, controller.approvePharmacy(id));
        refreshPharmacies();
    }

    private void rejectPharmacy() {
        int row = pharmaciesTable.getSelectedRow();
        if (row < 0) { JOptionPane.showMessageDialog(this, "Select a pharmacy!"); return; }
        int id = (int) pharmaciesTableModel.getValueAt(row, 0);
        JOptionPane.showMessageDialog(this, controller.rejectPharmacy(id));
        refreshPharmacies();
    }

    // ==================== REFRESH ====================

    private void refreshUsers() {
        usersTableModel.setRowCount(0);
        for (Object[] row : controller.getAllUsers()) usersTableModel.addRow(row);
    }

    private void refreshPharmacies() {
        pharmaciesTableModel.setRowCount(0);
        Object[][] data = controller.getAllPharmacies();
        for (Object[] row : data) pharmaciesTableModel.addRow(row);
        // Update pharmacy dropdown
        cmbPharmacy.removeAllItems();
        for (Object[] row : data) {
            cmbPharmacy.addItem(row[0] + ": " + row[1]);
        }
    }

    private void refreshMedicines() {
        medicinesTableModel.setRowCount(0);
        for (Object[] row : controller.getAllMedicines()) medicinesTableModel.addRow(row);
    }

    private void refreshReservations() {
        reservationsTableModel.setRowCount(0);
        for (Object[] row : controller.getAllReservations()) reservationsTableModel.addRow(row);
    }

    public void refresh() {
        refreshUsers();
        refreshPharmacies();
        refreshMedicines();
        refreshReservations();
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
        table.setAutoResizeMode(JTable.AUTO_RESIZE_ALL_COLUMNS);
        return table;
    }
}
