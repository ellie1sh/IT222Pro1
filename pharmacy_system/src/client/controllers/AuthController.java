package client.controllers;

import server.DataStore;
import models.User;
import models.User.UserType;
import models.Pharmacy;

/**
 * Client-side controller for authentication.
 * Uses DataStore directly for authentication operations.
 */
public class AuthController {
    private final DataStore dataStore;
    private int currentUserId = -1;
    private String currentUsername = "";
    private String currentFullName = "";
    private String currentUserType = "";
    private int currentPharmacyId = -1;

    public AuthController(DataStore dataStore) {
        this.dataStore = dataStore;
    }

    public boolean login(String username, String password) {
        User user = dataStore.authenticateUser(username, password);
        if (user != null) {
            currentUserId = user.getId();
            currentUsername = user.getUsername();
            currentFullName = user.getFullName();
            currentUserType = user.getUserType().name();
            currentPharmacyId = user.getPharmacyId();
            return true;
        }
        return false;
    }

    public String register(String username, String password, String fullName, String email, String contactNumber, String address, String accountType) {
        // Check if username already exists
        User existing = dataStore.authenticateUser(username, password);
        if (existing != null) {
            return "Username already exists!";
        }
        
        if ("PHARMACY".equals(accountType)) {
            // Register as a pharmacy - create pharmacy entity (pending)
            Pharmacy newPharmacy = dataStore.createPharmacy(fullName, address, contactNumber, email, "");
            if (newPharmacy != null) {
                // Also create a pharmacist user account linked to this pharmacy (pending)
                User pharmacistUser = dataStore.createUser(username, password, fullName, email, contactNumber, address, 
                    UserType.PHARMACIST, newPharmacy.getId(), false);
                if (pharmacistUser != null) {
                    return "Registration successful! Your pharmacy account is pending admin approval.";
                }
            }
            return "Registration failed. Please try again.";
        } else {
            // Register as a resident user (pending)
            User newUser = dataStore.createUser(username, password, fullName, email, contactNumber, address, UserType.RESIDENT, -1, false);
            if (newUser != null) {
                return "Registration successful! Your account is pending admin approval.";
            }
            return "Registration failed. Please try again.";
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
