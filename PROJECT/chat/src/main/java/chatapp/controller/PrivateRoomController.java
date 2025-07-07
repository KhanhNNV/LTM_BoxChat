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
import javafx.scene.text.Text;
import javafx.scene.text.TextFlow;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import javafx.util.Duration;

import javafx.scene.layout.StackPane;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;



public class PrivateRoomController extends BaseController {

    @FXML
    private Pane headerGroup;

    @FXML
    private AnchorPane listGroupContainer;

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
    private ScrollPane emojiPane;
    @FXML
    private ImageView iconSendEmoji;
    @FXML
    private Pane emojiOverlay;

    @FXML
    private HBox passwordRow;

    @FXML
    private Label usernameLabelInHeader;


    private boolean emojiPaneVisible = false;

    private List<Room> allGroups = new ArrayList<>();

    private Room currentRoom;
    private User currentUser;
    private Map<Integer, Boolean> userStatusMap = new HashMap<>();

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

        inputTextArea.addEventFilter(KeyEvent.KEY_PRESSED, event -> {
            if (event.getCode() == KeyCode.ENTER) {
                event.consume();

                if (event.isShiftDown()) {
                    inputTextArea.appendText("\n");
                } else {
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

        for (int i = 0; i < chatBox.getChildren().size(); i++) {
            Node node = chatBox.getChildren().get(i);
            if (node instanceof HBox) {
                HBox messageContainer = (HBox) node;
                Message message = allMessages.get(i);

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

        currentSearchIndex = 0;
        scrollToMessage(foundMessages.get(0));

        createSearchResultsMenu(matchedMessages);
    }

    private void scrollToMessage(HBox messageBox) {
        Bounds boundsInScene = messageBox.localToScene(messageBox.getBoundsInLocal());
        double targetY = boundsInScene.getMinY();

        double scrollHeight = scrollPane.getContent().getBoundsInLocal().getHeight();
        double viewportHeight = scrollPane.getViewportBounds().getHeight();
        double scrollValue = (targetY - 100) / (scrollHeight - viewportHeight);

        scrollValue = Math.max(0, Math.min(1, scrollValue));
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

        searchResultsMenu.show(searchField, Side.BOTTOM, 0, 0);
    }
    private void initializeEmojiPane() {
        emojiPane.setVisible(false);
        emojiOverlay.setVisible(false);

        emojiPane.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        emojiPane.setVbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);

        GridPane emojiGrid = new GridPane();
        emojiGrid.setHgap(5);
        emojiGrid.setVgap(5);
        emojiGrid.setPadding(new Insets(7));

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

        int columns = 10;
        for (int i = 0; i < emojis.length; i++) {
            int row = i / columns;
            int col = i % columns;

            Label emojiLabel = new Label(emojis[i]);
            emojiLabel.setStyle("-fx-font-size: 14px; -fx-cursor: hand;");

            emojiLabel.setOnMouseClicked(e -> {
                String currentText = inputTextArea.getText();
                inputTextArea.setText(currentText + emojiLabel.getText());

                scrollPane.setVvalue(scrollPane.getVvalue());
                e.consume();
            });

            emojiGrid.add(emojiLabel, col, row);
        }

        emojiPane.setContent(emojiGrid);
    }

    @FXML
    private void showEmojiPane(MouseEvent event) {
        emojiPane.setVisible(true);
        emojiOverlay.setVisible(true);

        if (emojiPaneVisible) {
            Bounds iconBounds = iconSendEmoji.localToScene(iconSendEmoji.getBoundsInLocal());
            Bounds paneBounds = emojiPane.getParent().sceneToLocal(iconBounds);

            emojiPane.setLayoutX(paneBounds.getMinX() - emojiPane.getWidth() + 30);
            emojiPane.setLayoutY(paneBounds.getMinY() - emojiPane.getHeight() - 10);

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
                byte[] fileData = Files.readAllBytes(file.toPath());

                String fileName = file.getName();
                String fileType = Files.probeContentType(file.toPath());
                if (fileType == null) {
                    fileType = "application/octet-stream";
                }

                String base64Data = Base64.getEncoder().encodeToString(fileData);

                String fileMessage = String.format(
                        "FILE:%s:%s:%d:%s",
                        fileName,
                        fileType,
                        fileData.length,
                        base64Data);

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
                    userStatusMap.clear();
                    for (User u : membersWithStatus) {
                        userStatusMap.put(u.getId(), u.isOnline());
                    }
                    showGroupMembers(membersWithStatus);
                    break;
                case USER_STATUS_UPDATE:
                    User userWithStatus = (User) message.getPayload();
                    userStatusMap.put(userWithStatus.getId(), userWithStatus.isOnline());
                    memberListView.refresh();
                    break;
                case USER_RESPONSE:
                    this.currentUser = (User) message.getPayload();
                    updateUserInfoUI();
                    updatePersonalizedUI();
                    break;
                case JOIN_EXISTING_ROOM_RESPONSE:
                    if (message.getPayload() instanceof Room) {
                        Room joinedRoom = (Room) message.getPayload();
                        setRoom(joinedRoom);
                        refreshRoomList();
                    }
                    break;
                case BACK_HOME_SUCCESS:
                    try {
                        Main.setRoot("chatapp/chatroom");
                    } catch (Exception e) {
                        e.printStackTrace();
                        showAlert(Alert.AlertType.ERROR, "Lỗi: Không thể quay về màn hình chính.");
                    }
                    break;
                case YOU_HAVE_BEEN_REMOVED:
                    showAlert(Alert.AlertType.WARNING, "Bạn đã bị trưởng phòng xóa khỏi nhóm.");
                    try {
                        Main.setRoot("chatapp/chatroom");
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                    break;
                case CHANGE_PASSWORD_SUCCESS:
                    showAlert("Thành công", (String) message.getPayload());
                    break;

                case CHANGE_PASSWORD_FAILURE:
                    showAlert("Lỗi", (String) message.getPayload());
                    break;
                case UPDATE_FULLNAME_SUCCESS:
                    showAlert("Thành công", (String) message.getPayload());
                    requestCurrentUser();
                    break;
                case UPDATE_FULLNAME_FAILURE:
                    showAlert("Lỗi", (String) message.getPayload());
                    break;
                case UPDATE_GMAIL_SUCCESS:
                    showAlert("Thành công", (String) message.getPayload());
                    requestCurrentUser();
                    break;
                case UPDATE_GMAIL_FAILURE:
                    showAlert("Lỗi", (String) message.getPayload());
                    break;
                case UPDATE_ROOM_NAME_SUCCESS:
                    if (message.getPayload() instanceof Room) {
                        Room updatedRoom = (Room) message.getPayload();
                        currentRoom.setName(updatedRoom.getName());
                        groupNameLabel.setText(updatedRoom.getName());
                        infoNameGroup.setText(updatedRoom.getName());
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
                    if (message.getPayload() instanceof Room) {
                        Room updatedRoom = (Room) message.getPayload();
                        currentRoom.setPassword(updatedRoom.getPassword());
                        infoPassGroup.setText(updatedRoom.getPassword());
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
                    showListGroups(allGroups, "");
                    break;

                case GET_UNREAD_COUNTS_RESPONSE:
                    System.out.println("Received unread counts: " + message.getPayload());
                    unreadCounts = (Map<Integer, Integer>) message.getPayload();
                    showListGroups(allGroups, "");
                    break;

                case NEW_MESSAGE_NOTIFICATION:
                    Integer roomIdWithNewMessage = (Integer) message.getPayload();
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
    listGroupContainer.getChildren().add(headerGroup);

    if (rooms.isEmpty()) {
        Label emptyLabel = new Label();
        emptyLabel.setStyle("-fx-font-size: 16px; -fx-text-fill: gray;");
        emptyLabel.setLayoutX(10);
        emptyLabel.setLayoutY(10);
        listGroupContainer.getChildren().add(emptyLabel);
        return;
    }

    double layoutY = 62;

    for (Room room : rooms) {
        Pane groupPane = new Pane();
        groupPane.setPrefSize(198, 62);
        groupPane.setLayoutY(layoutY);
        groupPane.setCursor(Cursor.HAND);

        groupPane.getStyleClass().clear();
        groupPane.getStyleClass().add("vien-danh-sach-nhom");

        if (currentRoom != null && room.getId() == currentRoom.getId()) {
            groupPane.getStyleClass().add("active-room");
        } else {
            groupPane.getStyleClass().add("normal-room");
        }

        groupPane.setOnMouseEntered(e -> {
            if (!(currentRoom != null && room.getId() == currentRoom.getId())) {
                groupPane.setStyle("-fx-background-color: white;");
            }
        });

        groupPane.setOnMouseExited(e -> {
            if (currentRoom != null && room.getId() == currentRoom.getId()) {
                groupPane.setStyle("-fx-background-color: #a6a6a6;");
            } else {
                groupPane.setStyle("-fx-background-color: #d9d9d9;");
            }
        });

        groupPane.setOnMouseClicked(e -> {
            NetworkMessage request = new NetworkMessage(
                    NetworkMessage.MessageType.JOIN_EXISTING_ROOM_REQUEST,
                    room.getId()
            );
            Client.getInstance().sendMessage(request);
        });

        Label nameLabel = new Label(room.getName());
        nameLabel.setLayoutX(8);
        nameLabel.setLayoutY(17);
        nameLabel.setPrefSize(158, 27);
        nameLabel.setFont(new Font(18));
        nameLabel.setCursor(Cursor.HAND);

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

        listGroupContainer.getChildren().add(groupPane);

        layoutY += 62;
    }

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
        showListGroups(allGroups, "");
    }

    public void setRoom(Room room) {
        if (room != null) {
            this.currentRoom = room;

            NetworkMessage markReadRequest = new NetworkMessage(
                    NetworkMessage.MessageType.MARK_MESSAGES_READ_REQUEST,
                    room.getId());
            Client.getInstance().sendMessage(markReadRequest);
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

    private void showRoomHistory(List<Message> history) {
        Platform.runLater(() -> {
            chatBox.getChildren().clear();
            allMessages.clear();

            for (Message msg : history) {
                HBox messageContainer;
                if (msg.isFile()) {
                    messageContainer = createFileMessageContainer(msg);
                } else {
                    messageContainer = createTextMessageContainer(msg);
                }
                chatBox.getChildren().add(messageContainer);
                allMessages.add(msg);
            }

            scrollToBottom();
        });
    }

    private void addMessageToUI(Message msg) {
        Platform.runLater(() -> {
            HBox messageContainer;
            if (msg.isFile()) {
                messageContainer = createFileMessageContainer(msg);
            } else {
                messageContainer = createTextMessageContainer(msg);
            }

            chatBox.getChildren().add(messageContainer);

            allMessages.add(msg);

            scrollToBottom();

            applyNewMessageEffect(messageContainer);
        });
    }

    private HBox createTextMessageContainer(Message msg) {
        HBox messageContainer = new HBox(10);
        messageContainer.setAlignment(Pos.TOP_LEFT);
        messageContainer.setPadding(new Insets(5));
        messageContainer.setMaxWidth(580);

        ImageView avatar;
        if ("Langflow AI".equalsIgnoreCase(msg.getFullname())) {
            avatar = new ImageView(new Image(getClass().getResource("/image/icon_ai.png").toExternalForm()));
        } else {
            avatar = new ImageView(new Image(getClass().getResource("/image/icon_avatar.png").toExternalForm()));
        }
        avatar.setFitWidth(42);
        avatar.setFitHeight(44);
        avatar.setPreserveRatio(true);

        VBox contentBox = new VBox(3);

        HBox infoBox = new HBox(10);
        Label nameLabel = new Label(msg.getFullname());
        nameLabel.setStyle("-fx-font-weight: bold; -fx-font-size: 14px;");

        LocalDateTime sendAt = msg.getSendAt();
        String formattedTime = (sendAt != null) ? sendAt.format(DateTimeFormatter.ofPattern("HH:mm | dd-MM-yyyy")) : "";
        Label timeLabel = new Label(formattedTime);
        timeLabel.setStyle("-fx-text-fill: #666666; -fx-font-size: 12px;");

        infoBox.getChildren().addAll(nameLabel, timeLabel);

        Text messageText = new Text(msg.getContent());
        messageText.setWrappingWidth(480);

        TextFlow messageFlow = new TextFlow(messageText);
        messageFlow.setMaxWidth(480);
        messageFlow.setPadding(new Insets(5));

        if ("Langflow AI".equalsIgnoreCase(msg.getFullname())) {
            messageFlow.setStyle("-fx-background-color: #e0f7fa; -fx-background-radius: 10px; -fx-border-color: #00acc1; -fx-border-radius: 10px;");
        } else {
            messageFlow.setStyle("-fx-background-color: #f0f0f0; -fx-background-radius: 10px;");
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

        ImageView avatar = new ImageView(new Image(getClass().getResource("/image/icon_avatar.png").toExternalForm()));
        avatar.setFitWidth(42);
        avatar.setFitHeight(44);
        avatar.setPreserveRatio(true);

        VBox contentBox = new VBox(3);

        HBox infoBox = new HBox(10);
        Label nameLabel = new Label(msg.getFullname());
        nameLabel.setStyle("-fx-font-weight: bold; -fx-font-size: 14px;");

        String formattedTime = msg.getSendAt().format(DateTimeFormatter.ofPattern("HH:mm | dd-MM-yyyy"));
        Label timeLabel = new Label(formattedTime);
        timeLabel.setStyle("-fx-text-fill: #666666; -fx-font-size: 12px;");

        infoBox.getChildren().addAll(nameLabel, timeLabel);

        Button downloadButton = new Button(msg.getFileName());
        downloadButton.setStyle("-fx-background-color: #4CAF50; -fx-text-fill: white;");
        downloadButton.setOnAction(e -> handleDownloadFile(msg));

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
        messageContainer.setOpacity(0);
        Timeline fadeIn = new Timeline(
                new KeyFrame(Duration.seconds(0.3),
                        new KeyValue(messageContainer.opacityProperty(), 1)
                ));
        fadeIn.play();
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
            memberListView.setCellFactory(lv -> new MemberListCell());

            if (members != null) {
                memberListView.setItems(FXCollections.observableArrayList(members));
            } else {
                memberListView.getItems().clear();
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

            usernameLabelInHeader.setText(displayName);

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
            passwordRow.setVisible(true);
            passwordRow.setManaged(true);
            infoPassGroup.setText(String.valueOf(currentRoom.getPassword()));
        } else {
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
        searchPopup.setVisible(!searchPopup.isVisible());
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

        updateUserInfoUI();

        boxInfo.setVisible(true);
        overlay.setVisible(true);
        overlay2.setVisible(true);

        System.out.println("[DEBUG] Đã đặt boxInfo và overlay thành visible.");
        event.consume();
    }

    @FXML
    private void hideBoxInfo(MouseEvent event) {
        System.out.println("[DEBUG] hideBoxInfo được gọi.");

        boxInfo.setVisible(false);
        overlay.setVisible(false);
        overlay2.setVisible(false); // Nếu bạn dùng cả 2 overlay

        System.out.println("[DEBUG] Đã đặt boxInfo và overlay thành invisible.");
        if (event != null) {
            event.consume();
        }
    }

    @FXML
    public void exit(MouseEvent event) {
        event.consume();
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
        Client.getInstance()
                .sendMessage(new NetworkMessage(NetworkMessage.MessageType.BACK_HOME_REQUEST, null));
        try {
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

    @FXML
    private void handleChangePassword(ActionEvent event) {
        System.out.println("Change password button clicked!");

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

        event.consume();
    }

    @FXML
    private void handleUpdateGmail(MouseEvent event) {
        TextInputDialog dialog = new TextInputDialog(currentUser.getGmail());
        dialog.setTitle("Đổi Gmail");
        dialog.setHeaderText("Nhập Gmail mới");
        dialog.setContentText("Gmail:");

        Optional<String> result = dialog.showAndWait();
        result.ifPresent(newGmail -> {
            if (!newGmail.trim().isEmpty()) {
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

    @FXML
    private void handleUpdateRoomPassword(MouseEvent event) {
        if (currentRoom == null || currentUser == null || currentUser.getId() != currentRoom.getLeaderId()) {
            showAlert("Lỗi", "Chỉ chủ phòng mới có quyền đổi mật khẩu phòng");
            return;
        }

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

    private void refreshRoomList(Room updatedRoom) {
        for (Room room : allGroups) {
            if (room.getId() == updatedRoom.getId()) {
                room.setName(updatedRoom.getName());
                break;
            }
        }

        String currentKeyword = searchRoomField != null ? searchRoomField.getText().trim() : "";
        showListGroups(allGroups, currentKeyword);
    }

    @FXML
    private void handleSearchRoom(ActionEvent event) {
        String keyword = searchRoomField.getText().trim();

        if (keyword.isEmpty()) {
            showListGroups(allGroups, "");
            return;
        }

        List<Room> filteredRooms = new ArrayList<>();
        for (Room room : allGroups) {
            if (room.getName().toLowerCase().contains(keyword.toLowerCase())) {
                filteredRooms.add(room);
            }
        }

        showListGroups(filteredRooms, keyword);
    }


    private void showAlert(Alert.AlertType type, String msg) {
        Alert alert = new Alert(type);
        alert.setTitle("Thông báo");
        alert.setHeaderText(null);
        alert.setContentText(msg);
        alert.showAndWait();
    }


    @FXML
    public void handleLeaveRoom() {
        // Kiểm tra xem người dùng có thực sự ở trong phòng không
        if (currentRoom == null) {
            showAlert(Alert.AlertType.ERROR, "Lỗi: Không tìm thấy thông tin phòng hiện tại.");
            return;
        }


        Alert confirmationDialog = new Alert(Alert.AlertType.CONFIRMATION);
        confirmationDialog.setTitle("Xác nhận rời phòng");
        confirmationDialog.setHeaderText("Bạn có chắc chắn muốn rời khỏi phòng '" + currentRoom.getName() + "' không?");
        confirmationDialog
                .setContentText("Hành động này không thể hoàn tác. Nếu bạn là trưởng phòng, phòng sẽ bị giải tán.");


        Optional<ButtonType> result = confirmationDialog.showAndWait();


        if (result.isPresent() && result.get() == ButtonType.OK) {
            NetworkMessage leaveRequest = new NetworkMessage(MessageType.LEAVE_ROOM_REQUEST, null);
            Client.getInstance().sendMessage(leaveRequest);
        } else {
            System.out.println("Hành động rời phòng đã được hủy.");
        }
    }

    private class MemberListCell extends ListCell<User> {
        private HBox hbox = new HBox(10);
        private Label nameLabel = new Label();
        private Button removeButton = new Button("Xóa");
        private Region spacer = new Region();
        private Circle statusCircle = new Circle(5);

        public MemberListCell() {
            super();

            HBox.setHgrow(spacer, Priority.ALWAYS);
            hbox.setAlignment(Pos.CENTER_LEFT);
            hbox.getChildren().addAll(statusCircle, nameLabel, spacer, removeButton);

            removeButton.setStyle("-fx-background-color: #ffcdd2; -fx-text-fill: #b71c1c; -fx-font-size: 10px;");
            removeButton.setOnAction(event -> {
                User userToRemove = getItem();
                if (userToRemove == null) {
                    return;
                }
                Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
                alert.setTitle("Xác nhận xóa thành viên");
                alert.setHeaderText("Bạn có chắc chắn muốn xóa thành viên '" + userToRemove.getFullName() + "'?");
                alert.setContentText("Hành động này sẽ xóa họ khỏi phòng chat.");

                Optional<ButtonType> result = alert.showAndWait();

                if (result.isPresent() && result.get() == ButtonType.OK) {
                    System.out.println("Leader requested to remove user with ID: " + userToRemove.getId());

                    Client.getInstance().sendMessage(
                            new NetworkMessage(NetworkMessage.MessageType.REMOVE_MEMBER_REQUEST, userToRemove.getId()));
                }
            });
        }

        @Override
        protected void updateItem(User user, boolean empty) {
            super.updateItem(user, empty);
            if (empty || user == null) {
                setGraphic(null);
            } else {
                nameLabel.setText(user.getFullName() + " (@" + user.getUsername() + ")");

                boolean isOnline = userStatusMap.getOrDefault(user.getId(), false);
                if (isOnline) {
                    statusCircle.setFill(Color.LIMEGREEN);
                    statusCircle.setStroke(Color.DARKGREEN);
                } else {
                    statusCircle.setFill(Color.LIGHTGRAY);
                    statusCircle.setStroke(Color.DARKGRAY);
                }

                boolean canRemove = currentRoom != null && currentUser != null &&
                        currentUser.getId() == currentRoom.getLeaderId() && user.getId() != currentUser.getId();
                removeButton.setVisible(canRemove);
                removeButton.setManaged(canRemove);
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
