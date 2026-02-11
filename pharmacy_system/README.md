# Pharmacy Reservation System

A client-server pharmacy reservation application built with Java socket programming, XML data exchange, and MVC architecture.

**SDG 3: Good Health and Well-Being** - Ensures accessible and affordable medicine reservation for communities.

---

## Server-Client Architecture (How It Works)

### Overview

The system is split into two separate Java programs that communicate over TCP sockets using XML messages:

```
+---------------------+         TCP Socket (port 5555)         +---------------------+
|    CLIENT (GUI)     | <====================================> |   SERVER (Console)   |
|                     |       XML Request / XML Response        |                     |
| - Swing GUI         |                                         | - DataStore (XML)    |
| - MVC Pattern       |                                         | - Transaction Log    |
| - Controllers send  |                                         | - Thread per client  |
|   XML requests      |                                         | - Concurrency locks  |
+---------------------+                                         +---------------------+
```

### Server Side

1. **PharmacyServer** runs in the console. The operator types `1` to start or `2` to stop the server at any time without restarting the program.
2. When started, the server opens a `ServerSocket` on port 5555 and waits for incoming client connections.
3. Every time a client connects, the server spawns a new **ClientHandler** thread from a `CachedThreadPool` so multiple clients are handled concurrently.
4. **ClientHandler** reads an XML request string from the socket (`DataInputStream.readUTF()`), parses it with Java DOM, determines the `<action>` tag, dispatches to the correct handler method, builds an XML response, and sends it back (`DataOutputStream.writeUTF()`).
5. **DataStore** is a singleton that stores all data in `ConcurrentHashMap` collections protected by `ReentrantReadWriteLock` for thread safety. It persists everything to XML files (`data/users.xml`, `data/pharmacies.xml`, `data/medicines.xml`, `data/reservations.xml`).
6. Every action is logged to the console with: `[timestamp] [username] ACTION - details`.

### Client Side (MVC)

1. **ClientMain** creates a `ServerConnection` that opens a TCP socket to the server.
2. **ServerConnection** provides a `sendRequest(xmlString)` method that writes XML to the socket and reads the XML response, parsing it into a DOM `Document`.
3. **Controllers** (Auth, Admin, Pharmacist, User) build XML request strings using `ServerConnection.buildRequest()`, call `sendRequest()`, and parse the response XML to extract data.
4. **Views** (Login, Admin, Pharmacist, User) are Swing JPanels that call controller methods and display data in tables and forms. They never access the server directly.
5. **MainFrame** uses `CardLayout` to switch between views based on the logged-in user type.

### XML Message Format

Every client request:
```xml
<?xml version="1.0" encoding="UTF-8"?>
<request>
  <action>SEARCH_MEDICINES</action>
  <pharmacyId>-1</pharmacyId>
  <searchTerm>Biogesic</searchTerm>
</request>
```

Every server response:
```xml
<?xml version="1.0" encoding="UTF-8"?>
<response>
  <status>SUCCESS</status>
  <message>OK</message>
  <data>
    <medicines>
      <medicineEntry>
        <medicine>
          <id>1</id>
          <brandName>Biogesic</brandName>
          ...
        </medicine>
        <pharmacyName>Mercury Drug</pharmacyName>
      </medicineEntry>
    </medicines>
  </data>
</response>
```

### Concurrency Handling

- The server uses `ReentrantReadWriteLock` on each data collection (users, pharmacies, medicines, reservations).
- Read operations acquire a read lock (multiple readers allowed).
- Write operations acquire a write lock (exclusive access).
- Reservation creation locks both the medicine lock and reservation lock to prevent two clients from reserving the same last unit simultaneously.

### Transaction Logging

The server console shows a live log of every action:
```
[2026-02-06 14:30:00] [admin] LOGIN - User 'admin' logged in as ADMIN
[2026-02-06 14:31:00] [mercury_pharm] CREATE_MEDICINE - Added 'Biogesic 500mg' to Mercury Drug
[2026-02-06 14:32:00] [resident] RESERVE_MEDICINE - Reserved 5x Biogesic (Reservation #3, ONLINE_BANK)
[2026-02-06 14:32:05] [mercury_pharm] COMPLETE_RESERVATION - Completed reservation #3
```

---

## Features

### User Roles
1. **Admin** - Manage users (CRUD), approve/reject pharmacies, view all medicines and reservations system-wide
2. **Pharmacist** - Manage medicine inventory (add/edit/delete with duplicate check), view and handle reservations (approve/reject/complete pickup)
3. **Customer (Resident)** - Search medicines across pharmacies, reserve with payment options, view and cancel own reservations

### Medicine Search Flow (Customer)
1. Customer types a medicine name (e.g., "Biogesic") and clicks Search
2. System shows all pharmacies that carry it, with price and availability
3. Customer selects a row and clicks Reserve
4. Reservation dialog asks for quantity and payment method

### Payment Logic
- **Online Bank / E-Payment**: Payment is processed immediately. Reservation status = CONFIRMED (ready for pick-up).
- **Pay at Store**: Reservation status = PENDING. Pharmacist must approve before the customer picks up.

### Reservation Lifecycle
```
PENDING ──[Pharmacist Approves]──> CONFIRMED ──[Picked Up]──> COMPLETED
   │                                    │
   │──[Customer Cancels]──> CANCELLED   │──[Pharmacist Rejects]──> CANCELLED (refund if paid)
   │                                    │
   └──[24h passes]──> EXPIRED           └──[24h passes]──> EXPIRED (refund if paid)
```

### 24-Hour Auto-Expiry
- A background scheduler checks every 5 minutes for expired reservations.
- If a PENDING or CONFIRMED reservation passes its 24-hour window without pickup, it is automatically set to EXPIRED.
- If the customer paid online, the payment status is changed to REFUNDED.
- The reserved medicine stock is released back to available inventory.

### Medicine Duplicate Prevention
- When adding or editing a medicine, the system checks if the same brand name + generic name + dosage already exists in that pharmacy.
- If a duplicate is found, the operation is rejected with an error message.

### Pharmacist Button Layout
- **Add New Medicine** section: form fields + "Add Medicine" + "Clear Form" buttons
- **Inventory** section: "Save Changes" + "Delete Selected" + "Refresh" buttons above the table
- Click a table row to load it into the form for editing; then click "Save Changes" to update

---

## How to Run

### 1. Compile
```bash
./compile.sh
```

### 2. Start the Server
```bash
./run_server.sh
```
Then type `1` and press Enter to start the server.

### 3. Start the Client (in a separate terminal)
```bash
./run_client.sh
```
You can start multiple clients simultaneously.

### Demo Credentials
| Role | Username | Password |
|------|----------|----------|
| Admin | admin | admin123 |
| Pharmacist (Mercury Drug) | mercury_pharm | pharm123 |
| Pharmacist (Watsons) | watsons_pharm | pharm123 |
| Customer | resident | user123 |

---

## Project Structure
```
pharmacy_system/
  src/
    models/                    # Shared data models (Medicine, Pharmacy, Reservation, User)
    server/
      PharmacyServer.java      # Console server: start/stop, logging, thread pool
      ClientHandler.java       # Per-client thread: XML request dispatch
      DataStore.java           # XML file storage with concurrency locks
    client/
      ClientMain.java          # Client entry point
      network/
        ServerConnection.java  # TCP socket + XML send/receive
      controllers/             # MVC controllers (build XML, parse responses)
        AuthController.java
        AdminController.java
        PharmacistController.java
        UserController.java
      views/                   # MVC views (Swing GUI)
        MainFrame.java
        LoginView.java
        AdminView.java
        PharmacistView.java
        UserView.java
  bin/                         # Compiled .class files
  data/                        # XML data files (auto-created on first run)
  compile.sh                   # Build script
  run_server.sh                # Server launcher
  run_client.sh                # Client launcher
```
