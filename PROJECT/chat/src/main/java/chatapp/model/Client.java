// Thay thế/sửa file: src/main/java/chatapp/model/ClientSocket.java thành Client.java
package chatapp.model;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.util.function.Consumer;
import javax.net.ssl.SSLSocket;
import javax.net.ssl.SSLSocketFactory;
import java.io.IOException;

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
            // --- BẮT ĐẦU THAY ĐỔI ---
            // 1. Lấy Factory để tạo SSLSocket
            SSLSocketFactory sf = (SSLSocketFactory) SSLSocketFactory.getDefault();

            // 2. Tạo một SSLSocket thay vì Socket thông thường
            socket = (SSLSocket) sf.createSocket(host, port);

            // 3. (Tùy chọn) Bắt đầu "bắt tay" (handshake) ngay lập tức để phát hiện lỗi sớm
            ((SSLSocket) socket).startHandshake();
            // --- KẾT THÚC THAY ĐỔI ---

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
            // Handle error (e.g., show an alert)
        }
    }

    private void startListening() {
        new Thread(() -> {
            try {
                while (!socket.isClosed()) {
                    NetworkMessage message = (NetworkMessage) in.readObject();
                    if (onMessageReceived != null) {
                        // Cập nhật UI trên JavaFX Application Thread
                        Platform.runLater(() -> onMessageReceived.accept(message));
                    }
                }
            } catch (IOException | ClassNotFoundException e) {
                System.out.println("Disconnected from server.");
                // Platform.runLater(() -> showAlert(...));
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