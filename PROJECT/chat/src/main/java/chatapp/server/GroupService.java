// TẠO FILE MỚI: src/main/java/chatapp/server/GroupService.java
package chatapp.server;

import java.math.RoundingMode;
import java.sql.*;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.TimeZone;

import chatapp.model.Message;
import chatapp.model.Room;
import chatapp.model.User;
import chatapp.service.DBConfig;
import chatapp.server.UserService;

public class GroupService {
    private Connection connection;

    public GroupService() throws SQLException {
        this.connection = DBConfig.getConnection();
    }

    public Room createGroup(String name, String password, int leaderId) throws SQLException {
        if (getGroupByName(name) != null)
            return null; // Group đã tồn tại

        String sqlGroup = "INSERT INTO `Groups` (name, password, leader_id) VALUES (?, ?, ?)";
        String sqlUserGroup = "INSERT INTO User_Group (user_id, group_id) VALUES (?, ?)";

        connection.setAutoCommit(false);
        try (PreparedStatement stmtGroup = connection.prepareStatement(sqlGroup, Statement.RETURN_GENERATED_KEYS);
             PreparedStatement stmtUserGroup = connection.prepareStatement(sqlUserGroup)) {

            stmtGroup.setString(1, name);
            stmtGroup.setString(2, password);
            stmtGroup.setInt(3, leaderId);
            stmtGroup.executeUpdate();

            ResultSet generatedKeys = stmtGroup.getGeneratedKeys();
            if (generatedKeys.next()) {
                int groupId = generatedKeys.getInt(1);

                stmtUserGroup.setInt(1, leaderId);
                stmtUserGroup.setInt(2, groupId);
                stmtUserGroup.executeUpdate();

                connection.commit();
                //Room newRoom = new Room(name, password);
                Room newRoom = new Room(name, password, leaderId);
                newRoom.setId(groupId);
                return newRoom;
            }
        } catch (SQLException e) {
            connection.rollback();
            throw e;
        } finally {
            connection.setAutoCommit(true);
        }
        return null;
    }

//    public Room joinGroup(String name, String password, int userId) throws SQLException {
//        Room room = getGroupByName(name);
//        if (room == null || !room.getPassword().equals(password)) {
//            return null; // Không tìm thấy phòng hoặc sai mật khẩu
//        }
//
//        // Thêm user vào bảng User_Group
//        String sql = "INSERT IGNORE INTO User_Group (user_id, group_id) VALUES (?, ?)";
//        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
//            stmt.setInt(1, userId);
//            stmt.setInt(2, room.getId());
//            stmt.executeUpdate();
//            return room;
//        }
//    }

    public Room joinGroup(String name, String password, int userId) throws SQLException {
        // Lấy thông tin phòng kèm leader_id từ database
        String sql = "SELECT g.id, g.name, g.password, g.leader_id FROM `Groups` g WHERE g.name = ?";
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setString(1, name);
            ResultSet rs = stmt.executeQuery();

            if (rs.next()) {
                // Kiểm tra mật khẩu
                String dbPassword = rs.getString("password");
                if (!dbPassword.equals(password)) {
                    return null; // Sai mật khẩu
                }

                // Tạo đối tượng Room với đầy đủ thông tin
                Room room = new Room();
                room.setId(rs.getInt("id"));
                room.setName(rs.getString("name"));
                room.setPassword(dbPassword);
                room.setLeaderId(rs.getInt("leader_id")); // Lấy leader_id từ database

                // Thêm user vào phòng nếu chưa tham gia
                String joinSql = "INSERT IGNORE INTO User_Group (user_id, group_id) VALUES (?, ?)";
                try (PreparedStatement joinStmt = connection.prepareStatement(joinSql)) {
                    joinStmt.setInt(1, userId);
                    joinStmt.setInt(2, room.getId());
                    joinStmt.executeUpdate();
                }

                return room;
            }
        }
        return null; // Không tìm thấy phòng
    }
//
//    public Message saveMessage(int userId,  int groupId, String content) throws SQLException {
//        String sql = "INSERT INTO Messages (user_id, group_id, content) VALUES (?, ?, ?)";
//        try (PreparedStatement stmt = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
//            stmt.setInt(1, userId);
//            stmt.setInt(2, groupId);
//            stmt.setString(3, content);
//            stmt.executeUpdate();
//
//            ResultSet rs = stmt.getGeneratedKeys();
//            if (rs.next()) {
//                UserService userService = new UserService();
//                User sender = userService.getUserById(userId);
//                String fullname = (sender != null && sender.getFullName() != null)
//                        ? sender.getFullName()
//                        : "Unknown";
//                Message msg = new Message(userId, fullname, groupId, content);
//                msg.setId(rs.getInt(1));
//                // Lấy thời gian từ DB sẽ chính xác hơn, nhưng tạm thời dùng client time
//                msg.setSendAt(java.time.LocalDateTime.now());
//                return msg;
//            }
//        }
//        return null;
//    }

    public Message saveMessage(int userId, int groupId, String content) throws SQLException {
        String sql = "INSERT INTO Messages (user_id, group_id, content) VALUES (?, ?, ?)";
        try (PreparedStatement stmt = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            stmt.setInt(1, userId);
            stmt.setInt(2, groupId);
            stmt.setString(3, content);
            stmt.executeUpdate();

            ResultSet rs = stmt.getGeneratedKeys();
            if (rs.next()) {
                UserService userService = new UserService();
                User sender = userService.getUserById(userId);
                String fullname = (sender != null && sender.getFullName() != null)
                        ? sender.getFullName()
                        : "Unknown";
                Message msg = new Message(userId, fullname, groupId, content);
                msg.setId(rs.getInt(1));
                msg.setSendAt(java.time.LocalDateTime.now());
                return msg;
            }
        }
        return null;
    }

    public Message saveFileMessage(int userId, int groupId, String fileName, String fileType, byte[] fileData) throws SQLException {
        String sql = "INSERT INTO Messages (user_id, group_id, file_name, file_type, file_data) VALUES (?, ?, ?, ?, ?)";
        try (PreparedStatement stmt = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            stmt.setInt(1, userId);
            stmt.setInt(2, groupId);
            stmt.setString(3, fileName);
            stmt.setString(4, fileType);
            stmt.setBytes(5, fileData);
            stmt.executeUpdate();

            ResultSet rs = stmt.getGeneratedKeys();
            if (rs.next()) {
                UserService userService = new UserService();
                User sender = userService.getUserById(userId);
                String fullname = (sender != null && sender.getFullName() != null)
                        ? sender.getFullName()
                        : "Unknown";
                Message msg = new Message(userId, fullname, groupId, fileName, fileType, fileData);
                msg.setId(rs.getInt(1));
                msg.setSendAt(java.time.LocalDateTime.now());
                return msg;
            }
        }
        return null;
    }

    private Room getGroupByName(String name) throws SQLException {
        String sql = "SELECT * FROM `Groups` WHERE name = ?";
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setString(1, name);
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                Room room = new Room(rs.getString("name"), rs.getString("password"));
                room.setId(rs.getInt("id"));
                room.setLeaderId(rs.getInt("leader_id"));
                return room;
            }
        }
        return null;
    }
    public Room getGroupById(int id) throws SQLException {
        String sql = "SELECT * FROM `Groups` WHERE id = ?";
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setInt(1, id);
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                Room room = new Room(rs.getString("name"), rs.getString("password"));
                room.setId(rs.getInt("id"));
                room.setLeaderId(rs.getInt("leader_id"));
                return room;
            }
        }
        return null;
    }
    public boolean isUserInRoom(int userId, int groupId) throws SQLException {
        // Kiểm tra xem user có trong phòng không
        String sql = "SELECT 1 FROM user_group WHERE user_id = ? AND group_id = ?";
        try (Connection conn = DBConfig.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, userId);
            stmt.setInt(2, groupId);
            return stmt.executeQuery().next();
        }
    }

    public List<Room> getJoinedGroups(int userId) throws SQLException {
        List<Room> groups = new ArrayList<>();
        String sql = """
        SELECT g.id, g.name, g.password
        FROM `groups` g
        JOIN user_group ug ON g.id = ug.group_id
        WHERE ug.user_id = ?
    """;

        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setInt(1, userId);
            ResultSet rs = stmt.executeQuery();
            while (rs.next()) {
                Room room = new Room();
                room.setId(rs.getInt("id"));
                room.setName(rs.getString("name"));
                room.setPassword(rs.getString("password")); // hoặc bỏ nếu không cần gửi về client
                groups.add(room);
            }
        }
        return groups;
    }


    public List<Message> getRoomHistory(int roomId, int currentUserId) throws SQLException {
        List<Message> messages = new ArrayList<>();
        String sql = """
        SELECT m.id, m.user_id, u.fullname, m.group_id, m.content, 
               m.file_name, m.file_type, m.file_data, m.send_at
        FROM messages m
        JOIN users u ON m.user_id = u.id
        WHERE m.group_id = ?
        ORDER BY m.send_at ASC
        """;

        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setInt(1, roomId);
            ResultSet rs = stmt.executeQuery();

            while (rs.next()) {
                Message message;
                // Kiểm tra xem có phải là file không (dựa vào file_name hoặc file_data)
                if (rs.getString("file_name") != null) {
                    message = new Message(
                            rs.getInt("user_id"),
                            rs.getString("fullname"),
                            rs.getInt("group_id"),
                            rs.getString("file_name"),
                            rs.getString("file_type"),
                            rs.getBytes("file_data")
                    );
                    message.setFile(true); // <-- QUAN TRỌNG: Đánh dấu là tin nhắn file
                } else {
                    message = new Message(
                            rs.getInt("user_id"),
                            rs.getString("fullname"),
                            rs.getInt("group_id"),
                            rs.getString("content")
                    );
                }
                message.setId(rs.getInt("id"));
                Timestamp ts = rs.getTimestamp("send_at", Calendar.getInstance(TimeZone.getTimeZone("Asia/Ho_Chi_Minh")));
                message.setSendAt(ts.toLocalDateTime());
                messages.add(message);
            }
        }
        return messages;
    }

    public List<User> getMembersGroupList(int groupId) throws SQLException{
        List<User> members =new ArrayList<>();
        String sql= """
                SELECT u.id, u.username, u.fullname
                FROM users u
                JOIN user_group ug ON u.id=ug.user_id
                WHERE ug.group_id = ?
                """;
        try(PreparedStatement stmt= connection.prepareStatement(sql)){
            stmt.setInt(1,groupId);
            ResultSet rs=stmt.executeQuery();
            while (rs.next()) {
                User user = new User();
                user.setId(rs.getInt("id"));
                user.setUsername(rs.getString("username"));
                user.setFullName(rs.getString("fullname"));
                members.add(user);
            }
        }
        return members;
    }

    // GroupService.java
    public boolean updateRoomName(int roomId, String newName, int leaderId) throws SQLException {
        String checkLeaderSql = "SELECT leader_id FROM `Groups` WHERE id = ?";
        try (PreparedStatement checkStmt = connection.prepareStatement(checkLeaderSql)) {
            checkStmt.setInt(1, roomId);
            ResultSet rs = checkStmt.executeQuery();
            if (rs.next() && rs.getInt("leader_id") != leaderId) {
                return false; // Không phải leader
            }
        }

        String updateSql = "UPDATE `Groups` SET name = ? WHERE id = ?";
        try (PreparedStatement stmt = connection.prepareStatement(updateSql)) {
            stmt.setString(1, newName);
            stmt.setInt(2, roomId);
            return stmt.executeUpdate() > 0;
        }
    }
    // sua pass phong
    public boolean updateRoomPassword(int roomId, String newPassword, int leaderId) throws SQLException {
        // Kiểm tra quyền chủ phòng
        String checkLeaderSql = "SELECT leader_id FROM `Groups` WHERE id = ?";
        try (PreparedStatement checkStmt = connection.prepareStatement(checkLeaderSql)) {
            checkStmt.setInt(1, roomId);
            ResultSet rs = checkStmt.executeQuery();
            if (rs.next() && rs.getInt("leader_id") != leaderId) {
                return false;
            }
        }

        // Cập nhật mật khẩu mới
        String updateSql = "UPDATE `Groups` SET password = ? WHERE id = ?";
        try (PreparedStatement stmt = connection.prepareStatement(updateSql)) {
            stmt.setString(1, newPassword);
            stmt.setInt(2, roomId);
            return stmt.executeUpdate() > 0;
        }
    }
    public List<Room> searchRooms(String keyword, int userId) throws SQLException {
        String sql = """
        SELECT g.id, g.name, g.password 
        FROM `groups` g
        JOIN user_group ug ON g.id = ug.group_id
        WHERE ug.user_id = ? AND g.name LIKE ?
    """;

        List<Room> results = new ArrayList<>();
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setInt(1, userId);
            stmt.setString(2, "%" + keyword + "%");

            ResultSet rs = stmt.executeQuery();
            while (rs.next()) {
                Room room = new Room();
                room.setId(rs.getInt("id"));
                room.setName(rs.getString("name"));
                room.setPassword(rs.getString("password"));
                results.add(room);
            }
        }
        return results;
    }

}