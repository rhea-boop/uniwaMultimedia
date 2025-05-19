package uniwa.media.client;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import javafx.application.Platform;
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
    private Map<String,Integer> STREAM_PORTS = Map.of(
        "TCP",    5000,
        "UDP",    5001,
        "RTP/UDP",5002
    );
    
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
        String file     = ClientSession.selectedFile;
        String protocol = protocolChoice.getValue();
        int    port     = STREAM_PORTS.get(protocol);
        String serverIP = "localhost";

        String uri;
        switch (protocol) {
            case "TCP":
                uri = "tcp://0.0.0.0:" + port + "?listen";
                break;
            case "UDP":
                uri = "udp://0.0.0.0:" + port + "?listen";
                break;
            default: // RTP/UDP
                uri = "rtp://0.0.0.0:" + port + "?listen";
                break;
        }
        List<String> fullCmd = buildLowLatencyFfplayCommand(uri, file, protocol);

        // Run in background
        new Thread(() -> {
            try {
                new ProcessBuilder(fullCmd)
                    .inheritIO()
                    .start();

                Thread.sleep(500);

                try (Socket s = new Socket(serverIP, 8080);
                    BufferedReader in = new BufferedReader(new InputStreamReader(s.getInputStream()));
                    PrintWriter out = new PrintWriter(s.getOutputStream(), true)) {

                    in.readLine();  // consume greeting
                    out.printf("PLAY %s %s%n", file, protocol);
                    String response = in.readLine();
                    if (!response.startsWith("OK")) {
                        throw new IOException("Server error: " + response);
                    }
                }

            } catch (Exception ex) {
                ex.printStackTrace();
                Platform.runLater(() ->
                    fileLabel.setText("Streaming error: " + ex.getMessage())
                );
            }
        }).start();
    }

    /** helper method to build the full ffplay command */
    private List<String> buildLowLatencyFfplayCommand(String uri, String title, String protocol) {
        List<String> base = new ArrayList<>(List.of(
            "ffplay", "-window_title", title, "-autoexit",
            "-fflags", "+nobuffer",
            "-flags", "low_delay",
            "-probesize", "32",
            "-analyzeduration", "0",
            "-infbuf",
            "-framedrop",
            "-sync", "ext",
            "-protocol_whitelist", "file,udp,rtp,tcp"
        ));
        base.addAll(List.of("-f", "mpegts"));

        base.addAll(List.of("-i", uri));
        
        return base;
    }

    @FXML
    private void goBack() throws IOException {
        ClientMain.setRoot("primary");
    }
}