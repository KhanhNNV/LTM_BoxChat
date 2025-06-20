// TẠO FILE MỚI: src/main/java/chatapp/server/UserService.java
package chatapp.server;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import org.mindrot.jbcrypt.BCrypt;

import chatapp.model.User;
import chatapp.service.DBConfig;

public class UserService {
    private Connection connection;

    public UserService() throws SQLException {
        this.connection = DBConfig.getConnection();
    }

    // Phương thức này thảnh đổi để sử dụng mật khẩu đã hash
    public User login(String username, String password) throws SQLException {
        String sql = "SELECT * FROM Users WHERE username = ?";
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setString(1, username);
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                // Lấy hash từ DB
                String hashedPasswordFromDB = rs.getString("password");
                // So sánh mật khẩu người dùng nhập với hash trong DB
                if (BCrypt.checkpw(password, hashedPasswordFromDB)) {
                    // Nếu khớp, trả về đối tượng User
                    User user = new User();
                    user.setId(rs.getInt("id"));
                    user.setUsername(rs.getString("username"));
                    user.setFullName(rs.getString("fullname"));
                    // Không cần set password ở đây nữa để tăng bảo mật
                    return user;
                }
            }
        }
        // Sai username hoặc password
        return null;
    }

    // Phương thức thay đổi để hash mật khẩu
    public String register(String username, String password, String gmail, String fullName) throws SQLException {
        if (userExists(username)) {
            return "Username already exists";
        }
        if (gmailExists(gmail)) {
            return "Gmail already exists";
        }

        // HASH mật khẩu trước khi lưu
        String hashedPassword = BCrypt.hashpw(password, BCrypt.gensalt());

        String sql = "INSERT INTO Users (username, password, gmail, fullname) VALUES (?, ?, ?, ?)";
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setString(1, username);
            stmt.setString(2, hashedPassword); // Lưu mật khẩu đã hash
            stmt.setString(3, gmail);
            stmt.setString(4, fullName);
            int result = stmt.executeUpdate();
            // Sửa lại điều kiện trả về cho đúng logic bên RegisterController
            return result > 0 ? "SUCCESS" : "Registration failed.";
        }
    }

    private boolean userExists(String username) throws SQLException {
        String sql = "SELECT id FROM Users WHERE username = ?";
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setString(1, username);
            return stmt.executeQuery().next();
        }
    }

    private boolean gmailExists(String gmail) throws SQLException {
        String sql = "SELECT id FROM Users WHERE gmail = ?";
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setString(1, gmail);
            return stmt.executeQuery().next();
        }
    }

    public User getUserById(int userId) throws SQLException {
        String sql = "SELECT id,password, username, fullname, gmail FROM users WHERE id = ?";
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setInt(1, userId);
            ResultSet rs = stmt.executeQuery();

            if (rs.next()) {
                User user = new User();
                user.setId(rs.getInt("id"));
                user.setUsername(rs.getString("username"));
                user.setPassword(rs.getString("password"));
                user.setFullName(rs.getString("fullname"));
                user.setGmail(rs.getString("gmail"));
                return user;
            }
        }
        return null;
    }
    /// them code sua thong tin
    public boolean changePassword(int userId, String newPassword) throws SQLException {
        String sql = "UPDATE users SET password = ? WHERE id = ?";
        try (Connection conn = DBConfig.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, newPassword);
            stmt.setInt(2, userId);
            int rowsUpdated = stmt.executeUpdate();

            // Quan trọng: Commit thay đổi
            if (!conn.getAutoCommit()) {
                conn.commit();
            }

            return rowsUpdated > 0;
        }
    }
}