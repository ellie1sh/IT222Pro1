package models;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Random;
import java.util.Set;

/**
 * Represents a prescription photo submitted by a user.
 * Visible to all pharmacies; only one can accept it.
 */
public class PrescriptionRequest {
    public enum Status {
        PENDING,    // Waiting for a pharmacy to accept
        ACCEPTED,   // One pharmacy has accepted
        EXPIRED,    // Not accepted within time limit
        CANCELLED,  // Cancelled by customer
        READY_FOR_PICKUP, // Pharmacy marked ready for pickup
        COMPLETED // Paid and picked up
    }

    private int id;
    private int userId;
    private String referenceNumber;
    private String imagePath;
    private Status status;
    private int acceptedPharmacyId;  // -1 if not yet accepted (user-chosen pharmacy)
    private LocalDateTime submittedAt;
    private Set<Integer> declinedPharmacyIds = new HashSet<>();
    private Set<Integer> confirmedPharmacyIds = new HashSet<>();
    private int chosenPharmacyId = -1;
    private String medicineName;
    private int medicineQuantity;
    private double medicineAmount;
    private LocalDateTime paidAt;

    public PrescriptionRequest(int id, int userId, String imagePath) {
        this.id = id;
        this.userId = userId;
        // Generate random reference number: RX + 8 random digits
        Random rand = new Random();
        int randomNum = 10000000 + rand.nextInt(90000000); // 8-digit number
        this.referenceNumber = "RX" + randomNum;
        this.imagePath = imagePath;
        this.status = Status.PENDING;
        this.acceptedPharmacyId = -1;
        this.submittedAt = LocalDateTime.now();
        this.medicineName = "";
        this.medicineQuantity = 0;
        this.medicineAmount = 0.0;
    }

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public int getUserId() { return userId; }
    public void setUserId(int userId) { this.userId = userId; }

    public String getReferenceNumber() { return referenceNumber; }
    public void setReferenceNumber(String referenceNumber) { this.referenceNumber = referenceNumber; }

    public String getImagePath() { return imagePath; }
    public void setImagePath(String imagePath) { this.imagePath = imagePath; }

    public Status getStatus() { return status; }
    public void setStatus(Status status) { this.status = status; }

    public int getAcceptedPharmacyId() { return acceptedPharmacyId; }
    public void setAcceptedPharmacyId(int acceptedPharmacyId) { this.acceptedPharmacyId = acceptedPharmacyId; }

    public LocalDateTime getSubmittedAt() { return submittedAt; }
    public void setSubmittedAt(LocalDateTime submittedAt) { this.submittedAt = submittedAt; }

    public Set<Integer> getDeclinedPharmacyIds() { return declinedPharmacyIds; }
    public void setDeclinedPharmacyIds(Set<Integer> ids) { this.declinedPharmacyIds = ids != null ? ids : new HashSet<>(); }
    public void addDeclinedPharmacyId(int pharmacyId) { declinedPharmacyIds.add(pharmacyId); }

    public Set<Integer> getConfirmedPharmacyIds() { return confirmedPharmacyIds; }
    public void setConfirmedPharmacyIds(Set<Integer> ids) { this.confirmedPharmacyIds = ids != null ? ids : new HashSet<>(); }
    public void addConfirmedPharmacyId(int pharmacyId) { confirmedPharmacyIds.add(pharmacyId); }

    public int getChosenPharmacyId() { return chosenPharmacyId; }
    public void setChosenPharmacyId(int chosenPharmacyId) { this.chosenPharmacyId = chosenPharmacyId; }

    public String getMedicineName() { return medicineName; }
    public void setMedicineName(String medicineName) { this.medicineName = medicineName != null ? medicineName : ""; }

    public int getMedicineQuantity() { return medicineQuantity; }
    public void setMedicineQuantity(int medicineQuantity) { this.medicineQuantity = medicineQuantity; }

    public double getMedicineAmount() { return medicineAmount; }
    public void setMedicineAmount(double medicineAmount) { this.medicineAmount = medicineAmount; }

    public LocalDateTime getPaidAt() { return paidAt; }
    public void setPaidAt(LocalDateTime paidAt) { this.paidAt = paidAt; }
}
