import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.List;
import java.util.Map;

public class ClientHandler implements Runnable {
    private Socket socket;
    private PrintWriter out;
    private BufferedReader in;
    private Map<String, Room> rooms;
    private Map<String, ClientHandler> clients;
    private Room currentRoom;
    private ClientHandler directMessageRecipient; // ✅ Track DM recipient
    private String username;

    public ClientHandler(Socket socket, Map<String, Room> rooms, Map<String, ClientHandler> clients) {
        this.socket = socket;
        this.rooms = rooms;
        this.clients = clients;
    }

    @Override
    public void run() {
        try {
            initializeStreams();
            requestUsername();
            listenForMessages();
        } finally {
            cleanUp();
        }
    }

    private void initializeStreams() {
        try {
            in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            out = new PrintWriter(socket.getOutputStream(), true);
        } catch (IOException e) {
            System.out.println("⚠️ Error creating streams: " + e.getMessage());
        }
    }

    private void requestUsername() {
        try {
            //out.println("Enter your username:");
            username = in.readLine();
    
            if (username == null || username.trim().isEmpty()) {
                out.println("⚠️ Invalid username. Please restart and enter a valid name.");
                socket.close();
                return;
            }
    
            username = username.trim();
    
            // ✅ Ensure usernames are correctly formatted
            if (username.startsWith("USERNAME#")) {
                username = username.replace("USERNAME#", "").trim();
            }
    
    
            if (clients.containsKey(username)) {
                out.println("Username already in use. Try a different one.");
                socket.close();
                return;
            }
    
            clients.put(username, this);
    
            out.println("Username locked in: " + username);
            System.out.println("🔹 User connected: " + username);
        } catch (IOException e) {
            System.out.println("Error reading username: " + e.getMessage());
        }
    }

    private void listenForMessages() {
        try {
            String message;
            while ((message = in.readLine()) != null) {
                message = message.trim();
    
                if (message.startsWith("JOIN#") && message.contains("#")) {
                    handleJoinRoom(message);
                } else if (message.startsWith("JOIN_DM#") && message.contains("#")) {
                    handleJoinDM(message);
                } else if (message.startsWith("DM#") && message.contains("#")) {
                    handleDirectMessage(message);
                } else if (message.startsWith("MSG#")) {
                    handleRoomMessage(message);
                } else {
                    sendMessage("⚠️ Unknown command or incorrect format. Use JOIN#room, JOIN_DM#user, or DM#user#message.");
                }
            }
        } catch (IOException e) {
            System.out.println("⚠️ Error reading message: " + e.getMessage());
        }
    }

    private void handleJoinRoom(String message) {
        String[] parts = message.split("#");
    
        if (parts.length != 2) {
            sendMessage("⚠️ Invalid JOIN format! Use JOIN#roomName");
            return;
        }
    
        String roomName = parts[1].trim();
    
        // ✅ Exit DM mode completely
        if (directMessageRecipient != null) {
            sendMessage("🔹 Left private chat with " + directMessageRecipient.username);
            directMessageRecipient.sendMessage("🔹 " + username + " left the private chat.");
            directMessageRecipient.directMessageRecipient = null; // ✅ Remove DM link on both sides
            directMessageRecipient = null;
        }
    
        if (currentRoom != null) {
            currentRoom.removeClient(this);
        }
    
        currentRoom = rooms.computeIfAbsent(roomName, Room::new);
        currentRoom.addClient(this);
    
        currentRoom.broadcast(username + " joined the room.");

        // ✅ Retrieve stored messages for the room
        List<String> storedMessages = DataHandler.getRoomMessages(roomName);
        for (String storedMessage : storedMessages) {
            sendMessage(storedMessage);
        }
    }

    private void handleJoinDM(String message) {
        String[] parts = message.split("#");

        if (parts.length != 2) {
            sendMessage("⚠️ Invalid JOIN_DM format! Use JOIN_DM#username");
            return;
        }

        String recipientName = parts[1].trim();
        ClientHandler recipientClient = clients.get(recipientName);

        sendMessage("✅ Private chat started with " + recipientName);
        directMessageRecipient = recipientClient;

        // ✅ Retrieve stored messages and show them to the sender
        List<String> storedMessages = DataHandler.getDMMessages(username, recipientName);
        for (String storedMessage : storedMessages) {
            sendMessage(storedMessage);
        }
    }

    private void handleDirectMessage(String message) {
        String[] msgParts = message.split("#");

        if (msgParts.length != 3) {
            sendMessage("⚠️ Invalid DM format! Use DM#recipient#message");
            return;
        }

        String recipientName = msgParts[1].trim();
        String msgContent = msgParts[2];

        // ✅ Always log the message, even if recipient is offline
        DataHandler.appendToDMs(username, recipientName, msgContent);

        ClientHandler recipientClient = clients.get(recipientName);

        if (recipientClient != null && recipientClient.directMessageRecipient == this && this.directMessageRecipient == recipientClient) {
            recipientClient.sendMessage(username + ": " + msgContent);
        }
    }

    private void handleRoomMessage(String message) {
        if (directMessageRecipient != null) {
            return; // ✅ Ignore room messages if in DM mode
        }
    
        String[] msgParts = message.split("#", 2);
        if (msgParts.length != 2) {
            sendMessage("⚠️ Invalid message format! Use MSG#message");
            return;
        }
    
        String msgContent = msgParts[1].trim();
    
        if (currentRoom != null) {
            DataHandler.appendToRooms(currentRoom.getName(), username, msgContent); // ✅ Store message
            currentRoom.broadcast(username + ": " + msgContent);
        } else {
            sendMessage("⚠️ You are not in a room. Join a room first.");
        }
    }

    private void cleanUp() {
        if (username != null) {
            clients.remove(username);
            System.out.println(username + " disconnected.");

            if (currentRoom != null) {
                currentRoom.removeClient(this);
                currentRoom.broadcast(username + " left the room.");

                if (currentRoom.isEmpty()) {
                    rooms.remove(currentRoom.getName());
                    System.out.println("Room \"" + currentRoom.getName() + "\" deleted because it's empty.");
                }
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

    public void sendMessage(String message) {
        out.println(message);
    }
}