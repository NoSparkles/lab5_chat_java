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
                System.out.println("⚠️ Klaida kuriant srautus: " + e.getMessage());
                return; // Exit if stream initialization fails
            }
    
            String clientData;
            try {
                clientData = in.readLine();
            } catch (IOException e) {
                System.out.println("⚠️ Klaida skaitant prisijungimo duomenis: " + e.getMessage());
                return;
            }
    
            if (clientData != null && clientData.startsWith("JOIN#")) {
                String[] userData = clientData.split("#");
    
                if (userData.length == 3) {
                    username = userData[1].trim();
                    String roomName = userData[2].trim();
    
                    currentRoom = rooms.computeIfAbsent(roomName, Room::new);
                    currentRoom.addClient(this);
    
                    currentRoom.broadcast("🔹 " + username + " prisijungė prie kambario!");
                    out.println("Prisijungėte kaip " + username + " prie kambario: " + roomName);
                    System.out.println(username + " prisijungė prie kambario: " + roomName);
                } else {
                    out.println("Neteisingas formatas! Bandykite dar kartą.");
                    return;
                }
            }
    
            try {
                String message;
                while ((message = in.readLine()) != null) {
                    currentRoom.broadcast(message);
                }
            } catch (IOException e) {
                System.out.println("⚠️ Klaida skaitant žinutę: " + e.getMessage());
            }
        } finally {
            if (currentRoom != null && username != null) {
                currentRoom.removeClient(this);
                currentRoom.broadcast("❌ " + username + " paliko kambarį!");
                System.out.println(username + " paliko kambarį.");
            }
    
            try {
                if (socket != null && !socket.isClosed()) {
                    socket.shutdownInput();
                    socket.close();
                }
                if (in != null) in.close();
                if (out != null) out.close();
            } catch (IOException e) {
                System.out.println("⚠️ Klaida uždarant ryšį: " + e.getMessage());
            }
        }
    }

    public void sendMessage(String message) {
        out.println(message);
    }
}