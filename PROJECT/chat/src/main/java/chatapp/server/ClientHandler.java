// TẠO FILE MỚI: src/main/java/chatapp/server/ClientHandler.java
package chatapp.server;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.sql.SQLException; // Tái sử dụng DBConfig
import java.util.Base64;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import chatapp.model.Message;
import chatapp.model.NetworkMessage;
import chatapp.model.NetworkMessage.MessageType;
import chatapp.model.Room;
import chatapp.model.User;
import chatapp.service.AIService;

public class ClientHandler implements Runnable {
    private final Socket clientSocket;
    private ObjectOutputStream out;
    private ObjectInputStream in;
    private UserService userService;
    private GroupService groupService;
    private User currentUser;
    private int currentRoomId = -1;

    public void setCurrentRoomId(int roomId) {
        this.currentRoomId = roomId;
    }

    // Bạn cũng nên có một getter để đọc giá trị này nếu cần
    public int getCurrentRoomId() {
        return this.currentRoomId;
    }

    public ClientHandler(Socket socket) {
        this.clientSocket = socket;
        try {
            this.userService = new UserService();
            this.groupService = new GroupService();
        } catch (SQLException e) {
            e.printStackTrace();
            // Handle exception
        }
    }

    public String getUsername() {
        return currentUser != null ? currentUser.getUsername() : "Guest";
    }

    public void run() {
        try {
            out = new ObjectOutputStream(clientSocket.getOutputStream());
            in = new ObjectInputStream(clientSocket.getInputStream());

            NetworkMessage clientMessage;
            while ((clientMessage = (NetworkMessage) in.readObject()) != null) {
                handleMessage(clientMessage);
            }
        } catch (IOException | ClassNotFoundException e) {
            System.out.println("Client disconnected: " + clientSocket.getInetAddress());
        } finally {
            if (currentRoomId != -1) {
                Server.removeUserFromRoom(currentRoomId, this);
            }
            if (currentUser != null) {
                User userWithStatus = new User();
                userWithStatus.setId(currentUser.getId());
                userWithStatus.setOnline(false); // Đã offline

                Server.broadcastUserStatusUpdate(userWithStatus, this);
                Server.removeOnlineUser(currentUser.getId()); // THÊM DÒNG NÀY
            }
            try {
                clientSocket.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private void handleMessage(NetworkMessage message) {
        try {
            switch (message.getType()) {
                case LOGIN_REQUEST:
                    handleLogin((User) message.getPayload());
                    break;
                case REGISTER_REQUEST:
                    handleRegister((User) message.getPayload());
                    break;
                case CREATE_ROOM_REQUEST:
                    // handleCreateRoom((Room) message.getPayload());
                    handleCreateRoom((Room) message.getPayload());
                    break;
                case JOIN_ROOM_REQUEST:
                    handleJoinRoom((Room) message.getPayload());
                    break;
                case SEND_MESSAGE_REQUEST:
                    handleSendMessage((String) message.getPayload());
                    break;
                case LEAVE_ROOM_REQUEST:
                    handleLeaveRoom();
                    break;
                case GET_JOINED_GROUPS_REQUEST:
                    handleGetJoinedGroups();
                    break;
                case GET_ROOM_HISTORY_REQUEST:
                    handleGetRoomHistory((Integer) message.getPayload());
                    break;
                case GET_MEMBERS_GROUP_REQUEST:
                    handleGetMembersGroupList((Integer) message.getPayload());
                    break;
                case GET_USER_REQUEST:
                    handleGetUserRequest();
                    break;
                case JOIN_EXISTING_ROOM_REQUEST:
                    handleJoinExistingRoom((Integer) message.getPayload());
                    break;
                case BACK_HOME_REQUEST:
                    if (currentRoomId != -1) {
                        Server.removeUserFromRoom(currentRoomId, this);
                        currentRoomId = -1;
                        sendMessage(new NetworkMessage(NetworkMessage.MessageType.USER_LEFT_ROOM,
                                "You have left the room."));
                    } else {
                        sendMessage(new NetworkMessage(NetworkMessage.MessageType.ERROR_RESPONSE,
                                "You are not in any room."));
                    }
                    break;
                case REMOVE_MEMBER_REQUEST:
                    // Lấy payload là ID của người cần xóa
                    int memberIdToRemove = (Integer) message.getPayload();

                    // GỌI PHƯƠNG THỨC XỬ LÝ
                    handleRemoveMember(memberIdToRemove);
                    break;
                case CHANGE_PASSWORD_REQUEST:
                    Map<String, Object> payload = (Map<String, Object>) message.getPayload();
                    handleChangePassword(payload);
                    break;
                case UPDATE_FULLNAME_REQUEST:
                    handleUpdateFullName((Map<String, Object>) message.getPayload());
                    break;
                case UPDATE_GMAIL_REQUEST:
                    handleUpdateGmail((Map<String, Object>) message.getPayload());
                    break;
                case UPDATE_ROOM_NAME_REQUEST:
                    handleUpdateRoomName((Map<String, Object>) message.getPayload());
                    break;
                case UPDATE_ROOM_PASSWORD_REQUEST:
                    handleUpdateRoomPassword((Map<String, Object>) message.getPayload());
                    break;
                case SEARCH_ROOM_REQUEST:
                    handleSearchRoomRequest((String) message.getPayload());
                    break;
                case GET_UNREAD_COUNTS_REQUEST:
                    Map<Integer, Integer> unreadCounts = groupService.getUnreadCountsForUser(currentUser.getId());
                    sendMessage(
                            new NetworkMessage(NetworkMessage.MessageType.GET_UNREAD_COUNTS_RESPONSE, unreadCounts));
                    break;

                case MARK_MESSAGES_READ_REQUEST:
                    int roomIdToMarkRead = (Integer) message.getPayload();
                    groupService.markMessagesAsRead(currentUser.getId(), roomIdToMarkRead);
                    sendMessage(new NetworkMessage(NetworkMessage.MessageType.MARK_MESSAGES_READ_RESPONSE, true));
                    break;
                default:
                    // System.out.println("Received unknown message type: " + message.getType());
                    // // Có thể gửi thông báo lỗi về cho client
                    // sendMessage(new NetworkMessage(NetworkMessage.MessageType.ERROR_RESPONSE,
                    // "Unknown message type received."));
                    break;
            }
        } catch (SQLException e) {
            System.err.println("Error handling message from client: " + clientSocket.getInetAddress());
            e.printStackTrace(); // In ra lỗi để debug
            // Có thể gửi thông báo lỗi về cho client
            sendMessage(new NetworkMessage(NetworkMessage.MessageType.ERROR_RESPONSE,
                    "An internal server error occurred."));
        }
    }

    private void handleLogin(User user) throws SQLException {
        User loggedInUser = userService.login(user.getUsername(), user.getPassword());
        if (loggedInUser != null) {
            this.currentUser = loggedInUser;
            Server.addOnlineUser(loggedInUser.getId(), this);// THÊM DÒNG NÀY
            sendMessage(new NetworkMessage(NetworkMessage.MessageType.LOGIN_SUCCESS, loggedInUser));
            // THÔNG BÁO CHO CÁC CLIENT KHÁC
            User userWithStatus = new User();
            userWithStatus.setId(loggedInUser.getId());
            userWithStatus.setOnline(true); // Đã online
            Server.broadcastUserStatusUpdate(userWithStatus, this);
        } else {
            sendMessage(new NetworkMessage(NetworkMessage.MessageType.LOGIN_FAILURE, "Invalid username or password."));
        }
    }

    private void handleRegister(User user) throws SQLException {
        String result = userService.register(user.getUsername(), user.getPassword(), user.getGmail(),
                user.getFullName());
        if ("SUCCESS".equals(result)) {
            sendMessage(new NetworkMessage(NetworkMessage.MessageType.REGISTER_SUCCESS, "Registration successful."));
        } else {
            sendMessage(new NetworkMessage(NetworkMessage.MessageType.REGISTER_FAILURE, result));
        }
    }

    // private void handleCreateRoom(Room room) throws SQLException {
    // // Chỉ user đã đăng nhập mới được tạo phòng
    // if (currentUser == null) {
    // sendMessage(new NetworkMessage(NetworkMessage.MessageType.ERROR_RESPONSE,
    // "You must be logged in to create a room."));
    // return;
    // }
    // Room newRoom = groupService.createGroup(room.getName(), room.getPassword(),
    // currentUser.getId());
    // if (newRoom != null) {
    // this.currentRoomId = newRoom.getId();
    // Server.addUserToRoom(newRoom.getId(), this);
    // sendMessage(new NetworkMessage(NetworkMessage.MessageType.ROOM_CREATED,
    // newRoom));
    // } else {
    // sendMessage(new NetworkMessage(NetworkMessage.MessageType.ERROR_RESPONSE,
    // "Room name already exists."));
    // }
    // }

    private void handleCreateRoom(Room roomToCreate) throws SQLException {
        if (currentUser == null) {
            sendMessage(
                    new NetworkMessage(NetworkMessage.MessageType.ERROR_RESPONSE, "Bạn phải đăng nhập để tạo phòng."));
            return;
        }

        // Room newRoom = groupService.createGroupWithMembers(
        // roomToCreate.getName(),
        // roomToCreate.getPassword(),
        // currentUser.getId(),
        // roomToCreate.getMemberEmails() // Lấy danh sách email từ đối tượng Room
        // );
        Room newRoom = groupService.createGroup(
                roomToCreate.getName(),
                roomToCreate.getPassword(),
                currentUser.getId());

        if (newRoom != null) {
            this.currentRoomId = newRoom.getId();
            Server.addUserToRoom(newRoom.getId(), this);
            sendMessage(new NetworkMessage(NetworkMessage.MessageType.ROOM_CREATED, newRoom));
        } else {
            sendMessage(new NetworkMessage(NetworkMessage.MessageType.ERROR_RESPONSE,
                    "Tạo phòng thất bại."));
        }
    }

    private void handleJoinRoom(Room room) throws SQLException {
        if (currentUser == null) {
            sendMessage(new NetworkMessage(NetworkMessage.MessageType.ERROR_RESPONSE,
                    "You must be logged in to join a room."));
            return;
        }

        // yeu cau nhap id phong > 0
        if (room.getId() <= 0) {
            sendMessage(new NetworkMessage(NetworkMessage.MessageType.ROOM_JOIN_FAILED,
                    "Yêu cầu tham gia không hợp lệ (thiếu ID phòng)."));
            return;
        }

        Room joinedRoom = groupService.joinGroup(room.getId(), room.getPassword(), currentUser.getId());
        if (joinedRoom != null) {
            this.currentRoomId = joinedRoom.getId();
            Server.addUserToRoom(joinedRoom.getId(), this);

            sendMessage(new NetworkMessage(NetworkMessage.MessageType.ROOM_JOINED, joinedRoom));
            // Thông báo cho những người khác trong phòng
            Message notification = new Message(0, "System", joinedRoom.getId(),
                    currentUser.getUsername() + " has joined the room.");
            Server.broadcastMessage(joinedRoom.getId(),
                    new NetworkMessage(NetworkMessage.MessageType.RECEIVE_MESSAGE, notification), this);
            try {
                // Lấy danh sách member mới nhất từ DB (đã bao gồm người vừa join)
                List<User> updatedMembers = groupService.getMembersGroupList(joinedRoom.getId());

                // Cập nhật trạng thái online/offline cho danh sách này
                for (User member : updatedMembers) {
                    if (Server.onlineUsers.containsKey(member.getId())) {
                        member.setOnline(true);
                    }
                }

                // Gói vào message
                NetworkMessage memberListUpdateMsg = new NetworkMessage(
                        NetworkMessage.MessageType.MEMBERS_GROUP_RESPONSE,
                        updatedMembers);
                Server.broadcastToAllInRoom(joinedRoom.getId(), memberListUpdateMsg);

            } catch (SQLException e) {
                System.err.println("Error getting and broadcasting updated member list: " + e.getMessage());
            }
        } else {
            sendMessage(new NetworkMessage(NetworkMessage.MessageType.ROOM_JOIN_FAILED,
                    "Room not found or password incorrect."));
        }
    }

    private void handleSendMessage(String content) throws SQLException {
        if (currentUser == null || currentRoomId == -1) {
            sendMessage(new NetworkMessage(NetworkMessage.MessageType.ERROR_RESPONSE, "You are not in a room."));
            return;
        }

        // Kiểm tra nếu là file (content bắt đầu bằng FILE_PREFIX)
        if (content.startsWith("FILE:")) {
            String[] parts = content.split(":", 5);
            if (parts.length == 5) {
                String fileName = parts[1];
                String fileType = parts[2];
                int fileSize = Integer.parseInt(parts[3]);
                String base64Data = parts[4];

                // Chuyển base64 thành byte[]
                byte[] fileData = Base64.getDecoder().decode(base64Data);

                Message newMessage = groupService.saveFileMessage(
                        currentUser.getId(),
                        currentRoomId,
                        fileName,
                        fileType,
                        fileData);

                if (newMessage != null) {
                    List<User> members = groupService.getMembersGroupList(currentRoomId);
                    List<Integer> userIds = members.stream()
                            .map(User::getId)
                            .filter(id -> id != currentUser.getId()) // Không đánh dấu cho người gửi
                            .collect(Collectors.toList());

                    // Đánh dấu là chưa đọc cho các thành viên khác
                    groupService.markMessagesAsUnread(currentRoomId, userIds);
                    NetworkMessage broadcastMsg = new NetworkMessage(
                            NetworkMessage.MessageType.RECEIVE_MESSAGE,
                            newMessage);
                    Server.broadcastMessage(currentRoomId, broadcastMsg, null);

                    NetworkMessage notification = new NetworkMessage(
                            NetworkMessage.MessageType.NEW_MESSAGE_NOTIFICATION,
                            currentRoomId);
                    for (Map.Entry<Integer, ClientHandler> entry : Server.onlineUsers.entrySet()) {
                        ClientHandler client = entry.getValue();
                        if (client != this && client.getCurrentRoomId() != currentRoomId) {
                            client.sendMessage(notification);
                        }
                    }

                }
            }
        } else if (content.startsWith("@ai ")) {
            String question = content.substring(4).trim();

            // Lưu câu hỏi của người dùng
            Message userMsg = groupService.saveMessage(
                    currentUser.getId(),
                    currentRoomId,
                    content);

            if (userMsg != null) {
                // Lấy danh sách thành viên trong phòng
                List<User> members = groupService.getMembersGroupList(currentRoomId);
                List<Integer> userIds = members.stream()
                        .map(User::getId)
                        .filter(id -> id != currentUser.getId()) // Không đánh dấu cho người gửi
                        .collect(Collectors.toList());

                // Đánh dấu là chưa đọc cho các thành viên khác
                groupService.markMessagesAsUnread(currentRoomId, userIds);

                Server.broadcastMessage(currentRoomId, new NetworkMessage(
                        NetworkMessage.MessageType.RECEIVE_MESSAGE,
                        userMsg), null);
                NetworkMessage notification = new NetworkMessage(
                        NetworkMessage.MessageType.NEW_MESSAGE_NOTIFICATION,
                        currentRoomId);
                for (Map.Entry<Integer, ClientHandler> entry : Server.onlineUsers.entrySet()) {
                    ClientHandler client = entry.getValue();
                    if (client != this && client.getCurrentRoomId() != currentRoomId) {
                        client.sendMessage(notification);
                    }
                }
            }

            // Tạo thread riêng để gọi AI tránh block main thread
            new Thread(() -> {
                try {
                    AIService aiService = new AIService();
                    String aiAnswer = aiService.callLangflowAPI(question);

                    if (aiAnswer != null && !aiAnswer.isEmpty()) {
                        Message aiMsg = groupService.saveMessage(
                                -1,
                                currentRoomId,
                                aiAnswer);

                        if (aiMsg != null) {
                            aiMsg.setFullname("Langflow AI");
                            List<User> members = groupService.getMembersGroupList(currentRoomId);
                            List<Integer> userIds = members.stream()
                                    .map(User::getId)
                                    .filter(id -> id != currentUser.getId()) // Không đánh dấu cho người gửi
                                    .collect(Collectors.toList());

                            // Đánh dấu là chưa đọc cho các thành viên khác
                            groupService.markMessagesAsUnread(currentRoomId, userIds);
                            Server.broadcastMessage(currentRoomId, new NetworkMessage(
                                    NetworkMessage.MessageType.RECEIVE_MESSAGE,
                                    aiMsg), null);
                            NetworkMessage notification = new NetworkMessage(
                                    NetworkMessage.MessageType.NEW_MESSAGE_NOTIFICATION,
                                    currentRoomId);
                            for (Map.Entry<Integer, ClientHandler> entry : Server.onlineUsers.entrySet()) {
                                ClientHandler client = entry.getValue();
                                if (client != this && client.getCurrentRoomId() != currentRoomId) {
                                    client.sendMessage(notification);
                                }
                            }

                        }
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                    // Gửi thông báo lỗi về client
                    Message errorMsg = new Message(-1, "System", currentRoomId,
                            "Lỗi khi gọi AI: " + e.getMessage());
                    Server.broadcastMessage(currentRoomId, new NetworkMessage(
                            NetworkMessage.MessageType.RECEIVE_MESSAGE,
                            errorMsg), null);
                }
            }).start();
        }
        // Tin nhắn văn bản bình thường
        else {
            Message newMessage = groupService.saveMessage(
                    currentUser.getId(),
                    currentRoomId,
                    content);

            if (newMessage != null) {
                // Lấy danh sách thành viên trong phòng
                List<User> members = groupService.getMembersGroupList(currentRoomId);
                List<Integer> userIds = members.stream()
                        .map(User::getId)
                        .filter(id -> id != currentUser.getId()) // Không đánh dấu cho người gửi
                        .collect(Collectors.toList());

                // Đánh dấu là chưa đọc cho các thành viên khác
                groupService.markMessagesAsUnread(currentRoomId, userIds);

                NetworkMessage broadcastMsg = new NetworkMessage(
                        NetworkMessage.MessageType.RECEIVE_MESSAGE,
                        newMessage);
                Server.broadcastMessage(currentRoomId, broadcastMsg, null);

                NetworkMessage notification = new NetworkMessage(
                        NetworkMessage.MessageType.NEW_MESSAGE_NOTIFICATION,
                        currentRoomId);

                for (Map.Entry<Integer, ClientHandler> entry : Server.onlineUsers.entrySet()) {
                    ClientHandler client = entry.getValue();
                    if (client != this && client.getCurrentRoomId() != currentRoomId) {
                        client.sendMessage(notification);
                    }
                }
            }
        }
    }

    private void handleGetUserRequest() throws SQLException {
        User user = userService.getUserById(currentUser.getId());
        if (user != null) {
            sendMessage(new NetworkMessage(
                    NetworkMessage.MessageType.USER_RESPONSE,
                    user));
        } else {
            sendMessage(new NetworkMessage(
                    NetworkMessage.MessageType.ERROR_RESPONSE,
                    "User not found"));
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

    private void handleGetJoinedGroups() throws SQLException {
        if (currentUser == null) {
            sendMessage(new NetworkMessage(
                    NetworkMessage.MessageType.ERROR_RESPONSE,
                    "You must be logged in to view groups."));
            return;
        }

        List<Room> joinedGroups = groupService.getJoinedGroups(currentUser.getId());
        sendMessage(new NetworkMessage(
                NetworkMessage.MessageType.JOINED_GROUPS_RESPONSE,
                joinedGroups));
    }

    private void handleGetRoomHistory(int roomId) throws SQLException {
        if (currentUser == null) {
            sendMessage(new NetworkMessage(
                    NetworkMessage.MessageType.ERROR_RESPONSE,
                    "You must be logged in to view room history."));
            return;
        }

        List<Message> history = groupService.getRoomHistory(roomId, currentUser.getId());
        sendMessage(new NetworkMessage(
                NetworkMessage.MessageType.ROOM_HISTORY_RESPONSE,
                history));
    }

    private void handleGetMembersGroupList(int groupId) throws SQLException {
        if (currentUser == null) {
            sendMessage(new NetworkMessage(
                    NetworkMessage.MessageType.ERROR_RESPONSE,
                    "You must be logged in to view room history."));
            return;
        }
        List<User> members = groupService.getMembersGroupList(groupId);
        for (User member : members) {
            // Kiểm tra xem ID của thành viên có trong danh sách online của Server không
            if (Server.onlineUsers.containsKey(member.getId())) {
                member.setOnline(true);
            } else {
                member.setOnline(false);
            }
        }
        sendMessage(new NetworkMessage(
                NetworkMessage.MessageType.MEMBERS_GROUP_RESPONSE,
                members));
    }

    private void handleJoinExistingRoom(int roomId) throws SQLException {
        if (currentUser == null) {
            sendMessage(new NetworkMessage(
                    NetworkMessage.MessageType.ERROR_RESPONSE,
                    "You must be logged in to join a room."));
            return;
        }

        // Kiểm tra xem user đã ở trong phòng này chưa
        if (currentRoomId == roomId) {
            sendMessage(new NetworkMessage(
                    NetworkMessage.MessageType.ERROR_RESPONSE,
                    "You are already in this room."));
            return;
        }
        // Kiểm tra xem user đã ở trong phòng này chưa
        if (currentRoomId == roomId) {
            sendMessage(new NetworkMessage(
                    NetworkMessage.MessageType.ERROR_RESPONSE,
                    "You are already in this room."));
            return;
        }

        // Kiểm tra xem user có trong phòng không
        if (!groupService.isUserInRoom(currentUser.getId(), roomId)) {
            sendMessage(new NetworkMessage(
                    NetworkMessage.MessageType.ERROR_RESPONSE,
                    "You are not a member of this room."));
            return;
        }

        // Rời phòng hiện tại nếu đang ở trong phòng
        if (currentRoomId != -1) {
            Server.removeUserFromRoom(currentRoomId, this);

        }

        // Tham gia phòng mới
        Room room = groupService.getGroupById(roomId);
        if (room != null) {
            this.currentRoomId = roomId;
            Server.addUserToRoom(roomId, this);

            // Gửi phản hồi thành công với thông tin phòng
            sendMessage(new NetworkMessage(
                    NetworkMessage.MessageType.JOIN_EXISTING_ROOM_RESPONSE,
                    room));

        } else {
            sendMessage(new NetworkMessage(
                    NetworkMessage.MessageType.ERROR_RESPONSE,
                    "Room not found."));
        }
    }

    private void handleLeaveRoom() throws SQLException {
        // Kiểm tra xem user có đang ở trong phòng nào không
        if (currentRoomId == -1) {
            sendMessage(new NetworkMessage(MessageType.ERROR_RESPONSE, "Bạn không ở trong phòng nào cả."));
            return;
        }

        int roomToLeaveId = this.currentRoomId;

        // Kiểm tra xem người dùng có phải là leader của phòng này không
        boolean isLeader = groupService.isUserLeaderOfGroup(currentUser.getId(), roomToLeaveId);

        if (isLeader) {
            // --- LOGIC KHI LEADER RỜI ĐI ---

            // 1. Xóa phòng khỏi database
            boolean deleted = groupService.deleteGroup(roomToLeaveId);

            if (deleted) {
                // 2. Thông báo cho tất cả thành viên (bao gồm cả leader) rằng phòng đã bị giải
                // tán
                String notificationContent = "Trưởng phòng (" + currentUser.getUsername()
                        + ") đã rời đi. Phòng đã được giải tán.";
                Message notificationMsg = new Message(0, "System", roomToLeaveId, notificationContent);
                NetworkMessage broadcastMsg = new NetworkMessage(MessageType.ROOM_DELETED, notificationMsg);

                // Broadcast cho tất cả client trong phòng trước khi xóa phòng khỏi Server
                Server.broadcastMessage(roomToLeaveId, broadcastMsg, null);

                // 3. Xóa phòng khỏi bộ nhớ của server
                Server.removeAllUsersFromRoom(roomToLeaveId);

                // Cập nhật trạng thái của ClientHandler này
                this.currentRoomId = -1;

                // Ghi chú: Không cần gửi thêm tin nhắn riêng cho leader,
                // vì leader cũng nhận được tin nhắn broadcast ROOM_DELETED.

            } else {
                // Có lỗi khi xóa phòng
                sendMessage(new NetworkMessage(MessageType.ERROR_RESPONSE, "Lỗi: không thể xóa phòng."));
            }

        } else {
            // --- LOGIC KHI THÀNH VIÊN THƯỜNG RỜI ĐI ---

            // 1. Xóa user khỏi phòng trong Server và DB
            Server.removeUserFromRoom(roomToLeaveId, this); // Xóa khỏi bộ nhớ server
            groupService.removeUserFromGroup(currentUser.getId(), roomToLeaveId); // Xóa khỏi DB

            // 2. Thông báo cho bản thân user đã rời phòng thành công
            sendMessage(new NetworkMessage(MessageType.USER_LEFT_ROOM, "Bạn đã rời khỏi phòng."));

            // 3. Cập nhật danh sách thành viên trong phòng
            List<User> updatedMembers = groupService.getMembersGroupList(roomToLeaveId);

            for (User member : updatedMembers) {
                if (Server.onlineUsers.containsKey(member.getId())) {
                    member.setOnline(true);
                }
            }

            NetworkMessage memberListUpdateMsg = new NetworkMessage(
                    NetworkMessage.MessageType.MEMBERS_GROUP_RESPONSE,
                    updatedMembers);

            // Gửi danh sách mới cho TẤT CẢ mọi người đang online trong phòng
            Server.broadcastToAllInRoom(roomToLeaveId, memberListUpdateMsg);

            // 4. Thông báo cho những người còn lại trong phòng
            String notificationContent = currentUser.getUsername() + " đã rời khỏi phòng.";
            Message notificationMsg = new Message(0, "System", roomToLeaveId, notificationContent);
            NetworkMessage broadcastMsg = new NetworkMessage(MessageType.RECEIVE_MESSAGE, notificationMsg);

            Server.broadcastMessage(roomToLeaveId, broadcastMsg, this); // Gửi cho mọi người trừ người vừa rời

            // 5. Cập nhật trạng thái của ClientHandler
            this.currentRoomId = -1;
        }
    }

    private void handleRemoveMember(int memberIdToRemove) throws SQLException {
        if (currentRoomId == -1) {
            sendMessage(new NetworkMessage(NetworkMessage.MessageType.ERROR_RESPONSE, "Bạn không ở trong phòng nào."));
            return;
        }

        boolean success = groupService.removeMemberFromGroup(memberIdToRemove, currentRoomId, currentUser.getId());

        if (success) {
            // 1. Báo thành công cho leader
            sendMessage(new NetworkMessage(MessageType.MEMBER_REMOVED_SUCCESS, "Đã xóa thành viên."));

            // 2. Gửi danh sách thành viên mới cho tất cả mọi người trong phòng
            List<User> updatedMembers = groupService.getMembersGroupList(currentRoomId);

            for (User member : updatedMembers) {
                if (Server.onlineUsers.containsKey(member.getId())) {
                    member.setOnline(true);
                }
            }

            NetworkMessage memberListUpdateMsg = new NetworkMessage(
                    NetworkMessage.MessageType.MEMBERS_GROUP_RESPONSE,
                    updatedMembers);

            // Gửi danh sách mới cho TẤT CẢ mọi người đang online trong phòng
            Server.broadcastToAllInRoom(currentRoomId, memberListUpdateMsg);

            // 3. Tìm ClientHandler của người bị xóa và báo cho họ
            Server.notifyUserRemoved(memberIdToRemove, currentRoomId);

        } else {
            sendMessage(new NetworkMessage(MessageType.ERROR_RESPONSE,
                    "Không thể xóa thành viên. Bạn không có quyền hoặc người dùng không tồn tại."));
        }
    }

    /// code sua thong tin
    private void handleChangePassword(Map<String, Object> payload) {

        try {
            int userId = (int) payload.get("userId");
            String newPassword = (String) payload.get("newPassword");

            if (currentUser == null || currentUser.getId() != userId) {
                sendMessage(new NetworkMessage(
                        NetworkMessage.MessageType.CHANGE_PASSWORD_FAILURE,
                        "Không có quyền thay đổi mật khẩu"));
                return;
            }

            boolean success = userService.changePassword(userId, newPassword);
            if (success) {
                currentUser.setPassword(newPassword);
                sendMessage(new NetworkMessage(
                        NetworkMessage.MessageType.CHANGE_PASSWORD_SUCCESS,
                        "Đổi mật khẩu thành công"));
            } else {
                sendMessage(new NetworkMessage(
                        NetworkMessage.MessageType.CHANGE_PASSWORD_FAILURE,
                        "Đổi mật khẩu thất bại"));
            }
        } catch (SQLException e) {
            sendMessage(new NetworkMessage(
                    NetworkMessage.MessageType.CHANGE_PASSWORD_FAILURE,
                    "Lỗi cơ sở dữ liệu: " + e.getMessage()));
        } catch (Exception e) {
            sendMessage(new NetworkMessage(
                    NetworkMessage.MessageType.CHANGE_PASSWORD_FAILURE,
                    "Lỗi hệ thống: " + e.getMessage()));
        }
    }

    private void handleUpdateFullName(Map<String, Object> payload) {
        try {
            int userId = (int) payload.get("userId");
            String newFullName = (String) payload.get("newFullName");

            if (currentUser == null || currentUser.getId() != userId) {
                sendMessage(new NetworkMessage(
                        NetworkMessage.MessageType.UPDATE_FULLNAME_FAILURE,
                        "Không có quyền thay đổi tên hiển thị"));
                return;
            }

            boolean success = userService.updateFullName(userId, newFullName);
            if (success) {
                currentUser.setFullName(newFullName);
                sendMessage(new NetworkMessage(
                        NetworkMessage.MessageType.UPDATE_FULLNAME_SUCCESS,
                        "Đổi tên hiển thị thành công"));
            } else {
                sendMessage(new NetworkMessage(
                        NetworkMessage.MessageType.UPDATE_FULLNAME_FAILURE,
                        "Đổi tên hiển thị thất bại"));
            }
        } catch (SQLException e) {
            sendMessage(new NetworkMessage(
                    NetworkMessage.MessageType.UPDATE_FULLNAME_FAILURE,
                    "Lỗi cơ sở dữ liệu: " + e.getMessage()));
        }

    }

    // sua Gmail
    private void handleUpdateGmail(Map<String, Object> payload) {
        try {
            int userId = (int) payload.get("userId");
            String newGmail = (String) payload.get("newGmail");

            if (currentUser == null || currentUser.getId() != userId) {
                sendMessage(new NetworkMessage(
                        NetworkMessage.MessageType.UPDATE_GMAIL_FAILURE,
                        "Không có quyền thay đổi Gmail"));
                return;
            }

            boolean success = userService.updateGmail(userId, newGmail);
            if (success) {
                currentUser.setGmail(newGmail);
                sendMessage(new NetworkMessage(
                        NetworkMessage.MessageType.UPDATE_GMAIL_SUCCESS,
                        "Đổi Gmail thành công"));
            } else {
                sendMessage(new NetworkMessage(
                        NetworkMessage.MessageType.UPDATE_GMAIL_FAILURE,
                        "Đổi Gmail thất bại (có thể Gmail đã tồn tại)"));
            }
        } catch (SQLException e) {
            sendMessage(new NetworkMessage(
                    NetworkMessage.MessageType.UPDATE_GMAIL_FAILURE,
                    "Lỗi cơ sở dữ liệu: " + e.getMessage()));
        }
    }

    // sua ten phong
    private void handleUpdateRoomName(Map<String, Object> payload) {
        try {
            int roomId = (int) payload.get("roomId");
            String newName = (String) payload.get("newName");
            int leaderId = (int) payload.get("leaderId");

            if (groupService.updateRoomName(roomId, newName, leaderId)) {
                Room updatedRoom = groupService.getGroupById(roomId); // Lấy thông tin phòng đã cập nhật
                sendMessage(new NetworkMessage(
                        NetworkMessage.MessageType.UPDATE_ROOM_NAME_SUCCESS,
                        updatedRoom));

                // Broadcast cho tất cả thành viên trong phòng
                Server.broadcastMessage(roomId,
                        new NetworkMessage(
                                NetworkMessage.MessageType.UPDATE_ROOM_NAME_SUCCESS,
                                updatedRoom),
                        this);
            }
        } catch (SQLException e) {
            // Xử lý lỗi...
        }
    }

    // sua pass phong
    private void handleUpdateRoomPassword(Map<String, Object> payload) {
        try {
            int roomId = (int) payload.get("roomId");
            String newPassword = (String) payload.get("newPassword");
            int leaderId = (int) payload.get("leaderId");

            boolean success = groupService.updateRoomPassword(roomId, newPassword, leaderId);
            if (success) {
                // Trả về Room đã cập nhật để client cập nhật UI
                Room updatedRoom = groupService.getGroupById(roomId);
                sendMessage(new NetworkMessage(
                        NetworkMessage.MessageType.UPDATE_ROOM_PASSWORD_SUCCESS,
                        updatedRoom));

                // Thông báo cho các thành viên khác (nếu cần)
                NetworkMessage broadcastMsg = new NetworkMessage(
                        NetworkMessage.MessageType.UPDATE_ROOM_PASSWORD_SUCCESS,
                        updatedRoom);
                Server.broadcastMessage(roomId, broadcastMsg, this);
            } else {
                sendMessage(new NetworkMessage(
                        NetworkMessage.MessageType.UPDATE_ROOM_PASSWORD_FAILURE,
                        "Cập nhật mật khẩu thất bại"));
            }
        } catch (SQLException e) {
            sendMessage(new NetworkMessage(
                    NetworkMessage.MessageType.UPDATE_ROOM_PASSWORD_FAILURE,
                    "Lỗi database: " + e.getMessage()));
        }
    }

    private void handleSearchRoomRequest(String keyword) throws SQLException {
        if (currentUser == null) {
            sendMessage(new NetworkMessage(
                    NetworkMessage.MessageType.ERROR_RESPONSE,
                    "You must be logged in to search rooms."));
            return;
        }

        List<Room> searchResults = groupService.searchRooms(keyword, currentUser.getId());
        sendMessage(new NetworkMessage(
                NetworkMessage.MessageType.SEARCH_ROOM_RESPONSE,
                searchResults));
    }

}
