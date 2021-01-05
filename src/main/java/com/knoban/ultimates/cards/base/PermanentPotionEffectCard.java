package com.knoban.ultimates.cards.base;

import com.knoban.ultimates.Ultimates;
import com.knoban.ultimates.cards.Card;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.entity.EntityPotionEffectEvent;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

/**
 * Base class for {@link Card}s that give a permanent potion effect.
 * Effects that shouldn't always be added are also supported through
 * {@link #shouldHaveEffect(Player)}, {@link #tryAddEffect(Player)}
 * and {@link #tryRemoveEffect(Player)}.
 * This class automatically handles drawing, discarding, login, logout, death,
 * drinking milk, drinking of stronger potions, expiration of stronger potions.
 */
public abstract class PermanentPotionEffectCard extends Card {
	private final PotionEffect effect;
	
	protected PermanentPotionEffectCard(Ultimates plugin, PotionEffectType type, int amplifier) {
		this(plugin, type, amplifier, true, true, true);
	}
	
	protected PermanentPotionEffectCard(Ultimates plugin, PotionEffectType type, int amplifier,
			boolean ambient, boolean particles, boolean icon) {
		super(plugin);
		effect = new PotionEffect(type, Integer.MAX_VALUE, amplifier, ambient, particles, icon);
	}
	
	@Override
	public boolean draw(Player p) {
		boolean didEquip = super.draw(p);
		if(didEquip) {
			tryAddEffect(p);
		}
		return didEquip;
	}
	
	@Override
	public boolean discard(Player p) {
		boolean didDispose = super.discard(p);
		if(didDispose) {
			tryRemoveEffect(p);
		}
		return didDispose;
	}
	
	/**
	 * Should only be used internally.
	 * Gets whether a specific player should have the effect.
	 *
	 * @param player the player to check
	 * @return whether the player should have the effect
	 */
	protected abstract boolean shouldHaveEffect(Player player);
	
	/**
	 * Attempts to give the specified player the effect.
	 * {@link #shouldHaveEffect(Player)} is called internally.
	 *
	 * @param player the player to give the effect to
	 */
	public final void tryAddEffect(Player player) {
		if(shouldHaveEffect(player)) {
			player.addPotionEffect(effect);
		}
	}
	
	/**
	 * Attempts to remove the effect from the specified player.
	 * If the player has a better effect, then that better effect is kept.
	 *
	 * @param player the player to remove the effect from
	 */
	public final void tryRemoveEffect(Player player) {
		PotionEffect current = player.getPotionEffect(effect.getType());
		if(current == null) {
			return;
		}
		
		//can have multiple effects of the same type, but that's not really exposed in the API
		player.removePotionEffect(effect.getType());
		if(!isOurEffect(current)) {
			player.addPotionEffect(current);
		}
	}
	
	@EventHandler(ignoreCancelled = true, priority = EventPriority.HIGHEST)
	public final void onEffectRemoved(EntityPotionEffectEvent event) {
		if(!(event.getEntity() instanceof Player)
				|| event.getOldEffect() == null
				|| !event.getOldEffect().getType().equals(effect.getType())) {
			return; //we only care about players and this effect type
		}
		
		if(event.getAction() != EntityPotionEffectEvent.Action.CLEARED
				&& event.getAction() != EntityPotionEffectEvent.Action.REMOVED) {
			return; //we don't care about effects being added or being upgraded
		}
		
		if(event.getCause() != EntityPotionEffectEvent.Cause.DEATH
				&& event.getCause() != EntityPotionEffectEvent.Cause.MILK
				&& event.getCause() != EntityPotionEffectEvent.Cause.EXPIRATION) {
			return; //if a plugin/command tried to clear the effects, then let that happen
		}
		
		Player player = (Player) event.getEntity();
		if(!drawn.contains(player)) {
			return;
		}
		
		PotionEffect oldEffect = event.getOldEffect();
		if(isOurEffect(oldEffect)) { //the target effect was removed
			if(shouldHaveEffect(player)) {
				event.setCancelled(true);
			}
		} else { //a better effect was removed
			Bukkit.getScheduler().runTask(plugin, () -> { //we can't add effects during this event
				if(Bukkit.getPlayer(player.getUniqueId()) == player && drawn.contains(player)) {
					if(shouldHaveEffect(player)) {
						player.addPotionEffect(effect);
					}
				}
			});
		}
	}
	
	private boolean isOurEffect(PotionEffect other) {
		return other.getType().equals(effect.getType())
				&& other.getAmplifier() == effect.getAmplifier()
				&& other.isAmbient() == effect.isAmbient()
				&& other.hasParticles() == effect.hasParticles()
				&& other.hasIcon() == effect.hasIcon()
				&& other.getDuration() > Integer.MAX_VALUE / 1000;
	}
}
