package uniwa.media.client;

import java.io.IOException;
import java.io.PrintWriter;
import java.net.Socket;

import javafx.fxml.FXML;
import javafx.scene.control.Label;

public class SecondaryController {

    @FXML
    private Label label;

    @FXML
    private void switchToPrimary() throws IOException {
        ClientMain.setRoot("primary");
    }

    @FXML
    private void sendLabelContent() {
        try (Socket socket = new Socket("localhost", 8080)) {
            String message = label.getText();
            PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
            
            out.println(message);
            System.out.println("Sent to server: " + message);
            ClientMain.setRoot("primary");
            
        } catch (IOException e) {
            System.err.println("Error sending message: " + e.getMessage());
        }
    }
}