package chatapp.server;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import org.mindrot.jbcrypt.BCrypt;

import chatapp.model.User;
import chatapp.service.DBConfig;

public class UserService {
    private Connection connection;

    public UserService() throws SQLException {
        this.connection = DBConfig.getConnection();
    }

    public User login(String username, String password) throws SQLException {
        String sql = "SELECT * FROM Users WHERE username = ?";
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setString(1, username);
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                String hashedPasswordFromDB = rs.getString("password");
                if (BCrypt.checkpw(password, hashedPasswordFromDB)) {
                    User user = new User();
                    user.setId(rs.getInt("id"));
                    user.setUsername(rs.getString("username"));
                    user.setFullName(rs.getString("fullname"));
                    return user;
                }
            }
        }
        return null;
    }

    public String register(String username, String password, String gmail, String fullName) throws SQLException {
        if (userExists(username)) {
            return "Username already exists";
        }
        if (gmailExists(gmail)) {
            return "Gmail already exists";
        }

        String hashedPassword = BCrypt.hashpw(password, BCrypt.gensalt());

        String sql = "INSERT INTO Users (username, password, gmail, fullname) VALUES (?, ?, ?, ?)";
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setString(1, username);
            stmt.setString(2, hashedPassword);
            stmt.setString(3, gmail);
            stmt.setString(4, fullName);
            int result = stmt.executeUpdate();
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

    public boolean changePassword(int userId, String newPassword) throws SQLException {
        String hashedPassword = BCrypt.hashpw(newPassword, BCrypt.gensalt());
        String sql = "UPDATE users SET password = ? WHERE id = ?";
        try (Connection conn = DBConfig.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, hashedPassword);
            stmt.setInt(2, userId);
            int rowsUpdated = stmt.executeUpdate();

            if (!conn.getAutoCommit()) {
                conn.commit();
            }

            return rowsUpdated > 0;
        }
    }
    public boolean updateFullName(int userId, String newFullName) throws SQLException {
        String sql = "UPDATE users SET fullname = ? WHERE id = ?";
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setString(1, newFullName);
            stmt.setInt(2, userId);
            int rowsUpdated = stmt.executeUpdate();

            if (!connection.getAutoCommit()) {
                connection.commit();
            }

            return rowsUpdated > 0;
        }
    }
    public boolean updateGmail(int userId, String newGmail) throws SQLException {
        if (gmailExists(newGmail)) {
            return false;
        }

        String sql = "UPDATE users SET gmail = ? WHERE id = ?";
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setString(1, newGmail);
            stmt.setInt(2, userId);
            int rowsUpdated = stmt.executeUpdate();

            if (!connection.getAutoCommit()) {
                connection.commit();
            }

            return rowsUpdated > 0;
        }
    }
}
