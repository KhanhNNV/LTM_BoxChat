package chatapp.controller;

import chatapp.Main;
import chatapp.model.Client;
import chatapp.model.NetworkMessage;
import chatapp.model.User;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;

public class RegisterController extends BaseController {
    @FXML
    private TextField usernameField;
    @FXML
    private TextField fullnameField;
    @FXML
    private PasswordField passwordField;
    @FXML
    private PasswordField confirmPasswordField;
    @FXML
    private TextField gmailField;

    public void handleRegister() {

        String username = usernameField.getText();
        String password = passwordField.getText();
        String fullname = fullnameField.getText();
        String gmail = gmailField.getText();
        String confirmPassword = confirmPasswordField.getText();

        if (fullname.isEmpty()) {
            showAlert(Alert.AlertType.ERROR, "Họ và tên không được để trống.");
            return;
        }
        if (username.isEmpty()) {
            showAlert(Alert.AlertType.ERROR, "Tên đăng nhập không được để trống.");
            return;
        }
        if (gmail.isEmpty()) {
            showAlert(Alert.AlertType.ERROR, "Gmail không được để trống.");
            return;
        }
        if (password.isEmpty()) {
            showAlert(Alert.AlertType.ERROR, "Mật khẩu không được để trống.");
            return;
        }
        if (!gmail.matches("^[\\w-\\.]+@([\\w-]+\\.)+[\\w-]{2,4}$")) {
            showAlert(Alert.AlertType.ERROR, "Email is not valid.");
            return;
        }
        if (!password.equals(confirmPassword)) {
            showAlert(Alert.AlertType.ERROR, "Passwords do not match.");
            return;
        }
        User userToRegister = new User(username, password, gmail, fullname);
        NetworkMessage message = new NetworkMessage(NetworkMessage.MessageType.REGISTER_REQUEST, userToRegister);
        Client.getInstance().sendMessage(message);

    }

    @Override
    protected void handleServerMessage(NetworkMessage message) {
        switch (message.getType()) {
            case REGISTER_SUCCESS:
                Platform.runLater(() -> {
                    showAlert(Alert.AlertType.INFORMATION, (String) message.getPayload());
                    try {
                        Main.setRoot("chatapp/login");
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                });
                break;
            case REGISTER_FAILURE:
                Platform.runLater(() -> {
                    showAlert(Alert.AlertType.ERROR, (String) message.getPayload());
                });
                break;
            default:
                break;
        }
    }

    @FXML
    public void goToLogin() throws Exception {
        Main.setRoot("chatapp/login");
    }

    private void showAlert(Alert.AlertType alertType, String message) {
        Alert alert = new Alert(alertType);
        alert.setTitle("Thông báo");
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

}
