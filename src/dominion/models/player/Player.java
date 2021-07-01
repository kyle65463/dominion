package dominion.models.player;

import dominion.core.GameManager;
import dominion.models.areas.DisplayedCard;
import dominion.models.areas.LogBox;
import dominion.models.User;
import dominion.models.areas.PurchaseArea;
import dominion.models.events.game.*;
import dominion.models.cards.Card;
import dominion.models.cards.actions.Action;
import dominion.models.cards.actions.HasHandCardsSelection;
import dominion.models.cards.actions.Reaction;
import dominion.models.cards.treasures.Treasure;
import dominion.models.handlers.*;
import dominion.models.player.container.Deck;
import dominion.models.player.container.DiscardPile;
import dominion.models.player.container.FieldCards;
import dominion.models.player.container.HandCards;
import javafx.event.EventHandler;
import javafx.util.Pair;

import java.util.ArrayList;
import java.util.List;

public class Player {
    // Constructor
    public Player(User user) {
        this(user.getName(), user.getId(),user);
    }

    public Player(String name, int id,User user) {
        this.name = name;
        this.id = id;
        deck = new Deck();
        discardPile = new DiscardPile();
        handCards = new HandCards(this);
        actionBar = new ActionBar();
        playerStatus = new PlayerStatus();
        playerStatus.setName(name);
        this.user = user;
    }

    // Variables
    final private String name;
    final private int id;
    private int numScores = 3;

    private int numActions = 1;
    private int numCoins = 0;
    private int numPurchases = 1;

    private boolean isEnableUi;
    private boolean immuneNextAttack = false;

    private User user;
    /* Components */
    private final Deck deck;
    private final DiscardPile discardPile;
    private FieldCards fieldCards;
    private final HandCards handCards;
    private final ActionBar actionBar;
    private final PlayerStatus playerStatus;

    private int maxSelectedCard = Integer.MAX_VALUE;
    private List<DisplayedCard> selectedDisplayedCards = new ArrayList<>();
    private AfterPlayCardHandler afterPlayCardHandler = ()->{};

    private int exactSelectingCards = 0;
    private List<Card> selectedCards = new ArrayList<>();
    private CardFilter selectingHandCardsFilter;
    private DisplayedCardFilter selectingDisplayedCardsFilter;

    // Functions
    public User getUser(){return user;}

    public void setImmuneNextAttack(boolean b) {
        immuneNextAttack = b;
    }
    public void setAfterPlayCardHandler(AfterPlayCardHandler handler) { afterPlayCardHandler = handler; }

    public PlayerStatus getPlayerStatus() {
        return playerStatus;
    }

    public boolean getImmuneNextAttack() {
        return immuneNextAttack;
    }

    public String getName() {
        return name;
    }

    public int getId() {
        return id;
    }

    public int getNumCoins() {
        return numCoins;
    }

    public int getNumPurchases() {
        return numPurchases;
    }

    public int getNumActions() { return numActions; }

    public void enableUi() {
        deck.enableUi();
        discardPile.enableUi();
        handCards.enableUi();
        actionBar.enableUi();
    }

    public void buyNewCard(Card card) {
        LogBox.logBuyCard(this, card);
        discardPile.addCard(card);
        setPlayerStatusValues();
    }

    public void receiveNewCard(Card card) {
        LogBox.logReceiveCard(this, card);
        discardPile.addCard(card);
        setPlayerStatusValues();
    }

    public void receiveNewHandCard(Card card) {
        LogBox.logReceiveCard(this, card);
        handCards.addCard(card);
        setPlayerStatusValues();
    }

    public void decreaseNumActions() {
        numActions--;
        setActionBarValues();
        if (numActions <= 0) {
            GameManager.sendEvent(new EndPlayingActionsPhaseEvent(id));
        }
    }

    public void increaseNumActions(int numIncrease) {
        numActions += numIncrease;
        setActionBarValues();
    }

    public void increaseNumPurchases(int numIncrease) {
        numPurchases += numIncrease;
        setActionBarValues();
    }

    public void decreaseNumPurchases() {
        numPurchases--;
        setActionBarValues();
        if (numPurchases <= 0) {
            GameManager.sendEvent(new EndBuyingPhaseEvent(id));
        }
    }

    public void increaseNumCoins(int numIncrease) {
        numCoins += numIncrease;
        setActionBarValues();
    }

    public void decreaseNumCoins(int numDecreases) {
        numCoins -= numDecreases;
        setActionBarValues();
    }

    public void setFieldCards(FieldCards fieldCards) {
        this.fieldCards = fieldCards;
    }

    public FieldCards getFieldCards() { return this.fieldCards; }

    public void setDeckCards(List<Card> cards) {
        deck.addCards(cards, true);
        setPlayerStatusValues();
    }

    public List<Card> getHandCards() {
        return handCards.getCards();
    }

    public void displayHandCards() {
        LogBox.logDisplayHandCards(this, handCards.getCards());
    }

    public List<Card> getAllCards() {
        List<Card> cards = new ArrayList<>();
        if(fieldCards != null){
            cards.addAll(fieldCards.getCards());
        }
        cards.addAll(handCards.getCards());
        cards.addAll(discardPile.getCards());
        cards.addAll(deck.getCards());
        return cards;
    }

    public void retrieveHandCardFromFieldCards(Card card) {
        fieldCards.removeCard(card);
        handCards.addCard(card);
    }

    public void playCard(int cardId, boolean decreaseNumActions, CardNextMoveHandler nextMoveHandler) {
        System.out.println("play " + cardId);
        for(Card c : handCards.getCards()) {
            System.out.println(c.getName() + " " + c.getId());
        }
        Card card = handCards.getCardByCardId(cardId);
        LogBox.logPlayCard(this, card);
        handCards.removeCard(card);
        fieldCards.addCard(card);
        if (card instanceof Treasure) {
            numCoins += ((Treasure) card).getNumValue();
        } else if (card instanceof Action) {
            card.setCardNextMove(nextMoveHandler == null ? ()->{
                checkActionCardsAndEndPlayingActionPhase();
            } : nextMoveHandler);
            ((Action) card).perform(this, decreaseNumActions);
        }

        setPlayerStatusValues();
        setActionBarValues();
        if (this.afterPlayCardHandler != null) {
            afterPlayCardHandler.perform();
        }
    }

    public void checkActionCardsAndEndPlayingActionPhase() {
        if (!hasActionCards() || numActions == 0) {
            GameManager.sendEvent(new EndPlayingActionsPhaseEvent(id));
        }
    }

    public boolean hasActionCards() {
        boolean result = handCards.hasActionCards();
        return result;
    }

    public boolean hasReactionCards() {
        boolean result = handCards.hasReactionCards();
        return result;
    }

    public void react(int cardId) {
        Card card = handCards.getCardByCardId(cardId);
        LogBox.logReactCard(this, card);
        Reaction reactionCard = (Reaction) card;
        reactionCard.performReaction(this);
    }

    public Pair<String, String> getActionBarStatus() {
        String originalStatus = actionBar.getStatus();
        String originalButtonText = actionBar.getRightButtonText();
        return new Pair<>(originalStatus, originalButtonText);
    }

    public void setActionBarStatus(String status, String rightButtonText, String leftButtonText) {
        actionBar.setStatus(status);
        actionBar.setRightButtonText(rightButtonText);
        actionBar.setLeftButtonText(leftButtonText);
    }

    public void setActionBarStatus(String status, String rightButtonText) {
        actionBar.setStatus(status);
        actionBar.setRightButtonText(rightButtonText);
    }

    public EventHandler getActionBarButtonOnPressed() {
        return actionBar.getRightButtonOnPressed();
    }

    public void setActionBarRightButtonHandler(EventHandler eventHandler) {
        actionBar.setRightButtonOnPressed(eventHandler);
    }

    public void setActionBarLeftButtonHandler(EventHandler eventHandler) {
        actionBar.setLeftButtonOnPressed(eventHandler);
    }

    public void setActionBarLeftButtonText(String text) {
        actionBar.setLeftButtonText(text);
    }

    public void enableLeftButton(boolean b) {
        actionBar.enableLeftButton(b);
    }

    public CardSelectedHandler getCardSelectedHandler() {
        return handCards.getCardSelectedHandler();
    }

    public void setCardSelectedHandler(CardSelectedHandler cardSelectedHandler) {
        handCards.setCardSelectedHandler(cardSelectedHandler);
    }

    public void removeCardSelectedHandler() {
        handCards.removeCardSelectedHandler();
    }

    public void discardAllHandCards() {
        // Discard all hand cards to discard pile
        List<Card> cards = handCards.getCards();
        discardPile.addCards(cards);
        handCards.removeAllCards();
        setPlayerStatusValues();
    }

    public void discardAllFieldCards() {
        // Discard all field cards to discard `pile
        List<Card> cards = fieldCards.getCards();
        fieldCards.removeCards();
        discardPile.addCards(cards);
        setPlayerStatusValues();
    }

    public void reset() {
        // Update action status
        handCards.disableAllCards();
        numActions = 1;
        numCoins = 0;
        numPurchases = 1;
        setActionBarValues();
    }

    public void trashHandCard(Card card) {
        LogBox.logTrashCard(this, card);
        handCards.removeCard(card);
        card.disableUi();
        setPlayerStatusValues();
    }

    public void trashHandCards(List<Card> cards) {
        for (Card card : cards) {
            LogBox.logTrashCard(this, card);
        }
        handCards.removeCards(cards);
        for (Card card : cards) {
            card.disableUi();
        }
        setPlayerStatusValues();
    }

    public void discardHandCard(Card card) {
        handCards.removeCard(card);
        discardPile.addCard(card);
        setPlayerStatusValues();
    }

    public void discardHandCards(List<Card> cards) {
        handCards.removeCards(cards);
        discardPile.addCards(cards);
        setPlayerStatusValues();
    }

    public void drawCards(int numCards) {
        // Check bounds
        if (numCards > discardPile.getNumCards() + deck.getNumCards()) {
            List<Card> cards = deck.popCards(deck.getNumCards());
            handCards.addCards(cards);
            List<Card> newCards = discardPile.getCards();
            discardPile.removeCards();
            handCards.addCards(newCards);
            LogBox.logDrawCard(this, cards.size() + newCards.size());
            return;
        }

        // Refill the deck if it's empty
        if (deck.isEmpty()) {
            List<Card> newCards = discardPile.getCards();
            discardPile.removeCards();
            deck.addCards(newCards, true);
        }

        // Draw cards from the deck
        List<Card> cards = deck.popCards(numCards);
        LogBox.logDrawCard(this, cards.size());
        handCards.addCards(cards);

        // Draw cards again if not enough
        if (cards.size() < numCards) {
            drawCards(numCards - cards.size());
        }
        setPlayerStatusValues();
    }

    public int getNumScores() {
        return numScores;
    }

    private void setActionBarValues() {
        actionBar.setNumActions(numActions);
        actionBar.setNumPurchases(numPurchases);
        actionBar.setNumCoins(numCoins);
    }

    /*
        Call this when finishing operations on deck/hand/discard pile.
    */
    private void setPlayerStatusValues() {
        // Update num cards of deck/hand/discard pile
        int numDeckCards = deck.getNumCards();
        int numDiscardPileCards = discardPile.getNumCards();
        int numHandCards = handCards.getNumCards();
        playerStatus.setNumDeckCards(numDeckCards);
        playerStatus.setNumDiscardPileCards(numDiscardPileCards);
        playerStatus.setNumHandCards(numHandCards);

        // Update scores
        numScores = handCards.getNumScores(this) + deck.getNumScores(this) + discardPile.getNumScores(this);
        playerStatus.setScore(numScores);
    }


    class StatusSnapshot {
        String buttonText;
        String status;
        EventHandler buttonHandler;
        CardSelectedHandler cardSelectedHandler;

        StatusSnapshot(String buttonText, String status, EventHandler buttonHandler, CardSelectedHandler cardSelectedHandler) {
            this.buttonText = buttonText;
            this.status = status;
            this.buttonHandler = buttonHandler;
            this.cardSelectedHandler = cardSelectedHandler;
        }
    }

    private StatusSnapshot statusSnapshot;

    public void snapshotStatus() {
        statusSnapshot = new StatusSnapshot(actionBar.getRightButtonText(), actionBar.getStatus(),
                actionBar.getRightButtonOnPressed(), handCards.getCardSelectedHandler());
    }

    public void recoverStatus() {
        if (statusSnapshot != null) {
            setActionBarStatus(statusSnapshot.status, statusSnapshot.buttonText);
            setCardSelectedHandler(statusSnapshot.cardSelectedHandler);
            setActionBarRightButtonHandler(statusSnapshot.buttonHandler);
            statusSnapshot = null;
        }
    }

    /* Hand Cards Selection*/
    public void startSelectingHandCards(String statusText, int cardId) {
        if (GameManager.getCurrentPhase() == GameManager.Phase.SelectingHandCards) {
            snapshotStatus();
            setActionBarStatus(statusText, "完成", "重新選擇");
            setCardSelectedHandler((card) -> {
                if (selectingHandCardsFilter == null || selectingHandCardsFilter.filter(card)) {
                    GameManager.sendEvent(new SelectHandCardEvent(id, card.getId()));
                }
            });
            setActionBarLeftButtonHandler((e) -> {
                GameManager.sendEvent(new ClearSelectedHandCardsEvent(id));
            });
            setActionBarRightButtonHandler((e) -> {
                GameManager.sendEvent(new DoneSelectingHandCardEvent(id, cardId));
            });
            actionBar.enableLeftButton(false);
            if (exactSelectingCards > 0) {
                actionBar.enableRightButton(selectedCards.size() == exactSelectingCards);
            }
        }
    }

    public void doneHandCardsSelection(int cardId) {
        for (Card card : selectedCards) {
            card.removeHighlight();
        }
        maxSelectedCard = Integer.MAX_VALUE;
        exactSelectingCards = 0;
        actionBar.enableRightButton(true);
        actionBar.enableLeftButton(false);
        selectingHandCardsFilter = null;
        recoverStatus();

        HasHandCardsSelection card = (HasHandCardsSelection) fieldCards.getCardByCardId(cardId);
        card.performSelection(this, selectedCards);
        clearSelectedHandCards();
    }

    public void setSelectingHandCardsFilter(CardFilter filter) {
        if (GameManager.getCurrentPhase() == GameManager.Phase.SelectingHandCards) {
            this.selectingHandCardsFilter = filter;
        }
    }

    public void setSelectingDisplayedCardsFilter(DisplayedCardFilter filter) {
        if (GameManager.getCurrentPhase() == GameManager.Phase.SelectingDisplayedCards) {
            this.selectingDisplayedCardsFilter = filter;
        }
    }

    public void setMaxSelectingCards(int maxSelectingCards) {
        if (GameManager.getCurrentPhase() == GameManager.Phase.SelectingHandCards ||
                GameManager.getCurrentPhase() == GameManager.Phase.SelectingDisplayedCards) {
            this.maxSelectedCard = maxSelectingCards;
        }
    }

    public void setExactSelectingCards(int exactSelectingCards) {
        if (GameManager.getCurrentPhase() == GameManager.Phase.SelectingHandCards ||
                GameManager.getCurrentPhase() == GameManager.Phase.SelectingDisplayedCards) {
            this.exactSelectingCards = exactSelectingCards;
            this.maxSelectedCard = exactSelectingCards;
            actionBar.enableRightButton(false);
        }
    }

    public void selectHandCard(int cardId) {
        if (GameManager.getCurrentPhase() == GameManager.Phase.SelectingHandCards) {
            Card card = handCards.getCardByCardId(cardId);
            if (!selectedCards.contains(card) && selectedCards.size() < maxSelectedCard) {
                card.setHighlight();
                selectedCards.add(card);
                handCards.setToBottom(card);
            }
            actionBar.enableLeftButton(selectedCards.size() > 0);
            if (exactSelectingCards > 0) {
                actionBar.enableRightButton(selectedCards.size() == exactSelectingCards);
            }
        }
    }

    public void clearSelectedHandCards() {
        selectedCards.clear();
        handCards.rearrange();
        actionBar.enableLeftButton(false);
        if (exactSelectingCards > 0) {
            actionBar.enableRightButton(selectedCards.size() == exactSelectingCards);
        }
    }

    //    Select Displayed Card
    public void startSelectingDisplayedCards(String statusText, int cardId) {
        if (GameManager.getCurrentPhase() == GameManager.Phase.SelectingDisplayedCards) {
            snapshotStatus();
            setActionBarStatus(statusText, "完成", "重新選擇");
            PurchaseArea.setDisplayedCardSelectedHandler((displayedCard) -> {
                if (selectingDisplayedCardsFilter == null || selectingDisplayedCardsFilter.filter(displayedCard)) {
                    GameManager.sendEvent(new SelectDisplayedCardEvent(id, displayedCard.getId()));
                }
            });
            setActionBarLeftButtonHandler((e) -> {
                GameManager.sendEvent(new ClearSelectedDisplayedCardsEvent(id));
            });
            setActionBarRightButtonHandler((e) -> {
                PurchaseArea.rearrange();
                GameManager.sendEvent(new DoneSelectingDisplayedCardEvent(id, cardId));
            });
            setCardSelectedHandler(((card) -> {
            }));
            actionBar.enableLeftButton(false);
        }
    }

    public void doneDisplayedCardsSelection(int cardId) {
        for (DisplayedCard displayedCard : selectedDisplayedCards) {
            displayedCard.removeHighlight();
        }
        maxSelectedCard = Integer.MAX_VALUE;
        exactSelectingCards = 0;
        selectingDisplayedCardsFilter = null;
        actionBar.enableRightButton(true);
        actionBar.enableLeftButton(false);
        PurchaseArea.setDisplayedCardSelectedHandler((displayedCard) -> {
            displayedCard.setDisplayCardEventHandler(displayedCard.getOriginalHandler());
        });
        recoverStatus();
        HasDisplayedCardsSelection card = (HasDisplayedCardsSelection) fieldCards.getCardByCardId(cardId);
        card.performDisplayedSelection(this, selectedDisplayedCards);
        clearSelectedDisplayedCards();
    }

    public void selectDisplayedCard(int displayedCardId) {
        if (GameManager.getCurrentPhase() == GameManager.Phase.SelectingDisplayedCards) {
            DisplayedCard displayedCard = PurchaseArea.getDisplayedCardById(displayedCardId);
            if (!selectedDisplayedCards.contains(displayedCard) && selectedDisplayedCards.size() < maxSelectedCard) {
                if (id == GameManager.getApplicationPlayer().getId() && displayedCard != null) {
                    displayedCard.setHighlight();
                }
                selectedDisplayedCards.add(displayedCard);
            }
            actionBar.enableLeftButton(selectedDisplayedCards.size() > 0);
            if (exactSelectingCards > 0) {
                actionBar.enableRightButton(selectedDisplayedCards.size() == exactSelectingCards);
            }
        }
    }

    public void clearSelectedDisplayedCards() {
        selectedDisplayedCards.clear();
        PurchaseArea.rearrange();
        actionBar.enableLeftButton(false);
        if (exactSelectingCards > 0) {
            actionBar.enableRightButton(selectedDisplayedCards.size() == exactSelectingCards);
        }
    }

    public void receiveNewCardOnDeck(Card card) {
        deck.addCard(card);
        LogBox.logReceiveCard(this, card);
        setPlayerStatusValues();
    }

    public void removeHandCard(Card card) {
        handCards.removeCard(card);
        setPlayerStatusValues();
    }

    public List<Card> popDeckTop(int numCards) {
        List<Card> ret = deck.popCards(numCards);
        setPlayerStatusValues();
        return ret;
    }

    public void addCardsToDiscardPile(List<Card> cards){
        discardPile.addCards(cards);
        setPlayerStatusValues();
    }
}
