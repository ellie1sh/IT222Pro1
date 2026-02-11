package client.views;

import client.controllers.UserController;
import javax.swing.*;
import javax.swing.border.*;
import javax.swing.table.*;
import java.awt.*;

/**
 * Customer/Resident view with medicine-first search flow:
 *   1. User searches for a medicine (e.g., "Biogesic")
 *   2. System displays pharmacies where that medicine is available
 *   3. User selects a pharmacy row and clicks Reserve
 *   4. Payment method dialog with online/offsite options
 */
public class UserView extends JPanel {
    private final MainFrame mainFrame;
    private final UserController controller;
    private int userId = -1;

    // Search & results
    private JTextField txtSearch;
    private JTable resultsTable;
    private DefaultTableModel resultsTableModel;

    // Reservations
    private JTable reservationsTable;
    private DefaultTableModel reservationsTableModel;

    private JLabel lblWelcome;

    public UserView(MainFrame mainFrame) {
        this.mainFrame = mainFrame;
        this.controller = mainFrame.getUserController();
        initComponents();
    }

    public void setUserId(int id) { this.userId = id; }
    public void setUserName(String name) {
        lblWelcome.setText("Welcome, " + name + "!");
    }

    private void initComponents() {
        setLayout(new BorderLayout());
        setBackground(new Color(245, 245, 245));

        // Top bar
        JPanel topBar = new JPanel(new BorderLayout());
        topBar.setBackground(new Color(245, 124, 0));
        topBar.setBorder(new EmptyBorder(12, 20, 12, 20));

        lblWelcome = new JLabel("Welcome, Customer!");
        lblWelcome.setFont(new Font("SansSerif", Font.BOLD, 18));
        lblWelcome.setForeground(Color.WHITE);
        topBar.add(lblWelcome, BorderLayout.WEST);

        JButton btnLogout = LoginView.createDangerButton("Logout");
        btnLogout.addActionListener(e -> mainFrame.handleLogout());
        topBar.add(btnLogout, BorderLayout.EAST);

        add(topBar, BorderLayout.NORTH);

        // Tabs
        JTabbedPane tabs = new JTabbedPane();
        tabs.setFont(new Font("SansSerif", Font.PLAIN, 13));
        tabs.setBorder(new EmptyBorder(10, 10, 10, 10));

        tabs.addTab("Search & Reserve", createSearchPanel());
        tabs.addTab("My Reservations", createReservationsPanel());

        add(tabs, BorderLayout.CENTER);
    }

    // ==================== SEARCH & RESERVE PANEL ====================

    private JPanel createSearchPanel() {
        JPanel panel = new JPanel(new BorderLayout(10, 10));
        panel.setBorder(new EmptyBorder(15, 15, 15, 15));

        // --- Top: search bar ---
        JPanel searchSection = new JPanel(new BorderLayout(8, 8));
        searchSection.setBorder(BorderFactory.createTitledBorder(
            BorderFactory.createLineBorder(new Color(200, 200, 200)),
            "Search for Medicine", TitledBorder.LEFT, TitledBorder.TOP,
            new Font("SansSerif", Font.BOLD, 13)));

        JPanel searchRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 6));
        searchRow.add(new JLabel("Medicine Name:"));
        txtSearch = new JTextField(25);
        txtSearch.setFont(new Font("SansSerif", Font.PLAIN, 13));
        searchRow.add(txtSearch);

        JButton btnSearch = LoginView.createPrimaryButton("Search");
        btnSearch.addActionListener(e -> performSearch());
        searchRow.add(btnSearch);

        JLabel lblHint = new JLabel("Search by brand name, generic name, or dosage (e.g., Biogesic, Paracetamol)");
        lblHint.setFont(new Font("SansSerif", Font.ITALIC, 11));
        lblHint.setForeground(Color.GRAY);
        lblHint.setBorder(new EmptyBorder(0, 12, 4, 0));

        searchSection.add(searchRow, BorderLayout.CENTER);
        searchSection.add(lblHint, BorderLayout.SOUTH);
        panel.add(searchSection, BorderLayout.NORTH);

        txtSearch.addActionListener(e -> performSearch());

        // --- Center: results table ---
        JPanel resultsSection = new JPanel(new BorderLayout(5, 5));
        resultsSection.setBorder(BorderFactory.createTitledBorder(
            BorderFactory.createLineBorder(new Color(200, 200, 200)),
            "Available at These Pharmacies", TitledBorder.LEFT, TitledBorder.TOP,
            new Font("SansSerif", Font.BOLD, 13)));

        // Columns: pharmacy first, then medicine details
        String[] cols = {"ID", "Pharmacy", "Brand Name", "Generic Name", "Dosage", "Price (PHP)", "Available", "Category"};
        resultsTableModel = new DefaultTableModel(cols, 0) {
            public boolean isCellEditable(int r, int c) { return false; }
        };
        resultsTable = AdminView.createStyledTable(resultsTableModel);

        // Hide the ID column from display but keep it in the model
        resultsTable.getColumnModel().getColumn(0).setMinWidth(0);
        resultsTable.getColumnModel().getColumn(0).setMaxWidth(0);
        resultsTable.getColumnModel().getColumn(0).setPreferredWidth(0);

        JScrollPane scroll = new JScrollPane(resultsTable);
        scroll.setBorder(BorderFactory.createLineBorder(new Color(200, 200, 200)));
        resultsSection.add(scroll, BorderLayout.CENTER);

        // Reserve button under the table, right-aligned
        JPanel bottomBar = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 6));
        JButton btnReserve = LoginView.createSuccessButton("Reserve Selected Medicine");
        btnReserve.addActionListener(e -> reserveSelectedMedicine());
        bottomBar.add(btnReserve);
        resultsSection.add(bottomBar, BorderLayout.SOUTH);

        panel.add(resultsSection, BorderLayout.CENTER);

        return panel;
    }

    // ==================== RESERVATIONS PANEL ====================

    private JPanel createReservationsPanel() {
        JPanel panel = new JPanel(new BorderLayout(10, 10));
        panel.setBorder(new EmptyBorder(15, 15, 15, 15));

        // Buttons at top-left, note at right
        JPanel topPanel = new JPanel(new BorderLayout());

        JPanel btnPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        JButton btnCancel = LoginView.createDangerButton("Cancel Reservation");
        btnCancel.setToolTipText("Cancel a PENDING reservation");
        btnCancel.addActionListener(e -> cancelReservation());
        JButton btnRefresh = LoginView.createSecondaryButton("Refresh");
        btnRefresh.addActionListener(e -> refreshReservations());
        btnPanel.add(btnCancel);
        btnPanel.add(btnRefresh);
        topPanel.add(btnPanel, BorderLayout.WEST);

        JLabel lblNote = new JLabel("Reservations expire after 24 hours. Pick up before expiration.");
        lblNote.setFont(new Font("SansSerif", Font.ITALIC, 11));
        lblNote.setForeground(Color.GRAY);
        lblNote.setBorder(new EmptyBorder(0, 15, 0, 10));
        topPanel.add(lblNote, BorderLayout.CENTER);

        panel.add(topPanel, BorderLayout.NORTH);

        String[] cols = {"ID", "Medicine", "Pharmacy", "Qty", "Total (PHP)", "Payment", "Pay Status",
                         "Reserved At", "Expires At", "Status"};
        reservationsTableModel = new DefaultTableModel(cols, 0) {
            public boolean isCellEditable(int r, int c) { return false; }
        };
        reservationsTable = AdminView.createStyledTable(reservationsTableModel);
        JScrollPane scroll = new JScrollPane(reservationsTable);
        scroll.setBorder(BorderFactory.createLineBorder(new Color(200, 200, 200)));
        panel.add(scroll, BorderLayout.CENTER);

        return panel;
    }

    // ==================== ACTIONS ====================

    private void performSearch() {
        String term = txtSearch.getText().trim();
        if (term.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Please enter a medicine name to search!",
                "Info", JOptionPane.INFORMATION_MESSAGE);
            return;
        }

        resultsTableModel.setRowCount(0);

        // Search across ALL pharmacies (pharmacyId = -1)
        Object[][] data = controller.searchMedicines(-1, term);

        if (data.length == 0) {
            JOptionPane.showMessageDialog(this,
                "No pharmacies found with \"" + term + "\" in stock.\nTry a different search term.",
                "No Results", JOptionPane.INFORMATION_MESSAGE);
            return;
        }

        for (Object[] row : data) {
            // row from controller: ID, Pharmacy, Brand, Generic, Dosage, Price, Available, Category, Address
            // table columns:       ID, Pharmacy, Brand, Generic, Dosage, Price, Available, Category
            resultsTableModel.addRow(new Object[]{
                row[0], row[1], row[2], row[3], row[4], row[5], row[6], row[7]
            });
        }
    }

    private void reserveSelectedMedicine() {
        int row = resultsTable.getSelectedRow();
        if (row < 0) {
            JOptionPane.showMessageDialog(this, "Please search for a medicine and select a row to reserve!",
                "Info", JOptionPane.INFORMATION_MESSAGE);
            return;
        }

        int medicineId = (int) resultsTableModel.getValueAt(row, 0);
        String pharmacy = resultsTableModel.getValueAt(row, 1).toString();
        String medName = resultsTableModel.getValueAt(row, 2).toString();
        String price = resultsTableModel.getValueAt(row, 5).toString();
        int available = (int) resultsTableModel.getValueAt(row, 6);

        if (available <= 0) {
            JOptionPane.showMessageDialog(this, "This medicine is out of stock!",
                "Out of Stock", JOptionPane.WARNING_MESSAGE);
            return;
        }

        // --- Reservation dialog ---
        JPanel dialogPanel = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(4, 6, 4, 6);
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.anchor = GridBagConstraints.WEST;

        // Info header
        gbc.gridx = 0; gbc.gridy = 0; gbc.gridwidth = 2;
        JLabel lblPharm = new JLabel("Pharmacy: " + pharmacy);
        lblPharm.setFont(new Font("SansSerif", Font.BOLD, 13));
        dialogPanel.add(lblPharm, gbc);

        gbc.gridy = 1;
        JLabel lblMed = new JLabel("Medicine: " + medName + "  |  PHP " + price + " per unit");
        lblMed.setFont(new Font("SansSerif", Font.PLAIN, 12));
        dialogPanel.add(lblMed, gbc);

        gbc.gridy = 2;
        dialogPanel.add(new JLabel("Available: " + available + " units"), gbc);
        gbc.gridwidth = 1;

        gbc.gridy = 3; gbc.gridx = 0; gbc.gridwidth = 2;
        dialogPanel.add(new JSeparator(), gbc);
        gbc.gridwidth = 1;

        // Quantity
        gbc.gridy = 4; gbc.gridx = 0;
        dialogPanel.add(new JLabel("Quantity:"), gbc);
        gbc.gridx = 1;
        JSpinner spinQty = new JSpinner(new SpinnerNumberModel(1, 1, available, 1));
        dialogPanel.add(spinQty, gbc);

        // Payment method
        gbc.gridy = 5; gbc.gridx = 0; gbc.gridwidth = 2;
        dialogPanel.add(new JSeparator(), gbc);
        gbc.gridwidth = 1;

        gbc.gridy = 6; gbc.gridx = 0; gbc.gridwidth = 2;
        JLabel lblPay = new JLabel("Payment Method:");
        lblPay.setFont(new Font("SansSerif", Font.BOLD, 12));
        dialogPanel.add(lblPay, gbc);

        gbc.gridy = 7;
        JPanel payPanel = new JPanel(new GridLayout(3, 1, 4, 4));
        ButtonGroup payGroup = new ButtonGroup();
        JRadioButton rbOnlineBank = new JRadioButton("Online Bank  (Pay now - Ready for pick-up)");
        JRadioButton rbEPayment  = new JRadioButton("E-Payment  (Pay now - Ready for pick-up)");
        JRadioButton rbStore     = new JRadioButton("Pay at Store  (Reserved - Pending approval)");
        rbStore.setSelected(true);
        payGroup.add(rbOnlineBank); payGroup.add(rbEPayment); payGroup.add(rbStore);
        payPanel.add(rbOnlineBank); payPanel.add(rbEPayment); payPanel.add(rbStore);
        dialogPanel.add(payPanel, gbc);

        int option = JOptionPane.showConfirmDialog(this, dialogPanel,
            "Reserve Medicine", JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);

        if (option == JOptionPane.OK_OPTION) {
            int qty = (int) spinQty.getValue();
            String paymentMethod;
            if (rbOnlineBank.isSelected()) paymentMethod = "ONLINE_BANK";
            else if (rbEPayment.isSelected()) paymentMethod = "E_PAYMENT";
            else paymentMethod = "PAY_AT_STORE";

            // Online payment confirmation step
            if (!"PAY_AT_STORE".equals(paymentMethod)) {
                String payType = rbOnlineBank.isSelected() ? "Online Bank" : "E-Payment";
                double total = Double.parseDouble(price) * qty;
                int payConfirm = JOptionPane.showConfirmDialog(this,
                    "Confirm " + payType + " Payment\n\n" +
                    "Total: PHP " + String.format("%.2f", total) + "\n\n" +
                    "Proceed with payment?",
                    "Payment Confirmation", JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE);
                if (payConfirm != JOptionPane.YES_OPTION) return;
            }

            String result = controller.reserveMedicine(userId, medicineId, qty, paymentMethod);
            if (result.startsWith("SUCCESS|")) {
                JOptionPane.showMessageDialog(this,
                    result.substring(8) + "\n\nPick up at: " + pharmacy,
                    "Reservation Successful", JOptionPane.INFORMATION_MESSAGE);
                performSearch(); // refresh results
                refreshReservations();
            } else if (result.startsWith("ERROR|")) {
                JOptionPane.showMessageDialog(this, result.substring(6),
                    "Reservation Failed", JOptionPane.ERROR_MESSAGE);
            } else {
                JOptionPane.showMessageDialog(this, result);
            }
        }
    }

    private void cancelReservation() {
        int row = reservationsTable.getSelectedRow();
        if (row < 0) {
            JOptionPane.showMessageDialog(this, "Select a reservation to cancel!"); return;
        }
        String status = reservationsTableModel.getValueAt(row, 9).toString();
        if (!"PENDING".equals(status)) {
            JOptionPane.showMessageDialog(this, "Only PENDING reservations can be cancelled!",
                "Invalid Action", JOptionPane.WARNING_MESSAGE);
            return;
        }
        int confirm = JOptionPane.showConfirmDialog(this,
            "Are you sure you want to cancel this reservation?",
            "Confirm Cancel", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
        if (confirm == JOptionPane.YES_OPTION) {
            int resId = (int) reservationsTableModel.getValueAt(row, 0);
            String msg = controller.cancelReservation(resId, userId);
            JOptionPane.showMessageDialog(this, msg);
            refreshReservations();
        }
    }

    // ==================== REFRESH ====================

    private void refreshReservations() {
        reservationsTableModel.setRowCount(0);
        Object[][] data = controller.getUserReservations(userId);
        for (Object[] row : data) reservationsTableModel.addRow(row);
    }

    public void refresh() {
        refreshReservations();
        resultsTableModel.setRowCount(0);
        txtSearch.setText("");
    }
}
