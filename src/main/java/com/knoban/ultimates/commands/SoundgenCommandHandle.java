package com.knoban.ultimates.commands;

import com.knoban.atlas.commandsII.ACAPI;
import com.knoban.atlas.commandsII.annotations.AtlasCommand;
import com.knoban.atlas.utils.Tools;
import com.knoban.ultimates.Ultimates;
import com.knoban.ultimates.aspects.Message;
import com.knoban.ultimates.permissions.PermissionConstants;
import org.bukkit.Bukkit;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class SoundgenCommandHandle {

	private final Ultimates plugin;
	private final List<UUID> soundgen = new ArrayList<UUID>();

	public SoundgenCommandHandle(Ultimates plugin) {
		this.plugin = plugin;

		ACAPI api = ACAPI.getApi();
		api.registerCommandsFromClass(plugin, SoundgenCommandHandle.class, this);
	}

	@AtlasCommand(paths = {"soundgen"}, permission = PermissionConstants.ULTS_SOUNDGEN)
	public void cmdSoundgenBase(Player sender) {
		sender.sendMessage(Message.SOUNDGEN.getMessage("Flushing sounds..."));
		Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "stopsound " + sender.getName());
	}

	@AtlasCommand(paths = {"soundgen on"}, permission = PermissionConstants.ULTS_SOUNDGEN)
	public void cmdSoundgenOn(Player sender) {
		if(!soundgen.contains(sender.getUniqueId())) {
			soundgen.add(sender.getUniqueId());
			sender.sendMessage(Message.SOUNDGEN.getMessage("On..."));
			new BukkitRunnable() {
				public void run() {
					if(soundgen.contains(sender.getUniqueId()) && sender.isOnline()) {
						Sound s = Sound.values()[Tools.randomNumber(0, Sound.values().length-1)];
						while(s.name().contains("RECORD") || s.name().contains("MUSIC")) s = Sound.values()[Tools.randomNumber(0, Sound.values().length-1)];
						float pitch = (float) Tools.randomNumber(0.2, 2.0);

						sender.playSound(sender.getLocation(), s, 1F, pitch);
						sender.sendMessage(Message.SOUNDGEN.getMessage(s.name() + " - " + pitch));
					} else {
						soundgen.remove(sender.getUniqueId());
						this.cancel();
					}
				}
			}.runTaskTimer(plugin, 20L, 30L);
		} else
			sender.sendMessage(Message.SOUNDGEN.getMessage("On already..."));
	}

	@AtlasCommand(paths = {"soundgen off"}, permission = PermissionConstants.ULTS_SOUNDGEN)
	public void cmdSoundgenOff(Player sender) {
		soundgen.remove(sender.getUniqueId());
		sender.sendMessage(Message.SOUNDGEN.getMessage("Off..."));
	}
}
