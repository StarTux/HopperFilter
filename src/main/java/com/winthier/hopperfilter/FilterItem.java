package com.winthier.hopperfilter;

import lombok.Getter;
import org.bukkit.inventory.ItemStack;

@Getter
public final class FilterItem {
    private final ItemStack itemStack;

    public FilterItem(final ItemStack item) {
        this.itemStack = item.clone();
    }

    public boolean allowsItem(ItemStack item) {
        return itemStack.isSimilar(item);
    }
}
