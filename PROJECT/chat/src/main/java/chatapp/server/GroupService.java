// TẠO FILE MỚI: src/main/java/chatapp/server/GroupService.java
package chatapp.server;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.TimeZone;

import chatapp.model.Message;
import chatapp.model.Room;
import chatapp.model.User;
import chatapp.service.DBConfig;

public class GroupService {
    private Connection connection;

    public GroupService() throws SQLException {
        this.connection = DBConfig.getConnection();
    }

    public Room createGroup(String name, String password, int leaderId) throws SQLException {
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

                // tư động thêm người mới vào phòng
                stmtUserGroup.setInt(1, leaderId);
                stmtUserGroup.setInt(2, groupId);
                stmtUserGroup.executeUpdate();

                connection.commit();
                // Room newRoom = new Room(name, password);
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

    // public Room joinGroup(String name, String password, int userId) throws
    // SQLException {
    // Room room = getGroupByName(name);
    // if (room == null || !room.getPassword().equals(password)) {
    // return null; // Không tìm thấy phòng hoặc sai mật khẩu
    // }
    //
    // // Thêm user vào bảng User_Group
    // String sql = "INSERT IGNORE INTO User_Group (user_id, group_id) VALUES (?,
    // ?)";
    // try (PreparedStatement stmt = connection.prepareStatement(sql)) {
    // stmt.setInt(1, userId);
    // stmt.setInt(2, room.getId());
    // stmt.executeUpdate();
    // return room;
    // }
    // }

    public Room joinGroup(int groupId, String password, int userId) throws SQLException {
        // Lấy thông tin phòng kèm leader_id từ database
        String sql = "SELECT g.id, g.name, g.password, g.leader_id FROM `Groups` g WHERE g.id = ?";
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setInt(1, groupId);
            ResultSet rs = stmt.executeQuery();

            if (rs.next()) {
                // Kiểm tra mật khẩu
                String dbPassword = rs.getString("password");
                if (dbPassword != null && dbPassword.equals(password)) {
                    // Thêm user vào phòng nếu chưa tham gia
                    String joinSql = "INSERT IGNORE INTO User_Group (user_id, group_id) VALUES (?, ?)";
                    try (PreparedStatement joinStmt = connection.prepareStatement(joinSql)) {
                        joinStmt.setInt(1, userId);
                        joinStmt.setInt(2, groupId);
                        joinStmt.executeUpdate();
                    }

                    // Tạo đối tượng Room với đầy đủ thông tin
                    Room room = new Room(rs.getString("name"), null);
                    room.setId(rs.getInt("id"));
                    room.setLeaderId(rs.getInt("leader_id")); // Lấy leader_id từ database

                    return room;

                }
            }
        }
        return null; // Không tìm thấy phòng
    }
    //
    // public Message saveMessage(int userId, int groupId, String content) throws
    // SQLException {
    // String sql = "INSERT INTO Messages (user_id, group_id, content) VALUES (?, ?,
    // ?)";
    // try (PreparedStatement stmt = connection.prepareStatement(sql,
    // Statement.RETURN_GENERATED_KEYS)) {
    // stmt.setInt(1, userId);
    // stmt.setInt(2, groupId);
    // stmt.setString(3, content);
    // stmt.executeUpdate();
    //
    // ResultSet rs = stmt.getGeneratedKeys();
    // if (rs.next()) {
    // UserService userService = new UserService();
    // User sender = userService.getUserById(userId);
    // String fullname = (sender != null && sender.getFullName() != null)
    // ? sender.getFullName()
    // : "Unknown";
    // Message msg = new Message(userId, fullname, groupId, content);
    // msg.setId(rs.getInt(1));
    // // Lấy thời gian từ DB sẽ chính xác hơn, nhưng tạm thời dùng client time
    // msg.setSendAt(java.time.LocalDateTime.now());
    // return msg;
    // }
    // }
    // return null;
    // }

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

    public Message saveFileMessage(int userId, int groupId, String fileName, String fileType, byte[] fileData)
            throws SQLException {
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
                            rs.getBytes("file_data"));
                    message.setFile(true); // <-- QUAN TRỌNG: Đánh dấu là tin nhắn file
                } else {
                    message = new Message(
                            rs.getInt("user_id"),
                            rs.getString("fullname"),
                            rs.getInt("group_id"),
                            rs.getString("content"));
                }
                message.setId(rs.getInt("id"));
                Timestamp ts = rs.getTimestamp("send_at",
                        Calendar.getInstance(TimeZone.getTimeZone("Asia/Ho_Chi_Minh")));
                message.setSendAt(ts.toLocalDateTime());
                messages.add(message);
            }
        }
        return messages;
    }

    public List<User> getMembersGroupList(int groupId) throws SQLException {
        List<User> members = new ArrayList<>();
        String sql = """
                SELECT u.id, u.username, u.fullname
                FROM users u
                JOIN user_group ug ON u.id=ug.user_id
                WHERE ug.group_id = ?
                """;
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setInt(1, groupId);
            ResultSet rs = stmt.executeQuery();
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

    public String getGroupPassword(int groupId) throws SQLException {
        String sql = "SELECT password FROM `Groups` WHERE id = ?";
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setInt(1, groupId);
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                return rs.getString("password");
            }
        }
        return null;
    }

    // Thêm phương thức mới: thêm thành viên bằng email
    // public Room createGroupWithMembers(String name, String password, int
    // leaderId, List<String> memberEmails)
    // throws SQLException {
    // Room newRoom = null;

    // connection.setAutoCommit(false);
    // try {
    // // 1. Tạo phòng và lấy ID
    // String sqlGroup = "INSERT INTO `Groups` (name, password, leader_id) VALUES
    // (?, ?, ?)";
    // PreparedStatement stmtGroup = connection.prepareStatement(sqlGroup,
    // Statement.RETURN_GENERATED_KEYS);
    // stmtGroup.setString(1, name);
    // stmtGroup.setString(2, password);
    // stmtGroup.setInt(3, leaderId);
    // stmtGroup.executeUpdate();

    // ResultSet generatedKeys = stmtGroup.getGeneratedKeys();
    // if (generatedKeys.next()) {
    // int groupId = generatedKeys.getInt(1);

    // // 2. Thêm người tạo vào phòng
    // String sqlAddLeader = "INSERT INTO User_Group (user_id, group_id) VALUES (?,
    // ?)";
    // PreparedStatement stmtAddLeader = connection.prepareStatement(sqlAddLeader);
    // stmtAddLeader.setInt(1, leaderId);
    // stmtAddLeader.setInt(2, groupId);
    // stmtAddLeader.executeUpdate();

    // // 3. Thêm các thành viên khác dựa trên email
    // if (memberEmails != null && !memberEmails.isEmpty()) {
    // // Chuyển đổi danh sách email thành chuỗi (?,?,?) để dùng trong câu lệnh IN
    // String placeholders = String.join(",",
    // Collections.nCopies(memberEmails.size(), "?"));
    // String sqlAddMembers = "INSERT IGNORE INTO User_Group (user_id, group_id) " +
    // "SELECT id, ? FROM Users WHERE gmail IN (" + placeholders + ")";

    // PreparedStatement stmtAddMembers =
    // connection.prepareStatement(sqlAddMembers);
    // stmtAddMembers.setInt(1, groupId);
    // for (int i = 0; i < memberEmails.size(); i++) {
    // stmtAddMembers.setString(i + 2, memberEmails.get(i));
    // }
    // stmtAddMembers.executeUpdate();
    // }

    // newRoom = new Room(name, password, leaderId);
    // newRoom.setId(groupId);
    // }

    // connection.commit();
    // } catch (SQLException e) {
    // connection.rollback();
    // throw e;
    // } finally {
    // connection.setAutoCommit(true);
    // }
    // return newRoom;
    // }

    // kiểm tra user có phải là leader không
    public boolean isUserLeaderOfGroup(int userId, int groupId) throws SQLException {
        String sql = "SELECT leader_id FROM `Groups` WHERE id = ? AND leader_id = ?";
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setInt(1, groupId);
            stmt.setInt(2, userId);
            // Nếu có kết quả trả về, tức là user chính là leader
            return stmt.executeQuery().next();
        }
    }

    public boolean deleteGroup(int groupId) throws SQLException {
        String sql = "DELETE FROM `Groups` WHERE id = ?";
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setInt(1, groupId);
            // executeUpdate() trả về số dòng bị ảnh hưởng.
            // Nếu > 0, tức là đã xóa thành công.
            return stmt.executeUpdate() > 0;
        }
    }

    public boolean removeUserFromGroup(int userId, int groupId) throws SQLException {
        String sql = "DELETE FROM User_Group WHERE user_id = ? AND group_id = ?";
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setInt(1, userId);
            stmt.setInt(2, groupId);
            return stmt.executeUpdate() > 0;
        }
    }

    // Phương thức này dùng để leader xóa thành viên khỏi nhóm
    public boolean removeMemberFromGroup(int userIdToRemove, int groupId, int requestingUserId) throws SQLException {
        // 1. Kiểm tra quyền: người yêu cầu phải là leader
        if (!isUserLeaderOfGroup(requestingUserId, groupId)) {
            return false; // Không có quyền
        }

        // 2. Không cho phép leader tự xóa mình bằng chức năng này
        if (userIdToRemove == requestingUserId) {
            return false; // Leader phải dùng chức năng "Rời phòng"
        }

        // 3. Thực hiện xóa
        String sql = "DELETE FROM User_Group WHERE user_id = ? AND group_id = ?";
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setInt(1, userIdToRemove);
            stmt.setInt(2, groupId);
            return stmt.executeUpdate() > 0;
        }
    }
}