package com.knoban.ultimates.cards.impl;

import com.knoban.ultimates.Ultimates;
import com.knoban.ultimates.cards.Card;
import com.knoban.ultimates.cards.CardInfo;
import com.knoban.ultimates.primal.PrimalSource;
import com.knoban.ultimates.primal.Tier;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.util.Vector;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@CardInfo(
		material = Material.FLINT,
		name = "panther",
		display = "§9Panther", // Typically we want the color to match the Primal
		description = {"§7Taking damages §2charges", "§7a special melee attack."},
		source = PrimalSource.NONE,
		tier = Tier.COMMON
)
public class PantherCard extends Card {
	private static final String PERSISTENCE_KEY = "charges";
	private static final float REQUIRED_CHARGE = 100; //charge required to be able to "use ability"
	private static final float DAMAGE_TO_CHARGE_MULTIPLIER = 4; //charge(damage) = damage * this
	private static final double ABILITY_EXTRA_DAMAGE = 4;
	private static final PotionEffect ABILITY_BLINDNESS = new PotionEffect(PotionEffectType.BLINDNESS, 60, 0);
	private static final double ABILITY_KNOCKBACK_STRENGTH = 5;
	private static final double ABILITY_KNOCKBACK_VERTICAL = 0.7;
	private final Map<UUID, Float> charges = new ConcurrentHashMap<>();
	
	public PantherCard(Ultimates plugin) {
		super(plugin);
	}
	
	@Override
	public boolean discard(Player p) {
		boolean toRet = super.discard(p);
		if (toRet) {
			writeData(p.getUniqueId(), PERSISTENCE_KEY, charges.remove(p.getUniqueId()), null);
		}
		return toRet;
	}
	
	@Override
	public void onPlayerData(Player p, Map<String, Object> data) {
		float gain = ((Number) data.getOrDefault(PERSISTENCE_KEY, 0)).floatValue();
		if(addCharge(p.getUniqueId(), gain)) {
			broadcastReady(p);
		}
	}
	
	private boolean addCharge(UUID player, float amount) {
		float newCharge = charges.merge(player, amount, Float::sum);
		return newCharge >= REQUIRED_CHARGE && newCharge - amount < REQUIRED_CHARGE;
	}
	
	private void broadcastReady(Player player) {
		player.sendMessage("§dYour special " + info.display() + "§d attack has charged.");
	}
	
	@EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
	public void onPlayerDied(PlayerDeathEvent event) {
		if (drawn.contains(event.getEntity())) {
			charges.put(event.getEntity().getUniqueId(), 0f);
		}
	}
	
	@EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
	public void onDamageTaken(EntityDamageEvent event) {
		if (event.getEntityType() != EntityType.PLAYER) {
			return;
		}
		
		Player player = (Player) event.getEntity();
		if (drawn.contains(player) && player.getHealth() > event.getFinalDamage()) {
			//only add charge if the player won't die
			float gain = (float) event.getDamage() * DAMAGE_TO_CHARGE_MULTIPLIER;
			//we use the damage dealt and not the damage taken
			if (addCharge(player.getUniqueId(), gain)) {
				broadcastReady(player);
			}
		}
	}
	
	@EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
	public void onDamageGiven(EntityDamageByEntityEvent event) {
		if (!(event.getEntity() instanceof LivingEntity) || !(event.getDamager() instanceof Player)) {
			return;
		}
		
		Player player = (Player) event.getDamager();
		//the following two accesses of charges aren't atomic, but it's fine
		if (!drawn.contains(player) || charges.getOrDefault(player.getUniqueId(), 0f) < REQUIRED_CHARGE) {
			return;
		}
		
		charges.put(player.getUniqueId(), 0f);
		LivingEntity victim = (LivingEntity) event.getEntity();
		Location victimLocation = victim.getLocation();
		
		victimLocation.getWorld().playSound(victimLocation, Sound.ENTITY_WITHER_SHOOT, 0.3f, 1);
		event.setDamage(event.getDamage() + ABILITY_EXTRA_DAMAGE);
		victim.addPotionEffect(ABILITY_BLINDNESS);
		
		Vector knockback = victimLocation.subtract(player.getLocation())
				.toVector().setY(0).normalize()
				.multiply(ABILITY_KNOCKBACK_STRENGTH)
				.setY(ABILITY_KNOCKBACK_VERTICAL);
		victim.setVelocity(victim.getVelocity().add(knockback));
	}
}
