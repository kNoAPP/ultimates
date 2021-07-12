package com.knoban.ultimates.cardholder;

import com.google.api.core.ApiFuture;
import com.google.api.core.ApiFutureCallback;
import com.google.api.core.ApiFutures;
import com.google.cloud.firestore.DocumentSnapshot;
import com.google.cloud.firestore.SetOptions;
import com.google.cloud.firestore.WriteResult;
import com.google.common.util.concurrent.MoreExecutors;
import com.knoban.atlas.callbacks.GenericCallback1;
import com.knoban.atlas.claims.Landlord;
import com.knoban.atlas.data.firebase.AtlasFirebaseMutex;
import com.knoban.atlas.structure.HashSetArrayList;
import com.knoban.ultimates.Ultimates;
import com.knoban.ultimates.cardpack.CardPack;
import com.knoban.ultimates.cards.Card;
import com.knoban.ultimates.cards.CardInfo;
import com.knoban.ultimates.cards.Cards;
import com.knoban.ultimates.primal.PrimalSource;
import org.bukkit.Bukkit;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.concurrent.ExecutionException;
import java.util.stream.Collectors;

public abstract class Holder extends AtlasFirebaseMutex {

    protected final Ultimates plugin;
    protected final UUID uuid;
    protected final String name;
    protected final Landlord landlord;

    protected PrimalSource primalSource;
    protected HashSetArrayList<Card> ownedCards = new HashSetArrayList<>();
    protected HashSetArrayList<Card> drawnCards = new HashSetArrayList<>();
    protected ArrayList<Integer> ownedCardPacks = new ArrayList<>(CardPack.values().length);

    protected boolean loaded, battlePass;
    protected int timePlayed, maxEstateClaims, maxCardSlots, xp, maxFreeRewardedLevel, maxPremiumRewardedLevel, wisdom;
    protected long lastSeen;

    private boolean dataSyncIssue;

    private static boolean cardsDrawOnLoadSaveOnUnload = true;

    protected Holder(@NotNull Ultimates plugin, @NotNull UUID uuid, @NotNull String name) {
        super(plugin.getFirebase().getFirestore(),
                plugin.getFirebase().getFirestore().collection("cardholder").document(uuid.toString()),
                plugin.getLogger());

        this.plugin = plugin;
        this.uuid = uuid;
        this.name = name;
        this.landlord = plugin.getLandManager().getLandlord(uuid);
        this.loaded = false;
        this.dataSyncIssue = false;
    }

    /**
     * Get the {@link Holder}'s unique id.
     * @return The {@link Holder}'s unique id.
     */
    @NotNull
    public UUID getUniqueId() {
        return uuid;
    }

    /**
     * Gets the name of the {@link Holder} (from when they last logged in)
     * @return their username eg. "kNoAPP"
     */
    @NotNull
    public String getName() {
        return name;
    }

    /**
     * @return True if this has finished and loaded all the {@link Holder} data
     */
    public boolean isLoaded() {
        return loaded;
    }

    /**
     * Gets the system time when this {@link Holder} was last online. Specifically, this time is
     * when they logged out last. This value does not change when a Holder is online. It
     * will only change when a {@link Holder} goes offline.
     *
     * By default, newly joined Holders will have a value here of "0".
     *
     * @return System time of the time the {@link Holder} was seen
     */
    public long getLastSeen() {
        return lastSeen;
    }

    /**
     * @return The {@link Holder}'s {@link PrimalSource}.
     */
    public PrimalSource getPrimalSource() {
        return primalSource;
    }

    /**
     * Set the {@link Holder}'s {@link PrimalSource}.
     * @param primalSource The {@link PrimalSource} to set for this {@link Holder}.
     */
    public void setPrimalSource(PrimalSource primalSource) {
        this.primalSource = primalSource;
    }

    /**
     * Equip cards to the {@link Holder}
     * @param cards - The cards to equip. If no override, will equip as many cards as possible in the order given
     * @return true if the equipped set of cards changed as a result of the call
     */
    public boolean drawCards(@NotNull Card... cards) {
        boolean toRet = false;
        for(Card card : cards) {
            toRet = toRet || drawnCards.add(card);
        }

        return toRet;
    }

    /**
     * Equip cards to the {@link Holder}
     * @param cards - The cards to equip. If no override, will equip as many cards as possible in the order given
     * @return true if the equipped set of cards changed as a result of the call
     */
    public boolean drawCards(@NotNull Collection<Card> cards) {
        boolean toRet = false;
        for(Card card : cards) {
            toRet = toRet || drawnCards.add(card);
        }

        return toRet;
    }

    /**
     * Discard cards from the {@link Holder}
     * @param cards - The cards to discard
     * @return true if the equipped set of cards changed as a result of the call
     */
    public boolean discardCards(@NotNull Card... cards) {
        boolean toRet = false;
        for(Card card : cards) {
            toRet = toRet || drawnCards.remove(card);
        }

        return toRet;
    }

    /**
     * Discard cards from the {@link Holder}
     * @param cards - The cards to discard
     * @return true if the equipped set of cards changed as a result of the call
     */
    public boolean discardCards(@NotNull Collection<Card> cards) {
        boolean toRet = false;
        for(Card card : cards) {
            toRet = toRet || drawnCards.remove(card);
        }

        return toRet;
    }

    /**
     * Give a {@link Holder} permission to draw a card. This will also update the Card's owner list.
     * @param cards - The cards to give the {@link Holder} access to
     * @return false if the {@link Holder} already owns all the passed cards
     */
    public boolean grantCards(@NotNull Card... cards) {
        boolean toRet = false;
        for(Card card : cards) {
            toRet = toRet || ownedCards.add(card);
        }
        return toRet;
    }

    /**
     * Give a {@link Holder} permission to draw a card. This will also update the Card's owner list.
     * @param cards - The cards to give the {@link Holder} access to
     * @return false if the {@link Holder} already owns all the passed cards
     */
    public boolean grantCards(@NotNull Collection<Card> cards) {
        boolean toRet = false;
        for(Card card : cards) {
            toRet = toRet || ownedCards.add(card);
        }
        return toRet;
    }

    /**
     * Revoke a {@link Holder}'s permission to draw a card. This will also update the Card's owner list.
     * @param cards - The cards to revoke the {@link Holder}'s access to
     * @return false if the {@link Holder} doesn't own any the passed cards
     */
    public boolean revokeCards(@NotNull Card... cards) {
        boolean toRet = false;
        for(Card card : cards) {
            toRet = toRet || ownedCards.remove(card);
        }
        return toRet;
    }

    /**
     * Revoke a {@link Holder}'s permission to draw a card. This will also update the Card's owner list.
     * @param cards - The cards to revoke the {@link Holder}'s access to
     * @return false if the {@link Holder} doesn't own any the passed cards
     */
    public boolean revokeCards(@NotNull Collection<Card> cards) {
        boolean toRet = false;
        for(Card card : cards) {
            toRet = toRet || ownedCards.remove(card);
        }
        return toRet;
    }

    /**
     * @return the maximum number of cards a {@link Holder} can draw given their level
     */
    public int getMaxCardSlots() {
        return maxCardSlots;
    }

    /**
     * Sets the maximum amount of cards this {@link Holder} may draw at one time.
     * @param maxCardSlots The maximum number of cards that may be drawn
     */
    public void setMaxCardSlots(int maxCardSlots) {
        this.maxCardSlots = maxCardSlots;
    }

    /**
     * Increments the maximum amount of cards this {@link Holder} may draw at one time.
     * @param amt The incremented amount
     */
    public void incrementMaxCardSlots(int amt) {
        this.maxCardSlots += amt;
    }

    /**
     * @return the maximum number of claimed chunks a {@link Holder} can have given their level
     */
    public int getMaxEstateClaims() {
        return maxEstateClaims;
    }

    /**
     * Sets the maximum amount of claims this {@link Landlord} may have. If the value is lower than
     * before, this will not unclaim land to satisfy the new maximum.
     * @param maxEstateClaims The maximum number of claims to set
     */
    public void setMaxEstateClaims(int maxEstateClaims) {
        this.maxEstateClaims = maxEstateClaims;
    }

    /**
     * Increments the maximum amount of claims this {@link Landlord} may have. If the value is lower than
     * before, this will not unclaim land to satisfy the new maximum.
     * @param amt The maximum number of claims to increment
     */
    public void incrementMaxEstateClaims(int amt) {
        this.maxEstateClaims += amt;
    }

    /**
     * @return an immutable copy of the collection of all the cards a {@link Holder} owns.
     */
    @NotNull
    public Set<Card> getOwnedCards() {
        return Collections.unmodifiableSet(ownedCards);
    }

    /**
     * @return an immutable copy of the collection of all the cards a {@link Holder} has drawn
     */
    @NotNull
    public Set<Card> getDrawnCards() {
        return Collections.unmodifiableSet(drawnCards);
    }

    /**
     * @return the amount of experience the {@link Holder} has
     */
    public int getXp() {
        return xp;
    }

    /**
     * @param xp - Set the amount of experience the {@link Holder} should have
     */
    public void setXp(int xp) {
        this.xp = xp;
        checkForRewardFromBattlePass();
    }

    /**
     * @param amt - Increments the amount of experience the {@link Holder} should have.
     */
    public void incrementXp(int amt) {
        this.xp += amt;
        checkForRewardFromBattlePass();
    }

    /**
     * @return the Level of the {@link Holder}
     */
    public int getLevel() {
        return getLevelFromXp(xp);
    }

    /**
     * Set the level of a {@link Holder}. Their xp within the level will be set to 0.
     * @param level The new level to set.
     */
    public void setLevel(int level) {
        this.xp = getXpFromLevel(level);
        checkForRewardFromBattlePass();
    }

    /**
     * Increments the level of a {@link Holder}. Their xp within the level will be set to 0.
     * @param amt The increment amount.
     */
    public void incrementLevel(int amt) {
        this.xp = getXpFromLevel(getLevelFromXp(xp) + amt);
        checkForRewardFromBattlePass();
    }

    protected void checkForRewardFromBattlePass() {
        int currentLevel = getLevel();

        Player p = Bukkit.getPlayer(uuid);
        if(currentLevel > maxFreeRewardedLevel) {
            if(p != null) {
                p.sendMessage("§5Congratulations! §dYou've leveled up to level §2" + currentLevel + "§d!");
                p.playSound(p.getLocation(), Sound.ENTITY_MOOSHROOM_CONVERT, 1F, 0.8F);
                p.playSound(p.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1F, 0.85F);
            }

            for(; maxFreeRewardedLevel<currentLevel; ++maxFreeRewardedLevel)
                plugin.getBattlepassManager().rewardFreeLevel(p, maxFreeRewardedLevel + 1);
        }

        if(battlePass && currentLevel > maxPremiumRewardedLevel) {
            for(; maxPremiumRewardedLevel<currentLevel; ++maxPremiumRewardedLevel)
                plugin.getBattlepassManager().rewardPremiumLevel(p, maxPremiumRewardedLevel + 1);
        }
    }

    /**
     * @return True, if the {@link Holder} has purchased the battle pass.
     */
    public boolean hasBattlePass() {
        return battlePass;
    }

    /**
     * Sets the status of whether or not the {@link Holder} owns the battle pass. If this is updated to
     * true, then all previously missed rewards are immediately granted. These rewards cannot be duplicated
     * by toggling the battle pass in rapid succession. If a player loses the battle pass they once had, they
     * will keep all earned rewards, but will not gain new ones.
     * @param battlePass True, if the player should own the battle pass. False, if not.
     */
    public void setBattlePass(boolean battlePass) {
        this.battlePass = battlePass;
        if(battlePass) {
            // Grant all missed rewards to them immediately.
            checkForRewardFromBattlePass();
        }
    }

    /**
     * @return the amount of time in seconds the {@link Holder} has played on the server
     */
    public int getTimePlayed() {
        return timePlayed;
    }

    /**
     * Set the time played of the {@link Holder} in seconds
     * @param timePlayed - The amount in seconds to set
     */
    public void setTimePlayed(int timePlayed) {
        this.timePlayed = timePlayed;
    }

    /**
     * Get this {@link Holder}'s owned amount of Wisdom-- the main currency for Cards.
     * @return The amount of Wisdom owned.
     */
    public int getWisdom() {
        return wisdom;
    }

    /**
     * Sets this {@link Holder}'s owned amount of Wisdom-- the main currency for Cards.
     * @param wisdom The new amount of Wisdom owned.
     */
    public void setWisdom(int wisdom) {
        this.wisdom = wisdom;
    }

    /**
     * Increments this {@link Holder}'s owned amount of Wisdom-- the main currency for Cards.
     * @param amt The incremented amount
     */
    public void incrementWisdom(int amt) {
        this.wisdom += amt;
    }

    /**
     * Returns the amount of card packs of a specific type this {@link Holder} owns.
     * @param pack The card pack type
     * @return The amount of owned packs for that type. Returns null if an invalid pack type is passed.
     */
    @Nullable
    public Integer getOwnedCardPack(int pack) {
        if(pack < 0 || ownedCardPacks.size() <= pack)
            return null;

        return ownedCardPacks.get(pack);
    }

    /**
     * Sets the amount of card packs of a specific type this {@link Holder} should own. If an invalid pack type is passed,
     * this function does nothing.
     * @param pack The card pack type
     * @param amt The amount of packs
     */
    public void setOwnedCardPacks(int pack, int amt) {
        if(pack < 0 || ownedCardPacks.size() <= pack)
            return;

        ownedCardPacks.set(pack, amt);
    }

    /**
     * Increments the amount of card packs of a specific type this {@link Holder} should own. If an invalid pack type
     * is passed, this function does nothing.
     * @param pack The card pack type
     * @param amt The incremented amount of packs
     */
    public void incrementOwnedCardPacks(int pack, int amt) {
        if(pack < 0 || ownedCardPacks.size() <= pack)
            return;

        ownedCardPacks.set(pack, ownedCardPacks.get(pack) + amt);
    }

    /**
     * Gets the {@link Holder}'s representation as a Landlord. All calls from Landlord are safe to use when the {@link Holder}
     * is offline.
     * @return The {@link Holder} as a {@link Landlord}
     */
    @NotNull
    public Landlord asLandlord() {
        return landlord;
    }

    /**
     * Load the {@link Holder}'s data from the database when a mutex is available.
     * Runs on a separate thread.
     * When run, will set the isLoaded() flag to true on successful completion.
     * @param millisTimeout - The maximum amount of time you're willing to wait for data to sync. If this time is
     * exceeded, pending data transactions will be cleared from other servers. Pass a -1 for no wait limit.
     * @param karen - If True, when millisTimeout is reached all other mutex's tasks will be cancelled, but yours
     * will run. If false, when millisTimeout is reached your task is cancelled.
     * @param callback - A callback to call when finished loading. Runs on main game thread. The boolean flags success.
     * (or null if none)
     */
    protected void load(long millisTimeout, boolean karen, @Nullable CardCallback callback) {
        addMutex((successful) -> {
            if(!successful) {
                if(callback != null)
                    plugin.getServer().getScheduler().runTask(plugin, () -> callback.call(false, null));
                return;
            }

            listenToMutex(millisTimeout, karen, (success) -> {
                if(!success) {
                    if(callback != null)
                        plugin.getServer().getScheduler().runTask(plugin, () -> callback.call(false, null));
                    return;
                }

                loadWithoutMutex(callback);
                listenForKarens((noError) -> {
                    dataSyncIssue = true;
                    plugin.getServer().getScheduler().runTask(plugin, () -> {
                        Player p = Bukkit.getPlayer(uuid);
                        if(p != null) {
                            p.kickPlayer("§cData synchronization error on CardHolder data, did you login elsewhere?" +
                                    "\n§cIf this problem continues, contact your server administrator." +
                                    "\n§cGive them this error code: §4ults-12");
                        }
                    });
                });
            });
        });
    }

    /**
     * Load the {@link Holder}'s data from the database ignoring mutex's. The risk of data inconsistency is on you.
     * Runs on a separate thread.
     * When run, will set the isLoaded() flag to true on successful completion.
     * @param callback - A callback to call when finished loading. Runs on main game thread. The boolean flags success.
     * (or null if none)
     */
    @SuppressWarnings("unchecked")
    protected void loadWithoutMutex(@Nullable CardCallback callback) {
        ApiFuture<DocumentSnapshot> future = firestoreReference.get();
        ApiFutures.addCallback(future, new ApiFutureCallback<DocumentSnapshot>() {
            @Override
            public void onFailure(Throwable t) {
                plugin.getLogger().warning("Failed to load Holder data for " + uuid + ": " + t.getMessage());
                if(callback != null)
                    plugin.getServer().getScheduler().runTask(plugin, () -> callback.call(false, null));
            }

            @Override
            public void onSuccess(DocumentSnapshot result) {
                HashSetArrayList<Card> ownedCards = new HashSetArrayList<>();
                ArrayList<Card> toDraw = new ArrayList<>();

                if(result.exists()) {
                    Map<String, Object> values = result.getData();
                    lastSeen = (Long) values.getOrDefault("lastSeen", 0L);
                    timePlayed = ((Long) values.getOrDefault("timePlayed", 0L)).intValue();
                    primalSource = PrimalSource.valueOf((String) values.getOrDefault("primalSource", PrimalSource.NONE.name()));
                    maxEstateClaims = ((Long) values.getOrDefault("maxEstateClaims", 1L)).intValue();
                    maxCardSlots = ((Long) values.getOrDefault("maxCardSlots", 2L)).intValue();
                    xp = ((Long) values.getOrDefault("xp", 0L)).intValue();
                    maxFreeRewardedLevel = ((Long) values.getOrDefault("maxFreeRewardedLevel", 0L)).intValue();
                    maxPremiumRewardedLevel = ((Long) values.getOrDefault("maxPremiumRewardedLevel", 0L)).intValue();
                    battlePass = (boolean) values.getOrDefault("battlePass", false);
                    wisdom = ((Long) values.getOrDefault("wisdom", 0L)).intValue();

                    if(cardsDrawOnLoadSaveOnUnload) {
                        for(String cardName : (Iterable<String>) values.getOrDefault("drawnCards", Collections.emptyList())) {
                            Card card = Cards.getInstance().getCardInstanceByName(cardName);
                            if(card != null) { //silent skip: it will get logged when "ownedCards" are parsed
                                toDraw.add(card);
                            }
                        }
                    }

                    for(String cardName : (Iterable<String>) values.getOrDefault("ownedCards", Collections.emptyList())) {
                        Card card = Cards.getInstance().getCardInstanceByName(cardName);
                        if(card != null) {
                            ownedCards.add(card);
                        } else {
                            plugin.getLogger().warning("Holder " + name + " owns non-existent card: " + cardName);
                        }
                    }

                    ArrayList<Long> ownedCardPacksLong = (ArrayList<Long>) values.getOrDefault("ownedCardPacks", new ArrayList<>(CardPack.values().length));
                    int i = 0;
                    for(; i < ownedCardPacksLong.size(); i++)
                        ownedCardPacks.add(ownedCardPacksLong.get(i).intValue());
                    for(; i < CardPack.values().length; i++)
                        ownedCardPacks.add(0);
                } else {
                    lastSeen = 0L;
                    timePlayed = 0;
                    primalSource = PrimalSource.NONE;
                    maxEstateClaims = 1;
                    maxCardSlots = 2;
                    xp = 0;
                    maxFreeRewardedLevel = 0; // Level zero rewards and below don't exist.
                    maxPremiumRewardedLevel = 0;
                    battlePass = false;
                    wisdom = 200; // Starter wisdom
                    ownedCardPacks = new ArrayList<>(CardPack.values().length);
                    for(int i = ownedCardPacks.size(); i < CardPack.values().length; i++)
                        ownedCardPacks.add(0);
                }

                Holder.this.ownedCards = ownedCards;
                loaded = true;
                if(callback != null)
                    plugin.getServer().getScheduler().runTask(plugin, () -> callback.call(true, toDraw));
            }
        }, MoreExecutors.directExecutor());
    }

    /**
     * Saves the {@link Holder}'s data to the database and releases any mutex. Will set isLoaded() to false.
     * @param block If true, this will block until completed. Useful for server shutdowns.
     * @param callback - A callback to call when finished saving. Runs on main game thread. The boolean flags success.
     * (or null if none)
     */
    protected void save(boolean block, @Nullable GenericCallback1<Boolean> callback) {
        if(dataSyncIssue) // Don't save the data if it will corrupt a save file.
            return;

        Map<String, Object> update = new TreeMap<>();
        update.put("username", name);
        update.put("lastSeen", System.currentTimeMillis());
        update.put("timePlayed", getTimePlayed());
        update.put("primalSource", primalSource.name());
        update.put("maxEstateClaims", maxEstateClaims);
        update.put("maxCardSlots", maxCardSlots);
        update.put("maxFreeRewardedLevel", maxFreeRewardedLevel);
        update.put("maxPremiumRewardedLevel", maxPremiumRewardedLevel);
        update.put("xp", xp);
        update.put("battlePass", battlePass);
        update.put("wisdom", wisdom);
        if(cardsDrawOnLoadSaveOnUnload)
            update.put("drawnCards", drawnCards.stream().map(Card::getInfo).map(CardInfo::name).collect(Collectors.toList()));
        update.put("ownedCards", ownedCards.stream().map(Card::getInfo).map(CardInfo::name).collect(Collectors.toList()));
        update.put("ownedCardPacks", ownedCardPacks);

        ApiFuture<WriteResult> future = firestoreReference.set(update, SetOptions.merge());
        if(block) {
            try {
                future.get();
                removeMutex(true); // Free our mutex
                if(callback != null)
                    plugin.getServer().getScheduler().runTask(plugin, () -> callback.call(true));
            } catch(InterruptedException | ExecutionException e) {
                plugin.getLogger().warning("Failed to save Holder data for " + name + ": " + e.getMessage());
                if(callback != null)
                    plugin.getServer().getScheduler().runTask(plugin, () -> callback.call(false));
            }
        } else {
            ApiFutures.addCallback(future, new ApiFutureCallback<WriteResult>() {
                @Override
                public void onFailure(Throwable t) {
                    plugin.getLogger().warning("Failed to save Holder data for " + name + ": " + t.getMessage());
                    if(callback != null)
                        plugin.getServer().getScheduler().runTask(plugin, () -> callback.call(false));
                }

                @Override
                public void onSuccess(WriteResult result) {
                    removeMutex(false); // Free our mutex
                    if(callback != null)
                        plugin.getServer().getScheduler().runTask(plugin, () -> callback.call(true));
                }
            }, MoreExecutors.directExecutor());
        }

        loaded = false;
    }

    /**
     * Get the projected {@link Holder} level given an amount of experience
     * @param xp - The amount of experience to get the level from
     * @return the projected level
     */
    public static int getLevelFromXp(int xp) {
        return xp / 1000;
    }

    /**
     * Get the projected {@link Holder} experience given a level
     * @param level - The level to get the experience from
     * @return the projected experience
     */
    public static int getXpFromLevel(int level) {
        return level * 1000;
    }

    /**
     * Should a {@link Holder}'s cards draw when loaded?
     * @return True, if they should. False if they shouldn't
     */
    public static boolean isCardsDrawOnLoadSaveOnUnload() {
        return cardsDrawOnLoadSaveOnUnload;
    }

    /**
     * Set if a Holder's cards draw when they log in.
     * @param cardsDrawOnLoadSaveOnUnload True, if cards should be drawn on load and saved on unload
     */
    public static void setCardsDrawOnLoadSaveOnUnload(boolean cardsDrawOnLoadSaveOnUnload) {
        Holder.cardsDrawOnLoadSaveOnUnload = cardsDrawOnLoadSaveOnUnload;
    }

    @Override
    public boolean equals(Object o) {
        if(!(o instanceof Holder))
            return false;

        return uuid.equals(((Holder) o).uuid);
    }

    @Override
    public int hashCode() {
        return uuid.hashCode();
    }
}
