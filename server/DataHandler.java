import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;

public class DataHandler {

    private static final String ROOM_FILE = "room.txt";
    private static final String DM_FILE = "dm.txt";

    // ✅ Append messages to room.txt (synchronized)
    public static synchronized void appendToRooms(String roomName, String username, String message) {
        String logEntry = "(" + roomName + " " + username + " ~" + message + "~)";
        appendToFile(ROOM_FILE, logEntry);
    }

    // ✅ Append messages to dm.txt (synchronized)
    public static synchronized void appendToDMs(String sender, String recipient, String message) {
        String logEntry = "(" + sender + " " + recipient + " ~" + message + "~)";
        appendToFile(DM_FILE, logEntry);
    }

    // ✅ Retrieve messages only from the specified room (synchronized)
    public static synchronized List<String> getRoomMessages(String roomName) {
        List<String> messages = new ArrayList<>();
        for (String line : readFromFile(ROOM_FILE)) {
            if (line.startsWith("(" + roomName + " ")) { // ✅ Filter by room name
                // ✅ Extract username and message content
                String formattedMessage = line.replaceFirst("\\(" + roomName + " ", "")
                                              .replaceFirst(" ~", ": ")
                                              .replace("~)", "");
                messages.add(formattedMessage);
            }
        }
        return messages;
    }

    public static synchronized List<String> getDMMessages(String user1, String user2) {
        List<String> messages = new ArrayList<>();
        for (String line : readFromFile(DM_FILE)) {
            if (line.startsWith("(" + user1 + " " + user2 + " ~") || line.startsWith("(" + user2 + " " + user1 + " ~")) {
                // ✅ Extract sender (first username) and message content
                String formattedMessage = line.replaceFirst("\\(", "") // Remove opening parenthesis
                                              .replaceFirst(" " + user2 + " ", ": ") // Remove recipient's name
                                              .replaceFirst(" " + user1 + " ", ": ") // Handle flipped order
                                              .replaceFirst(" ~", " ") // Replace the message delimiter
                                              .replace("~)", ""); // Remove ending symbols
                messages.add(formattedMessage);
            }
        }
        return messages;
    }

    // Helper function to append messages to a file (synchronized)
    private static synchronized void appendToFile(String fileName, String message) {
        try (FileWriter writer = new FileWriter(fileName, true);
             BufferedWriter bw = new BufferedWriter(writer);
             PrintWriter pw = new PrintWriter(bw)) {
            pw.println(message);
        } catch (IOException e) {
            System.out.println("⚠️ Error writing to " + fileName + ": " + e.getMessage());
        }
    }

    // Helper function to read messages from a file (synchronized)
    private static synchronized List<String> readFromFile(String fileName) {
        List<String> messages = new ArrayList<>();
        try (BufferedReader br = new BufferedReader(new FileReader(fileName))) {
            String line;
            while ((line = br.readLine()) != null) {
                messages.add(line);
            }
        } catch (IOException e) {
            System.out.println("⚠️ Error reading from " + fileName + ": " + e.getMessage());
        }
        return messages;
    }
}
