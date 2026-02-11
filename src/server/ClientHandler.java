package server;

import models.*;
import models.User.UserType;
import models.Reservation.PaymentMethod;

import javax.xml.parsers.*;
import javax.xml.transform.*;
import javax.xml.transform.dom.*;
import javax.xml.transform.stream.*;
import org.w3c.dom.*;
import java.io.*;
import java.net.Socket;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * Handles a single client connection. Reads XML requests,
 * dispatches to DataStore, and sends XML responses.
 */
public class ClientHandler implements Runnable {
    private final Socket socket;
    private final DataStore dataStore;
    private final PharmacyServer server;
    private DataInputStream in;
    private DataOutputStream out;
    private String currentUsername = "unknown";
    private final DateTimeFormatter dtf = DateTimeFormatter.ISO_LOCAL_DATE_TIME;

    public ClientHandler(Socket socket, PharmacyServer server) {
        this.socket = socket;
        this.server = server;
        this.dataStore = DataStore.getInstance();
    }

    @Override
    public void run() {
        try {
            in = new DataInputStream(socket.getInputStream());
            out = new DataOutputStream(socket.getOutputStream());
            server.log("SYSTEM", "CLIENT_CONNECTED", "Client connected from " + socket.getInetAddress());

            while (!socket.isClosed()) {
                String requestXml = in.readUTF();
                String responseXml = processRequest(requestXml);
                out.writeUTF(responseXml);
                out.flush();
            }
        } catch (EOFException e) {
            server.log(currentUsername, "DISCONNECTED", "Client disconnected");
        } catch (IOException e) {
            if (!socket.isClosed()) {
                server.log(currentUsername, "ERROR", "Connection error: " + e.getMessage());
            }
        } finally {
            try { socket.close(); } catch (IOException ignored) {}
        }
    }

    private String processRequest(String requestXml) {
        try {
            DocumentBuilder builder = DocumentBuilderFactory.newInstance().newDocumentBuilder();
            Document doc = builder.parse(new ByteArrayInputStream(requestXml.getBytes("UTF-8")));
            Element root = doc.getDocumentElement();
            String action = getText(root, "action");

            switch (action) {
                case "LOGIN": return handleLogin(root);
                case "REGISTER": return handleRegister(root);
                case "GET_ALL_USERS": return handleGetAllUsers();
                case "CREATE_USER": return handleCreateUser(root);
                case "UPDATE_USER": return handleUpdateUser(root);
                case "DELETE_USER": return handleDeleteUser(root);
                case "GET_ALL_PHARMACIES": return handleGetAllPharmacies();
                case "APPROVE_PHARMACY": return handleApprovePharmacy(root);
                case "REJECT_PHARMACY": return handleRejectPharmacy(root);
                case "GET_APPROVED_PHARMACIES": return handleGetApprovedPharmacies();
                case "GET_PHARMACY_MEDICINES": return handleGetPharmacyMedicines(root);
                case "CREATE_MEDICINE": return handleCreateMedicine(root);
                case "UPDATE_MEDICINE": return handleUpdateMedicine(root);
                case "DELETE_MEDICINE": return handleDeleteMedicine(root);
                case "CHECK_MEDICINE_EXISTS": return handleCheckMedicineExists(root);
                case "SEARCH_MEDICINES": return handleSearchMedicines(root);
                case "GET_ALL_MEDICINES": return handleGetAllMedicines();
                case "RESERVE_MEDICINE": return handleReserveMedicine(root);
                case "CANCEL_RESERVATION": return handleCancelReservation(root);
                case "APPROVE_RESERVATION": return handleApproveReservation(root);
                case "REJECT_RESERVATION": return handleRejectReservation(root);
                case "COMPLETE_RESERVATION": return handleCompleteReservation(root);
                case "GET_PHARMACY_RESERVATIONS": return handleGetPharmacyReservations(root);
                case "GET_USER_RESERVATIONS": return handleGetUserReservations(root);
                case "GET_ALL_RESERVATIONS": return handleGetAllReservations();
                default: return errorResponse("Unknown action: " + action);
            }
        } catch (Exception e) {
            return errorResponse("Server error: " + e.getMessage());
        }
    }

    // ==================== AUTH ====================

    private String handleLogin(Element req) {
        String username = getText(req, "username");
        String password = getText(req, "password");
        User user = dataStore.authenticateUser(username, password);
        if (user == null) {
            server.log(username, "LOGIN_FAILED", "Invalid credentials for '" + username + "'");
            return errorResponse("Invalid username or password");
        }
        currentUsername = username;
        server.log(username, "LOGIN", "User '" + username + "' logged in as " + user.getUserType());
        return successResponse("Login successful", userToXml(user));
    }

    private String handleRegister(Element req) {
        String username = getText(req, "username");
        String password = getText(req, "password");
        String fullName = getText(req, "fullName");
        String email = getText(req, "email");
        User user = dataStore.createUser(username, password, fullName, email, UserType.RESIDENT, -1);
        if (user == null) {
            server.log(username, "REGISTER_FAILED", "Username '" + username + "' already exists");
            return errorResponse("Username already exists");
        }
        server.log(username, "REGISTER", "New resident account created: " + fullName);
        return successResponse("Registration successful", userToXml(user));
    }

    // ==================== USER MANAGEMENT ====================

    private String handleGetAllUsers() {
        List<User> users = dataStore.getAllUsers();
        StringBuilder data = new StringBuilder("<users>");
        for (User u : users) data.append(userToXml(u));
        data.append("</users>");
        server.log(currentUsername, "GET_ALL_USERS", "Retrieved " + users.size() + " users");
        return successResponse("OK", data.toString());
    }

    private String handleCreateUser(Element req) {
        String username = getText(req, "username");
        String password = getText(req, "password");
        String fullName = getText(req, "fullName");
        String email = getText(req, "email");
        String userType = getText(req, "userType");
        int pharmacyId = parseInt(getText(req, "pharmacyId"), -1);
        User user = dataStore.createUser(username, password, fullName, email,
            UserType.valueOf(userType), pharmacyId);
        if (user == null) {
            return errorResponse("Failed to create user. Username may already exist.");
        }
        server.log(currentUsername, "CREATE_USER", "Created user: " + fullName + " (" + userType + ")");
        return successResponse("User created", userToXml(user));
    }

    private String handleUpdateUser(Element req) {
        int userId = parseInt(getText(req, "userId"), -1);
        String fullName = getText(req, "fullName");
        String email = getText(req, "email");
        String password = getText(req, "password");
        boolean ok = dataStore.updateUser(userId, fullName, email, password);
        if (!ok) return errorResponse("Failed to update user");
        server.log(currentUsername, "UPDATE_USER", "Updated user ID: " + userId);
        return successResponse("User updated", "");
    }

    private String handleDeleteUser(Element req) {
        int userId = parseInt(getText(req, "userId"), -1);
        boolean ok = dataStore.deleteUser(userId);
        if (!ok) return errorResponse("Failed to delete user");
        server.log(currentUsername, "DELETE_USER", "Deactivated user ID: " + userId);
        return successResponse("User deleted", "");
    }

    // ==================== PHARMACY MANAGEMENT ====================

    private String handleGetAllPharmacies() {
        List<Pharmacy> list = dataStore.getAllPharmacies();
        StringBuilder data = new StringBuilder("<pharmacies>");
        for (Pharmacy p : list) data.append(pharmacyToXml(p));
        data.append("</pharmacies>");
        return successResponse("OK", data.toString());
    }

    private String handleGetApprovedPharmacies() {
        List<Pharmacy> list = dataStore.getApprovedPharmacies();
        StringBuilder data = new StringBuilder("<pharmacies>");
        for (Pharmacy p : list) data.append(pharmacyToXml(p));
        data.append("</pharmacies>");
        return successResponse("OK", data.toString());
    }

    private String handleApprovePharmacy(Element req) {
        int id = parseInt(getText(req, "pharmacyId"), -1);
        boolean ok = dataStore.approvePharmacy(id);
        if (!ok) return errorResponse("Failed to approve pharmacy");
        server.log(currentUsername, "APPROVE_PHARMACY", "Approved pharmacy ID: " + id);
        return successResponse("Pharmacy approved", "");
    }

    private String handleRejectPharmacy(Element req) {
        int id = parseInt(getText(req, "pharmacyId"), -1);
        boolean ok = dataStore.rejectPharmacy(id);
        if (!ok) return errorResponse("Failed to reject pharmacy");
        server.log(currentUsername, "REJECT_PHARMACY", "Rejected pharmacy ID: " + id);
        return successResponse("Pharmacy rejected", "");
    }

    // ==================== MEDICINE MANAGEMENT ====================

    private String handleGetPharmacyMedicines(Element req) {
        int pharmacyId = parseInt(getText(req, "pharmacyId"), -1);
        List<Medicine> list = dataStore.getMedicinesByPharmacy(pharmacyId);
        StringBuilder data = new StringBuilder("<medicines>");
        for (Medicine m : list) data.append(medicineToXml(m));
        data.append("</medicines>");
        return successResponse("OK", data.toString());
    }

    private String handleCreateMedicine(Element req) {
        int pharmacyId = parseInt(getText(req, "pharmacyId"), -1);
        String brandName = getText(req, "brandName");
        String genericName = getText(req, "genericName");
        String dosage = getText(req, "dosage");
        String dosageForm = getText(req, "dosageForm");
        double price = parseDouble(getText(req, "price"), 0);
        int quantity = parseInt(getText(req, "quantity"), 0);
        String category = getText(req, "category");
        Medicine med = dataStore.createMedicine(pharmacyId, brandName, genericName, "",
            dosage, dosageForm, price, quantity, category);
        if (med == null) return errorResponse("Failed to add medicine. It may already exist in this pharmacy.");
        Pharmacy p = dataStore.getPharmacyById(pharmacyId);
        String pName = p != null ? p.getName() : "ID:" + pharmacyId;
        server.log(currentUsername, "CREATE_MEDICINE", "Added '" + brandName + " " + dosage + "' to " + pName);
        return successResponse("Medicine added", medicineToXml(med));
    }

    private String handleUpdateMedicine(Element req) {
        int medicineId = parseInt(getText(req, "medicineId"), -1);
        String brandName = getText(req, "brandName");
        String genericName = getText(req, "genericName");
        String dosage = getText(req, "dosage");
        String dosageForm = getText(req, "dosageForm");
        double price = parseDouble(getText(req, "price"), 0);
        int quantity = parseInt(getText(req, "quantity"), 0);
        String category = getText(req, "category");
        boolean ok = dataStore.updateMedicine(medicineId, brandName, genericName, dosage,
            dosageForm, price, quantity, category);
        if (!ok) return errorResponse("Failed to update medicine. Duplicate may exist.");
        server.log(currentUsername, "UPDATE_MEDICINE", "Updated medicine ID: " + medicineId + " (" + brandName + ")");
        return successResponse("Medicine updated", "");
    }

    private String handleDeleteMedicine(Element req) {
        int medicineId = parseInt(getText(req, "medicineId"), -1);
        int pharmacyId = parseInt(getText(req, "pharmacyId"), -1);
        boolean ok = dataStore.deleteMedicine(medicineId, pharmacyId);
        if (!ok) return errorResponse("Failed to delete medicine");
        server.log(currentUsername, "DELETE_MEDICINE", "Deleted medicine ID: " + medicineId);
        return successResponse("Medicine deleted", "");
    }

    private String handleCheckMedicineExists(Element req) {
        int pharmacyId = parseInt(getText(req, "pharmacyId"), -1);
        String brandName = getText(req, "brandName");
        String genericName = getText(req, "genericName");
        String dosage = getText(req, "dosage");
        int excludeId = parseInt(getText(req, "excludeId"), -1);
        boolean exists = dataStore.medicineExists(pharmacyId, brandName, genericName, dosage, excludeId);
        return successResponse("OK", "<exists>" + exists + "</exists>");
    }

    private String handleSearchMedicines(Element req) {
        int pharmacyId = parseInt(getText(req, "pharmacyId"), -1);
        String searchTerm = getText(req, "searchTerm");
        List<Medicine> list;
        if (pharmacyId > 0) {
            list = dataStore.searchMedicines(pharmacyId, searchTerm);
        } else {
            // Cross-pharmacy search: find medicine across all approved pharmacies
            list = dataStore.getAllMedicines();
            if (searchTerm != null && !searchTerm.isEmpty()) {
                String lower = searchTerm.toLowerCase();
                list.removeIf(m -> !m.getBrandName().toLowerCase().contains(lower) &&
                    !m.getGenericName().toLowerCase().contains(lower) &&
                    !m.getDosage().toLowerCase().contains(lower));
            }
            // Only show medicines from approved pharmacies
            list.removeIf(m -> {
                Pharmacy p = dataStore.getPharmacyById(m.getPharmacyId());
                return p == null || p.getStatus() != Pharmacy.PharmacyStatus.APPROVED;
            });
            // Only show medicines that have available stock
            list.removeIf(m -> m.getEffectiveQuantity() <= 0);
        }
        StringBuilder data = new StringBuilder("<medicines>");
        for (Medicine m : list) {
            Pharmacy p = dataStore.getPharmacyById(m.getPharmacyId());
            data.append("<medicineEntry>");
            data.append(medicineToXml(m));
            data.append("<pharmacyName>").append(esc(p != null ? p.getName() : "Unknown")).append("</pharmacyName>");
            data.append("<pharmacyAddress>").append(esc(p != null ? p.getAddress() : "")).append("</pharmacyAddress>");
            data.append("</medicineEntry>");
        }
        data.append("</medicines>");
        server.log(currentUsername, "SEARCH_MEDICINES", "Search: '" + searchTerm + "' pharmacyId=" + pharmacyId + " results=" + list.size());
        return successResponse("OK", data.toString());
    }

    private String handleGetAllMedicines() {
        List<Medicine> list = dataStore.getAllMedicines();
        StringBuilder data = new StringBuilder("<medicines>");
        for (Medicine m : list) {
            Pharmacy p = dataStore.getPharmacyById(m.getPharmacyId());
            data.append("<medicineEntry>");
            data.append(medicineToXml(m));
            data.append("<pharmacyName>").append(esc(p != null ? p.getName() : "Unknown")).append("</pharmacyName>");
            data.append("</medicineEntry>");
        }
        data.append("</medicines>");
        return successResponse("OK", data.toString());
    }

    // ==================== RESERVATION MANAGEMENT ====================

    private String handleReserveMedicine(Element req) {
        int userId = parseInt(getText(req, "userId"), -1);
        int medicineId = parseInt(getText(req, "medicineId"), -1);
        int quantity = parseInt(getText(req, "quantity"), 0);
        String pm = getText(req, "paymentMethod");
        PaymentMethod paymentMethod = PaymentMethod.valueOf(pm);

        dataStore.checkExpiredReservations();
        Reservation res = dataStore.reserveMedicine(userId, medicineId, quantity, paymentMethod);
        if (res == null) {
            return errorResponse("Reservation failed. Insufficient stock or invalid medicine.");
        }
        Medicine med = dataStore.getMedicineById(medicineId);
        String medName = med != null ? med.getBrandName() : "ID:" + medicineId;
        server.log(currentUsername, "RESERVE_MEDICINE", "Reserved " + quantity + "x " + medName + " (Reservation #" + res.getId() + ", " + pm + ")");
        return successResponse("Reservation successful", reservationToXml(res));
    }

    private String handleCancelReservation(Element req) {
        int reservationId = parseInt(getText(req, "reservationId"), -1);
        int userId = parseInt(getText(req, "userId"), -1);
        boolean ok = dataStore.cancelReservation(reservationId, userId);
        if (!ok) return errorResponse("Failed to cancel reservation");
        server.log(currentUsername, "CANCEL_RESERVATION", "Cancelled reservation #" + reservationId);
        return successResponse("Reservation cancelled", "");
    }

    private String handleApproveReservation(Element req) {
        int reservationId = parseInt(getText(req, "reservationId"), -1);
        boolean ok = dataStore.approveReservation(reservationId);
        if (!ok) return errorResponse("Failed to approve reservation");
        server.log(currentUsername, "APPROVE_RESERVATION", "Approved reservation #" + reservationId);
        return successResponse("Reservation approved", "");
    }

    private String handleRejectReservation(Element req) {
        int reservationId = parseInt(getText(req, "reservationId"), -1);
        boolean ok = dataStore.rejectReservation(reservationId);
        if (!ok) return errorResponse("Failed to reject reservation");
        server.log(currentUsername, "REJECT_RESERVATION", "Rejected reservation #" + reservationId);
        return successResponse("Reservation rejected", "");
    }

    private String handleCompleteReservation(Element req) {
        int reservationId = parseInt(getText(req, "reservationId"), -1);
        boolean ok = dataStore.completeReservation(reservationId);
        if (!ok) return errorResponse("Failed to complete reservation");
        server.log(currentUsername, "COMPLETE_RESERVATION", "Completed reservation #" + reservationId);
        return successResponse("Reservation completed", "");
    }

    private String handleGetPharmacyReservations(Element req) {
        int pharmacyId = parseInt(getText(req, "pharmacyId"), -1);
        dataStore.checkExpiredReservations();
        List<Reservation> list = dataStore.getReservationsByPharmacy(pharmacyId);
        StringBuilder data = new StringBuilder("<reservations>");
        for (Reservation r : list) {
            Medicine med = dataStore.getMedicineById(r.getMedicineId());
            User user = dataStore.getUserById(r.getUserId());
            data.append("<reservationEntry>");
            data.append(reservationToXml(r));
            data.append("<medicineName>").append(esc(med != null ? med.getBrandName() : "Unknown")).append("</medicineName>");
            data.append("<customerName>").append(esc(user != null ? user.getFullName() : "Unknown")).append("</customerName>");
            data.append("</reservationEntry>");
        }
        data.append("</reservations>");
        return successResponse("OK", data.toString());
    }

    private String handleGetUserReservations(Element req) {
        int userId = parseInt(getText(req, "userId"), -1);
        dataStore.checkExpiredReservations();
        List<Reservation> list = dataStore.getReservationsByUser(userId);
        StringBuilder data = new StringBuilder("<reservations>");
        for (Reservation r : list) {
            Medicine med = dataStore.getMedicineById(r.getMedicineId());
            Pharmacy pharm = dataStore.getPharmacyById(r.getPharmacyId());
            data.append("<reservationEntry>");
            data.append(reservationToXml(r));
            data.append("<medicineName>").append(esc(med != null ? med.getBrandName() : "Unknown")).append("</medicineName>");
            data.append("<pharmacyName>").append(esc(pharm != null ? pharm.getName() : "Unknown")).append("</pharmacyName>");
            data.append("</reservationEntry>");
        }
        data.append("</reservations>");
        return successResponse("OK", data.toString());
    }

    private String handleGetAllReservations() {
        dataStore.checkExpiredReservations();
        List<Reservation> list = dataStore.getAllReservations();
        StringBuilder data = new StringBuilder("<reservations>");
        for (Reservation r : list) {
            Medicine med = dataStore.getMedicineById(r.getMedicineId());
            Pharmacy pharm = dataStore.getPharmacyById(r.getPharmacyId());
            User user = dataStore.getUserById(r.getUserId());
            data.append("<reservationEntry>");
            data.append(reservationToXml(r));
            data.append("<medicineName>").append(esc(med != null ? med.getBrandName() : "Unknown")).append("</medicineName>");
            data.append("<pharmacyName>").append(esc(pharm != null ? pharm.getName() : "Unknown")).append("</pharmacyName>");
            data.append("<customerName>").append(esc(user != null ? user.getFullName() : "Unknown")).append("</customerName>");
            data.append("</reservationEntry>");
        }
        data.append("</reservations>");
        return successResponse("OK", data.toString());
    }

    // ==================== XML HELPERS ====================

    private String userToXml(User u) {
        return "<user>" +
            "<id>" + u.getId() + "</id>" +
            "<username>" + esc(u.getUsername()) + "</username>" +
            "<fullName>" + esc(u.getFullName()) + "</fullName>" +
            "<email>" + esc(u.getEmail()) + "</email>" +
            "<userType>" + u.getUserType().name() + "</userType>" +
            "<pharmacyId>" + u.getPharmacyId() + "</pharmacyId>" +
            "<isActive>" + u.isActive() + "</isActive>" +
            "</user>";
    }

    private String pharmacyToXml(Pharmacy p) {
        return "<pharmacy>" +
            "<id>" + p.getId() + "</id>" +
            "<name>" + esc(p.getName()) + "</name>" +
            "<address>" + esc(p.getAddress()) + "</address>" +
            "<contactNumber>" + esc(p.getContactNumber()) + "</contactNumber>" +
            "<email>" + esc(p.getEmail()) + "</email>" +
            "<description>" + esc(p.getDescription()) + "</description>" +
            "<status>" + p.getStatus().name() + "</status>" +
            "</pharmacy>";
    }

    private String medicineToXml(Medicine m) {
        return "<medicine>" +
            "<id>" + m.getId() + "</id>" +
            "<pharmacyId>" + m.getPharmacyId() + "</pharmacyId>" +
            "<brandName>" + esc(m.getBrandName()) + "</brandName>" +
            "<genericName>" + esc(m.getGenericName()) + "</genericName>" +
            "<dosage>" + esc(m.getDosage()) + "</dosage>" +
            "<dosageForm>" + esc(m.getDosageForm()) + "</dosageForm>" +
            "<price>" + m.getPrice() + "</price>" +
            "<quantityAvailable>" + m.getQuantityAvailable() + "</quantityAvailable>" +
            "<quantityReserved>" + m.getQuantityReserved() + "</quantityReserved>" +
            "<category>" + esc(m.getCategory()) + "</category>" +
            "<requiresPrescription>" + m.isRequiresPrescription() + "</requiresPrescription>" +
            "<effectiveQuantity>" + m.getEffectiveQuantity() + "</effectiveQuantity>" +
            "<status>" + m.getStatus().name() + "</status>" +
            "</medicine>";
    }

    private String reservationToXml(Reservation r) {
        return "<reservation>" +
            "<id>" + r.getId() + "</id>" +
            "<userId>" + r.getUserId() + "</userId>" +
            "<medicineId>" + r.getMedicineId() + "</medicineId>" +
            "<pharmacyId>" + r.getPharmacyId() + "</pharmacyId>" +
            "<quantity>" + r.getQuantity() + "</quantity>" +
            "<totalPrice>" + r.getTotalPrice() + "</totalPrice>" +
            "<reservationTime>" + r.getReservationTime().format(dtf) + "</reservationTime>" +
            "<expirationTime>" + r.getExpirationTime().format(dtf) + "</expirationTime>" +
            "<status>" + r.getStatus().name() + "</status>" +
            "<paymentMethod>" + r.getPaymentMethod().name() + "</paymentMethod>" +
            "<paymentStatus>" + r.getPaymentStatus().name() + "</paymentStatus>" +
            "<notes>" + esc(r.getNotes()) + "</notes>" +
            "</reservation>";
    }

    private String successResponse(String message, String data) {
        return "<?xml version=\"1.0\" encoding=\"UTF-8\"?>" +
            "<response><status>SUCCESS</status>" +
            "<message>" + esc(message) + "</message>" +
            "<data>" + (data != null ? data : "") + "</data></response>";
    }

    private String errorResponse(String message) {
        return "<?xml version=\"1.0\" encoding=\"UTF-8\"?>" +
            "<response><status>ERROR</status>" +
            "<message>" + esc(message) + "</message>" +
            "<data></data></response>";
    }

    private String esc(String s) {
        if (s == null) return "";
        return s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;")
                .replace("\"", "&quot;").replace("'", "&apos;");
    }

    private String getText(Element parent, String tag) {
        NodeList list = parent.getElementsByTagName(tag);
        if (list.getLength() > 0) return list.item(0).getTextContent().trim();
        return "";
    }

    private int parseInt(String s, int def) {
        try { return Integer.parseInt(s); } catch (Exception e) { return def; }
    }

    private double parseDouble(String s, double def) {
        try { return Double.parseDouble(s); } catch (Exception e) { return def; }
    }
}
