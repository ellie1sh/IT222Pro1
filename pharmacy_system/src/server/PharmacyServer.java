package server;

import java.io.*;
import java.net.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.*;

/**
 * Pharmacy Reservation System - Server
 * Console-based server with start/stop capability and transaction logging.
 * Handles multiple concurrent client connections via socket programming.
 */
public class PharmacyServer {
    private static final int DEFAULT_PORT = 5555;
    private int port;
    private ServerSocket serverSocket;
    private ExecutorService threadPool;
    private volatile boolean running = false;
    private final List<String> transactionLog = Collections.synchronizedList(new ArrayList<>());
    private final DateTimeFormatter logFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    public PharmacyServer(int port) {
        this.port = port;
    }

    public void start() {
        if (running) {
            System.out.println("[!] Server is already running.");
            return;
        }
        try {
            DataStore.getInstance(); // Initialize data store
            serverSocket = new ServerSocket(port);
            threadPool = Executors.newCachedThreadPool();
            running = true;

            log("SYSTEM", "SERVER_START", "Server started on port " + port);
            System.out.println("[*] Server started on port " + port);
            System.out.println("[*] Waiting for client connections...");

            // Start expiry check timer
            ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();
            scheduler.scheduleAtFixedRate(() -> {
                if (running) DataStore.getInstance().checkExpiredReservations();
            }, 1, 5, TimeUnit.MINUTES);

            // Accept connections in a separate thread
            Thread acceptThread = new Thread(() -> {
                while (running && !serverSocket.isClosed()) {
                    try {
                        Socket clientSocket = serverSocket.accept();
                        System.out.println("[+] Client connected: " + clientSocket.getInetAddress());
                        threadPool.execute(new ClientHandler(clientSocket, this));
                    } catch (IOException e) {
                        if (running) {
                            System.err.println("[!] Accept error: " + e.getMessage());
                        }
                    }
                }
            });
            acceptThread.setDaemon(true);
            acceptThread.start();

        } catch (IOException e) {
            System.err.println("[!] Failed to start server: " + e.getMessage());
        }
    }

    public void stop() {
        if (!running) {
            System.out.println("[!] Server is not running.");
            return;
        }
        running = false;
        try {
            if (serverSocket != null && !serverSocket.isClosed()) {
                serverSocket.close();
            }
            if (threadPool != null) {
                threadPool.shutdown();
                threadPool.awaitTermination(5, TimeUnit.SECONDS);
            }
            DataStore.getInstance().saveAllData();
            log("SYSTEM", "SERVER_STOP", "Server stopped");
            System.out.println("[*] Server stopped.");
        } catch (Exception e) {
            System.err.println("[!] Error stopping server: " + e.getMessage());
        }
    }

    public void log(String user, String action, String details) {
        String timestamp = LocalDateTime.now().format(logFormatter);
        String entry = String.format("[%s] [%s] %s - %s", timestamp, user, action, details);
        transactionLog.add(entry);
        System.out.println(entry);
    }

    public void showLog() {
        System.out.println("\n========== TRANSACTION LOG ==========");
        if (transactionLog.isEmpty()) {
            System.out.println("  (No transactions recorded)");
        } else {
            for (String entry : transactionLog) {
                System.out.println("  " + entry);
            }
        }
        System.out.println("=====================================\n");
    }

    public static void main(String[] args) {
        int port = DEFAULT_PORT;
        if (args.length > 0) {
            try { port = Integer.parseInt(args[0]); } catch (NumberFormatException ignored) {}
        }

        PharmacyServer server = new PharmacyServer(port);
        Scanner scanner = new Scanner(System.in);

        System.out.println("=============================================");
        System.out.println("  Pharmacy Reservation System - Server");
        System.out.println("  SDG 3: Good Health and Well-Being");
        System.out.println("=============================================");
        System.out.println();

        boolean exit = false;
        while (!exit) {
            System.out.println("Server Menu:");
            System.out.println("  1. Start Server");
            System.out.println("  2. Stop Server");
            System.out.println("  3. View Transaction Log");
            System.out.println("  4. Exit");
            System.out.print("Choice: ");

            String choice = scanner.nextLine().trim();
            switch (choice) {
                case "1":
                    server.start();
                    break;
                case "2":
                    server.stop();
                    break;
                case "3":
                    server.showLog();
                    break;
                case "4":
                    server.stop();
                    exit = true;
                    System.out.println("Goodbye.");
                    break;
                default:
                    System.out.println("[!] Invalid choice. Please enter 1-4.");
            }
            System.out.println();
        }
        scanner.close();
    }
}
