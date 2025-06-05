package uniwa.media.client;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.net.URL;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import fr.bmartel.speedtest.SpeedTestSocket;
import fr.bmartel.speedtest.SpeedTestReport;
import fr.bmartel.speedtest.inter.ISpeedTestListener;
import fr.bmartel.speedtest.model.SpeedTestError;
import javafx.scene.control.ChoiceBox;
import javafx.scene.control.Label;
import javafx.application.Platform;
import javafx.fxml.FXML;

public class PrimaryController {
    private static final Logger logger = LoggerFactory.getLogger(PrimaryController.class);

    @FXML private ChoiceBox<String> formatChoice;
    @FXML private ChoiceBox<String> resolutionChoice;
    @FXML private Label speedLabel;
    @FXML private javafx.scene.control.ChoiceBox<String> videoChoice;
    @FXML private Label label;

    private double measuredMbps = 0.0;
    private SpeedTestSocket speedTestSocket = new SpeedTestSocket();
    
    @FXML
    private void switchToSecondary() throws IOException {
        ClientSession.selectedFile = resolutionChoice.getValue();
        logger.info("Selected file for playback: {}", ClientSession.selectedFile);
        ClientMain.setRoot("secondary");
    }
    
    @FXML
    public void initialize() {
        logger.debug("Initializing PrimaryController");
        formatChoice.getItems().addAll("mp4", "mkv", "avi");
        formatChoice.setValue("mp4");
        
        formatChoice.getSelectionModel().selectedItemProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal != null && !newVal.equals(oldVal)) {
                logger.debug("Format changed from {} to {}", oldVal, newVal);
                fetchResolutions();
            }
        });
    }

    @FXML
    private void sendLabelContent() {
        try (Socket socket = new Socket("localhost", 8080)) {
            String message = label.getText();
            PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
            
            out.println(message);
            logger.info("Sent to server: {}", message);
            ClientMain.setRoot("secondary");
            
        } catch (IOException e) {
            logger.error("Error sending message: {}", e.getMessage());
        }
    }

    @FXML
    private void fetchResolutions() {
        String fmt = formatChoice.getValue();
        logger.debug("Fetching resolutions for format: {}", fmt);
        
        try (Socket s = new Socket("localhost", 8080);
             BufferedReader in = new BufferedReader(
                 new InputStreamReader(s.getInputStream()));
             PrintWriter out = new PrintWriter(s.getOutputStream(), true)) {

            // 1) consume greeting
            in.readLine();
            
            // 2) send speed+format
            String command = String.format("LIST %.2f %s", measuredMbps, fmt);
            out.println(command);
            logger.debug("Sent command: {}", command);
            
            // 3) read server response
            String line = in.readLine();
            if (line != null && line.startsWith("VIDEO_LIST ")) {
                String payload = line.substring("VIDEO_LIST ".length());
                List<String> items = List.of(payload.split(","));
                logger.info("Received {} video options from server", items.size());
                
                Platform.runLater(() -> {
                    resolutionChoice.getItems().setAll(items);
                    if (!items.isEmpty()) resolutionChoice.setValue(items.get(0));
                });
            }
        } catch (Exception e) {
            logger.error("Error fetching resolutions", e);
        }
    }

    @FXML
    private void measureSpeed() {
        logger.info("Starting speed measurement...");
        speedLabel.setText("Measuring...");

        try {
            speedTestSocket.addSpeedTestListener(new ISpeedTestListener() {
                @Override
                public void onCompletion(SpeedTestReport report) {
                    double mbps = report.getTransferRateBit().doubleValue() / (1000 * 1000);
                    logger.info("Speed test completed: {} Mbps", mbps);
                    
                    Platform.runLater(() ->
                        speedLabel.setText(String.format("Speed: %.2f Mbps", mbps))
                    );
                    measuredMbps = mbps;
                    
                    Platform.runLater(() -> fetchResolutions());
                }

                @Override
                public void onError(SpeedTestError speedTestError, String errorMessage) {
                    logger.error("Speed test error: {}", errorMessage);
                    Platform.runLater(() ->
                        speedLabel.setText("Speed error: " + errorMessage)
                    );
                }

                @Override
                public void onProgress(float percent, SpeedTestReport report) {
                    double mbps = report.getTransferRateBit().doubleValue() / (1000 * 1000);
                    logger.debug("Speed test progress: {}%, current speed: {} Mbps", percent, mbps);
                }
            });

            speedTestSocket.startFixedDownload("http://speedtest.tele2.net/5MB.zip", 5000);
        } catch (Exception e) {
            logger.error("Error during speed measurement", e);
            speedLabel.setText("Speed: error");
        }
    }
}
