// server/ServerMain.java
package uniwa.media.server;

import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import uniwa.media.server.VideoLibraryBuilder;


public class ServerMain {
    private static final int PORT = 8080;
    private static final int MAX_THREADS = 10;
    private static final ExecutorService threadPool = Executors.newFixedThreadPool(MAX_THREADS);

    public static void main(String[] args) {
        System.out.println("Building video library...");
        VideoLibraryBuilder.buildLibrary();
        System.out.println("Video library ready.");
        try (ServerSocket serverSocket = new ServerSocket(PORT)) {
            System.out.println("Streaming Server started on port " + PORT);
            
            while (true) {
                Socket clientSocket = serverSocket.accept();
                System.out.println("New client connected: " + clientSocket.getInetAddress());
                
                // Handle each client in a separate thread
                threadPool.execute(new ClientHandler(clientSocket));
            }
        } catch (Exception e) {
            System.err.println("Server error: " + e.getMessage());
        }
    }
}