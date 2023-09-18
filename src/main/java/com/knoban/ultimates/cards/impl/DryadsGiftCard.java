package com.knoban.ultimates.cards.impl;

import com.knoban.ultimates.Ultimates;
import com.knoban.ultimates.cards.Card;
import com.knoban.ultimates.cards.CardInfo;
import com.knoban.ultimates.primal.PrimalSource;
import com.knoban.ultimates.primal.Tier;
import org.bukkit.GameMode;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.metadata.FixedMetadataValue;

import java.util.EnumSet;
import java.util.Random;

@CardInfo(
        material = Material.OAK_LOG,
        name = "dryads-gift",
        display = "§aDryad's Gift", // Typically we want the color to match the Primal
        description = {"§7You can gain §eVanilla Exp", "§7from breaking logs"},
        source = PrimalSource.EARTH,
        tier = Tier.COMMON)
public class DryadsGiftCard extends Card {

    private static final EnumSet<Material> WOODS = EnumSet.of(
            Material.OAK_LOG, Material.SPRUCE_LOG, Material.BIRCH_LOG, Material.JUNGLE_LOG, Material.ACACIA_LOG,
            Material.DARK_OAK_LOG, Material.WARPED_STEM, Material.CRIMSON_STEM
    );

    private final Random RANDOM = new Random();

    public DryadsGiftCard(Ultimates plugin) {
        super(plugin);
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onWoodBreakEvent(BlockBreakEvent e) {
        Player p = e.getPlayer();

        if(drawn.contains(p) && p.getGameMode() != GameMode.CREATIVE && WOODS.contains(e.getBlock().getType())
                && !e.getBlock().hasMetadata("is-placed")) {
            if(RANDOM.nextInt(10) <= 3) {
                e.setExpToDrop(RANDOM.nextInt(4) + 1);
            }
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onWoodPlaceEvent(BlockPlaceEvent blockPlaceEvent) {
        if(WOODS.contains(blockPlaceEvent.getBlock().getType())) {
            blockPlaceEvent.getBlock().getState().setMetadata("is-placed", new FixedMetadataValue(Ultimates.getPlugin(), true));
        }
    }
}