package uniwa.media.client;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.net.URL;

import fr.bmartel.speedtest.SpeedTestSocket;
import fr.bmartel.speedtest.SpeedTestReport;
import fr.bmartel.speedtest.inter.ISpeedTestListener;
import fr.bmartel.speedtest.model.SpeedTestError;
import javafx.scene.control.Label;
import javafx.application.Platform;
import javafx.fxml.FXML;

public class PrimaryController {

    @FXML private Label speedLabel;

    @FXML
    private javafx.scene.control.ChoiceBox<String> videoChoice;

    @FXML
    private Label label;

    @FXML
    private void switchToSecondary() throws IOException {
        ClientMain.setRoot("secondary");
    }

    @FXML
    private void sendLabelContent() {
        try (Socket socket = new Socket("localhost", 8080)) {
            String message = label.getText();
            PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
            
            out.println(message);
            System.out.println("Sent to server: " + message);
            ClientMain.setRoot("secondary");
            
        } catch (IOException e) {
            System.err.println("Error sending message: " + e.getMessage());
        }
    }

    @FXML
    private void loadVideoList() {
        try (Socket s = new Socket("localhost", 8080);
            BufferedReader in = new BufferedReader(new InputStreamReader(s.getInputStream()));
            PrintWriter out = new PrintWriter(s.getOutputStream(), true)) {
            
            // ① Read and discard the server’s greeting
            String greeting = in.readLine();
            System.out.println("Server says: " + greeting);
            
            // ② Now request the list
            out.println("LIST");
            
            // ③ Read the actual list
            String line = in.readLine();
            if (line != null && line.startsWith("VIDEO_LIST ")) {
                String[] files = line.substring(11).split(",");
                videoChoice.getItems().setAll(files);
            } else {
                System.err.println("Unexpected response: " + line);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @FXML
    private void measureSpeed() {
        speedLabel.setText("Speed: measuring…");

        try {
            // 1️⃣ Create the speed-test socket
            SpeedTestSocket speedTestSocket = new SpeedTestSocket();

            // 2️⃣ Add a listener implementing the correct callbacks
            speedTestSocket.addSpeedTestListener(new ISpeedTestListener() {
                @Override
                public void onCompletion(SpeedTestReport report) {
                    // Called when the fixed-duration download finishes
                    double mbps = report.getTransferRateBit().doubleValue() / 1_000_000.0;
                    // Update the JavaFX label on the UI thread
                    Platform.runLater(() ->
                        speedLabel.setText(String.format("Speed: %.2f Mbps", mbps))
                    );
                }

                @Override
                public void onError(SpeedTestError speedTestError, String errorMessage) {
                    // Called on any error
                    Platform.runLater(() ->
                        speedLabel.setText("Speed error: " + errorMessage)
                    );
                }

                @Override
                public void onProgress(float percent, SpeedTestReport report) {
                    // Optional: show intermediate progress if you like
                    // Platform.runLater(() ->
                    //     speedLabel.setText(String.format("Measuring: %.0f%%", percent))
                    // );
                }
            });

            // 3️⃣ Start a fixed-duration download (5 000 ms)
            // “Download for max 5 000 ms from that URL”
            speedTestSocket.startFixedDownload("http://speedtest.tele2.net/5MB.zip", 10000);


        } catch (Exception e) {
            speedLabel.setText("Speed: error");
            e.printStackTrace();
        }
    }
}
