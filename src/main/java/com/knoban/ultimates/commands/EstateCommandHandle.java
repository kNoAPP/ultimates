package com.knoban.ultimates.commands;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.knoban.atlas.claims.Estate;
import com.knoban.atlas.claims.EstatePermission;
import com.knoban.atlas.claims.LandManager;
import com.knoban.atlas.claims.Landlord;
import com.knoban.atlas.commandsII.ACAPI;
import com.knoban.atlas.commandsII.annotations.AtlasCommand;
import com.knoban.ultimates.Ultimates;
import com.knoban.ultimates.aspects.Message;
import com.knoban.ultimates.cardholder.CardHolder;
import org.bukkit.*;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.UUID;
import java.util.concurrent.TimeUnit;

public class EstateCommandHandle {

    private final Ultimates plugin;

    private static final int CONFIRMATION_SECONDS = 7;
    private final Cache<UUID, Chunk> confirmationPlayers = CacheBuilder.newBuilder()
            .expireAfterWrite(CONFIRMATION_SECONDS, TimeUnit.SECONDS).build();

    public EstateCommandHandle(Ultimates plugin) {
        this.plugin = plugin;

        ACAPI api = ACAPI.getApi();
        api.registerCommandsFromClass(plugin, EstateCommandHandle.class, this);
    }

    @AtlasCommand(paths = {"estate", "plot", "estate help", "plot help", "estate help 1", "plot help 1"})
    public void cmdEstateBase(CommandSender sender) {
        sender.sendMessage(Message.INFO.getMessage("Estates Help"));
        sender.sendMessage(ChatColor.DARK_GREEN + "------------------");
        sender.sendMessage(Message.HELP.getMessage("/estate - Show all commands for estates"));
        sender.sendMessage(Message.HELP.getMessage("/estate show - Show a 16x16 chunk of land"));
        sender.sendMessage(Message.HELP.getMessage("/estate claim - Claim a 16x16 chunk of land"));
        sender.sendMessage(Message.HELP.getMessage("/estate unclaim - Unclaim a 16x16 chunk of land"));
        sender.sendMessage(Message.HELP.getMessage("/estate unclaim all - Unclaim ALL your land"));
        sender.sendMessage(Message.HELP.getMessage("/estate grant <player> <permission> - Permissions"));
        sender.sendMessage(Message.HELP.getMessage("/estate revoke <player> <permission> - Permissions"));
    }

    @AtlasCommand(paths = {"estate claim", "plot claim"})
    public void cmdEstateClaim(Player sender) {
        LandManager landManager = plugin.getLandManager();
        CardHolder holder = CardHolder.getCardHolder(sender);
        if(!holder.isLoaded()) {
            sender.sendMessage(CardHolder.UNLOADED_MESSAGE);
            return;
        }

        Landlord landlord = holder.asLandlord();

        if(landlord.getOwnedEstates().size() < holder.getMaxEstateClaims()) {
            Chunk chunk = sender.getChunk();
            Estate estate = landManager.getEstate(chunk);
            if(estate == null || estate.getOwner() == null) {
                Chunk selected = confirmationPlayers.getIfPresent(sender.getUniqueId());
                if(selected != null) {
                    if(selected.equals(chunk)) {
                        landManager.setEstate(chunk, sender.getUniqueId(), true);

                        sender.sendMessage("§aChunk claimed! §2Congratulations on the new estate!");
                    } else
                        sender.sendMessage("§cPlease wait for your selection to expire before trying to claim this other" +
                                "chunk!");
                } else {
                    landManager.flashBorder(chunk, Material.GOLD_BLOCK, CONFIRMATION_SECONDS*20L, sender);
                    confirmationPlayers.put(sender.getUniqueId(), chunk);

                    sender.sendMessage("§eYou are about to claim this estate.");
                    sender.sendMessage("§eType §7/estate claim §eagain to confirm and claim.");
                    sender.sendMessage("§cThis selection will expire in " + CONFIRMATION_SECONDS + " seconds.");
                }
            } else
                sender.sendMessage("§cThis estate is claimed by §4"
                        + Bukkit.getOfflinePlayer(estate.getOwner()).getName() + "§c.");
        } else {
            sender.sendMessage("§cYou are out of estate claims! " +
                    "§fUnclaim some land or earn more land claims.");
        }
    }

    @AtlasCommand(paths = {"estate unclaim", "plot unclaim"})
    public void cmdEstateUnclaim(Player sender) {
        LandManager landManager = plugin.getLandManager();
        // Landlord landlord = landManager.getLandlord(sender.getUniqueId());
        Chunk chunk = sender.getChunk();
        Estate estate = landManager.getEstate(chunk);

        if(estate != null && estate.getOwner() != null && estate.getOwner().equals(sender.getUniqueId())) {
            Chunk selected = confirmationPlayers.getIfPresent(sender.getUniqueId());
            if(selected != null) {
                if(selected.equals(chunk)) {
                    landManager.removeEstate(selected);

                    sender.sendMessage("§aEstate unclaimed successfully.");
                } else
                    sender.sendMessage("§cPlease wait for your selection to expire before trying to unclaim this other"
                            + "chunk!");
            } else {
                landManager.flashBorder(chunk, Material.REDSTONE_BLOCK, CONFIRMATION_SECONDS*20L, sender);
                confirmationPlayers.put(sender.getUniqueId(), chunk);

                sender.sendMessage("§eYou are about to unclaim this estate.");
                sender.sendMessage("§eType §7/estate unclaim §eagain to confirm and unclaim.");
                sender.sendMessage("§cThis selection will expire in " + CONFIRMATION_SECONDS + " seconds.");
            }
        } else {
            sender.sendMessage("§cYou do not own this estate!");
        }
    }

    @AtlasCommand(paths = {"estate unclaim all", "plot unclaim all"})
    public void cmdEstateUnclaimAll(Player sender) {
        LandManager landManager = plugin.getLandManager();
        Landlord landlord = landManager.getLandlord(sender.getUniqueId());
        Chunk chunk = sender.getChunk();

        Chunk selected = confirmationPlayers.getIfPresent(sender.getUniqueId());
        if(selected != null) {
            for(Estate estate : landlord.getOwnedEstates())
                landManager.removeEstate(estate.getChunk());

            sender.sendMessage("§aAll estates unclaimed successfully.");
        } else {
            confirmationPlayers.put(sender.getUniqueId(), chunk);

            sender.sendMessage("§eYou are about to unclaim all your estates.");
            sender.sendMessage("§eType §7/estate unclaim all §eagain to confirm and unclaim.");
            sender.sendMessage("§cThis selection will expire in " + CONFIRMATION_SECONDS + " seconds.");
        }
    }

    @AtlasCommand(paths = {"estate show", "plot show"})
    public void cmdEstateShow(Player sender) {
        LandManager landManager = Ultimates.getPlugin().getLandManager();
        Estate estate = landManager.getEstate(sender.getChunk());

        if(confirmationPlayers.getIfPresent(sender.getUniqueId()) == null) {
            confirmationPlayers.put(sender.getUniqueId(), sender.getChunk());
            landManager.flashBorder(sender.getChunk(), Material.EMERALD_BLOCK, CONFIRMATION_SECONDS * 20, sender);
            if(estate != null && estate.getOwner() != null) {
                sender.sendMessage("§eThis chunk is claimed by §6"
                        + Bukkit.getOfflinePlayer(estate.getOwner()).getName() + "§e.");
                if(sender.getUniqueId().equals(estate.getOwner()))
                    sender.sendMessage("§eYou can unclaim this estate with §7/estate unclaim§e.");
            } else
                sender.sendMessage("§eThis estate is §7unclaimed§e.");
        } else {
            sender.sendMessage("§cYou are running this command too quickly!");
            sender.sendMessage("§6TIP! §eYou can see estate borders live by pressing F3+G.");
            sender.sendMessage("§eThen, press F3+G to exit the live borders view.");
        }
    }

    @AtlasCommand(paths = {"estate grant", "plot grant"})
    public void cmdEstateGrant(Player sender, String targetName, EstatePermission permission) {
        if(sender.getName().equalsIgnoreCase(targetName)) {
            sender.sendMessage("§7Nice try silly...");
            return;
        }

        LandManager landManager = plugin.getLandManager();
        Landlord landlord = landManager.getLandlord(sender.getUniqueId());

        // We need a thread because Bukkit.getOfflinePlayer() sometimes blocks.
        new Thread(() -> {
            OfflinePlayer target = Bukkit.getOfflinePlayer(targetName);
            String tname = target.getName();
            UUID tuuid = target.getUniqueId();

            for(Estate estate : landlord.getOwnedEstates())
                estate.addPermission(tuuid, permission.getCode()); // I've made estates Thread-safe

            sender.sendMessage("§aSuccessfully granted §2" + tname + "'s §apermission: §7"
                    + permission.name().toUpperCase());
        }).start();
    }

    @AtlasCommand(paths = {"estate revoke", "plot revoke"})
    public void cmdEstateRevoke(Player sender, String targetName, EstatePermission permission) {
        if(sender.getName().equalsIgnoreCase(targetName)) {
            sender.sendMessage("§7Nice try silly...");
            return;
        }

        LandManager landManager = plugin.getLandManager();
        Landlord landlord = landManager.getLandlord(sender.getUniqueId());

        // We need a thread because Bukkit.getOfflinePlayer() sometimes blocks.
        new Thread(() -> {
            OfflinePlayer target = Bukkit.getOfflinePlayer(targetName);
            String tname = target.getName();
            UUID tuuid = target.getUniqueId();

            for(Estate estate : landlord.getOwnedEstates())
                estate.removePermission(tuuid, permission.getCode()); // I've made estates Thread-safe

            sender.sendMessage("§aSuccessfully revoked §2" + tname + "'s §apermission: §7"
                    + permission.name().toUpperCase());
        }).start();
    }
}
