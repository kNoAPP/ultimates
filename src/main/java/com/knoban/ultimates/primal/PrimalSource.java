package com.knoban.ultimates.primal;

import com.knoban.ultimates.cards.Card;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.boss.BarColor;
import org.bukkit.scoreboard.Scoreboard;
import org.bukkit.scoreboard.Team;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public enum PrimalSource {

    NONE("§9No Primal", ChatColor.BLUE, BarColor.BLUE, NamedTextColor.BLUE),
    SUN("§6Sun", ChatColor.GOLD, BarColor.YELLOW, NamedTextColor.GOLD),
    MOON("§fMoon", ChatColor.WHITE, BarColor.WHITE, NamedTextColor.WHITE),
    SKY("§bSky", ChatColor.AQUA, BarColor.PINK, NamedTextColor.AQUA),
    EARTH("§aEarth", ChatColor.GREEN, BarColor.GREEN, NamedTextColor.GREEN),
    OCEAN("§3Ocean", ChatColor.DARK_AQUA, BarColor.BLUE, NamedTextColor.DARK_AQUA),
    FIRE("§cFire", ChatColor.RED, BarColor.RED, NamedTextColor.RED),
    DARK("§8Dark", ChatColor.DARK_GRAY, BarColor.PURPLE, NamedTextColor.DARK_GRAY);

    private final String display;
    private final ChatColor chatColor;
    private final BarColor barColor;
    private final TextColor textColor;
    private final Team team;

    PrimalSource(@NotNull String display, @NotNull ChatColor chatColor, @NotNull BarColor barColor,
                 @NotNull TextColor textColor) {
        this.display = display;
        this.chatColor = chatColor;
        this.barColor = barColor;
        this.textColor = textColor;
        Scoreboard sb = Bukkit.getScoreboardManager().getMainScoreboard();
        String teamName = "ults-" + name();
        Team team = sb.getTeam(teamName);
        if(team != null)
            team.unregister();

        team = sb.registerNewTeam(teamName);
        if(!display.equals("§9No Primal")) {
            team.setAllowFriendlyFire(false);
            team.setCanSeeFriendlyInvisibles(true);
            team.prefix(Component.text(display + " "));
        }
        team.color(NamedTextColor.GRAY);

        this.team = team;
    }

    @NotNull
    public String getDisplay() {
        return display;
    }

    @NotNull
    public ChatColor getChatColor() {
        return chatColor;
    }

    @NotNull
    public BarColor getBarColor() {
        return barColor;
    }

    @NotNull
    public TextColor getTextColor() {
        return textColor;
    }

    @NotNull
    public Team getTeam() {
        return team;
    }

    @NotNull
    public static PrimalSource getSourceFromCards(@NotNull List<Card> cardList) {
        int[] numCards = new int[values().length];
        for(int i=0; i<values().length; i++)
            numCards[i] = 0;

        // The NONE primal should only be displayed if no other primal'd cards are present.
        PrimalSource highestSource = PrimalSource.NONE;
        int highestCount = 0;
        for(Card card : cardList) {
            PrimalSource source = card.getInfo().source();
            if(source == PrimalSource.NONE)
                continue;

            int count = ++numCards[source.ordinal()];
            if(count > highestCount) {
                highestSource = source;
                highestCount = count;
            }
        }

        return highestSource;
    }
}
