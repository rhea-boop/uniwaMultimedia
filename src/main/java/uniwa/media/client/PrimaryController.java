package uniwa.media.client;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.net.URL;
import java.util.List;

import fr.bmartel.speedtest.SpeedTestSocket;
import fr.bmartel.speedtest.SpeedTestReport;
import fr.bmartel.speedtest.inter.ISpeedTestListener;
import fr.bmartel.speedtest.model.SpeedTestError;
import javafx.scene.control.ChoiceBox;
import javafx.scene.control.Label;
import javafx.application.Platform;
import javafx.fxml.FXML;

public class PrimaryController {

    @FXML private ChoiceBox<String> formatChoice;
    @FXML private ChoiceBox<String> resolutionChoice;

    @FXML private Label speedLabel;
    
    @FXML
    private javafx.scene.control.ChoiceBox<String> videoChoice;

    @FXML
    private Label label;

    private double measuredMbps = 0.0;
    
    @FXML
    private void switchToSecondary() throws IOException {
        ClientSession.selectedFile = resolutionChoice.getValue();
        ClientMain.setRoot("secondary");
    }
    
    @FXML
    public void initialize() {
        formatChoice.getItems().addAll("mp4", "mkv", "avi");
        formatChoice.setValue("mp4");
    }

    @FXML
    private void fetchResolutions() {
        String fmt = formatChoice.getValue();
        try (Socket s = new Socket("localhost", 8080);
             BufferedReader in = new BufferedReader(
                 new InputStreamReader(s.getInputStream()));
             PrintWriter out = new PrintWriter(s.getOutputStream(), true)) {

            // 1) consume greeting
            in.readLine();
            // 2) send speed+format
            out.printf("LIST %.2f %s%n", measuredMbps, fmt);
            // 3) read server response
            String line = in.readLine();
            if (line != null && line.startsWith("VIDEO_LIST ")) {
                String payload = line.substring("VIDEO_LIST ".length());
                List<String> items = List.of(payload.split(","));
                Platform.runLater(() -> {
                    resolutionChoice.getItems().setAll(items);
                    if (!items.isEmpty()) resolutionChoice.setValue(items.get(0));
                });
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @FXML
    private void measureSpeed() {
        speedLabel.setText("Speed: measuring…");

        try {
            SpeedTestSocket speedTestSocket = new SpeedTestSocket();
            speedTestSocket.addSpeedTestListener(new ISpeedTestListener() {
                @Override
                public void onCompletion(SpeedTestReport report) {
                    double mbps = report.getTransferRateBit().doubleValue() / 1_000_000.0;
                    Platform.runLater(() ->
                        speedLabel.setText(String.format("Speed: %.2f Mbps", mbps))
                    );
                    measuredMbps = mbps;
                }

                @Override
                public void onError(SpeedTestError speedTestError, String errorMessage) {
                    Platform.runLater(() ->
                        speedLabel.setText("Speed error: " + errorMessage)
                    );
                }

                @Override
                public void onProgress(float percent, SpeedTestReport report) {
                    // lmao
                }
            });

            speedTestSocket.startFixedDownload("http://speedtest.tele2.net/5MB.zip", 5000);


        } catch (Exception e) {
            speedLabel.setText("Speed: error");
            e.printStackTrace();
        }
    }

   
}
