// server/ServerMain.java
package uniwa.media.server;

import java.io.*;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;

public class ServerMain {
    private static final Logger logger = LoggerFactory.getLogger(ServerMain.class);
    
    private static final int PORT = 8080;
    private static final int HTTP_PORT = 8081;
    private static final int MAX_THREADS = 10;
    private static final ExecutorService threadPool = Executors.newFixedThreadPool(MAX_THREADS);

    public static void main(String[] args) {
        try {
            logger.info("Building video library...");
            VideoLibraryBuilder.buildLibrary();
            logger.info("Video library ready.");
            
            // Process HLS content, skipping already generated content
            logger.info("Processing HLS content...");
            processHLSContent(new File("videos"));
            logger.info("HLS content processing complete");
            
            startHttpServer();
            startSocketServer();
            
        } catch (Exception e) {
            logger.error("Server initialization failed", e);
        }
    }
    
    private static void processHLSContent(File videoDir) {
        if (videoDir.exists() && videoDir.isDirectory() && videoDir.listFiles() != null) {
            Map<String, List<File>> filesByBase = new HashMap<>();
            
            for (File f : videoDir.listFiles()) {
                if (!isVideoFile(f.getName())) continue;
                String base = f.getName().replaceAll("-\\d+p\\..*", "");     
                filesByBase.computeIfAbsent(base, k -> new ArrayList<>()).add(f);
            }
            
            logger.info("Found {} unique video titles", filesByBase.size());
            
            for (String base : filesByBase.keySet()) {
                File masterPlaylist = new File("hls/" + base + "/master.m3u8");
                
                if (masterPlaylist.exists()) {
                    logger.info("HLS content already exists for {}, skipping", base);
                } else {
                    try {
                        logger.info("Generating HLS content for {}", base);
                        HLSBuilder.buildHLSForMovie(base);
                    } catch (Exception e) {
                        logger.error("HLS generation failed for {}: {}", base, e.getMessage());
                    }
                }
            }
        } else {
            logger.warn("Videos directory not found or empty");
        }
    }
    
    private static boolean isVideoFile(String filename) {
        String lowerName = filename.toLowerCase();
        return lowerName.endsWith(".mp4") || 
               lowerName.endsWith(".mkv") || 
               lowerName.endsWith(".avi") ||
               lowerName.endsWith(".mov");
    }
    
    private static void startHttpServer() throws IOException {
        HttpServer httpServer = HttpServer.create(new InetSocketAddress(HTTP_PORT), 0);
        
        httpServer.createContext("/hls", new HttpHandler() {
            @Override
            public void handle(HttpExchange exchange) throws IOException {
                String uriPath = exchange.getRequestURI().getPath();
                File file = new File("." + uriPath);
                
                logger.debug("HTTP request: {}", uriPath);
                
                if (!file.exists() || file.isDirectory()) {
                    logger.warn("404 Not Found: {}", uriPath);
                    exchange.sendResponseHeaders(404, -1);
                    return;
                }
                
                String contentType = Files.probeContentType(file.toPath());
                if (contentType == null) {
                    if (uriPath.endsWith(".m3u8")) {
                        contentType = "application/vnd.apple.mpegurl";
                    } else if (uriPath.endsWith(".ts")) {
                        contentType = "video/mp2t";
                    } else {
                        contentType = "application/octet-stream";
                    }
                }
                
                exchange.getResponseHeaders().set("Access-Control-Allow-Origin", "*");
                exchange.getResponseHeaders().set("Content-Type", contentType);
                exchange.sendResponseHeaders(200, file.length());
                
                try (OutputStream os = exchange.getResponseBody();
                     FileInputStream fis = new FileInputStream(file)) {
                    byte[] buffer = new byte[8192];
                    int bytesRead;
                    while ((bytesRead = fis.read(buffer)) != -1) {
                        os.write(buffer, 0, bytesRead);
                    }
                }
                
                logger.debug("200 OK: Served {} bytes for {}", file.length(), uriPath);
            }
        });
        
        httpServer.setExecutor(Executors.newFixedThreadPool(4));
        httpServer.start();
        logger.info("HTTP HLS server running on http://localhost:{}/hls/", HTTP_PORT);
    }
    
    private static void startSocketServer() {
        try (ServerSocket serverSocket = new ServerSocket(PORT)) {
            logger.info("Command server started on port {}", PORT);
            
            while (true) {
                Socket clientSocket = serverSocket.accept();
                logger.info("New client connected: {}", clientSocket.getInetAddress());
                threadPool.execute(new ClientHandler(clientSocket));
            }
        } catch (IOException e) {
            logger.error("Socket server error", e);
        }
    }
}