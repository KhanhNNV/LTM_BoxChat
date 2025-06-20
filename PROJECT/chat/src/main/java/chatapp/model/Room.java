package chatapp.model;

import java.io.Serializable; // Thêm import

public class Room implements Serializable { // Implement Serializable
    private int id;
    private String name;
    private String password;
    private int leaderId;
    public Room() {

    }
    // constructor, getters, setters...
    public Room(String name, String password) {
        this.name = name;
        this.password = password;
    }
    public Room(String name, String password, int leaderId) {
        this.name = name;
        this.password = password;
        this.leaderId=leaderId;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }
    public int getLeaderId() {
        return leaderId;
    }

    public void setLeaderId(int leaderId) {
        this.leaderId = leaderId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }
}