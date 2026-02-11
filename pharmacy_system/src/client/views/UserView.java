package client.views;

import client.controllers.UserController;
import models.PrescriptionRequest;
import models.Pharmacy;
import javax.swing.*;
import javax.swing.border.*;
import java.awt.*;
import java.awt.datatransfer.DataFlavor;
import java.awt.dnd.*;
import java.io.File;
import java.util.List;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;

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
    private JComboBox<String> filterCombo;
    private JPanel productsCardsPanel;

    // Reservations
    private JPanel reservationsCardsPanel;

    // Transaction History
    private JPanel transactionHistoryCardsPanel;

    // Prescription Reserve
    private File selectedPrescriptionFile;
    private JLabel prescriptionFileLabel;
    private volatile boolean prescriptionSending = false;
    private JPanel prescriptionRequestsPanel;

    // Navigation
    private JPanel searchProductsNavCard;
    private JPanel reservationsNavCard;
    private JPanel transactionHistoryNavCard;
    private JPanel prescriptionNavCard;
    private JPanel contentPanel;
    private CardLayout contentCardLayout;
    private String currentNav = "Search Products";

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

        // Main content with rectangular nav buttons (like pharma side)
        JPanel mainPanel = new JPanel(new BorderLayout(10, 10));
        mainPanel.setBorder(new EmptyBorder(10, 10, 10, 10));
        mainPanel.setBackground(new Color(245, 245, 245));

        // Navigation buttons row
        JPanel navPanel = new JPanel(new BorderLayout());
        navPanel.setBackground(new Color(245, 245, 245));

        JPanel leftNavPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 12, 0));
        leftNavPanel.setBackground(new Color(245, 245, 245));
        searchProductsNavCard = createNavButton("Search Products", new Color(25, 118, 210));
        makeNavCardClickable(searchProductsNavCard, "Search Products");
        leftNavPanel.add(searchProductsNavCard);
        reservationsNavCard = createNavButton("Reservations", new Color(245, 124, 0));
        makeNavCardClickable(reservationsNavCard, "Reservations");
        leftNavPanel.add(reservationsNavCard);
        transactionHistoryNavCard = createNavButton("Transaction History", new Color(46, 125, 50));
        makeNavCardClickable(transactionHistoryNavCard, "Transaction History");
        leftNavPanel.add(transactionHistoryNavCard);
        navPanel.add(leftNavPanel, BorderLayout.WEST);

        prescriptionNavCard = createGreenNavButton("Reserve Medication with Prescription");
        prescriptionNavCard.setPreferredSize(new Dimension(360, 50));
        makeNavCardClickable(prescriptionNavCard, "Reserve Medication with Prescription", true);
        navPanel.add(prescriptionNavCard, BorderLayout.EAST);

        mainPanel.add(navPanel, BorderLayout.NORTH);

        // Content area
        contentCardLayout = new CardLayout();
        contentPanel = new JPanel(contentCardLayout);
        contentPanel.setBackground(new Color(245, 245, 245));
        contentPanel.add(createSearchPanel(), "Search Products");
        contentPanel.add(createReservationsPanel(), "Reservations");
        contentPanel.add(createTransactionHistoryPanel(), "Transaction History");
        contentPanel.add(createPrescriptionReservePanel(), "Reserve Medication with Prescription");
        mainPanel.add(contentPanel, BorderLayout.CENTER);

        updateNavSelection("Search Products");
        add(mainPanel, BorderLayout.CENTER);
    }

    private JPanel createNavButton(String label, Color accentColor) {
        JPanel card = new JPanel(new BorderLayout(3, 3));
        card.setBackground(Color.WHITE);
        card.setOpaque(true);
        card.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(new Color(220, 220, 220), 1),
            new EmptyBorder(12, 20, 12, 20)));
        card.setPreferredSize(new Dimension(180, 50));

        JLabel lbl = new JLabel(label, SwingConstants.CENTER);
        lbl.setFont(new Font("SansSerif", Font.BOLD, 14));
        lbl.setForeground(accentColor);
        card.add(lbl, BorderLayout.CENTER);

        return card;
    }

    private JPanel createGreenNavButton(String label) {
        Color green = new Color(34, 139, 34);
        JPanel card = new JPanel(new BorderLayout(3, 3));
        card.setBackground(green);
        card.setOpaque(true);
        card.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(green, 2),
            new EmptyBorder(12, 20, 12, 20)));

        JLabel lbl = new JLabel(label, SwingConstants.CENTER);
        lbl.setFont(new Font("SansSerif", Font.BOLD, 14));
        lbl.setForeground(Color.WHITE);
        card.add(lbl, BorderLayout.CENTER);

        return card;
    }

    private void makeNavCardClickable(JPanel card, String navKey) {
        makeNavCardClickable(card, navKey, false);
    }

    private void makeNavCardClickable(JPanel card, String navKey, boolean keepGreen) {
        card.setCursor(new Cursor(Cursor.HAND_CURSOR));
        card.addMouseListener(new java.awt.event.MouseAdapter() {
            @Override
            public void mouseClicked(java.awt.event.MouseEvent e) {
                showNavContent(navKey);
            }
            @Override
            public void mouseEntered(java.awt.event.MouseEvent e) {
                if (!keepGreen && !navKey.equals(currentNav)) {
                    card.setBackground(new Color(250, 250, 250));
                }
            }
            @Override
            public void mouseExited(java.awt.event.MouseEvent e) {
                if (!keepGreen && !navKey.equals(currentNav)) {
                    card.setBackground(Color.WHITE);
                }
            }
        });
    }

    private void showNavContent(String navKey) {
        currentNav = navKey;
        contentCardLayout.show(contentPanel, navKey);
        updateNavSelection(navKey);
        if ("Reservations".equals(navKey)) refreshReservations();
        else if ("Transaction History".equals(navKey)) refreshTransactionHistory();
        // Prescription panel has no refresh
    }

    private void updateNavSelection(String selected) {
        resetNavCard(searchProductsNavCard);
        resetNavCard(reservationsNavCard);
        resetNavCard(transactionHistoryNavCard);
        resetPrescriptionNavCard();
        if ("Search Products".equals(selected) && searchProductsNavCard != null) {
            highlightNavCard(searchProductsNavCard, new Color(25, 118, 210));
        } else if ("Reservations".equals(selected) && reservationsNavCard != null) {
            highlightNavCard(reservationsNavCard, new Color(245, 124, 0));
        } else if ("Transaction History".equals(selected) && transactionHistoryNavCard != null) {
            highlightNavCard(transactionHistoryNavCard, new Color(46, 125, 50));
        } else if ("Reserve Medication with Prescription".equals(selected) && prescriptionNavCard != null) {
            highlightPrescriptionNavCard();
        }
    }

    private void resetNavCard(JPanel card) {
        if (card == null) return;
        card.setBackground(Color.WHITE);
        card.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(new Color(220, 220, 220), 1),
            new EmptyBorder(12, 20, 12, 20)));
        for (Component comp : card.getComponents()) {
            if (comp instanceof JLabel) {
                ((JLabel) comp).setForeground(new Color(100, 100, 100));
            }
        }
        card.revalidate();
        card.repaint();
    }

    private static final Color PRESCRIPTION_GREEN = new Color(34, 139, 34);

    private void resetPrescriptionNavCard() {
        if (prescriptionNavCard == null) return;
        prescriptionNavCard.setBackground(PRESCRIPTION_GREEN);
        prescriptionNavCard.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(PRESCRIPTION_GREEN, 2),
            new EmptyBorder(12, 20, 12, 20)));
        for (Component comp : prescriptionNavCard.getComponents()) {
            if (comp instanceof JLabel) {
                ((JLabel) comp).setForeground(Color.WHITE);
            }
        }
        prescriptionNavCard.revalidate();
        prescriptionNavCard.repaint();
    }

    private void highlightPrescriptionNavCard() {
        if (prescriptionNavCard == null) return;
        prescriptionNavCard.setBackground(PRESCRIPTION_GREEN);
        prescriptionNavCard.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(new Color(0, 100, 0), 3),
            new EmptyBorder(12, 20, 12, 20)));
        for (Component comp : prescriptionNavCard.getComponents()) {
            if (comp instanceof JLabel) {
                ((JLabel) comp).setForeground(Color.WHITE);
            }
        }
        prescriptionNavCard.revalidate();
        prescriptionNavCard.repaint();
    }

    private void highlightNavCard(JPanel card, Color accentColor) {
        if (card == null) return;
        card.setBackground(new Color(240, 248, 255));
        card.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(accentColor, 3),
            new EmptyBorder(12, 20, 12, 20)));
        for (Component comp : card.getComponents()) {
            if (comp instanceof JLabel) {
                ((JLabel) comp).setForeground(accentColor);
            }
        }
        card.revalidate();
        card.repaint();
    }

    // ==================== SEARCH & RESERVE PANEL ====================

    private JPanel createSearchPanel() {
        JPanel panel = new JPanel(new BorderLayout(10, 10));
        panel.setBorder(new EmptyBorder(15, 15, 15, 15));

        // Search bar (same style as pharmacist side)
        JPanel searchBarPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 10));
        searchBarPanel.setBackground(new Color(245, 245, 245));

        txtSearch = new JTextField(50);
        txtSearch.setFont(new Font("SansSerif", Font.PLAIN, 14));
        txtSearch.setText("Search products...");
        txtSearch.setForeground(new Color(150, 150, 150));
        txtSearch.addFocusListener(new FocusAdapter() {
            @Override
            public void focusGained(FocusEvent e) {
                if ("Search products...".equals(txtSearch.getText())) {
                    txtSearch.setText("");
                    txtSearch.setForeground(Color.BLACK);
                }
            }
            @Override
            public void focusLost(FocusEvent e) {
                if (txtSearch.getText().trim().isEmpty()) {
                    txtSearch.setText("Search products...");
                    txtSearch.setForeground(new Color(150, 150, 150));
                }
            }
        });
        txtSearch.addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                if (e.getKeyCode() == KeyEvent.VK_ENTER) {
                    performSearch();
                }
            }
        });
        searchBarPanel.add(txtSearch);

        filterCombo = new JComboBox<>(
            new String[]{"All Categories", "OTC Drugs", "Vitamins", "Personal Care"});
        filterCombo.setFont(new Font("SansSerif", Font.PLAIN, 13));
        filterCombo.addActionListener(e -> refreshProductsDisplay());
        searchBarPanel.add(filterCombo);

        JButton btnSearch = LoginView.createSecondaryButton("Search");
        btnSearch.setBackground(new Color(100, 100, 100));
        btnSearch.setForeground(Color.WHITE);
        btnSearch.addActionListener(e -> performSearch());
        searchBarPanel.add(btnSearch);

        panel.add(searchBarPanel, BorderLayout.NORTH);

        // --- Center: product cards (rectangular form) ---
        JPanel resultsSection = new JPanel(new BorderLayout(5, 5));
        resultsSection.setBorder(BorderFactory.createTitledBorder(
            BorderFactory.createLineBorder(new Color(200, 200, 200)),
            "Available Products", TitledBorder.LEFT, TitledBorder.TOP,
            new Font("SansSerif", Font.BOLD, 13)));

        productsCardsPanel = new JPanel();
        productsCardsPanel.setLayout(new BoxLayout(productsCardsPanel, BoxLayout.Y_AXIS));
        productsCardsPanel.setBackground(new Color(245, 245, 245));

        JScrollPane scroll = new JScrollPane(productsCardsPanel);
        scroll.setBorder(BorderFactory.createLineBorder(new Color(200, 200, 200)));
        scroll.getVerticalScrollBar().setUnitIncrement(20);
        resultsSection.add(scroll, BorderLayout.CENTER);

        panel.add(resultsSection, BorderLayout.CENTER);

        // Load all products on init (no search = show all)
        refreshProductsDisplay();

        return panel;
    }

    // ==================== TRANSACTION HISTORY PANEL ====================

    private JPanel createTransactionHistoryPanel() {
        JPanel panel = new JPanel(new BorderLayout(10, 10));
        panel.setBorder(new EmptyBorder(15, 15, 15, 15));
        panel.setBackground(new Color(245, 245, 245));

        JPanel topPanel = new JPanel(new BorderLayout());
        topPanel.setBackground(new Color(245, 245, 245));
        JLabel lblNote = new JLabel("Past transactions (completed, cancelled, or expired reservations).");
        lblNote.setFont(new Font("SansSerif", Font.ITALIC, 11));
        lblNote.setForeground(Color.GRAY);
        topPanel.add(lblNote, BorderLayout.WEST);

        JPanel btnPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        JButton btnRefresh = LoginView.createSecondaryButton("Refresh");
        btnRefresh.addActionListener(e -> refreshTransactionHistory());
        btnPanel.add(btnRefresh);
        topPanel.add(btnPanel, BorderLayout.EAST);

        panel.add(topPanel, BorderLayout.NORTH);

        transactionHistoryCardsPanel = new JPanel();
        transactionHistoryCardsPanel.setLayout(new BoxLayout(transactionHistoryCardsPanel, BoxLayout.Y_AXIS));
        transactionHistoryCardsPanel.setBackground(new Color(245, 245, 245));

        JScrollPane scroll = new JScrollPane(transactionHistoryCardsPanel);
        scroll.setBorder(BorderFactory.createLineBorder(new Color(200, 200, 200)));
        scroll.getVerticalScrollBar().setUnitIncrement(20);
        panel.add(scroll, BorderLayout.CENTER);

        return panel;
    }

    private void refreshTransactionHistory() {
        if (transactionHistoryCardsPanel == null) return;

        transactionHistoryCardsPanel.removeAll();
        Object[][] data = controller.getUserReservations(userId);
        List<PrescriptionRequest> prescHistory = controller.getPrescriptionRequestsForHistoryByUser(userId);

        for (Object[] row : data) {
            String status = row[10].toString();
            // Show only completed, cancelled, or expired (past transactions)
            if ("PENDING".equals(status) || "CONFIRMED".equals(status)) continue;

            transactionHistoryCardsPanel.add(createTransactionHistoryCard(
                row[1].toString(), row[2].toString(), row[3].toString(),
                (int) row[4], row[5].toString(), row[6].toString(), row[7].toString(),
                row[8].toString(), row[9].toString(), status));
            transactionHistoryCardsPanel.add(Box.createVerticalStrut(10));
        }

        for (PrescriptionRequest pr : prescHistory) {
            transactionHistoryCardsPanel.add(createPrescriptionTransactionHistoryCard(pr));
            transactionHistoryCardsPanel.add(Box.createVerticalStrut(10));
        }

        if (transactionHistoryCardsPanel.getComponentCount() == 0) {
            JLabel lblEmpty = new JLabel("No transaction history yet.");
            lblEmpty.setFont(new Font("SansSerif", Font.PLAIN, 14));
            lblEmpty.setForeground(new Color(150, 150, 150));
            lblEmpty.setAlignmentX(0.5f);
            transactionHistoryCardsPanel.add(Box.createVerticalGlue());
            transactionHistoryCardsPanel.add(lblEmpty);
            transactionHistoryCardsPanel.add(Box.createVerticalGlue());
        }

        transactionHistoryCardsPanel.revalidate();
        transactionHistoryCardsPanel.repaint();
    }

    private JPanel createTransactionHistoryCard(String refNum, String medicine, String pharmacy,
                                               int qty, String total, String payment, String payStatus,
                                               String reservedAt, String expiresAt, String status) {
        JPanel card = new JPanel(new BorderLayout(15, 10));
        card.setBackground(Color.WHITE);
        card.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(new Color(220, 220, 220), 1),
            new EmptyBorder(12, 15, 12, 15)));
        card.setMaximumSize(new Dimension(Integer.MAX_VALUE, 120));

        JPanel leftPanel = new JPanel();
        leftPanel.setLayout(new BoxLayout(leftPanel, BoxLayout.Y_AXIS));
        leftPanel.setOpaque(false);

        JLabel lblRef = new JLabel(refNum);
        lblRef.setFont(new Font("SansSerif", Font.BOLD, 13));
        lblRef.setForeground(new Color(25, 118, 210));
        lblRef.setAlignmentX(0);
        leftPanel.add(lblRef);
        leftPanel.add(Box.createVerticalStrut(4));

        JLabel lblMain = new JLabel(medicine + "  @  " + pharmacy);
        lblMain.setFont(new Font("SansSerif", Font.PLAIN, 13));
        lblMain.setAlignmentX(0);
        leftPanel.add(lblMain);
        leftPanel.add(Box.createVerticalStrut(2));

        JLabel lblDetails = new JLabel("Qty: " + qty + "  |  PHP " + total + "  |  " + payment + "  |  " + payStatus);
        lblDetails.setFont(new Font("SansSerif", Font.PLAIN, 12));
        lblDetails.setForeground(new Color(100, 100, 100));
        lblDetails.setAlignmentX(0);
        leftPanel.add(lblDetails);
        leftPanel.add(Box.createVerticalStrut(2));

        String timesText = "Reserved: " + reservedAt;
        if (!"CANCELLED".equals(status)) {
            timesText += "  |  Expires: " + expiresAt;
        }
        timesText += "  |  Status: " + status;
        JLabel lblTimes = new JLabel(timesText);
        lblTimes.setFont(new Font("SansSerif", Font.PLAIN, 11));
        lblTimes.setForeground(new Color(120, 120, 120));
        lblTimes.setAlignmentX(0);
        leftPanel.add(lblTimes);

        card.add(leftPanel, BorderLayout.CENTER);
        return card;
    }

    private JPanel createPrescriptionTransactionHistoryCard(PrescriptionRequest pr) {
        JPanel card = new JPanel(new BorderLayout(15, 10));
        card.setBackground(Color.WHITE);
        card.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(new Color(220, 220, 220), 1),
            new EmptyBorder(12, 15, 12, 15)));
        card.setMaximumSize(new Dimension(Integer.MAX_VALUE, 120));

        JPanel leftPanel = new JPanel();
        leftPanel.setLayout(new BoxLayout(leftPanel, BoxLayout.Y_AXIS));
        leftPanel.setOpaque(false);

        String ref = pr.getReferenceNumber() != null ? pr.getReferenceNumber() : ("RX" + String.format("%06d", pr.getId()));
        JLabel lblRef = new JLabel(ref);
        lblRef.setFont(new Font("SansSerif", Font.BOLD, 13));
        lblRef.setForeground(new Color(25, 118, 210));
        lblRef.setAlignmentX(0);
        leftPanel.add(lblRef);
        leftPanel.add(Box.createVerticalStrut(4));

        Pharmacy chosen = controller.getPharmacyById(pr.getChosenPharmacyId());
        String pharmText = chosen != null ? chosen.getName() : "Not chosen";
        String medName = pr.getMedicineName() != null && !pr.getMedicineName().isEmpty()
            ? pr.getMedicineName() : "Prescription";
        JLabel lblMain = new JLabel(medName + "  @  " + pharmText);
        lblMain.setFont(new Font("SansSerif", Font.PLAIN, 13));
        lblMain.setAlignmentX(0);
        leftPanel.add(lblMain);
        leftPanel.add(Box.createVerticalStrut(2));

        String details = "Qty: " + (pr.getMedicineQuantity() > 0 ? pr.getMedicineQuantity() : 1) +
            "  |  PHP " + String.format("%.2f", pr.getMedicineAmount());
        if (pr.getStatus() == models.PrescriptionRequest.Status.CANCELLED) {
            details = "Cancelled";
        }
        JLabel lblDetails = new JLabel(details);
        lblDetails.setFont(new Font("SansSerif", Font.PLAIN, 12));
        lblDetails.setForeground(new Color(100, 100, 100));
        lblDetails.setAlignmentX(0);
        leftPanel.add(lblDetails);
        leftPanel.add(Box.createVerticalStrut(2));

        String status = pr.getStatus().name();
        JLabel lblTimes = new JLabel("Submitted: " + pr.getSubmittedAt() + "  |  Status: " + status);
        lblTimes.setFont(new Font("SansSerif", Font.PLAIN, 11));
        lblTimes.setForeground(new Color(120, 120, 120));
        lblTimes.setAlignmentX(0);
        leftPanel.add(lblTimes);

        card.add(leftPanel, BorderLayout.CENTER);
        return card;
    }

    // ==================== PRESCRIPTION RESERVE PANEL ====================

    private JPanel createPrescriptionReservePanel() {
        JPanel panel = new JPanel(new BorderLayout(10, 10));
        panel.setBorder(new EmptyBorder(15, 15, 15, 15));
        panel.setBackground(new Color(245, 245, 245));

        JPanel centerPanel = new JPanel(new GridBagLayout());
        centerPanel.setBackground(new Color(245, 245, 245));
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(10, 10, 10, 10);
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.anchor = GridBagConstraints.CENTER;

        JLabel lblInstruction = new JLabel("Upload your prescription photo. It will be sent to all pharmacies.");
        lblInstruction.setFont(new Font("SansSerif", Font.PLAIN, 14));
        lblInstruction.setForeground(new Color(80, 80, 80));
        gbc.gridx = 0; gbc.gridy = 0; gbc.gridwidth = 2;
        centerPanel.add(lblInstruction, gbc);

        gbc.gridy = 1; gbc.gridwidth = 2;
        JPanel dropZone = createPrescriptionDropZone();
        centerPanel.add(dropZone, gbc);

        gbc.gridy = 2;
        JButton btnSend = LoginView.createSuccessButton("Send to Pharmacies");
        btnSend.addActionListener(e -> sendPrescription(btnSend));
        centerPanel.add(btnSend, gbc);

        panel.add(centerPanel, BorderLayout.CENTER);
        return panel;
    }

    private JPanel createPrescriptionDropZone() {
        JPanel dropZone = new JPanel(new BorderLayout(10, 10));
        dropZone.setPreferredSize(new Dimension(450, 140));
        dropZone.setMinimumSize(new Dimension(350, 120));
        dropZone.setBackground(new Color(250, 250, 250));
        dropZone.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createDashedBorder(new Color(180, 180, 180), 2, 8, 4, false),
            new EmptyBorder(20, 20, 20, 20)));
        dropZone.setCursor(new Cursor(Cursor.HAND_CURSOR));

        prescriptionFileLabel = new JLabel("<html><center>Drag and drop your prescription here<br>or click to browse</center></html>", SwingConstants.CENTER);
        prescriptionFileLabel.setFont(new Font("SansSerif", Font.PLAIN, 14));
        prescriptionFileLabel.setForeground(new Color(120, 120, 120));
        prescriptionFileLabel.setCursor(new Cursor(Cursor.HAND_CURSOR));
        dropZone.add(prescriptionFileLabel, BorderLayout.CENTER);

        dropZone.addMouseListener(new java.awt.event.MouseAdapter() {
            @Override
            public void mouseClicked(java.awt.event.MouseEvent e) {
                openFileChooser(dropZone);
            }
        });
        prescriptionFileLabel.addMouseListener(new java.awt.event.MouseAdapter() {
            @Override
            public void mouseClicked(java.awt.event.MouseEvent e) {
                openFileChooser(dropZone);
            }
        });

        dropZone.setTransferHandler(new TransferHandler() {
            @Override
            public boolean canImport(TransferSupport support) {
                return support.isDataFlavorSupported(DataFlavor.javaFileListFlavor);
            }
            @Override
            public boolean importData(TransferSupport support) {
                if (!canImport(support)) return false;
                try {
                    @SuppressWarnings("unchecked")
                    List<File> files = (List<File>) support.getTransferable().getTransferData(DataFlavor.javaFileListFlavor);
                    if (files != null && !files.isEmpty()) {
                        File f = files.get(0);
                        String name = f.getName().toLowerCase();
                        if (name.endsWith(".jpg") || name.endsWith(".jpeg") || name.endsWith(".png") ||
                            name.endsWith(".gif") || name.endsWith(".bmp")) {
                            setPrescriptionFile(f);
                            return true;
                        }
                    }
                } catch (Exception ex) {
                    ex.printStackTrace();
                }
                return false;
            }
        });

        new DropTarget(dropZone, DnDConstants.ACTION_COPY, new DropTargetAdapter() {
            @Override
            public void drop(DropTargetDropEvent dtde) {
                dtde.acceptDrop(DnDConstants.ACTION_COPY);
                try {
                    @SuppressWarnings("unchecked")
                    List<File> files = (List<File>) dtde.getTransferable().getTransferData(DataFlavor.javaFileListFlavor);
                    if (files != null && !files.isEmpty()) {
                        File f = files.get(0);
                        String name = f.getName().toLowerCase();
                        if (name.endsWith(".jpg") || name.endsWith(".jpeg") || name.endsWith(".png") ||
                            name.endsWith(".gif") || name.endsWith(".bmp")) {
                            setPrescriptionFile(f);
                        }
                    }
                    dtde.dropComplete(true);
                } catch (Exception ex) {
                    dtde.dropComplete(false);
                }
            }
        });

        return dropZone;
    }

    private void openFileChooser(Component parent) {
        JFileChooser chooser = new JFileChooser();
        chooser.setDialogTitle("Select Prescription Image");
        chooser.setFileFilter(new javax.swing.filechooser.FileNameExtensionFilter("Images (jpg, png, gif, bmp)", "jpg", "jpeg", "png", "gif", "bmp"));
        chooser.setCurrentDirectory(new File(System.getProperty("user.home")));
        java.awt.Window win = SwingUtilities.getWindowAncestor(parent != null ? parent : this);
        if (chooser.showOpenDialog(win != null ? win : this) == JFileChooser.APPROVE_OPTION) {
            setPrescriptionFile(chooser.getSelectedFile());
        }
    }

    private void setPrescriptionFile(File f) {
        if (f != null && f.exists()) {
            selectedPrescriptionFile = f;
            prescriptionFileLabel.setText("<html><center>Selected: " + f.getName() + "</center></html>");
            prescriptionFileLabel.setForeground(new Color(34, 139, 34));
        }
    }

    private void refreshPrescriptionRequests() {
        if (prescriptionRequestsPanel == null) return;
        prescriptionRequestsPanel.removeAll();
        List<PrescriptionRequest> requests = controller.getPendingPrescriptionRequestsByUser(userId);
        List<PrescriptionRequest> chosen = controller.getChosenPrescriptionRequestsByUser(userId);
        String userAddress = controller.getUserAddress(userId);

        if (!chosen.isEmpty()) {
            JLabel lblChosen = new JLabel("Your Confirmed Prescription Pickup");
            lblChosen.setFont(new Font("SansSerif", Font.BOLD, 12));
            lblChosen.setForeground(new Color(25, 118, 210));
            lblChosen.setBorder(new EmptyBorder(2, 2, 6, 2));
            prescriptionRequestsPanel.add(lblChosen);
            for (PrescriptionRequest pr : chosen) {
                JPanel card = createChosenPrescriptionRequestCard(pr);
                prescriptionRequestsPanel.add(card);
                prescriptionRequestsPanel.add(Box.createVerticalStrut(10));
            }
        }

        if (!requests.isEmpty()) {
            JLabel lblPending = new JLabel("Waiting for your pharmacy selection");
            lblPending.setFont(new Font("SansSerif", Font.BOLD, 12));
            lblPending.setForeground(new Color(90, 90, 90));
            lblPending.setBorder(new EmptyBorder(2, 2, 6, 2));
            prescriptionRequestsPanel.add(lblPending);
        }
        for (PrescriptionRequest pr : requests) {
            JPanel card = createPrescriptionRequestCard(pr, userAddress);
            prescriptionRequestsPanel.add(card);
            prescriptionRequestsPanel.add(Box.createVerticalStrut(10));
        }

        if (requests.isEmpty() && chosen.isEmpty()) {
            JLabel lblEmpty = new JLabel("No pending prescription requests yet.");
            lblEmpty.setFont(new Font("SansSerif", Font.PLAIN, 14));
            lblEmpty.setForeground(new Color(150, 150, 150));
            lblEmpty.setAlignmentX(0.5f);
            prescriptionRequestsPanel.add(Box.createVerticalGlue());
            prescriptionRequestsPanel.add(lblEmpty);
            prescriptionRequestsPanel.add(Box.createVerticalGlue());
        }

        prescriptionRequestsPanel.revalidate();
        prescriptionRequestsPanel.repaint();
    }

    private JPanel createPrescriptionRequestCard(PrescriptionRequest pr, String userAddress) {
        JPanel card = new JPanel(new BorderLayout(10, 8));
        card.setBackground(Color.WHITE);
        card.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(new Color(200, 200, 200), 1),
            new EmptyBorder(10, 12, 10, 12)));
        card.setMaximumSize(new Dimension(Integer.MAX_VALUE, 240));

        String ref = pr.getReferenceNumber() != null ? pr.getReferenceNumber() : ("RX" + String.format("%06d", pr.getId()));
        JPanel header = new JPanel(new BorderLayout());
        header.setOpaque(false);
        JLabel lblTitle = new JLabel("Prescription Request - Ref: " + ref);
        lblTitle.setFont(new Font("SansSerif", Font.BOLD, 13));
        header.add(lblTitle, BorderLayout.WEST);

        JButton btnCancel = LoginView.createDangerButton("Cancel");
        btnCancel.addActionListener(e -> {
            int confirm = JOptionPane.showConfirmDialog(this,
                "Cancel this prescription request?", "Confirm Cancel",
                JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
            if (confirm == JOptionPane.YES_OPTION) {
                String msg = controller.cancelPrescriptionRequest(pr.getId(), userId);
                JOptionPane.showMessageDialog(this, msg);
                refreshPrescriptionRequests();
            }
        });
        header.add(btnCancel, BorderLayout.EAST);
        card.add(header, BorderLayout.NORTH);

        JPanel body = new JPanel();
        body.setLayout(new BoxLayout(body, BoxLayout.Y_AXIS));
        body.setOpaque(false);

        JLabel lblTime = new JLabel("Submitted: " + pr.getSubmittedAt().toString());
        lblTime.setFont(new Font("SansSerif", Font.PLAIN, 11));
        lblTime.setForeground(new Color(100, 100, 100));
        body.add(lblTime);
        body.add(Box.createVerticalStrut(6));

        List<Pharmacy> confirmed = new java.util.ArrayList<>();
        if (pr.getConfirmedPharmacyIds() != null) {
            for (Integer pid : pr.getConfirmedPharmacyIds()) {
                Pharmacy p = controller.getPharmacyById(pid);
                if (p != null) confirmed.add(p);
            }
        }

        if (confirmed.isEmpty()) {
            JLabel lblNone = new JLabel("No pharmacies have confirmed stock yet.");
            lblNone.setFont(new Font("SansSerif", Font.ITALIC, 12));
            lblNone.setForeground(new Color(140, 140, 140));
            body.add(lblNone);
        } else {
            confirmed.sort(java.util.Comparator.comparing(Pharmacy::getName));
            int suggestedId = getSuggestedPharmacyId(userAddress, confirmed);
            for (Pharmacy p : confirmed) {
                JPanel row = new JPanel(new BorderLayout(6, 0));
                row.setOpaque(false);

                String name = p.getName();
                if (p.getId() == suggestedId) name += "  (Suggested)";
                JLabel lblPharm = new JLabel(name);
                lblPharm.setFont(new Font("SansSerif", Font.BOLD, 12));
                lblPharm.setForeground(p.getId() == suggestedId ? new Color(25, 118, 210) : new Color(60, 60, 60));

                String addrText = p.getAddress() != null ? p.getAddress() : "";
                JLabel lblAddr = new JLabel("<html><div style='width:360px;'>" + addrText + "</div></html>");
                lblAddr.setFont(new Font("SansSerif", Font.PLAIN, 11));
                lblAddr.setForeground(new Color(120, 120, 120));

                JPanel left = new JPanel();
                left.setLayout(new BoxLayout(left, BoxLayout.Y_AXIS));
                left.setOpaque(false);
                left.setAlignmentX(Component.LEFT_ALIGNMENT);
                left.add(lblPharm);
                left.add(lblAddr);

                JButton btnChoose = LoginView.createSuccessButton("Choose");
                btnChoose.addActionListener(e -> {
                    String msg = controller.selectPrescriptionPharmacy(pr.getId(), p.getId());
                    JOptionPane.showMessageDialog(this, msg);
                    refreshPrescriptionRequests();
                });

                row.add(left, BorderLayout.CENTER);
                row.add(btnChoose, BorderLayout.EAST);
                body.add(row);
                body.add(Box.createVerticalStrut(6));
                JSeparator sep = new JSeparator();
                sep.setForeground(new Color(220, 220, 220));
                body.add(sep);
                body.add(Box.createVerticalStrut(6));
            }
        }

        card.add(body, BorderLayout.CENTER);
        return card;
    }

    private JPanel createChosenPrescriptionRequestCard(PrescriptionRequest pr) {
        JPanel card = new JPanel(new BorderLayout(10, 8));
        card.setBackground(Color.WHITE);
        card.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(new Color(200, 200, 200), 1),
            new EmptyBorder(10, 12, 10, 12)));
        card.setMaximumSize(new Dimension(Integer.MAX_VALUE, 120));

        String ref = pr.getReferenceNumber() != null ? pr.getReferenceNumber() : ("RX" + String.format("%06d", pr.getId()));
        JLabel lblTitle = new JLabel("Prescription Pickup - Ref: " + ref);
        lblTitle.setFont(new Font("SansSerif", Font.BOLD, 13));
        card.add(lblTitle, BorderLayout.NORTH);

        JPanel body = new JPanel();
        body.setLayout(new BoxLayout(body, BoxLayout.Y_AXIS));
        body.setOpaque(false);

        Pharmacy chosen = controller.getPharmacyById(pr.getChosenPharmacyId());
        String chosenText = chosen != null ? chosen.getName() + " - " + chosen.getAddress() : "Chosen pharmacy";
        JLabel lblPharm = new JLabel("Pharmacy: " + chosenText);
        lblPharm.setFont(new Font("SansSerif", Font.PLAIN, 12));
        body.add(lblPharm);

        String status;
        if (pr.getStatus() == models.PrescriptionRequest.Status.READY_FOR_PICKUP) {
            status = "Ready for Pickup";
        } else if (pr.getStatus() == models.PrescriptionRequest.Status.COMPLETED) {
            status = "Paid";
        } else {
            status = "Waiting for pharmacy to prepare";
        }
        JLabel lblStatus = new JLabel("Status: " + status);
        lblStatus.setFont(new Font("SansSerif", Font.PLAIN, 11));
        if (pr.getStatus() == models.PrescriptionRequest.Status.READY_FOR_PICKUP) {
            lblStatus.setForeground(new Color(25, 118, 210));
        } else if (pr.getStatus() == models.PrescriptionRequest.Status.COMPLETED) {
            lblStatus.setForeground(new Color(0, 150, 0));
        } else {
            lblStatus.setForeground(new Color(120, 120, 120));
        }
        body.add(lblStatus);

        if (pr.getStatus() == models.PrescriptionRequest.Status.COMPLETED) {
            String medName = pr.getMedicineName() != null && !pr.getMedicineName().isEmpty()
                ? pr.getMedicineName() : "Prescription";
            JLabel lblMed = new JLabel("Medicine: " + medName);
            lblMed.setFont(new Font("SansSerif", Font.PLAIN, 11));
            lblMed.setForeground(new Color(100, 100, 100));
            body.add(lblMed);

            JLabel lblQty = new JLabel("Quantity: " + (pr.getMedicineQuantity() > 0 ? pr.getMedicineQuantity() : 1));
            lblQty.setFont(new Font("SansSerif", Font.PLAIN, 11));
            lblQty.setForeground(new Color(100, 100, 100));
            body.add(lblQty);

            JLabel lblAmt = new JLabel("Amount: PHP " + String.format("%.2f", pr.getMedicineAmount()));
            lblAmt.setFont(new Font("SansSerif", Font.PLAIN, 11));
            lblAmt.setForeground(new Color(100, 100, 100));
            body.add(lblAmt);
        }

        card.add(body, BorderLayout.CENTER);
        return card;
    }

    private int getSuggestedPharmacyId(String userAddress, List<Pharmacy> pharmacies) {
        if (userAddress == null) return -1;
        String addr = userAddress.trim().toLowerCase();
        if (addr.isEmpty()) return -1;
        String[] parts = addr.split(",");
        String token = parts.length > 0 ? parts[parts.length - 1].trim() : addr;
        if (token.isEmpty()) return -1;
        for (Pharmacy p : pharmacies) {
            if (p.getAddress() != null && p.getAddress().toLowerCase().contains(token)) {
                return p.getId();
            }
        }
        return -1;
    }

    private void sendPrescription(JButton btnSend) {
        if (prescriptionSending) return;
        if (selectedPrescriptionFile == null || !selectedPrescriptionFile.exists()) {
            JOptionPane.showMessageDialog(this, "Please select a prescription image first.",
                "No Photo", JOptionPane.WARNING_MESSAGE);
            return;
        }
        prescriptionSending = true;
        btnSend.setEnabled(false);
        btnSend.setText("Sending...");
        SwingUtilities.invokeLater(() -> {
            try {
                String result = controller.submitPrescription(userId, selectedPrescriptionFile);
                if (result.startsWith("SUCCESS|")) {
                    JOptionPane.showMessageDialog(this, result.substring(8),
                        "Prescription Sent", JOptionPane.INFORMATION_MESSAGE);
                    selectedPrescriptionFile = null;
                    if (prescriptionFileLabel != null) {
                        prescriptionFileLabel.setText("<html><center>Drag and drop your prescription here<br>or click to browse</center></html>");
                        prescriptionFileLabel.setForeground(new Color(120, 120, 120));
                    }
                    refreshPrescriptionRequests();
                } else {
                    JOptionPane.showMessageDialog(this, result.replace("ERROR|", ""),
                        "Error", JOptionPane.ERROR_MESSAGE);
                }
            } finally {
                prescriptionSending = false;
                btnSend.setEnabled(true);
                btnSend.setText("Send to Pharmacies");
            }
        });
    }

    // ==================== RESERVATIONS PANEL ====================

    private JPanel createReservationsPanel() {
        JPanel panel = new JPanel(new BorderLayout(10, 10));
        panel.setBorder(new EmptyBorder(15, 15, 15, 15));

        // Refresh button and note at top
        JPanel topPanel = new JPanel(new BorderLayout());
        JPanel btnPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        JButton btnRefresh = LoginView.createSecondaryButton("Refresh");
        btnRefresh.addActionListener(e -> {
            refreshReservations();
            refreshPrescriptionRequests();
        });
        btnPanel.add(btnRefresh);
        topPanel.add(btnPanel, BorderLayout.WEST);

        JLabel lblNote = new JLabel("Reservations expire after 24 hours. Pick up before expiration. Cancel to move to Transaction History.");
        lblNote.setFont(new Font("SansSerif", Font.ITALIC, 11));
        lblNote.setForeground(Color.GRAY);
        lblNote.setBorder(new EmptyBorder(0, 15, 0, 10));
        topPanel.add(lblNote, BorderLayout.CENTER);

        panel.add(topPanel, BorderLayout.NORTH);

        JPanel mainContent = new JPanel();
        mainContent.setLayout(new BoxLayout(mainContent, BoxLayout.Y_AXIS));
        mainContent.setBackground(new Color(245, 245, 245));

        JPanel requestsSection = new JPanel(new BorderLayout(5, 5));
        requestsSection.setBackground(new Color(245, 245, 245));
        requestsSection.setBorder(BorderFactory.createTitledBorder(
            BorderFactory.createLineBorder(new Color(200, 200, 200)),
            "Your Prescription Requests", TitledBorder.LEFT, TitledBorder.TOP,
            new Font("SansSerif", Font.BOLD, 13)));

        JPanel reqHeader = new JPanel(new BorderLayout());
        reqHeader.setBackground(new Color(245, 245, 245));
        JLabel reqNote = new JLabel("Choose a pharmacy after they confirm stock.");
        reqNote.setFont(new Font("SansSerif", Font.ITALIC, 11));
        reqNote.setForeground(Color.GRAY);
        reqHeader.add(reqNote, BorderLayout.WEST);
        JButton btnRefreshReq = LoginView.createSecondaryButton("Refresh");
        btnRefreshReq.addActionListener(e -> refreshPrescriptionRequests());
        JPanel reqBtnPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 0, 0));
        reqBtnPanel.setBackground(new Color(245, 245, 245));
        reqBtnPanel.add(btnRefreshReq);
        reqHeader.add(reqBtnPanel, BorderLayout.EAST);
        requestsSection.add(reqHeader, BorderLayout.NORTH);

        prescriptionRequestsPanel = new JPanel();
        prescriptionRequestsPanel.setLayout(new BoxLayout(prescriptionRequestsPanel, BoxLayout.Y_AXIS));
        prescriptionRequestsPanel.setBackground(new Color(245, 245, 245));
        JScrollPane reqScroll = new JScrollPane(prescriptionRequestsPanel);
        reqScroll.setBorder(BorderFactory.createLineBorder(new Color(200, 200, 200)));
        reqScroll.getVerticalScrollBar().setUnitIncrement(20);
        requestsSection.add(reqScroll, BorderLayout.CENTER);
        requestsSection.setAlignmentX(Component.LEFT_ALIGNMENT);
        mainContent.add(requestsSection);
        mainContent.add(Box.createVerticalStrut(10));

        reservationsCardsPanel = new JPanel();
        reservationsCardsPanel.setLayout(new BoxLayout(reservationsCardsPanel, BoxLayout.Y_AXIS));
        reservationsCardsPanel.setBackground(new Color(245, 245, 245));

        JScrollPane scroll = new JScrollPane(reservationsCardsPanel);
        scroll.setBorder(BorderFactory.createLineBorder(new Color(200, 200, 200)));
        scroll.getVerticalScrollBar().setUnitIncrement(20);
        scroll.setAlignmentX(Component.LEFT_ALIGNMENT);
        mainContent.add(scroll);

        panel.add(mainContent, BorderLayout.CENTER);

        return panel;
    }

    // ==================== ACTIONS ====================

    private void refreshProductsDisplay() {
        if (productsCardsPanel == null) return;

        String term = txtSearch.getText().trim();
        if (term.isEmpty() || "Search products...".equals(term)) {
            term = ""; // Empty = show all products
        }

        productsCardsPanel.removeAll();

        // Search: empty term returns all products from approved pharmacies
        Object[][] data = controller.searchMedicines(-1, term);

        String category = filterCombo != null ? (String) filterCombo.getSelectedItem() : "All Categories";

        for (Object[] row : data) {
            if (!"All Categories".equals(category) &&
                !category.equals(row[7] != null ? row[7].toString() : "")) {
                continue;
            }
            JPanel card = createProductCard(
                (int) row[0], row[1].toString(), row[2].toString(), row[3].toString(),
                row[4].toString(), row[5].toString(), (int) row[6], row[7].toString());
            productsCardsPanel.add(card);
            productsCardsPanel.add(Box.createVerticalStrut(10));
        }

        if (productsCardsPanel.getComponentCount() == 0) {
            JLabel lblEmpty = new JLabel(term.isEmpty()
                ? "No products available."
                : "No results for \"" + term + "\"" + ("All Categories".equals(category) ? "" : " in category \"" + category + "\""));
            lblEmpty.setFont(new Font("SansSerif", Font.PLAIN, 14));
            lblEmpty.setForeground(new Color(150, 150, 150));
            lblEmpty.setAlignmentX(0.5f);
            productsCardsPanel.add(Box.createVerticalGlue());
            productsCardsPanel.add(lblEmpty);
            productsCardsPanel.add(Box.createVerticalGlue());
        }

        productsCardsPanel.revalidate();
        productsCardsPanel.repaint();
    }

    private JPanel createProductCard(int medicineId, String pharmacy, String brandName, String genericName,
                                     String dosage, String price, int available, String categoryStr) {
        JPanel card = new JPanel(new BorderLayout(15, 10));
        card.setBackground(Color.WHITE);
        card.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(new Color(220, 220, 220), 1),
            new EmptyBorder(12, 15, 12, 15)));
        card.setMaximumSize(new Dimension(Integer.MAX_VALUE, 110));

        JPanel leftPanel = new JPanel(new BorderLayout(5, 3));
        leftPanel.setOpaque(false);

        JLabel lblPharmacy = new JLabel(pharmacy);
        lblPharmacy.setFont(new Font("SansSerif", Font.BOLD, 13));
        lblPharmacy.setForeground(new Color(25, 118, 210));
        leftPanel.add(lblPharmacy, BorderLayout.NORTH);

        JLabel lblMedicine = new JLabel(brandName + " (" + genericName + ")");
        lblMedicine.setFont(new Font("SansSerif", Font.PLAIN, 13));
        leftPanel.add(lblMedicine, BorderLayout.CENTER);

        JLabel lblDetails = new JLabel(dosage + "  |  PHP " + price + "  |  Available: " + available + "  |  " + categoryStr);
        lblDetails.setFont(new Font("SansSerif", Font.PLAIN, 12));
        lblDetails.setForeground(new Color(100, 100, 100));
        leftPanel.add(lblDetails, BorderLayout.SOUTH);

        card.add(leftPanel, BorderLayout.CENTER);

        JButton btnReserve = LoginView.createSuccessButton("Reserve");
        btnReserve.addActionListener(e -> reserveMedicine(medicineId, pharmacy, brandName, price, available));
        JPanel btnPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        btnPanel.setOpaque(false);
        btnPanel.add(btnReserve);
        card.add(btnPanel, BorderLayout.EAST);

        return card;
    }

    private void performSearch() {
        refreshProductsDisplay();
    }

    private void reserveMedicine(int medicineId, String pharmacy, String medName, String price, int available) {
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
        JTextField txtQty = new JTextField("1", 8);
        txtQty.setFont(new Font("SansSerif", Font.PLAIN, 13));
        dialogPanel.add(txtQty, gbc);

        // Payment method
        gbc.gridy = 5; gbc.gridx = 0; gbc.gridwidth = 2;
        dialogPanel.add(new JSeparator(), gbc);
        gbc.gridwidth = 1;

        gbc.gridy = 6; gbc.gridx = 0; gbc.gridwidth = 2;
        JLabel lblPay = new JLabel("Payment Method:");
        lblPay.setFont(new Font("SansSerif", Font.BOLD, 12));
        dialogPanel.add(lblPay, gbc);

        gbc.gridy = 7;
        JPanel payPanel = new JPanel(new GridLayout(4, 1, 4, 4));
        ButtonGroup payGroup = new ButtonGroup();
        JRadioButton rbOnlineBank = new JRadioButton("Online Bank (Credit/Debit Card)");
        JRadioButton rbGCash = new JRadioButton("GCash (E-Wallet)");
        JRadioButton rbMaya = new JRadioButton("Maya (E-Wallet)");
        JRadioButton rbStore = new JRadioButton("Pay at Store (Reserved - Pending approval)");
        rbStore.setSelected(true);
        payGroup.add(rbOnlineBank); payGroup.add(rbGCash); payGroup.add(rbMaya); payGroup.add(rbStore);
        payPanel.add(rbOnlineBank); payPanel.add(rbGCash); payPanel.add(rbMaya); payPanel.add(rbStore);
        dialogPanel.add(payPanel, gbc);

        int option = JOptionPane.showConfirmDialog(this, dialogPanel,
            "Reserve Medicine", JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);

        if (option == JOptionPane.OK_OPTION) {
            String qtyText = txtQty.getText().trim();
            if (qtyText.isEmpty()) {
                JOptionPane.showMessageDialog(this, "Please enter quantity.", "Validation", JOptionPane.WARNING_MESSAGE);
                return;
            }
            int qty;
            try {
                qty = Integer.parseInt(qtyText);
            } catch (NumberFormatException ex) {
                JOptionPane.showMessageDialog(this, "Invalid quantity.", "Validation", JOptionPane.WARNING_MESSAGE);
                return;
            }
            if (qty <= 0 || qty > available) {
                JOptionPane.showMessageDialog(this, "Quantity must be between 1 and " + available + ".",
                    "Validation", JOptionPane.WARNING_MESSAGE);
                return;
            }

            String paymentMethod;
            if (rbOnlineBank.isSelected()) paymentMethod = "ONLINE_BANK";
            else if (rbGCash.isSelected()) paymentMethod = "E_PAYMENT";
            else if (rbMaya.isSelected()) paymentMethod = "E_PAYMENT";
            else paymentMethod = "PAY_AT_STORE";

            // Online payment confirmation step
            if (!"PAY_AT_STORE".equals(paymentMethod)) {
                double total = Double.parseDouble(price) * qty;
                boolean paySuccess = false;
                if (rbOnlineBank.isSelected()) {
                    paySuccess = showOnlineBankPaymentDialog(total);
                } else if (rbGCash.isSelected()) {
                    paySuccess = showEWalletPaymentDialog("GCash", total);
                } else if (rbMaya.isSelected()) {
                    paySuccess = showEWalletPaymentDialog("Maya", total);
                }
                if (!paySuccess) return;
            }

            String result = controller.reserveMedicine(userId, medicineId, qty, paymentMethod);
            if (result.startsWith("SUCCESS|")) {
                JOptionPane.showMessageDialog(this,
                    result.substring(8) + "\n\nPick up at: " + pharmacy,
                    "Reservation Successful", JOptionPane.INFORMATION_MESSAGE);
                refreshProductsDisplay(); // refresh results
                refreshReservations();
            } else if (result.startsWith("ERROR|")) {
                JOptionPane.showMessageDialog(this, result.substring(6),
                    "Reservation Failed", JOptionPane.ERROR_MESSAGE);
            } else {
                JOptionPane.showMessageDialog(this, result);
            }
        }
    }

    // ==================== REFRESH ====================

    private void refreshReservations() {
        if (reservationsCardsPanel == null) return;

        reservationsCardsPanel.removeAll();
        Object[][] data = controller.getUserReservations(userId);

        for (Object[] row : data) {
            // row: ID, RefNum, Medicine, Pharmacy, Qty, Total, Payment, PayStatus, ReservedAt, ExpiresAt, Status
            String status = row[10].toString();
            // Show only active reservations (PENDING, CONFIRMED) - past ones go to Transaction History
            if (!"PENDING".equals(status) && !"CONFIRMED".equals(status)) continue;

            int resId = (int) row[0];
            JPanel card = createReservationCard(
                resId, row[1].toString(), row[2].toString(), row[3].toString(),
                (int) row[4], row[5].toString(), row[6].toString(), row[7].toString(),
                row[8].toString(), row[9].toString(), row[10].toString());
            reservationsCardsPanel.add(card);
            reservationsCardsPanel.add(Box.createVerticalStrut(10));
        }

        if (reservationsCardsPanel.getComponentCount() == 0) {
            JLabel lblEmpty = new JLabel("No active reservations. Cancelled or completed orders appear in Transaction History.");
            lblEmpty.setFont(new Font("SansSerif", Font.PLAIN, 14));
            lblEmpty.setForeground(new Color(150, 150, 150));
            lblEmpty.setAlignmentX(0.5f);
            reservationsCardsPanel.add(Box.createVerticalGlue());
            reservationsCardsPanel.add(lblEmpty);
            reservationsCardsPanel.add(Box.createVerticalGlue());
        }

        reservationsCardsPanel.revalidate();
        reservationsCardsPanel.repaint();
    }

    private JPanel createReservationCard(int resId, String refNum, String medicine, String pharmacy,
                                         int qty, String total, String payment, String payStatus,
                                         String reservedAt, String expiresAt, String status) {
        JPanel card = new JPanel(new BorderLayout(15, 10));
        card.setBackground(Color.WHITE);
        card.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(new Color(220, 220, 220), 1),
            new EmptyBorder(12, 15, 12, 15)));
        card.setMaximumSize(new Dimension(Integer.MAX_VALUE, 120));

        JPanel leftPanel = new JPanel();
        leftPanel.setLayout(new BoxLayout(leftPanel, BoxLayout.Y_AXIS));
        leftPanel.setOpaque(false);

        JLabel lblRef = new JLabel(refNum);
        lblRef.setFont(new Font("SansSerif", Font.BOLD, 13));
        lblRef.setForeground(new Color(25, 118, 210));
        lblRef.setAlignmentX(0);
        leftPanel.add(lblRef);
        leftPanel.add(Box.createVerticalStrut(4));

        JLabel lblMain = new JLabel(medicine + "  @  " + pharmacy);
        lblMain.setFont(new Font("SansSerif", Font.PLAIN, 13));
        lblMain.setAlignmentX(0);
        leftPanel.add(lblMain);
        leftPanel.add(Box.createVerticalStrut(2));

        JLabel lblDetails = new JLabel("Qty: " + qty + "  |  PHP " + total + "  |  " + payment + "  |  " + payStatus);
        lblDetails.setFont(new Font("SansSerif", Font.PLAIN, 12));
        lblDetails.setForeground(new Color(100, 100, 100));
        lblDetails.setAlignmentX(0);
        leftPanel.add(lblDetails);
        leftPanel.add(Box.createVerticalStrut(2));

        String timesText = "Reserved: " + reservedAt;
        if (!"CANCELLED".equals(status)) {
            timesText += "  |  Expires: " + expiresAt;
        }
        timesText += "  |  Status: " + status;
        JLabel lblTimes = new JLabel(timesText);
        lblTimes.setFont(new Font("SansSerif", Font.PLAIN, 11));
        lblTimes.setForeground(new Color(120, 120, 120));
        lblTimes.setAlignmentX(0);
        leftPanel.add(lblTimes);

        card.add(leftPanel, BorderLayout.CENTER);

        JPanel rightPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        rightPanel.setOpaque(false);
        // Cancel button for PENDING and CONFIRMED (cancelled orders move to Transaction History)
        if ("PENDING".equals(status) || "CONFIRMED".equals(status)) {
            JButton btnCancel = LoginView.createDangerButton("Cancel");
            btnCancel.addActionListener(e -> cancelReservationById(resId));
            rightPanel.add(btnCancel);
        }
        card.add(rightPanel, BorderLayout.EAST);

        return card;
    }

    private void cancelReservationById(int resId) {
        int confirm = JOptionPane.showConfirmDialog(this,
            "Are you sure you want to cancel this reservation?",
            "Confirm Cancel", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
        if (confirm == JOptionPane.YES_OPTION) {
            String msg = controller.cancelReservation(resId, userId);
            JOptionPane.showMessageDialog(this, msg + "\n\nThis order has been moved to Transaction History.");
            refreshReservations();
            refreshTransactionHistory();
        }
    }

    public void refresh() {
        if (contentCardLayout != null && contentPanel != null) {
            showNavContent("Search Products");
        }
        refreshReservations();
        refreshTransactionHistory();
        refreshPrescriptionRequests();
        if (txtSearch != null) {
            txtSearch.setText("Search products...");
            txtSearch.setForeground(new Color(150, 150, 150));
        }
        refreshProductsDisplay();
    }

    private boolean showOnlineBankPaymentDialog(double total) {
        JPanel payDialog = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(6, 8, 6, 8);
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.anchor = GridBagConstraints.WEST;

        // Title
        gbc.gridx = 0; gbc.gridy = 0; gbc.gridwidth = 2;
        JLabel lblTitle = new JLabel("Online Bank Payment");
        lblTitle.setFont(new Font("SansSerif", Font.BOLD, 14));
        payDialog.add(lblTitle, gbc);

        gbc.gridy = 1;
        payDialog.add(new JSeparator(), gbc);

        // Amount
        gbc.gridy = 2; gbc.gridwidth = 1;
        payDialog.add(new JLabel("Total Amount:"), gbc);
        gbc.gridx = 1;
        JLabel lblAmount = new JLabel("PHP " + String.format("%.2f", total));
        lblAmount.setFont(new Font("SansSerif", Font.BOLD, 13));
        lblAmount.setForeground(new Color(25, 118, 210));
        payDialog.add(lblAmount, gbc);

        // Card Number (4 partitions of 4 digits)
        gbc.gridy = 3; gbc.gridx = 0;
        payDialog.add(new JLabel("Card Number:"), gbc);
        gbc.gridx = 1;
        
        JPanel cardPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 5, 0));
        JTextField txtCard1 = new JTextField(4);
        JTextField txtCard2 = new JTextField(4);
        JTextField txtCard3 = new JTextField(4);
        JTextField txtCard4 = new JTextField(4);
        
        // Auto-focus next field and limit to digits only
        txtCard1.addKeyListener(new java.awt.event.KeyAdapter() {
            public void keyTyped(java.awt.event.KeyEvent e) {
                if (!Character.isDigit(e.getKeyChar())) e.consume();
                if (txtCard1.getText().length() >= 4) e.consume();
            }
            public void keyReleased(java.awt.event.KeyEvent e) {
                if (txtCard1.getText().length() == 4) txtCard2.requestFocus();
            }
        });
        txtCard2.addKeyListener(new java.awt.event.KeyAdapter() {
            public void keyTyped(java.awt.event.KeyEvent e) {
                if (!Character.isDigit(e.getKeyChar())) e.consume();
                if (txtCard2.getText().length() >= 4) e.consume();
            }
            public void keyReleased(java.awt.event.KeyEvent e) {
                if (txtCard2.getText().length() == 4) txtCard3.requestFocus();
            }
        });
        txtCard3.addKeyListener(new java.awt.event.KeyAdapter() {
            public void keyTyped(java.awt.event.KeyEvent e) {
                if (!Character.isDigit(e.getKeyChar())) e.consume();
                if (txtCard3.getText().length() >= 4) e.consume();
            }
            public void keyReleased(java.awt.event.KeyEvent e) {
                if (txtCard3.getText().length() == 4) txtCard4.requestFocus();
            }
        });
        txtCard4.addKeyListener(new java.awt.event.KeyAdapter() {
            public void keyTyped(java.awt.event.KeyEvent e) {
                if (!Character.isDigit(e.getKeyChar())) e.consume();
                if (txtCard4.getText().length() >= 4) e.consume();
            }
        });
        
        cardPanel.add(txtCard1);
        cardPanel.add(new JLabel("-"));
        cardPanel.add(txtCard2);
        cardPanel.add(new JLabel("-"));
        cardPanel.add(txtCard3);
        cardPanel.add(new JLabel("-"));
        cardPanel.add(txtCard4);
        payDialog.add(cardPanel, gbc);

        // CVV
        gbc.gridy = 4; gbc.gridx = 0;
        payDialog.add(new JLabel("CVV:"), gbc);
        gbc.gridx = 1;
        JPasswordField txtCVV = new JPasswordField(3);
        txtCVV.addKeyListener(new java.awt.event.KeyAdapter() {
            public void keyTyped(java.awt.event.KeyEvent e) {
                if (!Character.isDigit(e.getKeyChar())) e.consume();
                if (txtCVV.getPassword().length >= 3) e.consume();
            }
        });
        payDialog.add(txtCVV, gbc);

        int result = JOptionPane.showConfirmDialog(this, payDialog,
            "Payment Details", JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);

        if (result == JOptionPane.OK_OPTION) {
            String card1 = txtCard1.getText().trim();
            String card2 = txtCard2.getText().trim();
            String card3 = txtCard3.getText().trim();
            String card4 = txtCard4.getText().trim();
            String cvv = new String(txtCVV.getPassword()).trim();
            
            if (card1.isEmpty() || card2.isEmpty() || card3.isEmpty() || card4.isEmpty() || cvv.isEmpty()) {
                JOptionPane.showMessageDialog(this, "Please fill all payment details.",
                    "Validation", JOptionPane.WARNING_MESSAGE);
                return false;
            }
            if (card1.length() != 4 || card2.length() != 4 || card3.length() != 4 || card4.length() != 4) {
                JOptionPane.showMessageDialog(this, "Card number must be 16 digits (4-4-4-4 format).",
                    "Validation", JOptionPane.WARNING_MESSAGE);
                return false;
            }
            if (cvv.length() != 3) {
                JOptionPane.showMessageDialog(this, "CVV must be exactly 3 digits.",
                    "Validation", JOptionPane.WARNING_MESSAGE);
                return false;
            }
            JOptionPane.showMessageDialog(this, "Payment Successful!",
                "Success", JOptionPane.INFORMATION_MESSAGE);
            return true;
        }
        return false;
    }

    private boolean showEWalletPaymentDialog(String walletType, double total) {
        JPanel payDialog = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(6, 8, 6, 8);
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.anchor = GridBagConstraints.WEST;

        // Title
        gbc.gridx = 0; gbc.gridy = 0; gbc.gridwidth = 2;
        JLabel lblTitle = new JLabel(walletType + " Payment");
        lblTitle.setFont(new Font("SansSerif", Font.BOLD, 14));
        payDialog.add(lblTitle, gbc);

        gbc.gridy = 1;
        payDialog.add(new JSeparator(), gbc);

        // Amount
        gbc.gridy = 2; gbc.gridwidth = 1;
        payDialog.add(new JLabel("Total Amount:"), gbc);
        gbc.gridx = 1;
        JLabel lblAmount = new JLabel("PHP " + String.format("%.2f", total));
        lblAmount.setFont(new Font("SansSerif", Font.BOLD, 13));
        lblAmount.setForeground(new Color(25, 118, 210));
        payDialog.add(lblAmount, gbc);

        // Phone Number
        gbc.gridy = 3; gbc.gridx = 0;
        payDialog.add(new JLabel("Mobile Number:"), gbc);
        gbc.gridx = 1;
        JTextField txtPhone = new JTextField("09", 11);
        txtPhone.addKeyListener(new java.awt.event.KeyAdapter() {
            public void keyTyped(java.awt.event.KeyEvent e) {
                if (!Character.isDigit(e.getKeyChar()) && e.getKeyChar() != java.awt.event.KeyEvent.VK_BACK_SPACE) {
                    e.consume();
                }
                if (txtPhone.getText().length() >= 11 && e.getKeyChar() != java.awt.event.KeyEvent.VK_BACK_SPACE) {
                    e.consume();
                }
            }
            public void keyReleased(java.awt.event.KeyEvent e) {
                String text = txtPhone.getText();
                if (!text.startsWith("09")) {
                    txtPhone.setText("09");
                }
            }
        });
        payDialog.add(txtPhone, gbc);

        int result = JOptionPane.showConfirmDialog(this, payDialog,
            "Payment Details", JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);

        if (result == JOptionPane.OK_OPTION) {
            String phone = txtPhone.getText().trim();
            if (phone.isEmpty()) {
                JOptionPane.showMessageDialog(this, "Please enter your mobile number.",
                    "Validation", JOptionPane.WARNING_MESSAGE);
                return false;
            }
            if (phone.length() != 11) {
                JOptionPane.showMessageDialog(this, "Mobile number must be exactly 11 digits.",
                    "Validation", JOptionPane.WARNING_MESSAGE);
                return false;
            }
            if (!phone.startsWith("09")) {
                JOptionPane.showMessageDialog(this, "Mobile number must start with 09.",
                    "Validation", JOptionPane.WARNING_MESSAGE);
                return false;
            }
            JOptionPane.showMessageDialog(this, "Payment Successful!",
                "Success", JOptionPane.INFORMATION_MESSAGE);
            return true;
        }
        return false;
    }
}
