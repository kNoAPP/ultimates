package com.knoban.ultimates.cards;

import com.google.firebase.database.*;
import org.bukkit.Bukkit;
import com.knoban.atlas.callbacks.Callback;
import com.knoban.ultimates.Ultimates;
import com.knoban.ultimates.cardholder.CardHolder;
import com.knoban.ultimates.events.CardDiscardEvent;
import com.knoban.ultimates.events.CardDrawEvent;
import com.knoban.ultimates.events.CardRegisterEvent;
import com.knoban.ultimates.events.CardUnregisterEvent;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;

public abstract class Card implements Listener {

    public static final String METADATA_MAGNITUDE = "ults_general_magnitude";
    public static final String LOCKED_METADATA_LORE = "§4§oCard Item";

    protected final Ultimates plugin;
    protected final HashSet<Player> drawn = new HashSet<>();
    protected final CardInfo info = getClass().getAnnotation(CardInfo.class);

    protected boolean enabled;
    protected ItemStack unownedCantBuyIcon, unownedCanBuyIcon, unownedIcon, ownedIcon, drawnIcon;

    private final DatabaseReference extrasReference, playerReference;
    private final ValueEventListener playerDataListener, extrasDataListener;
    private HashSet<UUID> writeLock = new HashSet<>(); // This prevents data from being written if not read once first.

    protected Card(Ultimates plugin) {
        this.plugin = plugin;
        this.enabled = true;

        extrasReference = plugin.getFirebase().getDatabase().getReference("/cards/" + info.name() + "/c_data");
        extrasDataListener = new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot snapshot) {
                if(!snapshot.exists())
                    plugin.getServer().getScheduler().runTask(plugin, () -> onDefaultExtraData());
                else
                    plugin.getServer().getScheduler().runTask(plugin, () -> onExtraData((Map<String, Object>) snapshot.getValue()));

            }

            @Override
            public void onCancelled(DatabaseError error) {
                plugin.getLogger().info("Failed to load c_data " + info.name()
                        + ": " + error.getMessage());
            }
        };

        playerReference = plugin.getFirebase().getDatabase().getReference("/cards/" + info.name() + "/p_data");
        playerDataListener = new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot snapshot) {
                if(!snapshot.exists())
                    plugin.getServer().getScheduler().runTask(plugin, () -> {
                        // As pointed out by Gere, without a check here we could be loading data we shouldn't
                        // because a player could have discarded the card in the off-chance this takes more than
                        // 2 ticks to resolve from the time the data is request.
                        UUID uuid = UUID.fromString(snapshot.getKey());
                        Player p = Bukkit.getPlayer(uuid);
                        if(p != null && drawn.contains(p)) {
                            writeLock.remove(p.getUniqueId());
                            onDefaultPlayerData(p);
                        }
                    });
                else
                    plugin.getServer().getScheduler().runTask(plugin, () -> {
                        // As pointed out by Gere, without a check here we could be loading data we shouldn't
                        // because a player could have discarded the card in the off-chance this takes more than
                        // 2 ticks to resolve from the time the data is request.
                        UUID uuid = UUID.fromString(snapshot.getKey());
                        Player p = Bukkit.getPlayer(uuid);
                        if(p != null && drawn.contains(p)) {
                            writeLock.remove(p.getUniqueId());
                            onPlayerData(p, (Map<String, Object>) snapshot.getValue());
                        }
                    });
            }

            @Override
            public void onCancelled(DatabaseError error) {
                plugin.getLogger().info("Failed to read p_data " + info.name() + ": " + error.getMessage());
            }
        };
        cacheItemStacks();
    }

    public void cacheItemStacks() {
        // Unowned Can't Buy ItemStack
        unownedCantBuyIcon = new ItemStack(info.material());
        ItemMeta im = unownedCantBuyIcon.getItemMeta();
        im.setDisplayName(info.display());
        List<String> lore = new ArrayList<String>();
        String cost = getCost() < Integer.MAX_VALUE
                ? "§c§oCannot afford §7- §b" + getCost() + " wisdom"
                : "§c§oCannot afford §7- §5§oPriceless";
        lore.add(cost);
        lore.add(info.source().getDisplay() + " §f- " + info.tier().getDisplay());
        lore.add("");
        lore.addAll(Arrays.asList(info.description()));
        if(!enabled) {
            lore.add("");
            lore.add("§4§lCurrently Disabled!");
        }
        im.setLore(lore);
        im.addItemFlags(ItemFlag.HIDE_ATTRIBUTES, ItemFlag.HIDE_ENCHANTS,
                ItemFlag.HIDE_POTION_EFFECTS, ItemFlag.HIDE_UNBREAKABLE);
        unownedCantBuyIcon.setItemMeta(im);

        // Unowned Can Buy ItemStack
        unownedCanBuyIcon = unownedCantBuyIcon.clone();
        im = unownedCanBuyIcon.getItemMeta();
        lore = im.getLore();
        cost = getCost() < Integer.MAX_VALUE
                ? "§a§oClick to Buy! §7- §b" + getCost() + " wisdom"
                : "§a§oClick to Buy! §7- §5§oPriceless";
        lore.set(0, cost);
        im.setLore(lore);
        unownedCanBuyIcon.setItemMeta(im);

        // Unowned ItemStack
        unownedIcon = unownedCantBuyIcon.clone();
        im = unownedIcon.getItemMeta();
        lore = im.getLore();
        lore.set(0, "");
        im.setLore(lore);
        unownedIcon.setItemMeta(im);

        // Owned ItemStack
        ownedIcon = unownedCantBuyIcon.clone();
        im = ownedIcon.getItemMeta();
        lore = im.getLore();
        lore.set(0, "§2§oOwned");
        im.setLore(lore);
        ownedIcon.setItemMeta(im);

        // Equipped ItemStack
        drawnIcon = unownedCantBuyIcon.clone();
        im = drawnIcon.getItemMeta();
        im.addEnchant(Enchantment.ARROW_INFINITE, 1, true);
        lore = im.getLore();
        lore.set(0, "§d§oDrawn");
        im.setLore(lore);
        drawnIcon.setItemMeta(im);
    }

    public ItemStack getUnownedCantBuyIcon() {
        return unownedCantBuyIcon.clone();
    }

    public ItemStack getUnownedCanBuyIcon() {
        return unownedCanBuyIcon.clone();
    }

    public ItemStack getUnownedIcon() {
        return unownedIcon.clone();
    }

    public ItemStack getOwnedIcon() {
        return ownedIcon.clone();
    }

    public ItemStack getDrawnIcon() {
        return drawnIcon.clone();
    }

    public CardInfo getInfo() {
        return info;
    }

    /**
     * This method intentionally ignores the isEnabled method. It
     * will always draw the card.
     * @param p - The player to draw the card for
     * @return true if the card wasn't already equipped
     */
    public boolean draw(Player p) {
        boolean toRet = drawn.add(p);
        if(toRet) {
            if(drawn.size() == 1)
                register();

            writeLock.add(p.getUniqueId()); // Prevent writes until one read has completed.
            playerReference.child(p.getUniqueId().toString()).addValueEventListener(playerDataListener);

            CardDrawEvent drawEvent = new CardDrawEvent(p, this);
            plugin.getServer().getPluginManager().callEvent(drawEvent);
        }
        return toRet;
    }

    public Set<Player> getDrawnPlayers() {
        return Collections.unmodifiableSet(drawn);
    }

    /**
     * This method intentionally ignores the isEnabled method. It
     * will always discard the card.
     * @param p - The player to discard the card for
     * @return true if the card wasn't already discarded
     */
    public boolean discard(Player p) {
        boolean toRet = drawn.remove(p);
        if(toRet) {
            CardDiscardEvent discardEvent = new CardDiscardEvent(p, this);
            plugin.getServer().getPluginManager().callEvent(discardEvent);

            playerReference.child(p.getUniqueId().toString()).removeEventListener(playerDataListener);

            if(drawn.size() == 0)
                unregister();
        }
        return toRet;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
        cacheItemStacks();

        if(!enabled) {
            for(Player p : new ArrayList<>(drawn)) {
                CardHolder holder = CardHolder.getCardHolder(p);
                if(holder.isLoaded())
                    holder.discardCards(this);
            }
        }
    }

    /**
     * Called when data must be created for this card for the first time.
     * Runs on main game thread.
     */
    public void onDefaultExtraData() {
        // By default, do nothing. This can be overridden on a per-card basis.
    }

    /**
     * Called when player data must be created for the first time.
     * Runs on main game thread.
     */
    public void onDefaultPlayerData(Player p) {
        // By default, do nothing. This can be overridden on a per-card basis.
    }

    /**
     * Called when new data becomes available for the card. This will be called whenever data is written
     * to firebase that concerns the Extra.
     * Runs on main game thread.
     */
    public void onExtraData(Map<String, Object> data) {
        // By default, do nothing. This can be overridden on a per-card basis.
    }

    /**
     * Called when new data becomes available for the player. This will be called whenever data is written
     * to firebase that concerns the player.
     * Runs on main game thread.
     */
    public void onPlayerData(Player p, Map<String, Object> data) {
        // By default, do nothing. This can be overridden on a per-card basis.
    }

    /**
     * Write data to the database that concerns a player. This will in turn call {@link #onPlayerData(Player, Map)}
     * The callback is async.
     */
    protected void writeData(@NotNull UUID uuid, @NotNull String child, @Nullable Object value,
                             @Nullable Callback onSuccess) {
        Map<String, Object> data = new TreeMap<>();
        data.put(child, value);

        writeData(uuid, data, onSuccess);
    }

    /**
     * Write data to the database that concerns a player. This will in turn call {@link #onPlayerData(Player, Map)}
     * The callback is async.
     */
    protected void writeData(@NotNull UUID uuid, @NotNull Map<String, Object> data,
                        @Nullable Callback onSuccess) {
        if(writeLock.contains(uuid))
            return;

        playerReference.child(uuid.toString()).updateChildren(data, (error, ref) -> {
                if(error != null) {
                    plugin.getLogger().info("Failed to save p_data " + info.name()
                            + " (" + uuid + "): " + error.getMessage());
                } else {
                    onSuccess.call();
                }
            });
    }

    /**
     * Increment long data to the database that concerns a player. This will in turn call {@link #onPlayerData(Player, Map)}
     * The callback is async.
     */
    protected void incrementData(@NotNull UUID uuid, @NotNull String key, long amount,
                                 @Nullable Callback onSuccess) {
        if(writeLock.contains(uuid))
            return;

        playerReference.child(uuid.toString()).child(key).runTransaction(new Transaction.Handler() {
            @Override
            public Transaction.Result doTransaction(MutableData currentData) {
                Long currentValue = (Long) currentData.getValue();
                currentData.setValue(currentValue != null ? currentValue + amount : amount);
                return Transaction.success(currentData);
            }

            @Override
            public void onComplete(DatabaseError error, boolean b, DataSnapshot dataSnapshot) {
                if(error != null)
                    plugin.getLogger().info("Failed to save p_data " + info.name()
                            + " (" + uuid + "): " + error.getMessage());
                else if(onSuccess != null)
                    onSuccess.call();
            }
        }, false);
    }

    /**
     * Write data to the database that concerns the card. This will in turn call {@link #onExtraData(Map)}
     * The callback is async.
     */
    protected void writeData(@NotNull String child, @Nullable Object value,
                             @Nullable Callback onSuccess) {
        Map<String, Object> data = new TreeMap<>();
        data.put(child, value);

        writeData(data, onSuccess);
    }

    /**
     * Write data to the database that concerns the card. This will in turn call {@link #onExtraData(Map)}
     * The callback is async.
     */
    protected void writeData(@NotNull Map<String, Object> data, @Nullable Callback onSuccess) {
        extrasReference.updateChildren(data, (error, ref) -> {
            if(error != null) {
                plugin.getLogger().info("Failed to save extras " + info.name()
                        + ": " + error.getMessage());
            } else {
                onSuccess.call();
            }
        });
    }

    /**
     * Increment long data to the database that concerns the card. This will in turn call {@link #onExtraData(Map)}
     * The callback is async.
     */
    protected void incrementData(@NotNull String key, long amount, @Nullable Callback onSuccess) {
        extrasReference.child(key).runTransaction(new Transaction.Handler() {
            @Override
            public Transaction.Result doTransaction(MutableData currentData) {
                Long currentValue = (Long) currentData.getValue();
                currentData.setValue(currentValue != null ? currentValue + amount : amount);
                return Transaction.success(currentData);
            }

            @Override
            public void onComplete(DatabaseError error, boolean b, DataSnapshot dataSnapshot) {
                if(error != null)
                    plugin.getLogger().info("Failed to save extras " + info.name()
                            + " (" + key + "): " + error.getMessage());
                else if(onSuccess != null)
                    onSuccess.call();
            }
        }, false);
    }

    @Nullable
    public Integer getCost() {
        return info.tier().getDefaultCost();
    }

    protected void register() {
        plugin.getServer().getPluginManager().registerEvents(this, plugin);
        extrasReference.addValueEventListener(extrasDataListener);

        plugin.getServer().getPluginManager().callEvent(new CardRegisterEvent(this));
    }

    protected void unregister() {
        plugin.getServer().getPluginManager().callEvent(new CardUnregisterEvent(this));

        HandlerList.unregisterAll(this);
        extrasReference.removeEventListener(extrasDataListener);
    }
}
