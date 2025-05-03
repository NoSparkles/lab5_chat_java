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

    private void connectToServer() {
        try {
            socket = new Socket(SERVER_ADDRESS, PORT);
            in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            out = new PrintWriter(socket.getOutputStream(), true);

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

            receiveThread.setDaemon(true); // ✅ Allow this thread to stop when the application closes
            receiveThread.start();

        } catch (IOException e) {
            addMessageToChat("Nepavyko prisijungti prie serverio...");
        }
    }

    @FXML
    private void onClose() {
        System.out.println("❌ Uždaro klientą...");

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

        Platform.exit(); // ✅ Close JavaFX
        System.exit(0);  // ✅ Fully terminate program
    }

    @FXML
    private void joinRoom() {
        username = usernameField.getText();
        roomName = roomNameField.getText();

        if (!username.isEmpty() && !roomName.isEmpty()) {
            out.println("JOIN#" + username + "#" + roomName);
        } else {
            addMessageToChat("Vartotojo vardas ir kambario pavadinimas negali būti tušti!");
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
            addMessageToChat("Pirmiausia prisijunkite ir įveskite žinutę!");
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