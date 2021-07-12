package com.knoban.ultimates.commands.parsables;

import com.knoban.atlas.commandsII.ACParsable;
import com.knoban.ultimates.cards.Card;
import com.knoban.ultimates.cards.Cards;
import org.bukkit.command.CommandSender;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class CardParsable implements ACParsable<Card> {

    @Nullable
    @Override
    public Card parse(@NotNull CommandSender sender, @NotNull String arg) {
        return Cards.getInstance().getCardInstanceByName(arg);
    }

    @Nullable
    @Override
    public List<String> defaultSuggestions(@NotNull CommandSender sender) {
        return new ArrayList<>(Cards.getInstance().getCardByName().keySet());
    }
}
