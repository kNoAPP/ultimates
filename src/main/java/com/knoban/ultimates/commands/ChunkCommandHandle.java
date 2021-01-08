package com.knoban.ultimates.commands;

import com.knoban.atlas.commandsII.ACAPI;
import com.knoban.atlas.commandsII.annotations.AtlasCommand;
import com.knoban.atlas.commandsII.annotations.AtlasParam;
import com.knoban.atlas.utils.Tools;
import com.knoban.ultimates.Ultimates;
import com.knoban.ultimates.aspects.Message;
import com.knoban.ultimates.permissions.PermissionConstants;
import org.bukkit.*;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitRunnable;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.UUID;

public class ChunkCommandHandle {

    private final Ultimates plugin;

    public ChunkCommandHandle(Ultimates plugin) {
        this.plugin = plugin;

        ACAPI api = ACAPI.getApi();
        api.registerCommandsFromClass(plugin, ChunkCommandHandle.class, this);
    }

    @AtlasCommand(paths = {"chunk", "chunk help", "chunk help 1"}, permission = PermissionConstants.ULTS_CHUNK)
    public void cmdChunkBase(CommandSender sender) {
        sender.sendMessage(Message.INFO.getMessage("Chunk Help"));
        sender.sendMessage(ChatColor.DARK_GREEN + "------------------");
        sender.sendMessage(Message.HELP.getMessage("/chunk - Show all commands for chunks"));
        sender.sendMessage(Message.HELP.getMessage("/chunk preload <range in chunks> - Preload chunks"));
        sender.sendMessage(Message.HELP.getMessage("/chunk status - Check chunk loading status"));
        sender.sendMessage(Message.HELP.getMessage("/chunk cancel - Cancel the ongoing task"));
    }

    private final HashMap<UUID, ChunkGenerationTask> generations = new HashMap<>();
    @AtlasCommand(paths = {"chunk preload"}, permission = PermissionConstants.ULTS_CHUNK)
    public void cmdChunkPreload(Player sender, @AtlasParam(filter = "min:0") int range) {
        World world = sender.getWorld();
        Chunk center = sender.getChunk();
        UUID uuid = sender.getUniqueId();

        ChunkGenerationTask chunkGenerationTask = generations.get(uuid);
        if(chunkGenerationTask != null && chunkGenerationTask.getPercentComplete() < 1.0f) {
            sender.sendMessage("§cA chunk task is already ongoing! §7Try /chunk cancel.");
            return;
        }
        chunkGenerationTask = new ChunkGenerationTask(world, center.getX(), center.getZ(), range);
        chunkGenerationTask.setCallback(() -> {
            Player player = Bukkit.getPlayer(uuid);
            if(player != null && player.isOnline()) {
                player.sendMessage("§2Good news! §7The chunk generation has completed.");
                player.playSound(player.getLocation(), Sound.ITEM_LODESTONE_COMPASS_LOCK, 1f, 1f);
            }
            generations.remove(uuid);
        });
        generations.put(uuid, chunkGenerationTask);
        chunkGenerationTask.start(plugin);
        sender.sendMessage("§aBeginning chunk generation of §2" + 4*range*range + " chunks§a!");
        sender.sendMessage("§eThis operation will complete in §6~" + Tools.millisToDHMS(chunkGenerationTask.getTimeLeftInMillis()) + "§e.");
    }

    @AtlasCommand(paths = {"chunk status"}, permission = PermissionConstants.ULTS_CHUNK)
    public void cmdChunkStatus(Player sender) {
        ChunkGenerationTask chunkGenerationTask = generations.get(sender.getUniqueId());
        if(chunkGenerationTask == null || chunkGenerationTask.getPercentComplete() >= 1.0f) {
            sender.sendMessage("§6Status: §2Complete!");
            return;
        }
        sender.sendMessage("§6Status: §a" + String.format("%.2f", chunkGenerationTask.getPercentComplete()*100f) + "% §7- §5" + Tools.millisToDHMS(chunkGenerationTask.getTimeLeftInMillis()));
    }

    @AtlasCommand(paths = {"chunk cancel"}, permission = PermissionConstants.ULTS_CHUNK)
    public void cmdChunkCancel(Player sender) {
        ChunkGenerationTask chunkGenerationTask = generations.get(sender.getUniqueId());
        if(chunkGenerationTask == null || chunkGenerationTask.getPercentComplete() >= 1.0f) {
            sender.sendMessage("§cNo ongoing task to cancel.");
            return;
        }
        sender.sendMessage("§aThe current task has been cancelled!");
        chunkGenerationTask.cancel();
        generations.remove(sender.getUniqueId());
    }

    /**
     * Pre-generates chunks to improve server speed post-generation.
     * @author Alden Bansemer (kNoAPP)
     */
    private static final class ChunkGenerationTask extends BukkitRunnable {

        private final World world;
        private final int range;
        private final int centerX, centerZ;
        private int i, dx, dz;

        private Runnable callback;

        /**
         * Generate part of a world's chunks in an async matter. Improves server speed in areas where pre-generation
         * has occurred.
         * @param world The world subject to generation
         * @param centerX The x-coordinate of the center of the generation
         * @param centerZ The z-coordinate of the center of the generation
         * @param range The number of chunks in radius around the center to generate
         */
        private ChunkGenerationTask(@NotNull World world, int centerX, int centerZ, int range) {
            this.world = world;
            this.range = range;
            this.centerX = centerX;
            this.centerZ = centerZ;
            this.i = 0;
            this.dx = -range;
            this.dz = -range;
        }

        @Override
        public void run() {
            if(dx >= range) {
                world.save();
                this.cancel();

                if(callback != null)
                    callback.run();
                return;
            }

            if(dz >= range) {
                dz = -range;
                ++dx;
            }

            Chunk c = world.getChunkAt(centerX+(dx*16), centerZ+(dz*16)); // Generate chunk
            c.load(true);
            if(i % 100 == 0) { // Save world every 100 chunks just in case.
                world.save();
            }

            ++dz;
            ++i;
        }

        /**
         * This callback will be made on the game thread when chunk generation completes. The callback will
         * not be made if the task is cancelled.
         * @param callback The callback to make
         */
        public void setCallback(@Nullable Runnable callback) {
            this.callback = callback;
        }

        /**
         * Start generating chunks (1 per tick). This may only be called once
         * @param pl The plugin scheduling the task
         */
        public void start(@NotNull Plugin pl) {
            this.runTaskTimer(pl, 0L, 1L);
        }

        /**
         * @return A percentage 0.0f to 1.0f of how complete the task is
         */
        public float getPercentComplete() {
            return (float)i / (4f*range*range);
        }

        /**
         * @return The number of milliseconds projected to remain in the task
         */
        public long getTimeLeftInMillis() {
            return (4L*range*range - i) * 50L;
        }
    }
}
