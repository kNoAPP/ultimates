package com.knoban.ultimates.cards;

import com.knoban.ultimates.Ultimates;
import com.knoban.ultimates.primal.PrimalSource;
import com.knoban.ultimates.primal.Tier;
import org.checkerframework.checker.units.qual.A;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;

public final class Cards {

    private static final Cards INSTANCE = new Cards();

    private final List<Class<? extends Card>> cards = new ArrayList<>();
    private final Map<String, Class<? extends Card>> cardsByName = new HashMap<>();
    private final List<String> cardNames = new ArrayList<>();

    private final Map<Class<? extends Card>, Card> cardInstances = new HashMap<>();
    private final Map<String, Card> cardInstancesByName = new HashMap<>();
    private final Map<Tier, ArrayList<Card>> cardInstancesByTier = new TreeMap<>();
    private final Map<PrimalSource, ArrayList<Card>> cardInstancesByPrimal = new TreeMap<>();

    private final List<Card> cardInstancesByTierAscending = new ArrayList<>();
    private final List<Card> cardInstancesByTierDescending = new ArrayList<>();
    private final List<Card> cardInstancesByPrimalAscending = new ArrayList<>();

    public void addCard(@NotNull Class<? extends Card> card) {
        cards.add(card);
        String cardName = card.getAnnotation(CardInfo.class).name();
        cardsByName.put(cardName, card);
        cardNames.add(cardName);

        try {
            Card c = card.getConstructor(Ultimates.class).newInstance(Ultimates.getPlugin());
            cardInstances.put(card, c);
            cardInstancesByName.put(c.info.name(), c);
            cardInstancesByTier.computeIfAbsent(c.info.tier(), k -> new ArrayList<>()).add(c);
            cardInstancesByPrimal.computeIfAbsent(c.info.source(), k -> new ArrayList<>()).add(c);

            reSortCards();
        } catch(Exception e) {
            Ultimates.getPlugin().getLogger().warning("Failed to initialize card (" + card.getName() + "): " + e.getMessage());
            e.printStackTrace();
        }
    }

    public void removeCard(@NotNull Class<? extends Card> card) {
        cards.remove(card);
        String cardName = card.getAnnotation(CardInfo.class).name();
        cardsByName.remove(cardName);
        cardNames.remove(cardName);

        Card c = cardInstances.remove(card);
        if(c == null)
            return;

        cardInstancesByName.remove(c.info.name());
        cardInstancesByTier.computeIfAbsent(c.info.tier(), k -> new ArrayList<>()).remove(c);
        cardInstancesByPrimal.computeIfAbsent(c.info.source(), k -> new ArrayList<>()).remove(c);

        reSortCards();
    }

    // TODO Optimize this to not be n^2 runtime in conjunction with startup.
    private void reSortCards() {
        cardInstancesByTierAscending.clear();
        cardInstancesByTierDescending.clear();
        cardInstancesByPrimalAscending.clear();

        for(Tier t : Tier.values()) {
            cardInstancesByTierAscending.addAll(cardInstancesByTier.getOrDefault(t, new ArrayList<>()));
            cardInstancesByTierDescending.addAll(cardInstancesByTier.getOrDefault(t, new ArrayList<>()));
        }
        Collections.reverse(cardInstancesByTierDescending);

        for(PrimalSource ps : PrimalSource.values())
            cardInstancesByPrimalAscending.addAll(cardInstancesByPrimal.getOrDefault(ps, new ArrayList<>()));
    }

    @NotNull
    public List<Class<? extends Card>> getCards() {
        return Collections.unmodifiableList(cards);
    }

    @NotNull
    public Map<String, Class<? extends Card>> getCardByName() {
        return Collections.unmodifiableMap(cardsByName);
    }

    @Nullable
    public <T extends Card> T getCardInstance(@NotNull Class<T> card) {
        return (T) cardInstances.get(card);
    }

    @Nullable
    public Card getCardInstanceByName(@NotNull String name) {
        return cardInstancesByName.get(name);
    }

    @NotNull
    public List<Card> getCardInstances() {
        return getCardInstancesByPrimal();
    }

    @NotNull
    public List<Card> getCardInstancesByTierAscending() {
        return Collections.unmodifiableList(cardInstancesByTierAscending);
    }

    @NotNull
    public List<Card> getCardInstancesByTierDescending() {
        return Collections.unmodifiableList(cardInstancesByTierDescending);
    }

    @NotNull
    public List<Card> getCardInstancesByPrimal() {
        return Collections.unmodifiableList(cardInstancesByPrimalAscending);
    }

    @NotNull
    public List<Card> getCardInstancesFilterTier(@NotNull Tier tier) {
        return Collections.unmodifiableList(cardInstancesByTier.getOrDefault(tier, new ArrayList<>()));
    }

    @Nullable
    public static CardInfo getInfo(@NotNull Class<? extends Card> card) {
        return card.getAnnotation(CardInfo.class);
    }

    public static Cards getInstance() {
        return INSTANCE;
    }
}
