package com.knoban.ultimates.aspects;

import co.aikar.timings.Timing;
import co.aikar.timings.Timings;
import com.knoban.ultimates.Ultimates;
import com.knoban.ultimates.events.CombatEnterEvent;
import org.apache.commons.lang.Validate;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerTeleportEvent;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;

/**
 * Class designed to fulfill all player movement listening related needs using callbacks.
 * This class internally uses {@link CombatStateManager}, as combat and movement are often handled the same way.
 * Server shutdown is as if {@link PlayerQuitEvent} was fired for each player.
 * <br><br>
 * Callbacks are automatically unregistered in case of {@link PlayerDeathEvent} and {@link PlayerQuitEvent},
 * and of course in case it was specified that the callback should unregister after a single call.
 */
public class MoveCallbackManager implements Listener {
	private final Map<Player, List<RegisteredCallback>> entries = new HashMap<>();
	private final Ultimates plugin;
	
	/**
	 * Creates and initializes a new instance.
	 *
	 * @param plugin the plugin instance
	 */
	public MoveCallbackManager(Ultimates plugin) {
		this.plugin = plugin;
		plugin.getServer().getPluginManager().registerEvents(this, plugin);
	}
	
	/**
	 * Safely shuts this system down.
	 */
	public void shutdown() {
		for(Player player : new ArrayList<>(entries.keySet())) {
			onQuit(new PlayerQuitEvent(player, (String) null));
		}
	}
	
	/**
	 * Registers a new callback.
	 *
	 * @param player the player to listen for
	 * @param callback the callback to call in case of an event
	 * @param onlyCallOnce whether the callback should be unregistered after being called once
	 * @param allowMovementY whether to ignore movement on the vertical axis
	 * @param allowCombat whether to ignore the player entering combat (without moving)
	 * (if this is set, then the player mustn't already be in combat)
	 * @return an instance that can be used in {@link #unregister(Player, RegisteredCallback)}
	 */
	public RegisteredCallback register(Player player, Callback callback, boolean onlyCallOnce,
			boolean allowMovementY, boolean allowCombat) {
		if(!allowCombat) {
			Validate.isTrue(!plugin.getCombatManager().isInCombat(player),
					"Player mustn't already be in combat if combat isn't allowed");
		}
		
		RegisteredCallback registered = new RegisteredCallback(plugin, callback, onlyCallOnce, allowMovementY, allowCombat);
		entries.computeIfAbsent(player, k -> new ArrayList<>(1)).add(registered);
		return registered;
	}
	
	/**
	 * Stops a callback that was registered using {@link #register(Player, Callback, boolean, boolean, boolean)}.
	 * Must only be called once for each {@link RegisteredCallback} instance.
	 * Keep in mind that unregistration might be done internally for eg. {@code onlyCallOnce} callbacks.
	 * If these guarantees cannot be made, use {@link #tryUnregister(Player, RegisteredCallback)} instead.
	 *
	 * @param player the player whose callback to unregister
	 * @param callback the callback to unregister
	 */
	public void unregister(Player player, RegisteredCallback callback) {
		Validate.isTrue(tryUnregister(player, callback), "The callback must still be registered");
	}
	
	/**
	 * Stops a callback that was registered using {@link #register(Player, Callback, boolean, boolean, boolean)}.
	 * This method doesn't fail in case there is nothing to unregister.
	 * In case you do want to throw exceptions, use {@link #unregister(Player, RegisteredCallback)}.
	 *
	 * @param player the player whose callback to unregister
	 * @param callback the callback to unregister
	 * @return whether a callback was actually unregistered
	 */
	public boolean tryUnregister(Player player, RegisteredCallback callback) {
		List<RegisteredCallback> list = entries.get(player);
		boolean success = list != null && list.remove(callback);
		Validate.isTrue(success == callback.isActive());
		callback.active = false;
		if(list != null && list.isEmpty()) {
			entries.remove(player);
		}
		return success;
	}
	
	@EventHandler(ignoreCancelled = true, priority = EventPriority.HIGHEST)
	private void onMove(PlayerMoveEvent event) {
		handleEvent(event);
	}
	
	@EventHandler(ignoreCancelled = true, priority = EventPriority.HIGHEST)
	private void onTeleport(PlayerTeleportEvent event) {
		handleEvent(event);
	}
	
	private void handleEvent(PlayerMoveEvent event) {
		List<RegisteredCallback> list = entries.get(event.getPlayer());
		if(list == null) {
			return;
		}
		
		Location from = event.getFrom();
		Location to = event.getTo();
		if(blockEquals(from, to)) {
			return;
		}
		
		boolean isTeleport = event instanceof PlayerTeleportEvent;
		boolean onlyChangedY = from.getBlockX() == to.getBlockX() && from.getBlockZ() == to.getBlockZ();
		//copy list: can get modified while iterating
		for(RegisteredCallback callback : new ArrayList<>(list)) {
			if(!callback.active) {
				continue; //callbacks can unregister each other
			}
			
			if(callback.isAllowMovementY() && onlyChangedY) {
				continue;
			}
			
			callback.timing.startTiming();
			try {
				if(isTeleport) {
					callback.getCallback().onTeleport(event.getPlayer(), (PlayerTeleportEvent) event);
				} else {
					callback.getCallback().onMove(event.getPlayer(), event);
				}
			} catch (Throwable t) {
				plugin.getLogger().log(Level.SEVERE, "Error handling callback in " + getClass().getSimpleName(), t);
			}
			callback.timing.stopTiming();
			if(callback.isOnlyCallOnce()) {
				tryUnregister(event.getPlayer(), callback);
			}
			
			if(event.isCancelled()) {
				break;
			}
		}
	}
	
	@EventHandler(ignoreCancelled = true, priority = EventPriority.NORMAL)
	private void onCombat(CombatEnterEvent event) {
		List<RegisteredCallback> list = entries.get(event.getPlayer());
		if(list == null) {
			return;
		}
		
		//copy list: can get modified while iterating
		for(RegisteredCallback callback : new ArrayList<>(list)) {
			if(!callback.active) {
				continue; //callbacks can unregister each other
			}
			
			if(callback.isAllowCombat()) {
				continue;
			}
			
			callback.timing.startTiming();
			try {
				callback.getCallback().onCombat(event.getPlayer());
			} catch (Throwable t) {
				plugin.getLogger().log(Level.SEVERE, "Error handling callback in " + getClass().getSimpleName(), t);
			}
			callback.timing.stopTiming();
			if(callback.isOnlyCallOnce()) {
				tryUnregister(event.getPlayer(), callback);
			}
		}
	}
	
	//highest priority: event is cancellable
	@EventHandler(ignoreCancelled = true, priority = EventPriority.HIGHEST)
	private void onDeath(PlayerDeathEvent event) {
		//don't remove from the map: allow callbacks to unregister each other
		List<RegisteredCallback> list = entries.get(event.getEntity());
		if(list == null) {
			return;
		}
		
		//copy list: can get modified while iterating
		for(RegisteredCallback callback : new ArrayList<>(list)) {
			if(!callback.active) {
				continue; //callbacks can unregister each other
			}
			
			callback.timing.startTiming();
			try {
				callback.getCallback().onDeath(event.getEntity());
			} catch (Throwable t) {
				plugin.getLogger().log(Level.SEVERE, "Error handling callback in " + getClass().getSimpleName(), t);
			}
			callback.timing.stopTiming();
			tryUnregister(event.getEntity(), callback);
		}
		
		list = entries.remove(event.getEntity());
		if(list != null && !list.isEmpty()) {
			for(RegisteredCallback callback : list) {
				plugin.getLogger().severe(getClass().getSimpleName()
						+ " callback registered during player death: " + callback.getCallback().getClass().getName());
				callback.active = false;
			}
		}
	}
	
	@EventHandler(ignoreCancelled = true, priority = EventPriority.NORMAL)
	private void onQuit(PlayerQuitEvent event) {
		//don't remove from the map: allow callbacks to unregister each other
		List<RegisteredCallback> list = entries.get(event.getPlayer());
		if(list == null) {
			return;
		}
		
		//copy list: can get modified while iterating
		for(RegisteredCallback callback : new ArrayList<>(list)) {
			if(!callback.active) {
				continue; //callbacks can unregister each other
			}
			
			callback.timing.startTiming();
			try {
				callback.getCallback().onQuit(event.getPlayer());
			} catch (Throwable t) {
				plugin.getLogger().log(Level.SEVERE, "Error handling callback in " + getClass().getSimpleName(), t);
			}
			callback.timing.stopTiming();
			tryUnregister(event.getPlayer(), callback);
		}
		
		list = entries.remove(event.getPlayer());
		if(list != null && !list.isEmpty()) {
			for(RegisteredCallback callback : list) {
				plugin.getLogger().severe(getClass().getSimpleName()
						+ " callback registered during player quitting: " + callback.getCallback().getClass().getName());
				callback.active = false;
			}
		}
	}
	
	private boolean blockEquals(Location alpha, Location beta) {
		return alpha.getBlockX() == beta.getBlockX()
				&& alpha.getBlockY() == beta.getBlockY()
				&& alpha.getBlockZ() == beta.getBlockZ();
	}
	
	/**
	 * The base class for all callbacks used in this class.
	 *
	 * @see SimpleCallback
	 */
	public interface Callback {
		void onMove(Player player, PlayerMoveEvent event);
		
		void onTeleport(Player player, PlayerTeleportEvent event);
		
		void onCombat(Player player);
		
		void onDeath(Player player);
		
		void onQuit(Player player);
	}
	
	/**
	 * A {@link Callback} implementation that calls {@link #onTriggered(Player)}
	 * from {@link #onMove(Player, PlayerMoveEvent)}, {@link #onTeleport(Player, PlayerTeleportEvent)}
	 * and {@link #onCombat(Player)}.
	 */
	public interface SimpleCallback extends Callback {
		default void onMove(Player player, PlayerMoveEvent event) {
			onTriggered(player);
		}
		
		default void onTeleport(Player player, PlayerTeleportEvent event) {
			onTriggered(player);
		}
		
		default void onCombat(Player player) {
			onTriggered(player);
		}
		
		void onTriggered(Player player);
	}
	
	/**
	 * Represents a {@link Callback} that was registered using {@link #register(Player, Callback, boolean, boolean, boolean)}.
	 * Instances can be used in {@link #unregister(Player, RegisteredCallback)}.
	 */
	public static final class RegisteredCallback {
		private final Callback callback;
		private final boolean onlyCallOnce;
		private final boolean allowMovementY;
		private final boolean allowCombat;
		final Timing timing;
		boolean active = true;
		
		RegisteredCallback(JavaPlugin plugin, Callback callback,
				boolean onlyCallOnce, boolean allowMovementY, boolean allowCombat) {
			this.callback = callback;
			this.onlyCallOnce = onlyCallOnce;
			this.allowMovementY = allowMovementY;
			this.allowCombat = allowCombat;
			timing = Timings.of(plugin, MoveCallbackManager.class.getSimpleName()
					+ " - " + callback.getClass().getName());
		}
		
		public Callback getCallback() {
			return callback;
		}
		
		public boolean isOnlyCallOnce() {
			return onlyCallOnce;
		}
		
		public boolean isAllowMovementY() {
			return allowMovementY;
		}
		
		public boolean isAllowCombat() {
			return allowCombat;
		}
		
		public boolean isActive() {
			return active;
		}
	}
}
