package com.knoban.ultimates.aspects.warmup;

import com.knoban.ultimates.Ultimates;
import com.knoban.ultimates.aspects.CombatStateManager;
import com.knoban.ultimates.aspects.MoveCallbackManager;
import com.knoban.ultimates.events.CardDiscardEvent;
import com.knoban.ultimates.events.CombatEnterEvent;
import net.kyori.adventure.text.Component;
import org.apache.commons.lang.Validate;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerTeleportEvent;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Predicate;

/**
 * Class dedicated to handling actions warmups: the countdown before an action can be activated.
 * This class internally uses {@link MoveCallbackManager} and {@link CombatStateManager}.
 */
public final class ActionWarmupManager implements Listener {
	private final Map<Player, List<ActionWarmupTask>> playerToTasks = new HashMap<>();
	private final List<ActionWarmupTask> rawTasks = new ArrayList<>();
	private final Ultimates plugin;
	
	/**
	 * Creates and initializes a new instance.
	 *
	 * @param plugin the plugin instance
	 */
	public ActionWarmupManager(Ultimates plugin) {
		this.plugin = plugin;
		Bukkit.getPluginManager().registerEvents(this, plugin);
		
		Bukkit.getScheduler().runTaskTimer(plugin, () -> {
			//copying is necessary: callbacks might register/unregister tasks
			for(ActionWarmupTask task : new ArrayList<>(rawTasks)) {
				//tasks might unregister each other
				if(task.isActive()) {
					task.tick();
					//the tick method's callbacks might unregister this task
					if(task.isActive() && task.tryComplete()) {
						//the complete callback might unregister this task, but isActive returns false now
						if(rawTasks.contains(task)) {
							removeTask(task);
						}
					}
				}
			}
		}, 1, 1);
	}
	
	/**
	 * Safely shuts this system down.
	 */
	public void shutdown() {
		for(Player player : new ArrayList<>(playerToTasks.keySet())) {
			onQuit(new PlayerQuitEvent(player, (Component) null, PlayerQuitEvent.QuitReason.DISCONNECTED));
		}
	}
	
	/**
	 * Starts a new warmup.
	 * If combat isn't allowed, then the player mustn't already be in combat.
	 *
	 * @param player the player the warmup is for
	 * @param template the template defining the warmup
	 * @return the newly created warmup
	 */
	public ActionWarmupTask register(Player player, ActionWarmupTaskTemplate template) {
		if(template.getCombat() != ActionWarmupTaskTemplate.Combat.ALLOW) {
			Validate.isTrue(!plugin.getCombatManager().isInCombat(player),
					"Player mustn't already be in combat if combat isn't allowed");
		}
		
		MoveCallbackManager.RegisteredCallback registeredMovement = null;
		MovementCallback movementCallback = null;
		if(template.getMovement() == ActionWarmupTaskTemplate.Movement.DISALLOW
				|| template.getMovement() == ActionWarmupTaskTemplate.Movement.ALLOW_VERTICAL) {
			movementCallback = new MovementCallback();
			registeredMovement = plugin.getMoveCallbackManager().register(player,
					movementCallback, false, template.getMovement() == ActionWarmupTaskTemplate.Movement.ALLOW_VERTICAL, true);
			//only call once is false: we can handle the unregistration ourselves
		}
		
		ActionWarmupTask task = template.instantiate(plugin, player, registeredMovement);
		if(movementCallback != null) {
			movementCallback.task = task;
		}
		
		playerToTasks.computeIfAbsent(player, ignored -> new ArrayList<>(1)).add(task);
		rawTasks.add(task);
		return task;
	}
	
	/**
	 * Stops a warmup created using {@link #register(Player, ActionWarmupTaskTemplate)}.
	 *
	 * @param task the warmup to stop
	 */
	public void unregister(ActionWarmupTask task) {
		interrupt(task, ActionWarmupTaskTemplate.InterruptReason.PLUGIN_CODE);
	}
	
	/**
	 * Gets whether the specified player has a warmup that was created using the specified template.
	 *
	 * @param player the player to check
	 * @param template the template to search for
	 * @return whether the player has a warmup from the specified template
	 */
	public boolean hasTaskCreatedFrom(Player player, ActionWarmupTaskTemplate template) {
		List<ActionWarmupTask> list = playerToTasks.get(player);
		if(list != null) {
			//noinspection Convert2streamapi
			for(ActionWarmupTask task : list) {
				if(task.wasCreatedFrom(template)) {
					return true;
				}
			}
		}
		return false;
	}
	
	@EventHandler(ignoreCancelled = true, priority = EventPriority.NORMAL)
	private void onCombat(CombatEnterEvent event) {
		tryInterrupt(event.getPlayer(), ActionWarmupTaskTemplate.InterruptReason.COMBAT,
				task -> task.getCombat() == ActionWarmupTaskTemplate.Combat.DISALLOW);
	}
	
	@EventHandler(ignoreCancelled = true, priority = EventPriority.NORMAL)
	private void onDiscard(CardDiscardEvent event) {
		tryInterrupt(event.getPlayer(), ActionWarmupTaskTemplate.InterruptReason.DISCARDED_CARD,
				task -> task.getCard() == event.getCard().getClass());
	}
	
	//highest priority: event is cancellable
	@EventHandler(ignoreCancelled = true, priority = EventPriority.HIGHEST)
	private void onDeath(PlayerDeathEvent event) {
		tryInterrupt(event.getEntity(), ActionWarmupTaskTemplate.InterruptReason.DEATH, ignored -> true);
	}
	
	@EventHandler(ignoreCancelled = true, priority = EventPriority.NORMAL)
	private void onQuit(PlayerQuitEvent event) {
		tryInterrupt(event.getPlayer(), ActionWarmupTaskTemplate.InterruptReason.LOGOUT, ignored -> true);
	}
	
	private void tryInterrupt(Player player, ActionWarmupTaskTemplate.InterruptReason reason, Predicate<ActionWarmupTask> filter) {
		List<ActionWarmupTask> list = playerToTasks.get(player);
		if(list != null) {
			for(ActionWarmupTask task : new ArrayList<>(list)) {
				//tasks might unregister each other
				if(task.isActive() && filter.test(task)) {
					interrupt(task, reason);
				}
			}
		}
	}
	
	private void interrupt(ActionWarmupTask task, ActionWarmupTaskTemplate.InterruptReason reason) {
		removeTask(task);
		task.interrupt(reason);
	}
	
	private void removeTask(ActionWarmupTask task) {
		Validate.isTrue(rawTasks.remove(task));
		List<ActionWarmupTask> list = playerToTasks.get(task.getPlayer());
		Validate.isTrue(list.remove(task));
		if(list.isEmpty()) {
			playerToTasks.remove(task.getPlayer());
		}
	}
	
	private class MovementCallback implements MoveCallbackManager.Callback {
		ActionWarmupTask task;
		
		@Override
		public void onMove(Player player, PlayerMoveEvent event) {
			interrupt(task, ActionWarmupTaskTemplate.InterruptReason.MOVEMENT);
		}
		
		@Override
		public void onTeleport(Player player, PlayerTeleportEvent event) {
			onMove(player, event);
		}
		
		@Override
		public void onCombat(Player player) {} //handled by listener
		
		@Override
		public void onDeath(Player player) {} //handled by listener
		
		@Override
		public void onQuit(Player player) {} //handled by listener
	}
}
