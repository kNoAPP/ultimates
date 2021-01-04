package com.knoban.ultimates.commands;

import com.knoban.atlas.commandsII.ACAPI;
import com.knoban.atlas.commandsII.annotations.AtlasCommand;
import com.knoban.atlas.utils.Tools;
import com.knoban.ultimates.Ultimates;
import com.knoban.ultimates.aspects.Message;
import com.knoban.ultimates.cardholder.CardHolder;
import com.knoban.ultimates.cardholder.OfflineCardHolder;
import com.knoban.ultimates.permissions.PermissionConstants;
import org.bukkit.ChatColor;
import org.bukkit.Color;
import org.bukkit.Sound;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class BattlePassCommandHandle {

    private final Ultimates plugin;

    public BattlePassCommandHandle(Ultimates plugin) {
        this.plugin = plugin;

        ACAPI api = ACAPI.getApi();
        ACAPI.getApi().registerCommandsFromClass(plugin, BattlePassCommandHandle.class, this);
    }

    @AtlasCommand(paths = {"battlepass", "bp", "pass",
            "battlepass help", "bp help", "pass help",
            "battlepass help 1", "bp help 1", "pass help 1"})
    public void cmdBattlePassBase(CommandSender sender) {
        sender.sendMessage(Message.INFO.getMessage("Battle Pass Help"));
        sender.sendMessage(ChatColor.DARK_GREEN + "------------------");
        sender.sendMessage(Message.HELP.getMessage("/pass - Show all commands for battle pass"));
        sender.sendMessage(Message.HELP.getMessage("/pass menu - Open the battle pass menu"));
        if(sender.hasPermission(PermissionConstants.ULTS_BATTLEPASS)) {
            sender.sendMessage(Message.HELP.getMessage("/pass premium <player> <T/F> - Grant BP"));
        }
    }

    @AtlasCommand(paths = {"battlepass menu", "bp menu", "pass menu"})
    public void cmdBattlePassMenu(Player player) {
        CardHolder.getCardHolder(player).openBattlePassMainGUI(player, true);
    }

    @AtlasCommand(paths = {"battlepass premium", "bp premium", "pass premium"},
            permission = PermissionConstants.ULTS_BATTLEPASS, classPriority = 1)
    public void cmdBattlePassPremium(CommandSender sender, Player target, boolean premium) {
        CardHolder tch = CardHolder.getCardHolder(target);
        if(!tch.isLoaded()) {
            sender.sendMessage(CardHolder.UNLOADED_MESSAGE);
            return;
        }

        tch.setBattlePass(premium);
        if(premium) {
            target.sendMessage("§f§lYou've unlocked the Battle Pass!");
            target.playSound(target.getLocation(), Sound.ITEM_TOTEM_USE, 1F, 1F);
            Tools.launchFirework(target.getLocation(), Color.RED, 1);
        }

        sender.sendMessage("§aSuccessfully modified §2" + target.getName() + "'s Premium Battle Pass §ato: §7" +
                premium);
    }

    @AtlasCommand(paths = {"battlepass premium", "bp premium", "pass premium"},
            permission = PermissionConstants.ULTS_BATTLEPASS)
    public void cmdBattlePassPremium(CommandSender sender, String target, boolean premium) {
        sender.sendMessage("§eTask queued! The request will execute soon.");
        OfflineCardHolder.getOfflineCardHolder(Ultimates.getPlugin(), target, -1, (success, tch) -> {
            if(success) {
                tch.setBattlePass(premium);

                sender.sendMessage("§aSuccessfully modified §2" + tch.getName() + "'s Premium Battle Pass §ato: §7" +
                        premium);
            } else
                sender.sendMessage("§4" + target + " §c's data cannot be reached.");
        });
    }
}
