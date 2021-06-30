package dominion.models.cards.actions;

import dominion.models.player.Player;
import dominion.models.cards.Card;
import dominion.models.cards.CardStyles;
import dominion.models.cards.CardTypes;

public class Moat extends Card implements Action, Reaction{
    public Moat() {
        name = "護城河";
        description = "";
        style = CardStyles.blue;
        type = CardTypes.action;
        numCost = 2;
    }

    @Override
    public void perform(Player performer, boolean decreaseNumActions) {
        performer.drawCards(2);
        if(decreaseNumActions) {
            performer.decreaseNumActions();
        }
        performer.checkActionCardsAndEndPlayingActionPhase();
    }

    @Override
    public void performReaction(Player performer) {
        performer.setImmuneNextAttack(true);
    }
}
