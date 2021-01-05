package com.knoban.ultimates.aspects.warmup;

import com.knoban.ultimates.Ultimates;
import com.knoban.ultimates.aspects.MoveCallbackManager;
import com.knoban.ultimates.cards.Card;
import org.apache.commons.lang.Validate;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collectors;

/**
 * A template based on which {@link ActionWarmupTask} instances can be created using {@link ActionWarmupManager}.
 * Instances are mutable, but changes are not visible in already created {@link ActionWarmupTask} instances.
 */
public final class ActionWarmupTaskTemplate {
	private final List<RegisteredTickHandler> handlers = new ArrayList<>();
	private final Function<ActionWarmupTask, Boolean> isComplete;
	private Supplier<?> companionConstructor;
	private Movement movement = Movement.DISALLOW;
	private Combat combat = Combat.DISALLOW;
	private Class<? extends Card> card;
	private CompletedCallback onCompleted;
	private InterruptedCallback onInterrupted;
	
	private ActionWarmupTaskTemplate(Function<ActionWarmupTask, Boolean> isComplete) {
		this.isComplete = isComplete;
	}
	
	/**
	 * Creates a new instance.
	 *
	 * @param isComplete the function that determines whether the warmup is over
	 * @return the newly created template
	 */
	public static ActionWarmupTaskTemplate create(Function<ActionWarmupTask, Boolean> isComplete) {
		return new ActionWarmupTaskTemplate(isComplete);
	}
	
	/**
	 * Creates a new instance.
	 *
	 * @param lengthTicks the length of the warmup, in ticks (must be positive)
	 * @return the newly created template
	 */
	public static ActionWarmupTaskTemplate createTimed(long lengthTicks) {
		Validate.isTrue(lengthTicks > 0, "Tick length must be positive");
		return create(task -> task.getElapsedTicks() >= lengthTicks);
	}
	
	/**
	 * Creates a new instance.
	 *
	 * @param minLengthTicks the minimum length of the warmup, in ticks (must be positive)
	 * @param isComplete the function that determines whether the warmup is over
	 * (only called after at least {@code minLengthTicks} ticks have elapsed)
	 * @return the newly created template
	 */
	public static ActionWarmupTaskTemplate createMinTimed(long minLengthTicks, Function<ActionWarmupTask, Boolean> isComplete) {
		Validate.isTrue(minLengthTicks > 0, "Tick length must be positive");
		return create(task -> task.getElapsedTicks() >= minLengthTicks && isComplete.apply(task));
	}
	
	/**
	 * Sets a constructor for companion objects.
	 * These objects can be used to store data related to the warmups.
	 *
	 * @param companionConstructor the constructor of companion objects
	 * @return the current instance (for chaining)
	 */
	public ActionWarmupTaskTemplate setCompanionConstructor(Supplier<?> companionConstructor) {
		this.companionConstructor = companionConstructor;
		return this;
	}
	
	/**
	 * Sets the policy towards movement during the warmup.
	 *
	 * @param movement the new movement policy
	 * @return the current instance (for chaining)
	 */
	public ActionWarmupTaskTemplate setMovement(Movement movement) {
		this.movement = movement;
		return this;
	}
	
	/**
	 * Sets the policy towards combat during the warmup.
	 *
	 * @param combat the new combat policy
	 * @return the current instance (for chaining)
	 */
	public ActionWarmupTaskTemplate setCombat(Combat combat) {
		this.combat = combat;
		return this;
	}
	
	/**
	 * Sets the card linked to the warmup.
	 * Upon discarding the card, the warmup will be interrupted.
	 *
	 * @param card the card to link
	 * @return the current instance (for chaining)
	 */
	public ActionWarmupTaskTemplate setCard(Class<? extends Card> card) {
		this.card = card;
		return this;
	}
	
	/**
	 * Adds a tick handler: a callback that's called every X ticks.
	 * These callbacks are called before {@link #setOnCompleted(CompletedCallback)}
	 * if both get called in the same tick.
	 *
	 * @param handler the callback to add
	 * @param intervalTicks how often the callback should be called (must be positive)
	 * @param delayTicks by how many ticks the first call should be delayed by (mustn't be negative)
	 * @return the current instance (for chaining)
	 */
	public ActionWarmupTaskTemplate addTickHandler(TickHandler handler, int intervalTicks, int delayTicks) {
		Validate.isTrue(intervalTicks > 0, "Interval ticks must be positive");
		Validate.isTrue(delayTicks >= 0, "Delay ticks mustn't be negative");
		handlers.add(new RegisteredTickHandler(handler, intervalTicks, delayTicks));
		return this;
	}
	
	/**
	 * Sets the callback to execute when the warmup is completed.
	 *
	 * @param onCompleted the callback
	 * @return the current instance (for chaining)
	 */
	public ActionWarmupTaskTemplate setOnCompleted(CompletedCallback onCompleted) {
		this.onCompleted = onCompleted;
		return this;
	}
	
	/**
	 * Sets the callback to execute when the warmup is interrupted.
	 *
	 * @param onInterrupted the callback
	 * @return the current instance (for chaining)
	 */
	public ActionWarmupTaskTemplate setOnInterrupted(InterruptedCallback onInterrupted) {
		this.onInterrupted = onInterrupted;
		return this;
	}
	
	/**
	 * Gets the policy towards movement during the warmup.
	 *
	 * @return the movement policy
	 */
	public Movement getMovement() {
		return movement;
	}
	
	/**
	 * Gets the policy towards combat during the warmup.
	 *
	 * @return the combat policy
	 */
	public Combat getCombat() {
		return combat;
	}
	
	ActionWarmupTask instantiate(Ultimates plugin, Player player, MoveCallbackManager.RegisteredCallback registeredMovement) {
		return new ActionWarmupTask(plugin, player, this,
				handlers.stream().map(h -> h.instantiate(plugin)).collect(Collectors.toList()),
				isComplete, companionConstructor == null ? null : companionConstructor.get(),
				movement, combat, card,
				onCompleted, onInterrupted,
				registeredMovement);
	}
	
	public enum Movement {
		DISALLOW,
		ALLOW_VERTICAL,
		ALLOW_ALL;
	}
	
	public enum Combat {
		DISALLOW,
		ALLOW
	}
	
	@FunctionalInterface
	public interface TickHandler {
		void onTick(ActionWarmupTask task, int elapsedCount, int elapsedTicks);
	}
	
	@FunctionalInterface
	public interface CompletedCallback {
		void onCompleted(ActionWarmupTask task);
	}
	
	@FunctionalInterface
	public interface InterruptedCallback {
		void onInterrupted(ActionWarmupTask task, InterruptReason reason);
	}
	
	public enum InterruptReason {
		MOVEMENT,
		COMBAT,
		DISCARDED_CARD,
		DEATH,
		LOGOUT,
		PLUGIN_CODE;
		
		public boolean shouldAnnounceInterruption() {
			return this == MOVEMENT || this == COMBAT;
		}
	}
	
	final class RegisteredTickHandler {
		private final TickHandler handler;
		private final int intervalTicks;
		private final int delayTicks;
		
		RegisteredTickHandler(TickHandler handler, int intervalTicks, int delayTicks) {
			this.handler = handler;
			this.intervalTicks = intervalTicks;
			this.delayTicks = delayTicks;
		}
		
		ActionWarmupTask.TickHandler instantiate(JavaPlugin plugin) {
			return new ActionWarmupTask.TickHandler(plugin, handler, intervalTicks, delayTicks);
		}
	}
}
