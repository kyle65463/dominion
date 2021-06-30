package dominion.models.events.game;

import dominion.game.GameManager;
import dominion.models.game.Player;

public class DoneSelectingHandCardEvent extends GameEvent {
    // Constructor
    public DoneSelectingHandCardEvent(int playerId, int cardId) {
        super(playerId);
        this.cardId = cardId;
    }

    // Variables
    private int cardId;

    // Functions
    @Override
    public void perform() {
        if(GameManager.getCurrentPhase() == GameManager.Phase.SelectingHandCards) {
            Player player = GameManager.getPlayerById(playerId);
            player.doneHandCardsSelection(cardId);
            GameManager.returnLastPhase();
        }
    }
}
