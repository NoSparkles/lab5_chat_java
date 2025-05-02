import java.util.HashSet;
import java.util.Set;

public class Room {
    private String name;
    private Set<ClientHandler> clients = new HashSet<>();

    public Room(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }

    public void addClient(ClientHandler client) {
        clients.add(client);
    }

    public void removeClient(ClientHandler client) {
        clients.remove(client);
    }

    public void broadcast(String message) {
        for (ClientHandler client : clients) {
            client.sendMessage(message);
        }
    }
}