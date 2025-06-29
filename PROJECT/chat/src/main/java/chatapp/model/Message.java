// D:\Education\Lap_trinh_mang\PROJECT\chat\src\main\java\chatapp\model\Message.java
package chatapp.model;

import java.io.Serializable;
import java.time.LocalDateTime;

public class Message implements Serializable {
    private int id;
    private int userId;
    private String fullname; // Tên người gửi
    private int groupId;
    private String content;
    private LocalDateTime sendAt;

    private String fileName;
    private String fileType;
    private byte[] fileData;

    private boolean isFile;
    // Thêm getters và setters


    // Constructors, Getters, Setters

    public Message(){}
    // Constructors
    public Message(int userId, String fullname, int groupId, String content) {
        this.userId = userId;
        this.fullname = fullname;
        this.groupId = groupId;
        this.content = content;
        this.isFile = false;
    }

    public Message(int userId, String fullname, int groupId, String fileName, String fileType, byte[] fileData) {
        this.userId = userId;
        this.fullname = fullname;
        this.groupId = groupId;
        this.fileName = fileName;
        this.fileType = fileType;
        this.fileData = fileData;
        this.isFile = true;
    }

    // Getters and Setters
    public boolean isFile() {
        return isFile;
    }

    public void setFile(boolean file) {
        isFile = file;
    }

    public String getFormattedMessage() {
        return fullname + ": " + content;
    }

    // Getters and setters for all fields...
    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public int getUserId() {
        return userId;
    }

    public void setUserId(int userId) {
        this.userId = userId;
    }

    public String getFullname() {
        return fullname;
    }

    public void setFullname(String fullname) {
        this.fullname = fullname;
    }

    public int getGroupId() {
        return groupId;
    }

    public void setGroupId(int groupId) {
        this.groupId = groupId;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public LocalDateTime getSendAt() {
        return sendAt;
    }

    public void setSendAt(LocalDateTime sendAt) {
        this.sendAt = sendAt;
    }

    public String getFileName() {
        return fileName;
    }

    public void setFileName(String fileName) {
        this.fileName = fileName;
    }

    public String getFileType() {
        return fileType;
    }

    public void setFileType(String fileType) {
        this.fileType = fileType;
    }

    public byte[] getFileData() {
        return fileData;
    }

    public void setFileData(byte[] fileData) {
        this.fileData = fileData;
    }
}