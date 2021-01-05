package com.knoban.ultimates.events;

import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.bukkit.event.player.PlayerEvent;

/**
 * An {@link Event} that shows that {@link #getPlayer()} is no longer
 * considered to be in combat because of {@link #getReason()}.
 * Generally speaking, filtering based on {@link #getReason()} before performing any action is necessary.
 */
public class CombatLeaveEvent extends PlayerEvent {
	private static final HandlerList HANDLER_LIST = new HandlerList();
	private final Reason reason;
	
	/**
	 * Creates and initializes a new instance.
	 *
	 * @param player the player who left combat
	 * @param reason the reason why the player left combat
	 */
	public CombatLeaveEvent(Player player, Reason reason) {
		super(player);
		this.reason = reason;
	}
	
	/**
	 * Gets the reason why {@link #getPlayer()} is no longer being considered to be in combat.
	 *
	 * @return the reason why {@link #getPlayer()} left combat
	 */
	public Reason getReason() {
		return reason;
	}
	
	@Override
	public HandlerList getHandlers() {
		return HANDLER_LIST;
	}
	
	public static HandlerList getHandlerList() {
		return HANDLER_LIST;
	}
	
	/**
	 * The reason why the {@link Player} is no longer considered to be in combat.
	 */
	public enum Reason {
		
		/**
		 * The in-combat status automatically wears off after some time.
		 * This time has passed for the {@link Player}.
		 */
		TIMEOUT,
		
		/**
		 * The {@link Player} has died.
		 */
		DEATH,
		
		/**
		 * The {@link Player} has left the server while being considered to be in-combat.
		 * If the {@link Player} logs back before much time passes, then {@link CombatEnterEvent}
		 * with {@link CombatEnterEvent.Reason#LOGIN_RESUME} is going to be called, but it is not guaranteed.
		 */
		QUIT_PAUSE
	}
}
