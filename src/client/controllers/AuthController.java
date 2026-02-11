package client.controllers;

import client.network.ServerConnection;
import org.w3c.dom.*;

/**
 * Client-side controller for authentication.
 * Sends XML requests to server and parses responses.
 */
public class AuthController {
    private final ServerConnection conn;
    private int currentUserId = -1;
    private String currentUsername = "";
    private String currentFullName = "";
    private String currentUserType = "";
    private int currentPharmacyId = -1;

    public AuthController(ServerConnection conn) {
        this.conn = conn;
    }

    public boolean login(String username, String password) {
        try {
            String xml = ServerConnection.buildRequest("LOGIN",
                "username", username, "password", password);
            Document resp = conn.sendRequest(xml);
            if (ServerConnection.isSuccess(resp)) {
                Element data = ServerConnection.getDataElement(resp);
                NodeList users = data.getElementsByTagName("user");
                if (users.getLength() > 0) {
                    Element user = (Element) users.item(0);
                    currentUserId = ServerConnection.parseInt(ServerConnection.getText(user, "id"), -1);
                    currentUsername = ServerConnection.getText(user, "username");
                    currentFullName = ServerConnection.getText(user, "fullName");
                    currentUserType = ServerConnection.getText(user, "userType");
                    currentPharmacyId = ServerConnection.parseInt(ServerConnection.getText(user, "pharmacyId"), -1);
                }
                return true;
            }
            return false;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    public String register(String username, String password, String fullName, String email) {
        try {
            String xml = ServerConnection.buildRequest("REGISTER",
                "username", username, "password", password,
                "fullName", fullName, "email", email);
            Document resp = conn.sendRequest(xml);
            return ServerConnection.getMessage(resp);
        } catch (Exception e) {
            return "Connection error: " + e.getMessage();
        }
    }

    public void logout() {
        currentUserId = -1;
        currentUsername = "";
        currentFullName = "";
        currentUserType = "";
        currentPharmacyId = -1;
    }

    public int getCurrentUserId() { return currentUserId; }
    public String getCurrentUsername() { return currentUsername; }
    public String getCurrentFullName() { return currentFullName; }
    public String getCurrentUserType() { return currentUserType; }
    public int getCurrentPharmacyId() { return currentPharmacyId; }
    public boolean isLoggedIn() { return currentUserId > 0; }
    public boolean isAdmin() { return "ADMIN".equals(currentUserType); }
    public boolean isPharmacist() { return "PHARMACIST".equals(currentUserType); }
    public boolean isResident() { return "RESIDENT".equals(currentUserType); }
}
