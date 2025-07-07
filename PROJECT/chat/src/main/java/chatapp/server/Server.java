package chatapp.server;

import javax.net.ssl.SSLServerSocket;
import javax.net.ssl.SSLServerSocketFactory;
import java.io.IOException;
import java.net.Socket;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.net.URL;

import chatapp.model.NetworkMessage;
import chatapp.model.User;

public class Server {
    private static final int PORT = 12345;
    private static final ExecutorService pool = Executors.newCachedThreadPool();
    private static final Map<Integer, List<ClientHandler>> roomClients = new ConcurrentHashMap<>();
    static final Map<Integer, ClientHandler> onlineUsers = new ConcurrentHashMap<>();

    public static void main(String[] args) throws IOException {

        URL keyStoreUrl = Server.class.getResource("/security/serverkeystore.jks");
        if (keyStoreUrl != null) {
            System.setProperty("javax.net.ssl.keyStore", keyStoreUrl.getPath());
            System.setProperty("javax.net.ssl.keyStorePassword", "secretpassword");
            System.out.println("Server KeyStore loaded from: " + keyStoreUrl.getPath());
        } else {
            System.err.println("Could not find serverkeystore.jks in classpath! Make sure it is in 'src/main/resources/security'");
            return;
        }

        SSLServerSocketFactory ssf = (SSLServerSocketFactory) SSLServerSocketFactory.getDefault();

        try (SSLServerSocket listener = (SSLServerSocket) ssf.createServerSocket(PORT)) {
            System.out.println("Secure Server is running and waiting for clients on port " + PORT);

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
                if (client != sender) {
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
        ClientHandler targetHandler = onlineUsers.get(userIdToNotify);
        if (targetHandler != null) {
            System.out.println("Notifying user ID " + userIdToNotify + " about removal from room " + fromGroupId);

            targetHandler.sendMessage(
                    new NetworkMessage(NetworkMessage.MessageType.YOU_HAVE_BEEN_REMOVED,
                            "Bạn đã bị trưởng phòng xóa khỏi nhóm."));

            targetHandler.setCurrentRoomId(-1);
        } else {
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

        NetworkMessage statusUpdateMessage = new NetworkMessage(NetworkMessage.MessageType.USER_STATUS_UPDATE,
                userWithStatus);

        for (ClientHandler handler : onlineUsers.values()) {
            if (handler != excludedHandler) {
                handler.sendMessage(statusUpdateMessage);
            } else {
                System.out.println("[DEBUG/Broadcast] Skipping excluded handler: " + handler.getUsername());
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