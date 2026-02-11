package client.views;

import client.controllers.PharmacistController;
import javax.swing.*;
import javax.swing.border.*;
import javax.swing.table.*;
import java.awt.*;

/**
 * Pharmacist dashboard with clear separation of concerns:
 *
 *   Medicine tab layout:
 *   +--------------------------------------------------+
 *   | ADD NEW MEDICINE (form)                           |
 *   |  Brand: [___]   Generic: [___]                    |
 *   |  Dosage: [___]  Form: [v]  Price: [___] Qty:[___]|
 *   |  Category: [v]                                    |
 *   |           [Add Medicine]  [Clear Form]            |
 *   +--------------------------------------------------+
 *   | INVENTORY                                         |
 *   |  [Save Changes]  [Delete Selected]  [Refresh]     |
 *   |  +----------------------------------------------+ |
 *   |  | table ...                                    | |
 *   |  +----------------------------------------------+ |
 *   +--------------------------------------------------+
 *
 *   Reservations tab: Approve / Reject / Complete / Refresh
 */
public class PharmacistView extends JPanel {
    private final MainFrame mainFrame;
    private final PharmacistController controller;
    private int pharmacyId = -1;

    // Medicine form
    private JTextField txtBrandName, txtGenericName, txtDosage, txtPrice, txtQuantity;
    private JComboBox<String> cmbDosageForm, cmbCategory;
    private JTable medicinesTable;
    private DefaultTableModel medicinesTableModel;

    // Reservations
    private JTable reservationsTable;
    private DefaultTableModel reservationsTableModel;

    private JLabel lblPharmacyName;

    public PharmacistView(MainFrame mainFrame) {
        this.mainFrame = mainFrame;
        this.controller = mainFrame.getPharmacistController();
        initComponents();
    }

    public void setPharmacyId(int id) { this.pharmacyId = id; }

    private void initComponents() {
        setLayout(new BorderLayout());
        setBackground(new Color(245, 245, 245));

        // Top bar
        JPanel topBar = new JPanel(new BorderLayout());
        topBar.setBackground(new Color(46, 125, 50));
        topBar.setBorder(new EmptyBorder(12, 20, 12, 20));

        lblPharmacyName = new JLabel("Pharmacy Dashboard");
        lblPharmacyName.setFont(new Font("SansSerif", Font.BOLD, 18));
        lblPharmacyName.setForeground(Color.WHITE);
        topBar.add(lblPharmacyName, BorderLayout.WEST);

        JButton btnLogout = LoginView.createDangerButton("Logout");
        btnLogout.addActionListener(e -> mainFrame.handleLogout());
        topBar.add(btnLogout, BorderLayout.EAST);

        add(topBar, BorderLayout.NORTH);

        // Tabs
        JTabbedPane tabs = new JTabbedPane();
        tabs.setFont(new Font("SansSerif", Font.PLAIN, 13));
        tabs.setBorder(new EmptyBorder(10, 10, 10, 10));

        tabs.addTab("Medicine Inventory", createMedicinesPanel());
        tabs.addTab("Reservations", createReservationsPanel());

        add(tabs, BorderLayout.CENTER);
    }

    // ==================== MEDICINES PANEL ====================

    private JPanel createMedicinesPanel() {
        JPanel panel = new JPanel(new BorderLayout(10, 10));
        panel.setBorder(new EmptyBorder(15, 15, 15, 15));

        // ---- TOP: Add New Medicine form ----
        JPanel formPanel = new JPanel(new GridBagLayout());
        formPanel.setBorder(BorderFactory.createTitledBorder(
            BorderFactory.createLineBorder(new Color(200, 200, 200)),
            "Add New Medicine", TitledBorder.LEFT, TitledBorder.TOP,
            new Font("SansSerif", Font.BOLD, 13)));

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(4, 8, 4, 8);
        gbc.fill = GridBagConstraints.HORIZONTAL;

        // Row 0
        gbc.gridx = 0; gbc.gridy = 0; gbc.weightx = 0;
        formPanel.add(new JLabel("Brand Name:"), gbc);
        gbc.gridx = 1; gbc.weightx = 1;
        txtBrandName = new JTextField(15);
        formPanel.add(txtBrandName, gbc);

        gbc.gridx = 2; gbc.weightx = 0;
        formPanel.add(new JLabel("Generic Name:"), gbc);
        gbc.gridx = 3; gbc.weightx = 1;
        txtGenericName = new JTextField(15);
        formPanel.add(txtGenericName, gbc);

        // Row 1
        gbc.gridx = 0; gbc.gridy = 1; gbc.weightx = 0;
        formPanel.add(new JLabel("Dosage:"), gbc);
        gbc.gridx = 1; gbc.weightx = 1;
        txtDosage = new JTextField(15);
        formPanel.add(txtDosage, gbc);

        gbc.gridx = 2; gbc.weightx = 0;
        formPanel.add(new JLabel("Form:"), gbc);
        gbc.gridx = 3; gbc.weightx = 1;
        cmbDosageForm = new JComboBox<>(new String[]{
            "Tablet", "Capsule", "Syrup", "Injection", "Cream", "Drops", "Softgel"});
        formPanel.add(cmbDosageForm, gbc);

        // Row 2
        gbc.gridx = 0; gbc.gridy = 2; gbc.weightx = 0;
        formPanel.add(new JLabel("Price:"), gbc);
        gbc.gridx = 1; gbc.weightx = 1;
        txtPrice = new JTextField(15);
        formPanel.add(txtPrice, gbc);

        gbc.gridx = 2; gbc.weightx = 0;
        formPanel.add(new JLabel("Quantity:"), gbc);
        gbc.gridx = 3; gbc.weightx = 1;
        txtQuantity = new JTextField(15);
        formPanel.add(txtQuantity, gbc);

        // Row 3 - Category
        gbc.gridx = 0; gbc.gridy = 3; gbc.weightx = 0;
        formPanel.add(new JLabel("Category:"), gbc);
        gbc.gridx = 1; gbc.weightx = 1;
        cmbCategory = new JComboBox<>(new String[]{
            "OTC Drugs", "Prescription", "Vitamins", "Personal Care"});
        formPanel.add(cmbCategory, gbc);

        JLabel lblCatHint = new JLabel("('Prescription' auto-sets requires prescription)");
        lblCatHint.setFont(new Font("SansSerif", Font.ITALIC, 11));
        lblCatHint.setForeground(Color.GRAY);
        gbc.gridx = 2; gbc.gridwidth = 2;
        formPanel.add(lblCatHint, gbc);
        gbc.gridwidth = 1;

        // Row 4 - Form buttons: Add + Clear only
        gbc.gridx = 0; gbc.gridy = 4; gbc.gridwidth = 4;
        gbc.fill = GridBagConstraints.NONE;
        gbc.anchor = GridBagConstraints.EAST;
        JPanel formBtnPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 4));
        JButton btnAdd = LoginView.createPrimaryButton("Add Medicine");
        JButton btnClear = LoginView.createSecondaryButton("Clear Form");
        btnAdd.addActionListener(e -> addMedicine());
        btnClear.addActionListener(e -> clearMedicineFields());
        formBtnPanel.add(btnAdd);
        formBtnPanel.add(btnClear);
        formPanel.add(formBtnPanel, gbc);

        panel.add(formPanel, BorderLayout.NORTH);

        // ---- BOTTOM: Inventory table with its own buttons ----
        JPanel inventoryPanel = new JPanel(new BorderLayout(5, 5));
        inventoryPanel.setBorder(BorderFactory.createTitledBorder(
            BorderFactory.createLineBorder(new Color(200, 200, 200)),
            "Inventory", TitledBorder.LEFT, TitledBorder.TOP,
            new Font("SansSerif", Font.BOLD, 13)));

        // Inventory action buttons
        JPanel invBtnPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 4));
        JButton btnUpdate = LoginView.createWarningButton("Save Changes");
        JButton btnDelete = LoginView.createDangerButton("Delete Selected");
        JButton btnRefresh = LoginView.createSecondaryButton("Refresh");

        btnUpdate.setToolTipText("Select a row, edit the form above, then click Save Changes");
        btnDelete.setToolTipText("Select a row and delete it from inventory");

        btnUpdate.addActionListener(e -> updateMedicine());
        btnDelete.addActionListener(e -> deleteMedicine());
        btnRefresh.addActionListener(e -> refreshMedicines());

        invBtnPanel.add(btnUpdate);
        invBtnPanel.add(btnDelete);
        invBtnPanel.add(btnRefresh);

        JLabel lblEditHint = new JLabel("Click a row to load it into the form above for editing");
        lblEditHint.setFont(new Font("SansSerif", Font.ITALIC, 11));
        lblEditHint.setForeground(Color.GRAY);
        invBtnPanel.add(lblEditHint);

        inventoryPanel.add(invBtnPanel, BorderLayout.NORTH);

        // Inventory table
        String[] cols = {"ID", "Brand Name", "Generic Name", "Dosage", "Form",
                         "Price", "Qty", "Reserved", "Category", "Status"};
        medicinesTableModel = new DefaultTableModel(cols, 0) {
            public boolean isCellEditable(int r, int c) { return false; }
        };
        medicinesTable = AdminView.createStyledTable(medicinesTableModel);
        medicinesTable.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseClicked(java.awt.event.MouseEvent e) {
                int row = medicinesTable.getSelectedRow();
                if (row >= 0) {
                    txtBrandName.setText(str(medicinesTableModel.getValueAt(row, 1)));
                    txtGenericName.setText(str(medicinesTableModel.getValueAt(row, 2)));
                    txtDosage.setText(str(medicinesTableModel.getValueAt(row, 3)));
                    cmbDosageForm.setSelectedItem(str(medicinesTableModel.getValueAt(row, 4)));
                    txtPrice.setText(str(medicinesTableModel.getValueAt(row, 5)));
                    txtQuantity.setText(str(medicinesTableModel.getValueAt(row, 6)));
                    cmbCategory.setSelectedItem(str(medicinesTableModel.getValueAt(row, 8)));
                }
            }
        });

        JScrollPane scroll = new JScrollPane(medicinesTable);
        scroll.setBorder(BorderFactory.createLineBorder(new Color(200, 200, 200)));
        inventoryPanel.add(scroll, BorderLayout.CENTER);

        panel.add(inventoryPanel, BorderLayout.CENTER);

        return panel;
    }

    // ==================== RESERVATIONS PANEL ====================

    private JPanel createReservationsPanel() {
        JPanel panel = new JPanel(new BorderLayout(10, 10));
        panel.setBorder(new EmptyBorder(15, 15, 15, 15));

        // Separate action buttons
        JPanel topPanel = new JPanel(new BorderLayout());

        JPanel btnPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        JButton btnApprove = LoginView.createSuccessButton("Approve");
        JButton btnReject = LoginView.createDangerButton("Reject");
        JButton btnComplete = LoginView.createPrimaryButton("Complete Pickup");
        JButton btnRefresh = LoginView.createSecondaryButton("Refresh");

        btnApprove.setToolTipText("Approve pending reservations (Pay at Store)");
        btnReject.setToolTipText("Reject a reservation (refund if paid online)");
        btnComplete.setToolTipText("Confirm customer picked up the medicine");

        btnApprove.addActionListener(e -> approveReservation());
        btnReject.addActionListener(e -> rejectReservation());
        btnComplete.addActionListener(e -> completeReservation());
        btnRefresh.addActionListener(e -> refreshReservations());

        btnPanel.add(btnApprove);
        btnPanel.add(btnReject);
        btnPanel.add(btnComplete);
        btnPanel.add(btnRefresh);
        topPanel.add(btnPanel, BorderLayout.WEST);

        JLabel lblHelp = new JLabel("24h expiry: unretrieved reservations auto-expire and refund if paid");
        lblHelp.setFont(new Font("SansSerif", Font.ITALIC, 11));
        lblHelp.setForeground(new Color(120, 120, 120));
        lblHelp.setBorder(new EmptyBorder(0, 0, 0, 10));
        topPanel.add(lblHelp, BorderLayout.EAST);

        panel.add(topPanel, BorderLayout.NORTH);

        // Reservations table
        String[] cols = {"ID", "Customer", "Medicine", "Qty", "Total",
                         "Payment", "Pay Status", "Reserved At", "Status"};
        reservationsTableModel = new DefaultTableModel(cols, 0) {
            public boolean isCellEditable(int r, int c) { return false; }
        };
        reservationsTable = AdminView.createStyledTable(reservationsTableModel);
        JScrollPane scroll = new JScrollPane(reservationsTable);
        scroll.setBorder(BorderFactory.createLineBorder(new Color(200, 200, 200)));
        panel.add(scroll, BorderLayout.CENTER);

        return panel;
    }

    // ==================== MEDICINE ACTIONS ====================

    private void addMedicine() {
        if (!validateMedicineForm()) return;
        try {
            String msg = controller.addMedicine(pharmacyId,
                txtBrandName.getText().trim(), txtGenericName.getText().trim(),
                txtDosage.getText().trim(), (String) cmbDosageForm.getSelectedItem(),
                Double.parseDouble(txtPrice.getText().trim()),
                Integer.parseInt(txtQuantity.getText().trim()),
                (String) cmbCategory.getSelectedItem());
            JOptionPane.showMessageDialog(this, msg);
            clearMedicineFields();
            refreshMedicines();
        } catch (NumberFormatException ex) {
            JOptionPane.showMessageDialog(this, "Please enter valid numbers for price and quantity!",
                "Validation Error", JOptionPane.WARNING_MESSAGE);
        }
    }

    private void updateMedicine() {
        int row = medicinesTable.getSelectedRow();
        if (row < 0) {
            JOptionPane.showMessageDialog(this,
                "Select a medicine from the inventory table first,\n" +
                "edit the values in the form above, then click Save Changes.",
                "No Selection", JOptionPane.INFORMATION_MESSAGE);
            return;
        }
        if (!validateMedicineForm()) return;
        try {
            int medId = (int) medicinesTableModel.getValueAt(row, 0);
            String msg = controller.updateMedicine(medId,
                txtBrandName.getText().trim(), txtGenericName.getText().trim(),
                txtDosage.getText().trim(), (String) cmbDosageForm.getSelectedItem(),
                Double.parseDouble(txtPrice.getText().trim()),
                Integer.parseInt(txtQuantity.getText().trim()),
                (String) cmbCategory.getSelectedItem());
            JOptionPane.showMessageDialog(this, msg);
            clearMedicineFields();
            refreshMedicines();
        } catch (NumberFormatException ex) {
            JOptionPane.showMessageDialog(this, "Please enter valid numbers for price and quantity!",
                "Validation Error", JOptionPane.WARNING_MESSAGE);
        }
    }

    private void deleteMedicine() {
        int row = medicinesTable.getSelectedRow();
        if (row < 0) {
            JOptionPane.showMessageDialog(this, "Select a medicine from the inventory table first!",
                "No Selection", JOptionPane.INFORMATION_MESSAGE);
            return;
        }
        int confirm = JOptionPane.showConfirmDialog(this,
            "Are you sure you want to delete this medicine?",
            "Confirm Delete", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
        if (confirm == JOptionPane.YES_OPTION) {
            int medId = (int) medicinesTableModel.getValueAt(row, 0);
            JOptionPane.showMessageDialog(this, controller.deleteMedicine(medId, pharmacyId));
            clearMedicineFields();
            refreshMedicines();
        }
    }

    private boolean validateMedicineForm() {
        if (txtBrandName.getText().trim().isEmpty() || txtGenericName.getText().trim().isEmpty() ||
            txtDosage.getText().trim().isEmpty() || txtPrice.getText().trim().isEmpty() ||
            txtQuantity.getText().trim().isEmpty()) {
            JOptionPane.showMessageDialog(this, "Please fill all required fields!",
                "Validation Error", JOptionPane.WARNING_MESSAGE);
            return false;
        }
        return true;
    }

    private void clearMedicineFields() {
        txtBrandName.setText(""); txtGenericName.setText("");
        txtDosage.setText(""); txtPrice.setText(""); txtQuantity.setText("");
        cmbDosageForm.setSelectedIndex(0); cmbCategory.setSelectedIndex(0);
        medicinesTable.clearSelection();
    }

    // ==================== RESERVATION ACTIONS ====================

    private void approveReservation() {
        int row = reservationsTable.getSelectedRow();
        if (row < 0) { JOptionPane.showMessageDialog(this, "Select a reservation!"); return; }
        String status = str(reservationsTableModel.getValueAt(row, 8));
        if (!"PENDING".equals(status)) {
            JOptionPane.showMessageDialog(this, "Only PENDING reservations can be approved!",
                "Invalid Action", JOptionPane.WARNING_MESSAGE);
            return;
        }
        int id = (int) reservationsTableModel.getValueAt(row, 0);
        JOptionPane.showMessageDialog(this, controller.approveReservation(id));
        refreshReservations();
    }

    private void rejectReservation() {
        int row = reservationsTable.getSelectedRow();
        if (row < 0) { JOptionPane.showMessageDialog(this, "Select a reservation!"); return; }
        String status = str(reservationsTableModel.getValueAt(row, 8));
        if (!"PENDING".equals(status) && !"CONFIRMED".equals(status)) {
            JOptionPane.showMessageDialog(this, "Only PENDING or CONFIRMED reservations can be rejected!",
                "Invalid Action", JOptionPane.WARNING_MESSAGE);
            return;
        }
        int confirm = JOptionPane.showConfirmDialog(this,
            "Are you sure you want to reject this reservation?\nIf paid, it will be refunded.",
            "Confirm Reject", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
        if (confirm == JOptionPane.YES_OPTION) {
            int id = (int) reservationsTableModel.getValueAt(row, 0);
            JOptionPane.showMessageDialog(this, controller.rejectReservation(id));
            refreshReservations();
            refreshMedicines();
        }
    }

    private void completeReservation() {
        int row = reservationsTable.getSelectedRow();
        if (row < 0) { JOptionPane.showMessageDialog(this, "Select a reservation!"); return; }
        String status = str(reservationsTableModel.getValueAt(row, 8));
        if (!"CONFIRMED".equals(status)) {
            JOptionPane.showMessageDialog(this, "Only CONFIRMED reservations can be completed!",
                "Invalid Action", JOptionPane.WARNING_MESSAGE);
            return;
        }
        int id = (int) reservationsTableModel.getValueAt(row, 0);
        JOptionPane.showMessageDialog(this, controller.completeReservation(id));
        refreshReservations();
        refreshMedicines();
    }

    // ==================== REFRESH ====================

    private void refreshMedicines() {
        medicinesTableModel.setRowCount(0);
        for (Object[] row : controller.getMedicines(pharmacyId))
            medicinesTableModel.addRow(row);
    }

    private void refreshReservations() {
        reservationsTableModel.setRowCount(0);
        for (Object[] row : controller.getReservations(pharmacyId))
            reservationsTableModel.addRow(row);
    }

    public void refresh() {
        lblPharmacyName.setText("Pharmacy Dashboard (ID: " + pharmacyId + ")");
        refreshMedicines();
        refreshReservations();
    }

    private String str(Object o) { return o != null ? o.toString() : ""; }
}
