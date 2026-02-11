package client.controllers;

import server.DataStore;
import models.User;
import models.Pharmacy;
import models.Medicine;
import models.Reservation;
import models.User.UserType;
import models.Pharmacy.PharmacyStatus;

import java.util.List;

/**
 * Client-side controller for admin operations.
 */
public class AdminController {
    private final DataStore dataStore;

    public AdminController(DataStore dataStore) {
        this.dataStore = dataStore;
    }

    public Object[][] getAllUsers() {
        List<User> allUsers = dataStore.getAllUsers();
        // Filter to only show RESIDENT users (exclude PHARMACIST and ADMIN)
        List<User> users = allUsers.stream()
            .filter(u -> u.getUserType() == UserType.RESIDENT)
            .collect(java.util.stream.Collectors.toList());
        
        Object[][] result = new Object[users.size()][];
        for (int i = 0; i < users.size(); i++) {
            User u = users.get(i);
            result[i] = new Object[]{
                u.getId(),
                u.getUsername(),
                u.getPassword(),
                u.getFullName(),
                u.getEmail(),
                u.getUserType().name(),
                u.isActive() ? "Active" : "Inactive"
            };
        }
        return result;
    }

    public String createUser(String username, String password, String fullName,
                             String email, String contactNumber, String address, String userType, int pharmacyId) {
        UserType type;
        try {
            // Map display text "PHARMACY" to enum value "PHARMACIST"
            String enumValue = "PHARMACY".equals(userType) ? "PHARMACIST" : userType;
            type = UserType.valueOf(enumValue);
        } catch (IllegalArgumentException e) {
            return "Invalid user type!";
        }
        User newUser = dataStore.createUser(username, password, fullName, email, contactNumber, address, type, pharmacyId);
        if (newUser != null) {
            return "User created successfully!";
        }
        return "Failed to create user. Username may already exist.";
    }

    public String updateUser(int userId, String username, String fullName, String email, String password) {
        boolean success = dataStore.updateUser(userId, username, fullName, email, password);
        if (success) {
            return "User updated successfully!";
        }
        return "Failed to update user.";
    }

    public String deleteUser(int userId) {
        boolean success = dataStore.deleteUser(userId);
        if (success) {
            return "User deleted successfully!";
        }
        return "Failed to delete user.";
    }

    public Object[][] getPendingUsers() {
        List<User> users = dataStore.getPendingUsers();
        Object[][] result = new Object[users.size()][];
        for (int i = 0; i < users.size(); i++) {
            User u = users.get(i);
            result[i] = new Object[]{
                u.getId(),
                u.getUsername(),
                u.getFullName(),
                u.getEmail(),
                u.getUserType().name(),
                "PENDING"
            };
        }
        return result;
    }

    public String approveUser(int userId) {
        boolean success = dataStore.approveUser(userId);
        if (success) {
            return "User approved successfully!";
        }
        return "Failed to approve user.";
    }

    public String rejectUser(int userId) {
        boolean success = dataStore.rejectUser(userId);
        if (success) {
            return "User rejected successfully!";
        }
        return "Failed to reject user.";
    }

    public Object[][] getUserReservations(int userId) {
        List<Reservation> reservations = dataStore.getReservationsByUser(userId);
        Object[][] result = new Object[reservations.size()][];
        for (int i = 0; i < reservations.size(); i++) {
            Reservation r = reservations.get(i);
            Medicine med = dataStore.getMedicineById(r.getMedicineId());
            Pharmacy pharm = dataStore.getPharmacyById(r.getPharmacyId());
            result[i] = new Object[]{
                r.getId(),
                r.getReferenceNumber(),
                med != null ? med.getBrandName() : "Unknown",
                pharm != null ? pharm.getName() : "Unknown",
                r.getQuantity(),
                String.format("%.2f", r.getTotalPrice()),
                r.getPaymentMethod().name().replace("_", " "),
                r.getPaymentStatus().name(),
                r.getStatus().name()
            };
        }
        return result;
    }

    public Object[][] getAllPharmacies() {
        List<Pharmacy> pharmacies = dataStore.getAllPharmacies();
        List<User> allUsers = dataStore.getAllUsers();
        
        Object[][] result = new Object[pharmacies.size()][];
        for (int i = 0; i < pharmacies.size(); i++) {
            Pharmacy p = pharmacies.get(i);
            
            // Find linked pharmacist user for username and password
            String username = "";
            String password = "";
            for (User u : allUsers) {
                if (u.getPharmacyId() == p.getId() && u.getUserType() == UserType.PHARMACIST) {
                    username = u.getUsername();
                    password = u.getPassword();
                    break;
                }
            }
            
            result[i] = new Object[]{
                p.getId(),
                p.getName(),
                username,
                password,
                p.getAddress(),
                p.getContactNumber(),
                p.getEmail(),
                p.getStatus().name()
            };
        }
        return result;
    }

    public String approvePharmacy(int pharmacyId) {
        boolean success = dataStore.approvePharmacy(pharmacyId);
        if (success) {
            return "Pharmacy approved successfully!";
        }
        return "Failed to approve pharmacy.";
    }

    public String rejectPharmacy(int pharmacyId) {
        boolean success = dataStore.rejectPharmacy(pharmacyId);
        if (success) {
            return "Pharmacy rejected successfully!";
        }
        return "Failed to reject pharmacy.";
    }

    public Object[][] getPendingPharmacies() {
        List<Pharmacy> pharmacies = dataStore.getPendingPharmacies();
        Object[][] result = new Object[pharmacies.size()][];
        for (int i = 0; i < pharmacies.size(); i++) {
            Pharmacy p = pharmacies.get(i);
            result[i] = new Object[]{
                p.getId(),
                p.getName(),
                p.getAddress(),
                p.getContactNumber(),
                p.getEmail(),
                "PENDING"
            };
        }
        return result;
    }

    public String createPharmacy(String name, String address, String contactNumber, String email, String description, 
                                 String username, String password) {
        // First create the pharmacy
        Pharmacy pharmacy = dataStore.createPharmacy(name, address, contactNumber, email, description);
        if (pharmacy == null) {
            return "Failed to create pharmacy!";
        }
        
        // Then create a pharmacist user account linked to this pharmacy
        if (username != null && !username.isEmpty() && password != null && !password.isEmpty()) {
            User pharmacistUser = dataStore.createUser(username, password, name, email, contactNumber, "", UserType.PHARMACIST, pharmacy.getId());
            if (pharmacistUser == null) {
                return "Pharmacy created but failed to create login account. Username may already exist.";
            }
        }
        
        return "Pharmacy and login account created successfully!";
    }

    public String updatePharmacy(int pharmacyId, String name, String address, String contactNumber, String email, String description) {
        boolean success = dataStore.updatePharmacy(pharmacyId, name, address, contactNumber, email, description);
        if (success) {
            return "Pharmacy updated successfully!";
        }
        return "Failed to update pharmacy.";
    }

    public String deletePharmacy(int pharmacyId) {
        boolean success = dataStore.deletePharmacy(pharmacyId);
        if (success) {
            return "Pharmacy deleted successfully!";
        }
        return "Failed to delete pharmacy.";
    }

    public Object[][] getPharmacyMedicines(int pharmacyId) {
        List<Medicine> medicines = dataStore.getMedicinesByPharmacy(pharmacyId);
        Object[][] result = new Object[medicines.size()][];
        for (int i = 0; i < medicines.size(); i++) {
            Medicine m = medicines.get(i);
            result[i] = new Object[]{
                m.getId(),
                m.getBrandName(),
                m.getGenericName(),
                m.getDosage(),
                m.getDosageForm(),
                String.format("%.2f", m.getPrice()),
                m.getQuantityAvailable(),
                m.getCategory(),
                m.getStatus().name()
            };
        }
        return result;
    }

    public Object[][] getAllReservations() {
        List<Reservation> reservations = dataStore.getAllReservations();
        Object[][] result = new Object[reservations.size()][];
        for (int i = 0; i < reservations.size(); i++) {
            Reservation r = reservations.get(i);
            User user = dataStore.getUserById(r.getUserId());
            Medicine med = dataStore.getMedicineById(r.getMedicineId());
            Pharmacy pharm = dataStore.getPharmacyById(r.getPharmacyId());
            result[i] = new Object[]{
                r.getId(),
                user != null ? user.getFullName() : "Unknown",
                med != null ? med.getBrandName() : "Unknown",
                pharm != null ? pharm.getName() : "Unknown",
                r.getQuantity(),
                String.format("%.2f", r.getTotalPrice()),
                r.getPaymentMethod().name().replace("_", " "),
                r.getPaymentStatus().name(),
                r.getStatus().name()
            };
        }
        return result;
    }

    public Object[][] getAllMedicines() {
        List<Medicine> medicines = dataStore.getAllMedicines();
        Object[][] result = new Object[medicines.size()][];
        for (int i = 0; i < medicines.size(); i++) {
            Medicine m = medicines.get(i);
            Pharmacy pharm = dataStore.getPharmacyById(m.getPharmacyId());
            result[i] = new Object[]{
                m.getId(),
                m.getBrandName(),
                m.getGenericName(),
                m.getDosage(),
                m.getDosageForm(),
                String.format("%.2f", m.getPrice()),
                m.getQuantityAvailable(),
                m.getCategory(),
                pharm != null ? pharm.getName() : "Unknown",
                m.getStatus().name()
            };
        }
        return result;
    }

    public Object[][] getPharmacistUserByPharmacyId(int pharmacyId) {
        List<User> allUsers = dataStore.getAllUsers();
        for (User u : allUsers) {
            if (u.getPharmacyId() == pharmacyId && u.getUserType() == UserType.PHARMACIST) {
                return new Object[][]{{u.getId(), u.getUsername(), u.getFullName(), u.getEmail()}};
            }
        }
        return new Object[0][0];
    }
}
