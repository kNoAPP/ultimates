package com.knoban.ultimates.events;

import org.bukkit.Chunk;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.bukkit.event.player.PlayerEvent;
import org.bukkit.event.player.PlayerTeleportEvent;

/**
 * An event that is fired before {@link PlayerTeleportEvent}.
 * These events are triggered by player abilities and not by force TP commands.
 * This means that the player might shouldn't be able to teleport the destination using the ability.
 * This event is exactly for that: by cancelling this event, the player won't be able to teleport,
 * {@link Player#teleport(Location)} and {@link PlayerTeleportEvent} won't get called.
 */
public class PreAbilityTeleportEvent extends PlayerEvent implements Cancellable {
	private static final HandlerList HANDLER_LIST = new HandlerList();
	private final Location destination;
	private final boolean loaded;
	private boolean cancelled;
	
	/**
	 * Creates and initializes a new instance.
	 *
	 * @param player the player who is trying to teleport
	 * @param destination the teleportation destination
	 * @param willLoad whether the caller of this method intends on loading the destination {@link Chunk} in the future,
	 * should it not already be loaded. (This parameter exists to not load chunks for no reason.)
	 */
	public PreAbilityTeleportEvent(Player player, Location destination, boolean willLoad) {
		super(player);
		this.destination = destination;
		loaded = !willLoad || destination.getWorld().isChunkLoaded(destination.getBlockX() >> 4, destination.getBlockZ() >> 4);
	}
	
	/**
	 * The {@link Location} the player is attempting to teleport to.
	 *
	 * @return the teleportation destination
	 */
	public Location getDestination() {
		return destination;
	}
	
	/**
	 * Whether the {@link #getDestination()} {@link Chunk} is loaded yet.
	 * More precisely, only returns false if the {@link Chunk} isn't loaded,
	 * but will be loaded at a later time, at which point this {@link Event} will get called again.
	 *
	 * @return whether the {@link #getDestination()} {@link Chunk} can be considered loaded
	 */
	public boolean isDestinationLoaded() {
		return loaded;
	}
	
	@Override
	public boolean isCancelled() {
		return cancelled;
	}
	
	@Override
	public void setCancelled(boolean cancel) {
		cancelled = cancel;
	}
	
	@Override
	public HandlerList getHandlers() {
		return HANDLER_LIST;
	}
	
	public static HandlerList getHandlerList() {
		return HANDLER_LIST;
	}
}
