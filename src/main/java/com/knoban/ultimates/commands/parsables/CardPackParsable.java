package com.knoban.ultimates.commands.parsables;

import com.knoban.atlas.commandsII.ACParsable;
import com.knoban.ultimates.cardpack.CardPack;
import org.bukkit.command.CommandSender;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

public class CardPackParsable implements ACParsable<CardPack> {

    private static final List<String> SUGGESTIONS = new ArrayList<>();

    public CardPackParsable() {
        for(CardPack pack : CardPack.values())
            SUGGESTIONS.add(pack.getName());
    }

    @Nullable
    @Override
    public CardPack parse(@NotNull CommandSender sender, @NotNull String arg) {
        try {
            return CardPack.valueOf(arg.toUpperCase());
        } catch(IllegalArgumentException e) {
            return null;
        }
    }

    @Nullable
    @Override
    public List<String> defaultSuggestions(@NotNull CommandSender sender) {
        return SUGGESTIONS;
    }
}
