package com.knoban.ultimates.commands.parsables;

import com.knoban.atlas.commandsII.ACParsable;
import com.knoban.ultimates.cardholder.CardHolder;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class CardHolderParsable implements ACParsable<CardHolder> {

    @Nullable
    @Override
    public CardHolder parse(@NotNull CommandSender sender, @NotNull String arg) {
        Player p = Bukkit.getPlayerExact(arg);
        return p != null ? CardHolder.getCardHolder(p) : null;
    }

    @Override
    public boolean likelyMatch(@NotNull CommandSender sender, @NotNull String arg, @Nullable String filter) {
        return arg.length() >= 3;
    }

    @Nullable
    @Override
    public List<String> defaultSuggestions(@NotNull CommandSender sender) {
        List<String> suggestions = new ArrayList<>();
        for(Player pl : Bukkit.getOnlinePlayers())
            suggestions.add(pl.getName());
        return suggestions;
    }

    @Nullable
    @Override
    public Optional<String> getOvercastName() {
        return Optional.of("Player");
    }
}
