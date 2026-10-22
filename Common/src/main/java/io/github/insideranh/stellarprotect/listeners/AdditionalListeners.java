package io.github.insideranh.stellarprotect.listeners;

import io.github.insideranh.stellarprotect.StellarProtect;
import io.github.insideranh.stellarprotect.cache.BlockSourceCache;
import io.github.insideranh.stellarprotect.cache.LoggerCache;
import io.github.insideranh.stellarprotect.database.entries.players.PlayerBlockLogEntry;
import io.github.insideranh.stellarprotect.database.entries.players.PlayerTransactionEntry;
import io.github.insideranh.stellarprotect.items.ItemReference;
import io.github.insideranh.stellarprotect.utils.PlayerUtils;
import org.bukkit.Location;
import org.bukkit.event.inventory.InventoryMoveItemEvent;
import org.bukkit.inventory.Inventory;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import io.github.insideranh.stellarprotect.database.entries.world.BrewingLogEntry;
import io.github.insideranh.stellarprotect.enums.ActionType;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockDispenseEvent;
import org.bukkit.event.inventory.BrewEvent;
import org.bukkit.inventory.BrewerInventory;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class AdditionalListeners implements Listener {

    private final StellarProtect plugin = StellarProtect.getInstance();
    private final Map<String, Boolean> pendingTransfers = new ConcurrentHashMap<>();

    private static final List<String> SCULK_BLOCKS = Arrays.asList(
        "SCULK", "SCULK_VEIN", "SCULK_CATALYST", "SCULK_SENSOR", "SCULK_SHRIEKER"
    );

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onInventoryMove(InventoryMoveItemEvent event) {
        if (!plugin.getConfig().getBoolean("logs.automatic_transfer.enabled", false)) return;
        if (!ActionType.INVENTORY_TRANSACTION.isEnabled()) return;
        Inventory source = event.getSource();
        Inventory destination = event.getDestination();
        Location from = source.getLocation();
        Location to = destination.getLocation();
        if (from == null || to == null || from.getWorld() == null || !from.getWorld().equals(to.getWorld()) || from.equals(to)) return;
        if ((from.getBlockX() >> 4) != (to.getBlockX() >> 4) || (from.getBlockZ() >> 4) != (to.getBlockZ() >> 4)) return;
        ItemStack transferred = event.getItem();
        if (transferred == null || transferred.getType() == Material.AIR) return;
        ItemStack item = transferred.clone();
        int beforeDestination = countSimilar(destination, item);
        String key = from.getWorld().getUID() + ":" + from.getBlockX() + ":" + from.getBlockY() + ":" + from.getBlockZ()
            + ":" + to.getBlockX() + ":" + to.getBlockY() + ":" + to.getBlockZ() + ":" + item.hashCode();
        if (pendingTransfers.putIfAbsent(key, Boolean.TRUE) != null) return;
        String actor = getTransferActor(event.getInitiator());
        plugin.getStellarTaskHook(() -> {
            try {
                int quantity = countSimilar(destination, item) - beforeDestination;
                if (quantity <= 0 || ActionType.INVENTORY_TRANSACTION.shouldSkipLog(from.getWorld().getName(), item.getType().name())) return;
                ItemReference reference = plugin.getItemsManager().getItemReference(item, quantity);
                long playerId = PlayerUtils.getPlayerOrEntityId(actor);
                Map<Long, Integer> removedItems = new HashMap<>();
                removedItems.put(reference.getTemplateId(), quantity);
                LoggerCache.addLog(new PlayerTransactionEntry(playerId, new HashMap<>(), removedItems, from, ActionType.INVENTORY_TRANSACTION));
                Map<Long, Integer> addedItems = new HashMap<>();
                addedItems.put(reference.getTemplateId(), quantity);
                LoggerCache.addLog(new PlayerTransactionEntry(playerId, addedItems, new HashMap<>(), to, ActionType.INVENTORY_TRANSACTION));
            } finally {
                pendingTransfers.remove(key);
            }
        }).runTask(from, 1L);
    }

    private static int countSimilar(Inventory inventory, ItemStack item) {
        int count = 0;
        for (ItemStack stored : inventory.getStorageContents()) {
            if (stored != null && stored.isSimilar(item)) count += stored.getAmount();
        }
        return count;
    }

    private static String getTransferActor(Inventory initiator) {
        Location location = initiator.getLocation();
        if (location == null || location.getWorld() == null) return "=hopper";
        Material material = location.getBlock().getType();
        if (material == Material.DROPPER) return "=dropper";
        if ("CRAFTER".equals(material.name())) return "=crafter";
        return "=hopper";
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onBlockDispense(BlockDispenseEvent event) {
        Block block = event.getBlock();
        ItemStack item = event.getItem();
        if (block == null || item == null) return;
        plugin.getEventLogicHandler().onDispense(block, item);
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onBrew(BrewEvent event) {
        if (!plugin.getConfigManager().isLiquidTracking()) return;
        BrewerInventory inventory = event.getContents();
        if (inventory == null) return;
        ItemStack ingredient = inventory.getIngredient();
        ItemStack fuel = getFuel(inventory);
        for (ItemStack result : getResults(event, inventory)) {
            if (result == null || result.getType() == Material.AIR) continue;
            LoggerCache.addLog(new BrewingLogEntry(event.getBlock().getLocation(), ingredient, fuel, result));
        }
    }

    private ItemStack getFuel(BrewerInventory inventory) {
        try {
            Object value = inventory.getClass().getMethod("getFuel").invoke(inventory);
            if (value instanceof ItemStack) return (ItemStack) value;
        } catch (Throwable ignored) {
        }
        return null;
    }

    private List<ItemStack> getResults(BrewEvent event, BrewerInventory inventory) {
        try {
            Object value = event.getClass().getMethod("getResults").invoke(event);
            if (value instanceof List) {
                List<ItemStack> results = new ArrayList<>();
                for (Object result : (List<?>) value) {
                    if (result instanceof ItemStack) results.add((ItemStack) result);
                }
                return results;
            }
        } catch (Throwable ignored) {
        }
        ItemStack[] contents = inventory.getContents();
        List<ItemStack> results = new ArrayList<>(3);
        for (int i = 0; i < Math.min(3, contents.length); i++) results.add(contents[i]);
        return results;
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onEntityExplode(org.bukkit.event.entity.EntityExplodeEvent event) {
        event.blockList().forEach(block -> {
            if (SCULK_BLOCKS.contains(block.getType().name())) {
                Long playerId = BlockSourceCache.getPlayerId(block.getLocation());
                if (playerId != null && !ActionType.BLOCK_BREAK.shouldSkipLog(block.getWorld().getName(), block.getType().name())) {
                    LoggerCache.addLog(new PlayerBlockLogEntry(playerId, block, ActionType.BLOCK_BREAK));
                }
            }
        });
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onBlockExplode(org.bukkit.event.block.BlockExplodeEvent event) {
        event.blockList().forEach(block -> {
            if (SCULK_BLOCKS.contains(block.getType().name())) {
                Long playerId = BlockSourceCache.getPlayerId(block.getLocation());
                if (playerId != null && !ActionType.BLOCK_BREAK.shouldSkipLog(block.getWorld().getName(), block.getType().name())) {
                    LoggerCache.addLog(new PlayerBlockLogEntry(playerId, block, ActionType.BLOCK_BREAK));
                }
            }
        });
    }

}
