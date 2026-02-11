package client.controllers;

import client.network.ServerConnection;
import org.w3c.dom.*;

/**
 * Client-side controller for pharmacist operations.
 */
public class PharmacistController {
    private final ServerConnection conn;

    public PharmacistController(ServerConnection conn) {
        this.conn = conn;
    }

    public Object[][] getMedicines(int pharmacyId) {
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
                    ServerConnection.getText(m, "dosage"),
                    ServerConnection.getText(m, "dosageForm"),
                    String.format("%.2f", ServerConnection.parseDouble(ServerConnection.getText(m, "price"), 0)),
                    ServerConnection.parseInt(ServerConnection.getText(m, "quantityAvailable"), 0),
                    ServerConnection.parseInt(ServerConnection.getText(m, "quantityReserved"), 0),
                    ServerConnection.getText(m, "category"),
                    ServerConnection.getText(m, "status")
                };
            }
            return result;
        } catch (Exception e) { return new Object[0][]; }
    }

    public String addMedicine(int pharmacyId, String brandName, String genericName,
                              String dosage, String dosageForm, double price,
                              int quantity, String category) {
        try {
            Document resp = conn.sendRequest(ServerConnection.buildRequest("CREATE_MEDICINE",
                "pharmacyId", String.valueOf(pharmacyId),
                "brandName", brandName, "genericName", genericName,
                "dosage", dosage, "dosageForm", dosageForm,
                "price", String.valueOf(price), "quantity", String.valueOf(quantity),
                "category", category));
            return ServerConnection.getMessage(resp);
        } catch (Exception e) { return "Error: " + e.getMessage(); }
    }

    public String updateMedicine(int medicineId, String brandName, String genericName,
                                 String dosage, String dosageForm, double price,
                                 int quantity, String category) {
        try {
            Document resp = conn.sendRequest(ServerConnection.buildRequest("UPDATE_MEDICINE",
                "medicineId", String.valueOf(medicineId),
                "brandName", brandName, "genericName", genericName,
                "dosage", dosage, "dosageForm", dosageForm,
                "price", String.valueOf(price), "quantity", String.valueOf(quantity),
                "category", category));
            return ServerConnection.getMessage(resp);
        } catch (Exception e) { return "Error: " + e.getMessage(); }
    }

    public String deleteMedicine(int medicineId, int pharmacyId) {
        try {
            Document resp = conn.sendRequest(ServerConnection.buildRequest("DELETE_MEDICINE",
                "medicineId", String.valueOf(medicineId),
                "pharmacyId", String.valueOf(pharmacyId)));
            return ServerConnection.getMessage(resp);
        } catch (Exception e) { return "Error: " + e.getMessage(); }
    }

    public Object[][] getReservations(int pharmacyId) {
        try {
            Document resp = conn.sendRequest(ServerConnection.buildRequest("GET_PHARMACY_RESERVATIONS",
                "pharmacyId", String.valueOf(pharmacyId)));
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
                    ServerConnection.parseInt(ServerConnection.getText(r, "quantity"), 0),
                    String.format("%.2f", ServerConnection.parseDouble(ServerConnection.getText(r, "totalPrice"), 0)),
                    ServerConnection.getText(r, "paymentMethod").replace("_", " "),
                    ServerConnection.getText(r, "paymentStatus"),
                    ServerConnection.getText(r, "reservationTime").substring(0, Math.min(16, ServerConnection.getText(r, "reservationTime").length())),
                    ServerConnection.getText(r, "status")
                };
            }
            return result;
        } catch (Exception e) { return new Object[0][]; }
    }

    public String approveReservation(int reservationId) {
        try {
            Document resp = conn.sendRequest(ServerConnection.buildRequest("APPROVE_RESERVATION",
                "reservationId", String.valueOf(reservationId)));
            return ServerConnection.getMessage(resp);
        } catch (Exception e) { return "Error: " + e.getMessage(); }
    }

    public String rejectReservation(int reservationId) {
        try {
            Document resp = conn.sendRequest(ServerConnection.buildRequest("REJECT_RESERVATION",
                "reservationId", String.valueOf(reservationId)));
            return ServerConnection.getMessage(resp);
        } catch (Exception e) { return "Error: " + e.getMessage(); }
    }

    public String completeReservation(int reservationId) {
        try {
            Document resp = conn.sendRequest(ServerConnection.buildRequest("COMPLETE_RESERVATION",
                "reservationId", String.valueOf(reservationId)));
            return ServerConnection.getMessage(resp);
        } catch (Exception e) { return "Error: " + e.getMessage(); }
    }
}
