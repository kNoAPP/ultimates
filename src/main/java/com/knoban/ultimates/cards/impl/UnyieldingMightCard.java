package com.knoban.ultimates.cards.impl;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.knoban.atlas.utils.Cooldown;
import com.knoban.ultimates.Ultimates;
import com.knoban.ultimates.cards.Card;
import com.knoban.ultimates.cards.CardInfo;
import com.knoban.ultimates.primal.PrimalSource;
import com.knoban.ultimates.primal.Tier;
import org.bukkit.Effect;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import java.util.UUID;
import java.util.concurrent.TimeUnit;

@CardInfo(
        material = Material.CHAINMAIL_HELMET,
        name = "unyielding-might",
        display = "§6Unyielding Might", // Typically we want the color to match the Primal
        description = {"§7Upon taking significant damage", "§7gain an §5absorption effect", "§7that absorbs additional blows."},
        source = PrimalSource.SUN,
        tier = Tier.RARE
)
public class UnyieldingMightCard extends Card {


    private static final int CRITICAL_HEALTH = 6;
    private static final int COOLDOWN_SECONDS = 120;
    private Cache<UUID, Cooldown> used = CacheBuilder.newBuilder()
            .expireAfterWrite(COOLDOWN_SECONDS, TimeUnit.SECONDS)
            .build();

    public UnyieldingMightCard(Ultimates plugin) {
        super(plugin);
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onDamage(EntityDamageEvent e) {
        Entity entity = e.getEntity();
        if(entity instanceof Player && drawn.contains(entity)) {
            Player p = (Player) entity;
            if(p.getHealth() > CRITICAL_HEALTH && p.getHealth() - e.getFinalDamage() <= CRITICAL_HEALTH) {
                Cooldown cooldown = used.getIfPresent(p.getUniqueId());
                if(cooldown != null) {
                    p.sendMessage(info.display() + " §cis still on cooldown for " + cooldown.toTimestampString() + ".");
                    return;
                }

                cooldown = new Cooldown(COOLDOWN_SECONDS*1000);
                cooldown.setCompletionTask(() -> {
                    if(drawn.contains(p)) {
                        p.sendMessage(info.display() + " §ais ready!");
                    }
                });
                used.put(p.getUniqueId(), cooldown);

                p.addPotionEffect(new PotionEffect(PotionEffectType.ABSORPTION, 200, 2));
                p.sendMessage("§6You've triggered " + info.display() + "§6!");
                p.getWorld().playSound(p.getLocation(), Sound.ENTITY_EVOKER_CAST_SPELL, 1F, 1.7F);
                p.getWorld().playEffect(p.getLocation().clone().add(0, 1, 0), Effect.STEP_SOUND, Material.GOLD_BLOCK);
            }
        }
    }
}