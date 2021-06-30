package dominion.models.cards.actions;

import dominion.core.GameManager;
import dominion.models.events.game.DoneAttackingEvent;
import dominion.models.player.Player;
import dominion.models.cards.Card;
import dominion.models.cards.AttackPlayers;
import dominion.models.cards.CardStyles;
import dominion.models.cards.CardTypes;

import java.util.List;

public class Militia extends Card implements Action, Attack, HasSelection {
    public Militia() {
        name = "義勇軍";
        description = "";
        style = CardStyles.white;
        type = CardTypes.action;
        numCost = 4;
    }

    @Override
    public void perform(Player performer, boolean decreaseNumActions) {
        Thread thread = new Thread(new AttackPlayers(performer, this, decreaseNumActions));
        thread.start();
    }

    @Override
    public void performAttack(Player performer, Player attacked) {
        if (attacked.getHandCards().size() > 3) {
            GameManager.setCurrentPhase(GameManager.Phase.SelectingHandCards);
            attacked.startSelectingHandCards("丟棄至三張牌", id);
            attacked.setExactSelectingHandCards(attacked.getHandCards().size() - 3);
        } else {
            GameManager.sendEvent(new DoneAttackingEvent(performer.getId()));
        }
    }

    @Override
    public void performSelection(Player performer, List<Card> cards) {
        performer.discardHandCards(cards);
        GameManager.sendEvent(new DoneAttackingEvent(performer.getId()));
    }

    @Override
    public void performAfterAttack(Player performer) {
        performer.increaseNumCoins(2);
    }
}