package uniwa.media.client;

import java.io.IOException;
import java.net.Socket;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;
import uniwa.media.App;

public class ClientMain extends Application {
    private static Scene scene;

    @Override
    public void start(Stage stage) throws IOException {
        scene = new Scene(loadFXML("primary"), 640, 480);
        stage.setScene(scene);
        stage.show();
    }

    static void setRoot(String fxml) throws IOException {
        scene.setRoot(loadFXML(fxml));
    }

    private static Parent loadFXML(String fxml) throws IOException {
        FXMLLoader fxmlLoader = new FXMLLoader(ClientMain.class.getResource("/uniwa/media/" + fxml + ".fxml"));
        return fxmlLoader.load();
    }
    public static void main(String[] args) {
       try (Socket socket = new Socket("localhost", 8080)) {
            System.out.println("Connected to server: " + socket.getInetAddress() + ":" + socket.getPort());
            launch();
        } catch (IOException e) {
            System.err.println("Error connecting to server: " + e.getMessage());
        }
    }
}
