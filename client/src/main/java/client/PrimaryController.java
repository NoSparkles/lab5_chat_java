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
    private TextField messageField;

    @FXML
    private Button sendButton;

    @FXML
    private ScrollPane chatScrollPane;

    @FXML
    private VBox chatBox;

    @FXML
    private void initialize() {
        connectToServer();
        chatScrollPane.setFitToWidth(true);

        // 🔹 Detect when the window is closed and shut down correctly
        Runtime.getRuntime().addShutdownHook(new Thread(() -> onClose()));
    }

    @FXML
    private void joinRoom() {
        String newUsername = usernameField.getText();
        String newRoomName = roomNameField.getText();

        if (!newUsername.isEmpty() && !newRoomName.isEmpty()) {
            // ✅ If already connected, close the current connection before reconnecting
            if (socket != null && !socket.isClosed()) {
                onClose(); // Close existing connection
            }

            // ✅ Create a new socket and reconnect
            try {
                socket = new Socket(SERVER_ADDRESS, PORT);
                in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                out = new PrintWriter(socket.getOutputStream(), true);

                username = newUsername;
                roomName = newRoomName;

                // ✅ Send new join request
                out.println("JOIN#" + username + "#" + roomName);

                // ✅ Start listening to messages again
                startReceivingMessages();

            } catch (IOException e) {
                addMessageToChat("⚠️ Klaida jungiantis prie serverio: " + e.getMessage());
            }
        } else {
            addMessageToChat("⚠️ Vartotojo vardas ir kambario pavadinimas negali būti tušti!");
        }
    }

    private void connectToServer() {
        try {
            socket = new Socket(SERVER_ADDRESS, PORT);
            in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            out = new PrintWriter(socket.getOutputStream(), true);
    
            startReceivingMessages(); // ✅ Start listening for incoming messages
    
        } catch (IOException e) {
            addMessageToChat("⚠️ Nepavyko prisijungti prie serverio: " + e.getMessage());
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
                System.out.println("⚠️ Klaida skaitant serverio žinutę: " + e.getMessage());
            }
        });

        receiveThread.setDaemon(true); // ✅ Ensure thread stops when app closes
        receiveThread.start();
    }

    @FXML
    private void onClose() {
        System.out.println("❌ Atsijungia nuo dabartinio kambario...");

        // ✅ Stop receiving messages
        if (receiveThread != null && receiveThread.isAlive()) {
            receiveThread.interrupt();
        }

        // ✅ Safely close connections
        try {
            if (socket != null && !socket.isClosed()) {
                socket.close();
            }
            if (in != null) in.close();
            if (out != null) out.close();
        } catch (IOException e) {
            System.out.println("⚠️ Klaida uždarant ryšį: " + e.getMessage());
        }
    }

    @FXML
    private void sendMessage() {
        String message = messageField.getText();

        if (!message.isEmpty() && username != null && !username.isEmpty()) {
            out.println(username + ": " + message);
            out.flush();
            messageField.clear();
        } else {
            addMessageToChat("⚠️ Prisijunkite ir/arba įveskite žinutę!");
        }
    }

    private void addMessageToChat(String message) {
        Platform.runLater(() -> {
            Label newMessage = new Label(message);
            newMessage.setWrapText(true);
            chatBox.getChildren().add(newMessage);
            chatScrollPane.setVvalue(1.0); // ✅ Automatiškai nuskrollina į apačią
        });
    }
}