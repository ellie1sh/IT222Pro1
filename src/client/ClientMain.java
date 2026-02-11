package client;

import client.network.ServerConnection;
import client.views.MainFrame;

import javax.swing.*;

/**
 * Client entry point for the Pharmacy Reservation System.
 * Connects to the server via socket and launches the GUI.
 */
public class ClientMain {
    public static void main(String[] args) {
        String host = "localhost";
        int port = 5555;

        if (args.length >= 1) host = args[0];
        if (args.length >= 2) {
            try { port = Integer.parseInt(args[1]); } catch (NumberFormatException ignored) {}
        }

        final String serverHost = host;
        final int serverPort = port;

        SwingUtilities.invokeLater(() -> {
            ServerConnection connection = new ServerConnection(serverHost, serverPort);

            if (!connection.connect()) {
                JOptionPane.showMessageDialog(null,
                    "Cannot connect to server at " + serverHost + ":" + serverPort + "\n" +
                    "Please make sure the server is running.",
                    "Connection Error", JOptionPane.ERROR_MESSAGE);
                System.exit(1);
            }

            MainFrame frame = new MainFrame(connection);
            frame.setVisible(true);

            System.out.println("======================================");
            System.out.println(" Pharmacy Reservation System - Client");
            System.out.println(" Connected to " + serverHost + ":" + serverPort);
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
