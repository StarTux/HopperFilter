package com.winthier.hopperfilter;

import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;

public class FilterItem {
    private final Material material;
    private final short data;

    public FilterItem(Material material, short data) {
        this.material = material;
        this.data = data;
    }

    public FilterItem(ItemStack item) {
        this(item.getType(), item.getDurability());
    }

    public boolean allowsItem(ItemStack item) {
        if (material != item.getType()) return false;
        if (material.getMaxDurability() <= (short)0) return data == item.getDurability();
        return true;
    }

    public String toString() {
        if (material.getMaxDurability() <= (short)0) {
            return material.name().toLowerCase() + ":" + (int)data;
        }
        return material.name().toLowerCase();
    }
}
