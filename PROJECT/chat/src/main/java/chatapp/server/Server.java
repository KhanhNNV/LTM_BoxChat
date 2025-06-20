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

public class Server {
    private static final int PORT = 12345; // Cổng mà server sẽ lắng nghe
    private static final ExecutorService pool = Executors.newCachedThreadPool();
    // Map<GroupId, List<ClientHandler>>
    private static final Map<Integer, List<ClientHandler>> roomClients = new ConcurrentHashMap<>();

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
}