package com.knoban.ultimates.aspects;

import com.knoban.ultimates.Ultimates;
import com.knoban.ultimates.cardholder.CardHolder;
import com.knoban.ultimates.player.LocalPDStore;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;

public class AlohaListener implements Listener {

    private final Ultimates plugin;

    public AlohaListener(Ultimates plugin) {
        this.plugin = plugin;

        plugin.getServer().getPluginManager().registerEvents(this, plugin);
    }

    public void join(Player p) {
        CardHolder.getNewCardHolder(plugin, p).login();
        LocalPDStore store = plugin.getPlayerDataStore().getPlayerDataStore(p);
        if(store.getFreeRespawns() > 0)
            p.sendMessage(Message.RESPAWN.getMessage("You have " + store.getFreeRespawns() + " respawn(s) left!"));
    }

    public void quit(Player p, boolean shutdown) {
        CardHolder.getCardHolder(p).logout(shutdown);
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent e) {
        Player p = e.getPlayer();
        join(p);
    }

    // Remember to also update the main class for onDisable if you're saving crap.
    @EventHandler
    public void onQuit(PlayerQuitEvent e) {
        Player p = e.getPlayer();
        quit(p, false);
    }
}
