package chatapp.model;

import java.io.Serializable; // Thêm import

public class User implements Serializable { // Implement Serializable
    private int id;
    private String username;
    private String password;
    private String gmail; // Thêm trường email
    private String fullName; // Thêm trường fullName
    private boolean isOnline;

    public User() {
        this.isOnline = false; // Mặc định là offline
    }

    public User(String username, String password) {
        this.username = username;
        this.password = password;
    }

    public User(String username, String password, String gmail, String fullName) {
        this.username = username;
        this.password = password;
        this.gmail = gmail;
        this.fullName = fullName;
    }

    // Thêm getters & setters đầy đủ
    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getGmail() {
        return gmail;
    }

    public void setGmail(String gmail) {
        this.gmail = gmail;
    }

    public String getFullName() {
        return fullName;
    }

    public void setFullName(String fullName) {
        this.fullName = fullName;
    }

    public boolean isOnline() {
        return isOnline;
    }

    public void setOnline(boolean online) {
        isOnline = online;
    }
}
