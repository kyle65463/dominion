package dominion.controller;

import dominion.connection.Server;
import javafx.fxml.FXML;

public class CreateRoomController extends ConnectToRoomController {
    @FXML
    public void initialize() {
        connection = new Server();
        nameField.setText("host");
    }
}
