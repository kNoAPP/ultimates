package com.knoban.ultimates.cards.impl;

import com.knoban.atlas.utils.Tools;
import com.knoban.ultimates.Ultimates;
import com.knoban.ultimates.cards.Card;
import com.knoban.ultimates.cards.CardInfo;
import com.knoban.ultimates.primal.PrimalSource;
import com.knoban.ultimates.primal.Tier;
import org.bukkit.Material;
import org.bukkit.entity.Egg;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.entity.ProjectileHitEvent;
import org.bukkit.event.entity.ProjectileLaunchEvent;
import org.bukkit.event.player.PlayerEggThrowEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.metadata.FixedMetadataValue;

import java.util.ArrayList;
import java.util.Random;
import java.util.concurrent.ThreadLocalRandom;

@CardInfo(
        material = Material.DROWNED_SPAWN_EGG,
        name = "collector",
        display = "§9Collector Card", // Typically we want the color to match the Primal
        description = {"§7Throwing an §aegg",
                "§7at a mob has§e a chance",
                "§7to capture the mob."},
        source = PrimalSource.MOON,
        tier = Tier.LEGENDARY
)
public class PokeCard extends Card {

    public static final String METADATA = "ults_poke";

    public PokeCard(Ultimates plugin) {
        super(plugin);
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onEggThrow(ProjectileLaunchEvent e) {
        if(e.getEntity() instanceof Egg egg && egg.getShooter() instanceof Player p && drawn.contains(p)) {
            egg.setMetadata(METADATA, new FixedMetadataValue(plugin, true));
        }
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onEggThrow(PlayerEggThrowEvent e) {
        Egg egg = e.getEgg();
        if(egg.hasMetadata(METADATA)) {
            e.setHatching(false);
        }
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onProjectileHit(ProjectileHitEvent e) {
        if(e.getEntity().hasMetadata(METADATA) && e.getHitEntity() instanceof LivingEntity hit) {
            Material egg = Tools.getSpawnEgg(hit.getType());
            if(egg != null) {
                ItemStack item = new ItemStack(egg);
                ItemMeta im = item.getItemMeta(); // Safe cast since the material is constant.
                ArrayList<String> lore = new ArrayList<>();
                //   im.setDisplayName(e.getHitEntity().getType().name() + " Spawn Egg");
                lore.add("A captured mob lives inside");
                im.setLore(lore);
                item.setItemMeta(im);
                int rand_int1 = ThreadLocalRandom.current().nextInt(4);
                if (rand_int1 == 1) {
                    e.getHitEntity().remove();
                    hit.getWorld().dropItemNaturally(hit.getLocation(), item);
                }
            }
        }
    }
}
//