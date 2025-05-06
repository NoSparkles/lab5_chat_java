package client;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;

public class PrimaryController {

    private static final String SERVER_ADDRESS = "localhost";
    private static final int PORT = 12345;
    private Socket socket;
    private PrintWriter out;
    private BufferedReader in;
    private String username;
    private String roomName;
    private Thread receiveThread;

    @FXML
    private TextField usernameField;

    @FXML
    private TextField roomNameField;

    @FXML
    private TextField recipientField; // ✅ New field for DM

    @FXML
    private TextField messageField;

    @FXML
    private Button sendButton;

    @FXML
    private Button connectButton; // ✅ New button to trigger connection

    @FXML
    private HBox disabledBox; // ✅ New HBox to disable buttons when not connected

    @FXML
    private ScrollPane chatScrollPane;

    @FXML
    private VBox chatBox;

    @FXML
    private void initialize() {
        chatScrollPane.setFitToWidth(true);

        // 🔹 Detect when the window is closed and shut down correctly
        Runtime.getRuntime().addShutdownHook(new Thread(() -> onClose()));
    }

    @FXML
    private void connectToServer() {
        username = usernameField.getText().trim();

        if (username.isEmpty()) {
            addMessageToChat("⚠️ Please enter a username before connecting!");
            return;
        }

        if (socket != null && !socket.isClosed()) {
            addMessageToChat("⚠️ Already connected!");
            return;
        }

        try {
            socket = new Socket(SERVER_ADDRESS, PORT);
            in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            out = new PrintWriter(socket.getOutputStream(), true);

            out.println("USERNAME#" + username); // ✅ Send username to the server
            addMessageToChat("✅ Connected as " + username);

            startReceivingMessages();

        } catch (IOException e) {
            addMessageToChat("⚠️ Failed to connect to server: " + e.getMessage());
        }
        disabledBox.setDisable(false); // ✅ Enable buttons after connection
    }

    @FXML
    private void joinRoom() {
        roomName = roomNameField.getText().trim();
    
        if (!roomName.isEmpty() && socket != null && !socket.isClosed()) {
            out.println("JOIN#" + roomName); // ✅ Correct format
    
            addMessageToChat("✅ Joined room: " + roomName);
    
            // ✅ Ensure user exits DM mode
            recipientField.setText(""); // ✅ Clear DM recipient
        } else {
            addMessageToChat("⚠️ Please connect first and enter a room name!");
        }
    }

    @FXML
    private void joinDM() {
        String recipient = recipientField.getText().trim();
    
        if (!recipient.isEmpty() && socket != null && !socket.isClosed()) {
            out.println("JOIN_DM#" + recipient); // ✅ Correct format
    
            // ✅ Clear room name field to indicate that the user is leaving the room
            roomNameField.clear();
            addMessageToChat("🔹 Private chat started with " + recipient);
        } else {
            addMessageToChat("⚠️ Please connect first and enter a recipient username!");
        }
    }

    private void startReceivingMessages() {
        receiveThread = new Thread(() -> {
            try {
                String serverMessage;
                while (!socket.isClosed() && (serverMessage = in.readLine()) != null) {
                    addMessageToChat(serverMessage);
                }
            } catch (IOException e) {
                System.out.println("⚠️ Error reading server message: " + e.getMessage());
            }
        });

        receiveThread.setDaemon(true);
        receiveThread.start();
    }

    @FXML
    private void onClose() {
        System.out.println("❌ Disconnecting from server...");

        if (receiveThread != null && receiveThread.isAlive()) {
            receiveThread.interrupt();
        }

        try {
            if (socket != null && !socket.isClosed()) {
                socket.close();
            }
            if (in != null) in.close();
            if (out != null) out.close();
        } catch (IOException e) {
            System.out.println("⚠️ Error closing connection: " + e.getMessage());
        }
    }

    @FXML
    private void sendMessage() {
        String message = messageField.getText().trim();
        String recipient = recipientField.getText().trim();
    
        if (!message.isEmpty() && username != null && !username.isEmpty()) {
            if (!recipient.isEmpty()) {
                out.println("DM#" + recipient + "#" + message); // ✅ Correct DM format
                addMessageToChat("📩 (To " + recipient + "): " + message);
            } else {
                out.println("MSG#" + message); // ✅ Send regular message
            }
            out.flush();
            messageField.clear();
        } else {
            addMessageToChat("⚠️ Please enter a message and/or recipient!");
        }
    }

    private void addMessageToChat(String message) {
        Platform.runLater(() -> {
            Label newMessage = new Label(message);
            newMessage.setWrapText(true);
            chatBox.getChildren().add(newMessage);
            chatScrollPane.setVvalue(1.0);
        });
    }
}