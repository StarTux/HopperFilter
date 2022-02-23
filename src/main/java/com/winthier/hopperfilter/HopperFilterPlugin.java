package com.winthier.hopperfilter;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.BlockState;
import org.bukkit.block.Hopper;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Entity;
import org.bukkit.entity.ItemFrame;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockDamageEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.hanging.HangingBreakByEntityEvent;
import org.bukkit.event.hanging.HangingBreakEvent;
import org.bukkit.event.hanging.HangingPlaceEvent;
import org.bukkit.event.inventory.InventoryMoveItemEvent;
import org.bukkit.event.inventory.InventoryPickupItemEvent;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.metadata.MetadataValue;
import org.bukkit.metadata.Metadatable;
import org.bukkit.plugin.java.JavaPlugin;
import static net.kyori.adventure.text.Component.text;
import static net.kyori.adventure.text.format.NamedTextColor.*;

public final class HopperFilterPlugin extends JavaPlugin implements Listener {
    private final Set<UUID> debugPlayers = new HashSet<>();
    private final Set<UUID> inspectPlayers = new HashSet<>();

    @Override
    public void onEnable() {
        getServer().getPluginManager().registerEvents(this, this);
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage("[hopperfilter:hopperfilter] player expected");
            return true;
        }
        Player player = (Player) sender;
        if (args.length == 0) return false;
        switch (args[0]) {
        case "debug": {
            boolean debug = debugPlayers.contains(player.getUniqueId());
            if (debug) {
                debugPlayers.remove(player.getUniqueId());
                player.sendMessage(text("Debug mode turned off", YELLOW));
            } else {
                debugPlayers.add(player.getUniqueId());
                player.sendMessage(text("Debug mode turned on", AQUA));
            }
            return true;
        }
        case "inspect": {
            boolean inspect = inspectPlayers.contains(player.getUniqueId());
            if (inspect) {
                inspectPlayers.remove(player.getUniqueId());
                player.sendMessage(text("Inspection mode turned off", YELLOW));
            } else {
                inspectPlayers.add(player.getUniqueId());
                player.sendMessage(text("Inspection mode turned on. Punch a hopper to display its filters", AQUA));
            }
            return true;
        }
        default:
            return false;
        }
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String label, String[] args) {
        return args.length == 1
            ? Stream.of("debug", "inspect").filter(s -> s.contains(args[0])).collect(Collectors.toList())
            : List.of();
    }

    private List<ItemFrame> getSurroundingItemFrames(Block block) {
        //Look right next to the hopper and avoid the frames on another hopper
        double radius = 0.75;
        List<ItemFrame> result = new ArrayList<ItemFrame>();
        for (Entity entity : block.getWorld().getNearbyEntities(block.getLocation().add(0.5, 0.5, 0.5), radius, radius, radius)) {
            if (entity instanceof ItemFrame) {
                ItemFrame itemFrame = (ItemFrame) entity;
                if (itemFrame.getLocation().getBlock().getRelative(itemFrame.getAttachedFace()).equals(block)) result.add(itemFrame);
            }
        }
        return result;
    }

    private void storeMetadata(Metadatable storage, FilterItemList value) {
        storage.setMetadata("filter", new FixedMetadataValue(this, value));
    }

    private void removeMetadata(Metadatable storage) {
        storage.removeMetadata("filter", this);
    }

    private FilterItemList getMetadata(Metadatable storage) {
        for (MetadataValue val : storage.getMetadata("filter")) {
            if (val.getOwningPlugin() == this && val.value() instanceof FilterItemList) return (FilterItemList) val.value();
        }
        return null;
    }

    private FilterItemList getFilterItemList(Block block) {
        if (block.getType() != Material.HOPPER) return null;
        FilterItemList result = getMetadata(block);
        if (result == null) {
            List<ItemFrame> itemFrames = getSurroundingItemFrames(block);
            result = new FilterItemList(itemFrames);
            storeMetadata(block, result);
        }
        return result;
    }

    private void clearCacheForItemFrame(ItemFrame itemFrame) {
        removeMetadata(itemFrame.getLocation().getBlock().getRelative(itemFrame.getAttachedFace()));
    }

    private void inspect(Player player, Block block) {
        FilterItemList filter = getFilterItemList(block);
        if (filter == null || filter.isEmpty()) {
            player.sendMessage(text("[HopperFilter] This hopper is not a filter!", RED));
            return;
        }
        StringBuilder sb = new StringBuilder();
        for (FilterItem item : filter.getFilterItems()) {
            sb.append(" ").append(item.getItemStack().toString());
        }
        player.sendMessage(text("[HopperFilter] Filters:" + sb.toString(), YELLOW));
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.MONITOR)
    public void onPlayerInteractEntity(PlayerInteractEntityEvent event) {
        final Entity entity = event.getRightClicked();
        if (!(entity instanceof ItemFrame)) return;
        clearCacheForItemFrame((ItemFrame) entity);
        if (debugPlayers.contains(event.getPlayer().getUniqueId())) {
            event.getPlayer().sendMessage(text("[HopperFilter] Interact ItemFrame", YELLOW));
        }
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.MONITOR)
    public void onEntityDamage(EntityDamageEvent event) {
        final Entity entity = event.getEntity();
        if (!(entity instanceof ItemFrame)) return;
        clearCacheForItemFrame((ItemFrame) entity);
        if (event instanceof EntityDamageByEntityEvent) {
            Entity e = ((EntityDamageByEntityEvent) event).getDamager();
            if (e instanceof Player player) {
                if (debugPlayers.contains(player.getUniqueId())) {
                    player.sendMessage(text("[HopperFilter] Damage ItemFrame", YELLOW));
                }
            }
        }
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.MONITOR)
    public void onHangingBreak(HangingBreakEvent event) {
        final Entity entity = event.getEntity();
        if (!(entity instanceof ItemFrame)) return;
        clearCacheForItemFrame((ItemFrame) entity);
        if (event instanceof HangingBreakByEntityEvent) {
            Entity e = ((HangingBreakByEntityEvent) event).getRemover();
            if (e instanceof Player player) {
                if (debugPlayers.contains(player.getUniqueId())) {
                    player.sendMessage(text("[HopperFilter] Break ItemFrame", YELLOW));
                }
            }
        }
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.MONITOR)
    public void onHangingPlace(HangingPlaceEvent event) {
        final Entity entity = event.getEntity();
        if (!(entity instanceof ItemFrame)) return;
        clearCacheForItemFrame((ItemFrame) entity);
        if (debugPlayers.contains(event.getPlayer().getUniqueId())) {
            event.getPlayer().sendMessage(text("[HopperFilter] Place ItemFrame", YELLOW));
        }
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.MONITOR)
    public void onBlockBreak(BlockBreakEvent event) {
        final Block block = event.getBlock();
        if (block.getType() != Material.HOPPER) return;
        if (inspectPlayers.contains(event.getPlayer().getUniqueId())) {
            event.setCancelled(true);
            inspect(event.getPlayer(), event.getBlock());
            return;
        }
        removeMetadata(block);
        if (debugPlayers.contains(event.getPlayer().getUniqueId())) {
            event.getPlayer().sendMessage(text("[HopperFilter] Break Hopper", YELLOW));
        }
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.MONITOR)
    public void onBlockPlace(BlockPlaceEvent event) {
        final Block block = event.getBlock();
        if (block.getType() != Material.HOPPER) return;
        removeMetadata(event.getBlock());
        if (debugPlayers.contains(event.getPlayer().getUniqueId())) {
            event.getPlayer().sendMessage(text("[HopperFilter] Place Hopper", YELLOW));
        }
    }

    private boolean canAdd(Inventory inventory, ItemStack add) {
        for (ItemStack item : inventory.getContents()) {
            if (item == null) return true;
            final Material mat = item.getType();
            if (mat == Material.AIR) return true;
            if (item.isSimilar(add) && item.getAmount() < item.getMaxStackSize()) return true;
        }
        return false;
    }

    private boolean canAdd(Block block, ItemStack add) {
        BlockState state = block.getState();
        if (!(state instanceof InventoryHolder)) return false;
        return canAdd(((InventoryHolder) state).getInventory(), add);
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.LOW)
    public void onInventoryMoveItem(InventoryMoveItemEvent event) {
        final InventoryHolder holder = event.getDestination().getHolder();
        if (!(holder instanceof Hopper)) return;
        final Block block = ((Hopper) holder).getBlock();
        // Special case for horizontal hopper movement which might be too fast.
        final InventoryHolder sourceHolder = event.getSource().getHolder();
        if (sourceHolder instanceof Hopper) {
            Block sourceBlock = ((Hopper) sourceHolder).getBlock();
            if (sourceBlock.getY() == block.getY()) {
                final Block lowerBlock = sourceBlock.getRelative(BlockFace.DOWN);
                if (!lowerBlock.isBlockPowered()) {
                    final FilterItemList lowerFilter = getFilterItemList(lowerBlock);
                    if (lowerFilter != null && !lowerFilter.isEmpty() && lowerFilter.allowsItem(event.getItem())) {
                        if (canAdd(lowerBlock, event.getItem())) {
                            event.setCancelled(true);
                            return;
                        }
                    }
                }
            }
        }
        //
        FilterItemList filter = getFilterItemList(block);
        if (filter == null) return;
        if (!filter.allowsItem(event.getItem())) {
            event.setCancelled(true);
        }
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.LOW)
    public void onInventoryPickupItem(InventoryPickupItemEvent event) {
        final InventoryHolder holder = event.getInventory().getHolder();
        if (!(holder instanceof Hopper)) return;
        final Block block = ((Hopper) holder).getBlock();
        FilterItemList filter = getFilterItemList(block);
        if (filter == null) return;
        if (!filter.allowsItem(event.getItem().getItemStack())) {
            event.setCancelled(true);
        }
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.HIGH)
    public void onBlockDamage(BlockDamageEvent event) {
        if (event.getBlock().getType() != Material.HOPPER) return;
        if (!inspectPlayers.contains(event.getPlayer().getUniqueId())) return;
        event.setCancelled(true);
        inspect(event.getPlayer(), event.getBlock());
    }
}
