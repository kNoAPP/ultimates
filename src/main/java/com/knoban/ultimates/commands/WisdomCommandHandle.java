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

public class WisdomCommandHandle {

    private final Ultimates plugin;

    public WisdomCommandHandle(Ultimates plugin) {
        this.plugin = plugin;

        ACAPI api = ACAPI.getApi();
        api.registerCommandsFromClass(plugin, WisdomCommandHandle.class, this);
    }

    @AtlasCommand(paths = {"wisdom", "wisdom help", "wisdom help 1"}, permission = PermissionConstants.ULTS_WISDOM)
    public void cmdWisdomHelp(CommandSender sender) {
        sender.sendMessage(Message.INFO.getMessage("Wisdom Help"));
        sender.sendMessage(ChatColor.DARK_GREEN + "------------------");
        sender.sendMessage(Message.HELP.getMessage("/wisdom - Show all commands for wisdom"));
        sender.sendMessage(Message.HELP.getMessage("/wisdom view - View your current wisdom"));
        if(sender.hasPermission(PermissionConstants.ULTS_WISDOM)) {
            sender.sendMessage(Message.HELP.getMessage("/wisdom view <player> - View a player's wisdom"));
            sender.sendMessage(Message.HELP.getMessage("/wisdom modify <player> <amt> - Modify a player's wisdom"));
            sender.sendMessage(Message.HELP.getMessage("/wisdom set <player> <amt> - Set a player's wisdom"));
        }
    }

    @AtlasCommand(paths = {"wisdom view"})
    public void cmdWisdomBase(Player sender) {
        CardHolder tch = CardHolder.getCardHolder(sender);
        if(!tch.isLoaded()) {
            sender.sendMessage(CardHolder.UNLOADED_MESSAGE);
            return;
        }

        sender.sendMessage("§7You have §a" + tch.getWisdom() + " wisdom§7.");
    }

    @AtlasCommand(paths = {"wisdom view"}, permission = PermissionConstants.ULTS_WISDOM, classPriority = 1)
    public void cmdWisdomView(CommandSender sender, Player target) {
        CardHolder tch = CardHolder.getCardHolder(target);
        if(!tch.isLoaded()) {
            sender.sendMessage(CardHolder.UNLOADED_MESSAGE);
            return;
        }

        sender.sendMessage("§2" + target.getName() + " §7has §a" + tch.getWisdom() + " wisdom§7.");
    }

    @AtlasCommand(paths = {"wisdom view"}, permission = PermissionConstants.ULTS_WISDOM)
    public void cmdWisdomView(CommandSender sender, String target) {
        sender.sendMessage("§eTask queued! The request will execute soon.");
        OfflineCardHolder.getOfflineCardHolder(Ultimates.getPlugin(), target, (success, tch) -> {
            if(success) {
                sender.sendMessage("§2" + tch.getName() + " §7has §a" + tch.getWisdom() + " wisdom§7.");
            } else
                sender.sendMessage("§4" + target + " §c's data cannot be reached.");
        });
    }

    @AtlasCommand(paths = {"wisdom modify"}, permission = PermissionConstants.ULTS_WISDOM, classPriority = 1)
    public void cmdWisdomModify(CommandSender sender, Player target, int mod) {
        CardHolder tch = CardHolder.getCardHolder(target);
        if(!tch.isLoaded()) {
            sender.sendMessage(CardHolder.UNLOADED_MESSAGE);
            return;
        }

        tch.incrementWisdom(mod);
        sender.sendMessage("§aSuccessfully modified §2" + target.getName() + "'s wisdom §ato: §7" + tch.getWisdom());
    }

    @AtlasCommand(paths = {"wisdom modify"}, permission = PermissionConstants.ULTS_WISDOM)
    public void cmdWisdomModify(CommandSender sender, String target, int mod) {
        sender.sendMessage("§eTask queued! The request will execute soon.");
        OfflineCardHolder.getOfflineCardHolder(Ultimates.getPlugin(), target, -1, (success, tch) -> {
            if(success) {
                tch.setWisdom(tch.getWisdom() + mod);
                sender.sendMessage("§aSuccessfully modified §2" + tch.getName() + "'s wisdom §ato: §7" + tch.getWisdom());
            } else
                sender.sendMessage("§4" + target + " §c's data cannot be reached. Usually means this player is online another Ultimates server.");
        });
    }

    @AtlasCommand(paths = {"wisdom set"}, permission = PermissionConstants.ULTS_WISDOM, classPriority = 1)
    public void cmdWisdomSet(CommandSender sender, Player target, int amt) {
        CardHolder tch = CardHolder.getCardHolder(target);
        if(!tch.isLoaded()) {
            sender.sendMessage(CardHolder.UNLOADED_MESSAGE);
            return;
        }

        tch.setWisdom(amt);
        sender.sendMessage("§aSuccessfully set §2" + target.getName() + "'s wisdom §ato: §7" + tch.getWisdom());
    }

    @AtlasCommand(paths = {"wisdom set"}, permission = PermissionConstants.ULTS_WISDOM)
    public void cmdWisdomSet(CommandSender sender, String target, int amt) {
        sender.sendMessage("§eTask queued! The request will execute soon.");
        OfflineCardHolder.getOfflineCardHolder(Ultimates.getPlugin(), target, -1, (success, tch) -> {
            if(success) {
                tch.setWisdom(amt);
                sender.sendMessage("§aSuccessfully set §2" + tch.getName() + "'s wisdom §ato: §7" + tch.getWisdom());
            } else
                sender.sendMessage("§4" + target + " §c's data cannot be reached. Usually means this player is online another Ultimates server.");
        });
    }
}
