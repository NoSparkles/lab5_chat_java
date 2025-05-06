import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public class Room {
    private String name;
    private Set<ClientHandler> clients = ConcurrentHashMap.newKeySet();

    public Room(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }

    public boolean isEmpty() {
        return clients.isEmpty();
    }
    
    public String getRoomName() {
        return this.name;
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