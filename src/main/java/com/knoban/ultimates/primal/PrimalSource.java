package com.knoban.ultimates.primal;

import com.knoban.ultimates.cards.Card;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.boss.BarColor;
import org.bukkit.scoreboard.Scoreboard;
import org.bukkit.scoreboard.Team;

import java.util.List;

public enum PrimalSource {

    NONE("§9No Primal", ChatColor.BLUE, BarColor.BLUE),
    SUN("§6Sun", ChatColor.GOLD, BarColor.YELLOW),
    MOON("§fMoon", ChatColor.WHITE, BarColor.WHITE),
    SKY("§bSky", ChatColor.AQUA, BarColor.PINK),
    EARTH("§aEarth", ChatColor.GREEN, BarColor.GREEN),
    OCEAN("§3Ocean", ChatColor.DARK_AQUA, BarColor.BLUE),
    FIRE("§cFire", ChatColor.RED, BarColor.RED),
    DARK("§8Dark", ChatColor.DARK_GRAY, BarColor.PURPLE);

    private final String display;
    private final ChatColor chatColor;
    private final BarColor barColor;
    private final Team team;

    PrimalSource(String display, ChatColor chatColor, BarColor barColor) {
        this.display = display;
        this.chatColor = chatColor;
        this.barColor = barColor;
        Scoreboard sb = Bukkit.getScoreboardManager().getMainScoreboard();
        String teamName = "ults-" + name();
        Team team = sb.getTeam(teamName);
        if(team != null)
            team.unregister();

        team = sb.registerNewTeam(teamName);
        if(!display.equals("§9No Primal")) {
            team.setAllowFriendlyFire(false);
            team.setCanSeeFriendlyInvisibles(true);
            team.setPrefix(display + " ");
        }
        team.setColor(ChatColor.GRAY);

        this.team = team;
    }

    public String getDisplay() {
        return display;
    }
    
    public ChatColor getChatColor() {
        return chatColor;
    }
    
    public BarColor getBarColor() {
        return barColor;
    }

    public Team getTeam() {
        return team;
    }

    public static PrimalSource getSourceFromCards(List<Card> cardList) {
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
