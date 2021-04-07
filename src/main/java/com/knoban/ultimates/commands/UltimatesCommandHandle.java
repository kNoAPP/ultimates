package com.knoban.ultimates.commands;

import com.knoban.atlas.commandsII.ACAPI;
import com.knoban.atlas.commandsII.annotations.AtlasCommand;
import com.knoban.ultimates.Ultimates;
import com.knoban.ultimates.aspects.Message;
import com.knoban.ultimates.permissions.PermissionConstants;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;

public class UltimatesCommandHandle {

    private final Ultimates plugin;
    private final FileConfiguration fc;

    public UltimatesCommandHandle(Ultimates plugin) {
        this.plugin = plugin;
        this.fc = plugin.getConfigFile().getCachedYML();

        ACAPI api = ACAPI.getApi();
        api.registerCommandsFromClass(plugin, UltimatesCommandHandle.class, this);
    }

    @AtlasCommand(paths = {"ultimates", "ults", "ultimates help", "ults help", "ultimates help 1", "ults help 1"})
    public void cmdUltimatesBase(CommandSender sender) {
        sender.sendMessage(Message.INFO.getMessage("Ultimates Help 1"));
        sender.sendMessage(ChatColor.DARK_GREEN + "------------------");

        if(fc.getBoolean("Command-Toggle.BattlePass", true))
            sender.sendMessage(Message.HELP.getMessage("/battlepass - Battlepass/Mission rewards"));
        if(fc.getBoolean("Command-Toggle.Card", true))
            sender.sendMessage(Message.HELP.getMessage("/card - Draw your ability cards"));
        if(fc.getBoolean("Command-Toggle.CardPack", true))
            sender.sendMessage(Message.HELP.getMessage("/cardpack - Open card packs"));
        if(fc.getBoolean("Command-Toggle.CardSlot", true))
            sender.sendMessage(Message.HELP.getMessage("/cardslot - View your card slots"));
        if(sender.hasPermission(PermissionConstants.ULTS_CHUNK))
            sender.sendMessage(Message.HELP.getMessage("/chunk - Preload chunks"));
        if(fc.getBoolean("Command-Toggle.Estate", true))
            sender.sendMessage(Message.HELP.getMessage("/estate - Claim land for yourself"));

        sender.sendMessage("§dFor more help, type /help 2.");
    }

    @AtlasCommand(paths = {"ultimates help 2", "ults help 2"})
    public void cmdUltimatesHelp2(CommandSender sender) {
        sender.sendMessage(Message.INFO.getMessage("Ultimates Help 2"));
        sender.sendMessage(ChatColor.DARK_GREEN + "------------------");

        if(fc.getBoolean("Command-Toggle.Flash", true))
            sender.sendMessage(Message.HELP.getMessage("/flash - Flash your collected cards"));
        if(fc.getBoolean("Command-Toggle.Level", true))
            sender.sendMessage(Message.HELP.getMessage("/level - View your level"));
        sender.sendMessage(Message.HELP.getMessage("/msg - Privately message a player"));
        sender.sendMessage(Message.HELP.getMessage("/r - Privately reply to a player"));
        if(fc.getBoolean("Command-Toggle.Recall", true))
            sender.sendMessage(Message.HELP.getMessage("/recall - Set a recall location"));
        if(fc.getBoolean("Command-Toggle.Soundgen", true) && sender.hasPermission(PermissionConstants.ULTS_SOUNDGEN))
            sender.sendMessage(Message.HELP.getMessage("/soundgen - Generate custom sounds"));

        sender.sendMessage("§dFor more help, type /help 3.");
    }

    @AtlasCommand(paths = {"ultimates help 3", "ults help 3"})
    public void cmdUltimatesHelp3(CommandSender sender) {
        sender.sendMessage(Message.INFO.getMessage("Ultimates Help 3"));
        sender.sendMessage(ChatColor.DARK_GREEN + "------------------");

        if(sender.hasPermission(PermissionConstants.ATLAS_SPY))
            sender.sendMessage(Message.HELP.getMessage("/spy - Spy on private messages"));
        if(fc.getBoolean("Command-Toggle.Ultimates", true))
            sender.sendMessage(Message.HELP.getMessage("/ultimates - Thats this command!"));
        if(fc.getBoolean("Command-Toggle.Wisdom", true))
            sender.sendMessage(Message.HELP.getMessage("/wisdom - View your currency (wisdom)"));
    }

    @AtlasCommand(paths = {"ultimates debug", "ults debug"}, permission = PermissionConstants.ULTS_DEBUG)
    public void cmdUltimatesDebug(CommandSender sender) {
        for(String cmd : ACAPI.getApi().getAllRegisteredCommands()) {
            sender.sendMessage(cmd);
        }
        sender.sendMessage("§d" + plugin.getDescription().getName() + " Build §5" + plugin.getDescription().getVersion());
    }
}
