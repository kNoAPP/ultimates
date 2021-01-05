package com.knoban.ultimates.cards.impl;

import com.knoban.ultimates.Ultimates;
import com.knoban.ultimates.aspects.warmup.ActionWarmupTask;
import com.knoban.ultimates.aspects.warmup.ActionWarmupTaskTemplate;
import com.knoban.ultimates.cards.Card;
import com.knoban.ultimates.cards.CardInfo;
import com.knoban.ultimates.events.PreAbilityTeleportEvent;
import com.knoban.ultimates.primal.PrimalSource;
import com.knoban.ultimates.primal.Tier;
import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.*;
import java.util.concurrent.ThreadLocalRandom;

@CardInfo(
		material = Material.COAL,
		name = "shadows-uprise",
		display = "§8Shadow's Uprise", // Typically we want the color to match the Primal
		description = {"§6Travel §7to the surface", "§7by consuming §8coal.", "§7Unavailable in the nether."},
		source = PrimalSource.DARK,
		tier = Tier.EPIC
)
public class ShadowsUpriseCard extends Card implements ActionWarmupTaskTemplate.TickHandler,
		ActionWarmupTaskTemplate.CompletedCallback, ActionWarmupTaskTemplate.InterruptedCallback {
	private static final Set<World.Environment> ENVIRONMENTS = EnumSet.of(World.Environment.NORMAL, World.Environment.THE_END);
	private static final int CONSUME_AMOUNT = 3;
	private static final int WARMUP_SECONDS = 5;
	private static final int PARTICLE_TIME_TICKS = 80;
	private static final int PARTICLE_INTERVAL = 4;
	private static final double PARTICLE_KEEP_RANGE_SQUARED = 5 * 5;
	private static final Collection<PotionEffect> AREA_EFFECTS = Collections.singletonList(
			new PotionEffect(PotionEffectType.BLINDNESS, 40, 0)
	);
	private static final double AREA_EFFECTS_RANGE = 3;
	private static final Collection<PotionEffect> PLAYER_EFFECTS = Arrays.asList(
			new PotionEffect(PotionEffectType.LEVITATION, PARTICLE_TIME_TICKS, 0),
			new PotionEffect(PotionEffectType.FIRE_RESISTANCE, 200, 0)
	);
	private final ActionWarmupTaskTemplate warmupTemplate;
	
	public ShadowsUpriseCard(Ultimates plugin) {
		super(plugin);
		warmupTemplate = ActionWarmupTaskTemplate.createTimed(WARMUP_SECONDS * 20)
				.setMovement(ActionWarmupTaskTemplate.Movement.ALLOW_VERTICAL)
				.setCombat(ActionWarmupTaskTemplate.Combat.DISALLOW)
				.setCard(getClass())
				.addTickHandler(this, 20, 0)
				.addTickHandler((task, i, ii) -> spawnParticles(task.getPlayer()), PARTICLE_INTERVAL, 0)
				.setOnCompleted(this)
				.setOnInterrupted(this);
	}
	
	@EventHandler(priority = EventPriority.HIGHEST)
	public void onTryActivate(PlayerInteractEvent event) {
		if((event.getAction() != Action.RIGHT_CLICK_AIR && event.getAction() != Action.RIGHT_CLICK_BLOCK)
				|| !event.hasItem() || !event.getPlayer().isSneaking()) {
			return;
		}
		
		if(event.useItemInHand() == Event.Result.DENY) {
			return; //this is how "cancellation" is handled
		}
		
		Player player = event.getPlayer();
		if(!drawn.contains(player)
				|| !ENVIRONMENTS.contains(player.getWorld().getEnvironment())) {
			return;
		}
		
		ItemStack item = event.getItem();
		//noinspection ConstantConditions
		if(item.getType() != Material.COAL) {
			return;
		}
		
		if(plugin.getActionWarmupManager().hasTaskCreatedFrom(player, warmupTemplate)) {
			player.sendMessage(info.display() + "§c is already being activated.");
			return;
		}
		
		event.setUseItemInHand(Event.Result.DENY);
		event.setUseInteractedBlock(Event.Result.DENY);
		
		if(item.getAmount() < CONSUME_AMOUNT) {
			player.sendMessage(info.display() + "§c requires " + CONSUME_AMOUNT + " coal.");
			return;
		}
		
		if(plugin.getCombatManager().isInCombat(player)) {
			player.sendMessage(info.display() + "§c is unavailable during combat.");
			return;
		}
		
		Location destination = tryGetDestination(player.getLocation());
		if(destination == null || !new PreAbilityTeleportEvent(player, destination, false).callEvent()) {
			player.sendMessage(info.display() + "§c is unavailable at this location.");
			return;
		}
		
		if(item.getAmount() == CONSUME_AMOUNT) {
			item = null;
		} else {
			item.setAmount(item.getAmount() - CONSUME_AMOUNT);
		}
		//noinspection ConstantConditions
		event.getPlayer().getInventory().setItem(event.getHand(), item);
		
		plugin.getActionWarmupManager().register(player, warmupTemplate);
	}
	
	@Override
	public void onTick(ActionWarmupTask task, int elapsedCount, int elapsedTicks) {
		int remaining = WARMUP_SECONDS - elapsedCount;
		if(remaining > 0) {
			Player player = task.getPlayer();
			player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_PLING, 0.2f, 1);
			player.sendMessage(info.display() + "§e activating in " + remaining + "...");
			spawnParticles(player);
		}
	}
	
	@Override
	public void onCompleted(ActionWarmupTask task) {
		Player player = task.getPlayer();
		Location destination = tryGetDestination(player.getLocation());
		//call the pre event again: stuff can change during the delay
		if(destination == null
				|| !new PreAbilityTeleportEvent(player, destination, false).callEvent()
				|| !player.teleport(destination)) {
			player.sendMessage(info.display() + "§c is unavailable at this location.");
			return;
		}
		
		player.getWorld().playSound(player.getLocation(), Sound.BLOCK_END_PORTAL_SPAWN, 0.5f, 0.5f);
		applyEffects(player, destination);
	}
	
	@Override
	public void onInterrupted(ActionWarmupTask task, ActionWarmupTaskTemplate.InterruptReason reason) {
		if(reason.shouldAnnounceInterruption()) {
			task.getPlayer().sendMessage(info.display() + "§c was interrupted.");
		}
	}
	
	private Location tryGetDestination(Location source) {
		Block block = source.getWorld().getHighestBlockAt(source);
		if(block.getY() <= source.getBlockY()) {
			return null;
		}
		
		Location result = block.getLocation().add(0.5, 0, 0.5);
		result.setYaw(source.getYaw());
		result.setPitch(source.getPitch());
		return result;
	}
	
	private void applyEffects(Player player, Location destination) {
		player.addPotionEffects(PLAYER_EFFECTS);
		
		for(Player close : destination.getNearbyEntitiesByType(Player.class, AREA_EFFECTS_RANGE)) {
			close.addPotionEffects(AREA_EFFECTS);
		}
		
		new BukkitRunnable() {
			private int counter = PARTICLE_TIME_TICKS / PARTICLE_INTERVAL;
			
			@Override
			public void run() {
				if(counter-- == 0 || !drawn.contains(player)
						|| destination.getWorld() != player.getWorld()
						|| destination.distanceSquared(player.getLocation()) > PARTICLE_KEEP_RANGE_SQUARED) {
					cancel();
				} else {
					spawnParticles(player);
				}
			}
		}.runTaskTimer(plugin, 0, PARTICLE_INTERVAL);
	}
	
	private void spawnParticles(Player player) {
		Location center = player.getLocation().add(0, player.getEyeHeight() / 2, 0);
		int count = ThreadLocalRandom.current().nextInt(2, 5);
		player.getWorld().spawnParticle(Particle.SQUID_INK, center, count, 0.8, 1, 0.8, 0.01);
	}
}
