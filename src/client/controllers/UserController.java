package client.controllers;

import client.network.ServerConnection;
import org.w3c.dom.*;

/**
 * Client-side controller for resident/user operations.
 */
public class UserController {
    private final ServerConnection conn;

    public UserController(ServerConnection conn) {
        this.conn = conn;
    }

    public Object[][] getApprovedPharmacies() {
        try {
            Document resp = conn.sendRequest(ServerConnection.buildRequest("GET_APPROVED_PHARMACIES"));
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
                    ServerConnection.getText(p, "contactNumber")
                };
            }
            return result;
        } catch (Exception e) { return new Object[0][]; }
    }

    public Object[][] getPharmacyMedicines(int pharmacyId) {
        try {
            Document resp = conn.sendRequest(ServerConnection.buildRequest("GET_PHARMACY_MEDICINES",
                "pharmacyId", String.valueOf(pharmacyId)));
            if (!ServerConnection.isSuccess(resp)) return new Object[0][];
            Element data = ServerConnection.getDataElement(resp);
            NodeList meds = data.getElementsByTagName("medicine");
            Object[][] result = new Object[meds.getLength()][];
            for (int i = 0; i < meds.getLength(); i++) {
                Element m = (Element) meds.item(i);
                result[i] = new Object[]{
                    ServerConnection.parseInt(ServerConnection.getText(m, "id"), 0),
                    ServerConnection.getText(m, "brandName"),
                    ServerConnection.getText(m, "genericName"),
                    ServerConnection.getText(m, "dosage") + " " + ServerConnection.getText(m, "dosageForm"),
                    String.format("%.2f", ServerConnection.parseDouble(ServerConnection.getText(m, "price"), 0)),
                    ServerConnection.parseInt(ServerConnection.getText(m, "effectiveQuantity"), 0),
                    ServerConnection.getText(m, "category")
                };
            }
            return result;
        } catch (Exception e) { return new Object[0][]; }
    }

    /**
     * Search medicines across all pharmacies (pharmacyId = -1) or within a specific pharmacy.
     * Returns: ID, Brand, Generic, Dosage, Price, Available, Category, Pharmacy, Address
     */
    public Object[][] searchMedicines(int pharmacyId, String searchTerm) {
        try {
            Document resp = conn.sendRequest(ServerConnection.buildRequest("SEARCH_MEDICINES",
                "pharmacyId", String.valueOf(pharmacyId),
                "searchTerm", searchTerm));
            if (!ServerConnection.isSuccess(resp)) return new Object[0][];
            Element data = ServerConnection.getDataElement(resp);
            NodeList entries = data.getElementsByTagName("medicineEntry");
            Object[][] result = new Object[entries.getLength()][];
            for (int i = 0; i < entries.getLength(); i++) {
                Element entry = (Element) entries.item(i);
                Element m = (Element) entry.getElementsByTagName("medicine").item(0);
                result[i] = new Object[]{
                    ServerConnection.parseInt(ServerConnection.getText(m, "id"), 0),
                    ServerConnection.getText(entry, "pharmacyName"),
                    ServerConnection.getText(m, "brandName"),
                    ServerConnection.getText(m, "genericName"),
                    ServerConnection.getText(m, "dosage") + " " + ServerConnection.getText(m, "dosageForm"),
                    String.format("%.2f", ServerConnection.parseDouble(ServerConnection.getText(m, "price"), 0)),
                    ServerConnection.parseInt(ServerConnection.getText(m, "effectiveQuantity"), 0),
                    ServerConnection.getText(m, "category"),
                    ServerConnection.getText(entry, "pharmacyAddress")
                };
            }
            return result;
        } catch (Exception e) { return new Object[0][]; }
    }

    /**
     * Reserve a medicine. Returns message string.
     * paymentMethod: ONLINE_BANK, E_PAYMENT, PAY_AT_STORE
     */
    public String reserveMedicine(int userId, int medicineId, int quantity, String paymentMethod) {
        try {
            Document resp = conn.sendRequest(ServerConnection.buildRequest("RESERVE_MEDICINE",
                "userId", String.valueOf(userId),
                "medicineId", String.valueOf(medicineId),
                "quantity", String.valueOf(quantity),
                "paymentMethod", paymentMethod));
            if (ServerConnection.isSuccess(resp)) {
                Element data = ServerConnection.getDataElement(resp);
                Element r = (Element) data.getElementsByTagName("reservation").item(0);
                int resId = ServerConnection.parseInt(ServerConnection.getText(r, "id"), 0);
                String status = ServerConnection.getText(r, "status");
                String total = String.format("%.2f", ServerConnection.parseDouble(ServerConnection.getText(r, "totalPrice"), 0));
                String notes = ServerConnection.getText(r, "notes");
                return "SUCCESS|Reservation #" + resId + "\nTotal: PHP " + total +
                    "\nStatus: " + status + "\n" + notes;
            }
            return "ERROR|" + ServerConnection.getMessage(resp);
        } catch (Exception e) { return "ERROR|" + e.getMessage(); }
    }

    public String cancelReservation(int reservationId, int userId) {
        try {
            Document resp = conn.sendRequest(ServerConnection.buildRequest("CANCEL_RESERVATION",
                "reservationId", String.valueOf(reservationId),
                "userId", String.valueOf(userId)));
            return ServerConnection.getMessage(resp);
        } catch (Exception e) { return "Error: " + e.getMessage(); }
    }

    public Object[][] getUserReservations(int userId) {
        try {
            Document resp = conn.sendRequest(ServerConnection.buildRequest("GET_USER_RESERVATIONS",
                "userId", String.valueOf(userId)));
            if (!ServerConnection.isSuccess(resp)) return new Object[0][];
            Element data = ServerConnection.getDataElement(resp);
            NodeList entries = data.getElementsByTagName("reservationEntry");
            Object[][] result = new Object[entries.getLength()][];
            for (int i = 0; i < entries.getLength(); i++) {
                Element entry = (Element) entries.item(i);
                Element r = (Element) entry.getElementsByTagName("reservation").item(0);
                String resTime = ServerConnection.getText(r, "reservationTime");
                String expTime = ServerConnection.getText(r, "expirationTime");
                result[i] = new Object[]{
                    ServerConnection.parseInt(ServerConnection.getText(r, "id"), 0),
                    ServerConnection.getText(entry, "medicineName"),
                    ServerConnection.getText(entry, "pharmacyName"),
                    ServerConnection.parseInt(ServerConnection.getText(r, "quantity"), 0),
                    String.format("%.2f", ServerConnection.parseDouble(ServerConnection.getText(r, "totalPrice"), 0)),
                    ServerConnection.getText(r, "paymentMethod").replace("_", " "),
                    ServerConnection.getText(r, "paymentStatus"),
                    resTime.length() >= 16 ? resTime.substring(0, 16) : resTime,
                    expTime.length() >= 16 ? expTime.substring(0, 16) : expTime,
                    ServerConnection.getText(r, "status")
                };
            }
            return result;
        } catch (Exception e) { return new Object[0][]; }
    }
}
