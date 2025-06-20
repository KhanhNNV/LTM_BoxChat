package chatapp.model;

import java.io.Serializable;
import java.util.List;

public class CreateRoomPayload implements Serializable {
    private static final long serialVersionUID = 3L; // Tăng ID lên

    private final String name;
    private final String password;
    private final List<String> memberEmails;
    // Thêm trường byte[] avatarData; sau này

    public CreateRoomPayload(String name, String password, List<String> memberEmails) {
        this.name = name;
        this.password = password;
        this.memberEmails = memberEmails;
    }

    // Getters
    public String getName() {
        return name;
    }

    public String getPassword() {
        return password;
    }

    public List<String> getMemberEmails() {
        return memberEmails;
    }
}
