package com.knoban.ultimates.cards.impl;

import com.destroystokyo.paper.event.player.PlayerJumpEvent;
import com.destroystokyo.paper.event.player.PlayerPostRespawnEvent;
import com.knoban.ultimates.Ultimates;
import com.knoban.ultimates.cards.Card;
import com.knoban.ultimates.cards.CardInfo;
import com.knoban.ultimates.primal.PrimalSource;
import com.knoban.ultimates.primal.Tier;
import org.bukkit.*;
import org.bukkit.block.data.BlockData;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.block.Action;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;

@CardInfo(
		material = Material.ELYTRA,
		name = "falcon",
		display = "§bFalcon", // Typically we want the color to match the Primal
		description = {"§7You can §2fly §7for", "§7short periods."},
		source = PrimalSource.SKY,
		tier = Tier.LEGENDARY
)
public class FalconCard extends Card {
	//charge: available flight time in milliseconds
	private static final long MAX_CHARGE = TimeUnit.SECONDS.toMillis(10);
	private static final long CHARGE_GAIN_PER_SECOND = 50;
	private static final long MILLIS_PER_TICK = 50;
	private static final long MIN_CHARGE_FOR_FLIGHT = 500;
	private static final String PERSISTENCE_KEY = "charges";
	private static final BlockData PARTICLE_BLOCK_DATA = Material.SNOW.createBlockData();
	private final Map<UUID, Long> charges = new HashMap<>();
	private final Map<Player, Long> updated = new HashMap<>();
	private final Map<Player, Entry> activated = new HashMap<>();
	private final ItemStack activationItem;
	
	public FalconCard(Ultimates plugin) {
		super(plugin);
		activationItem = createActivationItem();
	}
	
	@Override
	public boolean draw(Player p) {
		boolean didEquip = super.draw(p);
		if(didEquip) {
			giveActivationItem(p);
			updated.put(p, System.currentTimeMillis());
			charges.merge(p.getUniqueId(), 0L, Long::sum);
		}
		return didEquip;
	}
	
	@Override
	public boolean discard(Player p) {
		boolean didDispose = super.discard(p);
		if(didDispose) {
			p.getInventory().removeItemAnySlot(activationItem);
			if(isActive(p)) {
				toggleActive(p);
			} else {
				updateCharge(p);
			}
			writeData(p.getUniqueId(), PERSISTENCE_KEY, charges.remove(p.getUniqueId()), null);
			updated.remove(p);
		}
		return didDispose;
	}
	
	@EventHandler(ignoreCancelled = true, priority = EventPriority.HIGHEST)
	public void onRespawn(PlayerPostRespawnEvent event) {
		Player player = event.getPlayer();
		if(drawn.contains(player) && !player.getInventory().contains(activationItem)) {
			giveActivationItem(player);
			updated.put(player, System.currentTimeMillis());
		}
	}
	
	@Override
	public void onPlayerData(Player p, Map<String, Object> data) {
		addCharge(p, ((Number) data.getOrDefault(PERSISTENCE_KEY, 0)).longValue());
	}
	
	@EventHandler(ignoreCancelled = true, priority = EventPriority.HIGHEST)
	public void onPlayerDied(PlayerDeathEvent event) {
		Player player = event.getEntity();
		if(drawn.contains(player)) {
			if(isActive(player)) {
				toggleActive(player);
			}
			charges.put(player.getUniqueId(), 0L);
			updated.remove(player);
		}
	}
	
	@EventHandler(priority = EventPriority.HIGHEST)
	public void onTryActivate(PlayerInteractEvent event) {
		if(!event.hasItem() || event.useItemInHand() == Event.Result.DENY || event.getAction() == Action.PHYSICAL
				|| !drawn.contains(event.getPlayer()) || !activationItem.isSimilar(event.getItem())) {
			return;
		}
		
		event.setUseItemInHand(Event.Result.DENY);
		event.setUseInteractedBlock(Event.Result.DENY);
		Player player = event.getPlayer();
		
		if(!isActive(player)) {
			updateCharge(player);
		}
		
		boolean leftClick = event.getAction() == Action.LEFT_CLICK_AIR || event.getAction() == Action.LEFT_CLICK_BLOCK;
		if(leftClick) { //left click -> show information
			long charge = charges.get(player.getUniqueId());
			String time = String.format("%.1f", (float) charge / 1000);
			player.sendMessage("§eYou have §6" + time + " seconds§e of " + info.display() + "§e.");
			return;
		}
		
		//right click -> toggle flight
		if(!isActive(player) && charges.get(player.getUniqueId()) < MIN_CHARGE_FOR_FLIGHT) {
			player.sendMessage("§cNot enough charge to activate " + info.display() + "§c.");
			return;
		}
		
		toggleActive(player);
		if(isActive(player)) {
			long charge = charges.get(player.getUniqueId());
			String time = String.format("%.1f", (float) charge / 1000);
			player.sendMessage("§eActivated " + info.display() + "§e, §6" + time + " seconds§e remaining.");
		} else {
			player.sendMessage("§eDeactivated " + info.display() + "§e.");
		}
	}
	
	@EventHandler(ignoreCancelled = true)
	public void onJump(PlayerJumpEvent event) {
		//necessary because player can't take fall damage while setAllowFlight(true)
		//issue with this: spacebar needs to be pressed 3 times instead of 2
		Player player = event.getPlayer();
		if(drawn.contains(player) && !player.isFlying()) {
			Entry entry = activated.get(player);
			if(entry != null) { //isActive, but more performant
				player.setAllowFlight(true);
				entry.allowFlightCountdown = 15; //this value was found to be right
				//too large -> player won't take fall damage from short falls
				//too little -> player doesn't have time to activate ability
			}
		}
	}
	
	private boolean isActive(Player player) {
		return activated.containsKey(player);
	}
	
	private void toggleActive(Player player) {
		Entry oldEntry = activated.remove(player);
		if(oldEntry != null) {
			//enabled -> disabled
			oldEntry.task.cancel();
			player.setFlying(false);
			if(player.getGameMode() != GameMode.CREATIVE) {
				player.setAllowFlight(false);
			}
			updated.put(player, System.currentTimeMillis());
			return;
		}
		
		//disabled -> enabled
		updated.remove(player);
		long[] lastTime = {System.currentTimeMillis()};
		int[] tick = new int[1];
		Entry newEntry = new Entry();
		activated.put(player, newEntry);
		newEntry.task = new BukkitRunnable() {
			@Override
			public void run() {
				long currentTime = System.currentTimeMillis();
				long deltaTime = currentTime - lastTime[0];
				lastTime[0] = currentTime;
				if(deltaTime <= 0) {
					return; //whatever happened here, let's just forget about it
				}
				
				if(!player.isFlying()) {
					if(newEntry.allowFlightCountdown > 0 && --newEntry.allowFlightCountdown == 0) {
						if(player.getGameMode() != GameMode.CREATIVE) {
							player.setAllowFlight(false);
						}
					}
					return; //only take charge away while the player is actually flying
					//we do not give charge during this period on purpose
				}
				
				long remaining = charges.get(player.getUniqueId());
				remaining -= deltaTime;
				
				if(remaining < 0) {
					remaining = 0; //misprediction, tick took more than MILLIS_PER_TICK
				}
				charges.put(player.getUniqueId(), remaining);
				
				if(remaining < MILLIS_PER_TICK) {
					toggleActive(player); //not enough charge for one more tick
					player.sendMessage(info.display() + "§c has ran out.");
					player.sendActionBar(info.display() + "§e: §60.0 seconds");
					return;
				}
				
				if(tick[0]++ % 5 == 0) { //every 5 ticks
					playEffects(player, remaining);
				}
				
				//do this very tick: we display with 0.1 second precision
				String time = String.format("%.1f", (float) remaining / 1000);
				player.sendActionBar(info.display() + "§e: §6" + time + " seconds");
			}
		}.runTaskTimer(plugin, 1, 1);
	}
	
	private void updateCharge(Player player) {
		long current = System.currentTimeMillis();
		Long started = updated.put(player, current);
		if(started != null) {
			long delta = current - started;
			long gain = delta * CHARGE_GAIN_PER_SECOND / 1000; //we are rounding down
			addCharge(player, gain);
		}
	}
	
	private void addCharge(Player player, long amount) {
		long charge = charges.get(player.getUniqueId());
		charge += amount;
		
		if(charge > MAX_CHARGE) {
			charge = MAX_CHARGE;
		}
		
		charges.put(player.getUniqueId(), charge);
	}
	
	private ItemStack createActivationItem() {
		ItemStack item = new ItemStack(Material.FEATHER);
		ItemMeta meta = item.getItemMeta();
		meta.setDisplayName(info.display());
		meta.setLore(Arrays.asList(
				LOCKED_METADATA_LORE,
				"",
				"§aLeft-click §7for information",
				"§aRight-click §7to toggle."
		));
		item.setItemMeta(meta);
		return item;
	}
	
	private void giveActivationItem(Player player) {
		if(!player.getInventory().addItem(activationItem).isEmpty()) {
			player.sendMessage("§cUnable to give §r" + info.display() + "§c item, please clear an inventory slot.");
		}
	}
	
	private void playEffects(Player player, long remaining) {
		Location center = player.getLocation().add(0, player.getEyeHeight() / 2, 0);
		int count = ThreadLocalRandom.current().nextInt(3, 6);
		player.getWorld().spawnParticle(Particle.FALLING_DUST, center, count, 0.5, 1, 0.5, 0.02, PARTICLE_BLOCK_DATA);
		
		float progress = (float) remaining / MAX_CHARGE;
		player.playSound(center, Sound.BLOCK_NOTE_BLOCK_PLING, 0.5f, (1 - progress) * 2);
	}
	
	private static class Entry {
		BukkitTask task;
		int allowFlightCountdown;
	}
}
