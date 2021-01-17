package com.knoban.ultimates.aspects;

import com.knoban.ultimates.cards.Card;
import com.knoban.ultimates.primal.Tier;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.ShapedRecipe;
import org.bukkit.inventory.meta.ItemMeta;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

public class Items {
	
	public static final ItemStack RESPAWN_ITEM = getRespawnItem();
	public static final ItemStack DEFAULT_REWARD_ITEM = getDefaultRewardItem();
	public static final ItemStack GUI_PLACEHOLDER_LIGHT_GRAY_ITEM = getGUIPlaceholderLightGrayItem();
	public static final ItemStack GUI_PLACEHOLDER_LIME_ITEM = getGUIPlaceholderLimeItem();
	public static final ItemStack GUI_PLACEHOLDER_ORANGE_ITEM = getGUIPlaceholderOrangeItem();
	public static final ItemStack CARDPACK_SPINNER_ITEM = getCardPackSpinnerItem();
	public static final ItemStack CARD_MENU_EXPLANATION_ITEM = getCardMenuExplanationItem();
	public static final ItemStack YOUR_DECK_MENU_ITEM = getYourDeckMenuItem();
	public static final ItemStack SHOP_MENU_ITEM = getShopMenuItem();
	public static final ItemStack SHOP_EXPLANATION_ITEM = getShopExplanationItem();
	public static final ItemStack ALL_CARDS_NO_SORT_ITEM = getAllCardsNoSortItem();
	public static final ItemStack ALL_CARDS_ASC_SORT_ITEM = getAllCardsAscSortItem();
	public static final ItemStack ALL_CARDS_DESC_SORT_ITEM = getAllCardsDescSortItem();
	public static final ItemStack ALL_CARDS_PRIMAL_SORT_ITEM = getAllCardsPrimalSortItem();
	public static final ItemStack OWNED_CARD_EXPLANATION_ITEM = getOwnedCardExplanationItem();
	public static final ItemStack BACK_ITEM = getBackItem();
	public static final ItemStack DECLINE_ITEM = getDeclineItem();
	public static final ItemStack ACCEPT_ITEM = getAcceptItem();
	public static final ItemStack NEXT_PAGE = getNextPageItem();
	public static final ItemStack PREVIOUS_PAGE = getPreviousPageItem();
	public static final ItemStack CARD_POINTER = getCardPointerItem();
	public static final ItemStack CARD_SLOT_OPEN = getCardSlotOpenItem();
	public static final ItemStack CARD_SLOT_LOCKED = getCardSlotLockedItem();
	public static final ItemStack ULTIMATE_POINTER_L = getUltimatePointerLItem();
	public static final ItemStack ULTIMATE_POINTER_R = getUltimatePointerRItem();
	public static final ItemStack PASS_MAIN_EXPLANATION_ITEM = getMainPassExplanationItem();
	public static final ItemStack PASS_MISSIONS_MENU_ITEM = getMissionsMenuItem();
	public static final ItemStack PASS_PASS_MENU_ITEM = getBattlePassMenuItem();
	public static final ItemStack MISSIONS_EXPLANATION_ITEM = getMissionsExplanationItem();
	public static final ItemStack BATTLEPASS_EXPLANATION_ITEM = getBattlePassExplanationItem();
	public static final ItemStack BATTLEPASS_PURCHASE_PASS = getBattlePassPurchasePass();
	public static final ItemStack BATTLEPASS_UNLOCKED_LEVEL = getBattlePassUnlockedLevel();
	public static final ItemStack BATTLEPASS_LOCKED_LEVEL = getBattlePassLockedLevel();

	private static ItemStack getDefaultRewardItem() {
		ItemStack is = new ItemStack(Material.CHEST);
		ItemMeta im = is.getItemMeta();
		im.setDisplayName("§7Reward Placeholder");
		List<String> lores = new ArrayList<>();
		lores.add(ChatColor.RED + "Developer description here.");
		im.setLore(lores);
		is.setItemMeta(im);
		return is;
	}

	private static ItemStack getRespawnItem() {
		ItemStack is = new ItemStack(Material.TOTEM_OF_UNDYING, 1);
		ItemMeta im = is.getItemMeta();
		im.setDisplayName(ChatColor.GOLD + "Respawn Token");
		List<String> lores = new ArrayList<>();
		lores.add(ChatColor.RED + "Requires 30 levels to craft.");
		lores.add(ChatColor.GRAY + "Get your items back after death.");
		im.setLore(lores);
		im.addEnchant(Enchantment.ARROW_INFINITE, 1, true);
		im.addItemFlags(ItemFlag.HIDE_ENCHANTS);
		is.setItemMeta(im);
		return is;
	}

	private static ItemStack getGUIPlaceholderLightGrayItem() {
		ItemStack is = new ItemStack(Material.LIGHT_GRAY_STAINED_GLASS_PANE);
		ItemMeta im = is.getItemMeta();
		im.setDisplayName("§7");
		is.setItemMeta(im);
		return is;
	}

	private static ItemStack getGUIPlaceholderLimeItem() {
		ItemStack is = new ItemStack(Material.LIME_STAINED_GLASS_PANE);
		ItemMeta im = is.getItemMeta();
		im.setDisplayName("§a");
		is.setItemMeta(im);
		return is;
	}

	private static ItemStack getGUIPlaceholderOrangeItem() {
		ItemStack is = new ItemStack(Material.ORANGE_STAINED_GLASS_PANE);
		ItemMeta im = is.getItemMeta();
		im.setDisplayName("§6");
		is.setItemMeta(im);
		return is;
	}

	private static ItemStack getCardPackSpinnerItem() {
		ItemStack is = new ItemStack(Material.HOPPER);
		ItemMeta im = is.getItemMeta();
		im.setDisplayName("§bYour Reward");
		is.setItemMeta(im);
		return is;
	}

	private static ItemStack getCardMenuExplanationItem() {
		ItemStack is = new ItemStack(Material.BIRCH_SIGN);
		ItemMeta im = is.getItemMeta();
		im.setDisplayName("§eWhat's this menu?");
		List<String> lores = new ArrayList<String>();
		lores.add("§7Here you can see and");
		lores.add("§7manage your owned");
		lores.add("§7cards and view cards");
		lores.add("§7you don't own just yet.");
		lores.add("");
		lores.add("§6Hover over a selection");
		lores.add("§6to learn more about it!");
		im.setLore(lores);
		is.setItemMeta(im);
		return is;
	}

	private static ItemStack getYourDeckMenuItem() {
		ItemStack is = new ItemStack(Material.BOOK);
		ItemMeta im = is.getItemMeta();
		im.setDisplayName("§2Your Deck");
		List<String> lores = new ArrayList<String>();
		lores.add("§7Draw and discard cards");
		lores.add("§7that you own! This can");
		lores.add("§7be done once every");
		lores.add("§330 minutes§7.");
		lores.add("");
		lores.add("§7Each card comes with a");
		lores.add("§dunique ability§7.");
		lores.add("");
		lores.add("§6Click to select!");
		im.setLore(lores);
		is.setItemMeta(im);
		return is;
	}

	private static ItemStack getShopMenuItem() {
		ItemStack is = new ItemStack(Material.CHEST);
		ItemMeta im = is.getItemMeta();
		im.setDisplayName("§dThe Shop");
		List<String> lores = new ArrayList<String>();
		lores.add("§7Discover all the cards");
		lores.add("§7that can be collected");
		lores.add("§7on the server! §9Click");
		lores.add("§9on cards to purchase them.");
		lores.add("");
		lores.add("§7This menu also shows");
		lores.add("§7who currently holds the");
		lores.add("§7exclusive " + Tier.EXOTIC.getDisplay() + " §7cards.");
		lores.add("");
		lores.add("§6Click to select!");
		im.setLore(lores);
		is.setItemMeta(im);
		return is;
	}

	private static ItemStack getShopExplanationItem() {
		ItemStack is = new ItemStack(Material.BIRCH_SIGN);
		ItemMeta im = is.getItemMeta();
		im.setDisplayName("§eWhat's this menu?");
		List<String> lores = new ArrayList<String>();
		lores.add("§7Here you can view the");
		lores.add("§6cards §7available to collect.");
		lores.add("§7§oEach card has a §d§ounique ability§7§o.");
		lores.add("");
		lores.add("§9Hover §7over a card to learn");
		lores.add("§7about its unique ability.");
		lores.add("");
		lores.add("§9Click §7on a card to buy");
		lores.add("§7it in exchange for §bwisdom.");
		lores.add("");
		lores.add(Tier.ELUSIVE.getDisplay() + " §7cards can only be");
		lores.add("§7collected in special events!");
		lores.add("");
		lores.add(Tier.EXOTIC.getDisplay() + " §7cards can only be");
		lores.add("§7collected by a single player!");
		lores.add("§7§oHover to see the current owner.");
		im.setLore(lores);
		is.setItemMeta(im);
		return is;
	}

	private static ItemStack getOwnedCardExplanationItem() {
		ItemStack is = new ItemStack(Material.BIRCH_SIGN);
		ItemMeta im = is.getItemMeta();
		im.setDisplayName("§eWhat's this menu?");
		List<String> lores = new ArrayList<String>();
		lores.add("§7Here you can equip the");
		lores.add("§6cards §7you've collected.");
		lores.add("§7§oEach card has a §d§ounique ability§7§o.");
		lores.add("");
		lores.add("§9Hover §7over a card to learn");
		lores.add("§7about its unique ability. §9Click");
		lores.add("§7on a card to equip/remove it.");
		lores.add("");
		lores.add("§cThe bottom item bar §7shows");
		lores.add("§7you which cards you've equipped.");
		im.setLore(lores);
		is.setItemMeta(im);
		return is;
	}

	private static ItemStack getAllCardsNoSortItem() {
		ItemStack is = new ItemStack(Material.GRAY_DYE);
		ItemMeta im = is.getItemMeta();
		im.setDisplayName("§7No Sort");
		is.setItemMeta(im);
		return is;
	}

	private static ItemStack getAllCardsAscSortItem() {
		ItemStack is = new ItemStack(Material.DIAMOND_HORSE_ARMOR);
		ItemMeta im = is.getItemMeta();
		im.setDisplayName("§aSort Ascending By Tier");
		is.setItemMeta(im);
		return is;
	}

	private static ItemStack getAllCardsDescSortItem() {
		ItemStack is = new ItemStack(Material.IRON_HORSE_ARMOR);
		ItemMeta im = is.getItemMeta();
		im.setDisplayName("§4Sort Descending By Tier");
		is.setItemMeta(im);
		return is;
	}

	private static ItemStack getAllCardsPrimalSortItem() {
		ItemStack is = new ItemStack(Material.END_CRYSTAL);
		ItemMeta im = is.getItemMeta();
		im.setDisplayName("§aSort By Primal");
		is.setItemMeta(im);
		return is;
	}

	private static ItemStack getBackItem() {
		ItemStack is = new ItemStack(Material.ARROW);
		ItemMeta im = is.getItemMeta();
		im.setDisplayName("§7Go Back");
		is.setItemMeta(im);
		return is;
	}

	private static ItemStack getDeclineItem() {
		ItemStack is = new ItemStack(Material.RED_DYE);
		ItemMeta im = is.getItemMeta();
		im.setDisplayName("§4Decline Purchase");
		is.setItemMeta(im);
		return is;
	}

	private static ItemStack getAcceptItem() {
		ItemStack is = new ItemStack(Material.LIME_DYE);
		ItemMeta im = is.getItemMeta();
		im.setDisplayName("§2Accept Purchase");
		is.setItemMeta(im);
		return is;
	}

	private static ItemStack getNextPageItem() {
		ItemStack is = new ItemStack(Material.GREEN_CARPET);
		ItemMeta im = is.getItemMeta();
		im.setDisplayName("§bNext Page");
		is.setItemMeta(im);
		return is;
	}

	private static ItemStack getPreviousPageItem() {
		ItemStack is = new ItemStack(Material.RED_CARPET);
		ItemMeta im = is.getItemMeta();
		im.setDisplayName("§bPrevious Page");
		is.setItemMeta(im);
		return is;
	}

	private static ItemStack getCardPointerItem() {
		ItemStack is = new ItemStack(Material.MAGENTA_STAINED_GLASS_PANE);
		ItemMeta im = is.getItemMeta();
		im.setDisplayName("§5Your Equipped Cards -->");
		is.setItemMeta(im);
		return is;
	}

	private static ItemStack getUltimatePointerLItem() {
		ItemStack is = new ItemStack(Material.LIME_STAINED_GLASS_PANE);
		ItemMeta im = is.getItemMeta();
		im.setDisplayName("§aYour Ultimate Ability -->");
		is.setItemMeta(im);
		return is;
	}

	private static ItemStack getUltimatePointerRItem() {
		ItemStack is = new ItemStack(Material.LIME_STAINED_GLASS_PANE);
		ItemMeta im = is.getItemMeta();
		im.setDisplayName("§a<-- Your Ultimate Ability");
		is.setItemMeta(im);
		return is;
	}

	private static ItemStack getCardSlotOpenItem() {
		ItemStack is = new ItemStack(Material.GRAY_STAINED_GLASS_PANE);
		ItemMeta im = is.getItemMeta();
		im.setDisplayName("§8Open Slot");
		List<String> lores = new ArrayList<String>();
		lores.add("§7Click on a card to");
		lores.add("§7equip it here!");
		im.setLore(lores);
		is.setItemMeta(im);
		return is;
	}

	private static ItemStack getCardSlotLockedItem() {
		ItemStack is = new ItemStack(Material.BARRIER);
		ItemMeta im = is.getItemMeta();
		im.setDisplayName("§4Locked Slot");
		List<String> lores = new ArrayList<String>();
		lores.add("§7Unlock this card slot by");
		lores.add("§7leveling up through gameplay!");
		im.setLore(lores);
		is.setItemMeta(im);
		return is;
	}

	private static ItemStack getMainPassExplanationItem() {
		ItemStack is = new ItemStack(Material.BIRCH_SIGN);
		ItemMeta im = is.getItemMeta();
		im.setDisplayName("§eWhat's this menu?");
		List<String> lores = new ArrayList<String>();
		lores.add("§7Here you can view the");
		lores.add("§7the §6Battle Pass§7 and your");
		lores.add("§2current missions§7.");
		im.setLore(lores);
		is.setItemMeta(im);
		return is;
	}

	private static ItemStack getMissionsMenuItem() {
		ItemStack is = new ItemStack(Material.END_CRYSTAL);
		ItemMeta im = is.getItemMeta();
		im.setDisplayName("§dMissions");
		List<String> lores = new ArrayList<>();
		lores.add("§7View your §2current missions");
		lores.add("§7and your §bprogress§7.");
		im.setLore(lores);
		is.setItemMeta(im);
		return is;
	}

	private static ItemStack getBattlePassMenuItem() {
		ItemStack is = new ItemStack(Material.GOLD_INGOT);
		ItemMeta im = is.getItemMeta();
		im.setDisplayName("§3Battle Pass");
		List<String> lores = new ArrayList<>();
		lores.add("§7View the §6Battle Pass §7and");
		lores.add("§7your §bupcoming rewards§7.");
		im.setLore(lores);
		is.setItemMeta(im);
		return is;
	}

	private static ItemStack getMissionsExplanationItem() {
		ItemStack is = new ItemStack(Material.BIRCH_SIGN);
		ItemMeta im = is.getItemMeta();
		im.setDisplayName("§eWhat's this menu?");
		List<String> lores = new ArrayList<String>();
		lores.add("§7Here you can view all your");
		lores.add("§2current missions §7and their");
		lores.add("§brewards§7.");
		im.setLore(lores);
		is.setItemMeta(im);
		return is;
	}

	private static ItemStack getBattlePassExplanationItem() {
		ItemStack is = new ItemStack(Material.BIRCH_SIGN);
		ItemMeta im = is.getItemMeta();
		im.setDisplayName("§eWhat's this menu?");
		List<String> lores = new ArrayList<>();
		lores.add("§7Here you can view your");
		lores.add("§6Battle Pass' §bupcoming rewards§7.");
		im.setLore(lores);
		is.setItemMeta(im);
		return is;
	}

	private static ItemStack getBattlePassPurchasePass() {
		ItemStack is = new ItemStack(Material.GOLD_INGOT);
		ItemMeta im = is.getItemMeta();
		im.setDisplayName("§eWant more rewards?");
		List<String> lores = new ArrayList<String>();
		lores.add("§2Click here to purchase this");
		lores.add("§2season's §bpremium §6Battle Pass");
		lores.add("§2for §b§oextra rewards§2!");
		im.setLore(lores);
		is.setItemMeta(im);
		return is;
	}

	private static ItemStack getBattlePassUnlockedLevel() {
		return new ItemStack(Material.PURPLE_STAINED_GLASS_PANE);
	}

	private static ItemStack getBattlePassLockedLevel() {
		return new ItemStack(Material.WHITE_STAINED_GLASS_PANE);
	}

	public static boolean isLocked(@Nullable ItemStack is) {
		if(is != null) {
			ItemMeta im = is.getItemMeta();
			if(im != null) {
				List<String> lore = im.getLore();
				return lore != null && lore.contains(Card.LOCKED_METADATA_LORE);
			}
		}
		return false;
	}
	
	public static ShapedRecipe getRespawnRecipe() {
		ShapedRecipe sr = new ShapedRecipe(NamespacedKey.minecraft("ultimates"), Items.getRespawnItem());
		sr.shape("AAA", "BCB", "AAA");
		sr.setIngredient('A', Material.GOLD_BLOCK);
		sr.setIngredient('B', Material.EMERALD);
		sr.setIngredient('C', Material.END_CRYSTAL);
		return sr;
	}
}
