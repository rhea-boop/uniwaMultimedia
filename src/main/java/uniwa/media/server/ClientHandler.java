// server/ClientHandler.java
package uniwa.media.server;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.stream.Collectors;
import uniwa.media.server.VideoLibraryBuilder;

public class ClientHandler implements Runnable {
    private final Socket clientSocket;
    
    public ClientHandler(Socket socket) {
        this.clientSocket = socket;
    }

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
                    String list = VideoLibraryBuilder.getAllFiles()
                                    .stream()
                                    .map(File::getName)
                                    .collect(Collectors.joining(","));
                    out.println("VIDEO_LIST " + list);
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