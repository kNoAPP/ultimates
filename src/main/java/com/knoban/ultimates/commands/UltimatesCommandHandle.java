package com.knoban.ultimates.commands;

import com.knoban.atlas.commandsII.ACAPI;
import com.knoban.atlas.commandsII.annotations.AtlasCommand;
import com.knoban.ultimates.Ultimates;
import com.knoban.ultimates.aspects.Message;
import com.knoban.ultimates.permissions.PermissionConstants;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;

public class UltimatesCommandHandle {

    private final Ultimates plugin;

    public UltimatesCommandHandle(Ultimates plugin) {
        this.plugin = plugin;

        ACAPI api = ACAPI.getApi();
        api.registerCommandsFromClass(plugin, UltimatesCommandHandle.class, this);
    }

    @AtlasCommand(paths = {"ultimates", "ults", "ultimates help", "ults help", "ultimates help 1", "ults help 1"})
    public void cmdUltimatesBase(CommandSender sender) {
        sender.sendMessage(Message.INFO.getMessage("Ultimates Help 1"));
        sender.sendMessage(ChatColor.DARK_GREEN + "------------------");
        sender.sendMessage(Message.HELP.getMessage("/card - Equip ability cards"));
        sender.sendMessage(Message.HELP.getMessage("/cardpack - Open owned card packs"));
        sender.sendMessage(Message.HELP.getMessage("/flash - Flash your collected cards"));
        sender.sendMessage(Message.HELP.getMessage("/pass - Battle Pass/Mission rewards"));
        sender.sendMessage(Message.HELP.getMessage("/estate - Claim land for yourself"));
        sender.sendMessage(Message.HELP.getMessage("/recall - Set a recall location"));
        sender.sendMessage("§dFor more help, type /help 2.");

    }

    @AtlasCommand(paths = {"ultimates help 2", "ults help 2"})
    public void cmdUltimatesHelp2(CommandSender sender) {
        sender.sendMessage(Message.INFO.getMessage("Ultimates Help 2"));
        sender.sendMessage(ChatColor.DARK_GREEN + "------------------");
        sender.sendMessage(Message.HELP.getMessage("/level - View your level"));
        sender.sendMessage(Message.HELP.getMessage("/wisdom - View your currency (wisdom)"));
        sender.sendMessage(Message.HELP.getMessage("/msg - Privately message a player"));
        sender.sendMessage(Message.HELP.getMessage("/r - Privately reply to a player"));
        if(sender.hasPermission(PermissionConstants.ATLAS_SPY))
            sender.sendMessage(Message.HELP.getMessage("/spy - Spy on private messages"));
        if(sender.hasPermission(PermissionConstants.ULTS_SOUNDGEN))
            sender.sendMessage(Message.HELP.getMessage("/soundgen - Generate custom sounds"));
        sender.sendMessage("§dFor more help, type /help 3.");
    }

    @AtlasCommand(paths = {"ultimates help 3", "ults help 3"})
    public void cmdUltimatesHelp3(CommandSender sender) {
        sender.sendMessage(Message.INFO.getMessage("Ultimates Help 3"));
        sender.sendMessage(ChatColor.DARK_GREEN + "------------------");
        if(sender.hasPermission(PermissionConstants.ULTS_CHUNK))
            sender.sendMessage(Message.HELP.getMessage("/chunk - Preload chunks"));
        if(sender.hasPermission(PermissionConstants.ATLAS_SPY))
            sender.sendMessage(Message.HELP.getMessage("/spy - Spy on private messages"));
        if(sender.hasPermission(PermissionConstants.ULTS_SOUNDGEN))
            sender.sendMessage(Message.HELP.getMessage("/soundgen - Generate custom sounds"));
    }

    @AtlasCommand(paths = {"ultimates debug", "ults debug"}, permission = PermissionConstants.ULTS_DEBUG)
    public void cmdUltimatesDebug(CommandSender sender) {
        for(String cmd : ACAPI.getApi().getAllRegisteredCommands()) {
            sender.sendMessage(cmd);
        }
        sender.sendMessage("§d" + plugin.getDescription().getName() + " Build §5" + plugin.getDescription().getVersion());
    }
}
