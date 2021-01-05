package com.knoban.ultimates.cards.impl;

import com.knoban.ultimates.Ultimates;
import com.knoban.ultimates.cards.Card;
import com.knoban.ultimates.cards.CardInfo;
import com.knoban.ultimates.primal.PrimalSource;
import com.knoban.ultimates.primal.Tier;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Item;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

@CardInfo(
		material = Material.IRON_NUGGET,
		name = "parley",
		display = "§9Parley", // Typically we want the color to match the Primal
		description = {"§7You drop items to avoid", "§7dying during PvP combat."},
		source = PrimalSource.NONE,
		tier = Tier.COMMON
)
public class ParleyCard extends Card {
	//value: amount divided by max stack size
	private static final double BASE_VALUE = 5; //always drop this much value
	private static final double VALUE_PER_DAMAGE = 0.5; //drop this much value for each damage point
	private static final int ITEM_PICKUP_DELAY_TICKS = 40;
	private static final List<Integer> INVENTORY_SLOTS = IntStream.range(0, 36)
			.boxed().collect(Collectors.toList());
	
	public ParleyCard(Ultimates plugin) {
		super(plugin);
	}
	
	//high priority: we read and modify the damage amount
	@EventHandler(ignoreCancelled = true, priority = EventPriority.NORMAL)
	private void onDamage(EntityDamageByEntityEvent event) {
		if(event.getEntityType() != EntityType.PLAYER) {
			return;
		}
		
		Player victim = (Player) event.getEntity();
		if(!drawn.contains(victim)) {
			return;
		}
		
		if(victim.getHealth() > event.getFinalDamage()) {
			return; //we only care about final blows
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
		
		if(damager == null || damager == victim) {
			return; //we only care about PvP
		}
		
		double targetValue = BASE_VALUE + event.getFinalDamage() * VALUE_PER_DAMAGE;
		if(targetValue <= 0) {
			return; //negative damage? incorrect card configuration?
		}
		
		PlayerInventory inventory = victim.getInventory();
		World world = victim.getWorld();
		Location location = victim.getLocation();
		
		double droppedValue = 0;
		Collections.shuffle(INVENTORY_SLOTS);
		for(int slot : INVENTORY_SLOTS) {
			ItemStack item = inventory.getItem(slot);
			if(item == null || item.getType() == Material.AIR
					|| item.getAmount() == 0 || item.getMaxStackSize() == 0) {
				continue;
			}
			
			Item itemEntity = world.dropItemNaturally(location, item);
			//itemEntity.setCanMobPickup(false); //should we disallow this? any reason to?
			itemEntity.setThrower(victim.getUniqueId());
			itemEntity.setPickupDelay(ITEM_PICKUP_DELAY_TICKS);
			
			if(!new PlayerDropItemEvent(victim, itemEntity).callEvent()) {
				itemEntity.remove();
				continue;
			}
			
			inventory.setItem(slot, null);
			droppedValue += (double) item.getAmount() / item.getType().getMaxStackSize();
			if(droppedValue >= targetValue) {
				break;
			}
		}
		
		if(droppedValue >= targetValue) {
			//we dropped the required amount of items
			event.setDamage(0);
			victim.sendMessage(info.display() + "§e!");
			victim.getWorld().playSound(victim.getLocation(), Sound.ENTITY_IRON_GOLEM_DAMAGE, 1, 1.5f);
		} else {
			//we dropped as much as we could, so let's decrease the damage
			//it may or may not be enough to stop the player from being killed
			double progress = droppedValue / targetValue;
			event.setDamage(event.getDamage() * (1 - progress));
		}
	}
}
