package com.knoban.ultimates.cards.impl;

import co.aikar.timings.Timing;
import co.aikar.timings.Timings;
import com.destroystokyo.paper.event.player.PlayerPostRespawnEvent;
import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.knoban.atlas.structure.Pair;
import com.knoban.atlas.utils.Cooldown;
import com.knoban.ultimates.Ultimates;
import com.knoban.ultimates.aspects.warmup.ActionWarmupTask;
import com.knoban.ultimates.aspects.warmup.ActionWarmupTaskTemplate;
import com.knoban.ultimates.cards.Card;
import com.knoban.ultimates.cards.CardInfo;
import com.knoban.ultimates.events.PreAbilityTeleportEvent;
import com.knoban.ultimates.primal.PrimalSource;
import com.knoban.ultimates.primal.Tier;
import org.apache.commons.lang.Validate;
import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.world.WorldLoadEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.*;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;

@CardInfo(
		material = Material.WITHER_ROSE,
		name = "reality-phase",
		display = "§cReality Phase", // Typically we want the color to match the Primal
		description = {"§7Instantly §6shift §7between", "§7the overworld and nether."},
		source = PrimalSource.FIRE,
		tier = Tier.EPIC
)
public class RealityPhaseCard extends Card implements ActionWarmupTaskTemplate.TickHandler,
		ActionWarmupTaskTemplate.CompletedCallback, ActionWarmupTaskTemplate.InterruptedCallback {
	private static final int SEARCH_RADIUS_HORIZONTAL = 5;
	private static final int SEARCH_RADIUS_VERTICAL = 10;
	private static final int WARMUP_SECONDS = 5;
	private static final int PARTICLE_INTERVAL = 4;
	private static final long COOLDOWN_MILLIS = TimeUnit.SECONDS.toMillis(5);
	private final Cache<UUID, Cooldown> cooldowns = CacheBuilder.newBuilder()
			.expireAfterWrite(COOLDOWN_MILLIS, TimeUnit.MILLISECONDS).build();
	private final ItemStack activationItem;
	private final WorldContainer worldContainer;
	private final Timing timings;
	private final ActionWarmupTaskTemplate warmupTemplate;
	
	public RealityPhaseCard(Ultimates plugin) {
		super(plugin);
		activationItem = createActivationItem();
		worldContainer = new WorldContainer(plugin);
		timings = Timings.of(plugin, getClass().getSimpleName() + "#getPostLoadDestination");
		
		warmupTemplate = ActionWarmupTaskTemplate.createMinTimed(WARMUP_SECONDS * 20,
				task -> task.getCompanion(Companion.class).chunk != null)
				.setCompanionConstructor(Companion::new)
				.setMovement(ActionWarmupTaskTemplate.Movement.DISALLOW)
				.setCombat(ActionWarmupTaskTemplate.Combat.DISALLOW)
				.setCard(getClass())
				.addTickHandler(this, 20, 0)
				.addTickHandler((task, i, ii) -> spawnParticles(task.getPlayer()), PARTICLE_INTERVAL, 0)
				.setOnCompleted(this)
				.setOnInterrupted(this);
	}
	
	@Override
	protected void register() {
		super.register();
		
		//TODO use not yet implemented config store
		//max height is configurable due to how the nether roof works
		worldContainer.register("world", 252);
		worldContainer.register("world_nether", 125);
		worldContainer.add("world", "world_nether");
		worldContainer.add("world_nether", "world");
	}
	
	@Override
	protected void unregister() {
		super.unregister();
		worldContainer.clear();
	}
	
	@Override
	public boolean draw(Player p) {
		boolean didEquip = super.draw(p);
		if(didEquip) {
			giveActivationItem(p);
		}
		return didEquip;
	}
	
	@Override
	public boolean discard(Player p) {
		boolean didDispose = super.discard(p);
		if(didDispose) {
			p.getInventory().removeItemAnySlot(activationItem);
		}
		return didDispose;
	}
	
	@EventHandler(ignoreCancelled = true, priority = EventPriority.HIGHEST)
	public void onRespawn(PlayerPostRespawnEvent event) {
		Player player = event.getPlayer();
		if(drawn.contains(player) && !player.getInventory().contains(activationItem)) {
			giveActivationItem(player);
		}
	}
	
	@EventHandler(priority = EventPriority.HIGHEST)
	public void onTryActivate(PlayerInteractEvent event) {
		if(!event.hasItem() || event.useItemInHand() == Event.Result.DENY
				|| (event.getAction() != Action.RIGHT_CLICK_AIR && event.getAction() != Action.RIGHT_CLICK_BLOCK)
				|| !drawn.contains(event.getPlayer()) || !activationItem.isSimilar(event.getItem())) {
			return;
		}
		
		event.setUseItemInHand(Event.Result.DENY);
		event.setUseInteractedBlock(Event.Result.DENY);
		Player player = event.getPlayer();
		
		if(plugin.getCombatManager().isInCombat(player)) {
			player.sendMessage(info.display() + "§c is unavailable during combat.");
			return;
		}
		
		Cooldown cooldown = cooldowns.getIfPresent(player.getUniqueId());
		if(cooldown != null) {
			player.sendMessage(info.display() + " §cis on cooldown for another " + cooldown.toTimestampString() + ".");
			return;
		}
		
		if(plugin.getActionWarmupManager().hasTaskCreatedFrom(player, warmupTemplate)) {
			player.sendMessage(info.display() + "§c is already being activated.");
			return;
		}
		
		World destinationWorld = worldContainer.getTo(player.getWorld());
		Location destination = destinationWorld == null ? null
				: getPreLoadDestination(player.getLocation(), destinationWorld);
		if(destination == null || !new PreAbilityTeleportEvent(player, destination, true).callEvent()) {
			player.sendMessage(info.display() + "§c is unavailable at this location.");
			return;
		}
		
		cooldowns.put(player.getUniqueId(), new Cooldown(COOLDOWN_MILLIS));
		ActionWarmupTask warmupTask = plugin.getActionWarmupManager().register(player, warmupTemplate);
		warmupTask.getCompanion(Companion.class).destination = destination;
		
		//load the chunk async (urgently)
		//the 5 second warmup should be enough, but if it isn't, we just delay it
		destinationWorld.getChunkAtAsyncUrgently(destination).whenComplete((chunk, error) -> {
			if(error != null) {
				plugin.getLogger().log(Level.SEVERE, RealityPhaseCard.class.getSimpleName()
						+ ": error during async chunk load", error);
				if(warmupTask.isActive()) {
					player.sendMessage(info.display() + "§c experienced an internal error.");
					cooldowns.invalidate(player.getUniqueId());
					plugin.getActionWarmupManager().unregister(warmupTask);
				}
			} else if(warmupTask.isActive()) {
				chunk.addPluginChunkTicket(plugin);
				warmupTask.getCompanion(Companion.class).chunk = chunk;
			}
		});
	}
	
	@Override
	public void onTick(ActionWarmupTask task, int elapsedCount, int elapsedTicks) {
		Player player = task.getPlayer();
		player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_PLING, 0.2f, 1);
		int remaining = WARMUP_SECONDS - elapsedCount;
		if(remaining > 0) {
			player.sendMessage(info.display() + "§e activating in " + remaining + "...");
		} else {
			//only waiting on async chunk load
			player.sendMessage(info.display() + "§e activating soon...");
		}
	}
	
	@Override
	public void onCompleted(ActionWarmupTask task) {
		Companion companion = task.getCompanion();
		Chunk chunk = companion.chunk;
		
		Player player = task.getPlayer();
		Location destination = getPostLoadDestination(companion.destination);
		if(destination == null
				|| !new PreAbilityTeleportEvent(player, destination, false).callEvent()
				|| !player.teleport(destination)) {
			player.sendMessage(info.display() + "§c is unavailable at this location.");
			cooldowns.invalidate(player.getUniqueId());
			chunk.removePluginChunkTicket(plugin);
			return;
		}
		
		player.getWorld().playSound(player.getLocation(), Sound.BLOCK_PORTAL_TRIGGER, 0.5f, 2);
		chunk.removePluginChunkTicket(plugin);
	}
	
	@Override
	public void onInterrupted(ActionWarmupTask task, ActionWarmupTaskTemplate.InterruptReason reason) {
		Chunk chunk = task.getCompanion(Companion.class).chunk;
		if(chunk != null) {
			chunk.removePluginChunkTicket(plugin);
		}
		
		if(reason.shouldAnnounceInterruption()) {
			task.getPlayer().sendMessage(info.display() + "§c was interrupted.");
		}
	}
	
	private Location getPreLoadDestination(Location source, World destinationWorld) {
		double distanceMultiplier = source.getWorld().getEnvironment() == World.Environment.NETHER ? 8 : 1;
		distanceMultiplier *= destinationWorld.getEnvironment() == World.Environment.NETHER ? 1.0 / 8 : 1;
		
		//make sure x and z +- search radius is inside a single chunk
		int x = (int) (source.getBlockX() * distanceMultiplier);
		int z = (int) (source.getBlockZ() * distanceMultiplier);
		int minX = (x >> 4) * 16 + SEARCH_RADIUS_HORIZONTAL;
		int minZ = (z >> 4) * 16 + SEARCH_RADIUS_HORIZONTAL;
		int maxX = (x >> 4) * 16 + 15 - SEARCH_RADIUS_HORIZONTAL;
		int maxZ = (z >> 4) * 16 + 15 - SEARCH_RADIUS_HORIZONTAL;
		
		Location destination = source.clone();
		destination.setWorld(destinationWorld);
		destination.setX(Math.min(maxX, Math.max(minX, x)) + 0.5);
		destination.setZ(Math.min(maxZ, Math.max(minZ, z)) + 0.5);
		
		int destinationMaxHeight = worldContainer.getMaxHeight(destinationWorld);
		int targetY = (int) (source.getY() / worldContainer.getMaxHeight(source.getWorld()) * destinationMaxHeight);
		if(targetY - SEARCH_RADIUS_VERTICAL < 1) {
			targetY = 1 + SEARCH_RADIUS_VERTICAL;
		} else if(targetY + SEARCH_RADIUS_VERTICAL > destinationMaxHeight) {
			targetY = destinationMaxHeight - SEARCH_RADIUS_VERTICAL;
		}
		destination.setY(targetY);
		
		if(!destinationWorld.getWorldBorder().isInside(destination)) {
			return null;
		}
		
		return destination;
	}
	
	private Location getPostLoadDestination(Location originalDestination) {
		timings.startTiming();
		try {
			for(Block block : getBlockIterator(originalDestination)) {
				if(isSafeGround(block.getRelative(0, -1, 0)) && isSafeAir(block) && isSafeAir(block.getRelative(0, 1, 0))) {
					Location location = block.getLocation().add(0.5, 0, 0.5);
					location.setYaw(originalDestination.getYaw());
					location.setPitch(originalDestination.getYaw());
					return location;
				}
			}
			return null;
		} finally {
			timings.stopTiming();
		}
	}
	
	private Iterable<Block> getBlockIterator(Location center) {
		//search for the closest valid location -> loop in a spiral
		//code from https://stackoverflow.com/questions/398299/looping-in-a-spiral
		
		int centerX = center.getBlockX();
		int centerZ = center.getBlockZ();
		int x = 0;
		int z = 0;
		int dx = 0;
		int dz = -1;
		int maxI = SEARCH_RADIUS_HORIZONTAL * 2 + 1;
		maxI *= maxI;
		int radius = SEARCH_RADIUS_HORIZONTAL;
		List<Block> list = new ArrayList<>();
		
		for(int i = 0; i < maxI; i++) {
			if(-radius <= x && x <= radius && -radius <= z && z <= radius) {
				int realX = centerX + x;
				int realZ = centerZ + z;
				Validate.isTrue(realX >> 4 == centerX >> 4, "Coordinates must be in the same chunk");
				Validate.isTrue(realZ >> 4 == centerZ >> 4, "Coordinates must be in the same chunk");
				
				int topY = center.getWorld().getHighestBlockYAt(realX, realZ);
				if(center.getBlockY() >= topY) {
					list.add(center.getWorld().getBlockAt(realX, topY, realZ));
				} else {
					int maxY = center.getBlockY() + SEARCH_RADIUS_VERTICAL;
					if(maxY > topY) {
						maxY = topY;
					}
					for(int y = center.getBlockY() - SEARCH_RADIUS_VERTICAL; y <= maxY; y++) {
						list.add(center.getWorld().getBlockAt(realX, y, realZ));
					}
				}
			}
			if(x == z || (x < 0 && x == -z) || (x > 0 && x == 1 - z)) {
				int temp = dx;
				dx = -dz;
				dz = temp;
			}
			x += dx;
			z += dz;
		}
		
		return list;
	}
	
	private boolean isSafeGround(Block block) {
		return block.getType().isSolid();
	}
	
	private boolean isSafeAir(Block block) {
		return !block.isLiquid() && block.isPassable();
	}
	
	private ItemStack createActivationItem() {
		ItemStack item = new ItemStack(Material.WITHER_ROSE);
		ItemMeta meta = item.getItemMeta();
		meta.setDisplayName(info.display());
		meta.setLore(Arrays.asList(
				LOCKED_METADATA_LORE,
				"",
				"§aRight-click §7to activate."
		));
		item.setItemMeta(meta);
		return item;
	}
	
	private void giveActivationItem(Player player) {
		if(!player.getInventory().addItem(activationItem).isEmpty()) {
			player.sendMessage("§cUnable to give §r" + info.display() + "§c item, please clear an inventory slot.");
		}
	}
	
	private void spawnParticles(Player player) {
		Location center = player.getLocation().add(0, player.getEyeHeight() / 2, 0);
		int count = ThreadLocalRandom.current().nextInt(5, 10);
		player.getWorld().spawnParticle(Particle.PORTAL, center, count, 0.5, 1, 0.5, 0.02);
	}
	
	private static class WorldContainer implements Listener {
		private final List<Pair<String, String>> unloaded = new ArrayList<>();
		private final Map<World, World> loaded = new HashMap<>();
		private final Map<String, Integer> maxHeights = new HashMap<>();
		
		WorldContainer(Ultimates plugin) {
			Bukkit.getPluginManager().registerEvents(this, plugin);
		}
		
		void register(String world, int maxHeight) {
			Validate.isTrue(maxHeight > 2 * SEARCH_RADIUS_VERTICAL + 1,
					"Max world height too little, code not prepared for it: " + maxHeight);
			maxHeights.put(world.toLowerCase(), maxHeight);
		}
		
		void add(String fromName, String toName) {
			fromName = fromName.toLowerCase();
			toName = toName.toLowerCase();
			Validate.isTrue(maxHeights.containsKey(fromName) && maxHeights.containsKey(toName), "Worlds must be registered");
			World from = Bukkit.getWorld(fromName);
			World to = Bukkit.getWorld(toName);
			if(from == null || to == null) {
				unloaded.add(new Pair<>(fromName, toName));
			} else {
				loaded.put(from, to);
			}
		}
		
		void clear() {
			unloaded.clear();
			loaded.clear();
		}
		
		World getTo(World from) {
			return loaded.get(from);
		}
		
		int getMaxHeight(World world) {
			//World#getMaxHeight doesn't work for eg. the nether bedrock roof
			return maxHeights.get(world.getName().toLowerCase());
		}
		
		@EventHandler(ignoreCancelled = true)
		private void onLoad(WorldLoadEvent event) {
			String name = event.getWorld().getName().toLowerCase();
			for(Iterator<Pair<String, String>> iterator = unloaded.iterator(); iterator.hasNext(); ) {
				Pair<String, String> pair = iterator.next();
				if(!name.equals(pair.getKey()) && !name.equals(pair.getValue())) {
					continue;
				}
				
				//noinspection ConstantConditions
				World from = Bukkit.getWorld(pair.getKey());
				//noinspection ConstantConditions
				World to = Bukkit.getWorld(pair.getValue());
				if(from != null && to != null) {
					iterator.remove();
					loaded.put(from, to);
					//not breaking on purpose
				}
			}
		}
	}
	
	private static class Companion {
		Location destination;
		Chunk chunk;
	}
}
