package client.network;

import javax.xml.parsers.*;
import org.w3c.dom.*;
import java.io.*;
import java.net.Socket;

/**
 * Handles socket communication with the server.
 * Sends XML requests and receives XML responses.
 */
public class ServerConnection {
    private static final String DEFAULT_HOST = "10.135.140.175";
    private static final int DEFAULT_PORT = 5555;
    private String host;
    private int port;
    private Socket socket;
    private DataInputStream in;
    private DataOutputStream out;

    public ServerConnection() {
        this(DEFAULT_HOST, DEFAULT_PORT);
    }

    public ServerConnection(String host, int port) {
        this.host = host;
        this.port = port;
    }

    public boolean connect() {
        try {
            socket = new Socket(host, port);
            in = new DataInputStream(socket.getInputStream());
            out = new DataOutputStream(socket.getOutputStream());
            return true;
        } catch (IOException e) {
            return false;
        }
    }

    public void disconnect() {
        try {
            if (socket != null && !socket.isClosed()) socket.close();
        } catch (IOException ignored) {}
    }

    public boolean isConnected() {
        return socket != null && !socket.isClosed() && socket.isConnected();
    }

    /**
     * Sends an XML request and returns the parsed response Document.
     */
    public synchronized Document sendRequest(String xmlRequest) throws Exception {
        if (!isConnected()) throw new IOException("Not connected to server");
        out.writeUTF(xmlRequest);
        out.flush();
        String responseXml = in.readUTF();
        DocumentBuilder builder = DocumentBuilderFactory.newInstance().newDocumentBuilder();
        return builder.parse(new ByteArrayInputStream(responseXml.getBytes("UTF-8")));
    }

    /**
     * Checks if the response indicates success.
     */
    public static boolean isSuccess(Document response) {
        return "SUCCESS".equals(getStatus(response));
    }

    public static String getStatus(Document response) {
        return getText(response.getDocumentElement(), "status");
    }

    public static String getMessage(Document response) {
        return getText(response.getDocumentElement(), "message");
    }

    public static Element getDataElement(Document response) {
        NodeList list = response.getDocumentElement().getElementsByTagName("data");
        if (list.getLength() > 0) return (Element) list.item(0);
        return null;
    }

    public static String getText(Element parent, String tagName) {
        NodeList list = parent.getElementsByTagName(tagName);
        if (list.getLength() > 0) return list.item(0).getTextContent().trim();
        return "";
    }

    public static NodeList getElements(Element parent, String tagName) {
        return parent.getElementsByTagName(tagName);
    }

    public static int parseInt(String s, int def) {
        try { return Integer.parseInt(s); } catch (Exception e) { return def; }
    }

    public static double parseDouble(String s, double def) {
        try { return Double.parseDouble(s); } catch (Exception e) { return def; }
    }

    public static boolean parseBoolean(String s) {
        return Boolean.parseBoolean(s);
    }

    /**
     * Builds an XML request string.
     */
    public static String buildRequest(String action, String... params) {
        StringBuilder sb = new StringBuilder();
        sb.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>");
        sb.append("<request>");
        sb.append("<action>").append(action).append("</action>");
        for (int i = 0; i < params.length - 1; i += 2) {
            sb.append("<").append(params[i]).append(">");
            sb.append(esc(params[i + 1]));
            sb.append("</").append(params[i]).append(">");
        }
        sb.append("</request>");
        return sb.toString();
    }

    private static String esc(String s) {
        if (s == null) return "";
        return s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
    }
}
