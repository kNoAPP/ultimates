package com.knoban.ultimates.commands.parsables;

import com.knoban.atlas.commandsII.ACParsable;
import com.knoban.ultimates.cards.Card;
import com.knoban.ultimates.cards.Cards;
import org.bukkit.command.CommandSender;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public class CardParsable implements ACParsable<Card> {

    private final List<String> SUGGESTIONS = Cards.getInstance().getCardNames();

    private List<String> generateDefaultSuggestions() {
        return SUGGESTIONS;
    }

    @Nullable
    @Override
    public Card parse(@NotNull CommandSender sender, @NotNull String arg) {
        return Cards.getInstance().getCardInstance(arg);
    }

    @Nullable
    @Override
    public List<String> defaultSuggestions(@NotNull CommandSender sender) {
        return SUGGESTIONS;
    }
}
