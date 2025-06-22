// D:\Education\Lap_trinh_mang\PROJECT\chat\src\main\java\chatapp\controller\ChatRoomController.java

package chatapp.controller;

import chatapp.Main;
import chatapp.model.*;
import javafx.animation.KeyFrame;
import javafx.animation.KeyValue;
import javafx.animation.Timeline;
import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Bounds;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.geometry.Side;
import javafx.scene.Cursor;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.KeyEvent;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.scene.text.Font;
import javafx.scene.text.FontPosture;
import javafx.scene.text.Text;
import javafx.scene.text.TextFlow;
import javafx.stage.*;

import java.io.File;
import java.nio.file.Files;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.io.ByteArrayInputStream;
import java.util.stream.Collectors;

import javafx.event.Event;
import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;
import javafx.util.Duration;

import javafx.scene.layout.StackPane; // Thêm import
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Base64;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import chatapp.Main;
import chatapp.model.Client;
import chatapp.model.Message;
import chatapp.model.NetworkMessage;
import chatapp.model.NetworkMessage.MessageType;
import chatapp.model.Room;
import chatapp.model.User;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.geometry.Bounds;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Cursor;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.scene.text.Font;
import javafx.scene.text.Text;
import javafx.scene.text.TextFlow;
import javafx.stage.FileChooser;
import javafx.stage.Stage;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import chatapp.model.Client;
import chatapp.model.NetworkMessage;
import chatapp.model.Room;
import chatapp.model.User;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonBar.ButtonData;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Dialog;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.Modality;
import javafx.stage.Stage;

public class ChatRoomController extends BaseController {

    // === Các biến FXML của màn hình chính ===
    @FXML private TextField roomIdField;
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
    @FXML
    private ListView<String> groupListView;

    @FXML
    private TextField searchField;

    private Map<Integer, Integer> unreadCounts = new HashMap<>();

    private List<Room> allGroups = new ArrayList<>();
    private User currentUser;

    @FXML
    public void initialize() {
        requestJoinedGroups();
        requestCurrentUser();
        requestUnreadCounts();

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



    // Xử lý sự kiện khi nhấn nút "Tạo phòng"
    @FXML
    private void handleCreateRoom() {
        try {
            // Lấy Stage (cửa sổ) hiện tại
            Stage ownerStage = (Stage) groupContainer.getScene().getWindow();

            // 1. Tải FXML của Dialog
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/chatapp/create_room.fxml"));
            GridPane dialogContent = loader.load();

            // 2. Tạo Dialog và đặt nội dung là FXML đã tải
            Dialog<ButtonType> dialog = new Dialog<>();
            // Gắn dialog vào cửa sổ cha (quan trọng)
            dialog.initOwner(ownerStage);
            dialog.initModality(Modality.WINDOW_MODAL); // Khóa cửa sổ cha

            dialog.setTitle("Tạo phòng chat mới");
            dialog.getDialogPane().setContent(dialogContent);

            // 3. Thêm các nút "Tạo" và "Hủy"
            ButtonType createButtonType = new ButtonType("Tạo phòng", ButtonBar.ButtonData.OK_DONE);
            dialog.getDialogPane().getButtonTypes().addAll(createButtonType, ButtonType.CANCEL);

            // Làm mờ cửa sổ cha khi mở dialog
            groupContainer.setEffect(new javafx.scene.effect.GaussianBlur(10)); // Làm mờ
            groupContainer.setOpacity(0.8); // Giảm độ trong suốt

            // 6. Xử lý kết quả khi dialog đóng lại
            Optional<ButtonType> result = dialog.showAndWait();

            // Xóa hiệu ứng làm mờ sau khi dialog đã đóng
            groupContainer.setEffect(null);
            groupContainer.setOpacity(1.0);

            // Logic cho avatar sẽ được thêm sau

            if (result.isPresent() && result.get() == createButtonType) {
                // Lấy các control từ FXML để xử lý sau này
                TextField newRoomNameField = (TextField) dialogContent.lookup("#newRoomNameField");
                PasswordField newRoomPasswordField = (PasswordField) dialogContent.lookup("#newRoomPasswordField");
                // TextArea memberEmailsTextArea = (TextArea)
                // dialogContent.lookup("#memberEmailsTextArea");
                // Người dùng đã nhấn "Tạo phòng"
                String name = newRoomNameField.getText().trim();
                String password = newRoomPasswordField.getText();
                // String emailsText = memberEmailsTextArea.getText().trim();

                // kiem tra rong
                if (name.isEmpty() || password.isEmpty()) {
                    showAlert(Alert.AlertType.ERROR, "Tên phòng và mật khẩu không được để trống.");
                    return;
                }

                // dinh nghia regex
                String nameValidationRegex = "^[\\p{L}][\\p{L}0-9 ]*$"; // Cho phép ký tự Unicode (bao gồm tiếng Việt)
                // 3. Kiểm tra tên phòng với Regex
                if (!name.matches(nameValidationRegex)) {
                    showAlert(Alert.AlertType.ERROR, "Room name invalid !\n\n" +
                            "Quy tắc:\n" +
                            "- Không được bắt đầu bằng số.\n" +
                            "- Chỉ chứa chữ cái, số, và khoảng trắng.");
                    return; // Dừng lại nếu tên không hợp lệ
                }
                // Chuyển đổi chuỗi emails thành một List<String>
                // List<String> memberEmails = Arrays.stream(emailsText.split("\\n"))
                // .map(String::trim)
                // .filter(email -> !email.isEmpty())
                // .collect(Collectors.toList());

                // Tạo một đối tượng mới để gửi lên server
                Room roomToCreate = new Room();
                roomToCreate.setName(name);
                roomToCreate.setPassword(password);
                // roomToCreate.setMemberEmails(memberEmails); // Gán danh sách email

                // 2. Gửi đối tượng Room đi
                NetworkMessage request = new NetworkMessage(NetworkMessage.MessageType.CREATE_ROOM_REQUEST,
                        roomToCreate);
                Client.getInstance().sendMessage(request);
                addStatusMessage("Đang gửi yêu cầu tạo phòng...");
            }

        } catch (IOException e) {
            e.printStackTrace();
            showAlert(Alert.AlertType.ERROR, "Không thể mở được cửa sổ tạo phòng.");
        }
    }


    @FXML
    public void handleJoinRoom() {
        int idText = Integer.parseInt(roomIdField.getText().trim()); // sửa ở đây
        String password = roomPasswordField.getText();

        if (password.isEmpty()) {
            showAlert(Alert.AlertType.ERROR, "Vui lòng nhập mật khẩu phòng.");
            return;
        }

        // Tạo đối tượng Room để gửi lên server Room(int, string)
        Room roomToJoin = new Room(idText, password);
        // Gói vào NetworkMessage và gửi đi
        NetworkMessage request = new NetworkMessage(NetworkMessage.MessageType.JOIN_ROOM_REQUEST, roomToJoin);
        Client.getInstance().sendMessage(request);
        addStatusMessage("Đang gửi yêu cầu tham gia phòng '" + roomToJoin.getName() + "'...");
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

    /**
     * Phương thức này được gọi bởi BaseController khi có tin nhắn từ Server.
     * Nó sẽ xử lý các phản hồi liên quan đến việc tạo/tham gia phòng.
     */
    @Override
    protected void handleServerMessage(NetworkMessage message) {
        switch (message.getType()) {
            case ROOM_CREATED:
                addStatusMessage("Tạo phòng thành công!");
                // Server trả về đối tượng Room đã được tạo (có cả ID)
                switchToPrivateRoom((Room) message.getPayload());
                break;

            case ROOM_JOINED:
                addStatusMessage("Tham gia phòng thành công!");
                // Server trả về đối tượng Room đã tham gia
                switchToPrivateRoom((Room) message.getPayload());
                break;

            case ROOM_JOIN_FAILED:
            case ERROR_RESPONSE: // Bắt cả lỗi chung, ví dụ "tên phòng đã tồn tại"
                // Server trả về một chuỗi thông báo lỗi
                String errorMessage = (String) message.getPayload();
                showAlert(Alert.AlertType.ERROR, errorMessage);
                addStatusMessage("Thất bại: " + errorMessage);
                break;
            case JOINED_GROUPS_RESPONSE:
                allGroups = (List<Room>) message.getPayload();
                showJoinedGroups(allGroups);
                break;
            case USER_RESPONSE:
                this.currentUser = (User) message.getPayload();
                updateUserInfoUI();
                requestUnreadCounts();
                break;
            case JOIN_EXISTING_ROOM_RESPONSE:
                if (message.getPayload() instanceof Room) {
                    // Thành công - chuyển đến phòng chat
                    Room joinedRoom = (Room) message.getPayload();
                    switchToPrivateRoom(joinedRoom);
                    addStatusMessage("Đã kết nối vào phòng '" + joinedRoom.getName() + "'");
                } else if (message.getPayload() instanceof String) {
                    // Thất bại - hiển thị thông báo lỗi
                    String error = (String) message.getPayload();
                    showAlert(Alert.AlertType.ERROR, error);
                    addStatusMessage("Lỗi: " + error);
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
            case GET_UNREAD_COUNTS_RESPONSE:
                System.out.println("Received unread counts: " + message.getPayload());
                unreadCounts = (Map<Integer, Integer>) message.getPayload();
                showJoinedGroups(allGroups);
                refreshRoomList();
                break;
            case NEW_MESSAGE_NOTIFICATION:
                Integer roomIdWithNewMessage = (Integer) message.getPayload();
                if (!unreadCounts.containsKey(roomIdWithNewMessage)) {
                    unreadCounts.put(roomIdWithNewMessage, 1);
                } else {
                    unreadCounts.put(roomIdWithNewMessage, unreadCounts.get(roomIdWithNewMessage) + 1);
                }
                refreshRoomList();
                break;
            case MARK_MESSAGES_READ_RESPONSE:
                Integer roomIdMarkedRead = (Integer) message.getPayload();
                if (roomIdMarkedRead != null) {
                    unreadCounts.remove(roomIdMarkedRead);
                    refreshRoomList();
                }
                break;
            default:
                // Bỏ qua các tin nhắn không liên quan đến màn hình này
                // (ví dụ: RECEIVE_MESSAGE, LOGIN_SUCCESS...)
                break;
        }
    }



    /**
     * Chuyển người dùng sang màn hình chat riêng tư của phòng.
     *
     * @param room Đối tượng Room chứa thông tin phòng (tên, id)
     */
    private void switchToPrivateRoom(Room room) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/chatapp/privateroom.fxml"));
            Parent root = loader.load();

            // Lấy controller của màn hình chat riêng tư
            PrivateRoomController privateRoomController = loader.getController();
            // Khởi tạo BaseController cho nó
            privateRoomController.initializeController();

            NetworkMessage unreadRequest = new NetworkMessage(
                    NetworkMessage.MessageType.GET_UNREAD_COUNTS_REQUEST,
                    null
            );
            Client.getInstance().sendMessage(unreadRequest);
            // Truyền thông tin phòng sang cho nó
            privateRoomController.setRoom(room);

            Stage stage = (Stage) roomIdField.getScene().getWindow();
            stage.setScene(new Scene(root));
            stage.setTitle("Chat Room - " + room.getName());

        } catch (IOException e) {
            e.printStackTrace();
            showAlert(Alert.AlertType.ERROR, "Không thể tải giao diện phòng chat.");
        }
    }

    // Hiển thị một tin nhắn trạng thái trên giao diện
    private void addStatusMessage(String msg) {
        statusListView.getItems().add(msg);
        // Cuộn xuống tin nhắn mới nhất
        statusListView.scrollTo(statusListView.getItems().size() - 1);
    }

    // Hiển thị một cửa sổ thông báo
    private void showAlert(Alert.AlertType type, String msg) {
        Alert alert = new Alert(type);
        alert.setTitle("Thông báo");
        alert.setHeaderText(null);
        alert.setContentText(msg);
        alert.showAndWait();
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
            HBox groupItem = new HBox();
            groupItem.setSpacing(10);
            groupItem.setAlignment(Pos.CENTER_LEFT);
            groupItem.setPadding(new Insets(10));
            groupItem.setStyle("-fx-border-color: #e0e0e0; -fx-border-radius: 5;");

            // Thêm sự kiện click
            groupItem.setOnMouseClicked(event -> {
                NetworkMessage request = new NetworkMessage(
                        NetworkMessage.MessageType.JOIN_EXISTING_ROOM_REQUEST,
                        room.getId()
                );
                Client.getInstance().sendMessage(request);
            });

            // Hiệu ứng hover
            groupItem.hoverProperty().addListener((obs, oldVal, isHovering) -> {
                if (isHovering) {
                    groupItem.setStyle("-fx-background-color: #f5f5f5; -fx-border-color: #e0e0e0; -fx-border-radius: 5; -fx-cursor: hand;");
                } else {
                    groupItem.setStyle("-fx-background-color: transparent; -fx-border-color: #e0e0e0; -fx-border-radius: 5; -fx-cursor: hand;");
                }
            });
            // Icon nhóm
            Label icon = new Label("#");
            icon.setStyle("-fx-font-size: 24px; -fx-text-fill: #4CAF50;");

            // Thông tin nhóm
            VBox infoBox = new VBox();
            Label nameLabel = new Label(room.getName());
            nameLabel.setStyle("-fx-font-size: 16px; -fx-font-weight: bold;");

            infoBox.getChildren().addAll(nameLabel);
            groupItem.getChildren().addAll(icon, infoBox);
            if (unreadCounts.containsKey(room.getId()) && unreadCounts.get(room.getId()) > 0) {
                StackPane indicator = createUnreadIndicator(unreadCounts.get(room.getId()));
                indicator.setAlignment(Pos.CENTER_RIGHT);
                // Canh chỉnh nếu cần, ví dụ: thêm spacing hoặc wrap lại bằng Region/HBox spacer
                Region spacer = new Region();
                HBox.setHgrow(spacer, Priority.ALWAYS);

                groupItem.getChildren().addAll(spacer, indicator);
            }

            groupContainer.getChildren().add(groupItem);
        }
    }

    private StackPane createUnreadIndicator(int count) {
        Circle redDot = new Circle(8, Color.RED);
        Label countLabel = new Label(String.valueOf(count));
        countLabel.setStyle("-fx-font-size: 10px; -fx-text-fill: white; -fx-font-weight: bold;");

        StackPane indicator = new StackPane(redDot, countLabel);
        indicator.setMinSize(16, 16);
        indicator.setPrefSize(16, 16);
        return indicator;
    }



    private void requestJoinedGroups() {
        NetworkMessage request = new NetworkMessage(
                NetworkMessage.MessageType.GET_JOINED_GROUPS_REQUEST,
                null);
        Client.getInstance().sendMessage(request);

    }


    private void requestCurrentUser() {
        Client.getInstance().sendMessage(new NetworkMessage(NetworkMessage.MessageType.GET_USER_REQUEST, null));
    }

    private void requestUnreadCounts() {
        if (currentUser != null) {
            NetworkMessage request = new NetworkMessage(
                    NetworkMessage.MessageType.GET_UNREAD_COUNTS_REQUEST,
                    null);

            Client.getInstance().sendMessage(request);
        }
    }

    public void refreshRoomList() {
        showJoinedGroups(allGroups); // Thêm tham số thứ 2
    }
}
