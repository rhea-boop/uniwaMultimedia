package uniwa.media.client;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.layout.StackPane;
import javafx.scene.web.WebEngine;
import javafx.scene.web.WebView;
import javafx.concurrent.Worker.State;
import javafx.stage.Stage;

import java.net.URL;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SecondaryController {
    private static final Logger logger = LoggerFactory.getLogger(SecondaryController.class);
    
    @FXML private Label fileLabel;
    @FXML private WebView webView;
    @FXML private StackPane webContainer;

    private static final double DEFAULT_WIDTH = 800;
    private static final double DEFAULT_HEIGHT = 450; // 16:9 aspect ratio

    @FXML
    public void initialize() {
        fileLabel.setText(ClientSession.selectedFile);
        
        // hls url
        String title = ClientSession.selectedFile.replaceAll("-\\d+p\\.[^.]+", "");
        String hlsUrl = "http://localhost:8081/hls/" + title + "/master.m3u8";

       // get resolution from filename
        String resolution = ClientSession.selectedFile.replaceAll(".*-(\\d+)p\\..*", "$1");
        final int height;
        height = Integer.parseInt(resolution);

        double aspectRatio = 16.0/9.0;
        int width = (int)(height * aspectRatio);

        WebEngine engine = webView.getEngine();
        URL resourceUrl = getClass().getResource("/uniwa/media/hls-player.html");
        
        if (resourceUrl != null) {
            String htmlPath = resourceUrl.toExternalForm();
            engine.load(htmlPath + "?src=" + hlsUrl);
            logger.info("Loading HLS player with URL: {}", hlsUrl);
            
            engine.getLoadWorker().stateProperty().addListener((obs, oldState, newState) -> {
                if (newState == State.SUCCEEDED) {
                    Platform.runLater(() -> {
                        new Thread(() -> {
                            try {
                                Thread.sleep(500);
                                Platform.runLater(() -> resizeWindow(width, height));
                            } catch (InterruptedException e) {
                                logger.error("Interrupted while waiting to resize window", e);
                            }
                        }).start();
                    });
                }
            });
        } else {
            logger.error("Could not find hls-player.html resource");
            engine.loadContent(
                "<html><body style='background:black;color:white;text-align:center;padding:20px'>" +
                "<h2>Error loading player</h2>" +
                "<p>HLS player resource not found</p>" +
                "<p>URL: " + hlsUrl + "</p>" +
                "</body></html>"
            );
        }
    }

    private void resizeWindow(int width, int height) {
        int paddingWidth = 40;
        int paddingHeight = 80;
        
        Stage stage = (Stage) webView.getScene().getWindow();
        
        stage.setWidth(Math.max(DEFAULT_WIDTH, width + paddingWidth));
        stage.setHeight(Math.max(DEFAULT_HEIGHT, height + paddingHeight));
        stage.centerOnScreen();
        
        stage.setTitle("Playing: " + ClientSession.selectedFile);
        
        logger.debug("Resized window to: {}x{}", stage.getWidth(), stage.getHeight());
    }

    @FXML
    private void switchToPrimary() throws Exception {
        logger.debug("Switching back to primary view");
        ClientMain.setRoot("primary");
    }
}
