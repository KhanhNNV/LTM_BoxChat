package chatapp.controller;

import chatapp.Main;
import chatapp.model.Client;
import chatapp.model.NetworkMessage;
import chatapp.model.User;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;

public class LoginController extends BaseController { // Kế thừa BaseController để nhận thông điệp từ server
    @FXML
    private TextField usernameField;
    @FXML
    private PasswordField passwordField;

    public void handleLogin() {
        String username = usernameField.getText();
        String password = passwordField.getText();
        if (username.isEmpty() || password.isEmpty()) {
            showAlert(Alert.AlertType.ERROR, "Username and password cannot be empty.");
            return;
        }
        User userToLogin = new User(username, password);
        NetworkMessage message = new NetworkMessage(NetworkMessage.MessageType.LOGIN_REQUEST, userToLogin);
        Client.getInstance().sendMessage(message);
    }

    @Override
    protected void handleServerMessage(NetworkMessage message) {
        switch (message.getType()) {
            case LOGIN_SUCCESS:
                showAlert(Alert.AlertType.INFORMATION, "Login Successful!");
                try {
                    // Lưu thông tin user đăng nhập vào Client instance nếu cần
                    // Client.getInstance().setCurrentUser((User) message.getPayload());
                    Main.setRoot("chatapp/chatroom");

                } catch (Exception e) {
                    e.printStackTrace();
                }
                break;
            case LOGIN_FAILURE:
                showAlert(Alert.AlertType.ERROR, (String) message.getPayload());
                break;
            default:
                // Ignore other messages on this screen
                break;
        }
    }

    public void goToRegister() throws Exception {
        Main.setRoot("chatapp/register");
    }

    private void showAlert(Alert.AlertType alertType, String message) {
        Alert alert = new Alert(alertType);
        alert.setTitle("Thông báo");
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

}
