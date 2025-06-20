// TẠO FILE MỚI: src/main/java/chatapp/server/UserService.java
package chatapp.server;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import chatapp.model.User;
import chatapp.service.DBConfig;

public class UserService {
    private Connection connection;

    public UserService() throws SQLException {
        this.connection = DBConfig.getConnection();
    }

    public User login(String username, String password) throws SQLException {
        String sql = "SELECT * FROM Users WHERE username = ? AND password = ?";
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setString(1, username);
            stmt.setString(2, password);
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                User user = new User(rs.getString("username"), rs.getString("password"));
                user.setId(rs.getInt("id"));
                return user;
            }
        }
        return null;
    }

    public String register(User user) throws SQLException {
        // Kiểm tra username đã tồn tại chưa
        if (userExists(user.getUsername())) {
            return "Ten dang nhap da ton tai";
        }
        // Kiểm tra gmail đã tồn tại chưa
        if (gmailExists(user.getGmail())) {
            return "Gmail đã được sử dụng.";
        }

        String sql = "INSERT INTO Users (username, password, fullname, gmail) VALUES (?, ?, ?, ?)";
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setString(1, user.getUsername());
            stmt.setString(2, user.getPassword());
            stmt.setString(3, user.getFullName());
            stmt.setString(4, user.getGmail());

            int rowsInserted = stmt.executeUpdate();
            if (rowsInserted > 0) {
                return "Registration Successful!";
            } else {
                return "Đăng ký thất bại. Vui lòng thử lại.";
            }
        } catch (SQLException e) {
            // Bắt lỗi ràng buộc UNIQUE nếu có 2 request đồng thời
            if (e.getErrorCode() == 1062) { // Mã lỗi cho duplicate entry
                if (e.getMessage().contains("unique_gmail")) {
                    return "Gmail đã được sử dụng.";
                }
                if (e.getMessage().contains("username")) { // Giả sử cột username cũng có UNIQUE constraint
                    return "Tên đăng nhập đã tồn tại.";
                }
            }
            e.printStackTrace();
            return "Lỗi cơ sở dữ liệu khi đăng ký.";
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
}