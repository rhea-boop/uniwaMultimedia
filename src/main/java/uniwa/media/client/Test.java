package uniwa.media.client;

import java.io.IOException;
import java.net.Socket;

public class Test {
    public static void main(String[] args) {
       try (Socket socket = new Socket("localhost", 8080)) {
            System.out.println("Connected to server: " + socket.getInetAddress() + ":" + socket.getPort());
        } catch (IOException e) {
            System.err.println("Error connecting to server: " + e.getMessage());
        }
    }
}
