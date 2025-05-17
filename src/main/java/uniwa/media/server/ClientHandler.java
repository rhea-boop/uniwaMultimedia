// server/ClientHandler.java
package uniwa.media.server;

import java.io.BufferedReader;
import java.io.File;
import java.io.IO;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import uniwa.media.server.VideoLibraryBuilder;

public class ClientHandler implements Runnable {
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
                System.out.println("Received from client: " + inputLine);
                
                // Handle commands (to be expanded later)
                if (inputLine.startsWith("LIST")) {
                    // Build a JSON or comma-separated list of files from VideoLibraryBuilder
                    double mbps = 0.0;
                    String format="mp4";
                    String[] parts = inputLine.split(" ");
                    if (parts.length > 2 ) {
                        mbps = Double.parseDouble(parts[1]);
                        format = parts[2];
                    }

                    if (parts.length >= 3) {
                        format = parts[2];
                        System.out.println("Requested format: " + format);
                    }

                    Map<String, Double> bitrateThresholds = Map.of(
                    "240p", 0.4,
                    "360p", 0.75,
                    "480p", 1.0,
                    "720p", 2.5,
                    "1080p", 4.5
                    );
 
                    List<File> allFiles = VideoLibraryBuilder.getAllFiles();
                    List<String> matchingVideos = new ArrayList<>();
                    
                   Pattern p = Pattern.compile(".*-(\\d+p)\\." + Pattern.quote(format));
                    for (File f : allFiles) {
                        String fn = f.getName();
                        Matcher m = p.matcher(fn);
                        if (mbps <= 0.0) {
                            if (m.matches() && mbps >= bitrateThresholds.getOrDefault(m.group(1), 0.4)) {
                                matchingVideos.add(fn);
                            }
                        } else if (m.matches() && mbps >= bitrateThresholds.getOrDefault(m.group(1), Double.MAX_VALUE)) {
                            matchingVideos.add(fn);
                        }
                    }

                    String response = String.join(",", matchingVideos);
                    System.out.println("Sending video list: " + response);
                    out.println("VIDEO_LIST " + response);

                } 
                
                else if (inputLine.startsWith("PLAY")) {
                    String[] request = inputLine.split("\\s+");
                    if (request.length == 3) {
                        String fileName = request[1];
                        String protocol = request[2];
                        Integer port = STREAM_PORTS.get(protocol);
                        String clientIP = clientSocket.getInetAddress().getHostAddress();
                        
                        List<String> cmd = new ArrayList<>();
                        cmd.add("ffmpeg");
                        cmd.add("-re");
                        cmd.add("-i");
                        cmd.add("videos/" + fileName);
                        cmd.add("-codec");
                        cmd.add("copy");
                        cmd.add("-f");
                        switch (protocol) {
                            case "TCP" : 
                                cmd.add("mpegts");
                                cmd.add("tcp://" + clientIP + ":" + port);
                                break;
                            
                            case "UDP" :  
                                cmd.add("mpegts");
                                cmd.add("udp://" + clientIP + ":" + port);
                                break;

                            default: 
                                cmd.add("rtp");
                                cmd.add("rtp://" + clientIP + ":" + port);
                                break;
                        }

                        try {
                            new ProcessBuilder(cmd)
                            .inheritIO()
                            .redirectErrorStream(true)
                            .start();
                            out.println("Started playing");
                        } catch (IOException e) { 
                            out.println("Error starting ffmpeg: " + e.getMessage());
                        }
                    }
                } else if (inputLine.startsWith("QUIT")) {
                    break;
                } else {
                    out.println("ERROR Unknown command");
                }
            }
            
        } catch (Exception e) {
            System.err.println("Client handler error: " + e.getMessage());
        } finally {
            try {
                clientSocket.close();
            } catch (Exception e) {
                System.err.println("Error closing socket: " + e.getMessage());
            }
        }
    }
}