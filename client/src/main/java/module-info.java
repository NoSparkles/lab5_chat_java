module client {
    requires javafx.controls;
    requires javafx.fxml;
    requires javafx.graphics;

    opens client to javafx.fxml;
    exports client;
}
