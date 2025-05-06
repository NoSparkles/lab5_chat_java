import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.HashMap;
import java.util.Map;

public class Server {
    private static final int PORT = 12345;
    private static Map<String, Room> rooms = new HashMap<>();
    private static Map<String, ClientHandler> clients = new HashMap<>(); // ✅ Store individual clients for DMs

    public static void main(String[] args) {
        try (ServerSocket serverSocket = new ServerSocket(PORT)) {
            System.out.println("Server is running...");

            while (true) {
                Socket socket = serverSocket.accept();
                System.out.println("New user connected.");
            
                new Thread(new ClientHandler(socket, rooms, clients)).start();
            }
            
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static Room getOrCreateRoom(String roomName) {
        return rooms.computeIfAbsent(roomName, Room::new);
    }
}