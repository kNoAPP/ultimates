package com.knoban.ultimates.cards.base;

import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.HashSet;
import java.util.UUID;

/**
 * An interface that allows a card to be silenced. A silenced card does not grant its effects to the CardHolder,
 * but can be drawn and kept drawn. Silencing should persist between logout and logins, so a UUID is kept track of.
 *
 * @author Alden Bansemer (kNoAPP)
 */
public interface Silenceable {

    HashSet<UUID> silencedPlayers = new HashSet<>();

    /**
     * Sets whether or not a card is silenced for a player.
     * @param player The player to set the silence on
     * @param silenced True, if they are to be silenced. False, if not
     */
    default void setSilenced(@NotNull Player player, boolean silenced) {
        if(silenced) {
            silencedPlayers.add(player.getUniqueId());
        } else {
            silencedPlayers.remove(player.getUniqueId());
        }
    }

    /**
     * @param player The player to check if silenced
     * @return True, if the player is silenced
     */
    default boolean isSilenced(@NotNull Player player) {
        return silencedPlayers.contains(player.getUniqueId());
    }
}
