package com.knoban.ultimates.events;

import com.knoban.ultimates.cards.Card;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

public class CardDiscardEvent extends Event {

    private final Player player;
    private final Card card;
    private static final HandlerList HANDLERS_LIST = new HandlerList();

    public CardDiscardEvent(Player player, Card card) {
        this.player = player;
        this.card = card;
    }

    public Player getPlayer() {
        return player;
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
