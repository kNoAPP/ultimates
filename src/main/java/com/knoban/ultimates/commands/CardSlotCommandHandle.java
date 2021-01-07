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

public class CardSlotCommandHandle {

    private final Ultimates plugin;

    public CardSlotCommandHandle(Ultimates plugin) {
        this.plugin = plugin;

        ACAPI api = ACAPI.getApi();
        ACAPI.getApi().registerCommandsFromClass(plugin, CardSlotCommandHandle.class, this);
    }

    @AtlasCommand(paths = {"cardslot", "cardslot help", "cardslot help 1"})
    public void cmdCardSlotBase(CommandSender sender) {
        sender.sendMessage(Message.INFO.getMessage("Card Slots Help"));
        sender.sendMessage(ChatColor.DARK_GREEN + "------------------");
        sender.sendMessage(Message.HELP.getMessage("/cardslot - Show all commands for card slots"));
        sender.sendMessage(Message.HELP.getMessage("/cardslot view - View your current max card slots"));
        if(sender.hasPermission(PermissionConstants.ULTS_CARDSLOT)) {
            sender.sendMessage(Message.HELP.getMessage("/cardslot view <player> - View a player's max card slots"));
            sender.sendMessage(Message.HELP.getMessage("/cardslot modify <player> <amt> - Modify a player's max card slots"));
            sender.sendMessage(Message.HELP.getMessage("/cardslot set <player> <amt> - Set a player's max card slots"));
        }
    }

    @AtlasCommand(paths = {"cardslot view"})
    public void cmdCardSlotView(Player sender) {
        CardHolder tch = CardHolder.getCardHolder(sender);
        if(!tch.isLoaded()) {
            sender.sendMessage(CardHolder.UNLOADED_MESSAGE);
            return;
        }

        sender.sendMessage("§7You can equip up to §a" + tch.getMaxCardSlots() + " cards §7at one time.");
    }

    @AtlasCommand(paths = {"cardslot view"}, permission = PermissionConstants.ULTS_CARDSLOT, classPriority = 1)
    public void cmdCardSlotView(CommandSender sender, Player target) {
        CardHolder tch = CardHolder.getCardHolder(target);
        if(!tch.isLoaded()) {
            sender.sendMessage(CardHolder.UNLOADED_MESSAGE);
            return;
        }

        sender.sendMessage("§2" + target.getName() + " §7can equip up to §a" + tch.getMaxCardSlots() + " cards §7at one time.");
    }

    @AtlasCommand(paths = {"cardslot view"}, permission = PermissionConstants.ULTS_CARDSLOT)
    public void cmdCardSlotView(CommandSender sender, String target) {
        sender.sendMessage("§eTask queued! The request will execute soon.");
        OfflineCardHolder.getOfflineCardHolder(Ultimates.getPlugin(), target, (success, tch) -> {
            if(success) {
                sender.sendMessage("§2" + tch.getName() + " §7can equip up to §a" + tch.getMaxCardSlots() + " cards §7at one time.");
            } else
                sender.sendMessage("§4" + target + " §c's data cannot be reached.");
        });
    }

    @AtlasCommand(paths = {"cardslot modify"}, permission = PermissionConstants.ULTS_CARDSLOT, classPriority = 1)
    public void cmdCardSlotModify(CommandSender sender, Player target, int mod) {
        CardHolder tch = CardHolder.getCardHolder(target);
        if(!tch.isLoaded()) {
            sender.sendMessage(CardHolder.UNLOADED_MESSAGE);
            return;
        }

        tch.incrementMaxCardSlots(mod);
        sender.sendMessage("§aSuccessfully modified §2" + target.getName() + "'s card slots §ato: §7" + tch.getMaxCardSlots());
    }

    @AtlasCommand(paths = {"cardslot modify"}, permission = PermissionConstants.ULTS_CARDSLOT)
    public void cmdCardSlotModify(CommandSender sender, String target, int mod) {
        sender.sendMessage("§eTask queued! The request will execute soon.");
        OfflineCardHolder.getOfflineCardHolder(Ultimates.getPlugin(), target, -1, (success, tch) -> {
            if(success) {
                tch.setMaxCardSlots(tch.getMaxCardSlots() + mod);
                sender.sendMessage("§aSuccessfully modified §2" + tch.getName() + "'s cards slots §ato: §7" + tch.getMaxCardSlots());
            } else
                sender.sendMessage("§4" + target + " §c's data cannot be reached. Usually means this player is online another Ultimates server.");
        });
    }

    @AtlasCommand(paths = {"cardslot set"}, permission = PermissionConstants.ULTS_CARDSLOT, classPriority = 1)
    public void cmdCardSlotSet(CommandSender sender, Player target, int amt) {
        CardHolder tch = CardHolder.getCardHolder(target);
        if(!tch.isLoaded()) {
            sender.sendMessage(CardHolder.UNLOADED_MESSAGE);
            return;
        }

        tch.setMaxCardSlots(amt);
        sender.sendMessage("§aSuccessfully set §2" + target.getName() + "'s cards slots §ato: §7" + tch.getMaxCardSlots());
    }

    @AtlasCommand(paths = {"cardslot set"}, permission = PermissionConstants.ULTS_CARDSLOT)
    public void cmdCardSlotSet(CommandSender sender, String target, int amt) {
        sender.sendMessage("§eTask queued! The request will execute soon.");
        OfflineCardHolder.getOfflineCardHolder(Ultimates.getPlugin(), target, -1, (success, tch) -> {
            if(success) {
                tch.setMaxCardSlots(amt);
                sender.sendMessage("§aSuccessfully set §2" + tch.getName() + "'s cards slots §ato: §7" + tch.getMaxCardSlots());
            } else
                sender.sendMessage("§4" + target + " §c's data cannot be reached. Usually means this player is online another Ultimates server.");
        });
    }
}
