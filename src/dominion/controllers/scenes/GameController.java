package dominion.controllers.scenes;

import dominion.connections.Connection;
import dominion.controllers.components.ShoutController;
import dominion.controllers.components.TauntController;
import dominion.core.Game;
import dominion.core.GameManager;
import dominion.models.areas.LogBox;
import dominion.models.User;
import dominion.models.areas.GameScene;
import dominion.models.areas.MajorPurchaseArea;
import dominion.models.areas.MinorPurchaseArea;
import dominion.models.areas.WinnerDialog;
import dominion.models.cards.Card;
import dominion.models.cards.actions.*;
import dominion.models.cards.curses.Curse;
import dominion.models.cards.treasures.Copper;
import dominion.models.cards.treasures.Gold;
import dominion.models.cards.treasures.Silver;
import dominion.models.cards.victories.Duchy;
import dominion.models.cards.victories.Estate;
import dominion.models.cards.victories.Province;
import dominion.models.player.DisplayedCard;
import dominion.models.player.FieldCards;
import dominion.models.player.Player;
import dominion.models.player.PlayerStatus;
import dominion.params.GameSceneParams;
import dominion.params.SceneParams;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.Pane;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

import java.util.ArrayList;
import java.util.List;

public class GameController extends SceneController {
    @FXML
    AnchorPane rootNode;
    @FXML
    GridPane majorKingdomCardsBoxNode;
    @FXML
    GridPane minorKingdomCardsBoxNode;
    @FXML
    VBox messageBoxNode;
    @FXML
    ScrollPane scrollPane;
    @FXML
    GridPane opponentsStatusBox;
    @FXML
    Pane playerStatusBox;
    @FXML
    Pane winnerBox;
    @FXML
    Label winnerLabel;

    // Functions
    public void initialize(Stage stage, SceneParams sceneParams) {
        // Unpack parameters
        GameSceneParams params = (GameSceneParams) sceneParams;
        List<User> users = params.users;
        User applicationUser =  params.applicationUser;
        Connection connection = params.connection;
        int randomSeed = params.randomSeed;

        // Set up random seed
        GameManager.setRandomSeed(randomSeed);

        // Set up UIs
        GameScene.initialize(rootNode);
        WinnerDialog.initialize(winnerBox, winnerLabel);
        LogBox.initialize(scrollPane, messageBoxNode);

        TauntController con = new TauntController();
        GameScene.add(con);
        ShoutController sho = new ShoutController();
        GameScene.add(sho);

        // Set up players
        Player applicationPlayer = new Player(applicationUser);
        List<Player> players = new ArrayList<>();
        FieldCards fieldCards = new FieldCards();
        fieldCards.enableUi();
        int index = 0;
        for (User user : users) {
            Player player = new Player(user);
            PlayerStatus playerStatus = player.getPlayerStatus();
            if(user.getId() == applicationUser.getId()) {
                applicationPlayer = player;
                player.enableUi();
                playerStatusBox.getChildren().add(playerStatus.getController().getRootNode());
            }
            else {
                opponentsStatusBox.add(playerStatus.getController().getRootNode(), index, 0);
                index++;
            }

            List<Card> initialCards = new ArrayList<>();
            for (int i = 0; i < 7; i++) {
                initialCards.add(new Copper());
//                initialCards.add(new Militia());

            }
            for (int i = 0; i < 3; i++) {
                initialCards.add(new Estate());
//                initialCards.add(new Moat());
            }
            player.setDeckCards(initialCards);
            player.setFieldCards(fieldCards);
            players.add(player);
        }

        // Set up purchase areas
        MajorPurchaseArea majorPurchaseArea = new MajorPurchaseArea(majorKingdomCardsBoxNode);
        MinorPurchaseArea minorPurchaseArea = new MinorPurchaseArea(minorKingdomCardsBoxNode);
        List<DisplayedCard> majorKingdomCards = new ArrayList<>();
        List<DisplayedCard> minorKingdomCards = new ArrayList<>();

        // Top 5
        majorKingdomCards.add(new DisplayedCard(new Smithy(), 10, applicationPlayer, 7));
        majorKingdomCards.add(new DisplayedCard(new CouncilRoom(), 10, applicationPlayer, 8));
        majorKingdomCards.add(new DisplayedCard(new Laboratory(), 10, applicationPlayer, 9));
        majorKingdomCards.add(new DisplayedCard(new Market(), 10, applicationPlayer, 10));
        majorKingdomCards.add(new DisplayedCard(new Festival(), 10, applicationPlayer, 11));

        // Bottom 5
        majorKingdomCards.add(new DisplayedCard(new Cellar(), 10, applicationPlayer, 12));
        majorKingdomCards.add(new DisplayedCard(new Chapel(), 10, applicationPlayer, 13));
        majorKingdomCards.add(new DisplayedCard(new Moat(), 10, applicationPlayer, 14));
        majorKingdomCards.add(new DisplayedCard(new MoneyLender(), 10, applicationPlayer, 15));
        majorKingdomCards.add(new DisplayedCard(new Militia(), 4 * users.size(), applicationPlayer, 16));
        majorPurchaseArea.setDisplayedCards(majorKingdomCards);

        // Scores
        minorKingdomCards.add(new DisplayedCard(new Province(), 4 * users.size(), applicationPlayer, 0));
        minorKingdomCards.add(new DisplayedCard(new Gold(), 30, applicationPlayer, 4));
        minorKingdomCards.add(new DisplayedCard(new Duchy(), 4 * users.size(), applicationPlayer, 1));
        minorKingdomCards.add(new DisplayedCard(new Silver(), 40, applicationPlayer, 5));
        minorKingdomCards.add(new DisplayedCard(new Estate(), 4 * users.size(), applicationPlayer, 2));
        minorKingdomCards.add(new DisplayedCard(new Copper(), 60 - 7 * users.size(), applicationPlayer, 6));
        minorKingdomCards.add(new DisplayedCard(new Curse(), 10 * (users.size() - 1), applicationPlayer, 3));
        minorPurchaseArea.setDisplayedCards(minorKingdomCards);

        // Set up game manager
        GameManager.initialize(players, applicationPlayer, connection, majorPurchaseArea, minorPurchaseArea);

        // Run the game
        Game game = new Game();
        Thread gameThread = new Thread(game);
        for(Player player : players){
            player.drawCards(5);
            player.setActionBarStatus("等待其他玩家的回合", "");
            player.reset();
        }
        gameThread.start();
    }
}
