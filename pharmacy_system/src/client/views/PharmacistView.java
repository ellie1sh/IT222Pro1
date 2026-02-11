package client.views;

import client.controllers.PharmacistController;
import server.DataStore;
import models.Reservation;
import models.Medicine;
import models.User;
import models.PrescriptionRequest;
import models.Pharmacy;
import javax.swing.*;
import javax.swing.border.*;
import javax.swing.table.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;

import javax.imageio.ImageIO;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.Duration;
import java.time.format.DateTimeFormatter;

/**
 * Pharmacist dashboard with clear separation of concerns:
 *
 *   Medicine tab: Search bar + Add Product button
 *
 *   Reservations tab: Approve / Reject / Complete / Refresh
 */
public class PharmacistView extends JPanel {
    private final MainFrame mainFrame;
    private final PharmacistController controller;
    private int pharmacyId = -1;

    // Medicine Inventory
    private JTable medicinesTable;
    private DefaultTableModel medicinesTableModel;
    private JTextField medicinesSearchField;
    private JComboBox<String> medicinesFilterCombo;

    // Reservations
    private JTable reservationsTable;
    private DefaultTableModel reservationsTableModel;

    private JLabel lblPharmacyName;
    
    // Dashboard labels
    private JLabel dashboardSalesLabel;
    private JLabel dashboardPendingLabel;
    private JLabel dashboardPickupLabel;
    private JLabel dashboardNotificationsLabel;
    
    // Dashboard cards
    private JPanel salesCard;
    private JPanel pendingCard;
    private JPanel pickupCard;
    private JPanel notificationsCard;
    
    // Tabs
    private JTabbedPane tabs;
    
    // Dashboard content panel
    private JPanel dashboardContentPanel;
    private String currentDashboardView = null;

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
        topBar.setBackground(new Color(25, 118, 210));
        topBar.setBorder(new EmptyBorder(12, 20, 12, 20));

        lblPharmacyName = new JLabel("RxPress");
        lblPharmacyName.setFont(new Font("SansSerif", Font.BOLD, 28));
        lblPharmacyName.setForeground(Color.WHITE);
        topBar.add(lblPharmacyName, BorderLayout.WEST);

        JButton btnLogout = LoginView.createDangerButton("Logout");
        btnLogout.addActionListener(e -> mainFrame.handleLogout());
        topBar.add(btnLogout, BorderLayout.EAST);

        add(topBar, BorderLayout.NORTH);

        // Tabs
        tabs = new JTabbedPane();
        tabs.setFont(new Font("SansSerif", Font.PLAIN, 13));
        tabs.setBorder(new EmptyBorder(10, 10, 10, 10));

        tabs.addTab("Dashboard", createDashboardPanel());
        tabs.addTab("Medicine Inventory", createMedicinesPanel());

        // Set Dashboard as the default selected tab
        tabs.setSelectedIndex(0);

        add(tabs, BorderLayout.CENTER);
    }

    // ==================== DASHBOARD PANEL ====================

    private JPanel createDashboardPanel() {
        JPanel panel = new JPanel(new BorderLayout(10, 10));
        panel.setBorder(new EmptyBorder(15, 15, 15, 15));
        panel.setBackground(new Color(245, 245, 245));

        // Metrics cards panel
        JPanel metricsPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 15, 0));
        metricsPanel.setBackground(new Color(245, 245, 245));

        // 1. Notifications
        dashboardNotificationsLabel = new JLabel("0");
        notificationsCard = createMetricCard("Notifications", dashboardNotificationsLabel, new Color(103, 58, 183));
        makeCardClickable(notificationsCard, "Notifications");
        metricsPanel.add(notificationsCard);

        // 2. Today's Sales
        dashboardSalesLabel = new JLabel("PHP 0.00");
        salesCard = createMetricCard("Today's Sales", dashboardSalesLabel, new Color(46, 125, 50));
        makeCardClickable(salesCard, "Today's Sales");
        metricsPanel.add(salesCard);

        // 3. Pending Orders
        dashboardPendingLabel = new JLabel("0");
        pendingCard = createMetricCard("Pending Orders", dashboardPendingLabel, new Color(245, 124, 0));
        makeCardClickable(pendingCard, "Pending Orders");
        metricsPanel.add(pendingCard);

        // 4. Ready for Pickup
        dashboardPickupLabel = new JLabel("0");
        pickupCard = createMetricCard("Ready for Pickup", dashboardPickupLabel, new Color(25, 118, 210));
        makeCardClickable(pickupCard, "Ready for Pickup");
        metricsPanel.add(pickupCard);

        panel.add(metricsPanel, BorderLayout.NORTH);

        // Content panel below cards
        dashboardContentPanel = new JPanel(new BorderLayout());
        dashboardContentPanel.setBackground(new Color(245, 245, 245));
        // Show notifications by default
        showNotifications();
        panel.add(dashboardContentPanel, BorderLayout.CENTER);

        return panel;
    }

    private JPanel createMetricCard(String title, JLabel valueLabel, Color color) {
        JPanel card = new JPanel(new BorderLayout(3, 3));
        card.setBackground(Color.WHITE);
        card.setOpaque(true);
        card.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(new Color(220, 220, 220), 1),
            new EmptyBorder(10, 12, 10, 12)));
        card.setPreferredSize(new Dimension(220, 90));

        // Title
        JLabel lblTitle = new JLabel(title);
        lblTitle.setFont(new Font("SansSerif", Font.PLAIN, 20));
        lblTitle.setForeground(new Color(100, 100, 100));
        card.add(lblTitle, BorderLayout.NORTH);

        // Value
        valueLabel.setFont(new Font("SansSerif", Font.BOLD, 18));
        valueLabel.setForeground(color);
        JPanel valuePanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        valuePanel.setOpaque(false);
        valuePanel.setBackground(Color.WHITE);
        valuePanel.add(valueLabel);
        card.add(valuePanel, BorderLayout.CENTER);

        return card;
    }

    // ==================== MEDICINES PANEL ====================

    private JPanel createMedicinesPanel() {
        JPanel panel = new JPanel(new BorderLayout(10, 10));
        panel.setBorder(new EmptyBorder(15, 15, 15, 15));
        panel.setBackground(new Color(245, 245, 245));

        JPanel toolbarPanel = new JPanel(new BorderLayout(10, 0));
        toolbarPanel.setBackground(new Color(245, 245, 245));
        toolbarPanel.setBorder(new EmptyBorder(0, 0, 5, 0));

        // Left side: search bar, search button, filter
        JPanel leftPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 10));
        leftPanel.setBackground(new Color(245, 245, 245));

        medicinesSearchField = new JTextField(70);
        medicinesSearchField.setFont(new Font("SansSerif", Font.PLAIN, 14));
        medicinesSearchField.setText("Search products...");
        medicinesSearchField.setForeground(new Color(150, 150, 150));
        medicinesSearchField.addFocusListener(new FocusAdapter() {
            @Override
            public void focusGained(FocusEvent e) {
                if ("Search products...".equals(medicinesSearchField.getText())) {
                    medicinesSearchField.setText("");
                    medicinesSearchField.setForeground(Color.BLACK);
                }
            }
            @Override
            public void focusLost(FocusEvent e) {
                if (medicinesSearchField.getText().trim().isEmpty()) {
                    medicinesSearchField.setText("Search products...");
                    medicinesSearchField.setForeground(new Color(150, 150, 150));
                }
            }
        });
        medicinesSearchField.addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                if (e.getKeyCode() == KeyEvent.VK_ENTER) {
                    refreshMedicines();
                }
            }
        });
        leftPanel.add(medicinesSearchField);

        JButton btnSearch = LoginView.createSecondaryButton("Search");
        btnSearch.setBackground(new Color(100, 100, 100));
        btnSearch.setForeground(Color.WHITE);
        btnSearch.addActionListener(e -> refreshMedicines());
        leftPanel.add(btnSearch);

        medicinesFilterCombo = new JComboBox<>(
            new String[]{"All Categories", "OTC Drugs", "Vitamins", "Personal Care"});
        medicinesFilterCombo.setFont(new Font("SansSerif", Font.PLAIN, 13));
        medicinesFilterCombo.addActionListener(e -> refreshMedicines());
        leftPanel.add(medicinesFilterCombo);

        toolbarPanel.add(leftPanel, BorderLayout.WEST);

        // Right side: Add Product
        JButton btnAddProduct = LoginView.createPrimaryButton("Add Product");
        btnAddProduct.addActionListener(e -> showAddProductDialog());
        JPanel rightPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 0, 10));
        rightPanel.setBackground(new Color(245, 245, 245));
        rightPanel.add(btnAddProduct);
        toolbarPanel.add(rightPanel, BorderLayout.EAST);

        panel.add(toolbarPanel, BorderLayout.NORTH);

        // Products table
        String[] cols = {"ID", "Brand Name", "Generic Name", "Dosage", "Form",
                         "Price", "Qty", "Reserved", "Category"};
        medicinesTableModel = new DefaultTableModel(cols, 0) {
            @Override
            public boolean isCellEditable(int r, int c) { return false; }
        };
        medicinesTable = AdminView.createStyledTable(medicinesTableModel);
        medicinesTable.getTableHeader().setReorderingAllowed(false);
        medicinesTable.addMouseListener(new java.awt.event.MouseAdapter() {
            @Override
            public void mouseClicked(java.awt.event.MouseEvent e) {
                if (e.getClickCount() == 1) {
                    int row = medicinesTable.getSelectedRow();
                    if (row >= 0) {
                        showEditProductDialog(row);
                    }
                }
            }
        });
        JScrollPane scroll = new JScrollPane(medicinesTable);
        scroll.setBorder(BorderFactory.createLineBorder(new Color(200, 200, 200)));
        panel.add(scroll, BorderLayout.CENTER);

        JLabel lblEditHint = new JLabel("Click a row to edit and save changes");
        lblEditHint.setFont(new Font("SansSerif", Font.ITALIC, 11));
        lblEditHint.setForeground(new Color(120, 120, 120));
        JPanel hintPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        hintPanel.setBackground(new Color(245, 245, 245));
        hintPanel.add(lblEditHint);
        panel.add(hintPanel, BorderLayout.SOUTH);

        refreshMedicines();

        return panel;
    }

    private void showAddProductDialog() {
        JDialog dialog = new JDialog((Frame) SwingUtilities.getWindowAncestor(this), "Add Product", true);
        dialog.setLayout(new BorderLayout(10, 10));
        dialog.getContentPane().setBackground(new Color(245, 245, 245));

        JPanel formPanel = new JPanel(new GridBagLayout());
        formPanel.setBackground(new Color(245, 245, 245));
        formPanel.setBorder(new EmptyBorder(15, 15, 15, 15));

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(4, 8, 4, 8);
        gbc.fill = GridBagConstraints.HORIZONTAL;

        JTextField txtBrandName = new JTextField(15);
        JTextField txtGenericName = new JTextField(15);
        JTextField txtDosage = new JTextField(15);
        JTextField txtPrice = new JTextField(15);
        JTextField txtQuantity = new JTextField(15);
        JComboBox<String> cmbDosageForm = new JComboBox<>(
            new String[]{"Tablet", "Capsule", "Syrup", "Injection", "Cream", "Drops", "Softgel"});
        JComboBox<String> cmbCategory = new JComboBox<>(
            new String[]{"OTC Drugs", "Vitamins", "Personal Care"});

        int row = 0;
        formPanel.add(new JLabel("Brand Name:"), gbc(0, row)); formPanel.add(txtBrandName, gbc(1, row)); row++;
        formPanel.add(new JLabel("Generic Name:"), gbc(0, row)); formPanel.add(txtGenericName, gbc(1, row)); row++;
        formPanel.add(new JLabel("Dosage:"), gbc(0, row)); formPanel.add(txtDosage, gbc(1, row)); row++;
        formPanel.add(new JLabel("Form:"), gbc(0, row)); formPanel.add(cmbDosageForm, gbc(1, row)); row++;
        formPanel.add(new JLabel("Price:"), gbc(0, row)); formPanel.add(txtPrice, gbc(1, row)); row++;
        formPanel.add(new JLabel("Quantity:"), gbc(0, row)); formPanel.add(txtQuantity, gbc(1, row)); row++;
        formPanel.add(new JLabel("Category:"), gbc(0, row)); formPanel.add(cmbCategory, gbc(1, row)); row++;

        dialog.add(formPanel, BorderLayout.CENTER);

        JPanel btnPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 8));
        btnPanel.setBackground(new Color(245, 245, 245));
        JButton btnAdd = LoginView.createPrimaryButton("Add Medicine");
        JButton btnCancel = LoginView.createSecondaryButton("Cancel");
        btnAdd.addActionListener(e -> {
            if (txtBrandName.getText().trim().isEmpty() || txtGenericName.getText().trim().isEmpty() ||
                txtDosage.getText().trim().isEmpty() || txtPrice.getText().trim().isEmpty() ||
                txtQuantity.getText().trim().isEmpty()) {
                JOptionPane.showMessageDialog(dialog, "Please fill all required fields!",
                    "Validation Error", JOptionPane.WARNING_MESSAGE);
                return;
            }
            try {
                String msg = controller.addMedicine(pharmacyId,
                    txtBrandName.getText().trim(), txtGenericName.getText().trim(),
                    txtDosage.getText().trim(), (String) cmbDosageForm.getSelectedItem(),
                    Double.parseDouble(txtPrice.getText().trim()),
                    Integer.parseInt(txtQuantity.getText().trim()),
                    (String) cmbCategory.getSelectedItem());
                JOptionPane.showMessageDialog(dialog, msg);
                dialog.dispose();
                refreshMedicines();
            } catch (NumberFormatException ex) {
                JOptionPane.showMessageDialog(dialog, "Please enter valid numbers for price and quantity!",
                    "Validation Error", JOptionPane.WARNING_MESSAGE);
            }
        });
        btnCancel.addActionListener(e -> dialog.dispose());
        btnPanel.add(btnAdd);
        btnPanel.add(btnCancel);
        dialog.add(btnPanel, BorderLayout.SOUTH);

        dialog.pack();
        dialog.setLocationRelativeTo(this);
        dialog.setVisible(true);
    }

    private void showEditProductDialog(int tableRow) {
        int medId = (Integer) medicinesTableModel.getValueAt(tableRow, 0);
        String brandName = str(medicinesTableModel.getValueAt(tableRow, 1));
        String genericName = str(medicinesTableModel.getValueAt(tableRow, 2));
        String dosage = str(medicinesTableModel.getValueAt(tableRow, 3));
        String form = str(medicinesTableModel.getValueAt(tableRow, 4));
        String priceStr = str(medicinesTableModel.getValueAt(tableRow, 5));
        String qtyStr = str(medicinesTableModel.getValueAt(tableRow, 6));
        String category = str(medicinesTableModel.getValueAt(tableRow, 8));

        JDialog dialog = new JDialog((Frame) SwingUtilities.getWindowAncestor(this), "Edit Product", true);
        dialog.setLayout(new BorderLayout(10, 10));
        dialog.getContentPane().setBackground(new Color(245, 245, 245));

        JPanel formPanel = new JPanel(new GridBagLayout());
        formPanel.setBackground(new Color(245, 245, 245));
        formPanel.setBorder(new EmptyBorder(15, 15, 15, 15));

        JTextField txtBrandName = new JTextField(brandName, 15);
        JTextField txtGenericName = new JTextField(genericName, 15);
        JTextField txtDosage = new JTextField(dosage, 15);
        JTextField txtPrice = new JTextField(priceStr, 15);
        JTextField txtQuantity = new JTextField(qtyStr, 15);
        JComboBox<String> cmbDosageForm = new JComboBox<>(
            new String[]{"Tablet", "Capsule", "Syrup", "Injection", "Cream", "Drops", "Softgel"});
        cmbDosageForm.setSelectedItem(form);
        JComboBox<String> cmbCategory = new JComboBox<>(
            new String[]{"OTC Drugs", "Vitamins", "Personal Care"});
        cmbCategory.setSelectedItem(category);

        int row = 0;
        formPanel.add(new JLabel("Brand Name:"), gbc(0, row)); formPanel.add(txtBrandName, gbc(1, row)); row++;
        formPanel.add(new JLabel("Generic Name:"), gbc(0, row)); formPanel.add(txtGenericName, gbc(1, row)); row++;
        formPanel.add(new JLabel("Dosage:"), gbc(0, row)); formPanel.add(txtDosage, gbc(1, row)); row++;
        formPanel.add(new JLabel("Form:"), gbc(0, row)); formPanel.add(cmbDosageForm, gbc(1, row)); row++;
        formPanel.add(new JLabel("Price:"), gbc(0, row)); formPanel.add(txtPrice, gbc(1, row)); row++;
        formPanel.add(new JLabel("Quantity:"), gbc(0, row)); formPanel.add(txtQuantity, gbc(1, row)); row++;
        formPanel.add(new JLabel("Category:"), gbc(0, row)); formPanel.add(cmbCategory, gbc(1, row)); row++;

        dialog.add(formPanel, BorderLayout.CENTER);

        JPanel btnPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 8));
        btnPanel.setBackground(new Color(245, 245, 245));
        JButton btnDelete = LoginView.createDangerButton("Delete");
        JButton btnSave = LoginView.createPrimaryButton("Save");
        JButton btnCancel = LoginView.createSecondaryButton("Cancel");
        btnDelete.addActionListener(e -> {
            int confirm = JOptionPane.showConfirmDialog(dialog,
                "Are you sure you want to delete this product?",
                "Confirm Delete", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
            if (confirm == JOptionPane.YES_OPTION) {
                JOptionPane.showMessageDialog(dialog, controller.deleteMedicine(medId, pharmacyId));
                dialog.dispose();
                refreshMedicines();
                refreshDashboard();
            }
        });
        btnSave.addActionListener(e -> {
            if (txtBrandName.getText().trim().isEmpty() || txtGenericName.getText().trim().isEmpty() ||
                txtDosage.getText().trim().isEmpty() || txtPrice.getText().trim().isEmpty() ||
                txtQuantity.getText().trim().isEmpty()) {
                JOptionPane.showMessageDialog(dialog, "Please fill all required fields!",
                    "Validation Error", JOptionPane.WARNING_MESSAGE);
                return;
            }
            try {
                String msg = controller.updateMedicine(medId,
                    txtBrandName.getText().trim(), txtGenericName.getText().trim(),
                    txtDosage.getText().trim(), (String) cmbDosageForm.getSelectedItem(),
                    Double.parseDouble(txtPrice.getText().trim()),
                    Integer.parseInt(txtQuantity.getText().trim()),
                    (String) cmbCategory.getSelectedItem());
                JOptionPane.showMessageDialog(dialog, msg);
                dialog.dispose();
                refreshMedicines();
            } catch (NumberFormatException ex) {
                JOptionPane.showMessageDialog(dialog, "Please enter valid numbers for price and quantity!",
                    "Validation Error", JOptionPane.WARNING_MESSAGE);
            }
        });
        btnCancel.addActionListener(e -> dialog.dispose());
        btnPanel.add(btnDelete);
        btnPanel.add(btnSave);
        btnPanel.add(btnCancel);
        dialog.add(btnPanel, BorderLayout.SOUTH);

        dialog.pack();
        dialog.setLocationRelativeTo(this);
        dialog.setVisible(true);
    }

    private GridBagConstraints gbc(int x, int y) {
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.gridx = x;
        gbc.gridy = y;
        gbc.insets = new Insets(4, 8, 4, 8);
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.weightx = (x == 1) ? 1 : 0;
        return gbc;
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
                         "Payment", "Pay Status", "Reserved At", "Pick up By", "Status"};
        reservationsTableModel = new DefaultTableModel(cols, 0) {
            public boolean isCellEditable(int r, int c) { return false; }
        };
        reservationsTable = AdminView.createStyledTable(reservationsTableModel);
        // Status column (index 9): CANCELLED in red
        reservationsTable.getColumnModel().getColumn(9).setCellRenderer(new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int col) {
                Component c = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, col);
                if (!isSelected && value != null && "CANCELLED".equals(value.toString())) {
                    c.setForeground(new Color(200, 0, 0));
                }
                return c;
            }
        });
        JScrollPane scroll = new JScrollPane(reservationsTable);
        scroll.setBorder(BorderFactory.createLineBorder(new Color(200, 200, 200)));
        panel.add(scroll, BorderLayout.CENTER);

        return panel;
    }

    // ==================== RESERVATION ACTIONS ====================

    private void approveReservation() {
        int row = reservationsTable.getSelectedRow();
        if (row < 0) { JOptionPane.showMessageDialog(this, "Select a reservation!"); return; }
        String status = str(reservationsTableModel.getValueAt(row, 9));
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
        String status = str(reservationsTableModel.getValueAt(row, 9));
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
        String status = str(reservationsTableModel.getValueAt(row, 9));
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
        if (medicinesTableModel == null) return;
        String searchTerm = (medicinesSearchField != null && !"Search products...".equals(medicinesSearchField.getText()))
            ? medicinesSearchField.getText().trim() : "";
        String category = medicinesFilterCombo != null ? (String) medicinesFilterCombo.getSelectedItem() : "All Categories";
        medicinesTableModel.setRowCount(0);
        for (Object[] row : controller.getFilteredMedicines(pharmacyId, searchTerm, category)) {
            medicinesTableModel.addRow(row);
        }
    }

    private void refreshReservations() {
        // Reservations tab removed - this method is no longer needed
        // Keeping for backward compatibility but doing nothing
        if (reservationsTableModel != null) {
            reservationsTableModel.setRowCount(0);
            for (Object[] row : controller.getReservations(pharmacyId))
                reservationsTableModel.addRow(row);
        }
    }

    public void refresh() {
        lblPharmacyName.setText("RxPress");
        refreshDashboard();
        refreshMedicines();
        // Reservations tab removed, so don't refresh it
        // Ensure Dashboard tab is selected when refreshed
        if (tabs != null) {
            tabs.setSelectedIndex(0);
        }
        // Show notifications by default when refreshed (on login)
        if (dashboardContentPanel != null && currentDashboardView == null) {
            showNotifications();
        }
    }

    private void refreshDashboard() {
        if (pharmacyId > 0 && dashboardSalesLabel != null) {
            double sales = controller.getTodaysSales(pharmacyId);
            int pending = controller.getPendingOrders(pharmacyId);
            int ready = controller.getReadyForPickup(pharmacyId);
            int activeReservations = (int) controller.getReservationsList(pharmacyId).stream()
                .filter(r -> r.getStatus() == models.Reservation.ReservationStatus.PENDING
                    || r.getStatus() == models.Reservation.ReservationStatus.CONFIRMED)
                .count();
            int notificationsCount = activeReservations
                + controller.getPendingPrescriptionRequests(pharmacyId).size()
                + controller.getChosenPrescriptionRequestsForPharmacy(pharmacyId).size();
            
            dashboardSalesLabel.setText(String.format("PHP %.2f", sales));
            dashboardPendingLabel.setText(String.valueOf(pending));
            dashboardPickupLabel.setText(String.valueOf(ready));
            if (dashboardNotificationsLabel != null) {
                dashboardNotificationsLabel.setText(String.valueOf(notificationsCount));
            }
            
            // Refresh content if currently showing
            if (currentDashboardView != null && dashboardContentPanel != null) {
                showDashboardContent(currentDashboardView);
            } else if (dashboardContentPanel != null) {
                // Show notifications if no button is selected
                showNotifications();
            }
        }
    }

    private void makeCardClickable(JPanel card, String cardType) {
        card.setCursor(new Cursor(Cursor.HAND_CURSOR));
        card.addMouseListener(new java.awt.event.MouseAdapter() {
            @Override
            public void mouseClicked(java.awt.event.MouseEvent e) {
                // If this button is already selected, go back to notifications
                if (cardType.equals(currentDashboardView)) {
                    showNotifications();
                } else {
                    showDashboardContent(cardType);
                }
            }
            @Override
            public void mouseEntered(java.awt.event.MouseEvent e) {
                if (!cardType.equals(currentDashboardView)) {
                    card.setBackground(new Color(250, 250, 250));
                }
            }
            @Override
            public void mouseExited(java.awt.event.MouseEvent e) {
                if (!cardType.equals(currentDashboardView)) {
                    card.setBackground(Color.WHITE);
                }
            }
        });
    }

    private void updateCardSelection(String selectedType) {
        // Reset all cards to default appearance
        if (salesCard != null) resetCardAppearance(salesCard);
        if (pendingCard != null) resetCardAppearance(pendingCard);
        if (pickupCard != null) resetCardAppearance(pickupCard);
        if (notificationsCard != null) resetCardAppearance(notificationsCard);
        
        // Highlight the selected card
        if (selectedType != null) {
            if ("Today's Sales".equals(selectedType) && salesCard != null) {
                highlightCard(salesCard, new Color(46, 125, 50));
            } else if ("Pending Orders".equals(selectedType) && pendingCard != null) {
                highlightCard(pendingCard, new Color(245, 124, 0));
            } else if ("Ready for Pickup".equals(selectedType) && pickupCard != null) {
                highlightCard(pickupCard, new Color(25, 118, 210));
            } else if ("Notifications".equals(selectedType) && notificationsCard != null) {
                highlightCard(notificationsCard, new Color(103, 58, 183));
            }
        }
    }

    private void resetCardAppearance(JPanel card) {
        if (card == null) return;
        card.setBackground(Color.WHITE);
        card.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(new Color(220, 220, 220), 1),
            new EmptyBorder(10, 12, 10, 12)));
        card.revalidate();
        card.repaint();
    }

    private void highlightCard(JPanel card, Color accentColor) {
        if (card == null) return;
        card.setBackground(new Color(240, 248, 255));
        card.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(accentColor, 3),
            new EmptyBorder(10, 12, 10, 12)));
        // Update value panel background
        for (Component comp : card.getComponents()) {
            if (comp instanceof JPanel) {
                ((JPanel) comp).setOpaque(false);
            }
        }
        card.revalidate();
        card.repaint();
        // Force parent to repaint
        if (card.getParent() != null) {
            card.getParent().revalidate();
            card.getParent().repaint();
        }
    }

    private void showDashboardContent(String cardType) {
        if (pharmacyId <= 0 || dashboardContentPanel == null) return;
        
        currentDashboardView = cardType;
        updateCardSelection(cardType);
        dashboardContentPanel.removeAll();
        
        JPanel content = null;
        switch (cardType) {
            case "Today's Sales":
                content = createTodaysSalesContent();
                break;
            case "Pending Orders":
                content = createPendingOrdersContent();
                break;
            case "Ready for Pickup":
                content = createReadyForPickupContent();
                break;
            case "Notifications":
                content = createNotificationsContent();
                break;
        }
        
        if (content != null) {
            dashboardContentPanel.add(content, BorderLayout.CENTER);
            dashboardContentPanel.revalidate();
            dashboardContentPanel.repaint();
        }
    }

    private void showNotifications() {
        if (pharmacyId <= 0 || dashboardContentPanel == null) return;
        
        currentDashboardView = "Notifications";
        updateCardSelection("Notifications");
        dashboardContentPanel.removeAll();
        
        JPanel notificationsPanel = createNotificationsContent();
        if (notificationsPanel != null) {
            dashboardContentPanel.add(notificationsPanel, BorderLayout.CENTER);
            dashboardContentPanel.revalidate();
            dashboardContentPanel.repaint();
        }
    }

    private JPanel createNotificationsContent() {
        java.util.List<Reservation> reservations = controller.getReservationsList(pharmacyId);
        java.util.List<PrescriptionRequest> prescriptionRequests = controller.getPendingPrescriptionRequests(pharmacyId);
        java.util.List<PrescriptionRequest> chosenRequests = controller.getChosenPrescriptionRequestsForPharmacy(pharmacyId);
        // Sort by order recency (when client made the order) - most recent first
        reservations = reservations.stream()
            .filter(r -> r.getStatus() == models.Reservation.ReservationStatus.PENDING
                || r.getStatus() == models.Reservation.ReservationStatus.CONFIRMED)
            .sorted((r1, r2) -> r2.getReservationTime().compareTo(r1.getReservationTime()))
            .collect(java.util.stream.Collectors.toList());
        DataStore dataStore = DataStore.getInstance();
        
        JPanel panel = new JPanel(new BorderLayout(10, 10));
        panel.setBorder(new EmptyBorder(20, 0, 0, 0));
        panel.setBackground(new Color(245, 245, 245));
        
        // Title
        JLabel lblTitle = new JLabel("Notifications - All Orders from Clients");
        lblTitle.setFont(new Font("SansSerif", Font.BOLD, 16));
        panel.add(lblTitle, BorderLayout.NORTH);
        
        // Cards container
        JPanel cardsPanel = new JPanel();
        cardsPanel.setLayout(new BoxLayout(cardsPanel, BoxLayout.Y_AXIS));
        cardsPanel.setBackground(new Color(245, 245, 245));
        
        DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("MMM dd, yyyy HH:mm");
        LocalDateTime now = LocalDateTime.now();
        
        for (Reservation r : reservations) {
            User user = dataStore.getUserById(r.getUserId());
            Medicine med = dataStore.getMedicineById(r.getMedicineId());
            
            if (user == null || med == null) continue;
            
            JPanel card = createNotificationCard(r, user, med, now, dateFormatter);
            cardsPanel.add(card);
            cardsPanel.add(Box.createVerticalStrut(10));
        }

        java.util.List<PrescriptionRequest> combined = new java.util.ArrayList<>();
        combined.addAll(prescriptionRequests);
        combined.addAll(chosenRequests);
        combined.sort((a, b) -> b.getSubmittedAt().compareTo(a.getSubmittedAt()));
        for (PrescriptionRequest pr : combined) {
            JPanel card = createPrescriptionNotificationCard(pr, dataStore, dateFormatter);
            cardsPanel.add(card);
            cardsPanel.add(Box.createVerticalStrut(10));
        }
        
        JScrollPane scroll = new JScrollPane(cardsPanel);
        scroll.setBorder(BorderFactory.createLineBorder(new Color(200, 200, 200)));
        scroll.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);
        // Make scrollbar faster
        JScrollBar verticalScrollBar = scroll.getVerticalScrollBar();
        verticalScrollBar.setUnitIncrement(20); // Default is usually 1
        verticalScrollBar.setBlockIncrement(100); // Default is usually 10
        panel.add(scroll, BorderLayout.CENTER);
        
        // Buttons panel
        JPanel btnPanel = new JPanel(new BorderLayout());
        JButton btnHistory = LoginView.createPrimaryButton("Order's History");
        btnHistory.addActionListener(e -> showOrdersHistory());
        btnPanel.add(btnHistory, BorderLayout.WEST);
        
        JButton btnRefresh = LoginView.createSecondaryButton("Refresh");
        btnRefresh.addActionListener(e -> showNotifications());
        JPanel refreshPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        refreshPanel.add(btnRefresh);
        btnPanel.add(refreshPanel, BorderLayout.EAST);
        panel.add(btnPanel, BorderLayout.SOUTH);
        
        return panel;
    }

    private void showOrdersHistory() {
        if (pharmacyId <= 0 || dashboardContentPanel == null) return;
        
        currentDashboardView = "Orders History";
        updateCardSelection(null); // Reset card selection
        dashboardContentPanel.removeAll();
        
        JPanel historyPanel = createOrdersHistoryContent();
        if (historyPanel != null) {
            dashboardContentPanel.add(historyPanel, BorderLayout.CENTER);
            dashboardContentPanel.revalidate();
            dashboardContentPanel.repaint();
        }
    }

    private JPanel createOrdersHistoryContent() {
        java.util.List<Reservation> allReservations = controller.getReservationsList(pharmacyId);
        java.util.List<PrescriptionRequest> acceptedRequests = controller.getPrescriptionRequestsForHistory(pharmacyId);
        DataStore dataStore = DataStore.getInstance();
        LocalDate today = LocalDate.now();
        
        // Filter past orders (not today's orders)
        java.util.List<Reservation> pastOrders = allReservations.stream()
            .filter(r -> {
                LocalDateTime resTime = r.getReservationTime();
                return !resTime.toLocalDate().equals(today);
            })
            .sorted((r1, r2) -> r2.getReservationTime().compareTo(r1.getReservationTime())) // Most recent first
            .collect(java.util.stream.Collectors.toList());
        
        JPanel panel = new JPanel(new BorderLayout(10, 10));
        panel.setBorder(new EmptyBorder(20, 0, 0, 0));
        panel.setBackground(new Color(245, 245, 245));
        
        // Title
        JLabel lblTitle = new JLabel("Order's History - All Past Orders");
        lblTitle.setFont(new Font("SansSerif", Font.BOLD, 16));
        panel.add(lblTitle, BorderLayout.NORTH);
        
        // Cards container
        JPanel cardsPanel = new JPanel();
        cardsPanel.setLayout(new BoxLayout(cardsPanel, BoxLayout.Y_AXIS));
        cardsPanel.setBackground(new Color(245, 245, 245));
        
        DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("MMM dd, yyyy HH:mm");
        
        if (pastOrders.isEmpty() && acceptedRequests.isEmpty()) {
            JLabel lblEmpty = new JLabel("No past orders found.");
            lblEmpty.setFont(new Font("SansSerif", Font.PLAIN, 14));
            lblEmpty.setForeground(new Color(150, 150, 150));
            lblEmpty.setAlignmentX(Component.CENTER_ALIGNMENT);
            cardsPanel.add(Box.createVerticalGlue());
            cardsPanel.add(lblEmpty);
            cardsPanel.add(Box.createVerticalGlue());
        } else {
            for (Reservation r : pastOrders) {
                User user = dataStore.getUserById(r.getUserId());
                Medicine med = dataStore.getMedicineById(r.getMedicineId());
                
                if (user == null || med == null) continue;
                
                JPanel card = createHistoryCard(r, user, med, dateFormatter);
                cardsPanel.add(card);
                cardsPanel.add(Box.createVerticalStrut(10));
            }
            for (PrescriptionRequest pr : acceptedRequests) {
                JPanel card = createPrescriptionHistoryCard(pr, dataStore, dateFormatter);
                cardsPanel.add(card);
                cardsPanel.add(Box.createVerticalStrut(10));
            }
        }
        
        JScrollPane scroll = new JScrollPane(cardsPanel);
        scroll.setBorder(BorderFactory.createLineBorder(new Color(200, 200, 200)));
        scroll.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);
        // Make scrollbar faster
        JScrollBar verticalScrollBar = scroll.getVerticalScrollBar();
        verticalScrollBar.setUnitIncrement(20); // Default is usually 1
        verticalScrollBar.setBlockIncrement(100); // Default is usually 10
        panel.add(scroll, BorderLayout.CENTER);
        
        // Back button
        JPanel btnPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        JButton btnBack = LoginView.createSecondaryButton("Back to Notifications");
        btnBack.addActionListener(e -> showNotifications());
        btnPanel.add(btnBack);
        panel.add(btnPanel, BorderLayout.SOUTH);
        
        return panel;
    }

    private JPanel createHistoryCard(Reservation r, User user, Medicine med, DateTimeFormatter formatter) {
        JPanel card = new JPanel(new BorderLayout(10, 10));
        card.setBackground(Color.WHITE);
        card.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(new Color(220, 220, 220), 1),
            new EmptyBorder(15, 15, 15, 15)));
        card.setMaximumSize(new Dimension(Integer.MAX_VALUE, 180));
        
        // Left side - Main info
        JPanel leftPanel = new JPanel();
        leftPanel.setLayout(new BoxLayout(leftPanel, BoxLayout.Y_AXIS));
        leftPanel.setBackground(Color.WHITE);
        leftPanel.setAlignmentX(Component.LEFT_ALIGNMENT);
        
        // Reference Number
        String refNum = r.getReferenceNumber() != null && !r.getReferenceNumber().isEmpty() 
            ? r.getReferenceNumber() : "REF" + String.format("%06d", r.getId());
        JLabel lblRef = new JLabel("Reference: " + refNum);
        lblRef.setFont(new Font("SansSerif", Font.BOLD, 14));
        lblRef.setForeground(new Color(25, 118, 210));
        leftPanel.add(lblRef);
        leftPanel.add(Box.createVerticalStrut(5));
        
        // Customer Name
        JLabel lblCustomer = new JLabel("Customer: " + user.getFullName());
        lblCustomer.setFont(new Font("SansSerif", Font.PLAIN, 13));
        leftPanel.add(lblCustomer);
        leftPanel.add(Box.createVerticalStrut(3));
        
        // Medicine
        JLabel lblMedicine = new JLabel("Medicine: " + med.getBrandName() + " (" + med.getGenericName() + ")");
        lblMedicine.setFont(new Font("SansSerif", Font.PLAIN, 13));
        leftPanel.add(lblMedicine);
        leftPanel.add(Box.createVerticalStrut(3));
        
        // Quantity and Total
        JLabel lblQtyTotal = new JLabel("Quantity: " + r.getQuantity() + " | Total: PHP " + 
            String.format("%.2f", r.getTotalPrice()));
        lblQtyTotal.setFont(new Font("SansSerif", Font.PLAIN, 13));
        leftPanel.add(lblQtyTotal);
        
        card.add(leftPanel, BorderLayout.WEST);
        
        // Right side - Date and Status
        JPanel rightPanel = new JPanel();
        rightPanel.setLayout(new BoxLayout(rightPanel, BoxLayout.Y_AXIS));
        rightPanel.setBackground(Color.WHITE);
        rightPanel.setAlignmentX(Component.RIGHT_ALIGNMENT);
        
        // Date and Time
        String dateTime = r.getReservationTime().format(formatter);
        JLabel lblDateTime = new JLabel("Ordered: " + dateTime);
        lblDateTime.setFont(new Font("SansSerif", Font.PLAIN, 12));
        rightPanel.add(lblDateTime);
        if (r.getStatus() != models.Reservation.ReservationStatus.CANCELLED) {
            rightPanel.add(Box.createVerticalStrut(3));
            JLabel lblPickupHistory = new JLabel("Pick up by: " + r.getExpirationTime().format(formatter));
            lblPickupHistory.setFont(new Font("SansSerif", Font.PLAIN, 12));
            lblPickupHistory.setForeground(new Color(25, 118, 210));
            rightPanel.add(lblPickupHistory);
        }
        rightPanel.add(Box.createVerticalStrut(5));
        
        // Payment Status
        JLabel lblPayment = new JLabel("Payment: " + r.getPaymentStatus().name());
        lblPayment.setFont(new Font("SansSerif", Font.PLAIN, 12));
        lblPayment.setForeground(r.getPaymentStatus() == models.Reservation.PaymentStatus.PAID ? 
            new Color(0, 150, 0) : new Color(150, 150, 150));
        rightPanel.add(lblPayment);
        rightPanel.add(Box.createVerticalStrut(5));
        
        // Status badge
        JLabel lblStatus = new JLabel("Status: " + r.getStatus().name());
        lblStatus.setFont(new Font("SansSerif", Font.BOLD, 12));
        Color statusColor = getStatusColor(r.getStatus());
        lblStatus.setForeground(statusColor);
        rightPanel.add(lblStatus);
        
        card.add(rightPanel, BorderLayout.EAST);
        
        return card;
    }

    private JPanel createPrescriptionHistoryCard(PrescriptionRequest pr, DataStore dataStore, DateTimeFormatter formatter) {
        User user = dataStore.getUserById(pr.getUserId());
        Pharmacy chosen = dataStore.getPharmacyById(pr.getChosenPharmacyId());

        JPanel card = new JPanel(new BorderLayout(10, 10));
        card.setBackground(Color.WHITE);
        card.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(new Color(200, 200, 200), 1),
            new EmptyBorder(12, 15, 12, 15)));
        card.setMaximumSize(new Dimension(Integer.MAX_VALUE, 120));

        JPanel leftPanel = new JPanel();
        leftPanel.setLayout(new BoxLayout(leftPanel, BoxLayout.Y_AXIS));
        leftPanel.setOpaque(false);

        JLabel lblRef = new JLabel("Ref: " + (pr.getReferenceNumber() != null ? pr.getReferenceNumber() : ("RX" + String.format("%06d", pr.getId()))));
        lblRef.setFont(new Font("SansSerif", Font.BOLD, 13));
        lblRef.setForeground(new Color(25, 118, 210));
        leftPanel.add(lblRef);
        leftPanel.add(Box.createVerticalStrut(4));

        JLabel lblUser = new JLabel("Customer: " + (user != null ? user.getFullName() : "Unknown"));
        lblUser.setFont(new Font("SansSerif", Font.PLAIN, 12));
        leftPanel.add(lblUser);

        String chosenText = chosen != null ? chosen.getName() + " - " + chosen.getAddress() : "Unknown pharmacy";
        JLabel lblChosen = new JLabel("Chosen pharmacy: " + chosenText);
        lblChosen.setFont(new Font("SansSerif", Font.PLAIN, 11));
        lblChosen.setForeground(new Color(90, 90, 90));
        leftPanel.add(lblChosen);

        JLabel lblTime = new JLabel("Submitted: " + pr.getSubmittedAt().format(formatter));
        lblTime.setFont(new Font("SansSerif", Font.PLAIN, 11));
        lblTime.setForeground(new Color(120, 120, 120));
        leftPanel.add(lblTime);

        card.add(leftPanel, BorderLayout.CENTER);

        JPanel rightPanel = new JPanel();
        rightPanel.setLayout(new BoxLayout(rightPanel, BoxLayout.Y_AXIS));
        rightPanel.setOpaque(false);
        JLabel lblStatus = new JLabel("Status: Chosen by customer");
        lblStatus.setFont(new Font("SansSerif", Font.PLAIN, 11));
        lblStatus.setForeground(new Color(0, 150, 0));
        rightPanel.add(lblStatus);
        card.add(rightPanel, BorderLayout.EAST);

        return card;
    }

    private Color getStatusColor(models.Reservation.ReservationStatus status) {
        switch (status) {
            case COMPLETED:
                return new Color(0, 150, 0); // Green
            case CONFIRMED:
                return new Color(25, 118, 210); // Blue
            case PENDING:
                return new Color(245, 124, 0); // Orange
            case CANCELLED:
            case EXPIRED:
                return new Color(200, 0, 0); // Red
            default:
                return new Color(100, 100, 100); // Gray
        }
    }

    private JPanel createNotificationCard(Reservation r, User user, Medicine med, 
                                         LocalDateTime now, DateTimeFormatter formatter) {
        JPanel card = new JPanel(new BorderLayout(10, 10));
        card.setBackground(Color.WHITE);
        card.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(new Color(220, 220, 220), 1),
            new EmptyBorder(15, 15, 15, 15)));
        card.setMaximumSize(new Dimension(Integer.MAX_VALUE, 200));
        
        // Left side - Main info
        JPanel leftPanel = new JPanel();
        leftPanel.setLayout(new BoxLayout(leftPanel, BoxLayout.Y_AXIS));
        leftPanel.setBackground(Color.WHITE);
        leftPanel.setAlignmentX(Component.LEFT_ALIGNMENT);
        
        // Reference Number
        String refNum = r.getReferenceNumber() != null && !r.getReferenceNumber().isEmpty() 
            ? r.getReferenceNumber() : "REF" + String.format("%06d", r.getId());
        JLabel lblRef = new JLabel("Reference: " + refNum);
        lblRef.setFont(new Font("SansSerif", Font.BOLD, 14));
        lblRef.setForeground(new Color(25, 118, 210));
        leftPanel.add(lblRef);
        leftPanel.add(Box.createVerticalStrut(5));
        
        // Customer Name
        JLabel lblCustomer = new JLabel("Customer: " + user.getFullName());
        lblCustomer.setFont(new Font("SansSerif", Font.PLAIN, 13));
        leftPanel.add(lblCustomer);
        leftPanel.add(Box.createVerticalStrut(3));
        
        // Medicine
        JLabel lblMedicine = new JLabel("Medicine: " + med.getBrandName() + " (" + med.getGenericName() + ")");
        lblMedicine.setFont(new Font("SansSerif", Font.PLAIN, 13));
        leftPanel.add(lblMedicine);
        leftPanel.add(Box.createVerticalStrut(3));
        
        // Prescription Required
        JLabel lblPrescription = new JLabel("Prescription: " + (med.isRequiresPrescription() ? "Required" : "Not Required"));
        lblPrescription.setFont(new Font("SansSerif", Font.PLAIN, 13));
        lblPrescription.setForeground(med.isRequiresPrescription() ? new Color(200, 0, 0) : new Color(0, 150, 0));
        leftPanel.add(lblPrescription);
        
        card.add(leftPanel, BorderLayout.WEST);
        
        // Right side - Time info
        JPanel rightPanel = new JPanel();
        rightPanel.setLayout(new BoxLayout(rightPanel, BoxLayout.Y_AXIS));
        rightPanel.setBackground(Color.WHITE);
        rightPanel.setAlignmentX(Component.RIGHT_ALIGNMENT);
        
        // Date and Time
        String dateTime = r.getReservationTime().format(formatter);
        JLabel lblDateTime = new JLabel("Ordered: " + dateTime);
        lblDateTime.setFont(new Font("SansSerif", Font.PLAIN, 12));
        rightPanel.add(lblDateTime);
        if (r.getStatus() != models.Reservation.ReservationStatus.CANCELLED) {
            rightPanel.add(Box.createVerticalStrut(3));
            String pickupBy = r.getExpirationTime().format(formatter);
            JLabel lblPickup = new JLabel("Pick up by: " + pickupBy);
            lblPickup.setFont(new Font("SansSerif", Font.PLAIN, 12));
            lblPickup.setForeground(new Color(25, 118, 210));
            rightPanel.add(lblPickup);
        }
        rightPanel.add(Box.createVerticalStrut(5));
        
        // Time Left - Only show for active orders (not CONFIRMED, COMPLETED, or CANCELLED)
        if (r.getStatus() != models.Reservation.ReservationStatus.CONFIRMED && 
            r.getStatus() != models.Reservation.ReservationStatus.COMPLETED &&
            r.getStatus() != models.Reservation.ReservationStatus.CANCELLED) {
            LocalDateTime expiration = r.getExpirationTime();
            String timeLeftText;
            Color timeColor;
            
            if (now.isAfter(expiration)) {
                timeLeftText = "Expired";
                timeColor = new Color(200, 0, 0);
            } else {
                Duration duration = Duration.between(now, expiration);
                long hours = duration.toHours();
                long minutes = duration.toMinutes() % 60;
                
                if (hours > 0) {
                    timeLeftText = String.format("Time Left: %d hour(s) %d minute(s)", hours, minutes);
                } else {
                    timeLeftText = String.format("Time Left: %d minute(s)", minutes);
                }
                
                if (hours < 1) {
                    timeColor = new Color(200, 0, 0); // Red if less than 1 hour
                } else if (hours < 6) {
                    timeColor = new Color(245, 124, 0); // Orange if less than 6 hours
                } else {
                    timeColor = new Color(0, 150, 0); // Green if more than 6 hours
                }
            }
            
            JLabel lblTimeLeft = new JLabel(timeLeftText);
            lblTimeLeft.setFont(new Font("SansSerif", Font.BOLD, 12));
            lblTimeLeft.setForeground(timeColor);
            rightPanel.add(lblTimeLeft);
            rightPanel.add(Box.createVerticalStrut(5));
        }
        
        // Status badge
        JLabel lblStatus = new JLabel("Status: " + r.getStatus().name());
        lblStatus.setFont(new Font("SansSerif", Font.PLAIN, 11));
        lblStatus.setForeground(getStatusColor(r.getStatus()));
        rightPanel.add(Box.createVerticalStrut(5));
        rightPanel.add(lblStatus);
        
        card.add(rightPanel, BorderLayout.EAST);
        
        return card;
    }

    private JPanel createPrescriptionNotificationCard(PrescriptionRequest pr, DataStore dataStore, DateTimeFormatter formatter) {
        User user = dataStore.getUserById(pr.getUserId());
        JPanel card = new JPanel(new BorderLayout(10, 10));
        card.setBackground(Color.WHITE);
        card.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(new Color(220, 220, 220), 1),
            new EmptyBorder(15, 15, 15, 15)));
        card.setMaximumSize(new Dimension(Integer.MAX_VALUE, 200));

        JPanel leftPanel = new JPanel();
        leftPanel.setLayout(new BoxLayout(leftPanel, BoxLayout.Y_AXIS));
        leftPanel.setBackground(Color.WHITE);
        leftPanel.setAlignmentX(Component.LEFT_ALIGNMENT);

        String ref = pr.getReferenceNumber() != null ? pr.getReferenceNumber() : ("RX" + String.format("%06d", pr.getId()));
        JLabel lblRef = new JLabel("Reference: " + ref);
        lblRef.setFont(new Font("SansSerif", Font.BOLD, 14));
        lblRef.setForeground(new Color(25, 118, 210));
        leftPanel.add(lblRef);
        leftPanel.add(Box.createVerticalStrut(5));

        JLabel lblCustomer = new JLabel("Customer: " + (user != null ? user.getFullName() : "Unknown"));
        lblCustomer.setFont(new Font("SansSerif", Font.PLAIN, 13));
        leftPanel.add(lblCustomer);
        leftPanel.add(Box.createVerticalStrut(3));

        JLabel lblType = new JLabel("Prescription: Photo Upload");
        lblType.setFont(new Font("SansSerif", Font.PLAIN, 13));
        lblType.setForeground(new Color(200, 0, 0));
        leftPanel.add(lblType);

        card.add(leftPanel, BorderLayout.WEST);

        JPanel rightPanel = new JPanel();
        rightPanel.setLayout(new BoxLayout(rightPanel, BoxLayout.Y_AXIS));
        rightPanel.setBackground(Color.WHITE);
        rightPanel.setAlignmentX(Component.RIGHT_ALIGNMENT);

        String dateTime = pr.getSubmittedAt().format(formatter);
        JLabel lblDateTime = new JLabel("Submitted: " + dateTime);
        lblDateTime.setFont(new Font("SansSerif", Font.PLAIN, 12));
        rightPanel.add(lblDateTime);
        rightPanel.add(Box.createVerticalStrut(5));

        String status = "Pending confirmation";
        if (pr.getChosenPharmacyId() > 0) {
            status = pr.getStatus() == models.PrescriptionRequest.Status.READY_FOR_PICKUP ? "Ready for Pickup" : "Chosen by customer";
        } else if (pr.getConfirmedPharmacyIds() != null && pr.getConfirmedPharmacyIds().contains(pharmacyId)) {
            status = "Waiting for customer to confirm";
        }
        JLabel lblStatus = new JLabel("Status: " + status);
        lblStatus.setFont(new Font("SansSerif", Font.PLAIN, 11));
        lblStatus.setForeground(new Color(25, 118, 210));
        rightPanel.add(lblStatus);

        card.add(rightPanel, BorderLayout.EAST);
        return card;
    }

    private JPanel createTodaysSalesContent() {
        Object[][] transactions = controller.getTodaysTransactions(pharmacyId);
        double totalSales = controller.getTodaysSales(pharmacyId);
        
        JPanel panel = new JPanel(new BorderLayout(10, 10));
        panel.setBorder(new EmptyBorder(20, 0, 0, 0));
        panel.setBackground(new Color(245, 245, 245));
        
        // Title with total
        JPanel titlePanel = new JPanel(new BorderLayout());
        JLabel lblTitle = new JLabel("Today's Sales");
        lblTitle.setFont(new Font("SansSerif", Font.BOLD, 16));
        titlePanel.add(lblTitle, BorderLayout.WEST);
        
        JLabel lblTotal = new JLabel("Total: " + String.format("PHP %.2f", totalSales));
        lblTotal.setFont(new Font("SansSerif", Font.BOLD, 14));
        lblTotal.setForeground(new Color(46, 125, 50));
        titlePanel.add(lblTotal, BorderLayout.EAST);
        panel.add(titlePanel, BorderLayout.NORTH);
        
        // Table
        String[] cols = {"Reference Number", "Customer", "Medicine", "Qty", "Total (PHP)", 
                        "Payment Method", "Payment Status", "Time"};
        DefaultTableModel model = new DefaultTableModel(cols, 0) {
            public boolean isCellEditable(int r, int c) { return false; }
        };
        for (Object[] row : transactions) {
            model.addRow(row);
        }
        JTable table = AdminView.createStyledTable(model);
        table.getTableHeader().setReorderingAllowed(false); // Disable column reordering
        JScrollPane scroll = new JScrollPane(table);
        scroll.setBorder(BorderFactory.createLineBorder(new Color(200, 200, 200)));
        panel.add(scroll, BorderLayout.CENTER);
        
        return panel;
    }

    private JPanel createPendingOrdersContent() {
        java.util.List<Reservation> unpaidNoPrescription = controller.getPendingUnpaidNoPrescriptionList(pharmacyId);
        java.util.List<Reservation> prescriptionRequired = controller.getPendingPrescriptionRequiredList(pharmacyId);
        DataStore dataStore = DataStore.getInstance();
        
        JPanel panel = new JPanel(new BorderLayout(10, 10));
        panel.setBorder(new EmptyBorder(20, 0, 0, 0));
        panel.setBackground(new Color(245, 245, 245));
        
        // Title
        JLabel lblTitle = new JLabel("Pending Orders (Unpaid)");
        lblTitle.setFont(new Font("SansSerif", Font.BOLD, 16));
        panel.add(lblTitle, BorderLayout.NORTH);
        
        // Create tabbed pane for separation - Prescription Required first so photo uploads are prominent
        JTabbedPane subTabs = new JTabbedPane();
        subTabs.setFont(new Font("SansSerif", Font.PLAIN, 12));
        
        java.util.List<PrescriptionRequest> prescriptionRequests = controller.getPendingPrescriptionRequests(pharmacyId);
        java.util.List<PrescriptionRequest> chosenPrescriptionRequests = controller.getChosenPrescriptionRequestsForPharmacy(pharmacyId);
        JPanel prescriptionPanel = createPrescriptionRequiredTabContent(prescriptionRequests, chosenPrescriptionRequests, prescriptionRequired, dataStore);
        subTabs.addTab("Prescription Required (" + (prescriptionRequests.size() + prescriptionRequired.size()) + ")", prescriptionPanel);
        
        JPanel notPaidPanel = createPendingOrdersCardsPanel(unpaidNoPrescription, dataStore, "Not Yet Paid");
        subTabs.addTab("Not Yet Paid (" + unpaidNoPrescription.size() + ")", notPaidPanel);
        
        panel.add(subTabs, BorderLayout.CENTER);
        
        // Refresh button
        JPanel btnPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        JButton btnRefresh = LoginView.createSecondaryButton("Refresh");
        btnRefresh.addActionListener(e -> {
            if (currentDashboardView != null) {
                showDashboardContent(currentDashboardView);
            }
        });
        btnPanel.add(btnRefresh);
        panel.add(btnPanel, BorderLayout.SOUTH);
        
        return panel;
    }

    private JPanel createPrescriptionRequiredTabContent(java.util.List<PrescriptionRequest> prescriptionRequests,
            java.util.List<PrescriptionRequest> chosenRequests,
            java.util.List<Reservation> prescriptionReservations, DataStore dataStore) {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBackground(new Color(245, 245, 245));

        JPanel cardsPanel = new JPanel();
        cardsPanel.setLayout(new BoxLayout(cardsPanel, BoxLayout.Y_AXIS));
        cardsPanel.setBackground(new Color(245, 245, 245));
        cardsPanel.setBorder(new EmptyBorder(15, 15, 15, 15));

        DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("MMM dd, yyyy HH:mm");

        // Prescription requests (photo uploads from users)
        for (PrescriptionRequest pr : prescriptionRequests) {
            JPanel card = createPrescriptionRequestCard(pr, dataStore, dateFormatter);
            cardsPanel.add(card);
            cardsPanel.add(Box.createVerticalStrut(10));
        }

        if (!chosenRequests.isEmpty()) {
            JLabel lblChosen = new JLabel("Customer Confirmed (Awaiting Pickup)");
            lblChosen.setFont(new Font("SansSerif", Font.BOLD, 12));
            lblChosen.setForeground(new Color(25, 118, 210));
            lblChosen.setBorder(new EmptyBorder(8, 2, 4, 2));
            lblChosen.setAlignmentX(Component.CENTER_ALIGNMENT);
            cardsPanel.add(lblChosen);
        }
        for (PrescriptionRequest pr : chosenRequests) {
            JPanel card = createChosenPrescriptionCard(pr, dataStore, dateFormatter);
            cardsPanel.add(card);
            cardsPanel.add(Box.createVerticalStrut(10));
        }

        // Prescription medicine reservations
        for (Reservation r : prescriptionReservations) {
            User user = dataStore.getUserById(r.getUserId());
            Medicine med = dataStore.getMedicineById(r.getMedicineId());
            if (user == null || med == null) continue;
            JPanel card = createPendingOrderCard(r, user, med, dateFormatter);
            cardsPanel.add(card);
            cardsPanel.add(Box.createVerticalStrut(10));
        }

        if (prescriptionRequests.isEmpty() && chosenRequests.isEmpty() && prescriptionReservations.isEmpty()) {
            JLabel lblEmpty = new JLabel("No prescription requests or orders in this category.");
            lblEmpty.setFont(new Font("SansSerif", Font.PLAIN, 14));
            lblEmpty.setForeground(new Color(150, 150, 150));
            lblEmpty.setAlignmentX(Component.CENTER_ALIGNMENT);
            cardsPanel.add(Box.createVerticalGlue());
            cardsPanel.add(lblEmpty);
            cardsPanel.add(Box.createVerticalGlue());
        }

        JScrollPane scroll = new JScrollPane(cardsPanel);
        scroll.setBorder(BorderFactory.createLineBorder(new Color(200, 200, 200)));
        JScrollBar vsb = scroll.getVerticalScrollBar();
        vsb.setUnitIncrement(20);
        vsb.setBlockIncrement(100);
        panel.add(scroll, BorderLayout.CENTER);

        JPanel btnPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        JButton btnRefresh = LoginView.createSecondaryButton("Refresh");
        btnRefresh.addActionListener(e -> {
            if (currentDashboardView != null) showDashboardContent(currentDashboardView);
        });
        btnPanel.add(btnRefresh);
        panel.add(btnPanel, BorderLayout.SOUTH);

        return panel;
    }

    private JPanel createPrescriptionRequestCard(PrescriptionRequest pr, DataStore dataStore, DateTimeFormatter formatter) {
        User user = dataStore.getUserById(pr.getUserId());
        JPanel card = new JPanel(new BorderLayout(15, 10));
        card.setBackground(Color.WHITE);
        card.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(new Color(200, 200, 200), 1),
            new EmptyBorder(12, 15, 12, 15)));
        card.setMaximumSize(new Dimension(Integer.MAX_VALUE, 100));

        JPanel leftPanel = new JPanel(new BorderLayout(5, 3));
        leftPanel.setOpaque(false);
        JPanel topLeft = new JPanel();
        topLeft.setLayout(new BoxLayout(topLeft, BoxLayout.Y_AXIS));
        topLeft.setOpaque(false);
        JLabel lblUser = new JLabel("Customer: " + (user != null ? user.getFullName() : "Unknown"));
        lblUser.setFont(new Font("SansSerif", Font.BOLD, 13));
        JLabel lblRef = new JLabel("Ref: " + (pr.getReferenceNumber() != null ? pr.getReferenceNumber() : ("RX" + String.format("%06d", pr.getId()))));
        lblRef.setFont(new Font("SansSerif", Font.PLAIN, 11));
        lblRef.setForeground(new Color(25, 118, 210));
        topLeft.add(lblUser);
        topLeft.add(lblRef);
        leftPanel.add(topLeft, BorderLayout.NORTH);
        JLabel lblTime = new JLabel("Submitted: " + pr.getSubmittedAt().format(formatter));
        lblTime.setFont(new Font("SansSerif", Font.PLAIN, 12));
        lblTime.setForeground(new Color(100, 100, 100));
        leftPanel.add(lblTime, BorderLayout.CENTER);
        String statusText = "Status: Pending confirmation";
        if (pr.getConfirmedPharmacyIds() != null && pr.getConfirmedPharmacyIds().contains(pharmacyId)) {
            statusText = "Status: Waiting for customer to confirm";
        }
        JLabel lblPresc = new JLabel("Prescription photo attached  |  " + statusText);
        lblPresc.setFont(new Font("SansSerif", Font.PLAIN, 11));
        lblPresc.setForeground(new Color(25, 118, 210));
        leftPanel.add(lblPresc, BorderLayout.SOUTH);
        card.add(leftPanel, BorderLayout.CENTER);

        JPanel btnWrap = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        btnWrap.setOpaque(false);

        JButton btnViewPhoto = LoginView.createSecondaryButton("View Photo");
        btnViewPhoto.addActionListener(e -> showPrescriptionImage(pr.getImagePath()));
        btnWrap.add(btnViewPhoto);

        JButton btnDecline = LoginView.createDangerButton("Decline");
        btnDecline.addActionListener(e -> {
            if (JOptionPane.showConfirmDialog(card, "Decline this prescription request? Other pharmacies can still accept it.",
                    "Decline", JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE) == JOptionPane.YES_OPTION) {
                if (controller.declinePrescriptionRequest(pr.getId(), pharmacyId)) {
                    JOptionPane.showMessageDialog(card, "Prescription request declined.", "Declined", JOptionPane.INFORMATION_MESSAGE);
                    if (currentDashboardView != null) showDashboardContent(currentDashboardView);
                }
            }
        });
        btnWrap.add(btnDecline);

        JButton btnConfirm = LoginView.createSuccessButton("Confirm Stock");
        boolean alreadyConfirmed = pr.getConfirmedPharmacyIds() != null &&
            pr.getConfirmedPharmacyIds().contains(pharmacyId);
        if (alreadyConfirmed) {
            btnConfirm.setEnabled(false);
            btnConfirm.setText("Confirmed");
        }
        btnConfirm.addActionListener(e -> {
            btnConfirm.setEnabled(false);
            btnConfirm.setText("...");
            String result = controller.confirmPrescriptionRequest(pr.getId(), pharmacyId);
            if (result.startsWith("ERROR|")) {
                JOptionPane.showMessageDialog(card, result.replace("ERROR|", ""), "Unable to Confirm", JOptionPane.WARNING_MESSAGE);
                btnConfirm.setEnabled(true);
                btnConfirm.setText("Confirm Stock");
            } else {
                JOptionPane.showMessageDialog(card, result, "Confirmed", JOptionPane.INFORMATION_MESSAGE);
                if (currentDashboardView != null) showDashboardContent(currentDashboardView);
            }
        });
        btnWrap.add(btnConfirm);
        card.add(btnWrap, BorderLayout.EAST);

        return card;
    }

    private JPanel createChosenPrescriptionCard(PrescriptionRequest pr, DataStore dataStore, DateTimeFormatter formatter) {
        User user = dataStore.getUserById(pr.getUserId());
        Pharmacy chosen = dataStore.getPharmacyById(pr.getChosenPharmacyId());

        JPanel card = new JPanel(new BorderLayout(15, 10));
        card.setBackground(Color.WHITE);
        card.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(new Color(200, 200, 200), 1),
            new EmptyBorder(12, 15, 12, 15)));
        card.setMaximumSize(new Dimension(Integer.MAX_VALUE, 110));

        JPanel leftPanel = new JPanel();
        leftPanel.setLayout(new BoxLayout(leftPanel, BoxLayout.Y_AXIS));
        leftPanel.setOpaque(false);

        String ref = pr.getReferenceNumber() != null ? pr.getReferenceNumber() : ("RX" + String.format("%06d", pr.getId()));
        JLabel lblRef = new JLabel("Ref: " + ref);
        lblRef.setFont(new Font("SansSerif", Font.BOLD, 13));
        lblRef.setForeground(new Color(70, 130, 180));
        leftPanel.add(lblRef);
        leftPanel.add(Box.createVerticalStrut(4));

        JLabel lblUser = new JLabel("Customer: " + (user != null ? user.getFullName() : "Unknown"));
        lblUser.setFont(new Font("SansSerif", Font.PLAIN, 12));
        leftPanel.add(lblUser);

        JLabel lblChosen = new JLabel("Chosen pharmacy: " + (chosen != null ? chosen.getName() : "Unknown"));
        lblChosen.setFont(new Font("SansSerif", Font.PLAIN, 11));
        lblChosen.setForeground(new Color(90, 90, 90));
        leftPanel.add(lblChosen);

        JLabel lblTime = new JLabel("Submitted: " + pr.getSubmittedAt().format(formatter));
        lblTime.setFont(new Font("SansSerif", Font.PLAIN, 11));
        lblTime.setForeground(new Color(120, 120, 120));
        leftPanel.add(lblTime);

        card.add(leftPanel, BorderLayout.CENTER);

        JButton actionButton;
        if (pr.getStatus() == models.PrescriptionRequest.Status.ACCEPTED) {
            actionButton = LoginView.createSuccessButton("Ready for Pickup");
            actionButton.addActionListener(e -> {
                String msg = controller.markPrescriptionReadyForPickup(pr.getId(), pharmacyId);
                JOptionPane.showMessageDialog(card, msg);
                if (currentDashboardView != null) showDashboardContent(currentDashboardView);
            });
        } else if (pr.getStatus() == models.PrescriptionRequest.Status.READY_FOR_PICKUP) {
            actionButton = LoginView.createSuccessButton("Paid");
            actionButton.setEnabled(true);
            actionButton.addActionListener(e -> {
                JTextField nameField = new JTextField(18);
                JTextField qtyField = new JTextField(6);
                JTextField amountField = new JTextField(10);
                JPanel form = new JPanel(new GridLayout(3, 2, 6, 6));
                form.add(new JLabel("Medicine name:"));
                form.add(nameField);
                form.add(new JLabel("Quantity:"));
                form.add(qtyField);
                form.add(new JLabel("Amount (PHP):"));
                form.add(amountField);
                int result = JOptionPane.showConfirmDialog(card, form,
                    "Payment Details", JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);
                if (result == JOptionPane.OK_OPTION) {
                    String medName = nameField.getText().trim();
                    String qtyStr = qtyField.getText().trim();
                    String amtStr = amountField.getText().trim();
                    if (medName.isEmpty() || qtyStr.isEmpty() || amtStr.isEmpty()) {
                        JOptionPane.showMessageDialog(card, "Please enter medicine name, quantity, and amount.");
                        return;
                    }
                    int qty;
                    try {
                        qty = Integer.parseInt(qtyStr);
                    } catch (NumberFormatException ex) {
                        JOptionPane.showMessageDialog(card, "Invalid quantity.");
                        return;
                    }
                    if (qty <= 0) {
                        JOptionPane.showMessageDialog(card, "Quantity must be greater than zero.");
                        return;
                    }
                    double amt;
                    try {
                        amt = Double.parseDouble(amtStr);
                    } catch (NumberFormatException ex) {
                        JOptionPane.showMessageDialog(card, "Invalid amount.");
                        return;
                    }
                    String msg = controller.markPrescriptionPaid(pr.getId(), pharmacyId, medName, qty, amt);
                    JOptionPane.showMessageDialog(card, msg);
                    refreshDashboard();
                    if (currentDashboardView != null) showDashboardContent(currentDashboardView);
                }
            });
        } else {
            actionButton = LoginView.createSuccessButton("Paid");
            actionButton.setEnabled(false);
        }
        JPanel btnWrap = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        btnWrap.setOpaque(false);
        btnWrap.add(actionButton);
        card.add(btnWrap, BorderLayout.EAST);

        return card;
    }

    private void showPrescriptionImage(String imagePath) {
        if (imagePath == null || imagePath.isEmpty()) {
            JOptionPane.showMessageDialog(this, "No prescription image available.", "Image Not Found", JOptionPane.WARNING_MESSAGE);
            return;
        }
        File file = new File(imagePath);
        if (!file.exists() || !file.canRead()) {
            JOptionPane.showMessageDialog(this, "Prescription image file not found or cannot be read.", "Image Not Found", JOptionPane.WARNING_MESSAGE);
            return;
        }
        try {
            BufferedImage img = ImageIO.read(file);
            if (img == null) {
                JOptionPane.showMessageDialog(this, "Could not load the prescription image.", "Invalid Image", JOptionPane.WARNING_MESSAGE);
                return;
            }
            Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
            int paneWidth = (int) (screenSize.width * 0.95);
            int paneHeight = (int) (screenSize.height * 0.9);
            int imgW = img.getWidth();
            int imgH = img.getHeight();
            double scale = Math.min((double) paneWidth / imgW, (double) paneHeight / imgH);
            int displayW = (int) (imgW * scale);
            int displayH = (int) (imgH * scale);
            Image displayImg = img.getScaledInstance(displayW, displayH, Image.SCALE_SMOOTH);
            JDialog dialog = new JDialog(JOptionPane.getFrameForComponent(this), "Prescription Photo", true);
            dialog.setLayout(new BorderLayout());
            JLabel lbl = new JLabel(new ImageIcon(displayImg));
            lbl.setHorizontalAlignment(JLabel.CENTER);
            JScrollPane scroll = new JScrollPane(lbl);
            scroll.setBorder(null);
            scroll.getViewport().setBackground(Color.WHITE);
            dialog.add(scroll, BorderLayout.CENTER);
            dialog.setSize(paneWidth, paneHeight);
            dialog.setLocationRelativeTo(null);
            dialog.setVisible(true);
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "Could not display the prescription image: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private JPanel createPendingOrdersCardsPanel(java.util.List<Reservation> reservations, DataStore dataStore, String category) {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBackground(new Color(245, 245, 245));
        
        // Cards container with vertical box layout for vertical scrolling
        JPanel cardsPanel = new JPanel();
        cardsPanel.setLayout(new BoxLayout(cardsPanel, BoxLayout.Y_AXIS));
        cardsPanel.setBackground(new Color(245, 245, 245));
        cardsPanel.setBorder(new EmptyBorder(15, 15, 15, 15));
        
        DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("MMM dd, yyyy HH:mm");
        
        for (Reservation r : reservations) {
            User user = dataStore.getUserById(r.getUserId());
            Medicine med = dataStore.getMedicineById(r.getMedicineId());
            
            if (user == null || med == null) continue;
            
            JPanel card = createPendingOrderCard(r, user, med, dateFormatter);
            cardsPanel.add(card);
            cardsPanel.add(Box.createVerticalStrut(10)); // Add spacing between cards
        }
        
        if (reservations.isEmpty()) {
            JLabel lblEmpty = new JLabel("No pending orders in this category.");
            lblEmpty.setFont(new Font("SansSerif", Font.PLAIN, 14));
            lblEmpty.setForeground(new Color(150, 150, 150));
            lblEmpty.setHorizontalAlignment(JLabel.CENTER);
            cardsPanel.add(lblEmpty);
        }
        
        JScrollPane scroll = new JScrollPane(cardsPanel);
        scroll.setBorder(BorderFactory.createLineBorder(new Color(200, 200, 200)));
        scroll.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);
        // Make scrollbar faster
        JScrollBar verticalScrollBar = scroll.getVerticalScrollBar();
        verticalScrollBar.setUnitIncrement(20);
        verticalScrollBar.setBlockIncrement(100);
        panel.add(scroll, BorderLayout.CENTER);
        
        return panel;
    }

    private JPanel createPendingOrderCard(Reservation r, User user, Medicine med, DateTimeFormatter formatter) {
        JPanel card = new JPanel(new BorderLayout(15, 10));
        card.setBackground(Color.WHITE);
        card.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(new Color(200, 200, 200), 1),
            new EmptyBorder(12, 15, 12, 15)));
        card.setPreferredSize(new Dimension(600, 120)); // Bigger cards for better visibility
        card.setMaximumSize(new Dimension(Integer.MAX_VALUE, 120));
        card.setMinimumSize(new Dimension(600, 120));
        
        // Left panel - Order info
        JPanel leftPanel = new JPanel();
        leftPanel.setLayout(new BoxLayout(leftPanel, BoxLayout.Y_AXIS));
        leftPanel.setOpaque(false);
        
        // Reference number
        JLabel lblRef = new JLabel("Ref: " + r.getReferenceNumber());
        lblRef.setFont(new Font("SansSerif", Font.BOLD, 13));
        lblRef.setForeground(new Color(70, 130, 180));
        leftPanel.add(lblRef);
        leftPanel.add(Box.createVerticalStrut(6));
        
        // Customer name
        JLabel lblCustomer = new JLabel(user.getFullName());
        lblCustomer.setFont(new Font("SansSerif", Font.BOLD, 14));
        leftPanel.add(lblCustomer);
        leftPanel.add(Box.createVerticalStrut(5));
        
        // Medicine
        JLabel lblMedicine = new JLabel(med.getBrandName());
        lblMedicine.setFont(new Font("SansSerif", Font.PLAIN, 13));
        leftPanel.add(lblMedicine);
        leftPanel.add(Box.createVerticalStrut(5));
        
        // Quantity and Total
        JLabel lblQtyTotal = new JLabel("Qty: " + r.getQuantity() + " | PHP " + 
            String.format("%.2f", r.getTotalPrice()));
        lblQtyTotal.setFont(new Font("SansSerif", Font.PLAIN, 12));
        leftPanel.add(lblQtyTotal);
        leftPanel.add(Box.createVerticalStrut(5));
        
        // Date
        JLabel lblDate = new JLabel("Ordered: " + r.getReservationTime().format(formatter));
        lblDate.setFont(new Font("SansSerif", Font.PLAIN, 11));
        lblDate.setForeground(new Color(100, 100, 100));
        leftPanel.add(lblDate);
        if (r.getStatus() != models.Reservation.ReservationStatus.CANCELLED) {
            leftPanel.add(Box.createVerticalStrut(2));
            JLabel lblPickup = new JLabel("Pick up by: " + r.getExpirationTime().format(formatter));
            lblPickup.setFont(new Font("SansSerif", Font.BOLD, 11));
            lblPickup.setForeground(new Color(25, 118, 210));
            leftPanel.add(lblPickup);
        }
        
        card.add(leftPanel, BorderLayout.CENTER);
        
        // Paid button on the right side
        JButton btnPaid = LoginView.createPrimaryButton("Paid");
        btnPaid.setPreferredSize(new Dimension(70, 25)); // Wide enough to show full "Paid" text
        btnPaid.addActionListener(e -> {
            String result = controller.markPendingOrderAsPaid(r.getId());
            JOptionPane.showMessageDialog(card, result, "Payment Status", 
                !result.contains("Failed") ? JOptionPane.INFORMATION_MESSAGE : JOptionPane.ERROR_MESSAGE);
            // Refresh dashboard to update Today's Sales and remove from pending
            refreshDashboard();
            if (currentDashboardView != null) {
                showDashboardContent(currentDashboardView);
            }
        });
        // Align button to center vertically on the right
        JPanel buttonContainer = new JPanel(new BorderLayout());
        buttonContainer.setOpaque(false);
        buttonContainer.add(btnPaid, BorderLayout.CENTER);
        card.add(buttonContainer, BorderLayout.EAST);
        
        return card;
    }

    private void showPendingOrderDetailsDialog(Reservation r, User user, Medicine med, DateTimeFormatter formatter) {
        JDialog dialog = new JDialog((JFrame) SwingUtilities.getWindowAncestor(this), "Order Details", true);
        dialog.setSize(450, 350);
        dialog.setLocationRelativeTo(this);
        dialog.setLayout(new BorderLayout(15, 15));
        dialog.getContentPane().setBackground(new Color(245, 245, 245));
        
        // Content panel
        JPanel contentPanel = new JPanel();
        contentPanel.setLayout(new BoxLayout(contentPanel, BoxLayout.Y_AXIS));
        contentPanel.setBorder(new EmptyBorder(20, 20, 20, 20));
        contentPanel.setBackground(Color.WHITE);
        
        // Reference Number
        JLabel lblRef = new JLabel("Reference: " + r.getReferenceNumber());
        lblRef.setFont(new Font("SansSerif", Font.BOLD, 14));
        lblRef.setForeground(new Color(70, 130, 180));
        contentPanel.add(lblRef);
        contentPanel.add(Box.createVerticalStrut(15));
        
        // Customer
        contentPanel.add(createDetailRow("Customer:", user.getFullName()));
        contentPanel.add(Box.createVerticalStrut(10));
        
        // Medicine
        contentPanel.add(createDetailRow("Medicine:", med.getBrandName() + " (" + med.getGenericName() + ")"));
        contentPanel.add(Box.createVerticalStrut(10));
        
        // Prescription
        String prescText = med.isRequiresPrescription() ? "Required" : "Not Required";
        Color prescColor = med.isRequiresPrescription() ? new Color(220, 53, 69) : new Color(40, 167, 69);
        JPanel prescPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        prescPanel.setOpaque(false);
        JLabel lblPrescLabel = new JLabel("Prescription: ");
        lblPrescLabel.setFont(new Font("SansSerif", Font.PLAIN, 12));
        JLabel lblPrescValue = new JLabel(prescText);
        lblPrescValue.setFont(new Font("SansSerif", Font.BOLD, 12));
        lblPrescValue.setForeground(prescColor);
        prescPanel.add(lblPrescLabel);
        prescPanel.add(lblPrescValue);
        contentPanel.add(prescPanel);
        contentPanel.add(Box.createVerticalStrut(10));
        
        // Quantity
        contentPanel.add(createDetailRow("Quantity:", String.valueOf(r.getQuantity())));
        contentPanel.add(Box.createVerticalStrut(10));
        
        // Total Price
        contentPanel.add(createDetailRow("Total Price:", "PHP " + String.format("%.2f", r.getTotalPrice())));
        contentPanel.add(Box.createVerticalStrut(10));
        
        // Payment Method
        contentPanel.add(createDetailRow("Payment Method:", r.getPaymentMethod().name().replace("_", " ")));
        contentPanel.add(Box.createVerticalStrut(10));
        
        // Date
        contentPanel.add(createDetailRow("Reserved At:", r.getReservationTime().format(formatter)));
        contentPanel.add(Box.createVerticalStrut(10));
        if (r.getStatus() != models.Reservation.ReservationStatus.CANCELLED) {
            contentPanel.add(createDetailRow("Pick up by:", r.getExpirationTime().format(formatter)));
            contentPanel.add(Box.createVerticalStrut(10));
        }
        
        // Status
        contentPanel.add(createDetailRow("Status:", r.getStatus().name()));
        
        dialog.add(contentPanel, BorderLayout.CENTER);
        
        // Button panel with Paid button in lower right
        JPanel btnPanel = new JPanel(new BorderLayout());
        btnPanel.setBorder(new EmptyBorder(15, 20, 15, 20));
        btnPanel.setOpaque(false);
        
        JButton btnClose = LoginView.createSecondaryButton("Close");
        btnClose.addActionListener(e -> dialog.dispose());
        btnPanel.add(btnClose, BorderLayout.WEST);
        
        JButton btnPaid = LoginView.createPrimaryButton("Paid");
        btnPaid.addActionListener(e -> {
            String result = controller.markPendingOrderAsPaid(r.getId());
            JOptionPane.showMessageDialog(dialog, result, "Payment Status", 
                !result.contains("Failed") ? JOptionPane.INFORMATION_MESSAGE : JOptionPane.ERROR_MESSAGE);
            dialog.dispose();
            // Refresh dashboard to update Today's Sales and remove from pending
            refreshDashboard();
            if (currentDashboardView != null) {
                showDashboardContent(currentDashboardView);
            }
        });
        btnPanel.add(btnPaid, BorderLayout.EAST);
        
        dialog.add(btnPanel, BorderLayout.SOUTH);
        dialog.setVisible(true);
    }

    private JPanel createDetailRow(String label, String value) {
        JPanel row = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        row.setOpaque(false);
        JLabel lblLabel = new JLabel(label);
        lblLabel.setFont(new Font("SansSerif", Font.PLAIN, 12));
        lblLabel.setPreferredSize(new Dimension(120, 20));
        JLabel lblValue = new JLabel(value);
        lblValue.setFont(new Font("SansSerif", Font.PLAIN, 12));
        row.add(lblLabel);
        row.add(lblValue);
        return row;
    }

    private JPanel createReadyForPickupContent() {
        java.util.List<Reservation> orders = controller.getReadyForPickupOrdersList(pharmacyId);
        DataStore dataStore = DataStore.getInstance();
        
        JPanel panel = new JPanel(new BorderLayout(10, 10));
        panel.setBorder(new EmptyBorder(20, 0, 0, 0));
        panel.setBackground(new Color(245, 245, 245));
        
        // Title
        JLabel lblTitle = new JLabel("Ready for Pickup (Paid Orders)");
        lblTitle.setFont(new Font("SansSerif", Font.BOLD, 16));
        panel.add(lblTitle, BorderLayout.NORTH);
        
        // Cards container with vertical box layout for vertical scrolling
        JPanel cardsPanel = new JPanel();
        cardsPanel.setLayout(new BoxLayout(cardsPanel, BoxLayout.Y_AXIS));
        cardsPanel.setBackground(new Color(245, 245, 245));
        cardsPanel.setBorder(new EmptyBorder(15, 15, 15, 15));
        
        DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("MMM dd, yyyy HH:mm");
        
        for (Reservation r : orders) {
            User user = dataStore.getUserById(r.getUserId());
            Medicine med = dataStore.getMedicineById(r.getMedicineId());
            
            if (user == null || med == null) continue;
            
            JPanel card = createReadyForPickupCard(r, user, med, dateFormatter);
            cardsPanel.add(card);
            cardsPanel.add(Box.createVerticalStrut(10)); // Add spacing between cards
        }
        
        if (orders.isEmpty()) {
            JLabel lblEmpty = new JLabel("No orders ready for pickup.");
            lblEmpty.setFont(new Font("SansSerif", Font.PLAIN, 14));
            lblEmpty.setForeground(new Color(150, 150, 150));
            lblEmpty.setAlignmentX(Component.CENTER_ALIGNMENT);
            cardsPanel.add(Box.createVerticalGlue());
            cardsPanel.add(lblEmpty);
            cardsPanel.add(Box.createVerticalGlue());
        }
        
        JScrollPane scroll = new JScrollPane(cardsPanel);
        scroll.setBorder(BorderFactory.createLineBorder(new Color(200, 200, 200)));
        scroll.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);
        // Make scrollbar faster
        JScrollBar verticalScrollBar = scroll.getVerticalScrollBar();
        verticalScrollBar.setUnitIncrement(20);
        verticalScrollBar.setBlockIncrement(100);
        panel.add(scroll, BorderLayout.CENTER);
        
        // Refresh button
        JPanel btnPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        JButton btnRefresh = LoginView.createSecondaryButton("Refresh");
        btnRefresh.addActionListener(e -> {
            if (currentDashboardView != null) {
                showDashboardContent(currentDashboardView);
            }
        });
        btnPanel.add(btnRefresh);
        panel.add(btnPanel, BorderLayout.SOUTH);
        
        return panel;
    }

    private JPanel createReadyForPickupCard(Reservation r, User user, Medicine med, DateTimeFormatter formatter) {
        JPanel card = new JPanel(new BorderLayout(15, 10));
        card.setBackground(Color.WHITE);
        card.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(new Color(200, 200, 200), 1),
            new EmptyBorder(12, 15, 12, 15)));
        card.setPreferredSize(new Dimension(600, 120)); // Same size as pending orders cards
        card.setMaximumSize(new Dimension(Integer.MAX_VALUE, 120));
        card.setMinimumSize(new Dimension(600, 120));
        
        // Left panel - Order info
        JPanel leftPanel = new JPanel();
        leftPanel.setLayout(new BoxLayout(leftPanel, BoxLayout.Y_AXIS));
        leftPanel.setOpaque(false);
        
        // Reference number
        JLabel lblRef = new JLabel("Ref: " + r.getReferenceNumber());
        lblRef.setFont(new Font("SansSerif", Font.BOLD, 13));
        lblRef.setForeground(new Color(70, 130, 180));
        leftPanel.add(lblRef);
        leftPanel.add(Box.createVerticalStrut(6));
        
        // Customer name
        JLabel lblCustomer = new JLabel(user.getFullName());
        lblCustomer.setFont(new Font("SansSerif", Font.BOLD, 14));
        leftPanel.add(lblCustomer);
        leftPanel.add(Box.createVerticalStrut(5));
        
        // Medicine
        JLabel lblMedicine = new JLabel(med.getBrandName());
        lblMedicine.setFont(new Font("SansSerif", Font.PLAIN, 13));
        leftPanel.add(lblMedicine);
        leftPanel.add(Box.createVerticalStrut(5));
        
        // Quantity and Total
        JLabel lblQtyTotal = new JLabel("Qty: " + r.getQuantity() + " | PHP " + 
            String.format("%.2f", r.getTotalPrice()));
        lblQtyTotal.setFont(new Font("SansSerif", Font.PLAIN, 12));
        leftPanel.add(lblQtyTotal);
        leftPanel.add(Box.createVerticalStrut(5));
        
        // Date
        JLabel lblDate = new JLabel("Ordered: " + r.getReservationTime().format(formatter));
        lblDate.setFont(new Font("SansSerif", Font.PLAIN, 11));
        lblDate.setForeground(new Color(100, 100, 100));
        leftPanel.add(lblDate);
        if (r.getStatus() != models.Reservation.ReservationStatus.CANCELLED) {
            leftPanel.add(Box.createVerticalStrut(2));
            JLabel lblPickup = new JLabel("Pick up by: " + r.getExpirationTime().format(formatter));
            lblPickup.setFont(new Font("SansSerif", Font.BOLD, 11));
            lblPickup.setForeground(new Color(25, 118, 210));
            leftPanel.add(lblPickup);
        }
        
        card.add(leftPanel, BorderLayout.CENTER);
        
        // Received button on the right side
        JButton btnReceived = LoginView.createSuccessButton("Received");
        btnReceived.setBorder(new EmptyBorder(6, 12, 6, 12));
        btnReceived.setPreferredSize(new Dimension(105, 30));
        btnReceived.setMinimumSize(new Dimension(105, 30));
        btnReceived.addActionListener(e -> {
            String result = controller.completeReservation(r.getId());
            JOptionPane.showMessageDialog(card, result, "Order Status", 
                result.contains("successfully") ? JOptionPane.INFORMATION_MESSAGE : JOptionPane.ERROR_MESSAGE);
            // Refresh dashboard to remove from ready for pickup and update Today's Sales
            refreshDashboard();
            if (currentDashboardView != null) {
                showDashboardContent(currentDashboardView);
            }
        });
        // Align button to center vertically on the right
        JPanel buttonContainer = new JPanel(new BorderLayout());
        buttonContainer.setOpaque(false);
        buttonContainer.add(btnReceived, BorderLayout.CENTER);
        card.add(buttonContainer, BorderLayout.EAST);
        
        return card;
    }


    private String str(Object o) { return o != null ? o.toString() : ""; }
}
