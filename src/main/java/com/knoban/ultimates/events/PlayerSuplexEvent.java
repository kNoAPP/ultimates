package com.knoban.ultimates.events;

import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

public class PlayerSuplexEvent extends Event implements Cancellable {

    private final Player suplexer;
    private final Entity suplexee;
    private boolean cancel;
    private static final HandlerList HANDLERS_LIST = new HandlerList();

    public PlayerSuplexEvent(Player suplexer, Entity suplexee) {
        this.suplexer = suplexer;
        this.suplexee = suplexee;
        this.cancel = false;
    }

    public Player getSuplexer() {
        return suplexer;
    }

    public Entity getSuplexee() {
        return suplexee;
    }

    @Override
    public boolean isCancelled() {
        return cancel;
    }

    @Override
    public void setCancelled(boolean cancel) {
        this.cancel = cancel;
    }

    @Override
    public HandlerList getHandlers() {
        return HANDLERS_LIST;
    }

    public static HandlerList getHandlerList() {
        return HANDLERS_LIST;
    }
}
