package com.knoban.ultimates.cards.impl;

import com.knoban.ultimates.Ultimates;
import com.knoban.ultimates.cards.CardInfo;
import com.knoban.ultimates.cards.base.PermanentPotionEffectCard;
import com.knoban.ultimates.events.CombatEnterEvent;
import com.knoban.ultimates.events.CombatLeaveEvent;
import com.knoban.ultimates.primal.PrimalSource;
import com.knoban.ultimates.primal.Tier;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.inventory.meta.PotionMeta;
import org.bukkit.potion.PotionData;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.potion.PotionType;

@CardInfo(
		material = Material.POTION,
		name = "druid-heart",
		display = "§9Druid Heart", // Typically we want the color to match the Primal
		description = {"§7Gain §dRegeneration I §7whenever", "§7you are out of combat."},
		source = PrimalSource.NONE,
		tier = Tier.COMMON
)
public class OOCRegenerationCard extends PermanentPotionEffectCard {
	
	public OOCRegenerationCard(Ultimates plugin) {
		super(plugin, PotionEffectType.REGENERATION, 0);
	}
	
	@Override
	public void cacheItemStacks() {
		super.cacheItemStacks();
		PotionData data = new PotionData(PotionType.REGEN);
		
		PotionMeta meta = (PotionMeta) unownedCantBuyIcon.getItemMeta();
		meta.setBasePotionData(data);
		unownedCantBuyIcon.setItemMeta(meta);
		
		meta = (PotionMeta) unownedCanBuyIcon.getItemMeta();
		meta.setBasePotionData(data);
		unownedCanBuyIcon.setItemMeta(meta);
		
		meta = (PotionMeta) unownedIcon.getItemMeta();
		meta.setBasePotionData(data);
		unownedIcon.setItemMeta(meta);
		
		meta = (PotionMeta) ownedIcon.getItemMeta();
		meta.setBasePotionData(data);
		ownedIcon.setItemMeta(meta);
		
		meta = (PotionMeta) drawnIcon.getItemMeta();
		meta.setBasePotionData(data);
		drawnIcon.setItemMeta(meta);
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
	
	@Override
	protected boolean shouldHaveEffect(Player player) {
		return !plugin.getCombatManager().isInCombat(player);
	}
	
	@EventHandler
	public void onEnterCombat(CombatEnterEvent event) {
		if(drawn.contains(event.getPlayer())) {
			tryRemoveEffect(event.getPlayer());
		}
	}
	
	@EventHandler
	public void onLeaveCombat(CombatLeaveEvent event) {
		if(event.getReason() == CombatLeaveEvent.Reason.TIMEOUT && drawn.contains(event.getPlayer())) {
			//Reason.TIMEOUT: don't try to add the effect if the player quit or died
			tryAddEffect(event.getPlayer());
		}
	}
}
