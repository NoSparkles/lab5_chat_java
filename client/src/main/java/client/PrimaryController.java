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
    }

    private void connectToServer() {
        try {
            socket = new Socket(SERVER_ADDRESS, PORT);
            in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            out = new PrintWriter(socket.getOutputStream(), true);

            new Thread(() -> {
                try {
                    String serverMessage;
                    while ((serverMessage = in.readLine()) != null) {
                        addMessageToChat(serverMessage);
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }).start();
        } catch (IOException e) {
            addMessageToChat("Nepavyko prisijungti prie serverio...");
        }
    }

    @FXML
    private void joinRoom() {
        username = usernameField.getText();
        String roomName = roomNameField.getText();

        if (!username.isEmpty() && !roomName.isEmpty()) {
            out.println(username + " prisijungė prie kambario: " + roomName);
            addMessageToChat("Prisijungta kaip " + username + " prie kambario: " + roomName);
        } else {
            addMessageToChat("Vartotojo vardas ir kambario pavadinimas negali būti tušti!");
        }
    }

    @FXML
    private void sendMessage() {
        String message = messageField.getText();

        if (!message.isEmpty() && username != null && !username.isEmpty()) {
            out.println(username + ": " + message);
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
            chatScrollPane.setVvalue(1.0); // Automatiškai nuskrollina į apačią
        });
    }

}