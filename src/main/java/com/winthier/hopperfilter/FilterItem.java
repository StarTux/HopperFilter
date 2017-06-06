package com.winthier.hopperfilter;

import org.bukkit.inventory.ItemStack;

public final class FilterItem {
    private final ItemStack itemStack;

    public FilterItem(ItemStack item) {
        this.itemStack = item.clone();
    }

    public boolean allowsItem(ItemStack item) {
        return itemStack.isSimilar(item);
    }
}
