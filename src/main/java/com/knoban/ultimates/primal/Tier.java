package com.knoban.ultimates.primal;

import net.kyori.adventure.text.Component;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public enum Tier {

    // Please make sure tiers are listed in ascending order! This impacts the runtime result of the Cards class.

    // Occurrence values do not need to add to 1. They will be normalized on runtime between all tiers.

    COMMON("§7COMMON", "GRAY", 100, 0.75),
    RARE("§bRARE", "LIGHT_BLUE", 300, 0.20),
    EPIC("§5EPIC", "PURPLE", 1500, 0.045),
    LEGENDARY("§6§lLEGENDARY", "ORANGE", 10000, 0.005),
    EXOTIC("§e§oEXOTIC", "YELLOW", null, 0), // Typically, only one player can own a specific type of Exotic card.

    ELUSIVE("§cELUSIVE", "RED", null, 0); // Elusive cards are uncollectable. They can only be obtained through administrators and special events

    private final String display;
    private final String color;
    private final Integer defaultCost;
    private final double occurrence;
    private Double chance;

    private ItemStack placeholder;

    Tier(@NotNull String display, @NotNull String color, @Nullable Integer defaultCost, double occurrence) {
        this.display = display;
        this.color = color;
        this.defaultCost = defaultCost;
        this.occurrence = occurrence;
    }

    @NotNull
    public String getDisplay() {
        return display;
    }

    @NotNull
    public String getColor() {
        return color;
    }

    @Nullable
    public Integer getDefaultCost() {
        return defaultCost;
    }

    // If you put this into the constructor, the CardTest class will fail since ItemMetas are note available
    // during testing. Only options are to put this stuff here or remove the CardsTest test class.
    @NotNull
    public ItemStack getPlaceholder() {
        if(placeholder != null)
            return placeholder;

        this.placeholder = new ItemStack(Material.getMaterial(color + "_STAINED_GLASS_PANE"));
        ItemMeta im = placeholder.getItemMeta();
        im.displayName(Component.text(display));
        placeholder.setItemMeta(im);

        return placeholder;
    }

    public double getChance() {
        if(chance != null)
            return chance;

        return chance = occurrence / SUM;
    }

    private static final double SUM = getSum();
    private static double getSum() {
        double sum = 0;
        for(Tier tier : Tier.values())
            sum += tier.occurrence;
        return sum;
    }
}
