// Sửa lại PrivateRoomController.java
package chatapp.controller;

import chatapp.Main;
import chatapp.model.*;
import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.geometry.Bounds;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Cursor;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
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
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.Optional;
import java.io.ByteArrayInputStream;
import javafx.event.Event;

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

    private boolean emojiPaneVisible = false;

    private List<Room> allGroups = new ArrayList<>();

    private Room currentRoom;
    private User currentUser;
    private boolean roomListenersInitialized = false;


    // Khởi tạo
    @FXML
    public void initialize() {
        requestJoinedGroups();
        scrollToBottom();
        requestCurrentUser();
        overlay.setOnMouseClicked(e -> hideMenu(null));


        initializeEmojiPane();
        emojiOverlay.setOnMouseClicked(e -> {
            hideEmojiPane(null);
        });

    }
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
        //emojiPane.addEventFilter(MouseEvent.MOUSE_CLICKED, Event::consume);
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
                        base64Data
                );

                // Gửi message
                Client.getInstance().sendMessage(
                        new NetworkMessage(
                                NetworkMessage.MessageType.SEND_MESSAGE_REQUEST,
                                fileMessage
                        )
                );
            } catch (Exception e) {
                e.printStackTrace();
                showAlert("Lỗi", "Không thể đọc file: " + e.getMessage());
            }
        }
    }


    @Override
    protected void handleServerMessage(NetworkMessage message) {
        switch (message.getType()) {
            case RECEIVE_MESSAGE:
                Message chatMessage = (Message) message.getPayload();
                addMessageToUI(chatMessage);
                break;
            case USER_JOINED_ROOM:
                // Hiển thị thông báo có người mới vào
                break;
            case USER_LEFT_ROOM:
                // Hiển thị thông báo có người rời đi
                break;
            case JOINED_GROUPS_RESPONSE:
                allGroups = (List<Room>) message.getPayload();
                showListGroups(allGroups);
                break;
            case ROOM_HISTORY_RESPONSE:
                List<Message> history = (List<Message>) message.getPayload();
                showRoomHistory(history);
                break;
            case MEMBERS_GROUP_RESPONSE:
                List<User> members = (List<User>) message.getPayload();
                showGroupMembers(members);
                break;
            case USER_RESPONSE:
                this.currentUser = (User) message.getPayload();
                updateUserInfoUI();
                break;
            case JOIN_EXISTING_ROOM_RESPONSE:
                if (message.getPayload() instanceof Room) {
                    Room joinedRoom = (Room) message.getPayload();
                    // Cập nhật giao diện với phòng mới
                    setRoom(joinedRoom);
                    refreshRoomList();
                }
                break;
            // Xử lý các loại message khác...
        }
    }


    public void showListGroups(List<Room> rooms) {
        listGroupContainer.getChildren().clear();
        // Giữ lại Pane "Danh Sách Nhóm" từ FXML
        listGroupContainer.getChildren().clear(); // xóa toàn bộ

        // Thêm lại headerGroup từ @FXML
        listGroupContainer.getChildren().add(headerGroup); // Đảm bảo nó vẫn còn
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
                    groupPane.setStyle("-fx-background-color: white;" );
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

            groupPane.getChildren().add(nameLabel);

            // Nếu muốn hiển thị trạng thái như "online"
//            Circle statusDot = new Circle(180, 31, 4);
//            statusDot.setFill(Color.DODGERBLUE);
//            statusDot.setStroke(Color.BLACK);
//
//            groupPane.getChildren().add(statusDot);

            // Thêm vào container
            listGroupContainer.getChildren().add(groupPane);

            // Tăng layoutY cho nhóm tiếp theo
            layoutY += 62;
        }

        // Cập nhật chiều cao động cho AnchorPane nếu cần
        listGroupContainer.setPrefHeight(layoutY);
    }

    public void refreshRoomList() {
        showListGroups(allGroups);
    }

    public void setRoom(Room room) {
        if (room != null) {
            this.currentRoom = room;
            groupNameLabel.setText(room.getName());

            chatBox.getChildren().clear();
            requestRoomHistory(room.getId());
            requestGroupMembers(room.getId());
        }

    }

//public void setRoom(Room room) {
//    // Dọn dẹp listener và resource của phòng hiện tại trước khi chuyển sang phòng mới
//    cleanupCurrentRoom();
//
//    this.currentRoom = room;
//    if (room != null) {
//        groupNameLabel.setText(room.getName());
//        chatBox.getChildren().clear();
//        requestRoomHistory(room.getId());
//        requestGroupMembers(room.getId());
//        initializeRoomListeners();
//    }
//}
//
//    private void cleanupCurrentRoom() {
//        // Dọn dẹp các listener và resource của phòng hiện tại
//        if (currentRoom != null) {
//            // Thực hiện các thao tác dọn dẹp cần thiết
//            chatBox.getChildren().clear();
//            roomListenersInitialized = false;
//        }
//    }
//
//    private void initializeRoomListeners() {
//        if (!roomListenersInitialized) {
//
//            roomListenersInitialized = true;
//        }
//    }


    private void requestJoinedGroups() {
        NetworkMessage request = new NetworkMessage(
                NetworkMessage.MessageType.GET_JOINED_GROUPS_REQUEST,
                null
        );
        Client.getInstance().sendMessage(request);

    }

    private void requestRoomHistory(int roomId) {
        NetworkMessage request = new NetworkMessage(
                NetworkMessage.MessageType.GET_ROOM_HISTORY_REQUEST,
                roomId
        );
        Client.getInstance().sendMessage(request);
    }

    public void requestGroupMembers(int groupId) {
        NetworkMessage request = new NetworkMessage(
                NetworkMessage.MessageType.GET_MEMBERS_GROUP_REQUEST,
                groupId
        );
        Client.getInstance().sendMessage(request);
    }

    private void requestCurrentUser() {
        NetworkMessage request = new NetworkMessage(
                NetworkMessage.MessageType.GET_USER_REQUEST,
                null
        );
        Client.getInstance().sendMessage(request);
    }


    private void showRoomHistory(List<Message> messages) {
        chatBox.getChildren().clear();
        for (Message msg : messages) {
            addMessageToUI(msg);
        }
    }



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

    private void addMessageToUI(Message msg) {
        Platform.runLater(() -> {
            if (msg.isFile()) {
                addFileMessageToUI(msg);
            } else {
                addTextMessageToUI(msg);
            }
            scrollToBottom();
        });
    }

//    private void addTextMessageToUI(Message msg) {
//        HBox messageContainer = new HBox(10);
//        messageContainer.setAlignment(Pos.TOP_LEFT);
//        messageContainer.setPadding(new Insets(5));
//        messageContainer.setMaxWidth(580);
//
//        // Avatar
//        ImageView avatar = new ImageView(new Image(getClass().getResource("/image/icon_avatar.png").toExternalForm()));
//        avatar.setFitWidth(42);
//        avatar.setFitHeight(44);
//        avatar.setPreserveRatio(true);
//
//        // Phần nội dung bên phải
//        VBox contentBox = new VBox(3);
//
//        // Dòng thông tin người gửi và thời gian
//        HBox infoBox = new HBox(10);
//        Label nameLabel = new Label(msg.getFullname());
//        nameLabel.setStyle("-fx-font-weight: bold; -fx-font-size: 14;");
//        Label timeLabel = new Label();
//        if (msg.getSendAt() != null) {
//            timeLabel.setText(msg.getSendAt().format(DateTimeFormatter.ofPattern("HH:mm | dd-MM-yyyy")));
//        } else {
//            timeLabel.setText(""); // hoặc "?" hay không hiển thị gì cả
//        }
//        //String formattedTime = msg.getSendAt().format(DateTimeFormatter.ofPattern("HH:mm dd-MM-yyyy"));
//
//        timeLabel.setStyle("-fx-text-fill: #666666; -fx-font-size: 12;");
//
//        infoBox.getChildren().addAll(nameLabel, timeLabel);
//
//        // Phần nội dung tin nhắn
//        Text messageText = new Text(msg.getContent());
//        messageText.setWrappingWidth(480);
//
//        TextFlow messageFlow = new TextFlow(messageText);
//        messageFlow.setMaxWidth(480);
//        messageFlow.setPadding(new Insets(5));
//        messageFlow.setStyle("-fx-background-color: #f0f0f0; -fx-background-radius: 10;");
//
//        contentBox.getChildren().addAll(infoBox, messageFlow);
//        messageContainer.getChildren().addAll(avatar, contentBox);
//
//        Platform.runLater(() -> {
//            messageFlow.applyCss();
//            messageFlow.layout();
//            messageContainer.setMinHeight(messageFlow.getHeight() + 30);
//            chatBox.getChildren().add(messageContainer);
//            scrollToBottom();
//        });
//    }
private void addTextMessageToUI(Message msg) {
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
        timeLabel.setText("");
    }
    timeLabel.setStyle("-fx-text-fill: #666666; -fx-font-size: 12;");

    infoBox.getChildren().addAll(nameLabel, timeLabel);

    // Nội dung tin nhắn
    Text messageText = new Text(msg.getContent());
    messageText.setWrappingWidth(480);

    TextFlow messageFlow = new TextFlow(messageText);
    messageFlow.setMaxWidth(480);
    messageFlow.setPadding(new Insets(5));

    // Nền đặc biệt nếu là AI
    if ("Langflow AI".equalsIgnoreCase(msg.getFullname())) {
        messageFlow.setStyle("-fx-background-color: #e0f7fa; -fx-background-radius: 10; -fx-border-color: #00acc1; -fx-border-radius: 10;");
    } else {
        messageFlow.setStyle("-fx-background-color: #f0f0f0; -fx-background-radius: 10;");
    }

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
            memberListView.getItems().clear();

            // Thiết lập CellFactory để tùy chỉnh hiển thị
            memberListView.setCellFactory(lv -> new ListCell<User>() {

                private final Label nameLabel = new Label();
                private final HBox container = new HBox(10);

                {
                    // Cấu hình giao diện
                    container.setAlignment(Pos.CENTER_LEFT);
                    container.getChildren().addAll(nameLabel);
                }

                @Override
                protected void updateItem(User user, boolean empty) {
                    super.updateItem(user, empty);

                    if (empty || user == null) {
                        setText(null);
                        setGraphic(null);
                    } else {
                        // Hiển thị tên người dùng (ưu tiên fullname)
                        String displayName = user.getFullName() != null && !user.getFullName().isEmpty()
                                ? user.getFullName()
                                : user.getUsername();
                        nameLabel.setText(displayName);

                        // Có thể thêm logic load avatar riêng ở đây nếu cần
                        setGraphic(container);
                    }
                }
            });
            // Thêm tất cả thành viên vào ListView
            memberListView.getItems().addAll(members);
        });
    }

    private void updateUserInfoUI() {
        if (currentUser != null) {
            Platform.runLater(() -> {
                infoFullNameUser.setText(currentUser.getFullName());
                infoUserNameUser.setText(currentUser.getUsername());
                infoPassUser.setText(currentUser.getPassword()); // Note: Consider security implications
                infoGmailUser.setText(currentUser.getGmail());
            });
        }
    }


    @FXML
    private void showMenu(MouseEvent event) {
        menuPopup.setVisible(true);
        overlay.setVisible(true);
        overlay2.setVisible(true);
        menuPopup.setManaged(true);

        // Kiểm tra nếu người dùng hiện tại là leader
        if (currentRoom != null && currentUser != null &&
                currentUser.getId() == currentRoom.getLeaderId()) {
            infoNameGroup.setText(currentRoom.getName());
            infoIdGroup.setText(String.valueOf(currentRoom.getId()));
            infoPassGroup.setText(currentRoom.getPassword());
        } else {
            infoNameGroup.setText("Không có quyền xem");
            infoIdGroup.setText("");
            infoPassGroup.setText("");
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
    private void showSearchBox(MouseEvent event){
        if(searchPopup.isVisible()){
            searchPopup.setVisible(false);
        }
        else{
            searchPopup.setVisible(true);
        }
    }

    @FXML
    private void showSearchBox2(MouseEvent event){
        if(searchPopup2.isVisible()){
            searchPopup2.setVisible(false);
        }
        else{
            searchPopup2.setVisible(true);
        }
    }

    @FXML
    private void showBoxInfo(MouseEvent event) {
        boxInfo.setVisible(true);
        overlay.setVisible(true);
        overlay2.setVisible(true);
    }



    @FXML
    private void hideBoxInfo(MouseEvent event) {
        boxInfo.setVisible(false);
        overlay.setVisible(false);
        overlay2.setVisible(false);
    }

    // Hàm thoát
    @FXML
    public void exit(MouseEvent event){
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
        // Gửi yêu cầu rời phòng đến server
        Client.getInstance()
                .sendMessage(new NetworkMessage(NetworkMessage.MessageType.LEAVE_ROOM_REQUEST, currentRoom.getId()));
        try {
            // Quay về màn hình chọn phòng
            Main.setRoot("chatapp/chatroom");
        } catch (Exception e) {
            e.printStackTrace();
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

}

