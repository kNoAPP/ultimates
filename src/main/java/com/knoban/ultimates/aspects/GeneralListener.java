package com.knoban.ultimates.aspects;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.knoban.ultimates.Ultimates;
import com.knoban.ultimates.cardholder.CardHolder;
import com.knoban.ultimates.player.LocalPDStore;
import com.knoban.ultimates.primal.PrimalSource;
import io.papermc.paper.event.player.AsyncChatEvent;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Item;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;
import org.bukkit.event.player.PlayerFishEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;

import java.util.Arrays;
import java.util.List;
import java.util.Random;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

public class GeneralListener implements Listener {

	private final Ultimates plugin;

	private final Random random;
	private final boolean allowAFKFishing;

	public GeneralListener(Ultimates plugin) {
		this.plugin = plugin;

		plugin.getServer().getPluginManager().registerEvents(this, plugin);
		this.random = new Random();
		this.allowAFKFishing = plugin.getConfigFile().getCachedYML().getBoolean("AllowAFKFishing", false);
	}


	private static List<Material> REDUCED_FISHING = Arrays.asList(
		Material.TROPICAL_FISH, Material.PUFFERFISH, Material.COD, Material.SALMON
	);
	private static final int SUSPICIOUS_THREASHHOLD = 10;
	private static final double SUSPICIOUS_RANGE = 1.2*1.2;
	private final Cache<UUID, Integer> suspiciousFishers = CacheBuilder.newBuilder().expireAfterAccess(10, TimeUnit.MINUTES).build();
	private final Cache<UUID, Location> lastFishingSpot = CacheBuilder.newBuilder().expireAfterAccess(10, TimeUnit.MINUTES).build();
	@EventHandler
	public void onFish(PlayerFishEvent e) {
		if(!allowAFKFishing && e.getState() == PlayerFishEvent.State.CAUGHT_FISH) {
			Entity caught = e.getCaught();
			if(caught != null) {
				Player p = e.getPlayer();
				Location currentSpot = e.getHook().getLocation();
				Location lastSpot = lastFishingSpot.getIfPresent(p.getUniqueId());
				lastFishingSpot.put(p.getUniqueId(), currentSpot);
				if(lastSpot != null && currentSpot.getWorld().equals(lastSpot.getWorld()) &&
						currentSpot.distanceSquared(lastSpot) <= SUSPICIOUS_RANGE) {
					Integer sameSpotTimes = suspiciousFishers.getIfPresent(p.getUniqueId());
					if(sameSpotTimes == null)
						sameSpotTimes = 0;
					suspiciousFishers.put(p.getUniqueId(), ++sameSpotTimes);

					if(sameSpotTimes >= SUSPICIOUS_THREASHHOLD) {
						if(sameSpotTimes == SUSPICIOUS_THREASHHOLD)
							plugin.getLogger().info(p.getName() + " has been flagged for AFK fishing. They are now receiving reduced rare drops.");
						Item item = (Item) e.getCaught();
						if(!REDUCED_FISHING.contains(item.getItemStack().getType()))
							item.setItemStack(new ItemStack(REDUCED_FISHING.get(random.nextInt(REDUCED_FISHING.size()))));
					}
				} else
					suspiciousFishers.put(p.getUniqueId(), 0);
			}
		}
	}
	
	@EventHandler
	public void onDeath(PlayerDeathEvent e) {
		Player p = e.getEntity();
		LocalPDStore store = plugin.getPlayerDataStore().getPlayerDataStore(p);
		if(store.getFreeRespawns() > 0) {
			store.setFreeRespawns(store.getFreeRespawns() - 1);

			e.setKeepInventory(true);
			e.setKeepLevel(true);
			e.setDroppedExp(0);
			e.getDrops().clear();

			p.sendMessage(Message.RESPAWN.getMessage("You have used a free respawn. " + store.getFreeRespawns() + " left."));
		}
	}
	
	@EventHandler
	public void onClick(PlayerInteractEvent e) {
		Player p = e.getPlayer();
		if(e.getHand() == EquipmentSlot.HAND) {
			if(e.getAction() == Action.RIGHT_CLICK_AIR || e.getAction() == Action.RIGHT_CLICK_BLOCK) {
				ItemStack is = p.getInventory().getItemInMainHand();
				if(is != null) {
					if(is.isSimilar(Items.RESPAWN_ITEM)) {
						if(p.getLevel() >= 30) {
							LocalPDStore store = plugin.getPlayerDataStore().getPlayerDataStore(p);
							store.setFreeRespawns(store.getFreeRespawns() + 1);

							if(is.getAmount() > 1) {
								is.setAmount(is.getAmount() - 1);
							} else {
								p.getInventory().setItemInMainHand(new ItemStack(Material.AIR));
							}
							p.setLevel(p.getLevel() - 30);
							p.updateInventory();

							p.sendMessage(Message.RESPAWN.getMessage("Respawn Token active!"));
							p.sendMessage(Message.RESPAWN.getMessage("You have " + store.getFreeRespawns() + " respawn(s) left!"));
							p.getWorld().playSound(p.getLocation(), Sound.ITEM_TOTEM_USE, 2F, 0.8F);
							p.getWorld().spawnParticle(Particle.TOTEM, p.getLocation().clone().add(0, 0.5, 0), 30, 0.5F, 0.5F, 0.5F, 1);
						} else {
							p.sendMessage(Message.RESPAWN.getMessage("This requires 30 levels of xp!"));
							p.playSound(p.getLocation(), Sound.ENTITY_CHICKEN_HURT, 1F, 1F);
						}
					}
				}
			}
		}
	}

	@EventHandler(priority = EventPriority.HIGHEST)
	public void beforeCommand(PlayerCommandPreprocessEvent e) {
		Player p = e.getPlayer();
		String cmd = e.getMessage();
		if(cmd.startsWith("/help") || cmd.startsWith("/HELP"))
			e.setMessage(cmd.replaceFirst("/help", "/ults help").replaceFirst("/HELP", "/ults help"));
		else if(cmd.equalsIgnoreCase("/reload") || cmd.equalsIgnoreCase("/reload confirm")) {
			p.sendMessage("§4It never a good idea to reload with Ultimates.");
			p.sendMessage("§4Please restart the server to ensure data consistency!");
			e.setCancelled(true);
		}
	}

	@EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
	public void onChat(AsyncChatEvent e) {
		Player p = e.getPlayer();
		CardHolder holder = CardHolder.getCardHolder(p);
		if(!holder.isLoaded()) {
			p.sendMessage(CardHolder.UNLOADED_MESSAGE);
			return;
		}

		PrimalSource source = holder.getPrimalSource();
		if(source == PrimalSource.NONE)
			e.composer((player, display, message) ->
					display.color(NamedTextColor.GRAY)
							.append(Component.text(": ").color(NamedTextColor.GRAY))
							.append(message.color(NamedTextColor.GRAY)));
		else
			e.composer((player, display, message) ->
					Component.text(source.getDisplay() + " ").color(source.getTextColor())
					.append(display.color(NamedTextColor.GRAY))
					.append(Component.text(": ").color(NamedTextColor.GRAY))
					.append(message.color(NamedTextColor.GRAY)));
	}
}
