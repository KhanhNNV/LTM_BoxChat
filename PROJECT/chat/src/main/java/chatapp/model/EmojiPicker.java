package chatapp.model;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.scene.control.ListView;
import javafx.scene.layout.Pane;

public class EmojiPicker extends Pane {
    private final ListView<String> emojiListView;
    private final ObservableList<String> emojis = FXCollections.observableArrayList(
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
    );

    public ListView<String> getEmojiListView() {
        return emojiListView;
    }

    public EmojiPicker() {
        emojiListView = new ListView<>(emojis);
        emojiListView.setPrefSize(200, 150);
        emojiListView.setStyle("-fx-font-size: 20; -fx-font-family: 'Segoe UI Emoji', 'Apple Color Emoji', sans-serif;");
        this.getChildren().add(emojiListView);
        this.setStyle("-fx-background-color: white; -fx-border-color: #ccc; -fx-border-radius: 5; -fx-background-radius: 5;");
        this.setVisible(false);

        // Close when clicking outside
        this.setOnMouseExited(e -> {
            if (!this.getBoundsInLocal().contains(e.getX(), e.getY())) {
                this.setVisible(false);
            }
        });
    }
}