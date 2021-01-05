package com.knoban.ultimates.aspects;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.knoban.ultimates.events.CombatEnterEvent;
import com.knoban.ultimates.events.CombatLeaveEvent;
import org.bukkit.Bukkit;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.*;
import java.util.concurrent.TimeUnit;

import static org.bukkit.event.entity.EntityDamageEvent.DamageCause.*;

/**
 * Class dedicated to keeping track of player "in-combat" status.
 * Please note the existence of {@link CombatEnterEvent} and {@link CombatLeaveEvent}.
 * <br><br>
 * A few important things to keep in mind:
 * <ul>
 *     <li>Some stuff put the player into combat, some only reset the countdown: the player already has to be in combat.</li>
 *     <li>This class depends on the current time, but this time is cached internally and doesn't change during ticks.
 *     This means that eg. {@link #getRemainingCombatTimeMillis(Player)} will return the same value for entire ticks.</li>
 *     <li>Players aren't in combat during {@link EntityDamageEvent}s, since the event could still get cancelled.
 *     More precisely, players enter combat during {@link EventPriority#HIGHEST}.</li>
 *     <li>The in-combat state cannot be cleared by logging out and logging back it. It persists for a few hours.
 *     {@link CombatLeaveEvent.Reason#QUIT_PAUSE} and {@link CombatEnterEvent.Reason#LOGIN_RESUME} exist for this reason.</li>
 *     <li>Server shutdown is as if {@link PlayerQuitEvent} was fired for each player.</li>
 * </ul>
 */
public class CombatStateManager implements Listener {
	private static final Set<EntityDamageEvent.DamageCause> START_CAUSES = EnumSet.of(BLOCK_EXPLOSION, CONTACT,
			CUSTOM, DRAGON_BREATH, ENTITY_ATTACK, ENTITY_EXPLOSION, ENTITY_SWEEP_ATTACK,
			FALLING_BLOCK, FIRE_TICK, LIGHTNING, MAGIC, POISON, PROJECTILE, THORNS, WITHER);
	private static final Set<EntityDamageEvent.DamageCause> LENGTHEN_CAUSES = EnumSet.of(DROWNING, FALL,
			FIRE, HOT_FLOOR, LAVA, SUFFOCATION);
	//Notable excludes from both: FLY_INTO_WALL, STARVATION
	private final Map<Player, Entry> online = new HashMap<>();
	private final PriorityQueue<Entry> entries = new PriorityQueue<>(Comparator.comparing(e -> e.until));
	private final Cache<UUID, Long> offline = CacheBuilder.newBuilder().expireAfterWrite(3, TimeUnit.HOURS)
			.build(); //expire: so we don't have a memory leak (and we don't persist through restarts -> would be unreliable)
	private final long inCombatLengthMillis;
	private long currentTimestamp; //so that queries during a single tick return the same value
	
	/**
	 * Creates and initializes a new instance.
	 *
	 * @param plugin the plugin instance
	 * @param inCombatLengthMillis how long the "in-combat" status should last, in milliseconds
	 */
	public CombatStateManager(JavaPlugin plugin, long inCombatLengthMillis) {
		this.inCombatLengthMillis = inCombatLengthMillis;
		Bukkit.getPluginManager().registerEvents(this, plugin);
		
		currentTimestamp = System.currentTimeMillis();
		Bukkit.getScheduler().runTaskTimer(plugin, () -> {
			currentTimestamp = System.currentTimeMillis();
			
			for(Iterator<Entry> iterator = entries.iterator(); iterator.hasNext(); ) {
				Entry entry = iterator.next();
				if(entry.until <= currentTimestamp) {
					iterator.remove();
					online.remove(entry.player);
					new CombatLeaveEvent(entry.player, CombatLeaveEvent.Reason.TIMEOUT).callEvent();
				} else {
					break; //PriorityQueue is ordered
				}
			}
		}, 0, 1);
	}
	
	/**
	 * Safely shuts this system down.
	 */
	public void shutdown() {
		for(Player player : new ArrayList<>(online.keySet())) {
			onQuit(new PlayerQuitEvent(player, null));
		}
	}
	
	/**
	 * Gets whether a {@link EntityDamageEvent.DamageCause} is one
	 * that would start/lengthen a player combat status.
	 *
	 * @param cause the cause to check
	 * @param includeLengthen whether to include causes that
	 * don't start a combat status, only lengthen already existing statuses.
	 * @return whether the cause would start/lengthen a player combat status
	 */
	public boolean isCombatCause(EntityDamageEvent.DamageCause cause, boolean includeLengthen) {
		return includeLengthen
				? START_CAUSES.contains(cause) || LENGTHEN_CAUSES.contains(cause)
				: START_CAUSES.contains(cause);
	}
	
	/**
	 * Gets how long the in-combat status lasts after start, in milliseconds.
	 *
	 * @return value of {@link #getRemainingCombatTimeMillis(Player)} during {@link CombatEnterEvent}
	 */
	public long getInCombatLengthMillis() {
		return inCombatLengthMillis;
	}
	
	/**
	 * Gets whether the specified player is currently in combat or not.
	 *
	 * @param player the player to check
	 * @return whether the player is in combat
	 */
	public boolean isInCombat(Player player) {
		return online.containsKey(player);
	}
	
	/**
	 * Gets how much more time the specified player will be in combat for.
	 * Guaranteed to return 0 in case the player isn't in combat.
	 * Guaranteed to return a positive value in case the player is in combat.
	 * Please considering rounding up in case this value is to be truncated.
	 *
	 * @param player the player to check
	 * @return how much more time the player will be in combat for
	 * @see #getRemainingCombatTimeMillis(Player)
	 */
	public int getRemainingCombatTimeSeconds(Player player) {
		return (int) (getRemainingCombatTimeMillis(player) + 19) / 20;
	}
	
	/**
	 * Gets how much more time the specified player will be in combat for.
	 * Guaranteed to return 0 in case the player isn't in combat.
	 * Guaranteed to return a positive value in case the player is in combat.
	 * Please considering rounding up in case this value is to be truncated.
	 *
	 * @param player he player to check
	 * @return how much more time the player will be in combat for
	 * @see #getRemainingCombatTimeSeconds(Player)
	 */
	public long getRemainingCombatTimeMillis(Player player) {
		Entry entry = online.get(player);
		return entry == null ? 0 : entry.until - currentTimestamp;
	}
	
	//Highest priority so that we don't act when the event gets cancelled
	@EventHandler(ignoreCancelled = true, priority = EventPriority.HIGHEST)
	private void onTakeDamage(EntityDamageEvent event) {
		if(event.getEntityType() != EntityType.PLAYER) {
			return;
		}
		
		boolean start = START_CAUSES.contains(event.getCause());
		if(start || LENGTHEN_CAUSES.contains(event.getCause())) {
			tryEnterCombat((Player) event.getEntity(), !start, CombatEnterEvent.Reason.TAKE_DAMAGE);
		}
	}
	
	//Highest priority so that we don't act when the event gets cancelled
	@EventHandler(ignoreCancelled = true, priority = EventPriority.HIGHEST)
	private void onDealDamage(EntityDamageByEntityEvent event) {
		/*if(!(event.getEntity() instanceof LivingEntity)) {
			return; //in case this event gets called for paintings, end crystals, fireballs
		}*/
		
		if(event.getEntityType() != EntityType.PLAYER) {
			return; //only damaging players should put player in combat
		}
		
		boolean start = START_CAUSES.contains(event.getCause());
		if(!start && !LENGTHEN_CAUSES.contains(event.getCause())) {
			return;
		}
		
		Player damager = null;
		if(event.getDamager() instanceof Player) {
			damager = (Player) event.getDamager();
		} else if(event.getDamager() instanceof Projectile) {
			Projectile projectile = (Projectile) event.getDamager();
			if(projectile.getShooter() instanceof Player) {
				damager = (Player) projectile.getShooter();
			}
		}
		
		if(damager != null) {
			tryEnterCombat(damager, !start, CombatEnterEvent.Reason.DEAL_DAMAGE);
		}
	}
	
	//Highest priority so that the player stays in combat as long as possible
	@EventHandler(ignoreCancelled = true, priority = EventPriority.HIGHEST)
	private void onDeath(PlayerDeathEvent event) {
		tryLeaveCombat(event.getEntity(), CombatLeaveEvent.Reason.DEATH);
	}
	
	//Highest priority so that the player stays in combat as long as possible
	@EventHandler(ignoreCancelled = true, priority = EventPriority.HIGHEST)
	private void onQuit(PlayerQuitEvent event) {
		Entry entry = tryLeaveCombat(event.getPlayer(), CombatLeaveEvent.Reason.QUIT_PAUSE);
		if(entry != null) {
			offline.put(event.getPlayer().getUniqueId(), entry.until - currentTimestamp);
		}
	}
	
	//Lowest priority so that the player re-enters combat as long as possible
	@EventHandler(ignoreCancelled = true, priority = EventPriority.LOWEST)
	private void onLogin(PlayerJoinEvent event) {
		Player player = event.getPlayer();
		if(isInCombat(player)) {
			offline.invalidate(player.getUniqueId());
			return; //if player is already in combat, our action is pointless
		}
		
		Long remaining = offline.getIfPresent(player.getUniqueId());
		if(remaining == null) {
			return;
		}
		
		offline.invalidate(player.getUniqueId());
		Entry newEntry = new Entry(player, currentTimestamp + remaining);
		online.put(player, newEntry);
		entries.add(newEntry);
		new CombatEnterEvent(player, CombatEnterEvent.Reason.LOGIN_RESUME).callEvent();
	}
	
	private void tryEnterCombat(Player player, boolean onlyLengthen, CombatEnterEvent.Reason reason) {
		if(onlyLengthen && !online.containsKey(player)) {
			return;
		}
		
		Entry newEntry = new Entry(player, currentTimestamp + inCombatLengthMillis);
		Entry oldEntry = online.put(player, newEntry);
		if(oldEntry != null) {
			entries.remove(oldEntry);
		}
		entries.add(newEntry);
		
		if(oldEntry == null) {
			new CombatEnterEvent(player, reason).callEvent();
		}
	}
	
	private Entry tryLeaveCombat(Player player, CombatLeaveEvent.Reason reason) {
		Entry entry = online.remove(player);
		if(entry != null) {
			entries.remove(entry);
			new CombatLeaveEvent(player, reason).callEvent();
		}
		return entry;
	}
	
	private static class Entry {
		final Player player;
		final long until;
		
		Entry(Player player, long until) {
			this.player = player;
			this.until = until;
		}
	}
}
