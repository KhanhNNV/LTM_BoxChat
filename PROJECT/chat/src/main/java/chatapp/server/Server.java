// TẠO FILE MỚI: src/main/java/chatapp/server/Server.java
package chatapp.server;

import javax.net.ssl.SSLServerSocket;
import javax.net.ssl.SSLServerSocketFactory;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class Server {
    private static final int PORT = 12345; // Cổng mà server sẽ lắng nghe
    private static final ExecutorService pool = Executors.newCachedThreadPool();
    // Map<GroupId, List<ClientHandler>>
    private static final Map<Integer, List<ClientHandler>> roomClients = new ConcurrentHashMap<>();

    public static void main(String[] args) throws IOException {

        // --- BẮT ĐẦU THAY ĐỔI ---
        // 1. Đặt các thuộc tính hệ thống để Java biết nơi tìm Keystore và mật khẩu của nó.
        //    Đường dẫn tương đối này giả định các file .jks nằm ở thư mục gốc của dự án.
        System.setProperty("javax.net.ssl.keyStore", "serverkeystore.jks");
        System.setProperty("javax.net.ssl.keyStorePassword", "secretpassword"); // Dùng mật khẩu bạn đã tạo

        // 2. Lấy Factory để tạo SSLServerSocket
        SSLServerSocketFactory ssf = (SSLServerSocketFactory) SSLServerSocketFactory.getDefault();

        // 3. Sử dụng SSLServerSocket thay vì ServerSocket
        try (SSLServerSocket listener = (SSLServerSocket) ssf.createServerSocket(PORT)) {
            // Tùy chọn: Yêu cầu Client phải xác thực (trong trường hợp này không cần)
            // listener.setNeedClientAuth(false);

            System.out.println("Secure Server is running and waiting for clients on port " + PORT);

            while (true) {
                // 4. `accept()` bây giờ trả về một SSLSocket, nhưng chúng ta có thể coi nó
                //    như một Socket bình thường vì nó kế thừa từ Socket.
                Socket clientSocket = listener.accept();
                System.out.println("New client connected: " + clientSocket.getInetAddress());
                ClientHandler clientHandler = new ClientHandler(clientSocket);
                pool.execute(clientHandler);
            }
        }
        // --- KẾT THÚC THAY ĐỔI ---
    }

    public static void broadcastMessage(int groupId, chatapp.model.NetworkMessage message, ClientHandler sender) {
        List<ClientHandler> clientsInRoom = roomClients.get(groupId);
        if (clientsInRoom != null) {
            for (ClientHandler client : clientsInRoom) {
                if (client != sender) { // Không gửi lại cho người gửi
                    client.sendMessage(message);
                }
            }
        }
    }

    public static void addUserToRoom(int groupId, ClientHandler client) {
        roomClients.computeIfAbsent(groupId, k -> new CopyOnWriteArrayList<>()).add(client);
        System.out.println("User " + client.getUsername() + " added to room " + groupId);
    }

    public static void removeUserFromRoom(int groupId, ClientHandler client) {
        List<ClientHandler> clientsInRoom = roomClients.get(groupId);
        if (clientsInRoom != null) {
            clientsInRoom.remove(client);
            System.out.println("User " + client.getUsername() + " removed from room " + groupId);
            if (clientsInRoom.isEmpty()) {
                roomClients.remove(groupId);
            }
        }
    }
}