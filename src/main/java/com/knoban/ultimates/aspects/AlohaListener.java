package com.knoban.ultimates.aspects;

import com.knoban.ultimates.Ultimates;
import com.knoban.ultimates.cardholder.CardHolder;
import com.knoban.ultimates.player.LocalPDStore;
import org.bukkit.Bukkit;
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
        p.sendMessage("§f§lHi " + p.getName() + "! §7Welcome to Sanctums.");
        p.sendMessage("§2You're about to be prompted for a resource pack.");
        p.sendMessage("§2This pack has some custom sounds we use often.");
        p.sendMessage("§2Don't worry, we won't change any of your textures.");
        p.sendMessage("§aFor the best experience, please accept our resource pack!");
        plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
            p.setResourcePack("https://firebasestorage.googleapis.com/v0/b/ultimates-gc.appspot.com/o/knoban.zip?alt=media&token=b3040514-9495-48eb-a15e-d3856341e0e1", "fd90f8051f8ad7f7197bf21758cb8a5484c0e563");
        }, 200L);
        join(p);
    }

    // Remember to also update the main class for onDisable if you're saving crap.
    @EventHandler
    public void onQuit(PlayerQuitEvent e) {
        Player p = e.getPlayer();
        quit(p, false);
    }
}
