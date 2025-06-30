// D:\Education\Lap_trinh_mang\PROJECT\chat\src\main\java\chatapp\Main.java
package chatapp;

import java.io.IOException; // import

import chatapp.model.Client;
import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;
import java.net.URL; // Thêm import này ở đầu file

public class Main extends Application {

    private static Stage primaryStage;

    @Override
    public void start(Stage stage) throws Exception {
        primaryStage = stage;

        // Nạp Truststore của client từ classpath (trong thư mục resources/security)
        URL trustStoreUrl = Main.class.getResource("/security/clienttruststore.jks");
        if (trustStoreUrl != null) {
            System.setProperty("javax.net.ssl.trustStore", trustStoreUrl.getPath());
            System.setProperty("javax.net.ssl.trustStorePassword", "secretpassword");
            System.out.println("Client TrustStore loaded from: " + trustStoreUrl.getPath());
        } else {
            System.err.println("Could not find clienttruststore.jks in classpath! Make sure it is in 'src/main/resources/security'");
            // Dừng ứng dụng nếu không tìm thấy truststore
            return;
        }

        // Kết nối tới Server khi ứng dụng khởi động
        try {
            Client.getInstance().connect("localhost", 12345); // Kết nối tới server tại localhost:12345
        } catch (IOException e) {
            System.err.println("Could not connect to the secure server. Error: " + e.getMessage());
            e.printStackTrace(); // In ra chi tiết lỗi để debug
            // TODO: Hiển thị Alert cho người dùng
            return;
        }

        setRoot("chatapp/login"); // chạy login.fxml lúc đầu
        primaryStage.setTitle("Ứng dụng Chat");
        primaryStage.show();

        // Đảm bảo ngắt kết nối khi đóng cửa sổ
        stage.setOnCloseRequest(event -> {
            Client.getInstance().disconnect();
        });
    }

    public static void setRoot(String fxmlPath) throws IOException {
        FXMLLoader loader = new FXMLLoader(Main.class.getResource("/" + fxmlPath + ".fxml"));
        Parent root = loader.load();

        // Lấy controller và set client cho nó nếu cần
        Object controller = loader.getController();
        if (controller instanceof chatapp.controller.BaseController) {
            ((chatapp.controller.BaseController) controller).initializeController();
        }

        primaryStage.setScene(new Scene(root));
    }

    public static void main(String[] args) {
        launch();
    }
}