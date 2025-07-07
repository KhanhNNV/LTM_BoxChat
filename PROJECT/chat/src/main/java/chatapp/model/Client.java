package chatapp.model;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.util.function.Consumer;
import javax.net.ssl.SSLSocket;
import javax.net.ssl.SSLSocketFactory;

import javafx.application.Platform;

public class Client {
    private static Client instance;
    private Socket socket;
    private ObjectOutputStream out;
    private ObjectInputStream in;
    private Consumer<NetworkMessage> onMessageReceived;

    private Client() {
    }

    public static synchronized Client getInstance() {
        if (instance == null) {
            instance = new Client();
        }
        return instance;
    }

    public void connect(String host, int port) throws IOException {
        if (socket == null || socket.isClosed()) {
            SSLSocketFactory sf = (SSLSocketFactory) SSLSocketFactory.getDefault();
            socket = (SSLSocket) sf.createSocket(host, port);
            ((SSLSocket) socket).startHandshake();
            out = new ObjectOutputStream(socket.getOutputStream());
            in = new ObjectInputStream(socket.getInputStream());
            startListening();
        }
    }

    public void sendMessage(NetworkMessage message) {
        try {
            out.writeObject(message);
            out.flush();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void startListening() {
        new Thread(() -> {
            try {
                System.out.println("[DEBUG/Client Listener] Starting to listen for messages from server..."); // DEBUG
                while (!socket.isClosed()) {
                    NetworkMessage message = (NetworkMessage) in.readObject();
                    System.out.println("[DEBUG/Client Listener] Raw message received. Type: " + message.getType());
                    if (onMessageReceived != null) {
                        Platform.runLater(() -> onMessageReceived.accept(message));
                    } else {
                        System.out.println(
                                "[DEBUG/Client Listener] WARNING: onMessageReceived is NULL. Message not handled."); // DEBUG
                    }
                }
            } catch (IOException | ClassNotFoundException e) {
                System.out.println("Disconnected from server.");
            } finally {
                System.out.println("[DEBUG/Client Listener] Listener thread stopped.");
            }
        }).start();
    }

    public void setOnMessageReceived(Consumer<NetworkMessage> onMessageReceived) {
        this.onMessageReceived = onMessageReceived;
    }

    public void disconnect() {
        try {
            if (socket != null && !socket.isClosed()) {
                socket.close();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}