package com.winthier.hopperfilter;

import java.util.ArrayList;
import java.util.List;
import org.bukkit.Material;
import org.bukkit.entity.ItemFrame;
import org.bukkit.inventory.ItemStack;

public final class FilterItemList {
    private List<FilterItem> items;

    public FilterItemList(final List<ItemFrame> itemFrames) {
        items = new ArrayList<FilterItem>();
        for (ItemFrame itemFrame : itemFrames) {
            ItemStack item = itemFrame.getItem();
            if (item != null && item.getType() != Material.AIR) {
                items.add(new FilterItem(item));
            }
        }
    }

    public boolean allowsItem(ItemStack item) {
        if (items.isEmpty()) return true;
        for (FilterItem filter : items) {
            if (filter.allowsItem(item)) return true;
        }
        return false;
    }

    public boolean isEmpty() {
        return items == null || items.isEmpty();
    }

    public List<FilterItem> getFilterItems() {
        return items;
    }
}
