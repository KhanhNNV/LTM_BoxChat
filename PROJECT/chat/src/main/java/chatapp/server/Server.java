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
import java.net.URL; // Thêm import này ở đầu file

import chatapp.model.NetworkMessage;
import chatapp.model.User;

public class Server {
    private static final int PORT = 12345; // Cổng mà server sẽ lắng nghe
    private static final ExecutorService pool = Executors.newCachedThreadPool();
    // Map<GroupId, List<ClientHandler>>
    private static final Map<Integer, List<ClientHandler>> roomClients = new ConcurrentHashMap<>();
    static final Map<Integer, ClientHandler> onlineUsers = new ConcurrentHashMap<>();

    public static void main(String[] args) throws IOException {

        // Nạp Keystore của server từ classpath (trong thư mục resources/security)
        URL keyStoreUrl = Server.class.getResource("/security/serverkeystore.jks");
        if (keyStoreUrl != null) {
            System.setProperty("javax.net.ssl.keyStore", keyStoreUrl.getPath());
            System.setProperty("javax.net.ssl.keyStorePassword", "secretpassword");
            System.out.println("Server KeyStore loaded from: " + keyStoreUrl.getPath());
        } else {
            System.err.println("Could not find serverkeystore.jks in classpath! Make sure it is in 'src/main/resources/security'");
            // Dừng server nếu không tìm thấy keystore
            return;
        }

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

    public static void removeAllUsersFromRoom(int groupId) {
        roomClients.remove(groupId);
        System.out.println("Room " + groupId + " has been deleted from server memory.");
    }

    public static void notifyUserRemoved(int userIdToNotify, int fromGroupId) {
        // Tra cứu ClientHandler theo userIdToNotify
        ClientHandler targetHandler = onlineUsers.get(userIdToNotify);
        if (targetHandler != null) { // Giả sử -1 và 0 là ID không hợp lệ
            System.out.println("Notifying user ID " + userIdToNotify + " about removal from room " + fromGroupId);

            // Gửi tin nhắn đặc biệt cho người bị xóa
            targetHandler.sendMessage(
                    new NetworkMessage(NetworkMessage.MessageType.YOU_HAVE_BEEN_REMOVED,
                            "Bạn đã bị trưởng phòng xóa khỏi nhóm."));

            // Cập nhật trạng thái của handler đó, đưa họ về "sảnh chờ"
            targetHandler.setCurrentRoomId(-1);
        } else {
            // Người dùng có thể đã offline, không sao cả.
            System.out.println("Could not notify user ID " + userIdToNotify + " because they are offline.");
        }
    }

    public static void addOnlineUser(int userId, ClientHandler handler) {
        onlineUsers.put(userId, handler);
    }

    public static void removeOnlineUser(int userId) {
        onlineUsers.remove(userId);
    }

    public static void broadcastUserStatusUpdate(User userWithStatus, ClientHandler excludedHandler) {
        // System.out.println("Broadcasting status update for user " +
        // userWithStatus.getId() + ": "
        // + (userWithStatus.isOnline() ? "Online" : "Offline"));
        NetworkMessage statusUpdateMessage = new NetworkMessage(NetworkMessage.MessageType.USER_STATUS_UPDATE,
                userWithStatus);

        // Gửi cho tất cả các user đang online
        for (ClientHandler handler : onlineUsers.values()) {
            if (handler != excludedHandler) {
                handler.sendMessage(statusUpdateMessage);
            } else {
                System.out.println("[DEBUG/Broadcast] Skipping excluded handler: " + handler.getUsername()); // DEBUG
            }
        }
    }

    public static void broadcastToAllInRoom(int groupId, chatapp.model.NetworkMessage message) {
        List<ClientHandler> clientsInRoom = roomClients.get(groupId);
        if (clientsInRoom != null) {
            for (ClientHandler client : clientsInRoom) {
                client.sendMessage(message);
            }
        }
    }

}