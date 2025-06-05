// server/ClientHandler.java
package uniwa.media.server;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import uniwa.media.server.VideoLibraryBuilder;

public class ClientHandler implements Runnable {
    private static final Logger logger = LoggerFactory.getLogger(ClientHandler.class);
    
    private final Socket clientSocket;
    
    public ClientHandler(Socket socket) {
        this.clientSocket = socket;
    }

    private static final Map<String,Integer> STREAM_PORTS = Map.of(
    "TCP",    5000,
    "UDP",    5001,
    "RTP/UDP",5002
    );

    @Override
    public void run() {
        try (BufferedReader in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
             PrintWriter out = new PrintWriter(clientSocket.getOutputStream(), true)) {
            
            // Protocol handling
            out.println("HELLO Streaming Server 1.0");
            String inputLine;
            while ((inputLine = in.readLine()) != null) {
                logger.info("Received from client: {}", inputLine);            

                if (inputLine.startsWith("LIST")) {    
                    double mbps = 0.0;
                    String format="mp4";
                    String[] parts = inputLine.split(" ");
                    if (parts.length > 1) {
                        try {
                            mbps = Double.parseDouble(parts[1]);
                        } catch (NumberFormatException e) {
                            logger.warn("Invalid speed value: {}", parts[1]);
                        }
                    }
                    
                    if (parts.length >= 3) {
                        format = parts[2];
                        logger.debug("Requested format: {}", format);
                    }

                    Map<String, Double> bitrateThresholds = Map.of(
                        "240p", 0.4,
                        "360p", 0.75,
                        "480p", 1.0,
                        "720p", 2.5,
                        "1080p", 4.5
                    );
                    
                    final String fmt = format;
                    final double speed = mbps;
                    
                    Pattern resolutionPattern = Pattern.compile(".*-(\\d{3,4}p)\\." + Pattern.quote(fmt) + "$");
                    
                    String list = VideoLibraryBuilder.getAllFiles().stream()
                        .map(File::getName)
                        .filter(name -> name.endsWith("." + fmt)) // filter by requested format
                        .filter(name -> {
                            Matcher matcher = resolutionPattern.matcher(name);
                            if (matcher.matches()) {
                                String resolution = matcher.group(1);
                                // if resolution requested exists and speed is sufficient
                                return bitrateThresholds.containsKey(resolution) && 
                                       speed >= bitrateThresholds.get(resolution);
                            }
                            return false;
                        })
                        .collect(Collectors.joining(","));
                    
                    logger.info("Sending video list: {}", list);
                    out.println("VIDEO_LIST " + list);
                } else if (inputLine.startsWith("QUIT")) {
                    logger.info("Client requested disconnect");
                    break;
                } else {
                    logger.warn("Unknown command: {}", inputLine);
                    out.println("ERROR Unknown command");
                }
            }
            
        } catch (Exception e) {
            logger.error("Error handling client connection", e);
        } finally {
            try {
                clientSocket.close();
                logger.debug("Client socket closed");
            } catch (IOException e) {
                logger.error("Error closing client socket", e);
            }
        }
    }
}