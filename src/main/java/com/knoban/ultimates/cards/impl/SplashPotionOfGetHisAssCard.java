package com.knoban.ultimates.cards.impl;

import com.destroystokyo.paper.event.entity.EntityPathfindEvent;
import com.destroystokyo.paper.event.player.PlayerLaunchProjectileEvent;
import com.destroystokyo.paper.event.player.PlayerPostRespawnEvent;
import com.knoban.atlas.listeners.HeldSlotListener;
import com.knoban.atlas.utils.Cooldown;
import com.knoban.ultimates.Ultimates;
import com.knoban.ultimates.cards.Card;
import com.knoban.ultimates.cards.CardInfo;
import com.knoban.ultimates.primal.PrimalSource;
import com.knoban.ultimates.primal.Tier;
import org.bukkit.*;
import org.bukkit.entity.*;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.entity.ProjectileHitEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.PotionMeta;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.potion.PotionData;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.potion.PotionType;

import java.util.*;
import java.util.stream.Collectors;

@CardInfo(
        material = Material.SPLASH_POTION,
        name = "splash-potion-of-get-his-ass",
        display = "§8Splash Potion of Get His Ass", // Typically we want the color to match the Primal
        description = {"§7Every §etwo minutes§7, gain a", "§7charge of this §dsplash potion",
                "§7if you have less than §ethree§7.", "", "§7When §ethrown§7, make all nearby mobs",
                "§chostile §7towards those affected by it."},
        source = PrimalSource.OCEAN,
        tier = Tier.EPIC
)
public class SplashPotionOfGetHisAssCard extends Card {

    public static final ItemStack SPLASH_POTION_OF_GET_HIS_ASS = getPotionItem();
    public static final String METADATA_POTION = "ults_get_his_ass";
    public static final String METADATA_ENTITY = "ults_get_whos_ass";
    public static final String METADATA_MY_ASS = "ults_get_my_ass";
    public static final String METADATA_ENTITY_EXPR = "ults_getting_their_ass";

    public static final int MAX_CHARGES = 3;

    public static final Long REMAIN_AGGROED_FOR_MILLIS = 30000L;
    public static final Long CHARGE_COOLDOWN = 120000L;

    private final HashMap<UUID, Integer> potions = new HashMap<>();
    private final HashMap<UUID, Cooldown> potionCooldown = new HashMap<>();

    public SplashPotionOfGetHisAssCard(Ultimates plugin) {
        super(plugin);
    }

    @Override
    public void cacheItemStacks() {
        super.cacheItemStacks();
        PotionMeta im = (PotionMeta) unownedCantBuyIcon.getItemMeta();
        im.setBasePotionData(new PotionData(PotionType.HARMING));
        unownedCantBuyIcon.setItemMeta(im);

        im = (PotionMeta) unownedCanBuyIcon.getItemMeta();
        im.setBasePotionData(new PotionData(PotionType.HARMING));
        unownedCanBuyIcon.setItemMeta(im);

        im = (PotionMeta) unownedIcon.getItemMeta();
        im.setBasePotionData(new PotionData(PotionType.HARMING));
        unownedIcon.setItemMeta(im);

        im = (PotionMeta) ownedIcon.getItemMeta();
        im.setBasePotionData(new PotionData(PotionType.HARMING));
        ownedIcon.setItemMeta(im);

        im = (PotionMeta) drawnIcon.getItemMeta();
        im.setBasePotionData(new PotionData(PotionType.HARMING));
        drawnIcon.setItemMeta(im);
    }

    @Override
    public void onDefaultPlayerData(Player p) {
        writeData(p.getUniqueId(), "potions", 0L, null);
    }

    @Override
    public void onPlayerData(Player p, Map<String, Object> data) {
        setCharges(p.getUniqueId(), ((Long) data.get("potions")).intValue());
    }

    public void setCharges(UUID uuid, int charges) {
        potions.put(uuid, charges);
        if(charges < MAX_CHARGES)
            addCooldown(uuid);
        else
            removeCooldown(uuid);
    }

    private void addCooldown(UUID uuid) {
        Cooldown cooldown = potionCooldown.get(uuid);
        if(cooldown != null && !cooldown.isFinished())
            return;

        cooldown = new Cooldown(CHARGE_COOLDOWN);
        cooldown.setCompletionTask(() -> incrementCharge(uuid));
        potionCooldown.put(uuid, cooldown);
    }

    private void removeCooldown(UUID uuid) {
        Cooldown cooldown = potionCooldown.remove(uuid);
        if(cooldown != null)
            cooldown.cancelCompletionTask();
    }

    private void incrementCharge(UUID uuid) {
        Player p = Bukkit.getPlayer(uuid); // Will always be non-null.
        int charges = potions.getOrDefault(uuid, 0) + 1;
        setCharges(uuid, charges);

        p.playSound(p.getLocation(), Sound.BLOCK_BREWING_STAND_BREW, 1F, 0.9F);
        p.playSound(p.getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 0.4F, 1F);
        p.sendMessage("§dYou received a charge of " + info.display() + "§d.");
    }

    @Override
    public boolean draw(Player p) {
        boolean toRet = super.draw(p);
        if(toRet) {
            p.getInventory().addItem(SPLASH_POTION_OF_GET_HIS_ASS);
            Runnable callback = () -> {
                int charges = potions.getOrDefault(p.getUniqueId(), 0);
                Cooldown cooldown = potionCooldown.getOrDefault(p.getUniqueId(), new Cooldown(0));
                String msg = charges >= MAX_CHARGES
                        ? "§6Charges §e(§f" + charges + "§e)"
                        : "§6Charges §e(§f" + charges + "§e) §c- " + cooldown.toTimestampString();
                p.sendActionBar(msg);
            };
            HeldSlotListener.getInstance().setCallbacks(p, SPLASH_POTION_OF_GET_HIS_ASS, callback, callback, null);
        }
        return toRet;
    }

    @Override
    public boolean discard(Player p) {
        boolean toRet = super.discard(p);
        if(toRet) {
            removeCooldown(p.getUniqueId());

            p.getInventory().removeItemAnySlot(SPLASH_POTION_OF_GET_HIS_ASS);
            writeData(p.getUniqueId(), "potions", potions.remove(p.getUniqueId()), null);

            HeldSlotListener.getInstance().removeCallbacks(p, SPLASH_POTION_OF_GET_HIS_ASS);
        }
        return toRet;
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onRespawn(PlayerPostRespawnEvent e) {
        Player p = e.getPlayer();
        if(drawn.contains(p) && !p.getInventory().contains(SPLASH_POTION_OF_GET_HIS_ASS))
            p.getInventory().addItem(SPLASH_POTION_OF_GET_HIS_ASS);
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onPotionThrow(PlayerLaunchProjectileEvent e) {
        Player p = e.getPlayer();
        if(drawn.contains(p) && e.getItemStack().isSimilar(SPLASH_POTION_OF_GET_HIS_ASS)) {
            e.setShouldConsume(false);
            int charges = potions.getOrDefault(p.getUniqueId(), 0);
            if(charges > 0) {
                Projectile proj = e.getProjectile();
                proj.setMetadata(METADATA_POTION, new FixedMetadataValue(plugin, true));

                setCharges(p.getUniqueId(), charges - 1);
            } else {
                p.sendMessage("§cYou don't have any charges for that item.");
                e.setCancelled(true);
            }
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onPotionLand(ProjectileHitEvent e) {
        Entity entity = e.getEntity();
        if(entity.hasMetadata(METADATA_POTION)) {
            List<Entity> affected = entity.getNearbyEntities(2.5, 2, 2.5)
                    .stream().filter((i) -> i instanceof LivingEntity).collect(Collectors.toList());
            if(affected.size() > 0) {
                entity.getWorld().playSound(entity.getLocation(), Sound.ENTITY_ILLUSIONER_PREPARE_BLINDNESS, 1F, 1.4F);
                List<Entity> nearbyMobs = entity.getNearbyEntities(30, 30, 30)
                        .stream().filter((i) -> i instanceof Mob).collect(Collectors.toList());
                for(int i=0; i<affected.size(); i++) {
                    LivingEntity target = (LivingEntity) affected.get(i);
                    List<Mob> aggroedOnMe = new ArrayList<>();
                    for(int j=(i*nearbyMobs.size())/affected.size(); j<((i+1)*nearbyMobs.size())/affected.size(); j++) {
                        Mob aggro = (Mob) nearbyMobs.get(j);
                        if(aggro != target) {
                            aggroedOnMe.add(aggro);
                            aggro.setTarget(target);
                            aggro.damage(0, target);
                            aggro.addPotionEffect(new PotionEffect(PotionEffectType.SPEED, 600, 5, true, false));
                            aggro.getWorld().spawnParticle(Particle.ANGRY_VILLAGER, aggro.getLocation().clone().add(0, 2.2, 0),
                                    1, 0F, 0F, 0F, 0.01);
                            aggro.setMetadata(METADATA_ENTITY, new FixedMetadataValue(plugin, target));
                            aggro.setMetadata(METADATA_ENTITY_EXPR, new FixedMetadataValue(plugin, System.currentTimeMillis() + REMAIN_AGGROED_FOR_MILLIS));
                        }
                    }
                    target.setMetadata(METADATA_MY_ASS, new FixedMetadataValue(plugin, aggroedOnMe));
                }
            }
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onPotionDamage(EntityDamageByEntityEvent e) {
        if(e.getDamager().hasMetadata(METADATA_POTION)) {
            e.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onPathFind(EntityPathfindEvent e) {
        if(e.getEntity() instanceof Mob && e.getEntity().hasMetadata(METADATA_ENTITY_EXPR)) {
            Mob attacker = (Mob) e.getEntity();
            long expiration = (long) attacker.getMetadata(METADATA_ENTITY_EXPR).get(0).value();
            LivingEntity target = (LivingEntity) attacker.getMetadata(METADATA_ENTITY).get(0).value();
            if(System.currentTimeMillis() < expiration && target.isValid()) {
                e.setCancelled(true);
                attacker.setTarget(target);
            } else {
                attacker.removeMetadata(METADATA_ENTITY, plugin);
                attacker.removeMetadata(METADATA_ENTITY_EXPR, plugin);
            }
        }
    }

    // Skeletons seem to miss the memo when they kill the entity
    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onEntityDeath(EntityDeathEvent e) {
        LivingEntity le = e.getEntity();
        if(le.hasMetadata(METADATA_MY_ASS)) {
            ((List<Mob>) le.getMetadata(METADATA_MY_ASS).get(0).value())
                    .stream().filter(Entity::isValid).forEach((m) -> {
                        m.removeMetadata(METADATA_ENTITY, plugin);
                        m.removeMetadata(METADATA_ENTITY_EXPR, plugin);
                        m.setTarget(null);
                        m.removePotionEffect(PotionEffectType.SPEED);
                    });
        }
    }

    private static ItemStack getPotionItem() {
        ItemStack is = new ItemStack(Material.SPLASH_POTION);
        PotionMeta im = (PotionMeta) is.getItemMeta();
        im.setBasePotionData(new PotionData(PotionType.HARMING));
        im.setDisplayName(ChatColor.LIGHT_PURPLE + "Splash Potion of Get His Ass");
        List<String> lores = new ArrayList<>();
        lores.add(LOCKED_METADATA_LORE);
        lores.add("");
        lores.add("§7Every §etwo minutes§7, gain a");
        lores.add("§7charge of this §dsplash potion");
        lores.add("§7if you have less than §ethree§7.");
        lores.add("");
        lores.add("§7When §ethrown§7, make all nearby mobs");
        lores.add("§chostile §7towards those affected by it.");
        im.setLore(lores);
        is.setItemMeta(im);
        return is;
    }
}
