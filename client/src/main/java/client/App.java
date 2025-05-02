package client;

import java.io.IOException;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.stage.Stage;

/**
 * JavaFX App
 */
public class App extends Application {

    private static Scene scene;

    @Override
    public void start(Stage stage) throws IOException {
         FXMLLoader loader = new FXMLLoader(getClass().getResource("primary.fxml"));
        App.scene = new Scene(loader.load(), 1440, 900);
        stage.setScene(App.scene);
        stage.show();
    }

    public static void main(String[] args) {
        launch();
    }

}