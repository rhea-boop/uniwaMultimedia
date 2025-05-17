package uniwa.media.client;

import java.io.IOException;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.List;

import javafx.fxml.FXML;
import javafx.scene.control.ChoiceBox;
import javafx.scene.control.Label;

public class SecondaryController {
    @FXML 
    private Label fileLabel;
    @FXML
    private ChoiceBox<String> protocolChoice;
    @FXML
    private Label label;
    private static final List<String> PROTOCOLS = List.of("TCP", "UDP", "RTP/UDP");
    
    @FXML
    public void initialize() {
        // Display chosen file
        fileLabel.setText(ClientSession.selectedFile);

        // Populate protocols
        protocolChoice.getItems().setAll(PROTOCOLS);

        // Auto-select default based on resolution
        String res = ClientSession.selectedFile.replaceAll(".*-(\\d+)p\\..*", "$1");
        int prt = Integer.parseInt(res);
        String def;
        if (prt <= 240)       def = "TCP";
        else if (prt <= 480)  def = "UDP";
        else                def = "RTP/UDP";
        protocolChoice.setValue(def);
    }
    
    @FXML
    private void startStreaming() {
        String file = ClientSession.selectedFile;
        String protocol = protocolChoice.getValue();
        ClientSession.selectedProtocol = protocol;

        new Thread(() -> {
            try (Socket socket = new Socket("localhost", 8080)) {
                PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
                out.println("START " + file + " " + protocol);
                socket.getInputStream().readAllBytes(); // Wait for server response
                out.printf("PLAY %s %s%n", file, protocol);
                out.flush();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }).start();
    }

    @FXML
    private void goBack() throws IOException {
        ClientMain.setRoot("primary");
    }
}