package com.knoban.ultimates.events;

import com.knoban.ultimates.cards.Card;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

public class CardRegisterEvent extends Event {

    private final Card card;
    private static final HandlerList HANDLERS_LIST = new HandlerList();

    public CardRegisterEvent(Card card) {
        this.card = card;
    }

    public Card getCard() {
        return card;
    }

    @Override
    public HandlerList getHandlers() {
        return HANDLERS_LIST;
    }

    public static HandlerList getHandlerList() {
        return HANDLERS_LIST;
    }
}
