package com.knoban.ultimates.commands;

import com.knoban.atlas.commandsII.ACAPI;
import com.knoban.atlas.commandsII.annotations.AtlasCommand;
import com.knoban.atlas.utils.Tools;
import com.knoban.ultimates.Ultimates;
import com.knoban.ultimates.aspects.Message;
import com.knoban.ultimates.permissions.PermissionConstants;
import org.bukkit.ChatColor;
import org.bukkit.Chunk;
import org.bukkit.World;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class ChunkCommandHandle {

    private final Ultimates plugin;

    public ChunkCommandHandle(Ultimates plugin) {
        this.plugin = plugin;

        ACAPI api = ACAPI.getApi();
        ACAPI.getApi().registerCommandsFromClass(plugin, ChunkCommandHandle.class, this);
    }

    @AtlasCommand(paths = {"chunk", "chunk help", "chunk help 1"}, permission = PermissionConstants.ULTS_CHUNK)
    public void cmdChunkBase(CommandSender sender) {
        sender.sendMessage(Message.INFO.getMessage("Chunk Help"));
        sender.sendMessage(ChatColor.DARK_GREEN + "------------------");
        sender.sendMessage(Message.HELP.getMessage("/chunk - Show all commands for chunks"));
        sender.sendMessage(Message.HELP.getMessage("/chunk preload <range> - Preload chunks from -range to +range"));
    }

    @AtlasCommand(paths = {"chunk preload"}, permission = PermissionConstants.ULTS_CHUNK)
    public void cmdChunkPreload(Player sender, int range) {
        World world = sender.getWorld();
        Chunk center = sender.getChunk();
        int centerX = center.getX();
        int centerZ = center.getZ();

        range = (range / 16) * 16; // Floor range to multiple of 16.
        int chunks = (range / 8) * (range / 8);
        sender.sendMessage("§aBeginning chunk generation of §2" + chunks + " chunks§a!");
        sender.sendMessage("§eThis operation will complete in §6~" + Tools.millisToDHMS(chunks * 50) + "§e.");

        long delay = 0;
        for(int dx=-range; dx<range; dx+=16) {
            for(int dz=-range; dz<range; dz+=16) {
                final int x = centerX+dx;
                final int z = centerZ+dz;
                plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
                    world.getChunkAtAsync(x, z, true);
                }, delay++);

                // Save the world every 1200 generated chunks, about every ~60s.
                if(delay % 1000 == 0) {
                    plugin.getServer().getScheduler().runTaskLater(plugin, world::save, delay);
                }
            }
        }
        plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
            world.save();
            plugin.getServer().getLogger().info("§aChunk generation of §2" + chunks + " chunks §ahas finished!");
            if(sender.isOnline()) {
                sender.sendMessage("§aChunk generation of §2" + chunks + " chunks §ahas finished!");
            }
        }, delay);
    }
}
