package client.controllers;

import client.network.ServerConnection;
import org.w3c.dom.*;

/**
 * Client-side controller for admin operations.
 */
public class AdminController {
    private final ServerConnection conn;

    public AdminController(ServerConnection conn) {
        this.conn = conn;
    }

    public Object[][] getAllUsers() {
        try {
            Document resp = conn.sendRequest(ServerConnection.buildRequest("GET_ALL_USERS"));
            if (!ServerConnection.isSuccess(resp)) return new Object[0][];
            Element data = ServerConnection.getDataElement(resp);
            NodeList users = data.getElementsByTagName("user");
            Object[][] result = new Object[users.getLength()][];
            for (int i = 0; i < users.getLength(); i++) {
                Element u = (Element) users.item(i);
                result[i] = new Object[]{
                    ServerConnection.parseInt(ServerConnection.getText(u, "id"), 0),
                    ServerConnection.getText(u, "username"),
                    ServerConnection.getText(u, "fullName"),
                    ServerConnection.getText(u, "email"),
                    ServerConnection.getText(u, "userType"),
                    ServerConnection.parseBoolean(ServerConnection.getText(u, "isActive")) ? "Active" : "Inactive"
                };
            }
            return result;
        } catch (Exception e) {
            e.printStackTrace();
            return new Object[0][];
        }
    }

    public String createUser(String username, String password, String fullName,
                             String email, String userType, int pharmacyId) {
        try {
            Document resp = conn.sendRequest(ServerConnection.buildRequest("CREATE_USER",
                "username", username, "password", password,
                "fullName", fullName, "email", email,
                "userType", userType, "pharmacyId", String.valueOf(pharmacyId)));
            return ServerConnection.getMessage(resp);
        } catch (Exception e) { return "Error: " + e.getMessage(); }
    }

    public String updateUser(int userId, String fullName, String email, String password) {
        try {
            Document resp = conn.sendRequest(ServerConnection.buildRequest("UPDATE_USER",
                "userId", String.valueOf(userId),
                "fullName", fullName, "email", email, "password", password));
            return ServerConnection.getMessage(resp);
        } catch (Exception e) { return "Error: " + e.getMessage(); }
    }

    public String deleteUser(int userId) {
        try {
            Document resp = conn.sendRequest(ServerConnection.buildRequest("DELETE_USER",
                "userId", String.valueOf(userId)));
            return ServerConnection.getMessage(resp);
        } catch (Exception e) { return "Error: " + e.getMessage(); }
    }

    public Object[][] getAllPharmacies() {
        try {
            Document resp = conn.sendRequest(ServerConnection.buildRequest("GET_ALL_PHARMACIES"));
            if (!ServerConnection.isSuccess(resp)) return new Object[0][];
            Element data = ServerConnection.getDataElement(resp);
            NodeList pharmacies = data.getElementsByTagName("pharmacy");
            Object[][] result = new Object[pharmacies.getLength()][];
            for (int i = 0; i < pharmacies.getLength(); i++) {
                Element p = (Element) pharmacies.item(i);
                result[i] = new Object[]{
                    ServerConnection.parseInt(ServerConnection.getText(p, "id"), 0),
                    ServerConnection.getText(p, "name"),
                    ServerConnection.getText(p, "address"),
                    ServerConnection.getText(p, "contactNumber"),
                    ServerConnection.getText(p, "email"),
                    ServerConnection.getText(p, "status")
                };
            }
            return result;
        } catch (Exception e) { return new Object[0][]; }
    }

    public String approvePharmacy(int pharmacyId) {
        try {
            Document resp = conn.sendRequest(ServerConnection.buildRequest("APPROVE_PHARMACY",
                "pharmacyId", String.valueOf(pharmacyId)));
            return ServerConnection.getMessage(resp);
        } catch (Exception e) { return "Error: " + e.getMessage(); }
    }

    public String rejectPharmacy(int pharmacyId) {
        try {
            Document resp = conn.sendRequest(ServerConnection.buildRequest("REJECT_PHARMACY",
                "pharmacyId", String.valueOf(pharmacyId)));
            return ServerConnection.getMessage(resp);
        } catch (Exception e) { return "Error: " + e.getMessage(); }
    }

    public Object[][] getAllReservations() {
        try {
            Document resp = conn.sendRequest(ServerConnection.buildRequest("GET_ALL_RESERVATIONS"));
            if (!ServerConnection.isSuccess(resp)) return new Object[0][];
            Element data = ServerConnection.getDataElement(resp);
            NodeList entries = data.getElementsByTagName("reservationEntry");
            Object[][] result = new Object[entries.getLength()][];
            for (int i = 0; i < entries.getLength(); i++) {
                Element entry = (Element) entries.item(i);
                Element r = (Element) entry.getElementsByTagName("reservation").item(0);
                result[i] = new Object[]{
                    ServerConnection.parseInt(ServerConnection.getText(r, "id"), 0),
                    ServerConnection.getText(entry, "customerName"),
                    ServerConnection.getText(entry, "medicineName"),
                    ServerConnection.getText(entry, "pharmacyName"),
                    ServerConnection.parseInt(ServerConnection.getText(r, "quantity"), 0),
                    String.format("%.2f", ServerConnection.parseDouble(ServerConnection.getText(r, "totalPrice"), 0)),
                    ServerConnection.getText(r, "paymentMethod").replace("_", " "),
                    ServerConnection.getText(r, "paymentStatus"),
                    ServerConnection.getText(r, "status")
                };
            }
            return result;
        } catch (Exception e) { return new Object[0][]; }
    }

    public Object[][] getAllMedicines() {
        try {
            Document resp = conn.sendRequest(ServerConnection.buildRequest("GET_ALL_MEDICINES"));
            if (!ServerConnection.isSuccess(resp)) return new Object[0][];
            Element data = ServerConnection.getDataElement(resp);
            NodeList entries = data.getElementsByTagName("medicineEntry");
            Object[][] result = new Object[entries.getLength()][];
            for (int i = 0; i < entries.getLength(); i++) {
                Element entry = (Element) entries.item(i);
                Element m = (Element) entry.getElementsByTagName("medicine").item(0);
                result[i] = new Object[]{
                    ServerConnection.parseInt(ServerConnection.getText(m, "id"), 0),
                    ServerConnection.getText(m, "brandName"),
                    ServerConnection.getText(m, "genericName"),
                    ServerConnection.getText(m, "dosage"),
                    ServerConnection.getText(m, "dosageForm"),
                    String.format("%.2f", ServerConnection.parseDouble(ServerConnection.getText(m, "price"), 0)),
                    ServerConnection.parseInt(ServerConnection.getText(m, "quantityAvailable"), 0),
                    ServerConnection.getText(m, "category"),
                    ServerConnection.getText(entry, "pharmacyName"),
                    ServerConnection.getText(m, "status")
                };
            }
            return result;
        } catch (Exception e) { return new Object[0][]; }
    }
}
