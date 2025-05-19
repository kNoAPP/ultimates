package com.knoban.ultimates.cards.helpers;

import com.knoban.ultimates.Ultimates;
import com.knoban.ultimates.claims.UltimatesEstateListener;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.block.Block;
import org.bukkit.block.data.BlockData;
import org.bukkit.entity.*;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent.DamageCause;
import org.bukkit.inventory.ItemStack;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.EulerAngle;

import java.util.ArrayList;
import java.util.List;

public class Levitation {

	private final JavaPlugin plugin;
	private final Player p;
	private final Runnable invalidationCallback;
	private LivingEntity le;
	private ArmorStand as;
	private BlockData bd;
	private Material m;
	private final int type;
	
	private static final int BLOCK = 0;
	private static final int ENTITY = 1;
	
	private boolean ready = false, valid = true;
	
	public Levitation(JavaPlugin plugin, Player p, Block b, Runnable invalidationCallback) {
		this.plugin = plugin;
		this.p = p;
		this.m = b.getType();
		this.bd = b.getBlockData();
		this.invalidationCallback = invalidationCallback;
		this.type = BLOCK;
	}
	
	public Levitation(JavaPlugin plugin, Player p, LivingEntity le, Runnable invalidationCallback) {
		this.plugin = plugin;
		this.p = p;
		this.le = le;
		this.invalidationCallback = invalidationCallback;
		this.type = ENTITY;
	}

	/**
	 * Call only once per instance of Levitation!
	 */
	public void levitate() {
		as = (ArmorStand) p.getWorld().spawnEntity(p.getLocation().clone().add(p.getLocation().getDirection().normalize().multiply(p.getInventory().getHeldItemSlot()*1.5+1)), EntityType.ARMOR_STAND);
		as.setVisible(false);
		as.setInvulnerable(true);
		as.setGravity(false);
		
		if(type == BLOCK) {
			ItemStack is = new ItemStack(m);
			as.getEquipment().setHelmet(is);
		} else as.addPassenger(le);
		
		as.setHeadPose(new EulerAngle(0, 0, 0));
		p.getWorld().playSound(p.getLocation(), Sound.BLOCK_BEACON_POWER_SELECT, 1F, 1.6F);

		new BukkitRunnable() {
			public void run() {
				ready = true;

				if(!valid) {
					this.cancel();
					return;
				}

				if(type == ENTITY && (!le.isValid() || le.isDead())) {
					drop();
					this.cancel();
					return;
				}

				if(type == ENTITY) {
					if(p.getVehicle() != null && p.getVehicle().equals(le)) le.removePassenger(p);
					if(as.getPassengers().contains(p)) as.removePassenger(p);
					as.removePassenger(le);
				}
				as.teleport(p.getLocation().clone().add(p.getLocation().getDirection().normalize().multiply(p.getInventory().getHeldItemSlot()*1.5+1)));
				if(type == ENTITY)
					as.addPassenger(le);
			}
		}.runTaskTimer(plugin, 1L, 2L);
	}
	
	public void launch() {
		if(!ready)
			return;
		
		Entity e;
		if(type == BLOCK) {
			FallingBlock fb = p.getWorld().spawnFallingBlock(as.getLocation().clone().add(0, 1, 0), bd);
			fb.setDropItem(false);
			fb.setMetadata(UltimatesEstateListener.SHOOTER, new FixedMetadataValue(plugin, p));
			e = fb;
			as.getEquipment().setHelmet(new ItemStack(Material.AIR));
		} else {
			e = le;
			as.removePassenger(le);
		}
		as.remove();
		as = null;
		
		e.setVelocity(p.getLocation().clone().getDirection().normalize().multiply((12-p.getInventory().getHeldItemSlot())/4));
		p.getWorld().playSound(p.getLocation(), Sound.ENTITY_FIREWORK_ROCKET_BLAST, 1F, 0.6F);
		
		valid = false;
		if(invalidationCallback != null)
			invalidationCallback.run();

		List<LivingEntity> hitEntities = new ArrayList<>();
		hitEntities.add(p);
		new BukkitRunnable() {
			public void run() {
				if(e.isValid() && !e.isDead() && !e.isOnGround()) {
					for(Entity te : e.getNearbyEntities(1, 1, 1)) {
						if(te instanceof LivingEntity && e != te) {
							LivingEntity tle = (LivingEntity) te;
							if(hitEntities.contains(tle))
								continue;
							else
								hitEntities.add(tle);

							tle.damage(e.getVelocity().clone().length() * 2.5);
							tle.getWorld().spawnParticle(Particle.EXPLOSION, tle.getLocation().clone().add(0, 0.8, 0), 1, 0.2F, 0.2F, 0.2F, 0.01);
							tle.getWorld().playSound(tle.getLocation(), Sound.ENTITY_ZOMBIE_ATTACK_WOODEN_DOOR, 1F, 1F);
							try {
								tle.setVelocity(e.getVelocity().clone().normalize());
							} catch(IllegalArgumentException ignored) {} //Launching a player riding a horse
							if(p.isValid() && p.isOnline()) {
								EntityDamageByEntityEvent event = new EntityDamageByEntityEvent(p, tle, DamageCause.FALLING_BLOCK, e.getVelocity().clone().length() * 2.5);
								plugin.getServer().getPluginManager().callEvent(event);
								if(!event.isCancelled())
									tle.setLastDamageCause(event);
							}
						}
					}
				} else {
					this.cancel();
				}
			}
		}.runTaskTimer(Ultimates.getPlugin(), 0L, 2L);
	}
	
	public void drop() {
		if(!ready)
			return;
		
		if(type == BLOCK) {
			FallingBlock fb = p.getWorld().spawnFallingBlock(as.getLocation().clone().add(0, 1, 0), bd);
			fb.setDropItem(false);
			as.getEquipment().setHelmet(new ItemStack(Material.AIR));
		} else if(le != null && le.isValid() && !le.isDead())
			as.removePassenger(le);
			
		as.remove();
		as = null;
		p.getWorld().playSound(p.getLocation(), Sound.ENTITY_ELDER_GUARDIAN_HURT_LAND, 1F, 1.4F);

		valid = false;
		if(invalidationCallback != null)
			invalidationCallback.run();
	}

	public boolean isValid() {
		return valid;
	}
}
