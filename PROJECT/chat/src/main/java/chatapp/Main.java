package chatapp;

import java.io.IOException;
import chatapp.model.Client;
import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;
import java.net.URL;

public class Main extends Application {

    private static Stage primaryStage;

    @Override
    public void start(Stage stage) throws Exception {
        primaryStage = stage;

        URL trustStoreUrl = Main.class.getResource("/security/clienttruststore.jks");
        if (trustStoreUrl != null) {
            System.setProperty("javax.net.ssl.trustStore", trustStoreUrl.getPath());
            System.setProperty("javax.net.ssl.trustStorePassword", "secretpassword");
            System.out.println("Client TrustStore loaded from: " + trustStoreUrl.getPath());
        } else {
            System.err.println("Could not find clienttruststore.jks in classpath! Make sure it is in 'src/main/resources/security'");
            return;
        }

        try {
            Client.getInstance().connect("localhost", 12345);
        } catch (IOException e) {
            System.err.println("Could not connect to the secure server. Error: " + e.getMessage());
            e.printStackTrace();
            // TODO: Hiển thị Alert cho người dùng
            return;
        }

        setRoot("chatapp/login");
        primaryStage.setTitle("Ứng dụng Chat");
        primaryStage.show();

        stage.setOnCloseRequest(event -> {
            Client.getInstance().disconnect();
        });
    }

    public static void setRoot(String fxmlPath) throws IOException {
        FXMLLoader loader = new FXMLLoader(Main.class.getResource("/" + fxmlPath + ".fxml"));
        Parent root = loader.load();

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