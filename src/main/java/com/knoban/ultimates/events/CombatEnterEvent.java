package com.knoban.ultimates.events;

import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.EventPriority;
import org.bukkit.event.HandlerList;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.player.PlayerEvent;

/**
 * An {@link Event} that shows that {@link #getPlayer()} is now considered to be in combat because of {@link #getReason()}.
 * Keep in mind while this event can get called during {@link EntityDamageEvent},
 * it is only called late due to the {@link EventPriority} setting in the listener.
 * So generally speaking, event listeners get notified of this event only after {@link EntityDamageEvent}.
 */
public class CombatEnterEvent extends PlayerEvent {
	private static final HandlerList HANDLER_LIST = new HandlerList();
	private final Reason reason;
	
	/**
	 * Creates and initializes a new instance.
	 *
	 * @param player the player who entered combat
	 * @param reason the reason why the player entered combat
	 */
	public CombatEnterEvent(Player player, Reason reason) {
		super(player);
		this.reason = reason;
	}
	
	/**
	 * Gets the reason why {@link #getPlayer()} is now being considered to be in combat.
	 *
	 * @return the reason why {@link #getPlayer()} entered combat
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
	 * The reason why the {@link Player} is now considered to be in combat.
	 */
	public enum Reason {
		
		/**
		 * The {@link Player} was involved in a (non-cancelled)
		 * {@link EntityDamageEvent} or something similar.
		 */
		TAKE_DAMAGE,
		
		/**
		 * The {@link Player} was involved in a (non-cancelled)
		 * {@link EntityDamageByEntityEvent} or something similar.
		 */
		DEAL_DAMAGE,
		
		/**
		 * The {@link Player} has quit while in combat, causing {@link CombatLeaveEvent}
		 * to be fired with {@link CombatLeaveEvent.Reason#QUIT_PAUSE}.
		 * Now the {@link Player} has logged back before much time has passed.
		 * No guarantees are made, do not rely on this reason.
		 */
		LOGIN_RESUME
	}
}
