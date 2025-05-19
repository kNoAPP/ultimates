package com.knoban.ultimates.cards.impl;

import com.knoban.ultimates.Ultimates;
import com.knoban.ultimates.cards.CardInfo;
import com.knoban.ultimates.cards.base.PermanentPotionEffectCard;
import com.knoban.ultimates.primal.PrimalSource;
import com.knoban.ultimates.primal.Tier;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.meta.PotionMeta;
import org.bukkit.potion.PotionData;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.potion.PotionType;

@CardInfo(
		material = Material.POTION,
		name = "running-shoes",
		display = "§9Running Shoes", // Typically we want the color to match the Primal
		description = {"§7You get permanent §eSpeed I§7!"},
		source = PrimalSource.NONE,
		tier = Tier.COMMON
)
public class SpeedCard extends PermanentPotionEffectCard {
	
	public SpeedCard(Ultimates plugin) {
		super(plugin, PotionEffectType.SPEED, 0);
	}
	
	@Override
	public void cacheItemStacks() {
		super.cacheItemStacks();
		PotionData data = new PotionData(PotionType.SWIFTNESS);
		
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
	protected boolean shouldHaveEffect(Player player) {
		return true;
	}
}
