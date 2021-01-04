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
		if (didEquip) {
			p.addPotionEffect(effect); //this will fail if the player already has a better version of the potion effect
		}
		return didEquip;
	}
	
	@Override
	public boolean discard(Player p) {
		boolean didDispose = super.discard(p);
		if (didDispose) {
			PotionEffect current = p.getPotionEffect(effect.getType());
			if (current != null && current.getAmplifier() == effect.getAmplifier()) { //don't remove effects not from this card
				p.removePotionEffect(effect.getType());
			}
		}
		return didDispose;
	}
	
	@EventHandler(ignoreCancelled = true, priority = EventPriority.HIGHEST)
	public void onEffectRemoved(EntityPotionEffectEvent event) {
		if (!(event.getEntity() instanceof Player)
				|| event.getOldEffect() == null
				|| !event.getOldEffect().getType().equals(effect.getType())) {
			return; //we only care about players and this effect type
		}
		
		if (event.getAction() != EntityPotionEffectEvent.Action.CLEARED
				&& event.getAction() != EntityPotionEffectEvent.Action.REMOVED) {
			return; //we don't care about effects being added or being upgraded
		}
		
		if (event.getCause() != EntityPotionEffectEvent.Cause.DEATH
				&& event.getCause() != EntityPotionEffectEvent.Cause.MILK
				&& event.getCause() != EntityPotionEffectEvent.Cause.EXPIRATION) {
			return; //if a plugin/command tried to clear the effects, then let that happen
		}
		
		Player player = (Player) event.getEntity();
		if (!drawn.contains(player)) {
			return;
		}
		
		PotionEffect oldEffect = event.getOldEffect();
		if (oldEffect.getAmplifier() == effect.getAmplifier()) { //the target effect was removed
			event.setCancelled(true);
		} else { //a better effect was removed
			Bukkit.getScheduler().runTask(plugin, () -> { //we can't add effects during this event
				if (Bukkit.getPlayer(player.getUniqueId()) == player) {
					player.addPotionEffect(effect);
				}
			});
		}
	}
}
