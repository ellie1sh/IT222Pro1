package client;

import server.DataStore;
import client.views.MainFrame;

import javax.swing.*;

/**
 * Main entry point for the Pharmacy Reservation System.
 * Standalone Swing application with local data storage.mercury_*/
public class ClientMain {
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            // Initialize data store
            DataStore dataStore = DataStore.getInstance();
            
            // Start expiry check timer
            java.util.Timer timer = new java.util.Timer(true);
            timer.scheduleAtFixedRate(new java.util.TimerTask() {
                @Override
                public void run() {
                    dataStore.checkExpiredReservations();
                }
            }, 60000, 300000); // Check every 5 minutes

            MainFrame frame = new MainFrame(dataStore);
            frame.setVisible(true);

            System.out.println("======================================");
            System.out.println(" Pharmacy Reservation System");
            System.out.println(" SDG 3: Good Health and Well-Being");
            System.out.println("======================================");
            System.out.println();
            System.out.println("Demo Credentials:");
            System.out.println("  Admin:      admin / admin123");
            System.out.println("  Pharmacist: mercury_pharm / pharm123");
            System.out.println("              watsons_pharm / pharm123");
            System.out.println("  Customer:   resident / user123");
            System.out.println();
        });
    }
}
