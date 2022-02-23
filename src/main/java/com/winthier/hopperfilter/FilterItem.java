package com.winthier.hopperfilter;

import com.cavetale.mytems.Mytems;
import lombok.Getter;
import org.bukkit.inventory.ItemStack;

@Getter
public final class FilterItem {
    private final ItemStack itemStack;
    private final Mytems mytems;
    private final boolean glow;

    public FilterItem(final ItemStack item, final boolean glow) {
        this.itemStack = item.clone();
        this.mytems = Mytems.forItem(item);
        this.glow = glow;
    }

    public boolean allowsItem(ItemStack item) {
        return (mytems != null)
            ? mytems.isItem(item)
            : (glow
               ? item.getType() == itemStack.getType()
               : itemStack.isSimilar(item));
    }
}
