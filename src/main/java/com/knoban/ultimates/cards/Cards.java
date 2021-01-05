package com.knoban.ultimates.cards;

import com.knoban.ultimates.Ultimates;
import com.knoban.ultimates.cards.impl.*;
import com.knoban.ultimates.primal.PrimalSource;
import com.knoban.ultimates.primal.Tier;

import java.util.*;

public final class Cards {

    private static final List<Class<? extends Card>> cards = Arrays.asList(
            OOCRegenerationCard.class, CultivatorCard.class, WormCard.class, RubberSkinCard.class,
            ForceLevitationCard.class, StrangeBowCard.class, VeganCard.class, RubberProjectileCard.class,
            ZeroGravityProjectileCard.class, DeflectionCard.class, MagmaWalkerCard.class,
            SplashPotionOfGetHisAssCard.class, ScavengerCard.class, LumberjackCard.class, LuckCard.class, SoulCard.class,
            FallCard.class, TwinsCard.class, EnlightenedCard.class, PokeCard.class, TeemoCard.class, FlashbangCard.class,
            DruidCard.class, XRayCard.class, PortalCard.class, JuggernautCard.class, TankCard.class, HotHandsCard.class,
            DryadsGiftCard.class, RunnersDietCard.class, TerrorCard.class, PantherCard.class, SpeedCard.class,
            SteadyHandsCard.class, AnchorCard.class, UnyieldingMightCard.class, FalconCard.class, ParleyCard.class,
            SchoolingCard.class, ShadowsUpriseCard.class, RealityPhaseCard.class, SchoolingCard.class);

    private static final Cards INSTANCE = new Cards();

    private final List<String> cardNames;
    private final List<Card> cardInstances;
    private final List<Card> cardInstancesByTierAscending;
    private final List<Card> cardInstancesByTierDescending;
    private final List<Card> cardInstancesByPrimal;
    private final Map<String, Card> cardInstancesNameMap;
    private final Map<Class<? extends Card>, Card> cardInstancesClassMap;
    private final Map<Tier, ArrayList<Card>> cardInstancesTierMap;

    private Cards() {
        List<String> cardNames = new ArrayList<>();
        List<Card> cardInstances = new ArrayList<>();
        List<Card> cardInstancesByTierAscending = new ArrayList<>();
        List<Card> cardInstancesByTierDescending = new ArrayList<>();
        List<Card> cardInstancesByPrimal = new ArrayList<>();
        HashMap<Class<? extends Card>, Card> cardInstancesClassMap = new HashMap<>();
        HashMap<String, Card> cardInstancesNameMap = new HashMap<>();
        TreeMap<Tier, ArrayList<Card>> cardInstancesTierMap = new TreeMap<>();


        for(Class<? extends Card> card : cards) {
            try {
                Card c = card.getConstructor(Ultimates.class).newInstance(Ultimates.getPlugin());
                cardNames.add(c.info.name());
                cardInstances.add(c);
                cardInstancesClassMap.put(card, c);
                cardInstancesNameMap.put(c.info.name(), c);
            } catch(Exception ex) {
                ex.printStackTrace();
                Ultimates.getPlugin().getLogger().warning("Failed to initialize card (" + card.getName() + "): " + ex.getMessage());
            }
        }

        // This isn't optimized and could be. However, since it is only ran once, I'm taking the easier route.
        for(Tier t : Tier.values()) {
            ArrayList<Card> primalCards = new ArrayList<>();
            for(Card c : cardInstances) {
                if(c.info.tier() == t) {
                    cardInstancesByTierAscending.add(c);
                    primalCards.add(c);
                }
            }
            cardInstancesTierMap.put(t, primalCards);
        }

        for(PrimalSource ps : PrimalSource.values()) {
            for(Card c : cardInstances) {
                if(c.info.source() == ps) {
                    cardInstancesByPrimal.add(c);
                }
            }
        }

        cardInstancesByTierDescending.addAll(cardInstancesByTierAscending);
        Collections.reverse(cardInstancesByTierDescending);

        this.cardNames = Collections.unmodifiableList(cardNames);
        this.cardInstances = Collections.unmodifiableList(cardInstances);
        this.cardInstancesByTierAscending = Collections.unmodifiableList(cardInstancesByTierAscending);
        this.cardInstancesByTierDescending = Collections.unmodifiableList(cardInstancesByTierDescending);
        this.cardInstancesByPrimal = Collections.unmodifiableList(cardInstancesByPrimal);
        this.cardInstancesClassMap = Collections.unmodifiableMap(cardInstancesClassMap);
        this.cardInstancesNameMap = Collections.unmodifiableMap(cardInstancesNameMap);
        this.cardInstancesTierMap = Collections.unmodifiableMap(cardInstancesTierMap);
    }

    public List<String> getCardNames() {
        return cardNames;
    }

    public List<Card> getCardInstances() {
        return cardInstances;
    }

    public List<Card> getCardInstancesByTierAscending() {
        return cardInstancesByTierAscending;
    }

    public List<Card> getCardInstancesByTierDescending() {
        return cardInstancesByTierDescending;
    }

    public List<Card> getCardInstancesByPrimal() {
        return cardInstancesByPrimal;
    }

    public Card getCardInstance(String name) {
        return cardInstancesNameMap.get(name);
    }

    public List<Card> getCardInstancesFilterTier(Tier tier) {
        return cardInstancesTierMap.get(tier);
    }

    public <T extends Card> T getCardInstance(Class<T> card) {
        return (T) cardInstancesClassMap.get(card);
    }

    public static List<Class<? extends Card>> getCardClasses() {
        return cards;
    }

    public static CardInfo getInfo(Class<? extends Card> card) {
        return card.getAnnotation(CardInfo.class);
    }

    public static Cards getInstance() {
        return INSTANCE;
    }
}
