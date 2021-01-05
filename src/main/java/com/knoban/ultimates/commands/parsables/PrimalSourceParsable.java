package com.knoban.ultimates.commands.parsables;

import com.google.common.collect.ImmutableList;
import com.knoban.atlas.commandsII.ACParsable;
import com.knoban.ultimates.primal.PrimalSource;
import org.bukkit.command.CommandSender;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

public class PrimalSourceParsable implements ACParsable<PrimalSource> {
	
	private final List<String> suggestions = Arrays.stream(PrimalSource.values())
			.map(Enum::name)
			.map(String::toLowerCase)
			.sorted()
			.collect(ImmutableList.toImmutableList());
	
	@Nullable
	@Override
	public PrimalSource parse(@NotNull CommandSender sender, @NotNull String arg) {
		//noinspection Convert2streamapi
		for(PrimalSource primalSource : PrimalSource.values()) {
			if(primalSource.name().equalsIgnoreCase(arg)) {
				return primalSource;
			}
		}
		return null;
	}
	
	@Nullable
	@Override
	public List<String> defaultSuggestions(@NotNull CommandSender sender) {
		return suggestions;
	}
	
	@Override
	public Optional<String> getOvercastName() {
		return Optional.of("Primal");
	}
}
