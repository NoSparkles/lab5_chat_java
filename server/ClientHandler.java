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

    public ClientHandler(Socket socket, Map<String, Room> rooms) {
        this.socket = socket;
        this.rooms = rooms;
    }

    @Override
    public void run() {
        try {
            in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            out = new PrintWriter(socket.getOutputStream(), true);

            out.println("Įveskite kambario pavadinimą:");
            String roomName = in.readLine();
            currentRoom = rooms.computeIfAbsent(roomName, Room::new);
            currentRoom.addClient(this);

            out.println("Prisijungėte prie kambario: " + roomName);

            String message;
            while ((message = in.readLine()) != null) {
                currentRoom.broadcast(message);
            }
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            if (currentRoom != null) {
                currentRoom.removeClient(this);
            }
        }
    }

    public void sendMessage(String message) {
        out.println(message);
    }
}