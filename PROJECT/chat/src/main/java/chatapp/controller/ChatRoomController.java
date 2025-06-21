// D:\Education\Lap_trinh_mang\PROJECT\chat\src\main\java\chatapp\controller\ChatRoomController.java

package chatapp.controller;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

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
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.Modality;
import javafx.stage.Stage;

public class ChatRoomController extends BaseController {
    @FXML
    private TextField roomIdField; // Trường nhập tên phòng
    @FXML
    private PasswordField roomPasswordField; // Trường nhập mật khẩu phòng
    @FXML
    private ListView<String> statusListView;

    @FXML
    private VBox groupContainer;

    @FXML
    private ListView<String> groupListView;

    @FXML
    private TextField searchField;

    private List<Room> allGroups = new ArrayList<>();

    @FXML
    public void initialize() {
        requestJoinedGroups();
    }

    // // Xử lý sự kiện khi nhấn nút "Tạo phòng"
    // @FXML
    // public void handleCreateRoom() {
    // String name = roomIdField.getText().trim();
    // String password = roomPasswordField.getText();

    // if (name.isEmpty() || password.isEmpty()) {
    // showAlert(Alert.AlertType.ERROR, "Vui lòng nhập đầy đủ tên và mật khẩu
    // phòng.");
    // return;
    // }

    // // Tạo đối tượng Room để gửi lên server
    // Room roomToCreate = new Room(name, password);
    // // Gói vào NetworkMessage và gửi đi
    // NetworkMessage request = new
    // NetworkMessage(NetworkMessage.MessageType.CREATE_ROOM_REQUEST, roomToCreate);
    // Client.getInstance().sendMessage(request);
    // addStatusMessage("Đang gửi yêu cầu tạo phòng '" + name + "'...");
    // }

    // xu li tham gia phong trong create_room.fxml
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
            ButtonType createButtonType = new ButtonType("Tạo phòng", ButtonData.OK_DONE);
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
                TextArea memberEmailsTextArea = (TextArea) dialogContent.lookup("#memberEmailsTextArea");
                // Người dùng đã nhấn "Tạo phòng"
                String name = newRoomNameField.getText().trim();
                String password = newRoomPasswordField.getText();
                String emailsText = memberEmailsTextArea.getText().trim();

                // kiem tra rong
                if (name.isEmpty() || password.isEmpty()) {
                    showAlert(Alert.AlertType.ERROR, "Tên phòng và mật khẩu không được để trống.");
                    return;
                }

                // dinh nghia regex
                String nameValidationRegex = "^[a-zA-Z][a-zA-Z0-9 ]*$";
                // 3. Kiểm tra tên phòng với Regex
                if (!name.matches(nameValidationRegex)) {
                    showAlert(Alert.AlertType.ERROR, "Room name invalid !\n\n" +
                            "Quy tắc:\n" +
                            "- Không được bắt đầu bằng số.\n" +
                            "- Chỉ chứa chữ cái, số, và khoảng trắng.");
                    return; // Dừng lại nếu tên không hợp lệ
                }
                // Chuyển đổi chuỗi emails thành một List<String>
                List<String> memberEmails = Arrays.stream(emailsText.split("\\n"))
                        .map(String::trim)
                        .filter(email -> !email.isEmpty())
                        .collect(Collectors.toList());

                // Tạo một đối tượng mới để gửi lên server
                Room roomToCreate = new Room();
                roomToCreate.setName(name);
                roomToCreate.setPassword(password);
                roomToCreate.setMemberEmails(memberEmails); // Gán danh sách email

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

    // Xử lý sự kiện khi nhấn nút "Tham gia phòng"
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
                User user = (User) message.getPayload();
                // showUserInfo(user);
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
                        room.getId());
                Client.getInstance().sendMessage(request);
            });

            // Hiệu ứng hover
            groupItem.hoverProperty().addListener((obs, oldVal, isHovering) -> {
                if (isHovering) {
                    groupItem.setStyle(
                            "-fx-background-color: #f5f5f5; -fx-border-color: #e0e0e0; -fx-border-radius: 5; -fx-cursor: hand;");
                } else {
                    groupItem.setStyle(
                            "-fx-background-color: transparent; -fx-border-color: #e0e0e0; -fx-border-radius: 5; -fx-cursor: hand;");
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
            groupContainer.getChildren().add(groupItem);
        }
    }

    private void requestJoinedGroups() {
        NetworkMessage request = new NetworkMessage(
                NetworkMessage.MessageType.GET_JOINED_GROUPS_REQUEST,
                null);
        Client.getInstance().sendMessage(request);

    }

    public void requestUserInfo(int userId) {
        NetworkMessage request = new NetworkMessage(
                NetworkMessage.MessageType.GET_USER_REQUEST,
                userId);
        Client.getInstance().sendMessage(request);
    }

}