package com.knoban.ultimates.tutorial;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.knoban.atlas.claims.Estate;
import com.knoban.ultimates.Ultimates;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockPlaceEvent;

import java.util.UUID;
import java.util.concurrent.TimeUnit;

public class HelperSuggestionsListener implements Listener {

    public static final int SECONDS_BETWEEN_SUGGESTIONS = 7200;

    Cache<UUID, Boolean> createClaimsSuggestion = CacheBuilder.newBuilder()
            .expireAfterWrite(SECONDS_BETWEEN_SUGGESTIONS, TimeUnit.SECONDS).build();

    Cache<UUID, Boolean> addPlayersToClaimSuggestion = CacheBuilder.newBuilder()
            .expireAfterWrite(SECONDS_BETWEEN_SUGGESTIONS, TimeUnit.SECONDS).build();

    private final Ultimates plugin;

    public HelperSuggestionsListener(Ultimates plugin) {
        this.plugin = plugin;

        plugin.getServer().getPluginManager().registerEvents(this, plugin);
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.MONITOR)
    public void onPlace(BlockPlaceEvent e) {
        Player p = e.getPlayer();
        Block b = e.getBlock();
        if(b.getType() == Material.CRAFTING_TABLE
                && createClaimsSuggestion.getIfPresent(p.getUniqueId()) == null) {
            p.sendMessage("");
            p.sendMessage("§b----------§2TIP§b----------");
            p.sendMessage("§eClaiming land protects it from other players! You");
            p.sendMessage("§ecan claim 16x16 chunks of land with the command:");
            p.sendMessage("§6/estate claim");
            p.sendMessage("§b-----------------------");
            p.sendMessage("");
            p.playSound(p.getLocation(), Sound.BLOCK_PISTON_CONTRACT, 1F, 1.9F);
            createClaimsSuggestion.put(p.getUniqueId(), true);
        } else if(b.getType() == Material.CHEST && addPlayersToClaimSuggestion.getIfPresent(p.getUniqueId()) == null) {
            Estate estate = plugin.getLandManager().getEstate(b.getChunk());
            if(estate != null && estate.getOwner() != null && estate.getOwner().equals(p.getUniqueId())) {
                p.sendMessage("");
                p.sendMessage("§b----------§2TIP§b----------");
                p.sendMessage("§eGive players access to your claim with the command:");
                p.sendMessage("§6/estate grant <player> <permission>");
                p.sendMessage("§ePermissions are: §6access / inventory / build / full");
                p.sendMessage("§b-----------------------");
                p.sendMessage("");
                p.playSound(p.getLocation(), Sound.BLOCK_PISTON_CONTRACT, 1F, 1.9F);
                addPlayersToClaimSuggestion.put(p.getUniqueId(), true);
            }
        }
    }
}
