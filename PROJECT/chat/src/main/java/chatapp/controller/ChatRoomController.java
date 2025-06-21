package chatapp.controller;

import chatapp.model.*;
import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.*;
import javafx.scene.text.Text;
import javafx.stage.Stage;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public class ChatRoomController extends BaseController {

    // === Các biến FXML của màn hình chính ===
    @FXML private TextField roomNameField;
    @FXML private PasswordField roomPasswordField;
    @FXML private ListView<String> statusListView;
    @FXML private VBox groupContainer;
    @FXML private ScrollPane scrollPane;
    @FXML private StackPane mainStackPane;

    // === Các biến FXML cho popup (đã được tách ra) ===
    @FXML private Pane infoPopupPane;       // Lớp phủ màu xám
    @FXML private VBox infoPopupContent;    // Nội dung popup màu trắng
    @FXML private Text infoFullNameUser;
    @FXML private Text infoUserNameUser;
    @FXML private Text infoGmailUser;

    private List<Room> allGroups = new ArrayList<>();
    private User currentUser;

    @FXML
    public void initialize() {
        requestJoinedGroups();
        requestCurrentUser();
    }

    // === Các hàm quản lý popup (đã được cập nhật) ===
    @FXML
    public void handleShowInfo() {
        if (currentUser == null) {
            showAlert(Alert.AlertType.WARNING, "Đang tải thông tin người dùng, vui lòng thử lại sau.");
            return;
        }
        updateUserInfoUI();
        infoPopupPane.setVisible(true);    // Hiện lớp phủ
        infoPopupContent.setVisible(true); // Hiện nội dung popup
    }

    @FXML
    public void handleCloseInfo() {
        infoPopupPane.setVisible(false);    // Ẩn lớp phủ
        infoPopupContent.setVisible(false); // Ẩn nội dung popup
    }

    private void updateUserInfoUI() {
        if (currentUser != null) {
            infoFullNameUser.setText(currentUser.getFullName());
            infoUserNameUser.setText(currentUser.getUsername());
            infoGmailUser.setText(currentUser.getGmail());
        }
    }

    // ... (Toàn bộ các hàm còn lại giữ nguyên không đổi)
    @FXML
    private void handleUpdateFullName(MouseEvent event) {
        TextInputDialog dialog = new TextInputDialog(currentUser.getFullName());
        dialog.setTitle("Đổi tên hiển thị");
        dialog.setHeaderText("Nhập tên hiển thị mới");
        dialog.setContentText("Tên hiển thị:");
        Optional<String> result = dialog.showAndWait();
        result.ifPresent(newFullName -> {
            if (!newFullName.trim().isEmpty()) {
                Map<String, Object> payload = new HashMap<>();
                payload.put("userId", currentUser.getId());
                payload.put("newFullName", newFullName.trim());
                Client.getInstance().sendMessage(new NetworkMessage(NetworkMessage.MessageType.UPDATE_FULLNAME_REQUEST, payload));
            }
        });
        event.consume();
    }

    @FXML
    private void handleChangePassword(ActionEvent event) {
        Dialog<Map<String, String>> dialog = new Dialog<>();
        dialog.setTitle("Đổi mật khẩu");
        dialog.setHeaderText("Nhập thông tin mật khẩu mới");
        ButtonType changeButtonType = new ButtonType("Đổi", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(changeButtonType, ButtonType.CANCEL);
        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setPadding(new Insets(20, 150, 10, 10));
        PasswordField newPasswordField = new PasswordField();
        newPasswordField.setPromptText("Mật khẩu mới");
        PasswordField confirmPasswordField = new PasswordField();
        confirmPasswordField.setPromptText("Xác nhận mật khẩu");
        grid.add(new Label("Mật khẩu mới:"), 0, 0);
        grid.add(newPasswordField, 1, 0);
        grid.add(new Label("Xác nhận mật khẩu:"), 0, 1);
        grid.add(confirmPasswordField, 1, 1);
        dialog.getDialogPane().setContent(grid);
        Platform.runLater(newPasswordField::requestFocus);
        dialog.setResultConverter(dialogButton -> {
            if (dialogButton == changeButtonType) {
                return Map.of("newPassword", newPasswordField.getText(), "confirmPassword", confirmPasswordField.getText());
            }
            return null;
        });
        Optional<Map<String, String>> result = dialog.showAndWait();
        result.ifPresent(passwordData -> {
            String newPassword = passwordData.get("newPassword");
            String confirmPassword = passwordData.get("confirmPassword");
            if (newPassword == null || newPassword.isEmpty() || !newPassword.equals(confirmPassword) || newPassword.length() < 6) {
                showAlert(Alert.AlertType.ERROR, "Mật khẩu không hợp lệ. Mật khẩu phải giống nhau và có ít nhất 6 ký tự.");
                return;
            }
            Map<String, Object> payload = new HashMap<>();
            payload.put("userId", currentUser.getId());
            payload.put("newPassword", newPassword);
            Client.getInstance().sendMessage(new NetworkMessage(NetworkMessage.MessageType.CHANGE_PASSWORD_REQUEST, payload));
        });
    }

    @FXML
    private void handleUpdateGmail(MouseEvent event) {
        TextInputDialog dialog = new TextInputDialog(currentUser.getGmail());
        dialog.setTitle("Đổi Gmail");
        dialog.setHeaderText("Nhập Gmail mới");
        dialog.setContentText("Gmail:");
        Optional<String> result = dialog.showAndWait();
        result.ifPresent(newGmail -> {
            if (!newGmail.trim().isEmpty() && newGmail.matches("^[\\w-\\.]+@([\\w-]+\\.)+[\\w-]{2,4}$")) {
                Map<String, Object> payload = new HashMap<>();
                payload.put("userId", currentUser.getId());
                payload.put("newGmail", newGmail.trim());
                Client.getInstance().sendMessage(new NetworkMessage(NetworkMessage.MessageType.UPDATE_GMAIL_REQUEST, payload));
            } else {
                showAlert(Alert.AlertType.ERROR, "Định dạng Gmail không hợp lệ");
            }
        });
        event.consume();
    }


    @Override
    protected void handleServerMessage(NetworkMessage message) {
        Platform.runLater(() -> {
            switch (message.getType()) {
                case ROOM_CREATED:
                case ROOM_JOINED:
                    switchToPrivateRoom((Room) message.getPayload());
                    break;
                case ROOM_JOIN_FAILED:
                case ERROR_RESPONSE:
                    showAlert(Alert.AlertType.ERROR, (String) message.getPayload());
                    break;
                case JOINED_GROUPS_RESPONSE:
                    allGroups = (List<Room>) message.getPayload();
                    showJoinedGroups(allGroups);
                    break;
                case USER_RESPONSE:
                    this.currentUser = (User) message.getPayload();
                    updateUserInfoUI();
                    break;
                case JOIN_EXISTING_ROOM_RESPONSE:
                    if (message.getPayload() instanceof Room) {
                        switchToPrivateRoom((Room) message.getPayload());
                    } else if (message.getPayload() instanceof String) {
                        showAlert(Alert.AlertType.ERROR, (String) message.getPayload());
                    }
                    break;
                case CHANGE_PASSWORD_SUCCESS:
                case UPDATE_FULLNAME_SUCCESS:
                case UPDATE_GMAIL_SUCCESS:
                    showAlert(Alert.AlertType.INFORMATION, (String) message.getPayload());
                    requestCurrentUser();
                    break;
                case CHANGE_PASSWORD_FAILURE:
                case UPDATE_FULLNAME_FAILURE:
                case UPDATE_GMAIL_FAILURE:
                    showAlert(Alert.AlertType.ERROR, (String) message.getPayload());
                    break;
                default:
                    break;
            }
        });
    }

    private void requestCurrentUser() {
        Client.getInstance().sendMessage(new NetworkMessage(NetworkMessage.MessageType.GET_USER_REQUEST, null));
    }
    private void requestJoinedGroups() {
        Client.getInstance().sendMessage(new NetworkMessage(NetworkMessage.MessageType.GET_JOINED_GROUPS_REQUEST, null));
    }
    public void showJoinedGroups(List<Room> rooms) {
        groupContainer.getChildren().clear();
        if (rooms.isEmpty()) {
            Label emptyLabel = new Label("Bạn chưa tham gia nhóm nào");
            emptyLabel.setStyle("-fx-font-size: 16px; -fx-text-fill: gray;");
            groupContainer.getChildren().add(emptyLabel);
            return;
        }
        for (Room room : rooms) {
            HBox groupItem = new HBox(10);
            groupItem.setAlignment(Pos.CENTER_LEFT);
            groupItem.setPadding(new Insets(10));
            groupItem.setStyle("-fx-border-color: #e0e0e0; -fx-border-radius: 5; -fx-cursor: hand;");
            groupItem.setOnMouseClicked(event -> {
                NetworkMessage request = new NetworkMessage(NetworkMessage.MessageType.JOIN_EXISTING_ROOM_REQUEST, room.getId());
                Client.getInstance().sendMessage(request);
            });
            groupItem.hoverProperty().addListener((obs, oldVal, isHovering) -> {
                if (isHovering) {
                    groupItem.setStyle("-fx-background-color: #f5f5f5; -fx-border-color: #e0e0e0; -fx-border-radius: 5; -fx-cursor: hand;");
                } else {
                    groupItem.setStyle("-fx-background-color: transparent; -fx-border-color: #e0e0e0; -fx-border-radius: 5; -fx-cursor: hand;");
                }
            });
            Label icon = new Label("#");
            icon.setStyle("-fx-font-size: 24px; -fx-text-fill: #4CAF50;");
            VBox infoBox = new VBox();
            Label nameLabel = new Label(room.getName());
            nameLabel.setStyle("-fx-font-size: 16px; -fx-font-weight: bold;");
            infoBox.getChildren().add(nameLabel);
            groupItem.getChildren().addAll(icon, infoBox);
            groupContainer.getChildren().add(groupItem);
        }
    }
    private void showAlert(Alert.AlertType type, String msg) {
        Alert alert = new Alert(type);
        alert.setTitle("Thông báo");
        alert.setHeaderText(null);
        alert.setContentText(msg);
        alert.showAndWait();
    }
    private void addStatusMessage(String msg) {
        statusListView.getItems().add(msg);
        statusListView.scrollTo(statusListView.getItems().size() - 1);
    }
    @FXML
    public void handleCreateRoom() {
        String name = roomNameField.getText().trim();
        String password = roomPasswordField.getText();
        if (name.isEmpty() || password.isEmpty()) {
            showAlert(Alert.AlertType.ERROR, "Vui lòng nhập đầy đủ tên và mật khẩu phòng.");
            return;
        }
        Room roomToCreate = new Room(name, password);
        NetworkMessage request = new NetworkMessage(NetworkMessage.MessageType.CREATE_ROOM_REQUEST, roomToCreate);
        Client.getInstance().sendMessage(request);
        addStatusMessage("Đang gửi yêu cầu tạo phòng '" + name + "'...");
    }
    @FXML
    public void handleJoinRoom() {
        String name = roomNameField.getText().trim();
        String password = roomPasswordField.getText();
        if (name.isEmpty() || password.isEmpty()) {
            showAlert(Alert.AlertType.ERROR, "Vui lòng nhập đầy đủ tên và mật khẩu phòng.");
            return;
        }
        Room roomToJoin = new Room(name, password);
        NetworkMessage request = new NetworkMessage(NetworkMessage.MessageType.JOIN_ROOM_REQUEST, roomToJoin);
        Client.getInstance().sendMessage(request);
        addStatusMessage("Đang gửi yêu cầu tham gia phòng '" + name + "'...");
    }
    private void switchToPrivateRoom(Room room) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/chatapp/privateroom.fxml"));
            Parent root = loader.load();
            PrivateRoomController privateRoomController = loader.getController();
            privateRoomController.initializeController();
            privateRoomController.setRoom(room);
            Stage stage = (Stage) roomNameField.getScene().getWindow();
            stage.setScene(new Scene(root));
            stage.setTitle("Chat Room - " + room.getName());
        } catch (IOException e) {
            e.printStackTrace();
            showAlert(Alert.AlertType.ERROR, "Không thể tải giao diện phòng chat.");
        }
    }
}