package server;

import models.*;
import models.User.UserType;
import models.Pharmacy.PharmacyStatus;
import models.Reservation.ReservationStatus;
import models.Reservation.PaymentMethod;
import models.Reservation.PaymentStatus;

import javax.xml.parsers.*;
import javax.xml.transform.*;
import javax.xml.transform.dom.*;
import javax.xml.transform.stream.*;
import org.w3c.dom.*;
import java.io.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.stream.Collectors;

/**
 * Server-side data store using XML files for persistence.
 * Thread-safe operations with read/write locks for concurrency.
 */
public class DataStore {
    private static DataStore instance;

    private static final String DATA_DIR = "data";
    private static final String USERS_FILE = DATA_DIR + "/users.xml";
    private static final String PHARMACIES_FILE = DATA_DIR + "/pharmacies.xml";
    private static final String MEDICINES_FILE = DATA_DIR + "/medicines.xml";
    private static final String RESERVATIONS_FILE = DATA_DIR + "/reservations.xml";

    private final ConcurrentHashMap<Integer, User> users;
    private final ConcurrentHashMap<Integer, Pharmacy> pharmacies;
    private final ConcurrentHashMap<Integer, Medicine> medicines;
    private final ConcurrentHashMap<Integer, Reservation> reservations;

    private final ReentrantReadWriteLock userLock;
    private final ReentrantReadWriteLock pharmacyLock;
    private final ReentrantReadWriteLock medicineLock;
    private final ReentrantReadWriteLock reservationLock;

    private int nextUserId;
    private int nextPharmacyId;
    private int nextMedicineId;
    private int nextReservationId;

    private final DateTimeFormatter dateFormatter = DateTimeFormatter.ISO_LOCAL_DATE_TIME;

    private DataStore() {
        users = new ConcurrentHashMap<>();
        pharmacies = new ConcurrentHashMap<>();
        medicines = new ConcurrentHashMap<>();
        reservations = new ConcurrentHashMap<>();

        userLock = new ReentrantReadWriteLock();
        pharmacyLock = new ReentrantReadWriteLock();
        medicineLock = new ReentrantReadWriteLock();
        reservationLock = new ReentrantReadWriteLock();

        nextUserId = 1;
        nextPharmacyId = 1;
        nextMedicineId = 1;
        nextReservationId = 1;

        new File(DATA_DIR).mkdirs();

        if (!loadAllData()) {
            initializeSampleData();
            saveAllData();
        }

        checkExpiredReservations();
    }

    public static synchronized DataStore getInstance() {
        if (instance == null) {
            instance = new DataStore();
        }
        return instance;
    }

    // ==================== EXPIRY CHECK ====================

    public void checkExpiredReservations() {
        reservationLock.writeLock().lock();
        medicineLock.writeLock().lock();
        try {
            LocalDateTime now = LocalDateTime.now();
            for (Reservation res : reservations.values()) {
                if (now.isAfter(res.getExpirationTime()) &&
                    (res.getStatus() == ReservationStatus.PENDING ||
                     res.getStatus() == ReservationStatus.CONFIRMED)) {

                    res.setStatus(ReservationStatus.EXPIRED);
                    if (res.getPaymentStatus() == PaymentStatus.PAID) {
                        res.setPaymentStatus(PaymentStatus.REFUNDED);
                        res.setNotes("Auto-expired: not picked up within 24 hours. Payment refunded.");
                    } else {
                        res.setNotes("Auto-expired: not picked up within 24 hours.");
                    }

                    Medicine med = medicines.get(res.getMedicineId());
                    if (med != null) {
                        med.setQuantityReserved(Math.max(0, med.getQuantityReserved() - res.getQuantity()));
                    }
                }
            }
            saveReservations();
            saveMedicines();
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            medicineLock.writeLock().unlock();
            reservationLock.writeLock().unlock();
        }
    }

    // ==================== XML LOADING ====================

    private boolean loadAllData() {
        File usersFile = new File(USERS_FILE);
        if (!usersFile.exists()) return false;
        try {
            loadUsers();
            loadPharmacies();
            loadMedicines();
            loadReservations();
            return true;
        } catch (Exception e) {
            System.err.println("Error loading XML data: " + e.getMessage());
            return false;
        }
    }

    private void loadUsers() throws Exception {
        File file = new File(USERS_FILE);
        if (!file.exists()) return;
        DocumentBuilder builder = DocumentBuilderFactory.newInstance().newDocumentBuilder();
        Document doc = builder.parse(file);
        NodeList nodeList = doc.getElementsByTagName("user");
        for (int i = 0; i < nodeList.getLength(); i++) {
            Element elem = (Element) nodeList.item(i);
            int id = Integer.parseInt(getText(elem, "id"));
            User user = new User(id, getText(elem, "username"), getText(elem, "password"),
                getText(elem, "fullName"), getText(elem, "email"),
                UserType.valueOf(getText(elem, "userType")));
            user.setPharmacyId(Integer.parseInt(getText(elem, "pharmacyId")));
            user.setActive(Boolean.parseBoolean(getText(elem, "isActive")));
            users.put(id, user);
            if (id >= nextUserId) nextUserId = id + 1;
        }
    }

    private void loadPharmacies() throws Exception {
        File file = new File(PHARMACIES_FILE);
        if (!file.exists()) return;
        DocumentBuilder builder = DocumentBuilderFactory.newInstance().newDocumentBuilder();
        Document doc = builder.parse(file);
        NodeList nodeList = doc.getElementsByTagName("pharmacy");
        for (int i = 0; i < nodeList.getLength(); i++) {
            Element elem = (Element) nodeList.item(i);
            int id = Integer.parseInt(getText(elem, "id"));
            Pharmacy pharmacy = new Pharmacy(id, getText(elem, "name"), getText(elem, "address"),
                getText(elem, "contactNumber"), getText(elem, "email"), getText(elem, "description"));
            pharmacy.setStatus(PharmacyStatus.valueOf(getText(elem, "status")));
            pharmacies.put(id, pharmacy);
            if (id >= nextPharmacyId) nextPharmacyId = id + 1;
        }
    }

    private void loadMedicines() throws Exception {
        File file = new File(MEDICINES_FILE);
        if (!file.exists()) return;
        DocumentBuilder builder = DocumentBuilderFactory.newInstance().newDocumentBuilder();
        Document doc = builder.parse(file);
        NodeList nodeList = doc.getElementsByTagName("medicine");
        for (int i = 0; i < nodeList.getLength(); i++) {
            Element elem = (Element) nodeList.item(i);
            int id = Integer.parseInt(getText(elem, "id"));
            Medicine med = new Medicine(id, Integer.parseInt(getText(elem, "pharmacyId")),
                getText(elem, "brandName"), getText(elem, "genericName"),
                getText(elem, "description"), getText(elem, "dosage"), getText(elem, "dosageForm"),
                Double.parseDouble(getText(elem, "price")),
                Integer.parseInt(getText(elem, "quantityAvailable")),
                getText(elem, "category"),
                Boolean.parseBoolean(getText(elem, "requiresPrescription")));
            med.setQuantityReserved(Integer.parseInt(getText(elem, "quantityReserved")));
            medicines.put(id, med);
            if (id >= nextMedicineId) nextMedicineId = id + 1;
        }
    }

    private void loadReservations() throws Exception {
        File file = new File(RESERVATIONS_FILE);
        if (!file.exists()) return;
        DocumentBuilder builder = DocumentBuilderFactory.newInstance().newDocumentBuilder();
        Document doc = builder.parse(file);
        NodeList nodeList = doc.getElementsByTagName("reservation");
        for (int i = 0; i < nodeList.getLength(); i++) {
            Element elem = (Element) nodeList.item(i);
            int id = Integer.parseInt(getText(elem, "id"));
            Reservation res = new Reservation(id, Integer.parseInt(getText(elem, "userId")),
                Integer.parseInt(getText(elem, "medicineId")),
                Integer.parseInt(getText(elem, "pharmacyId")),
                Integer.parseInt(getText(elem, "quantity")),
                Double.parseDouble(getText(elem, "totalPrice")));
            res.setReservationTime(LocalDateTime.parse(getText(elem, "reservationTime"), dateFormatter));
            res.setExpirationTime(LocalDateTime.parse(getText(elem, "expirationTime"), dateFormatter));
            res.setStatus(ReservationStatus.valueOf(getText(elem, "status")));
            String pm = getText(elem, "paymentMethod");
            if (!pm.isEmpty()) res.setPaymentMethod(PaymentMethod.valueOf(pm));
            String ps = getText(elem, "paymentStatus");
            if (!ps.isEmpty()) res.setPaymentStatus(PaymentStatus.valueOf(ps));
            res.setNotes(getText(elem, "notes"));
            reservations.put(id, res);
            if (id >= nextReservationId) nextReservationId = id + 1;
        }
    }

    private String getText(Element parent, String tagName) {
        NodeList list = parent.getElementsByTagName(tagName);
        if (list.getLength() > 0) return list.item(0).getTextContent();
        return "";
    }

    // ==================== XML SAVING ====================

    private void saveAllData() {
        try {
            saveUsers();
            savePharmacies();
            saveMedicines();
            saveReservations();
        } catch (Exception e) {
            System.err.println("Error saving XML data: " + e.getMessage());
        }
    }

    private void saveUsers() throws Exception {
        DocumentBuilder builder = DocumentBuilderFactory.newInstance().newDocumentBuilder();
        Document doc = builder.newDocument();
        Element root = doc.createElement("users");
        doc.appendChild(root);
        for (User u : users.values()) {
            Element e = doc.createElement("user");
            addChild(doc, e, "id", String.valueOf(u.getId()));
            addChild(doc, e, "username", u.getUsername());
            addChild(doc, e, "password", u.getPassword());
            addChild(doc, e, "fullName", u.getFullName());
            addChild(doc, e, "email", u.getEmail());
            addChild(doc, e, "userType", u.getUserType().name());
            addChild(doc, e, "pharmacyId", String.valueOf(u.getPharmacyId()));
            addChild(doc, e, "isActive", String.valueOf(u.isActive()));
            root.appendChild(e);
        }
        writeXml(doc, USERS_FILE);
    }

    private void savePharmacies() throws Exception {
        DocumentBuilder builder = DocumentBuilderFactory.newInstance().newDocumentBuilder();
        Document doc = builder.newDocument();
        Element root = doc.createElement("pharmacies");
        doc.appendChild(root);
        for (Pharmacy p : pharmacies.values()) {
            Element e = doc.createElement("pharmacy");
            addChild(doc, e, "id", String.valueOf(p.getId()));
            addChild(doc, e, "name", p.getName());
            addChild(doc, e, "address", p.getAddress());
            addChild(doc, e, "contactNumber", p.getContactNumber());
            addChild(doc, e, "email", p.getEmail());
            addChild(doc, e, "description", p.getDescription());
            addChild(doc, e, "status", p.getStatus().name());
            root.appendChild(e);
        }
        writeXml(doc, PHARMACIES_FILE);
    }

    private void saveMedicines() throws Exception {
        DocumentBuilder builder = DocumentBuilderFactory.newInstance().newDocumentBuilder();
        Document doc = builder.newDocument();
        Element root = doc.createElement("medicines");
        doc.appendChild(root);
        for (Medicine m : medicines.values()) {
            Element e = doc.createElement("medicine");
            addChild(doc, e, "id", String.valueOf(m.getId()));
            addChild(doc, e, "pharmacyId", String.valueOf(m.getPharmacyId()));
            addChild(doc, e, "brandName", m.getBrandName());
            addChild(doc, e, "genericName", m.getGenericName());
            addChild(doc, e, "description", m.getDescription());
            addChild(doc, e, "dosage", m.getDosage());
            addChild(doc, e, "dosageForm", m.getDosageForm());
            addChild(doc, e, "price", String.valueOf(m.getPrice()));
            addChild(doc, e, "quantityAvailable", String.valueOf(m.getQuantityAvailable()));
            addChild(doc, e, "quantityReserved", String.valueOf(m.getQuantityReserved()));
            addChild(doc, e, "category", m.getCategory());
            addChild(doc, e, "requiresPrescription", String.valueOf(m.isRequiresPrescription()));
            root.appendChild(e);
        }
        writeXml(doc, MEDICINES_FILE);
    }

    private void saveReservations() throws Exception {
        DocumentBuilder builder = DocumentBuilderFactory.newInstance().newDocumentBuilder();
        Document doc = builder.newDocument();
        Element root = doc.createElement("reservations");
        doc.appendChild(root);
        for (Reservation r : reservations.values()) {
            Element e = doc.createElement("reservation");
            addChild(doc, e, "id", String.valueOf(r.getId()));
            addChild(doc, e, "userId", String.valueOf(r.getUserId()));
            addChild(doc, e, "medicineId", String.valueOf(r.getMedicineId()));
            addChild(doc, e, "pharmacyId", String.valueOf(r.getPharmacyId()));
            addChild(doc, e, "quantity", String.valueOf(r.getQuantity()));
            addChild(doc, e, "totalPrice", String.valueOf(r.getTotalPrice()));
            addChild(doc, e, "reservationTime", r.getReservationTime().format(dateFormatter));
            addChild(doc, e, "expirationTime", r.getExpirationTime().format(dateFormatter));
            addChild(doc, e, "status", r.getStatus().name());
            addChild(doc, e, "paymentMethod", r.getPaymentMethod().name());
            addChild(doc, e, "paymentStatus", r.getPaymentStatus().name());
            addChild(doc, e, "notes", r.getNotes() != null ? r.getNotes() : "");
            root.appendChild(e);
        }
        writeXml(doc, RESERVATIONS_FILE);
    }

    private void addChild(Document doc, Element parent, String name, String value) {
        Element e = doc.createElement(name);
        e.setTextContent(value != null ? value : "");
        parent.appendChild(e);
    }

    private void writeXml(Document doc, String filename) throws Exception {
        Transformer transformer = TransformerFactory.newInstance().newTransformer();
        transformer.setOutputProperty(OutputKeys.INDENT, "yes");
        transformer.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "4");
        transformer.transform(new DOMSource(doc), new StreamResult(new File(filename)));
    }

    // ==================== SAMPLE DATA ====================

    private void initializeSampleData() {
        User admin = new User(nextUserId++, "admin", "admin123", "System Administrator",
            "admin@pharmacy.com", UserType.ADMIN);
        users.put(admin.getId(), admin);

        Pharmacy mercury = new Pharmacy(nextPharmacyId++, "Mercury Drug",
            "123 Main Street, Manila", "09171234567", "mercury@email.com",
            "Your trusted neighborhood pharmacy since 1945");
        mercury.setStatus(PharmacyStatus.APPROVED);
        pharmacies.put(mercury.getId(), mercury);

        Pharmacy watsons = new Pharmacy(nextPharmacyId++, "Watsons Pharmacy",
            "456 Health Ave, Quezon City", "09181234567", "watsons@email.com",
            "Look good, feel great with Watsons");
        watsons.setStatus(PharmacyStatus.APPROVED);
        pharmacies.put(watsons.getId(), watsons);

        Pharmacy southStar = new Pharmacy(nextPharmacyId++, "South Star Drug",
            "789 Wellness Blvd, Makati", "09191234567", "southstar@email.com",
            "Affordable medicines for everyone");
        southStar.setStatus(PharmacyStatus.APPROVED);
        pharmacies.put(southStar.getId(), southStar);

        User mercuryPharmacist = new User(nextUserId++, "mercury_pharm", "pharm123",
            "Juan Dela Cruz", "juan@mercury.com", UserType.PHARMACIST);
        mercuryPharmacist.setPharmacyId(mercury.getId());
        users.put(mercuryPharmacist.getId(), mercuryPharmacist);

        User watsonsPharmacist = new User(nextUserId++, "watsons_pharm", "pharm123",
            "Maria Santos", "maria@watsons.com", UserType.PHARMACIST);
        watsonsPharmacist.setPharmacyId(watsons.getId());
        users.put(watsonsPharmacist.getId(), watsonsPharmacist);

        User resident = new User(nextUserId++, "resident", "user123",
            "Pedro Penduko", "pedro@email.com", UserType.RESIDENT);
        users.put(resident.getId(), resident);

        addMed(mercury.getId(), "Biogesic", "Paracetamol", "For fever and mild pain relief",
            "500mg", "Tablet", 5.50, 100, "OTC Drugs", false);
        addMed(mercury.getId(), "Neozep", "Phenylephrine + Chlorphenamine",
            "For colds and flu symptoms", "500mg", "Tablet", 7.00, 80, "OTC Drugs", false);
        addMed(mercury.getId(), "Amoxicillin", "Amoxicillin",
            "Antibiotic for bacterial infections", "500mg", "Capsule", 15.00, 50, "Prescription", true);

        addMed(watsons.getId(), "Dolfenal", "Mefenamic Acid", "For pain and inflammation",
            "500mg", "Capsule", 12.00, 60, "OTC Drugs", false);
        addMed(watsons.getId(), "Ceelin", "Vitamin C", "Vitamin C supplement for immunity",
            "100mg", "Syrup", 95.00, 40, "Vitamins", false);
        addMed(watsons.getId(), "Biogesic", "Paracetamol", "For fever and mild pain relief",
            "500mg", "Tablet", 5.75, 120, "OTC Drugs", false);

        addMed(southStar.getId(), "Medicol", "Ibuprofen", "For headache and body pain",
            "200mg", "Softgel", 8.50, 90, "OTC Drugs", false);
        addMed(southStar.getId(), "Solmux", "Carbocisteine", "For cough with phlegm",
            "500mg", "Capsule", 9.00, 70, "OTC Drugs", false);
    }

    private void addMed(int pharmacyId, String brand, String generic, String desc,
                        String dosage, String form, double price, int qty,
                        String category, boolean prescription) {
        Medicine med = new Medicine(nextMedicineId++, pharmacyId, brand, generic,
            desc, dosage, form, price, qty, category, prescription);
        medicines.put(med.getId(), med);
    }

    // ==================== USER OPERATIONS ====================

    public User authenticateUser(String username, String password) {
        userLock.readLock().lock();
        try {
            for (User user : users.values()) {
                if (user.getUsername().equals(username) &&
                    user.getPassword().equals(password) && user.isActive()) {
                    return user;
                }
            }
            return null;
        } finally {
            userLock.readLock().unlock();
        }
    }

    public User createUser(String username, String password, String fullName,
                           String email, UserType userType, int pharmacyId) {
        userLock.writeLock().lock();
        try {
            for (User user : users.values()) {
                if (user.getUsername().equals(username)) return null;
            }
            User newUser = new User(nextUserId++, username, password, fullName, email, userType);
            if (pharmacyId > 0) newUser.setPharmacyId(pharmacyId);
            users.put(newUser.getId(), newUser);
            saveUsers();
            return newUser;
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        } finally {
            userLock.writeLock().unlock();
        }
    }

    public boolean updateUser(int userId, String fullName, String email, String password) {
        userLock.writeLock().lock();
        try {
            User user = users.get(userId);
            if (user == null) return false;
            if (fullName != null && !fullName.isEmpty()) user.setFullName(fullName);
            if (email != null && !email.isEmpty()) user.setEmail(email);
            if (password != null && !password.isEmpty()) user.setPassword(password);
            saveUsers();
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        } finally {
            userLock.writeLock().unlock();
        }
    }

    public boolean deleteUser(int userId) {
        userLock.writeLock().lock();
        try {
            User user = users.get(userId);
            if (user != null && user.getUserType() != UserType.ADMIN) {
                user.setActive(false);
                saveUsers();
                return true;
            }
            return false;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        } finally {
            userLock.writeLock().unlock();
        }
    }

    public List<User> getAllUsers() {
        userLock.readLock().lock();
        try {
            return new ArrayList<>(users.values());
        } finally {
            userLock.readLock().unlock();
        }
    }

    public User getUserById(int id) { return users.get(id); }

    // ==================== PHARMACY OPERATIONS ====================

    public Pharmacy createPharmacy(String name, String address, String contactNumber,
                                   String email, String description) {
        pharmacyLock.writeLock().lock();
        try {
            Pharmacy p = new Pharmacy(nextPharmacyId++, name, address, contactNumber, email, description);
            pharmacies.put(p.getId(), p);
            savePharmacies();
            return p;
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        } finally {
            pharmacyLock.writeLock().unlock();
        }
    }

    public boolean approvePharmacy(int pharmacyId) {
        pharmacyLock.writeLock().lock();
        try {
            Pharmacy p = pharmacies.get(pharmacyId);
            if (p != null) { p.setStatus(PharmacyStatus.APPROVED); savePharmacies(); return true; }
            return false;
        } catch (Exception e) { return false;
        } finally { pharmacyLock.writeLock().unlock(); }
    }

    public boolean rejectPharmacy(int pharmacyId) {
        pharmacyLock.writeLock().lock();
        try {
            Pharmacy p = pharmacies.get(pharmacyId);
            if (p != null) { p.setStatus(PharmacyStatus.REJECTED); savePharmacies(); return true; }
            return false;
        } catch (Exception e) { return false;
        } finally { pharmacyLock.writeLock().unlock(); }
    }

    public List<Pharmacy> getAllPharmacies() {
        pharmacyLock.readLock().lock();
        try { return new ArrayList<>(pharmacies.values()); }
        finally { pharmacyLock.readLock().unlock(); }
    }

    public List<Pharmacy> getApprovedPharmacies() {
        pharmacyLock.readLock().lock();
        try {
            return pharmacies.values().stream()
                .filter(p -> p.getStatus() == PharmacyStatus.APPROVED)
                .collect(Collectors.toList());
        } finally { pharmacyLock.readLock().unlock(); }
    }

    public Pharmacy getPharmacyById(int id) { return pharmacies.get(id); }

    // ==================== MEDICINE OPERATIONS ====================

    public boolean medicineExists(int pharmacyId, String brandName, String genericName,
                                  String dosage, int excludeId) {
        medicineLock.readLock().lock();
        try {
            return medicines.values().stream().anyMatch(m ->
                m.getPharmacyId() == pharmacyId &&
                m.getId() != excludeId &&
                m.getBrandName().equalsIgnoreCase(brandName) &&
                m.getGenericName().equalsIgnoreCase(genericName) &&
                m.getDosage().equalsIgnoreCase(dosage));
        } finally { medicineLock.readLock().unlock(); }
    }

    public Medicine createMedicine(int pharmacyId, String brandName, String genericName,
                                   String description, String dosage, String dosageForm,
                                   double price, int quantity, String category) {
        medicineLock.writeLock().lock();
        try {
            if (medicineExists(pharmacyId, brandName, genericName, dosage, -1)) return null;
            boolean prescription = "Prescription".equalsIgnoreCase(category);
            Medicine med = new Medicine(nextMedicineId++, pharmacyId, brandName, genericName,
                description, dosage, dosageForm, price, quantity, category, prescription);
            medicines.put(med.getId(), med);
            saveMedicines();
            return med;
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        } finally { medicineLock.writeLock().unlock(); }
    }

    public boolean updateMedicine(int medicineId, String brandName, String genericName,
                                  String dosage, String dosageForm, double price,
                                  int quantity, String category) {
        medicineLock.writeLock().lock();
        try {
            Medicine med = medicines.get(medicineId);
            if (med == null) return false;
            if (medicineExists(med.getPharmacyId(), brandName, genericName, dosage, medicineId)) return false;
            med.setBrandName(brandName);
            med.setGenericName(genericName);
            med.setDosage(dosage);
            med.setDosageForm(dosageForm);
            med.setPrice(price);
            med.setQuantityAvailable(quantity);
            med.setCategory(category);
            med.setRequiresPrescription("Prescription".equalsIgnoreCase(category));
            saveMedicines();
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        } finally { medicineLock.writeLock().unlock(); }
    }

    public boolean deleteMedicine(int medicineId, int pharmacyId) {
        medicineLock.writeLock().lock();
        try {
            Medicine med = medicines.get(medicineId);
            if (med != null && med.getPharmacyId() == pharmacyId) {
                medicines.remove(medicineId);
                saveMedicines();
                return true;
            }
            return false;
        } catch (Exception e) { return false;
        } finally { medicineLock.writeLock().unlock(); }
    }

    public List<Medicine> getMedicinesByPharmacy(int pharmacyId) {
        medicineLock.readLock().lock();
        try {
            return medicines.values().stream()
                .filter(m -> m.getPharmacyId() == pharmacyId)
                .collect(Collectors.toList());
        } finally { medicineLock.readLock().unlock(); }
    }

    public List<Medicine> searchMedicines(int pharmacyId, String searchTerm) {
        medicineLock.readLock().lock();
        try {
            String lower = searchTerm.toLowerCase();
            return medicines.values().stream()
                .filter(m -> m.getPharmacyId() == pharmacyId)
                .filter(m -> m.getBrandName().toLowerCase().contains(lower) ||
                            m.getGenericName().toLowerCase().contains(lower) ||
                            m.getDosage().toLowerCase().contains(lower) ||
                            m.getCategory().toLowerCase().contains(lower))
                .collect(Collectors.toList());
        } finally { medicineLock.readLock().unlock(); }
    }

    public List<Medicine> getAllMedicines() {
        medicineLock.readLock().lock();
        try { return new ArrayList<>(medicines.values()); }
        finally { medicineLock.readLock().unlock(); }
    }

    public Medicine getMedicineById(int id) { return medicines.get(id); }

    // ==================== RESERVATION OPERATIONS ====================

    public synchronized Reservation reserveMedicine(int userId, int medicineId,
                                                     int quantity, PaymentMethod paymentMethod) {
        medicineLock.writeLock().lock();
        reservationLock.writeLock().lock();
        try {
            Medicine med = medicines.get(medicineId);
            if (med == null) return null;
            if (med.getEffectiveQuantity() < quantity) return null;

            med.setQuantityReserved(med.getQuantityReserved() + quantity);

            Reservation res = new Reservation(nextReservationId++, userId, medicineId,
                med.getPharmacyId(), quantity, med.getPrice() * quantity);
            res.setPaymentMethod(paymentMethod);

            if (paymentMethod == PaymentMethod.ONLINE_BANK || paymentMethod == PaymentMethod.E_PAYMENT) {
                res.setPaymentStatus(PaymentStatus.PAID);
                res.setStatus(ReservationStatus.CONFIRMED);
                res.setNotes("Paid online - Ready for pick-up");
            } else {
                res.setPaymentStatus(PaymentStatus.UNPAID);
                res.setStatus(ReservationStatus.PENDING);
                res.setNotes("Pay at store - Pending pick-up");
            }

            reservations.put(res.getId(), res);
            saveMedicines();
            saveReservations();
            return res;
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        } finally {
            reservationLock.writeLock().unlock();
            medicineLock.writeLock().unlock();
        }
    }

    public synchronized boolean approveReservation(int reservationId) {
        reservationLock.writeLock().lock();
        try {
            Reservation res = reservations.get(reservationId);
            if (res == null || res.getStatus() != ReservationStatus.PENDING) return false;
            res.setStatus(ReservationStatus.CONFIRMED);
            res.setNotes("Approved by pharmacist - Ready for pick-up");
            saveReservations();
            return true;
        } catch (Exception e) { return false;
        } finally { reservationLock.writeLock().unlock(); }
    }

    public synchronized boolean rejectReservation(int reservationId) {
        medicineLock.writeLock().lock();
        reservationLock.writeLock().lock();
        try {
            Reservation res = reservations.get(reservationId);
            if (res == null) return false;
            if (res.getStatus() != ReservationStatus.PENDING &&
                res.getStatus() != ReservationStatus.CONFIRMED) return false;

            Medicine med = medicines.get(res.getMedicineId());
            if (med != null) {
                med.setQuantityReserved(Math.max(0, med.getQuantityReserved() - res.getQuantity()));
            }
            res.setStatus(ReservationStatus.CANCELLED);
            if (res.getPaymentStatus() == PaymentStatus.PAID) {
                res.setPaymentStatus(PaymentStatus.REFUNDED);
                res.setNotes("Rejected by pharmacist. Payment refunded.");
            } else {
                res.setNotes("Rejected by pharmacist.");
            }
            saveMedicines();
            saveReservations();
            return true;
        } catch (Exception e) { return false;
        } finally {
            reservationLock.writeLock().unlock();
            medicineLock.writeLock().unlock();
        }
    }

    public synchronized boolean completeReservation(int reservationId) {
        medicineLock.writeLock().lock();
        reservationLock.writeLock().lock();
        try {
            Reservation res = reservations.get(reservationId);
            if (res == null || res.getStatus() != ReservationStatus.CONFIRMED) return false;

            Medicine med = medicines.get(res.getMedicineId());
            if (med != null) {
                med.setQuantityAvailable(med.getQuantityAvailable() - res.getQuantity());
                med.setQuantityReserved(Math.max(0, med.getQuantityReserved() - res.getQuantity()));
            }
            res.setStatus(ReservationStatus.COMPLETED);
            if (res.getPaymentStatus() == PaymentStatus.UNPAID) {
                res.setPaymentStatus(PaymentStatus.PAID);
            }
            res.setNotes("Picked up and completed.");
            saveMedicines();
            saveReservations();
            return true;
        } catch (Exception e) { return false;
        } finally {
            reservationLock.writeLock().unlock();
            medicineLock.writeLock().unlock();
        }
    }

    public synchronized boolean cancelReservation(int reservationId, int userId) {
        medicineLock.writeLock().lock();
        reservationLock.writeLock().lock();
        try {
            Reservation res = reservations.get(reservationId);
            if (res == null || res.getUserId() != userId ||
                res.getStatus() != ReservationStatus.PENDING) return false;

            Medicine med = medicines.get(res.getMedicineId());
            if (med != null) {
                med.setQuantityReserved(Math.max(0, med.getQuantityReserved() - res.getQuantity()));
            }
            res.setStatus(ReservationStatus.CANCELLED);
            if (res.getPaymentStatus() == PaymentStatus.PAID) {
                res.setPaymentStatus(PaymentStatus.REFUNDED);
                res.setNotes("Cancelled by customer. Payment refunded.");
            } else {
                res.setNotes("Cancelled by customer.");
            }
            saveMedicines();
            saveReservations();
            return true;
        } catch (Exception e) { return false;
        } finally {
            reservationLock.writeLock().unlock();
            medicineLock.writeLock().unlock();
        }
    }

    public List<Reservation> getReservationsByPharmacy(int pharmacyId) {
        reservationLock.readLock().lock();
        try {
            return reservations.values().stream()
                .filter(r -> r.getPharmacyId() == pharmacyId)
                .collect(Collectors.toList());
        } finally { reservationLock.readLock().unlock(); }
    }

    public List<Reservation> getReservationsByUser(int userId) {
        reservationLock.readLock().lock();
        try {
            return reservations.values().stream()
                .filter(r -> r.getUserId() == userId)
                .collect(Collectors.toList());
        } finally { reservationLock.readLock().unlock(); }
    }

    public List<Reservation> getAllReservations() {
        reservationLock.readLock().lock();
        try { return new ArrayList<>(reservations.values()); }
        finally { reservationLock.readLock().unlock(); }
    }
}
