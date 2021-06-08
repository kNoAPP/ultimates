package com.knoban.ultimates.commands;

import com.knoban.atlas.commandsII.ACAPI;
import com.knoban.atlas.commandsII.annotations.AtlasCommand;
import com.knoban.ultimates.Ultimates;
import com.knoban.ultimates.aspects.Message;
import com.knoban.ultimates.cardholder.CardHolder;
import com.knoban.ultimates.cardholder.OfflineCardHolder;
import com.knoban.ultimates.permissions.PermissionConstants;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class LevelCommandHandle {

    private final Ultimates plugin;

    public LevelCommandHandle(Ultimates plugin) {
        this.plugin = plugin;

        ACAPI api = ACAPI.getApi();
        api.registerCommandsFromClass(plugin, LevelCommandHandle.class, this);
    }

    @AtlasCommand(paths = {"level", "level help", "level help 1"})
    public void cmdLevelBase(CommandSender sender) {
        sender.sendMessage(Message.INFO.getMessage("Levels Help"));
        sender.sendMessage(ChatColor.DARK_GREEN + "------------------");
        sender.sendMessage(Message.HELP.getMessage("/level - Show all commands for levels"));
        sender.sendMessage(Message.HELP.getMessage("/level view - View your current level"));
        if(sender.hasPermission(PermissionConstants.ULTS_LEVEL)) {
            sender.sendMessage(Message.HELP.getMessage("/level view <player> - View a player's level"));
            sender.sendMessage(Message.HELP.getMessage("/level modify <player> <amt> - Modify a player's level"));
            sender.sendMessage(Message.HELP.getMessage("/level set <player> <amt> - Set a player's level"));
        }
    }

    @AtlasCommand(paths = {"level view"})
    public void cmdLevelView(Player sender) {
        CardHolder tch = CardHolder.getCardHolder(sender);
        if(!tch.isLoaded()) {
            sender.sendMessage(CardHolder.UNLOADED_MESSAGE);
            return;
        }

        sender.sendMessage("§7You are §alevel " + tch.getLevel() + " §7(" + tch.getXp() + " xp).");
    }

    @AtlasCommand(paths = {"level view"}, permission = PermissionConstants.ULTS_LEVEL, classPriority = 1)
    public void cmdLevelView(CommandSender sender, Player target) {
        CardHolder tch = CardHolder.getCardHolder(target);
        if(!tch.isLoaded()) {
            sender.sendMessage(CardHolder.UNLOADED_MESSAGE);
            return;
        }

        sender.sendMessage("§2" + target.getName() + " §7is §alevel " + tch.getLevel() + " §7(" + tch.getXp() + " xp).");
    }

    @AtlasCommand(paths = {"level view"}, permission = PermissionConstants.ULTS_LEVEL)
    public void cmdLevelView(CommandSender sender, String target) {
        sender.sendMessage("§eTask queued! The request will execute soon.");
        OfflineCardHolder.getOfflineCardHolder(Ultimates.getPlugin(), target, (success, tch) -> {
            if(success) {
                sender.sendMessage("§2" + tch.getName() + " §7is §alevel " + tch.getLevel() + " §7(" + tch.getXp() + " xp).");
            } else
                sender.sendMessage("§4" + target + " §c's data cannot be reached.");
        });
    }

    @AtlasCommand(paths = {"level modify"}, permission = PermissionConstants.ULTS_LEVEL, classPriority = 1)
    public void cmdLevelModify(CommandSender sender, Player target, int mod) {
        CardHolder tch = CardHolder.getCardHolder(target);
        if(!tch.isLoaded()) {
            sender.sendMessage(CardHolder.UNLOADED_MESSAGE);
            return;
        }

        tch.incrementLevel(mod);
        sender.sendMessage("§aSuccessfully modified §2" + target.getName() + "'s level §ato: §7" + tch.getLevel());
    }

    @AtlasCommand(paths = {"level modify"}, permission = PermissionConstants.ULTS_LEVEL)
    public void cmdLevelModify(CommandSender sender, String target, int mod) {
        sender.sendMessage("§eTask queued! The request will execute soon.");
        OfflineCardHolder.getOfflineCardHolder(Ultimates.getPlugin(), target, -1, (success, tch) -> {
            if(success) {
                tch.setLevel(tch.getLevel() + mod);
                sender.sendMessage("§aSuccessfully modified §2" + tch.getName() + "'s level §ato: §7" + tch.getLevel());
            } else
                sender.sendMessage("§4" + target + " §c's data cannot be reached. Usually means this player is online another Ultimates server.");
        });
    }

    @AtlasCommand(paths = {"level set"}, permission = PermissionConstants.ULTS_LEVEL, classPriority = 1)
    public void cmdLevelSet(CommandSender sender, Player target, int amt) {
        CardHolder tch = CardHolder.getCardHolder(target);
        if(!tch.isLoaded()) {
            sender.sendMessage(CardHolder.UNLOADED_MESSAGE);
            return;
        }

        tch.setLevel(amt);
        sender.sendMessage("§aSuccessfully set §2" + target.getName() + "'s level §ato: §7" + tch.getLevel());
    }

    @AtlasCommand(paths = {"level set"}, permission = PermissionConstants.ULTS_LEVEL)
    public void cmdLevelSet(CommandSender sender, String target, int amt) {
        sender.sendMessage("§eTask queued! The request will execute soon.");
        OfflineCardHolder.getOfflineCardHolder(Ultimates.getPlugin(), target, -1, (success, tch) -> {
            if(success) {
                tch.setLevel(amt);
                sender.sendMessage("§aSuccessfully set §2" + tch.getName() + "'s level §ato: §7" + tch.getLevel());
            } else
                sender.sendMessage("§4" + target + " §c's data cannot be reached. Usually means this player is online another Ultimates server.");
        });
    }
}
