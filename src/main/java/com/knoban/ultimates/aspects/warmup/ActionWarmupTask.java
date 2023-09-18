package com.knoban.ultimates.aspects.warmup;

import com.knoban.ultimates.Ultimates;
import com.knoban.ultimates.aspects.MoveCallbackManager;
import com.knoban.ultimates.cards.Card;
import org.apache.commons.lang3.Validate;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;
import java.util.logging.Level;

/**
 * A warmup that might be in progress (check {@link #isActive()})
 * created by {@link ActionWarmupManager} based on a {@link ActionWarmupTaskTemplate}.
 */
public final class ActionWarmupTask {
	private final List<TickHandler> handlers = new ArrayList<>();
	private final Ultimates plugin;
	private final Player player;
	private final ActionWarmupTaskTemplate template;
	private final Function<ActionWarmupTask, Boolean> isComplete;
	private final Object companion;
	private final ActionWarmupTaskTemplate.Movement movement;
	private final ActionWarmupTaskTemplate.Combat combat;
	private final Class<? extends Card> card;
	private final ActionWarmupTaskTemplate.CompletedCallback onCompleted;
	private final ActionWarmupTaskTemplate.InterruptedCallback onInterrupted;
	private final MoveCallbackManager.RegisteredCallback registeredMovement;
	private int elapsedTicks;
	private boolean active = true;
	
	ActionWarmupTask(Ultimates plugin, Player player, ActionWarmupTaskTemplate template,
			List<TickHandler> handlers, Function<ActionWarmupTask, Boolean> isComplete, Object companion,
			ActionWarmupTaskTemplate.Movement movement, ActionWarmupTaskTemplate.Combat combat, Class<? extends Card> card,
			ActionWarmupTaskTemplate.CompletedCallback onCompleted, ActionWarmupTaskTemplate.InterruptedCallback onInterrupted,
			MoveCallbackManager.RegisteredCallback registeredMovement) {
		this.handlers.addAll(handlers);
		this.plugin = plugin;
		this.player = player;
		this.template = template;
		this.isComplete = isComplete;
		this.companion = companion;
		this.movement = movement;
		this.combat = combat;
		this.card = card;
		this.onCompleted = onCompleted;
		this.onInterrupted = onInterrupted;
		this.registeredMovement = registeredMovement;
	}
	
	public Player getPlayer() {
		return player;
	}
	
	public <T> T getCompanion() {
		//noinspection unchecked
		return (T) companion;
	}
	
	public <T> T getCompanion(Class<T> clazz) {
		return clazz.cast(companion);
	}
	
	public ActionWarmupTaskTemplate.Movement getMovement() {
		return movement;
	}
	
	public ActionWarmupTaskTemplate.Combat getCombat() {
		return combat;
	}
	
	public Class<? extends Card> getCard() {
		return card;
	}
	
	/**
	 * Gets whether this warmup is yet to be completed/interrupted.
	 *
	 * @return whether this warmup is still happening
	 */
	public boolean isActive() {
		return active;
	}
	
	/**
	 * Gets the amount of ticks that elapsed since the creation of this warmup.
	 * This counter is incremented before any callbacks are called.
	 *
	 * @return the count of ticks that elapsed since creation
	 */
	public int getElapsedTicks() {
		return elapsedTicks;
	}
	
	boolean wasCreatedFrom(ActionWarmupTaskTemplate template) {
		return this.template == template;
	}
	
	void tick() {
		Validate.isTrue(active);
		elapsedTicks++;
		for(TickHandler handler : handlers) {
			handler.tick(this);
		}
	}
	
	boolean tryComplete() {
		Validate.isTrue(active);
		if(isComplete.apply(this)) {
			active = false;
			if(registeredMovement != null) {
				plugin.getMoveCallbackManager().unregister(player, registeredMovement);
			}
			if(onCompleted != null) {
				try {
					onCompleted.onCompleted(this);
				} catch (Throwable t) {
					plugin.getLogger().log(Level.SEVERE, "Error handling completed callback in "
							+ getClass().getSimpleName(), t);
				}
			}
			return true;
		} else {
			return false;
		}
	}
	
	void interrupt(ActionWarmupTaskTemplate.InterruptReason reason) {
		Validate.isTrue(active);
		active = false;
		if(registeredMovement != null) {
			plugin.getMoveCallbackManager().tryUnregister(player, registeredMovement);
		}
		if(onInterrupted != null) {
			try {
				onInterrupted.onInterrupted(this, reason);
			} catch (Throwable t) {
				plugin.getLogger().log(Level.SEVERE, "Error handling interrupted callback in "
						+ getClass().getSimpleName(), t);
			}
		}
	}
	
	static final class TickHandler {
		private final JavaPlugin plugin;
		private final ActionWarmupTaskTemplate.TickHandler handler;
		private final int intervalTicks;
		private int remainingDelayTicks;
		private int elapsedTicks;
		
		TickHandler(JavaPlugin plugin, ActionWarmupTaskTemplate.TickHandler handler, int intervalTicks, int delayTicks) {
			this.plugin = plugin;
			this.handler = handler;
			this.intervalTicks = intervalTicks;
			remainingDelayTicks = delayTicks;
		}
		
		void tick(ActionWarmupTask task) {
			if(remainingDelayTicks > 0) {
				remainingDelayTicks--;
			} else {
				if(elapsedTicks % intervalTicks == 0) {
					try {
						handler.onTick(task, elapsedTicks / intervalTicks, elapsedTicks);
					} catch (Throwable t) {
						plugin.getLogger().log(Level.SEVERE, "Error calling tick handler in "
								+ ActionWarmupTask.class.getSimpleName(), t);
					}
				}
				elapsedTicks++;
			}
		}
	}
}
