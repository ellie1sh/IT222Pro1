package client.controllers;

import server.DataStore;
import models.Pharmacy;
import models.Medicine;
import models.Reservation;
import models.User;
import models.Reservation.PaymentMethod;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Client-side controller for resident/user operations.
 */
public class UserController {
    private final DataStore dataStore;

    public UserController(DataStore dataStore) {
        this.dataStore = dataStore;
    }

    public Object[][] getApprovedPharmacies() {
        List<Pharmacy> pharmacies = dataStore.getApprovedPharmacies();
        Object[][] result = new Object[pharmacies.size()][];
        for (int i = 0; i < pharmacies.size(); i++) {
            Pharmacy p = pharmacies.get(i);
            result[i] = new Object[]{
                p.getId(),
                p.getName(),
                p.getAddress(),
                p.getContactNumber()
            };
        }
        return result;
    }

    public Pharmacy getPharmacyById(int pharmacyId) {
        return dataStore.getPharmacyById(pharmacyId);
    }

    public String getUserAddress(int userId) {
        User user = dataStore.getUserById(userId);
        return user != null ? user.getAddress() : "";
    }

    public List<models.PrescriptionRequest> getPendingPrescriptionRequestsByUser(int userId) {
        return dataStore.getPendingPrescriptionRequestsByUser(userId);
    }

    public List<models.PrescriptionRequest> getChosenPrescriptionRequestsByUser(int userId) {
        return dataStore.getChosenPrescriptionRequestsByUser(userId);
    }

    public List<models.PrescriptionRequest> getPrescriptionRequestsForHistoryByUser(int userId) {
        return dataStore.getPrescriptionRequestsForHistoryByUser(userId);
    }

    public String selectPrescriptionPharmacy(int requestId, int pharmacyId) {
        boolean ok = dataStore.selectPrescriptionPharmacy(requestId, pharmacyId);
        if (ok) {
            Pharmacy pharm = dataStore.getPharmacyById(pharmacyId);
            return "You selected " + (pharm != null ? pharm.getName() : "the pharmacy") + " for pickup.";
        }
        return "Unable to select that pharmacy. Please try again.";
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
                m.getDosage() + " " + m.getDosageForm(),
                String.format("%.2f", m.getPrice()),
                m.getEffectiveQuantity(),
                m.getCategory()
            };
        }
        return result;
    }

    /**
     * Search medicines across all pharmacies (pharmacyId = -1) or within a specific pharmacy.
     * Returns: ID, Brand, Generic, Dosage, Price, Available, Category, Pharmacy, Address
     */
    public Object[][] searchMedicines(int pharmacyId, String searchTerm) {
        List<Medicine> medicines;
        if (pharmacyId == -1) {
            // Search across all approved pharmacies
            String lower = searchTerm.toLowerCase();
            medicines = dataStore.getAllMedicines().stream()
                .filter(m -> {
                    Pharmacy pharm = dataStore.getPharmacyById(m.getPharmacyId());
                    return pharm != null && pharm.getStatus() == models.Pharmacy.PharmacyStatus.APPROVED;
                })
                .filter(m -> m.getBrandName().toLowerCase().contains(lower) ||
                            m.getGenericName().toLowerCase().contains(lower) ||
                            m.getDosage().toLowerCase().contains(lower) ||
                            m.getCategory().toLowerCase().contains(lower))
                .collect(Collectors.toList());
        } else {
            medicines = dataStore.searchMedicines(pharmacyId, searchTerm);
        }
        Object[][] result = new Object[medicines.size()][];
        for (int i = 0; i < medicines.size(); i++) {
            Medicine m = medicines.get(i);
            Pharmacy pharm = dataStore.getPharmacyById(m.getPharmacyId());
            result[i] = new Object[]{
                m.getId(),
                pharm != null ? pharm.getName() : "Unknown",
                m.getBrandName(),
                m.getGenericName(),
                m.getDosage() + " " + m.getDosageForm(),
                String.format("%.2f", m.getPrice()),
                m.getEffectiveQuantity(),
                m.getCategory(),
                pharm != null ? pharm.getAddress() : "Unknown"
            };
        }
        return result;
    }

    /**
     * Get prescription-required medicines across all approved pharmacies.
     * Returns same format as searchMedicines: ID, Pharmacy, Brand, Generic, Dosage, Price, Available, Category
     */
    public Object[][] getPrescriptionMedicines(String searchTerm) {
        String lower = (searchTerm == null || searchTerm.isEmpty()) ? "" : searchTerm.toLowerCase();
        List<Medicine> medicines = dataStore.getAllMedicines().stream()
            .filter(m -> {
                Pharmacy pharm = dataStore.getPharmacyById(m.getPharmacyId());
                return pharm != null && pharm.getStatus() == models.Pharmacy.PharmacyStatus.APPROVED
                    && m.isRequiresPrescription();
            })
            .filter(m -> lower.isEmpty() ||
                m.getBrandName().toLowerCase().contains(lower) ||
                m.getGenericName().toLowerCase().contains(lower) ||
                m.getDosage().toLowerCase().contains(lower) ||
                m.getCategory().toLowerCase().contains(lower))
            .collect(Collectors.toList());
        Object[][] result = new Object[medicines.size()][];
        for (int i = 0; i < medicines.size(); i++) {
            Medicine m = medicines.get(i);
            Pharmacy pharm = dataStore.getPharmacyById(m.getPharmacyId());
            result[i] = new Object[]{
                m.getId(),
                pharm != null ? pharm.getName() : "Unknown",
                m.getBrandName(),
                m.getGenericName(),
                m.getDosage() + " " + m.getDosageForm(),
                String.format("%.2f", m.getPrice()),
                m.getEffectiveQuantity(),
                m.getCategory(),
                pharm != null ? pharm.getAddress() : "Unknown"
            };
        }
        return result;
    }

    /**
     * Reserve a medicine. Returns message string.
     * paymentMethod: ONLINE_BANK, E_PAYMENT, PAY_AT_STORE
     */
    public String reserveMedicine(int userId, int medicineId, int quantity, String paymentMethod) {
        PaymentMethod pm;
        try {
            pm = PaymentMethod.valueOf(paymentMethod);
        } catch (IllegalArgumentException e) {
            return "ERROR|Invalid payment method!";
        }
        Reservation res = dataStore.reserveMedicine(userId, medicineId, quantity, pm);
        if (res != null) {
            String refNum = res.getReferenceNumber() != null && !res.getReferenceNumber().isEmpty()
                ? res.getReferenceNumber() : "REF" + String.format("%06d", res.getId());
            return "SUCCESS|Reference: " + refNum + "\nTotal: PHP " +
                String.format("%.2f", res.getTotalPrice()) + "\nStatus: " +
                res.getStatus().name() + "\n" + res.getNotes();
        }
        return "ERROR|Failed to reserve medicine. May be out of stock.";
    }

    public String cancelReservation(int reservationId, int userId) {
        boolean success = dataStore.cancelReservation(reservationId, userId);
        if (success) {
            return "Reservation cancelled successfully!";
        }
        return "Failed to cancel reservation.";
    }

    public String cancelPrescriptionRequest(int requestId, int userId) {
        boolean success = dataStore.cancelPrescriptionRequest(requestId, userId);
        if (success) {
            return "Prescription request cancelled successfully!";
        }
        return "Failed to cancel prescription request.";
    }

    /** Returns: ID, RefNum, Medicine, Pharmacy, Qty, Total, Payment, PayStatus, ReservedAt, ExpiresAt, Status */
    public Object[][] getUserReservations(int userId) {
        List<Reservation> reservations = dataStore.getReservationsByUser(userId);
        Object[][] result = new Object[reservations.size()][];
        for (int i = 0; i < reservations.size(); i++) {
            Reservation r = reservations.get(i);
            Medicine med = dataStore.getMedicineById(r.getMedicineId());
            Pharmacy pharm = dataStore.getPharmacyById(r.getPharmacyId());
            String resTime = r.getReservationTime().toString();
            String expTime = r.getExpirationTime().toString();
            String refNum = r.getReferenceNumber() != null && !r.getReferenceNumber().isEmpty() 
                ? r.getReferenceNumber() : "REF" + String.format("%06d", r.getId());
            result[i] = new Object[]{
                r.getId(),
                refNum,
                med != null ? med.getBrandName() : "Unknown",
                pharm != null ? pharm.getName() : "Unknown",
                r.getQuantity(),
                String.format("%.2f", r.getTotalPrice()),
                r.getPaymentMethod().name().replace("_", " "),
                r.getPaymentStatus().name(),
                resTime.length() >= 16 ? resTime.substring(0, 16) : resTime,
                expTime.length() >= 16 ? expTime.substring(0, 16) : expTime,
                r.getStatus().name()
            };
        }
        return result;
    }

    public List<Reservation> getUserReservationsList(int userId) {
        return dataStore.getReservationsByUser(userId);
    }

    /**
     * Submit a prescription photo. Sends to all pharmacies' Prescription Required tab.
     * Returns success message or error.
     */
    public String submitPrescription(int userId, File imageFile) {
        if (imageFile == null || !imageFile.exists() || !imageFile.canRead()) {
            return "ERROR|Please select a valid image file.";
        }
        String ext = "";
        String name = imageFile.getName();
        int dot = name.lastIndexOf('.');
        if (dot > 0) ext = name.substring(dot);
        String destPath = DataStore.getPrescriptionsDir() + File.separator + "rx_" + System.currentTimeMillis() + ext;
        try {
            Files.copy(imageFile.toPath(), new File(destPath).toPath(), StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException e) {
            return "ERROR|Failed to save prescription image.";
        }
        int id = dataStore.createPrescriptionRequest(userId, destPath);
        if (id > 0) {
            return "SUCCESS|Prescription sent to all pharmacies. One pharmacy will accept and contact you.";
        }
        return "ERROR|Failed to submit prescription.";
    }
}
