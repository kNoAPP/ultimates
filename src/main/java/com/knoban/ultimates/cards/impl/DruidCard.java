package com.knoban.ultimates.cards.impl;

import com.knoban.ultimates.Ultimates;
import com.knoban.ultimates.cards.Card;
import com.knoban.ultimates.cards.CardInfo;
import com.knoban.ultimates.primal.PrimalSource;
import com.knoban.ultimates.primal.Tier;
import org.bukkit.Material;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.player.PlayerEggThrowEvent;

import java.util.concurrent.ThreadLocalRandom;

@CardInfo(
        material = Material.TURTLE_EGG,
        name = "druids-curse",
        display = "§aDruid's Curse", // Typically we want the color to match the Primal
        description = {"§7Throwing eggs has", "§7a chance to spawn","§7any mob"},
        source = PrimalSource.EARTH,
        tier = Tier.LEGENDARY
)
public class DruidCard extends Card {

    private static final EntityType[] entityTypeArray = new EntityType[]{EntityType.AXOLOTL,EntityType.BAT,EntityType.BEE,
            EntityType.BLAZE,
            EntityType.CAT,EntityType.CAVE_SPIDER,EntityType.CHICKEN,EntityType.COD,EntityType.COW,EntityType.CREEPER,
            EntityType.DOLPHIN,EntityType.DONKEY,EntityType.ELDER_GUARDIAN,EntityType.ENDERMAN,EntityType.ENDERMITE,
            EntityType.EVOKER,EntityType.FOX,EntityType.GHAST,EntityType.GIANT,EntityType.GOAT,EntityType.GUARDIAN,
            EntityType.HOGLIN,
            EntityType.HORSE, EntityType.HUSK,EntityType.ILLUSIONER,EntityType.IRON_GOLEM,EntityType.LIGHTNING,
            EntityType.LLAMA, EntityType.MULE,EntityType.MUSHROOM_COW,EntityType.MAGMA_CUBE,EntityType.OCELOT,
            EntityType.PANDA, EntityType.PARROT,EntityType.PHANTOM,EntityType.PIG,EntityType.PIGLIN,EntityType.PILLAGER,
            EntityType.POLAR_BEAR,EntityType.PUFFERFISH,EntityType.RABBIT,EntityType.RAVAGER,EntityType.SALMON,
            EntityType.SHEEP,EntityType.SHULKER,EntityType.SILVERFISH,EntityType.SKELETON,EntityType.SKELETON_HORSE,
            EntityType.SLIME,EntityType.SNOWMAN,EntityType.SPIDER,EntityType.SQUID,EntityType.STRAY,EntityType.TRADER_LLAMA,
            EntityType.TROPICAL_FISH,EntityType.TURTLE,EntityType.VEX,EntityType.VILLAGER,EntityType.VINDICATOR,
            EntityType.WANDERING_TRADER,EntityType.WITCH,EntityType.WITHER_SKELETON,EntityType.WOLF,EntityType.ZOGLIN,
            EntityType.ZOMBIFIED_PIGLIN,EntityType.ZOMBIE, EntityType.ZOMBIE_HORSE,EntityType.ZOMBIE_VILLAGER};

    public DruidCard(Ultimates plugin) {
        super(plugin);
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onEggThrow(PlayerEggThrowEvent e) {
        Player p = e.getPlayer();
        if(drawn.contains(p)) {
            e.setHatchingType(entityTypeArray[ThreadLocalRandom.current().nextInt(entityTypeArray.length)]);
        }
    }
}