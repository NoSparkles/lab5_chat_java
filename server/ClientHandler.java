import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.Map;

public class ClientHandler implements Runnable {
    private Socket socket;
    private PrintWriter out;
    private BufferedReader in;
    private Map<String, Room> rooms;
    private Room currentRoom;
    private String username;

    public ClientHandler(Socket socket, Map<String, Room> rooms) {
        this.socket = socket;
        this.rooms = rooms;
    }

    @Override
    public void run() {
        try {
            try {
                in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                out = new PrintWriter(socket.getOutputStream(), true);
            } catch (IOException e) {
                System.out.println("⚠️ Error creating streams: " + e.getMessage());
                return; // Exit if stream initialization fails
            }

            String clientData;
            try {
                clientData = in.readLine();
            } catch (IOException e) {
                System.out.println("⚠️ Error reading client data: " + e.getMessage());
                return;
            }

            if (clientData != null && clientData.startsWith("JOIN#")) {
                String[] userData = clientData.split("#");

                if (userData.length == 3) {
                    username = userData[1].trim();
                    String roomName = userData[2].trim();

                    currentRoom = rooms.computeIfAbsent(roomName, Room::new);
                    currentRoom.addClient(this);

                    currentRoom.broadcast("🔹 " + username + " joined the room!");
                    System.out.println(username + " joined the room: " + roomName);
                } else {
                    out.println("⚠️ Invalid format! Please try again.");
                    return;
                }
            }

            try {
                String message;
                while ((message = in.readLine()) != null) {
                    currentRoom.broadcast(message);
                }
            } catch (IOException e) {
                System.out.println("⚠️ Error reading message: " + e.getMessage());
            }
        } finally {
            if (currentRoom != null && username != null) {
                currentRoom.removeClient(this);
                currentRoom.broadcast("❌ " + username + " left the room!");
                System.out.println(username + " left the room.");

                // ✅ Remove room from HashMap if it's empty
                if (currentRoom.isEmpty()) {
                    rooms.remove(currentRoom.getRoomName());
                    System.out.println("🗑 Room \"" + currentRoom.getRoomName() + "\" deleted because it's empty.");
                }
            }

            try {
                if (socket != null && !socket.isClosed()) {
                    socket.shutdownInput();
                    socket.close();
                }
                if (in != null) in.close();
                if (out != null) out.close();
            } catch (IOException e) {
                System.out.println("⚠️ Error closing connection: " + e.getMessage());
            }
        }
    }

    public void sendMessage(String message) {
        out.println(message);
    }
}