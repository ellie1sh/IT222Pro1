package client.controllers;

import server.DataStore;
import models.Medicine;
import models.Pharmacy;
import models.Reservation;
import models.User;
import models.Reservation.PaymentMethod;
import models.Reservation.ReservationStatus;
import models.Reservation.PaymentStatus;
import models.PrescriptionRequest;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Client-side controller for pharmacist operations.
 */
public class PharmacistController {
    private final DataStore dataStore;

    public PharmacistController(DataStore dataStore) {
        this.dataStore = dataStore;
    }

    public Object[][] getMedicines(int pharmacyId) {
        return getFilteredMedicines(pharmacyId, "", "All Categories");
    }

    public Object[][] getFilteredMedicines(int pharmacyId, String searchTerm, String categoryFilter) {
        List<Medicine> medicines;
        if (searchTerm != null && !searchTerm.trim().isEmpty()) {
            medicines = dataStore.searchMedicines(pharmacyId, searchTerm.trim());
        } else {
            medicines = dataStore.getMedicinesByPharmacy(pharmacyId);
        }
        if (categoryFilter != null && !"All Categories".equals(categoryFilter)) {
            String cat = categoryFilter;
            medicines = medicines.stream().filter(m -> cat.equals(m.getCategory())).collect(Collectors.toList());
        }
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
                m.getQuantityReserved(),
                m.getCategory()
            };
        }
        return result;
    }

    public String addMedicine(int pharmacyId, String brandName, String genericName,
                              String dosage, String dosageForm, double price,
                              int quantity, String category) {
        Medicine med = dataStore.createMedicine(pharmacyId, brandName, genericName,
            "", dosage, dosageForm, price, quantity, category);
        if (med != null) {
            return "Medicine added successfully!";
        }
        return "Failed to add medicine. Medicine may already exist.";
    }

    public String updateMedicine(int medicineId, String brandName, String genericName,
                                 String dosage, String dosageForm, double price,
                                 int quantity, String category) {
        boolean success = dataStore.updateMedicine(medicineId, brandName, genericName,
            dosage, dosageForm, price, quantity, category);
        if (success) {
            return "Medicine updated successfully!";
        }
        return "Failed to update medicine.";
    }

    public String deleteMedicine(int medicineId, int pharmacyId) {
        boolean success = dataStore.deleteMedicine(medicineId, pharmacyId);
        if (success) {
            return "Medicine deleted successfully!";
        }
        return "Failed to delete medicine.";
    }

    public Object[][] getReservations(int pharmacyId) {
        List<Reservation> reservations = dataStore.getReservationsByPharmacy(pharmacyId);
        Object[][] result = new Object[reservations.size()][];
        for (int i = 0; i < reservations.size(); i++) {
            Reservation r = reservations.get(i);
            User user = dataStore.getUserById(r.getUserId());
            Medicine med = dataStore.getMedicineById(r.getMedicineId());
            String resTime = r.getReservationTime().toString();
            String expTime = (r.getStatus() == ReservationStatus.CANCELLED) ? ""
                : (r.getExpirationTime().toString().length() >= 16 ? r.getExpirationTime().toString().substring(0, 16) : r.getExpirationTime().toString());
            result[i] = new Object[]{
                r.getId(),
                user != null ? user.getFullName() : "Unknown",
                med != null ? med.getBrandName() : "Unknown",
                r.getQuantity(),
                String.format("%.2f", r.getTotalPrice()),
                r.getPaymentMethod().name().replace("_", " "),
                r.getPaymentStatus().name(),
                resTime.length() >= 16 ? resTime.substring(0, 16) : resTime,
                expTime,
                r.getStatus().name()
            };
        }
        return result;
    }

    public List<Reservation> getReservationsList(int pharmacyId) {
        return dataStore.getReservationsByPharmacy(pharmacyId);
    }

    public String approveReservation(int reservationId) {
        boolean success = dataStore.approveReservation(reservationId);
        if (success) {
            return "Reservation approved successfully!";
        }
        return "Failed to approve reservation.";
    }

    public String rejectReservation(int reservationId) {
        boolean success = dataStore.rejectReservation(reservationId);
        if (success) {
            return "Reservation rejected successfully!";
        }
        return "Failed to reject reservation.";
    }

    public String completeReservation(int reservationId) {
        boolean success = dataStore.completeReservation(reservationId);
        if (success) {
            return "Reservation completed successfully!";
        }
        return "Failed to complete reservation.";
    }

    public String markPendingOrderAsPaid(int reservationId) {
        boolean success = dataStore.markPendingOrderAsPaid(reservationId);
        if (success) {
            return "Order marked as paid and moved to history!";
        }
        return "Failed to mark order as paid.";
    }

    public List<Reservation> getPendingUnpaidNoPrescriptionList(int pharmacyId) {
        List<Reservation> reservations = dataStore.getReservationsByPharmacy(pharmacyId);
        return reservations.stream()
            .filter(r -> r.getStatus() == ReservationStatus.PENDING)
            .filter(r -> r.getPaymentStatus() == PaymentStatus.UNPAID)
            .filter(r -> {
                Medicine med = dataStore.getMedicineById(r.getMedicineId());
                return med != null && !med.isRequiresPrescription();
            })
            .collect(Collectors.toList());
    }

    public List<Reservation> getPendingPrescriptionRequiredList(int pharmacyId) {
        List<Reservation> reservations = dataStore.getReservationsByPharmacy(pharmacyId);
        return reservations.stream()
            .filter(r -> r.getStatus() == ReservationStatus.PENDING)
            .filter(r -> r.getPaymentStatus() == PaymentStatus.UNPAID)
            .filter(r -> {
                Medicine med = dataStore.getMedicineById(r.getMedicineId());
                return med != null && med.isRequiresPrescription();
            })
            .collect(Collectors.toList());
    }

    public List<PrescriptionRequest> getPendingPrescriptionRequests(int pharmacyId) {
        return dataStore.getPendingPrescriptionRequests(pharmacyId);
    }

    public List<PrescriptionRequest> getChosenPrescriptionRequestsForPharmacy(int pharmacyId) {
        return dataStore.getChosenPrescriptionRequestsForPharmacy(pharmacyId);
    }

    public List<PrescriptionRequest> getPrescriptionRequestsForHistory(int pharmacyId) {
        return dataStore.getPrescriptionRequestsForHistory(pharmacyId);
    }

    /** Decline a prescription request for this pharmacy. Returns true if declined. */
    public boolean declinePrescriptionRequest(int requestId, int pharmacyId) {
        return dataStore.declinePrescriptionRequest(requestId, pharmacyId);
    }

    /** Confirm stock for a prescription request. Returns success message. */
    public String confirmPrescriptionRequest(int requestId, int pharmacyId) {
        boolean ok = dataStore.confirmPrescriptionRequest(requestId, pharmacyId);
        if (ok) {
            Pharmacy pharm = dataStore.getPharmacyById(pharmacyId);
            return "Stock confirmed! Waiting for customer to choose a pharmacy. (" +
                (pharm != null ? pharm.getName() : "pharmacy") + ")";
        }
        return "ERROR|Unable to confirm this request (already chosen or no longer pending).";
    }

    public String markPrescriptionReadyForPickup(int requestId, int pharmacyId) {
        boolean ok = dataStore.markPrescriptionReadyForPickup(requestId, pharmacyId);
        if (ok) {
            return "Prescription marked ready for pickup.";
        }
        return "Unable to mark ready for pickup.";
    }

    public String markPrescriptionPaid(int requestId, int pharmacyId, String medicineName, int quantity, double amount) {
        boolean ok = dataStore.markPrescriptionPaid(requestId, pharmacyId, medicineName, quantity, amount);
        if (ok) {
            return "Prescription marked as paid and added to today's sales.";
        }
        return "Unable to mark as paid.";
    }

    // ==================== DASHBOARD METRICS ====================

    public double getTodaysSales(int pharmacyId) {
        List<Reservation> reservations = dataStore.getReservationsByPharmacy(pharmacyId);
        LocalDate today = LocalDate.now();
        double reservationSales = reservations.stream()
            .filter(r -> {
                LocalDateTime resTime = r.getReservationTime();
                return resTime.toLocalDate().equals(today);
            })
            .filter(r -> {
                // Include completed reservations OR paid reservations (online payments)
                return r.getStatus() == ReservationStatus.COMPLETED ||
                       (r.getPaymentStatus() == PaymentStatus.PAID && 
                        (r.getStatus() == ReservationStatus.PENDING || 
                         r.getStatus() == ReservationStatus.CONFIRMED));
            })
            .mapToDouble(Reservation::getTotalPrice)
            .sum();
        double prescriptionSales = dataStore.getCompletedPrescriptionRequestsForPharmacy(pharmacyId).stream()
            .filter(pr -> pr.getPaidAt() != null && pr.getPaidAt().toLocalDate().equals(today))
            .mapToDouble(models.PrescriptionRequest::getMedicineAmount)
            .sum();
        return reservationSales + prescriptionSales;
    }

    public int getPendingOrders(int pharmacyId) {
        List<Reservation> reservations = dataStore.getReservationsByPharmacy(pharmacyId);
        int pendingReservations = (int) reservations.stream()
            .filter(r -> r.getStatus() == ReservationStatus.PENDING)
            .count();
        int pendingPrescriptionRequests = dataStore.getPendingPrescriptionRequests(pharmacyId).size();
        int chosenPrescriptionRequests = dataStore.getChosenPrescriptionRequestsForPharmacy(pharmacyId).size();
        return pendingReservations + pendingPrescriptionRequests + chosenPrescriptionRequests;
    }

    public int getReadyForPickup(int pharmacyId) {
        List<Reservation> reservations = dataStore.getReservationsByPharmacy(pharmacyId);
        return (int) reservations.stream()
            .filter(r -> r.getStatus() == ReservationStatus.CONFIRMED)
            .count();
    }

    public Object[][] getTodaysTransactions(int pharmacyId) {
        List<Reservation> reservations = dataStore.getReservationsByPharmacy(pharmacyId);
        LocalDate today = LocalDate.now();
        List<Reservation> todaysSales = reservations.stream()
            .filter(r -> {
                LocalDateTime resTime = r.getReservationTime();
                return resTime.toLocalDate().equals(today);
            })
            .filter(r -> {
                // Include completed reservations OR paid reservations (online payments)
                return r.getStatus() == ReservationStatus.COMPLETED ||
                       (r.getPaymentStatus() == PaymentStatus.PAID && 
                        (r.getStatus() == ReservationStatus.PENDING || 
                         r.getStatus() == ReservationStatus.CONFIRMED));
            })
            .collect(Collectors.toList());
        
        java.util.List<Object[]> rows = new java.util.ArrayList<>();
        for (Reservation r : todaysSales) {
            User user = dataStore.getUserById(r.getUserId());
            Medicine med = dataStore.getMedicineById(r.getMedicineId());
            String resTime = r.getReservationTime().toString();
            String refNum = r.getReferenceNumber() != null && !r.getReferenceNumber().isEmpty() 
                ? r.getReferenceNumber() : "REF" + String.format("%06d", r.getId());
            rows.add(new Object[]{
                refNum,
                user != null ? user.getFullName() : "Unknown",
                med != null ? med.getBrandName() : "Unknown",
                r.getQuantity(),
                String.format("%.2f", r.getTotalPrice()),
                r.getPaymentMethod().name().replace("_", " "),
                r.getPaymentStatus().name(),
                resTime.length() >= 16 ? resTime.substring(0, 16) : resTime
            });
        }
        dataStore.getCompletedPrescriptionRequestsForPharmacy(pharmacyId).stream()
            .filter(pr -> pr.getPaidAt() != null && pr.getPaidAt().toLocalDate().equals(today))
            .forEach(pr -> {
                User user = dataStore.getUserById(pr.getUserId());
                String time = pr.getPaidAt() != null ? pr.getPaidAt().toString() : "";
                String refNum = pr.getReferenceNumber() != null ? pr.getReferenceNumber() : ("RX" + String.format("%06d", pr.getId()));
                rows.add(new Object[]{
                    refNum,
                    user != null ? user.getFullName() : "Unknown",
                    pr.getMedicineName() != null && !pr.getMedicineName().isEmpty() ? pr.getMedicineName() : "Prescription",
                    pr.getMedicineQuantity() > 0 ? pr.getMedicineQuantity() : 1,
                    String.format("%.2f", pr.getMedicineAmount()),
                    "PRESCRIPTION",
                    "PAID",
                    time.length() >= 16 ? time.substring(0, 16) : time
                });
            });
        Object[][] result = new Object[rows.size()][];
        for (int i = 0; i < rows.size(); i++) result[i] = rows.get(i);
        return result;
    }

    public Object[][] getPendingUnpaidOrders(int pharmacyId) {
        List<Reservation> reservations = dataStore.getReservationsByPharmacy(pharmacyId);
        List<Reservation> pendingUnpaid = reservations.stream()
            .filter(r -> r.getStatus() == ReservationStatus.PENDING)
            .filter(r -> r.getPaymentStatus() == PaymentStatus.UNPAID)
            .collect(Collectors.toList());
        
        Object[][] result = new Object[pendingUnpaid.size()][];
        for (int i = 0; i < pendingUnpaid.size(); i++) {
            Reservation r = pendingUnpaid.get(i);
            User user = dataStore.getUserById(r.getUserId());
            Medicine med = dataStore.getMedicineById(r.getMedicineId());
            String resTime = r.getReservationTime().toString();
            String refNum = r.getReferenceNumber() != null && !r.getReferenceNumber().isEmpty() 
                ? r.getReferenceNumber() : "REF" + String.format("%06d", r.getId());
            result[i] = new Object[]{
                refNum,
                user != null ? user.getFullName() : "Unknown",
                med != null ? med.getBrandName() : "Unknown",
                r.getQuantity(),
                String.format("%.2f", r.getTotalPrice()),
                r.getPaymentMethod().name().replace("_", " "),
                resTime.length() >= 16 ? resTime.substring(0, 16) : resTime
            };
        }
        return result;
    }

    public Object[][] getPendingUnpaidNoPrescription(int pharmacyId) {
        List<Reservation> reservations = dataStore.getReservationsByPharmacy(pharmacyId);
        List<Reservation> pendingUnpaid = reservations.stream()
            .filter(r -> r.getStatus() == ReservationStatus.PENDING)
            .filter(r -> r.getPaymentStatus() == PaymentStatus.UNPAID)
            .filter(r -> {
                Medicine med = dataStore.getMedicineById(r.getMedicineId());
                return med != null && !med.isRequiresPrescription();
            })
            .collect(Collectors.toList());
        
        Object[][] result = new Object[pendingUnpaid.size()][];
        for (int i = 0; i < pendingUnpaid.size(); i++) {
            Reservation r = pendingUnpaid.get(i);
            User user = dataStore.getUserById(r.getUserId());
            Medicine med = dataStore.getMedicineById(r.getMedicineId());
            String resTime = r.getReservationTime().toString();
            String refNum = r.getReferenceNumber() != null && !r.getReferenceNumber().isEmpty() 
                ? r.getReferenceNumber() : "REF" + String.format("%06d", r.getId());
            result[i] = new Object[]{
                refNum,
                user != null ? user.getFullName() : "Unknown",
                med != null ? med.getBrandName() : "Unknown",
                r.getQuantity(),
                String.format("%.2f", r.getTotalPrice()),
                r.getPaymentMethod().name().replace("_", " "),
                resTime.length() >= 16 ? resTime.substring(0, 16) : resTime
            };
        }
        return result;
    }

    public Object[][] getPendingPrescriptionRequired(int pharmacyId) {
        List<Reservation> reservations = dataStore.getReservationsByPharmacy(pharmacyId);
        List<Reservation> pendingUnpaid = reservations.stream()
            .filter(r -> r.getStatus() == ReservationStatus.PENDING)
            .filter(r -> r.getPaymentStatus() == PaymentStatus.UNPAID)
            .filter(r -> {
                Medicine med = dataStore.getMedicineById(r.getMedicineId());
                return med != null && med.isRequiresPrescription();
            })
            .collect(Collectors.toList());
        
        Object[][] result = new Object[pendingUnpaid.size()][];
        for (int i = 0; i < pendingUnpaid.size(); i++) {
            Reservation r = pendingUnpaid.get(i);
            User user = dataStore.getUserById(r.getUserId());
            Medicine med = dataStore.getMedicineById(r.getMedicineId());
            String resTime = r.getReservationTime().toString();
            result[i] = new Object[]{
                r.getId(),
                user != null ? user.getFullName() : "Unknown",
                med != null ? med.getBrandName() : "Unknown",
                r.getQuantity(),
                String.format("%.2f", r.getTotalPrice()),
                r.getPaymentMethod().name().replace("_", " "),
                resTime.length() >= 16 ? resTime.substring(0, 16) : resTime
            };
        }
        return result;
    }

    public Object[][] getReadyForPickupOrders(int pharmacyId) {
        List<Reservation> reservations = dataStore.getReservationsByPharmacy(pharmacyId);
        List<Reservation> readyOrders = reservations.stream()
            .filter(r -> r.getStatus() == ReservationStatus.CONFIRMED)
            .filter(r -> r.getPaymentStatus() == PaymentStatus.PAID)
            .collect(Collectors.toList());
        
        Object[][] result = new Object[readyOrders.size()][];
        for (int i = 0; i < readyOrders.size(); i++) {
            Reservation r = readyOrders.get(i);
            User user = dataStore.getUserById(r.getUserId());
            Medicine med = dataStore.getMedicineById(r.getMedicineId());
            String resTime = r.getReservationTime().toString();
            String refNum = r.getReferenceNumber() != null && !r.getReferenceNumber().isEmpty() 
                ? r.getReferenceNumber() : "REF" + String.format("%06d", r.getId());
            result[i] = new Object[]{
                refNum,
                user != null ? user.getFullName() : "Unknown",
                med != null ? med.getBrandName() : "Unknown",
                r.getQuantity(),
                String.format("%.2f", r.getTotalPrice()),
                r.getPaymentMethod().name().replace("_", " "),
                resTime.length() >= 16 ? resTime.substring(0, 16) : resTime
            };
        }
        return result;
    }

    public List<Reservation> getReadyForPickupOrdersList(int pharmacyId) {
        List<Reservation> reservations = dataStore.getReservationsByPharmacy(pharmacyId);
        return reservations.stream()
            .filter(r -> r.getStatus() == ReservationStatus.CONFIRMED)
            .filter(r -> r.getPaymentStatus() == PaymentStatus.PAID)
            .collect(Collectors.toList());
    }
}
