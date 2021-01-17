package com.knoban.ultimates.cardholder;

import com.knoban.atlas.gui.GUI;
import com.knoban.atlas.gui.GUIClickable;
import com.knoban.atlas.utils.SoundBundle;
import com.knoban.ultimates.Ultimates;
import com.knoban.ultimates.aspects.Items;
import com.knoban.ultimates.cards.Card;
import com.knoban.ultimates.cards.Cards;
import com.knoban.ultimates.cards.base.Silenceable;
import com.knoban.ultimates.primal.PrimalSource;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.function.Consumer;

public class CardHolder extends Holder {

    private static final HashMap<Player, CardHolder> players = new HashMap<>();

    private final Player player;
    private long loggedInTime;

    private CardFlash ongoingFlash;

    /**
     * Create a CardHolder to get data from a player.
     * @param plugin - Ultimates plugin instance
     * @param player - Player to create CardHolder from
     */
    private CardHolder(@NotNull Ultimates plugin, @NotNull Player player) {
        super(plugin, player.getUniqueId(), player.getName());
        if(players.containsKey(player))
            throw new IllegalStateException("Illegal to call new PlayerData twice on logged in CardHolder. " +
                    "Use CardHolder.getPlayerData() or CardHolder#logout().");

        this.player = player;
    }

    /**
     * @return the Player this CardHolder is tied to
     */
    @NotNull
    public Player getPlayer() {
        return player;
    }

    @Override
    public void setPrimalSource(PrimalSource primalSource) {
        this.primalSource.getTeam().removeEntry(player.getName());
        super.setPrimalSource(primalSource);
        this.primalSource.getTeam().addEntry(player.getName());
    }

    /**
     * Equip cards to the CardHolder
     * @param cards - The cards to equip. If no override, will equip as many cards as possible in the order given
     * @return true if the equipped set of cards changed as a result of the call
     */
    @Override
    public boolean drawCards(@NotNull Card... cards) {
        boolean toRet = false;
        for(Card card : cards) {
            toRet = toRet || drawnCards.add(card);
            card.draw(player);
        }

        return toRet;
    }

    /**
     * Equip cards to the CardHolder
     * @param cards - The cards to equip. If no override, will equip as many cards as possible in the order given
     * @return true if the equipped set of cards changed as a result of the call
     */
    @Override
    public boolean drawCards(@NotNull Collection<Card> cards) {
        boolean toRet = false;
        for(Card card : cards) {
            toRet = toRet || drawnCards.add(card);
            card.draw(player);
        }

        return toRet;
    }

    /**
     * Discard cards from the CardHolder
     * @param cards - The cards to discard
     * @return true if the equipped set of cards changed as a result of the call
     */
    @Override
    public boolean discardCards(@NotNull Card... cards) {
        boolean toRet = false;
        for(Card card : cards) {
            toRet = toRet || drawnCards.remove(card);
            card.discard(player);
        }

        return toRet;
    }

    /**
     * Discard cards from the CardHolder
     * @param cards - The cards to discard
     * @return true if the equipped set of cards changed as a result of the call
     */
    @Override
    public boolean discardCards(@NotNull Collection<Card> cards) {
        boolean toRet = false;
        for(Card card : cards) {
            toRet = toRet || drawnCards.remove(card);
            card.discard(player);
        }

        return toRet;
    }

    /**
     * Revoke a CardHolder's permission to draw a card. This will also update the Card's owner list.
     * @param cards - The cards to revoke the CardHolder's access to
     * @return false if the CardHolder doesn't own any the passed cards
     */
    @Override
    public boolean revokeCards(@NotNull Card... cards) {
        boolean toRet = false;
        for(Card card : cards) {
            toRet = toRet || ownedCards.remove(card);
            card.discard(player);
        }
        return toRet;
    }

    /**
     * Revoke a CardHolder's permission to draw a card. This will also update the Card's owner list.
     * @param cards - The cards to revoke the CardHolder's access to
     * @return false if the CardHolder doesn't own any the passed cards
     */
    @Override
    public boolean revokeCards(@NotNull Collection<Card> cards) {
        boolean toRet = false;
        for(Card card : cards) {
            toRet = toRet || ownedCards.remove(card);
            card.discard(player);
        }
        return toRet;
    }

    /**
     * Silences all cards in the game for a player. Silenced cards can be drawn and remain drawn, but their
     * affects are not applied. Silencing persists automatically between logouts.
     * <br>
     * There's a more performance friendly version of silencing for silencable cards. If you'd like to silence
     * specific cards for a player, check out {@link Silenceable} cards.
     * @param silenced True, if the player should be silenced.
     */
    public void setSilenced(boolean silenced) {
        for(Card card : Cards.getInstance().getCardInstances()) {
            if(card instanceof Silenceable) {
                Silenceable silenceable = (Silenceable) card;
                silenceable.setSilenced(player, silenced);
            }
        }
    }

    public static final String UNLOADED_MESSAGE = "§cYour data is still loading. Please wait...";

    public void openCardMenuGUI(@NotNull Player showTo) {
        openCardMenuGUI(showTo, true);
    }

    public void openCardMenuGUI(@NotNull Player showTo, boolean withOpenSounds) {
        if(!loaded) {
            showTo.sendMessage(UNLOADED_MESSAGE);
            return;
        }

        GUI gui = withOpenSounds ? new GUI(plugin, "Card Menu", 18,
                new SoundBundle(Sound.BLOCK_CHEST_OPEN, 1F, 0.9F),
                null,
                new SoundBundle(Sound.BLOCK_WOODEN_BUTTON_CLICK_ON, 1F, 1F),
                new SoundBundle(Sound.ENTITY_LINGERING_POTION_THROW, 1F, 1.5F))
                : new GUI(plugin, "Card Menu", 18,
                null,
                null,
                new SoundBundle(Sound.BLOCK_WOODEN_BUTTON_CLICK_ON, 1F, 1F),
                        new SoundBundle(Sound.ENTITY_LINGERING_POTION_THROW, 1F, 1.5F));

        // Explanation Item
        gui.setSlot(4, Items.CARD_MENU_EXPLANATION_ITEM);

        GUIClickable yourDeck = new GUIClickable();
        yourDeck.setActionOnClick((g, e) -> {
            openYourDeckGUI(showTo,0, false);
            showTo.playSound(showTo.getLocation(), Sound.BLOCK_WOODEN_BUTTON_CLICK_ON, 1F, 1F);
        });
        gui.setSlot(11, Items.YOUR_DECK_MENU_ITEM, yourDeck);

        GUIClickable allCards = new GUIClickable();
        allCards.setActionOnClick((g, e) -> {
            openShopGUI(showTo, 0, NO_SORT, false);
            showTo.playSound(showTo.getLocation(), Sound.BLOCK_WOODEN_BUTTON_CLICK_ON, 1F, 1F);
        });
        gui.setSlot(15, Items.SHOP_MENU_ITEM, allCards);

        gui.openInv(showTo);
    }

    private static final int YOUR_DECK_CARDS_PER_PAGE = 21; // Range: 0 - 28

    /**
     * Opens a GUI for the CardHolder to draw or discard from their owned cards
     * @param showTo The player to show the deck to
     * @param page The page number to open containing their owned cards
     */
    public void openYourDeckGUI(@NotNull Player showTo, final int page) {
        openYourDeckGUI(showTo, page, true);
    }

    private void openYourDeckGUI(@NotNull Player showTo, final int page, boolean withOpenSounds) {
        if(!loaded) {
            showTo.sendMessage(UNLOADED_MESSAGE);
            return;
        }

        if(!hasYourDeckPage(page))
            throw new IndexOutOfBoundsException("Not a valid card page: " + page);

        GUI gui = withOpenSounds ? new GUI(plugin, "Your Deck", 54,
                new SoundBundle(Sound.BLOCK_CHEST_OPEN, 1F, 0.9F),
                null,
                new SoundBundle(Sound.BLOCK_WOODEN_BUTTON_CLICK_ON, 1F, 1F),
                new SoundBundle(Sound.ENTITY_LINGERING_POTION_THROW, 1F, 1.5F))
                : new GUI(plugin, "Your Deck", 54,
                null,
                null,
                new SoundBundle(Sound.BLOCK_WOODEN_BUTTON_CLICK_ON, 1F, 1F),
                new SoundBundle(Sound.ENTITY_LINGERING_POTION_THROW, 1F, 1.5F));

        // Back Button
        GUIClickable back = new GUIClickable();
        back.setActionOnClick((g, e) -> {
            openCardMenuGUI(showTo, false);
            showTo.playSound(showTo.getLocation(), Sound.BLOCK_WOODEN_BUTTON_CLICK_ON, 1F, 1F);
        });
        gui.setSlot(0, Items.BACK_ITEM, back);

        // Explanation Item
        gui.setSlot(4, Items.OWNED_CARD_EXPLANATION_ITEM);

        // Card Selection
        int startFromCard = page * YOUR_DECK_CARDS_PER_PAGE;
        int endAtCard = startFromCard + YOUR_DECK_CARDS_PER_PAGE;
        int slot = 10;
        for(int i = startFromCard; i < ownedCards.size() && i < endAtCard; i++) {
            Card card = ownedCards.get(i);
            if(!drawnCards.contains(card)) {
                gui.setSlot(slot, card.getOwnedIcon(), getCardClickable(showTo, card, page, slot));
            }
            ++slot;
            if(slot == 17 || slot == 26 || slot == 35)
                slot += 2;
        }

        // Previous page
        if(hasYourDeckPage(page-1)) {
            GUIClickable click = new GUIClickable();
            click.setActionOnClick((g, e) -> {
                openYourDeckGUI(showTo,page - 1, false);
                showTo.playSound(showTo.getLocation(), Sound.BLOCK_WOODEN_BUTTON_CLICK_ON, 1F, 1F);
            });
            gui.setSlot(36, Items.PREVIOUS_PAGE, click);
        }

        // Next Page
        if(hasYourDeckPage(page+1)) {
            GUIClickable click = new GUIClickable();
            click.setActionOnClick((g, e) -> {
                openYourDeckGUI(showTo,page + 1, false);
                showTo.playSound(showTo.getLocation(), Sound.BLOCK_WOODEN_BUTTON_CLICK_ON, 1F, 1F);
            });
            gui.setSlot(44, Items.NEXT_PAGE, click);
        }

        // Pointers
        gui.setSlot(45, Items.CARD_POINTER);

        addDrawnBar(showTo, gui, page);

        // TODO Add ultimate card here in slot 52.
        // gui.setSlot(51, Items.ULTIMATE_POINTER_L);
        // gui.setSlot(53, Items.ULTIMATE_POINTER_R);

        gui.openInv(showTo);
    }

    // Helper methods
    @NotNull
    private GUIClickable getCardClickable(@NotNull Player showTo, @NotNull Card card, final int page, final int slot) {
        GUIClickable click = new GUIClickable();
        click.setActionOnClick((g, e) -> {
            if(!loaded) {
                showTo.sendMessage(UNLOADED_MESSAGE);
                return;
            }

            if(drawnCards.contains(card)) {
                discardCards(card);
                PrimalSource source = PrimalSource.getSourceFromCards(drawnCards);
                if(getPrimalSource() != source)
                    setPrimalSource(source);

                showTo.sendMessage(card.getInfo().display() + " §7discarded!");
                showTo.playSound(showTo.getLocation(), Sound.BLOCK_NOTE_BLOCK_BELL, 1F, 0.8F);
            } else {
                if(!card.isEnabled()) {
                    showTo.sendMessage("§cThis card has been disabled for everyone by the administrator.");
                    showTo.playSound(showTo.getLocation(), Sound.BLOCK_NOTE_BLOCK_BASS, 1F, 1F);
                } else if(!ownedCards.contains(card)) {
                    showTo.sendMessage("§cYou do not own this card!");
                    showTo.playSound(showTo.getLocation(), Sound.BLOCK_NOTE_BLOCK_BASS, 1F, 1F);
                } else if(drawnCards.size() < getMaxCardSlots()) {
                    drawCards(card);
                    PrimalSource source = PrimalSource.getSourceFromCards(drawnCards);
                    if(getPrimalSource() != source)
                        setPrimalSource(source);

                    showTo.sendMessage(card.getInfo().display() + " §7drawn!");
                    showTo.playSound(showTo.getLocation(), Sound.BLOCK_NOTE_BLOCK_BELL, 1F, 1F);
                } else {
                    showTo.sendMessage("§cYou're out of card space! §fDiscard some cards or earn more card slots.");
                    showTo.playSound(showTo.getLocation(), Sound.BLOCK_NOTE_BLOCK_BASS, 1F, 1F);
                }
            }

            openYourDeckGUI(showTo, page, false);
        });
        return click;
    }

    private void addDrawnBar(@NotNull Player showTo, @NotNull GUI gui, final int page) {
        // Drawn Abilities
        final int drawnSlot = 46;
        final int guiSlots = Math.min(getMaxCardSlots(), 8);
        for(int i=0; i<guiSlots; i++) {
            if(i < drawnCards.size()) {
                Card card = drawnCards.get(i);
                gui.setSlot(drawnSlot+i, card.getDrawnIcon(),
                        getCardClickable(showTo, card, page, drawnSlot+i));
            } else
                gui.setSlot(drawnSlot+i, Items.CARD_SLOT_OPEN);
        }

        for(int i=guiSlots; i<8; i++) {
            gui.setSlot(drawnSlot+i, Items.CARD_SLOT_LOCKED);
        }
    }

    /**
     * Checks if a CardHolder has a specific page in their card selection GUI
     * Useful for openCardSelectionGUI
     * @param page - The page to check against
     * @return true if they have the page
     */
    public boolean hasYourDeckPage(int page) {
        return 0 <= page && page <= ownedCards.size() / YOUR_DECK_CARDS_PER_PAGE;
    }

    private static final int SHOP_CARDS_PER_PAGE = 36;
    public static final short NO_SORT = 0;
    public static final short ASC_SORT = 1;
    public static final short DESC_SORT = 2;
    public static final short PRIMAL_SORT = 3;

    public void openShopGUI(@NotNull Player showTo, final int page, final short strat) {
        openShopGUI(showTo, page, strat, true);
    }

    private void openShopGUI(@NotNull Player showTo, final int page, final short strat, boolean withOpenSounds) {
        if(!loaded) {
            showTo.sendMessage(UNLOADED_MESSAGE);
            return;
        }

        if(!hasAllCardsPage(page))
            throw new IndexOutOfBoundsException("Not a valid card page: " + page);

        GUI gui = withOpenSounds ? new GUI(plugin, "The Shop", 54,
                new SoundBundle(Sound.BLOCK_CHEST_OPEN, 1F, 0.9F),
                null,
                new SoundBundle(Sound.BLOCK_WOODEN_BUTTON_CLICK_ON, 1F, 1F),
                new SoundBundle(Sound.ENTITY_LINGERING_POTION_THROW, 1F, 1.5F))
                : new GUI(plugin, "The Shop", 54,
                null,
                null,
                new SoundBundle(Sound.BLOCK_WOODEN_BUTTON_CLICK_ON, 1F, 1F),
                new SoundBundle(Sound.ENTITY_LINGERING_POTION_THROW, 1F, 1.5F));

        // Back Button
        GUIClickable back = new GUIClickable();
        back.setActionOnClick((g, e) -> {
            openCardMenuGUI(showTo, false);
            showTo.playSound(showTo.getLocation(), Sound.BLOCK_WOODEN_BUTTON_CLICK_ON, 1F, 1F);
        });
        gui.setSlot(0, Items.BACK_ITEM, back);

        // Explanation Item
        gui.setSlot(4, Items.SHOP_EXPLANATION_ITEM);

        // Card Selection
        List<Card> cards;
        Cards cardsContainer = Cards.getInstance();
        switch(strat) {
            case NO_SORT:
                cards = cardsContainer.getCardInstances();
                break;

            case ASC_SORT:
                cards = cardsContainer.getCardInstancesByTierAscending();
                break;

            case DESC_SORT:
                cards = cardsContainer.getCardInstancesByTierDescending();
                break;

            case PRIMAL_SORT:
                cards = cardsContainer.getCardInstancesByPrimal();
                break;

            default:
                cards = cardsContainer.getCardInstances();
                break;
        }
        int startFromCard = page * SHOP_CARDS_PER_PAGE;
        int endAtCard = startFromCard + SHOP_CARDS_PER_PAGE;
        int slot = 9;
        for(int i = startFromCard; i < cards.size() && i < endAtCard; i++) {
            Card card = cards.get(i);
            ItemStack icon = ownedCards.contains(card)
                    ? (drawnCards.contains(card) ? card.getDrawnIcon() : card.getOwnedIcon())
                    : (wisdom >= card.getCost() ? card.getUnownedCanBuyIcon() : card.getUnownedCantBuyIcon());

            GUIClickable click = new GUIClickable();
            click.setActionOnClick((g, e) -> {
                if(!ownedCards.contains(card)) {
                    if(card.getCost() != Integer.MAX_VALUE) {
                        if(wisdom >= card.getCost()) {
                            openBuyCardGUI(showTo, card, false);
                        } else {
                            showTo.sendMessage("§cYou don't have enough wisdom to purchase this card!");
                            showTo.sendMessage("§7§oGain more wisdom by completing missions in the battle pass!");
                            // TODO Give more info about the battle pass.
                        }
                    } else
                        showTo.sendMessage("§cThis card is §5priceless§c. It cannot be bought.");
                } else {
                    showTo.sendMessage("§cYou already own this card!");
                    if(!drawnCards.contains(card))
                        showTo.sendMessage("§7You can equip this card from the \"Your Deck\" menu! Use /card menu.");
                }
                showTo.playSound(showTo.getLocation(), Sound.BLOCK_WOODEN_BUTTON_CLICK_ON, 1F, 1F);
            });

            gui.setSlot(slot, icon, click);

            ++slot;
        }

        // Previous page
        if(hasAllCardsPage(page-1)) {
            GUIClickable click = new GUIClickable();
            click.setActionOnClick((g, e) -> {
                openShopGUI(showTo,page - 1, strat, false);
                showTo.playSound(showTo.getLocation(), Sound.BLOCK_WOODEN_BUTTON_CLICK_ON, 1F, 1F);
            });
            gui.setSlot(45, Items.PREVIOUS_PAGE, click);
        }

        // Sorting Strategies
        GUIClickable noSort = new GUIClickable();
        noSort.setActionOnClick((g, e) -> {
            showTo.playSound(showTo.getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1F, 1F);
            openShopGUI(showTo, page, NO_SORT, false);
        });
        gui.setSlot(46, Items.ALL_CARDS_NO_SORT_ITEM, noSort);

        GUIClickable ascSort = new GUIClickable();
        ascSort.setActionOnClick((g, e) -> {
            showTo.playSound(showTo.getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1F, 1F);
            openShopGUI(showTo, page, ASC_SORT, false);
        });
        gui.setSlot(48, Items.ALL_CARDS_ASC_SORT_ITEM, ascSort);

        GUIClickable descSort = new GUIClickable();
        descSort.setActionOnClick((g, e) -> {
            showTo.playSound(showTo.getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1F, 1F);
            openShopGUI(showTo, page, DESC_SORT, false);
        });
        gui.setSlot(50, Items.ALL_CARDS_DESC_SORT_ITEM, descSort);

        GUIClickable primalSort = new GUIClickable();
        primalSort.setActionOnClick((g, e) -> {
            showTo.playSound(showTo.getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1F, 1F);
            openShopGUI(showTo, page, PRIMAL_SORT, false);
        });
        gui.setSlot(52, Items.ALL_CARDS_PRIMAL_SORT_ITEM, primalSort);

        // Next Page
        if(hasAllCardsPage(page+1)) {
            GUIClickable click = new GUIClickable();
            click.setActionOnClick((g, e) -> {
                openShopGUI(showTo,page + 1, strat, false);
                showTo.playSound(showTo.getLocation(), Sound.BLOCK_WOODEN_BUTTON_CLICK_ON, 1F, 1F);
            });
            gui.setSlot(53, Items.NEXT_PAGE, click);
        }

        gui.openInv(showTo);
    }

    /**
     * Checks if a CardHolder has a specific page in their all cards GUI
     * Useful for openAllCardsGUI
     * @param page - The page to check against
     * @return true if they have the page
     */
    public boolean hasAllCardsPage(int page) {
        return 0 <= page && page <= Cards.getInstance().getCardInstances().size() / SHOP_CARDS_PER_PAGE;
    }

    public void openBuyCardGUI(@NotNull Player showTo, @NotNull Card toBuy, boolean withOpenSounds) {
        if(!loaded) {
            showTo.sendMessage(UNLOADED_MESSAGE);
            return;
        }

        GUI gui = withOpenSounds ? new GUI(plugin, "Complete your Purchase", 45,
                new SoundBundle(Sound.BLOCK_CHEST_OPEN, 1F, 0.9F),
                null,
                new SoundBundle(Sound.BLOCK_WOODEN_BUTTON_CLICK_ON, 1F, 1F),
                new SoundBundle(Sound.ENTITY_LINGERING_POTION_THROW, 1F, 1.5F))
                : new GUI(plugin, "Complete your Purchase", 45,
                null,
                null,
                new SoundBundle(Sound.BLOCK_WOODEN_BUTTON_CLICK_ON, 1F, 1F),
                new SoundBundle(Sound.ENTITY_LINGERING_POTION_THROW, 1F, 1.5F));

        ItemStack ownedWisdom = new ItemStack(Material.ENCHANTING_TABLE);
        ItemMeta metadata = ownedWisdom.getItemMeta();
        metadata.setDisplayName("§5Wisdom");
        List<String> lore = new ArrayList<>();
        lore.add("§eCurrent: §7" + wisdom);
        lore.add("§bAfter:     §7" + (wisdom - toBuy.getCost()));
        metadata.setLore(lore);
        ownedWisdom.setItemMeta(metadata);

        gui.setSlot(4, toBuy.getUnownedCanBuyIcon());
        gui.setSlot(31, ownedWisdom);

        GUIClickable decline = new GUIClickable();
        decline.setActionOnClick((g, e) -> {
            showTo.playSound(showTo.getLocation(), Sound.ENCHANT_THORNS_HIT, 1F, 0.8F);
            showTo.sendMessage("§4Purchase declined!");
            openShopGUI(showTo, 0, NO_SORT, false);
        });

        GUIClickable accept = new GUIClickable();
        accept.setActionOnClick((g, e) -> {
            ownedCards.add(toBuy);
            wisdom -= toBuy.getCost();

            showTo.playSound(showTo.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1F, 0.8F);
            showTo.playSound(showTo.getLocation(), Sound.UI_TOAST_CHALLENGE_COMPLETE, 1F, 1F);
            showTo.sendMessage("§2Purchase accepted! §7Enjoy your new card: " + toBuy.getInfo().display());
            showTo.closeInventory();
        });

        gui.setSlot(18, Items.DECLINE_ITEM, decline);
        gui.setSlot(19, Items.DECLINE_ITEM, decline);
        gui.setSlot(20, Items.DECLINE_ITEM, decline);
        gui.setSlot(27, Items.DECLINE_ITEM, decline);
        gui.setSlot(28, Items.DECLINE_ITEM, decline);
        gui.setSlot(29, Items.DECLINE_ITEM, decline);
        gui.setSlot(36, Items.DECLINE_ITEM, decline);
        gui.setSlot(37, Items.DECLINE_ITEM, decline);
        gui.setSlot(38, Items.DECLINE_ITEM, decline);

        gui.setSlot(24, Items.ACCEPT_ITEM, accept);
        gui.setSlot(25, Items.ACCEPT_ITEM, accept);
        gui.setSlot(26, Items.ACCEPT_ITEM, accept);
        gui.setSlot(33, Items.ACCEPT_ITEM, accept);
        gui.setSlot(34, Items.ACCEPT_ITEM, accept);
        gui.setSlot(35, Items.ACCEPT_ITEM, accept);
        gui.setSlot(42, Items.ACCEPT_ITEM, accept);
        gui.setSlot(43, Items.ACCEPT_ITEM, accept);
        gui.setSlot(44, Items.ACCEPT_ITEM, accept);

        gui.openInv(showTo);
    }

    public void openBattlePassMainGUI(@NotNull Player showTo, boolean withOpenSounds) {
        if(!loaded) {
            showTo.sendMessage(UNLOADED_MESSAGE);
            return;
        }

        GUI gui = withOpenSounds ? new GUI(plugin, "Battle Pass Menu", 18,
                new SoundBundle(Sound.BLOCK_CHEST_OPEN, 1F, 0.9F),
                null,
                new SoundBundle(Sound.BLOCK_WOODEN_BUTTON_CLICK_ON, 1F, 1F),
                new SoundBundle(Sound.ENTITY_LINGERING_POTION_THROW, 1F, 1.5F))
                : new GUI(plugin, "Battle Pass Menu", 18,
                null,
                null,
                new SoundBundle(Sound.BLOCK_WOODEN_BUTTON_CLICK_ON, 1F, 1F),
                new SoundBundle(Sound.ENTITY_LINGERING_POTION_THROW, 1F, 1.5F));

        gui.setSlot(4, Items.PASS_MAIN_EXPLANATION_ITEM);

        GUIClickable battlepass = new GUIClickable();
        battlepass.setActionOnClick((g, e) -> {
            showTo.playSound(showTo.getLocation(), Sound.BLOCK_WOODEN_BUTTON_CLICK_ON, 1F, 1F);
            plugin.getBattlepassManager().openBattlePassGUI(showTo, this, 0, false);
        });
        gui.setSlot(11, Items.PASS_PASS_MENU_ITEM, battlepass);

        GUIClickable missions = new GUIClickable();
        missions.setActionOnClick((g, e) -> {
            showTo.playSound(showTo.getLocation(), Sound.BLOCK_WOODEN_BUTTON_CLICK_ON, 1F, 1F);
            plugin.getMissionManager().openMissionGUI(showTo, this, false);
        });
        gui.setSlot(15, Items.PASS_MISSIONS_MENU_ITEM, missions);

        gui.openInv(showTo);
    }

    /**
     * @return An ongoing cardflash animation if present. Otherwise null.
     */
    @Nullable
    public CardFlash getOngoingFlash() {
        return ongoingFlash;
    }

    /**
     * Flash a CardHolder's drawn cards. Do not call this from onDisable
     * or you'll get a scheduling exception. Safe to call multiple consecutive times.
     */
    public void flashCards() {
        if(!loaded)
            return;

        if(ongoingFlash != null)
            ongoingFlash.invalidate();
        ongoingFlash = new CardFlash(plugin, this, () -> ongoingFlash = null);
    }

    /**
     * @return the amount of time in seconds the CardHolder has played on the server
     */
    @Override
    public int getTimePlayed() {
        return super.getTimePlayed() + (int) ((System.currentTimeMillis() - loggedInTime) / 1000L);
    }

    /**
     * Set the time played of the CardHolder in seconds
     * @param timePlayed - The amount in seconds to set
     */
    @Override
    public void setTimePlayed(int timePlayed) {
        super.setTimePlayed(timePlayed);
        this.loggedInTime = System.currentTimeMillis();
    }

    /**
     * To be called when a CardHolder initially logs in
     */
    public void login() {
        if(loaded)
            return;

        players.put(player, this);
        loggedInTime = System.currentTimeMillis();
        load(8000, true, (success, toDraw) -> {
            if(success) {
                for(Card draw : toDraw) {
                    if(draw.isEnabled() && ownedCards.contains(draw)) {
                        drawCards(draw);
                    } else {
                        player.sendMessage("§4An admin has disabled the card: " + draw.getInfo().display());
                        player.sendMessage("§4You had this card drawn. It has been temporarily discarded.");
                    }
                }
                primalSource.getTeam().addEntry(player.getName());

                plugin.getMissionManager().registerDataListener(uuid);
                checkForRewardFromBattlePass();
            } else {
                player.kickPlayer("§cFailed to load your CardHolder data, please try again!" +
                        "\n§cIf this problem continues, contact your server administrator." +
                        "\n§cGive them this error code: §4ults-3");
            }
        });
    }

    /**
     * To be called when a CardHolder logs out
     * @param shutdown True, if the logout was caused by a server shutdown
     */
    public void logout(boolean shutdown) {
        players.remove(player);

        if(!loaded) {
            removeMutex(shutdown);
            return;
        }

        plugin.getMissionManager().unregisterDataListener(uuid);

        if(ongoingFlash != null)
            ongoingFlash.invalidate();

        drawnCards.forEach((c) -> c.discard(player));
        primalSource.getTeam().removeEntry(player.getName());

        save(shutdown, null);
    }

    public static Collection<CardHolder> getCardHolders() {
        return Collections.unmodifiableCollection(players.values());
    }

    /**
     * Get a CardHolder instance given a Player instance. May be null if the {@link Player} is offline.
     * @param p - The player to get the CardHolder instance from
     * @return the CardHolder instance
     */
    public static CardHolder getCardHolder(@NotNull Player p) {
        return players.get(p);
    }

    /**
     * Get a new CardHolder instance given a Player instance
     * @param p - The player to get the CardHolder instance from
     * @param plugin - An instance of the Ultimates plugin
     * @return the CardHolder instance
     */
    @NotNull
    public static CardHolder getNewCardHolder(@NotNull Ultimates plugin, @NotNull Player p) {
        return new CardHolder(plugin, p);
    }
    
    /**
     * Gets all instances whose {@link #getPrimalSource()} equals the specified one.
     * Please keep in mind that the returned value is not cached.
     *
     * @param primal the {@link PrimalSource} to search for
     * @return all instances in the {@link PrimalSource}
     */
    public static Collection<CardHolder> getAllInPrimal(@NotNull PrimalSource primal) {
        Set<CardHolder> result = new HashSet<>();
        forEachInPrimal(primal, result::add);
        return result;
    }
    
    /**
     * Executes the specified callback for each instance whose
     * {@link #getPrimalSource()} equals the specified one.
     * Please keep in mind the lookup is not cached.
     *
     * @param primal the {@link PrimalSource} to search for
     * @param action the action to execute for each matching instance
     * @return the amount of times the callback was called
     */
    public static int forEachInPrimal(@NotNull PrimalSource primal, Consumer<CardHolder> action) {
        int counter = 0;
        for(CardHolder cardHolder : getCardHolders()) {
            if(cardHolder.getPrimalSource() == primal) {
                action.accept(cardHolder);
                counter++;
            }
        }
        return counter;
    }
}
