package models;

public class Medicine {
    public enum MedicineStatus {
        AVAILABLE, RESERVED, OUT_OF_STOCK
    }

    private int id;
    private int pharmacyId;
    private String brandName;
    private String genericName;
    private String description;
    private String dosage;
    private String dosageForm;
    private double price;
    private int quantityAvailable;
    private int quantityReserved;
    private String category;
    private boolean requiresPrescription;
    private MedicineStatus status;

    public Medicine(int id, int pharmacyId, String brandName, String genericName,
                    String description, String dosage, String dosageForm,
                    double price, int quantityAvailable, String category,
                    boolean requiresPrescription) {
        this.id = id;
        this.pharmacyId = pharmacyId;
        this.brandName = brandName;
        this.genericName = genericName;
        this.description = description;
        this.dosage = dosage;
        this.dosageForm = dosageForm;
        this.price = price;
        this.quantityAvailable = quantityAvailable;
        this.quantityReserved = 0;
        this.category = category;
        this.requiresPrescription = requiresPrescription;
        this.status = quantityAvailable > 0 ? MedicineStatus.AVAILABLE : MedicineStatus.OUT_OF_STOCK;
    }

    // Getters and Setters
    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public int getPharmacyId() { return pharmacyId; }
    public void setPharmacyId(int pharmacyId) { this.pharmacyId = pharmacyId; }

    public String getBrandName() { return brandName; }
    public void setBrandName(String brandName) { this.brandName = brandName; }

    public String getGenericName() { return genericName; }
    public void setGenericName(String genericName) { this.genericName = genericName; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public String getDosage() { return dosage; }
    public void setDosage(String dosage) { this.dosage = dosage; }

    public String getDosageForm() { return dosageForm; }
    public void setDosageForm(String dosageForm) { this.dosageForm = dosageForm; }

    public double getPrice() { return price; }
    public void setPrice(double price) { this.price = price; }

    public int getQuantityAvailable() { return quantityAvailable; }
    public void setQuantityAvailable(int quantityAvailable) {
        this.quantityAvailable = quantityAvailable;
        updateStatus();
    }

    public int getQuantityReserved() { return quantityReserved; }
    public void setQuantityReserved(int quantityReserved) {
        this.quantityReserved = quantityReserved;
        updateStatus();
    }

    public String getCategory() { return category; }
    public void setCategory(String category) {
        this.category = category;
        this.requiresPrescription = "Prescription".equalsIgnoreCase(category);
    }

    public boolean isRequiresPrescription() { return requiresPrescription; }
    public void setRequiresPrescription(boolean requiresPrescription) {
        this.requiresPrescription = requiresPrescription;
    }

    public MedicineStatus getStatus() { return status; }
    public void setStatus(MedicineStatus status) { this.status = status; }

    private void updateStatus() {
        int effective = quantityAvailable - quantityReserved;
        if (effective <= 0) {
            this.status = MedicineStatus.OUT_OF_STOCK;
        } else if (quantityReserved > 0) {
            this.status = MedicineStatus.RESERVED;
        } else {
            this.status = MedicineStatus.AVAILABLE;
        }
    }

    public int getEffectiveQuantity() {
        return Math.max(0, quantityAvailable - quantityReserved);
    }

    @Override
    public String toString() {
        return brandName + " (" + genericName + ") - " + dosage;
    }
}
