package uniwa.media.client;

import java.io.IOException;
import java.net.Socket;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

public class ClientMain extends Application {
    private static final Logger logger = LoggerFactory.getLogger(ClientMain.class);
    
    private static Scene scene;
    private static Stage primaryStage;

    @Override
    public void start(Stage stage) throws IOException {
        primaryStage = stage;
        scene = new Scene(loadFXML("primary"), 640, 480);
        stage.setScene(scene);
        stage.setTitle("Media Player");
        stage.show();
        logger.info("Application started");
    }

    static void setRoot(String fxml) throws IOException {
        logger.debug("Changing view to: {}", fxml);
        scene.setRoot(loadFXML(fxml));
        
        if ("primary".equals(fxml)) {
            primaryStage.setTitle("Media Player - Select Video");
            primaryStage.setWidth(640);
            primaryStage.setHeight(480);
        } else if ("secondary".equals(fxml)) {
            primaryStage.setTitle("Media Player - Loading...");
        }
    }

    private static Parent loadFXML(String fxml) throws IOException {
        String resourcePath = "/uniwa/media/" + fxml + ".fxml";
        logger.debug("Loading FXML from {}", resourcePath);
        FXMLLoader fxmlLoader = new FXMLLoader(ClientMain.class.getResource(resourcePath));
        return fxmlLoader.load();
    }

    public static Stage getPrimaryStage() {
        return primaryStage;
    }
    
    public static void main(String[] args) {
       try (Socket socket = new Socket("localhost", 8080)) {
            logger.info("Connected to server: {}:{}", socket.getInetAddress(), socket.getPort());
            launch();
        } catch (IOException e) {
            logger.error("Error connecting to server: {}", e.getMessage());
        }
    }
}
