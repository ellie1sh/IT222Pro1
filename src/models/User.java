package models;

public class User {
    public enum UserType {
        ADMIN, PHARMACIST, RESIDENT
    }

    private int id;
    private String username;
    private String password;
    private String fullName;
    private String email;
    private UserType userType;
    private int pharmacyId;
    private boolean isActive;

    public User(int id, String username, String password, String fullName,
                String email, UserType userType) {
        this.id = id;
        this.username = username;
        this.password = password;
        this.fullName = fullName;
        this.email = email;
        this.userType = userType;
        this.pharmacyId = -1;
        this.isActive = true;
    }

    // Getters and Setters
    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }

    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }

    public String getFullName() { return fullName; }
    public void setFullName(String fullName) { this.fullName = fullName; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public UserType getUserType() { return userType; }
    public void setUserType(UserType userType) { this.userType = userType; }

    public int getPharmacyId() { return pharmacyId; }
    public void setPharmacyId(int pharmacyId) { this.pharmacyId = pharmacyId; }

    public boolean isActive() { return isActive; }
    public void setActive(boolean active) { isActive = active; }

    @Override
    public String toString() {
        return fullName + " (" + userType + ")";
    }
}
