package com.knoban.ultimates.commands;

import com.knoban.atlas.commandsII.ACAPI;
import com.knoban.atlas.commandsII.annotations.AtlasCommand;
import com.knoban.ultimates.Ultimates;
import com.knoban.ultimates.aspects.Message;
import com.knoban.ultimates.cardholder.CardHolder;
import com.knoban.ultimates.cardholder.OfflineCardHolder;
import com.knoban.ultimates.cards.Card;
import com.knoban.ultimates.cards.Cards;
import com.knoban.ultimates.commands.parsables.CardParsable;
import com.knoban.ultimates.permissions.PermissionConstants;
import net.kyori.adventure.text.Component;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class CardCommandHandle {

    private final Ultimates plugin;

    public CardCommandHandle(Ultimates plugin) {
        this.plugin = plugin;

        ACAPI api = ACAPI.getApi();
        api.addParser(Card.class, new CardParsable());
        api.registerCommandsFromClass(plugin, CardCommandHandle.class, this);
    }

    @AtlasCommand(paths = {"card", "card help", "card help 1"})
    public void cmdCardBase(CommandSender sender) {
        sender.sendMessage(Message.INFO.getMessage("Cards Help"));
        sender.sendMessage(ChatColor.DARK_GREEN + "------------------");
        sender.sendMessage(Message.HELP.getMessage("/card - Show all commands for cards"));
        sender.sendMessage(Message.HELP.getMessage("/card menu - Open card draw/discard menu"));
        if(sender.hasPermission(PermissionConstants.ULTS_CARD_GRANT)) {
            sender.sendMessage(Message.HELP.getMessage("/card grant <player> <id> - Give a player a card"));
            sender.sendMessage(Message.HELP.getMessage("/card revoke <player> <id> - Remove a player a card"));
        }
    }

    @AtlasCommand(paths = {"card grant"}, permission = PermissionConstants.ULTS_CARD_GRANT, classPriority = 1)
    public void cmdCardGrant(CommandSender sender, Player target, Card card) {
        CardHolder tch = CardHolder.getCardHolder(target);
        if(!tch.isLoaded()) {
            sender.sendMessage(CardHolder.UNLOADED_MESSAGE);
            return;
        }

        if(tch.grantCards(card)) {
            sender.sendMessage("§aSuccessfully gave §2" + target.getName() + " §athe card: " + card.getInfo().display());
        } else {
            sender.sendMessage("§cPlayer §4" + target.getName() + " §calready has card: " + card.getInfo().display());
        }
    }

    @AtlasCommand(paths = {"card grant <?> all"}, permission = PermissionConstants.ULTS_CARD_GRANT, classPriority = 1)
    public void cmdCardGrant(CommandSender sender, Player target) {
        for(Card card : Cards.getInstance().getCardInstances()) {
            cmdCardGrant(sender, target, card);
        }
    }

    @AtlasCommand(paths = {"card grant"}, permission = PermissionConstants.ULTS_CARD_GRANT)
    public void cmdCardGrant(CommandSender sender, String target, Card card) {
        OfflineCardHolder.getOfflineCardHolder(plugin, target, -1, (success, tch) -> {
            if(success) {
                if(tch.grantCards(card)) {
                    sender.sendMessage("§aSuccessfully gave §2" + tch.getName() + " §athe card: " + card.getInfo().display());
                } else {
                    sender.sendMessage("§cPlayer §4" + tch.getName() + " §calready has card: " + card.getInfo().display());
                }
            } else
                sender.sendMessage("§4" + target + " §c's data cannot be reached. Usually means this player is online another Ultimates server.");
        });
    }

    @AtlasCommand(paths = {"card grant <?> all"}, permission = PermissionConstants.ULTS_CARD_GRANT)
    public void cmdCardGrant(CommandSender sender, String target) {
        for(Card card : Cards.getInstance().getCardInstances()) {
            cmdCardGrant(sender, target, card);
        }
    }

    @AtlasCommand(paths = {"card revoke"}, permission = PermissionConstants.ULTS_CARD_REVOKE, classPriority = 1)
    public void cmdCardRevoke(CommandSender sender, Player target, Card card) {
        CardHolder tch = CardHolder.getCardHolder(target);
        if(!tch.isLoaded()) {
            sender.sendMessage(CardHolder.UNLOADED_MESSAGE);
            return;
        }

        if(tch.revokeCards(card)) {
            sender.sendMessage("§aSuccessfully revoked §2" + target.getName() + "'s §acard: " + card.getInfo().display());
        } else {
            sender.sendMessage("§cPlayer §4" + target.getName() + " §cdoesn't have card: " + card.getInfo().display());
        }
    }

    @AtlasCommand(paths = {"card revoke"}, permission = PermissionConstants.ULTS_CARD_REVOKE)
    public void cmdCardRevoke(CommandSender sender, String target, Card card) {
        OfflineCardHolder.getOfflineCardHolder(plugin, target, -1, (success, tch) -> {
            if(success) {
                if(tch.revokeCards(card)) {
                    sender.sendMessage("§aSuccessfully revoked §2" + tch.getName() + "'s §acard: " + card.getInfo().display());
                } else {
                    sender.sendMessage("§cPlayer §4" + tch.getName() + " §cdoesn't have card: " + card.getInfo().display());
                }
            } else
                sender.sendMessage("§4" + target + " §c's data cannot be reached. Usually means this player is online another Ultimates server.");
        });
    }

    @AtlasCommand(paths = {"card menu"})
    public void cmdCardMenu(Player player) {
        CardHolder holder = CardHolder.getCardHolder(player);
        holder.openCardMenuGUI(player);
    }

    @AtlasCommand(paths = {"card menu"}, permission = PermissionConstants.ULTS_CARD_ADMINMENU)
    public void cmdCardMenuPlayer(Player player, Player target) {
        CardHolder holder = CardHolder.getCardHolder(target);
        player.sendMessage("§cWarning! §fYou are accessing §d" + target.getName() + "'s §fcard deck.");
        player.sendMessage("§fThis menu is from their perspective and making changes will impact them.");
        holder.openCardMenuGUI(player);
    }

    @AtlasCommand(paths = {"card toggle"}, permission = PermissionConstants.ULTS_CARD_TOGGLE)
    public void cmdCardToggle(CommandSender sender, Card card) {
        if(card.isEnabled()) {
            plugin.getServer().sendMessage(Component.text("§4An admin has disabled the card: " + card.getInfo().display()));
            for(Player pl : card.getDrawnPlayers()) {
                pl.sendMessage("§4You had this card drawn. It has been temporarily discarded.");
            }
            card.setEnabled(false);
            for(Player p : card.getDrawnPlayers()) {
                CardHolder holder = CardHolder.getCardHolder(p);
                if(holder.isLoaded())
                    holder.discardCards(card);
            }
        } else {
            plugin.getServer().sendMessage(Component.text("§aAn admin has enabled the card: " + card.getInfo().display()));
            card.setEnabled(true);
        }
    }
}