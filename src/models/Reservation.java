package models;

import java.time.LocalDateTime;

public class Reservation {
    public enum ReservationStatus {
        PENDING, CONFIRMED, CANCELLED, COMPLETED, EXPIRED
    }

    public enum PaymentMethod {
        ONLINE_BANK, E_PAYMENT, PAY_AT_STORE
    }

    public enum PaymentStatus {
        PAID, UNPAID, REFUNDED
    }

    private int id;
    private int userId;
    private int medicineId;
    private int pharmacyId;
    private int quantity;
    private double totalPrice;
    private LocalDateTime reservationTime;
    private LocalDateTime expirationTime;
    private ReservationStatus status;
    private PaymentMethod paymentMethod;
    private PaymentStatus paymentStatus;
    private String notes;

    public Reservation(int id, int userId, int medicineId, int pharmacyId,
                       int quantity, double totalPrice) {
        this.id = id;
        this.userId = userId;
        this.medicineId = medicineId;
        this.pharmacyId = pharmacyId;
        this.quantity = quantity;
        this.totalPrice = totalPrice;
        this.reservationTime = LocalDateTime.now();
        this.expirationTime = reservationTime.plusHours(24);
        this.status = ReservationStatus.PENDING;
        this.paymentMethod = PaymentMethod.PAY_AT_STORE;
        this.paymentStatus = PaymentStatus.UNPAID;
        this.notes = "";
    }

    // Getters and Setters
    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public int getUserId() { return userId; }
    public void setUserId(int userId) { this.userId = userId; }

    public int getMedicineId() { return medicineId; }
    public void setMedicineId(int medicineId) { this.medicineId = medicineId; }

    public int getPharmacyId() { return pharmacyId; }
    public void setPharmacyId(int pharmacyId) { this.pharmacyId = pharmacyId; }

    public int getQuantity() { return quantity; }
    public void setQuantity(int quantity) { this.quantity = quantity; }

    public double getTotalPrice() { return totalPrice; }
    public void setTotalPrice(double totalPrice) { this.totalPrice = totalPrice; }

    public LocalDateTime getReservationTime() { return reservationTime; }
    public void setReservationTime(LocalDateTime reservationTime) {
        this.reservationTime = reservationTime;
    }

    public LocalDateTime getExpirationTime() { return expirationTime; }
    public void setExpirationTime(LocalDateTime expirationTime) {
        this.expirationTime = expirationTime;
    }

    public ReservationStatus getStatus() { return status; }
    public void setStatus(ReservationStatus status) { this.status = status; }

    public PaymentMethod getPaymentMethod() { return paymentMethod; }
    public void setPaymentMethod(PaymentMethod paymentMethod) {
        this.paymentMethod = paymentMethod;
    }

    public PaymentStatus getPaymentStatus() { return paymentStatus; }
    public void setPaymentStatus(PaymentStatus paymentStatus) {
        this.paymentStatus = paymentStatus;
    }

    public String getNotes() { return notes; }
    public void setNotes(String notes) { this.notes = notes; }

    public boolean isExpired() {
        return LocalDateTime.now().isAfter(expirationTime) &&
               (status == ReservationStatus.PENDING || status == ReservationStatus.CONFIRMED);
    }

    @Override
    public String toString() {
        return "Reservation #" + id + " - Qty: " + quantity + " - Status: " + status;
    }
}
