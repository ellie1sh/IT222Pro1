package models;

public class Pharmacy {
    public enum PharmacyStatus {
        PENDING, APPROVED, REJECTED
    }

    private int id;
    private String name;
    private String address;
    private String contactNumber;
    private String email;
    private String description;
    private PharmacyStatus status;

    public Pharmacy(int id, String name, String address, String contactNumber,
                    String email, String description) {
        this.id = id;
        this.name = name;
        this.address = address;
        this.contactNumber = contactNumber;
        this.email = email;
        this.description = description;
        this.status = PharmacyStatus.PENDING;
    }

    // Getters and Setters
    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getAddress() { return address; }
    public void setAddress(String address) { this.address = address; }

    public String getContactNumber() { return contactNumber; }
    public void setContactNumber(String contactNumber) { this.contactNumber = contactNumber; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public PharmacyStatus getStatus() { return status; }
    public void setStatus(PharmacyStatus status) { this.status = status; }

    @Override
    public String toString() {
        return name + " - " + address;
    }
}
