// TẠO FILE MỚI: src/main/java/chatapp/server/Server.java
package chatapp.server;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import chatapp.model.NetworkMessage;

public class Server {
    private static final int PORT = 12345; // Cổng mà server sẽ lắng nghe
    private static final ExecutorService pool = Executors.newCachedThreadPool();
    // Map<GroupId, List<ClientHandler>>
    private static final Map<Integer, List<ClientHandler>> roomClients = new ConcurrentHashMap<>();
    private static final Map<Integer, ClientHandler> onlineUsers = new ConcurrentHashMap<>();

    public static void main(String[] args) throws IOException {
        System.out.println("Server is running and waiting for clients...");
        try (ServerSocket listener = new ServerSocket(PORT)) {
            while (true) {
                Socket clientSocket = listener.accept();
                System.out.println("New client connected: " + clientSocket.getInetAddress());
                ClientHandler clientHandler = new ClientHandler(clientSocket);
                pool.execute(clientHandler);
            }
        }
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
}