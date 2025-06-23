// Sửa lại PrivateRoomController.java
package chatapp.controller;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Base64;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import chatapp.Main;
import chatapp.model.*;
import chatapp.model.NetworkMessage.MessageType;
import javafx.animation.KeyFrame;
import javafx.animation.KeyValue;
import javafx.animation.PauseTransition;
import javafx.animation.Timeline;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.geometry.Bounds;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.geometry.Side;
import javafx.scene.Cursor;
import javafx.scene.Node;
import javafx.scene.control.*;
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
import javafx.scene.input.KeyEvent;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.scene.text.Font;
import javafx.scene.text.FontPosture;
import javafx.scene.text.Text;
import javafx.scene.text.TextFlow;
import javafx.stage.FileChooser;
import javafx.stage.Stage;

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



public class PrivateRoomController extends BaseController {

    @FXML
    private ListView<String> statusListView;
    @FXML
    private TextField messageField;
    @FXML
    private Label roomNameLabel;

    @FXML
    private Pane headerGroup;

    @FXML
    private AnchorPane listGroupContainer;
    @FXML
    private HBox headerChat;

    @FXML
    private Label groupNameLabel;

    @FXML
    private Text infoNameGroup;
    @FXML
    private Text infoIdGroup;
    @FXML
    private Text infoPassGroup;

    @FXML
    private Text infoFullNameUser;
    @FXML
    private Text infoUserNameUser;
    @FXML
    private Text infoPassUser;
    @FXML
    private Text infoGmailUser;


    @FXML
    private VBox chatBox;

    @FXML
    private TextArea inputTextArea;

    @FXML
    private ScrollPane scrollPane;

    @FXML
    private Pane overlay;
    @FXML
    private Pane overlay2;
    @FXML
    private VBox menuPopup;
    @FXML
    private HBox searchPopup;
    @FXML
    private Pane searchPopup2;
    @FXML
    private Pane boxInfo;

    @FXML
    private ListView<User> memberListView;

    @FXML
    private ImageView iconSendFile;

    @FXML
    private ScrollPane emojiPane; // Pane chứa emoji
    @FXML
    private ImageView iconSendEmoji; // ImageView của icon emoji
    @FXML
    private Pane emojiOverlay;
    @FXML
    private Button leaveRoomButton;
    @FXML
    private HBox passwordRow;

    @FXML
    private ImageView userAvatarImageView;
    @FXML
    private Label usernameLabelInHeader;

    @FXML private StackPane mainStackPane; // Thêm biến này

    private boolean emojiPaneVisible = false;

    private List<Room> allGroups = new ArrayList<>();

    private Room currentRoom;
    private User currentUser;
    private boolean roomListenersInitialized = false;
    private Map<Integer, Boolean> userStatusMap = new HashMap<>(); // Luu trạng thái online/offline của người dùng

    @FXML private TextField searchField;
    private List<Message> allMessages = new ArrayList<>();
    private int currentSearchIndex = -1;
    private Map<Integer, Integer> unreadCounts = new HashMap<>();


    @FXML private TextField searchRoomField;

    // Khởi tạo
    @FXML
    public void initialize() {
        requestJoinedGroups();
        requestUnreadCounts();
        scrollToBottom();
        requestCurrentUser();
        overlay.setOnMouseClicked(e -> hideMenu(null));

        // === BẮT ĐẦU CODE MỚI CHO TÍNH NĂNG GỬI BẰNG ENTER ===
        inputTextArea.addEventFilter(KeyEvent.KEY_PRESSED, event -> {
            if (event.getCode() == KeyCode.ENTER) {
                // Ngăn chặn hành vi mặc định của phím Enter trong mọi trường hợp
                event.consume();

                if (event.isShiftDown()) {
                    // Nếu nhấn Shift + Enter, tự thêm một dòng mới
                    inputTextArea.appendText("\n");
                } else {
                    // Nếu chỉ nhấn Enter, gửi tin nhắn
                    handleSendMessage();
                }
            }
        });

        initializeEmojiPane();
        emojiOverlay.setOnMouseClicked(e -> {
            hideEmojiPane(null);
        });
        searchField.textProperty().addListener((observable, oldValue, newValue) -> {
            if (newValue.isEmpty()) {
                resetSearchHighlight();
            }
        });


    }
    // xu ly tiem kiem
    @FXML
    private void handleSearch(ActionEvent event) {
        String keyword = searchField.getText().trim();
        if (keyword.isEmpty()) {
            showAlert("Thông báo", "Vui lòng nhập từ khóa tìm kiếm");
            return;
        }

        searchMessages(keyword);

    }

    private void searchMessages(String keyword) {
        resetSearchHighlight();

        List<HBox> foundMessages = new ArrayList<>();
        List<Message> matchedMessages = new ArrayList<>();
        String lowerKeyword = keyword.toLowerCase();

        // Tìm kiếm trong tất cả tin nhắn
        for (int i = 0; i < chatBox.getChildren().size(); i++) {
            Node node = chatBox.getChildren().get(i);
            if (node instanceof HBox) {
                HBox messageContainer = (HBox) node;
                Message message = allMessages.get(i);

                // Kiểm tra nội dung tin nhắn
                String content = "";
                if (message.isFile()) {
                    content = message.getFileName().toLowerCase();
                } else {
                    content = message.getContent().toLowerCase();
                }

                if (content.contains(lowerKeyword)) {
                    messageContainer.setStyle("-fx-background-color: #fff9c4;");
                    foundMessages.add(messageContainer);
                    matchedMessages.add(message);
                }
            }
        }

        if (foundMessages.isEmpty()) {
            showAlert("Thông báo", "Không tìm thấy tin nhắn nào chứa từ khóa: " + keyword);
            return;
        }

        // Di chuyển đến kết quả đầu tiên
        currentSearchIndex = 0;
        scrollToMessage(foundMessages.get(0));

        // Tạo menu kết quả tìm kiếm
        createSearchResultsMenu(matchedMessages);
    }

    private void scrollToMessage(HBox messageBox) {
        // Tính toán vị trí để cuộn đến
        Bounds boundsInScene = messageBox.localToScene(messageBox.getBoundsInLocal());
        double targetY = boundsInScene.getMinY();

        // Tính toán vị trí scroll
        double scrollHeight = scrollPane.getContent().getBoundsInLocal().getHeight();
        double viewportHeight = scrollPane.getViewportBounds().getHeight();
        double scrollValue = (targetY - 100) / (scrollHeight - viewportHeight);

        // Giới hạn giá trị scroll trong khoảng 0-1
        scrollValue = Math.max(0, Math.min(1, scrollValue));

        // Cuộn đến vị trí
        scrollPane.setVvalue(scrollValue);
    }

    private void resetSearchHighlight() {
        for (Node node : chatBox.getChildren()) {
            if (node instanceof HBox) {
                node.setStyle("");
            }
        }
        currentSearchIndex = -1;
    }

    private void createSearchResultsMenu(List<Message> matchedMessages) {
        // Tạo popup menu hiển thị kết quả
        ContextMenu searchResultsMenu = new ContextMenu();

        for (Message message : matchedMessages) {
            String displayText = message.getFullname() + ": ";
            if (message.isFile()) {
                displayText += "[File] " + message.getFileName();
            } else {
                // Giới hạn độ dài hiển thị
                String content = message.getContent();
                if (content.length() > 50) {
                    content = content.substring(0, 47) + "...";
                }
                displayText += content;
            }

            MenuItem item = new MenuItem(displayText);
            item.setOnAction(e -> {
                int index = allMessages.indexOf(message);
                if (index >= 0) {
                    HBox messageBox = (HBox) chatBox.getChildren().get(index);
                    scrollToMessage(messageBox);
                    messageBox.setStyle("-fx-background-color: #ffeb3b;");
                }
            });

            searchResultsMenu.getItems().add(item);
        }

        // Hiển thị menu ngay dưới ô tìm kiếm
        searchResultsMenu.show(searchField, Side.BOTTOM, 0, 0);
    }
    //xulytim kiem
    // Được gọi từ controller trước đó để truyền thông tin phòng

    // Thêm phương thức khởi tạo emoji
    private void initializeEmojiPane() {
        emojiPane.setVisible(false);
        emojiOverlay.setVisible(false);

        emojiPane.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER); // Ẩn thanh cuộn ngang
        emojiPane.setVbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED); // Hiện thanh cuộn dọc khi cần

        // Tạo grid layout cho emoji
        GridPane emojiGrid = new GridPane();
        emojiGrid.setHgap(5);
        emojiGrid.setVgap(5);
        emojiGrid.setPadding(new Insets(7));

        // Danh sách emoji mẫu (giữ nguyên)
        String[] emojis = {
                "😀", "😃", "😄", "😁", "😆", "😅", "😂", "🤣", "😊", "😇",
                "🙂", "🙃", "😉", "😌", "😍", "🥰", "😘", "😗", "😙", "😚",
                "😋", "😛", "😝", "😜", "🤪", "🤨", "🧐", "🤓", "😎", "🤩",
                "🥳", "😏", "😒", "😞", "😔", "😟", "😕", "🙁", "☹️", "😣",
                "😖", "😫", "😩", "🥺", "😢", "😭", "😤", "😠", "😡", "🤬",
                "🤯", "😳", "🥵", "🥶", "😱", "😨", "😰", "😥", "😓", "🤗",
                "🤔", "🤭", "🤫", "🤥", "😶", "😐", "😑", "😬", "🙄", "😯",
                "😦", "😧", "😮", "😲", "🥱", "😴", "🤤", "😪", "😵", "🤐",
                "🥴", "🤢", "🤮", "🤧", "😷", "🤒", "🤕", "🤑", "🤠", "😈",
                "👿", "👹", "👺", "🤡", "💩", "👻", "💀", "☠️", "👽", "👾",
                "🤖", "🎃", "😺", "😸", "😹", "😻", "😼", "😽", "🙀", "😿",
                "😾", "👋", "🤚", "🖐️", "✋", "🖖", "👌", "🤏", "✌️", "🤞",
                "🤟", "🤘", "🤙", "👈", "👉", "👆", "🖕", "👇", "☝️", "👍",
                "👎", "✊", "👊", "🤛", "🤜", "👏", "🙌", "👐", "🤲", "🤝",
                "🙏", "✍️", "💅", "🤳", "💪", "🦾", "🦿", "🦵", "🦶", "👂",
                "🦻", "👃", "🧠", "🦷", "🦴", "👀", "👁️", "👅", "👄", "💋",
                "🩸", "❤️", "🧡", "💛", "💚", "💙", "💜", "🖤", "🤍", "🤎",
                "💔", "❣️", "💕", "💞", "💓", "💗", "💖", "💘", "💝", "💟"
        };

        // Thêm emoji vào grid
        int columns = 10;
        for (int i = 0; i < emojis.length; i++) {
            int row = i / columns;
            int col = i % columns;

            Label emojiLabel = new Label(emojis[i]);
            emojiLabel.setStyle("-fx-font-size: 14px; -fx-cursor: hand;");

            emojiLabel.setOnMouseClicked(e -> {
                String currentText = inputTextArea.getText();
                // Tạm thời vô hiệu hóa listener nếu có
                inputTextArea.setText(currentText + emojiLabel.getText());

                // Giữ vị trí scroll
                scrollPane.setVvalue(scrollPane.getVvalue());
                e.consume();
            });

            emojiGrid.add(emojiLabel, col, row);
        }

        // Đặt grid vào ScrollPane
        emojiPane.setContent(emojiGrid);
        // emojiPane.addEventFilter(MouseEvent.MOUSE_CLICKED, Event::consume);
    }

    // Thêm phương thức hiển thị/ẩn emoji pane
    @FXML
    private void showEmojiPane(MouseEvent event) {
        emojiPane.setVisible(true);
        emojiOverlay.setVisible(true);

        if (emojiPaneVisible) {
            // Tính toán vị trí chính xác hơn
            Bounds iconBounds = iconSendEmoji.localToScene(iconSendEmoji.getBoundsInLocal());
            Bounds paneBounds = emojiPane.getParent().sceneToLocal(iconBounds);

            emojiPane.setLayoutX(paneBounds.getMinX() - emojiPane.getWidth() + 30);
            emojiPane.setLayoutY(paneBounds.getMinY() - emojiPane.getHeight() - 10);

            // Đảm bảo emojiPane không bị che bởi các phần tử khác
            emojiPane.toFront();
        }

        if (event != null) {
            event.consume();
        }
    }

    private void hideEmojiPane(MouseEvent event) {
        emojiPane.setVisible(false);
        emojiOverlay.setVisible(false);
        Platform.runLater(() -> {
            inputTextArea.requestFocus();
            // Di chuyển con trỏ đến cuối
            inputTextArea.end();
        });
    }

    @FXML
    public void handleSendMessage() {
        String content = inputTextArea.getText().trim();
        if (!content.isEmpty()) {
            Client.getInstance()
                    .sendMessage(new NetworkMessage(NetworkMessage.MessageType.SEND_MESSAGE_REQUEST, content));
            inputTextArea.clear();
        }

    }


    @FXML
    private void handleSendFile() {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Chọn file để gửi");
        Stage stage = (Stage) iconSendFile.getScene().getWindow();
        File file = fileChooser.showOpenDialog(stage);


        if (file != null) {
            try {
                // Đọc file thành byte array
                byte[] fileData = Files.readAllBytes(file.toPath());

                // Lấy thông tin file
                String fileName = file.getName();
                String fileType = Files.probeContentType(file.toPath());
                if (fileType == null) {
                    fileType = "application/octet-stream";
                }

                // Chuyển thành base64 để gửi qua network
                String base64Data = Base64.getEncoder().encodeToString(fileData);

                // Tạo message đặc biệt để nhận biết là file
                String fileMessage = String.format(
                        "FILE:%s:%s:%d:%s",
                        fileName,
                        fileType,
                        fileData.length,
                        base64Data);

                // Gửi message
                Client.getInstance().sendMessage(
                        new NetworkMessage(
                                NetworkMessage.MessageType.SEND_MESSAGE_REQUEST,
                                fileMessage));
            } catch (Exception e) {
                e.printStackTrace();
                showAlert("Lỗi", "Không thể đọc file: " + e.getMessage());
            }
        }
    }

    @Override
    protected void handleServerMessage(NetworkMessage message) {
        Platform.runLater(() -> {
            switch (message.getType()) {
                case RECEIVE_MESSAGE:
                    Message chatMessage = (Message) message.getPayload();
                    addMessageToUI(chatMessage);
                    break;
                case USER_JOINED_ROOM:
                    // Hiển thị thông báo có người mới vào
                    break;
                case USER_LEFT_ROOM:
                    showAlert(Alert.AlertType.INFORMATION, "Bạn đã rời khỏi phòng.");
                    try {
                        Main.setRoot("chatapp/chatroom");
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                    break;
                case ROOM_DELETED:
                    Message notification = (Message) message.getPayload();
                    // Hiển thị thông báo và tự động quay về sảnh chờ
                    showAlert(Alert.AlertType.WARNING, notification.getContent());
                    try {
                        Main.setRoot("chatapp/chatroom");
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                    break;
                case ROOM_HISTORY_RESPONSE:
                    List<Message> history = (List<Message>) message.getPayload();
                    showRoomHistory(history);
                    break;
                case MEMBERS_GROUP_RESPONSE:
                    List<User> membersWithStatus = (List<User>) message.getPayload();
                    // Cập nhật map trạng thái từ danh sách nhận được
                    userStatusMap.clear();
                    for (User u : membersWithStatus) {
                        userStatusMap.put(u.getId(), u.isOnline());
                    }
                    showGroupMembers(membersWithStatus); // Gọi phương thức hiển thị
                    break;
                case USER_STATUS_UPDATE:
                    User userWithStatus = (User) message.getPayload();
                    // Cập nhật trạng thái của user cụ thể
                    userStatusMap.put(userWithStatus.getId(), userWithStatus.isOnline());
                    // Cập nhật lại ListView để vẽ lại cell của user đó
                    memberListView.refresh();
                    break;
                case USER_RESPONSE:
                    this.currentUser = (User) message.getPayload();
                    updateUserInfoUI();
                    updatePersonalizedUI(); // Cập nhật giao diện cá nhân hóa
                    break;
                case JOIN_EXISTING_ROOM_RESPONSE:
                    if (message.getPayload() instanceof Room) {
                        Room joinedRoom = (Room) message.getPayload();
                        // Cập nhật giao diện với phòng mới
                        setRoom(joinedRoom);
                        refreshRoomList();
                    }
                    break;
                case BACK_HOME_SUCCESS:
                    try {
                        // Quay về màn hình chọn phòng
                        Main.setRoot("chatapp/chatroom");
                    } catch (Exception e) {
                        e.printStackTrace();
                        showAlert(Alert.AlertType.ERROR, "Lỗi: Không thể quay về màn hình chính.");
                    }
                    break;
                case YOU_HAVE_BEEN_REMOVED: // Tin nhắn riêng cho người bị xóa
                    showAlert(Alert.AlertType.WARNING, "Bạn đã bị trưởng phòng xóa khỏi nhóm.");
                    try {
                        Main.setRoot("chatapp/chatroom");
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                    break;
                // Xử lý các loại message khác...
                case CHANGE_PASSWORD_SUCCESS:
                    // SỬA LỖI Ở ĐÂY: Thay bằng showAlert(String, String)
                    showAlert("Thành công", (String) message.getPayload());
                    break;

                case CHANGE_PASSWORD_FAILURE:
                    // SỬA LỖI Ở ĐÂY: Thay bằng showAlert(String, String)
                    showAlert("Lỗi", (String) message.getPayload());
                    break;
                case UPDATE_FULLNAME_SUCCESS:
                    showAlert("Thành công", (String) message.getPayload());
                    requestCurrentUser(); // Lấy lại thông tin user mới
                    break;
                case UPDATE_FULLNAME_FAILURE:
                    showAlert("Lỗi", (String) message.getPayload());
                    break;
                case UPDATE_GMAIL_SUCCESS:
                    showAlert("Thành công", (String) message.getPayload());
                    requestCurrentUser(); // Lấy lại thông tin user mới
                    break;
                case UPDATE_GMAIL_FAILURE:
                    showAlert("Lỗi", (String) message.getPayload());
                    break;
                case UPDATE_ROOM_NAME_SUCCESS:
                    if (message.getPayload() instanceof Room) {
                        Room updatedRoom = (Room) message.getPayload();

                        // 1. Cập nhật thông tin phòng hiện tại
                        currentRoom.setName(updatedRoom.getName());

                        // 2. Cập nhật UI ngay lập tức
                        groupNameLabel.setText(updatedRoom.getName());  // Header
                        infoNameGroup.setText(updatedRoom.getName());  // Popup menu

                        // 3. CẬP NHẬT DANH SÁCH NHÓM BÊN TRÁI
                        refreshRoomList(updatedRoom);

                        showAlert("Thành công", "Đã đổi tên phòng thành công!");
                    }
                    break;

                case UPDATE_ROOM_NAME_FAILURE:
                    showAlert("Lỗi", message.getPayload() != null
                            ? message.getPayload().toString()
                            : "Không thể đổi tên phòng");
                    break;

                case UPDATE_ROOM_PASSWORD_SUCCESS:
                    // Cập nhật mật khẩu mới trong currentRoom
                    if (message.getPayload() instanceof Room) {
                        Room updatedRoom = (Room) message.getPayload();
                        currentRoom.setPassword(updatedRoom.getPassword());
                        infoPassGroup.setText(updatedRoom.getPassword()); // Cập nhật UI
                        showAlert("Thành công", "Đã cập nhật mật khẩu phòng thành công!");
                    } else {
                        showAlert("Thông báo", "Mật khẩu phòng đã được cập nhật");
                    }
                    break;

                case UPDATE_ROOM_PASSWORD_FAILURE:
                    showAlert("Lỗi", message.getPayload() != null
                            ? message.getPayload().toString()
                            : "Không thể cập nhật mật khẩu phòng");
                    break;
                case SEARCH_ROOM_RESPONSE:
                    List<Room> searchResults = (List<Room>) message.getPayload();
                    String keyword = searchRoomField.getText().trim();
                    showListGroups(searchResults, keyword);
                    break;
                case JOINED_GROUPS_RESPONSE:
                    allGroups = (List<Room>) message.getPayload();
                    showListGroups(allGroups, ""); // Thêm tham số thứ 2 là chuỗi rỗng
                    break;

                case GET_UNREAD_COUNTS_RESPONSE:
                    System.out.println("Received unread counts: " + message.getPayload());
                    unreadCounts = (Map<Integer, Integer>) message.getPayload();
                    showListGroups(allGroups, "");
                    //refreshRoomList();
                    break;

                case NEW_MESSAGE_NOTIFICATION:
                    Integer roomIdWithNewMessage = (Integer) message.getPayload();
                    // Chỉ cập nhật unread count nếu:
                    // 1. Đang không ở trong phòng này
                    // 2. Hoặc phòng này không phải là phòng hiện tại
                    if (currentRoom == null || currentRoom.getId() != roomIdWithNewMessage) {
                        if (!unreadCounts.containsKey(roomIdWithNewMessage)) {
                            unreadCounts.put(roomIdWithNewMessage, 1);
                        } else {
                            unreadCounts.put(roomIdWithNewMessage, unreadCounts.get(roomIdWithNewMessage) + 1);
                        }
                        refreshRoomList();
                    }
                    break;
                default:
                    System.out.println("Received message of type: " + message.getType());
                    break;
            }
        });
    }


public void showListGroups(List<Room> rooms, String highlightKeyword) {
    listGroupContainer.getChildren().clear();
    // Thêm lại headerGroup từ @FXML
    listGroupContainer.getChildren().add(headerGroup);

    if (rooms.isEmpty()) {
        Label emptyLabel = new Label("Bạn chưa tham gia nhóm nào");
        emptyLabel.setStyle("-fx-font-size: 16px; -fx-text-fill: gray;");
        emptyLabel.setLayoutX(10);
        emptyLabel.setLayoutY(10);
        listGroupContainer.getChildren().add(emptyLabel);
        return;
    }

    double layoutY = 62;

    for (Room room : rooms) {
        // Pane đại diện cho nhóm
        Pane groupPane = new Pane();
        groupPane.setPrefSize(198, 62);
        groupPane.setLayoutY(layoutY);
        groupPane.setCursor(Cursor.HAND);

        // Xóa tất cả style class trước khi thêm mới
        groupPane.getStyleClass().clear();
        groupPane.getStyleClass().add("vien-danh-sach-nhom");

        // Thêm style class tùy theo trạng thái
        if (currentRoom != null && room.getId() == currentRoom.getId()) {
            groupPane.getStyleClass().add("active-room");
        } else {
            groupPane.getStyleClass().add("normal-room");
        }

        // Thêm hiệu ứng hover
        groupPane.setOnMouseEntered(e -> {
            if (!(currentRoom != null && room.getId() == currentRoom.getId())) {
                groupPane.setStyle("-fx-background-color: white;");
            }
        });

        groupPane.setOnMouseExited(e -> {
            // Luôn reset về màu đúng theo trạng thái active khi chuột rời đi
            if (currentRoom != null && room.getId() == currentRoom.getId()) {
                groupPane.setStyle("-fx-background-color: #a6a6a6;");
            } else {
                groupPane.setStyle("-fx-background-color: #d9d9d9;");
            }
        });

        // Thêm sự kiện click
        groupPane.setOnMouseClicked(e -> {
            // Gửi yêu cầu tham gia phòng đã tồn tại
            NetworkMessage request = new NetworkMessage(
                    NetworkMessage.MessageType.JOIN_EXISTING_ROOM_REQUEST,
                    room.getId()
            );
            Client.getInstance().sendMessage(request);
        });

        // Label hiển thị tên nhóm
        Label nameLabel = new Label(room.getName());
        nameLabel.setLayoutX(8);
        nameLabel.setLayoutY(17);
        nameLabel.setPrefSize(158, 27);
        nameLabel.setFont(new Font(18));
        nameLabel.setCursor(Cursor.HAND);

        // Highlight nếu có từ khóa tìm kiếm
        if (highlightKeyword != null && !highlightKeyword.isEmpty() &&
                room.getName().toLowerCase().contains(highlightKeyword.toLowerCase())) {
            nameLabel.setStyle("-fx-font-size: 18px; -fx-font-weight: bold; -fx-text-fill: #ff0000;");
        } else {
            nameLabel.setStyle("-fx-font-size: 18px; -fx-font-weight: bold;");
        }


        groupPane.getChildren().add(nameLabel);

        if (unreadCounts.containsKey(room.getId()) && unreadCounts.get(room.getId()) > 0) {
            StackPane indicator = createUnreadIndicator(unreadCounts.get(room.getId()));
            groupPane.getChildren().add(indicator);
        }

        // Thêm vào container
        listGroupContainer.getChildren().add(groupPane);

        // Tăng layoutY cho nhóm tiếp theo
        layoutY += 62;
    }

    // Cập nhật chiều cao động cho AnchorPane nếu cần
    listGroupContainer.setPrefHeight(layoutY);
}
    private StackPane createUnreadIndicator(int count) {
        Circle redDot = new Circle(8, Color.RED);
        Label countLabel = new Label(String.valueOf(count));
        countLabel.setStyle("-fx-font-size: 10px; -fx-text-fill: white; -fx-font-weight: bold;");

        StackPane indicator = new StackPane(redDot, countLabel);
        indicator.setLayoutX(180);
        indicator.setLayoutY(15);
        return indicator;
    }

    public void refreshRoomList() {
        showListGroups(allGroups, ""); // Thêm tham số thứ 2
    }

    public void setRoom(Room room) {
        if (room != null) {
            this.currentRoom = room;

            //Đã đọc tin nhắn
            NetworkMessage markReadRequest = new NetworkMessage(
                    NetworkMessage.MessageType.MARK_MESSAGES_READ_REQUEST,
                    room.getId());
            Client.getInstance().sendMessage(markReadRequest);
            // Cập nhật local unreadCounts
            unreadCounts.remove(room.getId());
            refreshRoomList();

            Platform.runLater(() -> {
                PauseTransition pause = new PauseTransition(Duration.seconds(0.5));
                pause.setOnFinished(e -> requestUnreadCounts());
                pause.play();
            });
            groupNameLabel.setText(room.getName());

            chatBox.getChildren().clear();
            requestRoomHistory(room.getId());
            requestGroupMembers(room.getId());
        }

    }


    private void requestJoinedGroups() {
        NetworkMessage request = new NetworkMessage(
                NetworkMessage.MessageType.GET_JOINED_GROUPS_REQUEST,
                null);
        Client.getInstance().sendMessage(request);

    }

    private void requestRoomHistory(int roomId) {
        NetworkMessage request = new NetworkMessage(
                NetworkMessage.MessageType.GET_ROOM_HISTORY_REQUEST,
                roomId);
        Client.getInstance().sendMessage(request);
    }

    public void requestGroupMembers(int groupId) {
        NetworkMessage request = new NetworkMessage(
                NetworkMessage.MessageType.GET_MEMBERS_GROUP_REQUEST,
                groupId);
        Client.getInstance().sendMessage(request);
    }

    private void requestCurrentUser() {
        NetworkMessage request = new NetworkMessage(
                NetworkMessage.MessageType.GET_USER_REQUEST,
                null);
        Client.getInstance().sendMessage(request);
    }

    // Cập nhật khi tải lịch sử
//    private void showRoomHistory(List<Message> messages) {
//        chatBox.getChildren().clear();
//        allMessages.clear();
//
//        for (Message msg : messages) {
//            addMessageToUI(msg);
//        }
//    }
    private void showRoomHistory(List<Message> history) {
        Platform.runLater(() -> {
            // Xóa tin nhắn cũ
            chatBox.getChildren().clear();
            allMessages.clear();

            // Thêm tất cả tin nhắn lịch sử
            for (Message msg : history) {
                HBox messageContainer;
                if (msg.isFile()) {
                    messageContainer = createFileMessageContainer(msg);
                } else {
                    messageContainer = createTextMessageContainer(msg);
                }
                chatBox.getChildren().add(messageContainer);
                allMessages.add(msg); // Lưu vào danh sách tìm kiếm
            }

            // Cuộn xuống dưới cùng
            scrollToBottom();
        });
    }

    //code mơi



    private void showTextHistory(Message msg) {
        HBox messageContainer = new HBox(10);
        messageContainer.setAlignment(Pos.TOP_LEFT);
        messageContainer.setPadding(new Insets(5));
        messageContainer.setMaxWidth(580);

        // Avatar
        ImageView avatar = new ImageView(new Image(getClass().getResource("/image/icon_avatar.png").toExternalForm()));
        avatar.setFitWidth(42);
        avatar.setFitHeight(44);
        avatar.setPreserveRatio(true);

        // Phần nội dung bên phải
        VBox contentBox = new VBox(3);

        // Dòng thông tin người gửi và thời gian
        HBox infoBox = new HBox(10);
        Label nameLabel = new Label(msg.getFullname());
        nameLabel.setStyle("-fx-font-weight: bold; -fx-font-size: 14;");

        String formattedTime = msg.getSendAt().format(DateTimeFormatter.ofPattern("HH:mm"));
        Label timeLabel = new Label(formattedTime);
        timeLabel.setStyle("-fx-text-fill: #666666; -fx-font-size: 12;");

        infoBox.getChildren().addAll(nameLabel, timeLabel);

        // Phần nội dung tin nhắn
        Text messageText = new Text(msg.getContent());
        messageText.setWrappingWidth(480); // Giới hạn chiều rộng

        TextFlow messageFlow = new TextFlow(messageText);
        messageFlow.setMaxWidth(480);
        messageFlow.setPadding(new Insets(5));
        messageFlow.setStyle("-fx-background-color: #f0f0f0; -fx-background-radius: 10;");

        contentBox.getChildren().addAll(infoBox, messageFlow);
        messageContainer.getChildren().addAll(avatar, contentBox);

        // Tính toán và cập nhật layout
        Platform.runLater(() -> {
            messageFlow.applyCss();
            messageFlow.layout();

            // Đảm bảo chiều cao phù hợp
            double requiredHeight = messageFlow.getHeight() + 30; // +30 cho phần header
            messageContainer.setMinHeight(requiredHeight);

            chatBox.getChildren().add(messageContainer);
            scrollToBottom();
        });
    }

    //    private void addMessageToUI(Message msg) {
//        Platform.runLater(() -> {
//            if (msg.isFile()) {
//                addFileMessageToUI(msg);
//            } else {
//                addTextMessageToUI(msg);
//            }
//            scrollToBottom();
//
//        });
//    }
    private void addMessageToUI(Message msg) {
        Platform.runLater(() -> {
            // 1. Tạo container cho tin nhắn dựa trên loại tin nhắn
            HBox messageContainer;
            if (msg.isFile()) {
                messageContainer = createFileMessageContainer(msg);
            } else {
                messageContainer = createTextMessageContainer(msg);
            }

            // 2. Thêm container vào giao diện chat
            chatBox.getChildren().add(messageContainer);

            // 3. Lưu tin nhắn vào danh sách tất cả tin nhắn
            allMessages.add(msg);

            // 4. Cuộn xuống tin nhắn mới nhất
            scrollToBottom();

            // 5. Áp dụng hiệu ứng cho tin nhắn mới (tuỳ chọn)
            applyNewMessageEffect(messageContainer);
        });
    }

    private HBox createTextMessageContainer(Message msg) {
        HBox messageContainer = new HBox(10);
        messageContainer.setAlignment(Pos.TOP_LEFT);
        messageContainer.setPadding(new Insets(5));
        messageContainer.setMaxWidth(580);

        // Avatar
        ImageView avatar;
        if ("Langflow AI".equalsIgnoreCase(msg.getFullname())) {
            avatar = new ImageView(new Image(getClass().getResource("/image/icon_ai.png").toExternalForm()));
        } else {
            avatar = new ImageView(new Image(getClass().getResource("/image/icon_avatar.png").toExternalForm()));
        }
        avatar.setFitWidth(42);
        avatar.setFitHeight(44);
        avatar.setPreserveRatio(true);

        // Tạo phần nội dung
        VBox contentBox = new VBox(3);

        // Dòng thông tin người gửi và thời gian
        HBox infoBox = new HBox(10);
        Label nameLabel = new Label(msg.getFullname());
        nameLabel.setStyle("-fx-font-weight: bold; -fx-font-size: 14;");

        LocalDateTime sendAt = msg.getSendAt();
        String formattedTime = (sendAt != null) ? sendAt.format(DateTimeFormatter.ofPattern("HH:mm: | dd-MM-yyyy")) : "";
        Label timeLabel = new Label(formattedTime);
        timeLabel.setStyle("-fx-text-fill: #666666; -fx-font-size: 12;");

        infoBox.getChildren().addAll(nameLabel, timeLabel);

        // Nội dung tin nhắn
        Text messageText = new Text(msg.getContent());
        messageText.setWrappingWidth(480);

        TextFlow messageFlow = new TextFlow(messageText);
        messageFlow.setMaxWidth(480);
        messageFlow.setPadding(new Insets(5));
        messageFlow.setStyle("-fx-background-color: #f0f0f0; -fx-background-radius: 10;");

        // Nền đặc biệt nếu là AI
        if ("Langflow AI".equalsIgnoreCase(msg.getFullname())) {
            messageFlow.setStyle("-fx-background-color: #e0f7fa; -fx-background-radius: 10; -fx-border-color: #00acc1; -fx-border-radius: 10;");
        } else {
            messageFlow.setStyle("-fx-background-color: #f0f0f0; -fx-background-radius: 10;");
        }

        contentBox.getChildren().addAll(infoBox, messageFlow);
        messageContainer.getChildren().addAll(avatar, contentBox);

        return messageContainer;
    }

    private HBox createFileMessageContainer(Message msg) {
        HBox messageContainer = new HBox(10);
        messageContainer.setAlignment(Pos.TOP_LEFT);
        messageContainer.setPadding(new Insets(5));
        messageContainer.setMaxWidth(580);

        // Tạo avatar
        ImageView avatar = new ImageView(new Image(getClass().getResource("/image/icon_avatar.png").toExternalForm()));
        avatar.setFitWidth(42);
        avatar.setFitHeight(44);
        avatar.setPreserveRatio(true);

        // Tạo phần nội dung
        VBox contentBox = new VBox(3);

        // Dòng thông tin người gửi và thời gian
        HBox infoBox = new HBox(10);
        Label nameLabel = new Label(msg.getFullname());
        nameLabel.setStyle("-fx-font-weight: bold; -fx-font-size: 14;");

        String formattedTime = msg.getSendAt().format(DateTimeFormatter.ofPattern("HH:mm | dd-MM-yyyy"));
        Label timeLabel = new Label(formattedTime);
        timeLabel.setStyle("-fx-text-fill: #666666; -fx-font-size: 12;");

        infoBox.getChildren().addAll(nameLabel, timeLabel);

        // Tạo nút tải file
        Button downloadButton = new Button(msg.getFileName());
        downloadButton.setStyle("-fx-background-color: #4CAF50; -fx-text-fill: white;");
        downloadButton.setOnAction(e -> handleDownloadFile(msg));

        // Thêm hình ảnh xem trước nếu là ảnh
        if (msg.getFileType() != null && msg.getFileType().startsWith("image/")) {
            try {
                Image image = new Image(new ByteArrayInputStream(msg.getFileData()));
                ImageView imageView = new ImageView(image);
                imageView.setFitWidth(200);
                imageView.setPreserveRatio(true);

                VBox fileBox = new VBox(5, downloadButton, imageView);
                contentBox.getChildren().addAll(infoBox, fileBox);
            } catch (Exception e) {
                contentBox.getChildren().addAll(infoBox, downloadButton);
            }
        } else {
            contentBox.getChildren().addAll(infoBox, downloadButton);
        }

        messageContainer.getChildren().addAll(avatar, contentBox);
        return messageContainer;
    }

    private void applyNewMessageEffect(HBox messageContainer) {
        // Hiệu ứng cho tin nhắn mới
        messageContainer.setOpacity(0);
        Timeline fadeIn = new Timeline(
                new KeyFrame(Duration.seconds(0.3),
                        new KeyValue(messageContainer.opacityProperty(), 1)
                ));
        fadeIn.play();
    }

    /// /code moi

    private void addTextMessageToUI(Message msg) {
        HBox messageContainer = new HBox(10);
        messageContainer.setAlignment(Pos.TOP_LEFT);
        messageContainer.setPadding(new Insets(5));
        messageContainer.setMaxWidth(580);

        // Avatar
        ImageView avatar = new ImageView(new Image(getClass().getResource("/image/icon_avatar.png").toExternalForm()));
        avatar.setFitWidth(42);
        avatar.setFitHeight(44);
        avatar.setPreserveRatio(true);

        // Phần nội dung bên phải
        VBox contentBox = new VBox(3);

        // Dòng thông tin người gửi và thời gian
        HBox infoBox = new HBox(10);
        Label nameLabel = new Label(msg.getFullname());
        nameLabel.setStyle("-fx-font-weight: bold; -fx-font-size: 14;");
        Label timeLabel = new Label();
        if (msg.getSendAt() != null) {
            timeLabel.setText(msg.getSendAt().format(DateTimeFormatter.ofPattern("HH:mm | dd-MM-yyyy")));
        } else {
            timeLabel.setText(""); // hoặc "?" hay không hiển thị gì cả
        }
        //String formattedTime = msg.getSendAt().format(DateTimeFormatter.ofPattern("HH:mm dd-MM-yyyy"));

        timeLabel.setStyle("-fx-text-fill: #666666; -fx-font-size: 12;");

        infoBox.getChildren().addAll(nameLabel, timeLabel);

        // Phần nội dung tin nhắn
        Text messageText = new Text(msg.getContent());
        messageText.setWrappingWidth(480);

        TextFlow messageFlow = new TextFlow(messageText);
        messageFlow.setMaxWidth(480);
        messageFlow.setPadding(new Insets(5));
        messageFlow.setStyle("-fx-background-color: #f0f0f0; -fx-background-radius: 10;");

        contentBox.getChildren().addAll(infoBox, messageFlow);
        messageContainer.getChildren().addAll(avatar, contentBox);

        Platform.runLater(() -> {
            messageFlow.applyCss();
            messageFlow.layout();
            messageContainer.setMinHeight(messageFlow.getHeight() + 30);
            chatBox.getChildren().add(messageContainer);
            scrollToBottom();
        });
    }

    private void addFileMessageToUI(Message msg) {
        HBox messageContainer = new HBox(10);
        messageContainer.setAlignment(Pos.TOP_LEFT);
        messageContainer.setPadding(new Insets(5));
        messageContainer.setMaxWidth(580);

        // Avatar
        ImageView avatar = new ImageView(new Image(getClass().getResource("/image/icon_avatar.png").toExternalForm()));
        avatar.setFitWidth(42);
        avatar.setFitHeight(44);
        avatar.setPreserveRatio(true);

        // Phần nội dung bên phải
        VBox contentBox = new VBox(3);

        // Dòng thông tin người gửi và thời gian
        HBox infoBox = new HBox(10);
        Label nameLabel = new Label(msg.getFullname());
        nameLabel.setStyle("-fx-font-weight: bold; -fx-font-size: 14;");

        String formattedTime = msg.getSendAt().format(DateTimeFormatter.ofPattern("HH:mm dd-MM-yyyy"));
        Label timeLabel = new Label(formattedTime);
        timeLabel.setStyle("-fx-text-fill: #666666; -fx-font-size: 12;");

        infoBox.getChildren().addAll(nameLabel, timeLabel);

        // Phần hiển thị file
        Button downloadButton = new Button(msg.getFileName());
        downloadButton.setStyle("-fx-background-color: #4CAF50; -fx-text-fill: white;");
        downloadButton.setOnAction(e -> handleDownloadFile(msg));

        // Hiển thị hình ảnh trực tiếp nếu là ảnh
        if (msg.getFileType().startsWith("image/")) {
            try {
                Image image = new Image(new ByteArrayInputStream(msg.getFileData()));
                ImageView imageView = new ImageView(image);
                imageView.setFitWidth(200);
                imageView.setPreserveRatio(true);
                imageView.setSmooth(true);
                imageView.setCache(true);

                VBox imageBox = new VBox(5, downloadButton, imageView);
                contentBox.getChildren().addAll(infoBox, imageBox);
            } catch (Exception e) {
                contentBox.getChildren().addAll(infoBox, downloadButton);
            }
        } else {
            contentBox.getChildren().addAll(infoBox, downloadButton);
        }

        Platform.runLater(() -> {
            messageContainer.getChildren().addAll(avatar, contentBox);
            chatBox.getChildren().add(messageContainer);
            scrollToBottom();
        });


    }

    private void scrollToBottom() {
        Platform.runLater(() -> {
            chatBox.heightProperty().addListener((obs, oldVal, newVal) -> {
                scrollPane.applyCss();
                scrollPane.layout();
                scrollPane.setVvalue(1.0);
            });
        });
    }

    private void showGroupMembers(List<User> members) {
        Platform.runLater(() -> {
            // Xóa dữ liệu cũ
            // memberListView.getItems().clear();

            // // Thiết lập CellFactory để tùy chỉnh hiển thị
            // memberListView.setCellFactory(lv -> new ListCell<User>() {

            // private final Label nameLabel = new Label();
            // private final HBox container = new HBox(10);

            // {
            // // Cấu hình giao diện
            // container.setAlignment(Pos.CENTER_LEFT);
            // container.getChildren().addAll(nameLabel);
            // }

            // @Override
            // protected void updateItem(User user, boolean empty) {
            // super.updateItem(user, empty);

            // if (empty || user == null) {
            // setText(null);
            // setGraphic(null);
            // } else {
            // // Hiển thị tên người dùng (ưu tiên fullname)
            // String displayName = user.getFullName() != null &&
            // !user.getFullName().isEmpty()
            // ? user.getFullName()
            // : user.getUsername();
            // nameLabel.setText(displayName);

            // // Có thể thêm logic load avatar riêng ở đây nếu cần
            // setGraphic(container);
            // }
            // }
            // });
            // Thêm tất cả thành viên vào ListView
            // memberListView.getItems().addAll(members);

            // 1. Gán CellFactory cho ListView.
            memberListView.setCellFactory(lv -> new MemberListCell());
            // 2. Xóa dữ liệu cũ và thêm dữ liệu mới.
            // Dùng setItems sẽ hiệu quả hơn clear() và addAll().
            if (members != null) {
                memberListView.setItems(FXCollections.observableArrayList(members));
            } else {
                memberListView.getItems().clear(); // Nếu danh sách là null thì xóa trắng
            }
        });
    }

    private void updateUserInfoUI() {
        if (currentUser != null) {
            Platform.runLater(() -> {
                infoFullNameUser.setText(currentUser.getFullName());
                infoUserNameUser.setText(currentUser.getUsername());
                infoGmailUser.setText(currentUser.getGmail());
                System.out.println("[DEBUG] Đã cập nhật UI cho popup.");
            });
        }
    }

    private void updatePersonalizedUI() {
        if (currentUser != null) {
            String displayName = (currentUser.getFullName() != null && !currentUser.getFullName().isEmpty())
                    ? currentUser.getFullName()
                    : currentUser.getUsername();

            // Cập nhật Label mới
            usernameLabelInHeader.setText(displayName);

            // Ví dụ: nếu user có trường avatarUrl
            // if (currentUser.getAvatarUrl() != null &&
            // !currentUser.getAvatarUrl().isEmpty()) {
            // userAvatarImageView.setImage(new Image(currentUser.getAvatarUrl()));
            // }
        }
    }

    @FXML
    private void showMenu(MouseEvent event) {
        menuPopup.setVisible(true);
        overlay.setVisible(true);
        overlay2.setVisible(true);
        menuPopup.setManaged(true);

        infoNameGroup.setText(currentRoom.getName());
        infoIdGroup.setText(String.valueOf(currentRoom.getId()));
        // Kiểm tra nếu người dùng hiện tại là leader
        if (currentRoom != null && currentUser != null &&
                currentUser.getId() == currentRoom.getLeaderId()) {
            // Hiển thị thông tin nhóm đầy đủ nếu là leader
            passwordRow.setVisible(true);
            passwordRow.setManaged(true);
            infoPassGroup.setText(String.valueOf(currentRoom.getPassword()));
        } else {
            // infoNameGroup.setText("Không có quyền xem");
            // infoIdGroup.setText("");
            // infoPassGroup.setText("");
            // Ẩn các thông tin nếu không phải leader
            passwordRow.setVisible(false);
            passwordRow.setManaged(false);
        }
    }

    @FXML
    private void hideMenu(ActionEvent event) {
        menuPopup.setVisible(false);
        overlay.setVisible(false);
        overlay2.setVisible(false);
        menuPopup.setManaged(false);
    }

    @FXML
    private void showSearchBox(MouseEvent event) {
        // Đảo trạng thái hiển thị của searchPopup
        searchPopup.setVisible(!searchPopup.isVisible());

        // Nếu đang hiển thị thì focus vào ô tìm kiếm
        if (searchPopup.isVisible()) {
            Platform.runLater(() -> {
                searchField.requestFocus();
            });
        }

        event.consume();
    }

    @FXML
    private void showSearchBox2(MouseEvent event) {
        if (searchPopup2.isVisible()) {
            searchPopup2.setVisible(false);
        } else {
            searchPopup2.setVisible(true);
        }
    }

    @FXML
    private void showBoxInfo(MouseEvent event) {
        System.out.println("[DEBUG] showBoxInfo được gọi.");
        if (currentUser == null) {
            showAlert("Lỗi", "Thông tin người dùng chưa sẵn sàng.");
            return;
        }

        // Cập nhật dữ liệu trước khi hiển thị
        updateUserInfoUI();

        // Hiển thị popup và lớp phủ
        boxInfo.setVisible(true);
        overlay.setVisible(true);
        overlay2.setVisible(true); // Nếu bạn dùng cả 2 overlay

        System.out.println("[DEBUG] Đã đặt boxInfo và overlay thành visible.");
        event.consume();
    }

    // === HÀM ẨN POPUP (Gắn với nút Đóng trong FXML) ===
    @FXML
    private void hideBoxInfo(MouseEvent event) {
        System.out.println("[DEBUG] hideBoxInfo được gọi.");

        // Ẩn popup và lớp phủ
        boxInfo.setVisible(false);
        overlay.setVisible(false);
        overlay2.setVisible(false); // Nếu bạn dùng cả 2 overlay

        System.out.println("[DEBUG] Đã đặt boxInfo và overlay thành invisible.");
        if (event != null) {
            event.consume();
        }
    }

    // Hàm thoát
    @FXML
    public void exit(MouseEvent event) {
        event.consume(); // Ngăn đóng ngay lập tức
        // Hiển thị hộp thoại xác nhận
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Xác nhận thoát");
        alert.setHeaderText("Bạn có chắc muốn thoát?");

        Optional<ButtonType> result = alert.showAndWait();
        if (result.isPresent() && result.get() == ButtonType.OK) {
            Platform.exit();
            System.exit(0);
        }
    }

    @FXML
    private void handleHomeClick(MouseEvent event) {
        // Gửi yêu cầu trở về home đến server
        Client.getInstance()
                .sendMessage(new NetworkMessage(NetworkMessage.MessageType.BACK_HOME_REQUEST, null));
        // Không cần chờ phản hồi từ server, sẽ tự động chuyển
        try {
            // Quay về màn hình chọn phòng
            Main.setRoot("chatapp/chatroom");
        } catch (Exception e) {
            e.printStackTrace();
            showAlert(Alert.AlertType.ERROR, "Lỗi: Không thể quay về màn hình chính.");
        }
    }

    private void handleDownloadFile(Message msg) {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Lưu file");
        fileChooser.setInitialFileName(msg.getFileName());
        Stage stage = (Stage) iconSendFile.getScene().getWindow();
        File file = fileChooser.showSaveDialog(stage);

        if (file != null) {
            try {
                Files.write(file.toPath(), msg.getFileData());
                showAlert("Thành công", "File đã được lưu thành công!");
            } catch (Exception e) {
                e.printStackTrace();
                showAlert("Lỗi", "Không thể lưu file: " + e.getMessage());
            }
        }
    }

    private void showAlert(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
    /// / them codde sua mk
    @FXML
    private void handleChangePassword(ActionEvent event) {
        System.out.println("Change password button clicked!");

        // Tạo dialog
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
                return Map.of(
                        "newPassword", newPasswordField.getText(),
                        "confirmPassword", confirmPasswordField.getText()
                );
            }
            return null;
        });

        Optional<Map<String, String>> result = dialog.showAndWait();
        result.ifPresent(passwordData -> {
            System.out.println("Dialog result received!");

            String newPassword = passwordData.get("newPassword");
            String confirmPassword = passwordData.get("confirmPassword");

            if (newPassword == null || newPassword.isEmpty()) {
                showAlert("Lỗi", "Mật khẩu không được để trống!");
                return;
            }

            if (!newPassword.equals(confirmPassword)) {
                showAlert("Lỗi", "Mật khẩu xác nhận không khớp!");
                return;
            }

            if (newPassword.length() < 6) {
                showAlert("Lỗi", "Mật khẩu phải có ít nhất 6 ký tự!");
                return;
            }

            if (currentUser == null) {
                showAlert("Lỗi", "Không tìm thấy thông tin người dùng!");
                return;
            }

            System.out.println("Sending change password request for user: " + currentUser.getId());

            Map<String, Object> payload = new HashMap<>();
            payload.put("userId", currentUser.getId());
            payload.put("newPassword", newPassword);

            NetworkMessage message = new NetworkMessage(
                    NetworkMessage.MessageType.CHANGE_PASSWORD_REQUEST,
                    payload
            );

            Client.getInstance().sendMessage(message);
            System.out.println("Change password request sent!");
        });
    }
    // PrivateRoomController.java
    // Thay đổi từ ActionEvent sang MouseEvent( sua ten hien thi)
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

                NetworkMessage message = new NetworkMessage(
                        NetworkMessage.MessageType.UPDATE_FULLNAME_REQUEST,
                        payload
                );
                Client.getInstance().sendMessage(message);
            }
        });

        event.consume(); // Ngăn sự kiện tiếp tục lan truyền
    }
    // Sua Gmail
    @FXML
    private void handleUpdateGmail(MouseEvent event) {
        TextInputDialog dialog = new TextInputDialog(currentUser.getGmail());
        dialog.setTitle("Đổi Gmail");
        dialog.setHeaderText("Nhập Gmail mới");
        dialog.setContentText("Gmail:");

        Optional<String> result = dialog.showAndWait();
        result.ifPresent(newGmail -> {
            if (!newGmail.trim().isEmpty()) {
                // Kiểm tra định dạng email
                if (!newGmail.matches("^[\\w-\\.]+@([\\w-]+\\.)+[\\w-]{2,4}$")) {
                    showAlert("Lỗi", "Định dạng Gmail không hợp lệ");
                    return;
                }

                Map<String, Object> payload = new HashMap<>();
                payload.put("userId", currentUser.getId());
                payload.put("newGmail", newGmail.trim());

                NetworkMessage message = new NetworkMessage(
                        NetworkMessage.MessageType.UPDATE_GMAIL_REQUEST,
                        payload
                );
                Client.getInstance().sendMessage(message);
            }
        });

        event.consume();
    }
    // sua ten phong
    // PrivateRoomController.java
    @FXML
    private void handleUpdateRoomName(MouseEvent event) {
        if (currentRoom == null || currentUser == null || currentUser.getId() != currentRoom.getLeaderId()) {
            showAlert("Lỗi", "Chỉ chủ phòng mới có quyền sửa tên phòng");
            return;
        }

        TextInputDialog dialog = new TextInputDialog(currentRoom.getName());
        dialog.setTitle("Đổi tên phòng");
        dialog.setHeaderText("Nhập tên phòng mới");
        dialog.setContentText("Tên phòng:");

        Optional<String> result = dialog.showAndWait();
        result.ifPresent(newName -> {
            if (!newName.trim().isEmpty()) {
                Map<String, Object> payload = new HashMap<>();
                payload.put("roomId", currentRoom.getId());
                payload.put("newName", newName.trim());
                payload.put("leaderId", currentUser.getId());

                NetworkMessage message = new NetworkMessage(
                        NetworkMessage.MessageType.UPDATE_ROOM_NAME_REQUEST,
                        payload
                );
                Client.getInstance().sendMessage(message);
            }
        });

        event.consume();
    }
    // sua pass phong
    // PrivateRoomController.java
    @FXML
    private void handleUpdateRoomPassword(MouseEvent event) {
        if (currentRoom == null || currentUser == null || currentUser.getId() != currentRoom.getLeaderId()) {
            showAlert("Lỗi", "Chỉ chủ phòng mới có quyền đổi mật khẩu phòng");
            return;
        }

        // Tạo dialog nhập mật khẩu
        Dialog<String> dialog = new Dialog<>();
        dialog.setTitle("Đổi mật khẩu phòng");
        dialog.setHeaderText("Nhập mật khẩu mới (tối thiểu 4 ký tự)");

        PasswordField passwordField = new PasswordField();
        passwordField.setPromptText("Nhập mật khẩu mới...");

        dialog.getDialogPane().setContent(passwordField);
        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);

        dialog.setResultConverter(buttonType -> {
            return buttonType == ButtonType.OK ? passwordField.getText() : null;
        });

        Optional<String> result = dialog.showAndWait();
        result.ifPresent(newPassword -> {
            if (newPassword.trim().length() < 4) {
                showAlert("Lỗi", "Mật khẩu phải có ít nhất 4 ký tự");
                return;
            }

            // Gửi yêu cầu cập nhật mật khẩu phòng
            Map<String, Object> payload = new HashMap<>();
            payload.put("roomId", currentRoom.getId());
            payload.put("newPassword", newPassword.trim());
            payload.put("leaderId", currentUser.getId());

            NetworkMessage message = new NetworkMessage(
                    NetworkMessage.MessageType.UPDATE_ROOM_PASSWORD_REQUEST,
                    payload
            );
            Client.getInstance().sendMessage(message);
        });

        event.consume();
    }
    // load laij danh sach nhom
    private void refreshRoomList(Room updatedRoom) {
        // Cập nhật tên phòng trong allGroups
        for (Room room : allGroups) {
            if (room.getId() == updatedRoom.getId()) {
                room.setName(updatedRoom.getName());
                break;
            }
        }

        // Load lại danh sách nhóm với từ khóa tìm kiếm hiện tại (nếu có)
        String currentKeyword = searchRoomField != null ? searchRoomField.getText().trim() : "";
        showListGroups(allGroups, currentKeyword);
    }
    // Tim Phong
    @FXML
    private void handleSearchRoom(ActionEvent event) {
        String keyword = searchRoomField.getText().trim();

        if (keyword.isEmpty()) {
            // Nếu từ khóa trống, hiển thị tất cả nhóm không highlight
            showListGroups(allGroups, "");
            return;
        }

        // Lọc danh sách phòng dựa trên từ khóa
        List<Room> filteredRooms = new ArrayList<>();
        for (Room room : allGroups) {
            if (room.getName().toLowerCase().contains(keyword.toLowerCase())) {
                filteredRooms.add(room);
            }
        }

        // Hiển thị kết quả đã lọc với highlight
        showListGroups(filteredRooms, keyword);
    }
    private void requestRoomSearch(String keyword) {
        NetworkMessage request = new NetworkMessage(
                NetworkMessage.MessageType.SEARCH_ROOM_REQUEST,
                keyword
        );
        Client.getInstance().sendMessage(request);
    }

    private void showAlert(Alert.AlertType type, String msg) {
        Alert alert = new Alert(type);
        alert.setTitle("Thông báo");
        alert.setHeaderText(null);
        alert.setContentText(msg);
        alert.showAndWait();
    }

    /**
     * Phương thức này được gọi khi người dùng nhấn vào nút "Rời nhóm".
     * Nó sẽ hiển thị một hộp thoại xác nhận trước khi thực hiện hành động.
     */
    @FXML
    public void handleLeaveRoom() {
        // Kiểm tra xem người dùng có thực sự ở trong phòng không
        if (currentRoom == null) {
            showAlert(Alert.AlertType.ERROR, "Lỗi: Không tìm thấy thông tin phòng hiện tại.");
            return;
        }

        // Tạo một hộp thoại xác nhận
        Alert confirmationDialog = new Alert(Alert.AlertType.CONFIRMATION);
        confirmationDialog.setTitle("Xác nhận rời phòng");
        confirmationDialog.setHeaderText("Bạn có chắc chắn muốn rời khỏi phòng '" + currentRoom.getName() + "' không?");
        confirmationDialog
                .setContentText("Hành động này không thể hoàn tác. Nếu bạn là trưởng phòng, phòng sẽ bị giải tán.");

        // Hiển thị hộp thoại và chờ người dùng phản hồi
        Optional<ButtonType> result = confirmationDialog.showAndWait();

        // Chỉ xử lý nếu người dùng nhấn nút "OK"
        if (result.isPresent() && result.get() == ButtonType.OK) {
            // Người dùng đã xác nhận
            // Gửi yêu cầu rời phòng đến server
            // Không cần payload phức tạp, server biết user nào đang gửi yêu cầu
            // và họ đang ở trong phòng nào (dựa trên currentRoomId của ClientHandler)
            NetworkMessage leaveRequest = new NetworkMessage(MessageType.LEAVE_ROOM_REQUEST, null);
            Client.getInstance().sendMessage(leaveRequest);

            // Ghi chú: Việc chuyển về màn hình chatroom sẽ được xử lý khi nhận được
            // phản hồi từ server (USER_LEFT_ROOM hoặc ROOM_DELETED).
            // Không nên chuyển màn hình ngay tại đây.
        } else {
            // Người dùng đã nhấn "Cancel" hoặc đóng hộp thoại, không làm gì cả.
            System.out.println("Hành động rời phòng đã được hủy.");
        }
    }

    // LỚP NỘI BỘ ĐỂ CUSTOM CELL
    private class MemberListCell extends ListCell<User> {
        private HBox hbox = new HBox(10);
        private Label nameLabel = new Label();
        private Button removeButton = new Button("Xóa");
        private Region spacer = new Region();
        private Circle statusCircle = new Circle(5);

        public MemberListCell() {
            super();

            // Cấu hình layout cho cell
            HBox.setHgrow(spacer, Priority.ALWAYS); // Để nút "Xóa" luôn ở bên phải
            hbox.setAlignment(Pos.CENTER_LEFT);
            hbox.getChildren().addAll(statusCircle, nameLabel, spacer, removeButton);

            // Thêm style cho nút xóa để nó nhỏ và đẹp hơn
            removeButton.setStyle("-fx-background-color: #ffcdd2; -fx-text-fill: #b71c1c; -fx-font-size: 10px;");

            // =======================================================
            // XỬ LÝ SỰ KIỆN CHO NÚT XÓA - PHẦN QUAN TRỌNG NHẤT
            // =======================================================
            removeButton.setOnAction(event -> {
                // Lấy đối tượng User tương ứng với dòng này
                User userToRemove = getItem();

                // Kiểm tra để chắc chắn rằng có một user để xóa
                if (userToRemove == null) {
                    return;
                }

                // Tạo một dialog xác nhận để tránh xóa nhầm
                Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
                alert.setTitle("Xác nhận xóa thành viên");
                alert.setHeaderText("Bạn có chắc chắn muốn xóa thành viên '" + userToRemove.getFullName() + "'?");
                alert.setContentText("Hành động này sẽ xóa họ khỏi phòng chat.");

                // Chờ người dùng nhấn "OK" hoặc "Cancel"
                Optional<ButtonType> result = alert.showAndWait();

                if (result.isPresent() && result.get() == ButtonType.OK) {
                    // Người dùng đã xác nhận
                    System.out.println("Leader requested to remove user with ID: " + userToRemove.getId());

                    // Gửi yêu cầu lên server với payload là ID của người cần xóa
                    Client.getInstance().sendMessage(
                            new NetworkMessage(NetworkMessage.MessageType.REMOVE_MEMBER_REQUEST, userToRemove.getId()));
                }
            });
        }

        @Override
        protected void updateItem(User user, boolean empty) {
            super.updateItem(user, empty);
            if (empty || user == null) {
                // Nếu dòng rỗng, không hiển thị gì cả
                setGraphic(null);
            } else {
                // Nếu có dữ liệu, hiển thị tên và nút
                nameLabel.setText(user.getFullName() + " (@" + user.getUsername() + ")");

                // Hiển thị trạng thái trực tuyến
                boolean isOnline = userStatusMap.getOrDefault(user.getId(), false);
                if (isOnline) {
                    statusCircle.setFill(Color.LIMEGREEN);
                    statusCircle.setStroke(Color.DARKGREEN);
                } else {
                    statusCircle.setFill(Color.LIGHTGRAY);
                    statusCircle.setStroke(Color.DARKGRAY);
                }

                // Điều kiện hiển thị nút "Xóa":
                // 1. Người đang xem phải là leader (biến isLeader của PrivateRoomController).
                // 2. Thành viên trong dòng này không phải là chính leader đó (leader không thể
                // tự xóa mình).
                boolean canRemove = currentRoom != null && currentUser != null &&
                        currentUser.getId() == currentRoom.getLeaderId() && user.getId() != currentUser.getId();
                removeButton.setVisible(canRemove);
                removeButton.setManaged(canRemove); // Quan trọng: setManaged(false) để nút không chiếm không gian khi
                                                    // bị ẩn
                // Đặt HBox làm nội dung đồ họa cho cell
                setGraphic(hbox);
            }
        }
    }

    private void requestUnreadCounts() {
        if (currentUser != null) {
            NetworkMessage request = new NetworkMessage(
                    NetworkMessage.MessageType.GET_UNREAD_COUNTS_REQUEST,
                    null);

            Client.getInstance().sendMessage(request);
        }
    }
}
