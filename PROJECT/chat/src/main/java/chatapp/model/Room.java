package chatapp.model;

import java.io.Serializable;
import java.util.List;

public class Room implements Serializable { // Implement Serializable
    private int id;
    private String name;
    private String password;
    private int leaderId;
    private transient List<String> memberEmails;

    public Room() {

    }

    // constructor, getters, setters...
    public Room(String name, String password) {
        this.name = name;
        this.password = password;
    }

    public Room(int id, String password) {
        this.id = id;
        this.password = password;
    }

    public Room(String name, String password, int leader_id) {
        this.name = name;
        this.password = password;
        this.leaderId = leader_id;
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

    public List<String> getMemberEmails() {
        return memberEmails;
    }

    public void setMemberEmails(List<String> memberEmails) {
        this.memberEmails = memberEmails;
    }
}