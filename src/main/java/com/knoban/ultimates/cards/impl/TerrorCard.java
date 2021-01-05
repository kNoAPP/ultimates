package com.knoban.ultimates.cards.impl;

import com.knoban.ultimates.Ultimates;
import com.knoban.ultimates.cards.Card;
import com.knoban.ultimates.cards.CardInfo;
import com.knoban.ultimates.primal.PrimalSource;
import com.knoban.ultimates.primal.Tier;
import net.minecraft.server.v1_16_R3.EntityPlayer;
import net.minecraft.server.v1_16_R3.EntityVillager;
import net.minecraft.server.v1_16_R3.MemoryModuleType;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.craftbukkit.v1_16_R3.entity.CraftPlayer;
import org.bukkit.craftbukkit.v1_16_R3.entity.CraftVillager;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Item;
import org.bukkit.entity.Player;
import org.bukkit.entity.Villager;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.inventory.MerchantRecipe;

import java.util.Collection;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

@CardInfo(
		material = Material.WITHER_SKELETON_SKULL,
		name = "terror",
		display = "§8Terror", // Typically we want the color to match the Primal
		description = {"§2Slaying §7a villager", "§cscares §7nearby ones."},
		source = PrimalSource.DARK,
		tier = Tier.EPIC
)
public class TerrorCard extends Card {
	private static final double RANGE_HORIZONTAL = 8;
	private static final double RANGE_VERTICAL = 4;
	private static final double THROWN_ITEM_VELOCITY = 0.15;
	
	public TerrorCard(Ultimates plugin) {
		super(plugin);
	}
	
	@EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
	public void onVillagerKilled(EntityDeathEvent event) {
		if (event.getEntityType() != EntityType.VILLAGER) {
			return;
		}
		
		Player player = event.getEntity().getKiller();
		if (player == null || !drawn.contains(player)) {
			return;
		}
		
		Villager victim = (Villager) event.getEntity();
		Location center = victim.getLocation().add(0, victim.getEyeHeight() / 2, 0);
		
		Collection<Villager> nearby = center.getNearbyEntitiesByType(Villager.class, RANGE_HORIZONTAL,
				RANGE_VERTICAL, RANGE_HORIZONTAL, e -> canAffectVillager(player, e));
		
		for (Villager villager : nearby) {
			villager.getWorld().playSound(victim.getEyeLocation(), Sound.ENTITY_VILLAGER_HURT, 1, 1);
			dropRandomTradeGood(villager);
			runAway(villager, player);
		}
	}
	
	private boolean canAffectVillager(Player player, Villager villager) {
		//filter out custom NPCs
		if (!villager.isValid() || villager.isInvulnerable()
				|| !villager.isCollidable() || !villager.hasAI()) {
			return false;
		}
		
		EntityDamageByEntityEvent event = new EntityDamageByEntityEvent(player,
				villager, EntityDamageEvent.DamageCause.CUSTOM, 0);
		Bukkit.getPluginManager().callEvent(event);
		return !event.isCancelled();
	}
	
	private void dropRandomTradeGood(Villager villager) {
		List<MerchantRecipe> recipes = villager.getRecipes();
		if (recipes.isEmpty()) {
			return;
		}
		
		MerchantRecipe recipe = recipes.get(ThreadLocalRandom.current().nextInt(recipes.size()));
		Location location = villager.getEyeLocation();
		Item item = villager.getWorld().dropItem(location, recipe.getResult());
		item.setVelocity(location.getDirection().normalize().multiply(THROWN_ITEM_VELOCITY));
	}
	
	private void runAway(Villager villager, Player player) {
		EntityVillager nmsVillager = ((CraftVillager) villager).getHandle();
		EntityPlayer nmsPlayer = ((CraftPlayer) player).getHandle();
		nmsVillager.getBehaviorController().setMemory(MemoryModuleType.NEAREST_HOSTILE, nmsPlayer);
	}
}
