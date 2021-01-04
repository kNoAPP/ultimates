package com.knoban.ultimates.battlepass;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.ValueEventListener;
import com.knoban.atlas.gui.GUI;
import com.knoban.atlas.gui.GUIClickable;
import com.knoban.atlas.utils.SoundBundle;
import com.knoban.ultimates.Ultimates;
import com.knoban.ultimates.aspects.Items;
import com.knoban.ultimates.cardholder.CardHolder;
import com.knoban.ultimates.cardholder.Holder;
import com.knoban.ultimates.rewards.Reward;
import com.knoban.ultimates.rewards.SpecificReward;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.jetbrains.annotations.Nullable;

import java.util.*;

public class BattlePassManager {

    private final Ultimates plugin;

    private final DatabaseReference battlePassReference;
    private final ValueEventListener listener;

    private ArrayList<BattlePassLevel> battlePass = new ArrayList<>();

    public BattlePassManager(Ultimates plugin) {
        this.plugin = plugin;
        this.battlePassReference = plugin.getFirebase().getDatabase().getReference("/battlepass");

        battlePassReference.addValueEventListener(listener = new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot snapshot) {
                ArrayList<BattlePassLevel> newBattlepass = new ArrayList<>();
                newBattlepass.add(null); // No such thing as level "0."
                if(snapshot.exists()) {
                    List<Object> unparsedBattlepass = (List<Object>) snapshot.getValue();
                    for(int i=1; i<unparsedBattlepass.size(); i++) {
                        Object o = unparsedBattlepass.get(i);
                        if(o == null) {
                            newBattlepass.add(null);
                            continue;
                        }

                        Map<String, Object> levelData = (Map<String, Object>) o;
                        try {
                            BattlePassLevel level = new BattlePassLevel(i, levelData);
                            newBattlepass.add(level);
                        } catch(Exception e) {
                            plugin.getLogger().warning("Misconfigured battle pass level " + i + ": " + e.getMessage());
                            newBattlepass.add(null);
                        }
                    }
                }

                battlePass = newBattlepass;
            }

            @Override
            public void onCancelled(DatabaseError error) {
                plugin.getLogger().warning("Battle pass listener error: " + error.getMessage());
            }
        });
    }

    public List<BattlePassLevel> getBattlePass() {
        return Collections.unmodifiableList(battlePass);
    }

    public int getHighestBattlePassLevel() {
        return battlePass.size() - 1;
    }

    @Nullable
    public BattlePassLevel getBattlePassLevel(int level) {
        if(level < 1 || battlePass.size() <= level)
            return null;

        BattlePassLevel bpLevel = battlePass.get(level);
        return bpLevel;
    }

    public void rewardFreeLevel(Holder holder, int level) {
        if(level < 1 || battlePass.size() <= level)
            return;

        BattlePassLevel bpLevel = battlePass.get(level);
        if(bpLevel == null)
            return;

        Reward free = bpLevel.getFree();
        if(free != null)
            free.reward(holder);
    }

    public void rewardPremiumLevel(Holder holder, int level) {
        if(level < 1 || battlePass.size() <= level)
            return;

        BattlePassLevel bpLevel = battlePass.get(level);
        if(bpLevel == null)
            return;

        Reward premium = bpLevel.getPremium();
        if(premium != null)
            premium.reward(holder);
    }

    public void safeShutdown() {
        battlePassReference.removeEventListener(listener);
    }

    public void openBattlePassGUI(Player showTo, CardHolder player, int page, boolean withOpenSounds) {
        if(!player.isLoaded()) {
            showTo.sendMessage(CardHolder.UNLOADED_MESSAGE);
            return;
        }

        GUI gui = withOpenSounds ? new GUI(plugin, "Rewards", 45,
                new SoundBundle(Sound.BLOCK_CHEST_OPEN, 1F, 0.9F),
                null,
                new SoundBundle(Sound.BLOCK_WOODEN_BUTTON_CLICK_ON, 1F, 1F),
                new SoundBundle(Sound.ENTITY_LINGERING_POTION_THROW, 1F, 1.5F))
                : new GUI(plugin, "Rewards", 45,
                null,
                null,
                new SoundBundle(Sound.BLOCK_WOODEN_BUTTON_CLICK_ON, 1F, 1F),
                new SoundBundle(Sound.ENTITY_LINGERING_POTION_THROW, 1F, 1.5F));

        // Back Button
        GUIClickable back = new GUIClickable();
        back.setActionOnClick((g, e) -> {
            player.openBattlePassMainGUI(showTo, false);
            showTo.playSound(showTo.getLocation(), Sound.BLOCK_WOODEN_BUTTON_CLICK_ON, 1F, 1F);
        });
        gui.setSlot(0, Items.BACK_ITEM, back);

        // Explanation Item
        gui.setSlot(4, Items.BATTLEPASS_EXPLANATION_ITEM);

        int levelBase = page*9 + 1;
        for(int i=0; i<9; i++) {
            int level = levelBase + i;
            BattlePassLevel bpLevel = getBattlePassLevel(level);
            if(bpLevel == null)
                continue;

            Reward free = bpLevel.getFree();
            if(free != null) {
                if(free instanceof SpecificReward)
                    gui.setSlot(9+i, ((SpecificReward) free).getIcon(player));
                else
                    gui.setSlot(9+i, free.getIcon());
            }

            ItemStack progress;
            if(player.getLevel() >= level) {
                progress = new ItemStack(Material.PURPLE_STAINED_GLASS_PANE);
                ItemMeta im = progress.getItemMeta();
                im.setDisplayName("§dLevel " + level);
                im.setLore(Arrays.asList("§7Complete!"));
                progress.setItemMeta(im);
            } else if(player.getLevel() + 1 == level) {
                progress = new ItemStack(Material.PINK_STAINED_GLASS_PANE);
                ItemMeta im = progress.getItemMeta();
                im.setDisplayName("§dLevel " + level);
                im.setLore(Arrays.asList("§a" + (player.getXp() % 1000) + "§f / §21000xp", "§7Complete missions for more §2xp§7!"));
                progress.setItemMeta(im);
            } else {
                progress = new ItemStack(Material.WHITE_STAINED_GLASS_PANE);
                ItemMeta im = progress.getItemMeta();
                im.setDisplayName("§fLevel " + level);
                progress.setItemMeta(im);
            }

            gui.setSlot(18+i, progress);

            Reward premium = bpLevel.getPremium();
            if(premium != null) {
                /*if(premium instanceof SpecificReward)
                    gui.setSlot(27+i, ((SpecificReward) premium).getIcon(player));
                else*/
                gui.setSlot(27+i, premium.getIcon());
            }
        }

        // Show previous page
        if(page > 0) {
            GUIClickable previousPage = new GUIClickable();
            previousPage.setActionOnClick((g, e) -> {
                showTo.playSound(showTo.getLocation(), Sound.BLOCK_WOODEN_BUTTON_CLICK_ON, 1F, 1F);
                openBattlePassGUI(showTo, player, page - 1, false);
            });
            gui.setSlot(36, Items.PREVIOUS_PAGE, previousPage);
        }

        // Buy the battle pass!
        if(!player.hasBattlePass()) {
            GUIClickable purchasePass = new GUIClickable();
            purchasePass.setActionOnClick((g, e) -> {
                showTo.closeInventory();
                showTo.playSound(showTo.getLocation(), Sound.ENTITY_WANDERING_TRADER_REAPPEARED, 1F, 1F);
                showTo.sendMessage("§6§lBuy the Battle Pass! §fVisit our store at:");
                showTo.sendMessage("§bhttps://store.godcomplex.org");
            });
            gui.setSlot(40, Items.BATTLEPASS_PURCHASE_PASS, purchasePass);
        }

        // Show next page
        if(levelBase + 9 <= getHighestBattlePassLevel()) {
            GUIClickable nextPage = new GUIClickable();
            nextPage.setActionOnClick((g, e) -> {
                showTo.playSound(showTo.getLocation(), Sound.BLOCK_WOODEN_BUTTON_CLICK_ON, 1F, 1F);
                openBattlePassGUI(showTo, player, page + 1, false);
            });
            gui.setSlot(44, Items.NEXT_PAGE, nextPage);
        }

        // Open the inventory.
        gui.openInv(showTo);
    }
}
