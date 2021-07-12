package com.knoban.ultimates.commands;

import com.knoban.atlas.commandsII.ACAPI;
import com.knoban.atlas.commandsII.annotations.AtlasCommand;
import com.knoban.ultimates.Ultimates;
import com.knoban.ultimates.aspects.Message;
import com.knoban.ultimates.cardholder.CardHolder;
import com.knoban.ultimates.cardholder.OfflineCardHolder;
import com.knoban.ultimates.cardpack.CardPack;
import com.knoban.ultimates.cardpack.animations.CardPackAnimation;
import com.knoban.ultimates.commands.parsables.CardPackParsable;
import com.knoban.ultimates.permissions.PermissionConstants;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class CardPackCommandHandle {

    private final Ultimates plugin;

    public CardPackCommandHandle(Ultimates plugin) {
        this.plugin = plugin;

        ACAPI api = ACAPI.getApi();
        api.addParser(CardPack.class, new CardPackParsable());
        api.registerCommandsFromClass(plugin, CardPackCommandHandle.class, this);
    }

    @AtlasCommand(paths = {"cardpack", "cardpack help", "cardpack help 1"})
    public void cmdCardpackBase(CommandSender sender) {
        sender.sendMessage(Message.INFO.getMessage("Cardpacks Help"));
        sender.sendMessage(ChatColor.DARK_GREEN + "------------------");
        sender.sendMessage(Message.HELP.getMessage("/cardpack - Show all commands for cardpacks"));
        sender.sendMessage(Message.HELP.getMessage("/cardpack view - View your current cardpacks"));
        sender.sendMessage(Message.HELP.getMessage("/cardpack open <cardpack> - Open your cardpacks"));
        if(sender.hasPermission(PermissionConstants.ULTS_CARDPACK)) {
            sender.sendMessage(Message.HELP.getMessage("/cardpack view <player> - View a player's cardpacks"));
            sender.sendMessage(Message.HELP.getMessage("/cardpack modify <player> <cardpack> <amt> - Modify cardpacks"));
            sender.sendMessage(Message.HELP.getMessage("/cardpack set <player> <cardpack> <amt> - Set cardpacks"));
        }
    }

    @AtlasCommand(paths = {"cardpack view"})
    public void cmdCardpackView(Player sender) {
        CardHolder tch = CardHolder.getCardHolder(sender);
        if(!tch.isLoaded()) {
            sender.sendMessage(CardHolder.UNLOADED_MESSAGE);
            return;
        }

        sender.sendMessage("§bUnopened Cardpacks");
        sender.sendMessage("§2------------------");
        for(CardPack pack : CardPack.values()) {
            sender.sendMessage("§a" + pack.getName() + ": §7" + tch.getOwnedCardPack(pack.ordinal()));
        }
    }

    @AtlasCommand(paths = {"cardpack open"})
    public void cmdCardpackOpen(Player sender, CardPack pack) {
        CardHolder holder = CardHolder.getCardHolder(sender);
        if(!holder.isLoaded()) {
            sender.sendMessage(CardHolder.UNLOADED_MESSAGE);
            return;
        }

        int ownedPacks = holder.getOwnedCardPack(pack.ordinal());
        if(ownedPacks > 0) {
            CardPackAnimation animation = pack.createDefaultAnimation(plugin, sender);
            animation.play();
            holder.incrementOwnedCardPacks(pack.ordinal(), -1);
        } else {
            sender.sendMessage("§cYou don't own any Cardpacks of this type: §4" + pack.getName());
            sender.sendMessage("§7Buy more at: §b" + plugin.getConfigFile().getCachedYML().getString("Store-URL", "https://example.com/store"));
        }
    }


    @AtlasCommand(paths = {"cardpack view"}, permission = PermissionConstants.ULTS_CARDPACK, classPriority = 1)
    public void cmdCardpackView(CommandSender sender, Player target) {
        CardHolder tch = CardHolder.getCardHolder(target);
        if(!tch.isLoaded()) {
            sender.sendMessage(CardHolder.UNLOADED_MESSAGE);
            return;
        }

        sender.sendMessage("§b" + target.getName() + "'s Unopened Cardpacks");
        sender.sendMessage("§2------------------");
        for(CardPack pack : CardPack.values()) {
            sender.sendMessage("§a" + pack.getName() + ": §7" + tch.getOwnedCardPack(pack.ordinal()));
        }
    }

    @AtlasCommand(paths = {"cardpack view"}, permission = PermissionConstants.ULTS_CARDPACK)
    public void cmdCardpackView(CommandSender sender, String target) {
        sender.sendMessage("§eTask queued! The request will execute soon.");
        OfflineCardHolder.getOfflineCardHolder(Ultimates.getPlugin(), target, (success, tch) -> {
            if(success) {
                sender.sendMessage("§b" + tch.getName() + "'s Unopened Cardpacks");
                sender.sendMessage("§2------------------");
                for(CardPack pack : CardPack.values()) {
                    sender.sendMessage("§a" + pack.getName() + ": §7" + tch.getOwnedCardPack(pack.ordinal()));
                }
            } else
                sender.sendMessage("§4" + target + " §c's data cannot be reached.");
        });
    }

    @AtlasCommand(paths = {"cardpack modify"}, permission = PermissionConstants.ULTS_CARDPACK, classPriority = 1)
    public void cmdCardpackModify(CommandSender sender, Player target, CardPack pack, int mod) {
        CardHolder tch = CardHolder.getCardHolder(target);
        if(!tch.isLoaded()) {
            sender.sendMessage(CardHolder.UNLOADED_MESSAGE);
            return;
        }

        tch.incrementOwnedCardPacks(pack.ordinal(), mod);
        sender.sendMessage("§aSuccessfully modified §2" + target.getName() + "'s " +
                pack.name() + " pack §ato: §7" + tch.getOwnedCardPack(pack.ordinal()));
    }

    @AtlasCommand(paths = {"cardpack modify"}, permission = PermissionConstants.ULTS_CARDPACK)
    public void cmdCardpackModify(CommandSender sender, String target, CardPack pack, int mod) {
        sender.sendMessage("§eTask queued! The request will execute soon.");
        OfflineCardHolder.getOfflineCardHolder(Ultimates.getPlugin(), target, -1, (success, tch) -> {
            if(success) {
                tch.setOwnedCardPacks(pack.ordinal(), tch.getOwnedCardPack(pack.ordinal()) + mod);
                sender.sendMessage("§aSuccessfully modified §2" + tch.getName() + "'s " +
                        pack.name() + " pack §ato: §7" + tch.getOwnedCardPack(pack.ordinal()));
            } else
                sender.sendMessage("§4" + target + " §c's data cannot be reached. Usually means this player is online another Ultimates server.");
        });
    }

    @AtlasCommand(paths = {"cardpack set"}, permission = PermissionConstants.ULTS_CARDPACK, classPriority = 1)
    public void cmdCardpackSet(CommandSender sender, Player target, CardPack pack, int amt) {
        CardHolder tch = CardHolder.getCardHolder(target);
        if(!tch.isLoaded()) {
            sender.sendMessage(CardHolder.UNLOADED_MESSAGE);
            return;
        }

        tch.setOwnedCardPacks(pack.ordinal(), amt);
        sender.sendMessage("§aSuccessfully set §2" + target.getName() + "'s " +
                pack.name() + " pack §ato: §7" + tch.getOwnedCardPack(pack.ordinal()));
    }

    @AtlasCommand(paths = {"cardpack set"}, permission = PermissionConstants.ULTS_CARDPACK)
    public void cmdCardpackSet(CommandSender sender, String target, CardPack pack, int amt) {
        sender.sendMessage("§eTask queued! The request will execute soon.");
        OfflineCardHolder.getOfflineCardHolder(Ultimates.getPlugin(), target, -1, (success, tch) -> {
            if(success) {
                tch.setOwnedCardPacks(pack.ordinal(), amt);
                sender.sendMessage("§aSuccessfully set §2" + tch.getName() + "'s" +
                        pack.name() + " pack §ato: §7" + tch.getOwnedCardPack(pack.ordinal()));
            } else
                sender.sendMessage("§4" + target + " §c's data cannot be reached. Usually means this player is online another Ultimates server.");
        });
    }
}
