package com.knoban.ultimates.commands;

import com.knoban.atlas.commandsII.ACAPI;
import com.knoban.atlas.commandsII.annotations.AtlasCommand;
import com.knoban.atlas.utils.Tools;
import com.knoban.atlas.world.Coordinate;
import com.knoban.ultimates.Ultimates;
import com.knoban.ultimates.aspects.Message;
import com.knoban.ultimates.player.LocalPDStore;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.HashSet;
import java.util.List;
import java.util.UUID;

public class RecallCommandHandle implements Listener {

	private final Ultimates plugin;
	private final HashSet<UUID> recalls = new HashSet<>();

	public RecallCommandHandle(Ultimates plugin) {
		this.plugin = plugin;

		ACAPI api = ACAPI.getApi();
		api.registerCommandsFromClass(plugin, RecallCommandHandle.class, this);
		plugin.getServer().getPluginManager().registerEvents(this, plugin);
	}

	@AtlasCommand(paths = {"recall"})
	public void cmdRecallBase(Player sender) {
		LocalPDStore store = Ultimates.getPlugin().getPlayerDataStore().getPlayerDataStore(sender);

		Coordinate recallTo = store.getRecallLocation();
		if(recallTo != null) {
			if(!recalls.contains(sender.getUniqueId()))
				recall(sender, recallTo.getLocation());
			else
				sender.sendMessage(Message.RECALL.getMessage("Already recalling..."));
		} else
			sender.sendMessage(Message.RECALL.getMessage("You don't have recall location set. Try /recall set"));
	}

	@AtlasCommand(paths = {"recall set"})
	public void cmdRecallSet(Player sender) {
		LocalPDStore store = Ultimates.getPlugin().getPlayerDataStore().getPlayerDataStore(sender);

		if(!recalls.contains(sender.getUniqueId())) {
			sender.sendMessage(Message.RECALL.getMessage("Your location has been saved."));
			sender.playSound(sender.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 2F, 1F);

			store.setRecallLocation(new Coordinate(sender.getLocation()));
		} else
			sender.sendMessage(Message.RECALL.getMessage("Cannot do this while recalling!"));
	}

	@AtlasCommand(paths = {"recall kill"})
	public void cmdRecallKill(Player sender) {
		LocalPDStore store = Ultimates.getPlugin().getPlayerDataStore().getPlayerDataStore(sender);

		if(!recalls.contains(sender.getUniqueId())) {
			sender.sendMessage(Message.RECALL.getMessage("Your location has been removed."));
			sender.playSound(sender.getLocation(), Sound.ENTITY_BLAZE_AMBIENT, 2F, 1F);

			store.setRecallLocation(null);
		} else
			sender.sendMessage(Message.RECALL.getMessage("Cannot remove recall while recalling..."));
	}
	
	private void recall(Player p, Location l) {
		recalls.add(p.getUniqueId());
		
		new BukkitRunnable() {
			int t = 0;
			public void run() {
				if(recalls.contains(p.getUniqueId())) {
					double tl = 8 - ((double)t/20);
					
					p.sendActionBar(Tools.generateWaitBar((double) t / (20.0 * 8.0), 20, ChatColor.GOLD, '☕', ChatColor.GRAY, '☕') + " " + ChatColor.GREEN + Tools.round(tl, 1) + "s");
					p.getWorld().spawnParticle(Particle.VILLAGER_HAPPY, p.getLocation().clone().add(0, 0.5, 0), 1, 0.3F, 0.1F, 0.3F, 0.01);
					if(t%20 == 0) {
						p.playSound(p.getLocation(), Sound.BLOCK_TRIPWIRE_CLICK_ON, 1F, 1F);
					}
					if(tl <= 0) {
						List<Entity> passs = p.getPassengers();
						for(Entity pass : passs) {
							pass.leaveVehicle();
							pass.teleport(l);
						}
						
						Entity m = p.getVehicle();
						if(m != null) {
							p.leaveVehicle();
							p.teleport(l);
							m.teleport(l);
							m.addPassenger(p);
						} else
							p.teleport(l);
						p.playSound(p.getLocation(), Sound.BLOCK_PORTAL_TRAVEL, 1F, 1F);
						
						//Builds Particles
						for(double phi=0; phi<=Math.PI; phi+=Math.PI/15) {
							for(double theta=0; theta<=2*Math.PI; theta+=Math.PI/30) {
			    				double r = 1.5;
			    				double x = r*Math.cos(theta)*Math.sin(phi);
			    				double y = r*Math.cos(phi) + 1.5;
			    				double z = r*Math.sin(theta)*Math.sin(phi);
			    			
			    				l.add(x,y,z);
			    				p.getWorld().spawnParticle(Particle.DRIP_WATER, l, 1, 0F, 0F, 0F, 0.001);
			    				l.subtract(x, y, z);
			    			}
						}
						recalls.remove(p.getUniqueId());
						this.cancel();
					} else {
						t++;
					}
				} else {
					p.sendActionBar(ChatColor.RED + "Teleport Cancelled!");
					p.playSound(p.getLocation(), Sound.ENTITY_ZOMBIE_BREAK_WOODEN_DOOR, 1F, 1F);
					this.cancel();
				}
			}
		}.runTaskTimer(Ultimates.getPlugin(), 0L, 1L);
	}
	
	public void cancel(UUID uuid) {
		recalls.remove(uuid);
	}

	@EventHandler
	public void onleave(PlayerQuitEvent e) {
		cancel(e.getPlayer().getUniqueId());
	}
	
	@EventHandler
	public void onDamage(EntityDamageEvent e) {
		if(e.getEntity() instanceof Player) {
			Player p = (Player) e.getEntity();
			cancel(p.getUniqueId());
		}
	}
	
	@EventHandler
	public void move(PlayerMoveEvent e) {
		if(e.getTo().distance(e.getFrom()) > 0.05)
			cancel(e.getPlayer().getUniqueId());
	}
}