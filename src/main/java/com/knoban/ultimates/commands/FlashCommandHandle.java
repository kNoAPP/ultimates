package com.knoban.ultimates.commands;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.knoban.atlas.commandsII.ACAPI;
import com.knoban.atlas.commandsII.annotations.AtlasCommand;
import com.knoban.atlas.utils.Cooldown;
import com.knoban.ultimates.Ultimates;
import com.knoban.ultimates.cardholder.CardHolder;
import org.bukkit.entity.Player;

import java.util.UUID;
import java.util.concurrent.TimeUnit;

public class FlashCommandHandle {

	private final Ultimates plugin;

	private static final long FLASH_COOLDOWN = 10000;
	private Cache<UUID, Cooldown> cache = CacheBuilder.newBuilder()
			.expireAfterWrite(FLASH_COOLDOWN, TimeUnit.MILLISECONDS)
			.build();

	public FlashCommandHandle(Ultimates plugin) {
		this.plugin = plugin;

		ACAPI api = ACAPI.getApi();
		api.registerCommandsFromClass(plugin, FlashCommandHandle.class, this);
	}

	@AtlasCommand(paths = {"flash"})
	public void cmdFlashBase(Player sender) {
		Cooldown cd = cache.getIfPresent(sender.getUniqueId());
		if(cd == null) {
			CardHolder holder = CardHolder.getCardHolder(sender);
			if(!holder.isLoaded()) {
				sender.sendMessage(CardHolder.UNLOADED_MESSAGE);
				return;
			}

			if(holder.getDrawnCards().size() > 0) {
				sender.sendMessage("§6Flashing your cards...");
				cache.put(sender.getUniqueId(), new Cooldown(FLASH_COOLDOWN));
				holder.flashCards();
			} else
				sender.sendMessage("§cYou need at least one card drawn to flash!");
		} else {
			sender.sendMessage("§cYou can flash cards in §4" + cd.toTimestampString() + "§c.");
		}
	}
}
