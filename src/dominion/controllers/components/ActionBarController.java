package dominion.controllers.components;

import dominion.models.game.GameScene;
import javafx.event.EventHandler;
import javafx.fxml.FXMLLoader;
import javafx.scene.control.Button;
import javafx.scene.control.Label;

public class ActionBarController extends ComponentController{
    // Constructor
    public ActionBarController(GameScene gameScene) {
        super(gameScene);
        initialize();
    }

    // Variables
    private Button button;
    private Label statusLabel;
    private Label purchasesLabel;
    private Label actionsLabel;
    private Label coinsLabel;
    private final double x = 235;
    private final double y = 520;

    // Functions
    public void setNumActions(int numActions) {
        actionsLabel.setText(String.valueOf(numActions) + " 行動");
    }

    public void setNumPurchases(int numPurchases) {
        purchasesLabel.setText(String.valueOf(numPurchases) + " 購買");
    }

    public void setNumCoins(int numCoins) {
        coinsLabel.setText(String.valueOf(numCoins));
    }

    public void setButtonText(String buttonText) {
        button.setText(buttonText);
        if(buttonText.isEmpty()){
            button.setVisible(false);
        }
        else{
            button.setVisible(true);
        }
    }

    public void setStatus(String status) {
        statusLabel.setText(status);
    }

    public void setButtonOnPressed(EventHandler handler) {
        button.setOnMousePressed(handler);
    }

    private void initialize() {
        try {
            rootNode = FXMLLoader.load(ActionBarController.class.getClassLoader().getResource("resources/components/action_bar.fxml"));
            actionsLabel = (Label) rootNode.lookup("#num_actions");
            purchasesLabel = (Label) rootNode.lookup("#num_purchases");
            coinsLabel = (Label) rootNode.lookup("#num_coins");
            statusLabel = (Label) rootNode.lookup("#status");
            button = (Button) rootNode.lookup("#button");

            setNumActions(1);
            setNumPurchases(1);
            setNumCoins(3);
            setStatus("你可以購買卡片");
            setButtonText("結束購買");

            rootNode.setLayoutX(x);
            rootNode.setLayoutY(y);
            gameScene.add(this);
        } catch (Exception e) {
            System.out.println(e);
        }
    }

    public String getStatus() {
        return statusLabel.getText();
    }

    public String getButtonText() {
        return button.getText();
    }

    public EventHandler getButtonOnPressed() {
        return button.getOnMousePressed();
    }
}