package io.github.insideranh.stellarprotect.listeners;

import io.github.insideranh.stellarprotect.StellarProtect;
import io.github.insideranh.stellarprotect.cache.LoggerCache;
import io.github.insideranh.stellarprotect.data.PlayerProtect;
import io.github.insideranh.stellarprotect.database.entries.players.PlayerItemLogEntry;
import io.github.insideranh.stellarprotect.enums.ActionType;
import io.github.insideranh.stellarprotect.items.ItemReference;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerPickupItemEvent;
import org.bukkit.inventory.ItemStack;

public class PickUpDropListener implements Listener {

    private final StellarProtect plugin = StellarProtect.getInstance();

    private static void saveCurrentGroupIfExists(PlayerProtect playerProtect, Location location, long currentTime) {
        if (playerProtect.getLastPickUpAmount() > 0) {
            LoggerCache.addLog(new PlayerItemLogEntry(playerProtect.getPlayerId(), new ItemReference(playerProtect.getLastPickItemId(), playerProtect.getLastPickUpAmount()), location, ActionType.PICKUP_ITEM, currentTime));
        }
    }

    public static void forceFlushCurrentGroup(PlayerProtect playerProtect, boolean leave) {
        if (playerProtect.getLastLocation() == null) return;
        if (playerProtect.getLastPickUpAmount() > 0 && (playerProtect.getNextSeparateLogPickUp() < System.currentTimeMillis() || leave)) {
            saveCurrentGroupIfExists(playerProtect, playerProtect.getLastLocation(), System.currentTimeMillis());

            playerProtect.setLastLocation(null);
            playerProtect.setLastPickUpAmount(0);
            playerProtect.setNextSeparateLogPickUp(0);
            playerProtect.setLastPickItemId(0);
            playerProtect.setPickUpXYZ(0);
        }
    }

    private static boolean shouldStartNewGroup(PlayerProtect playerProtect, long currentTime, long locationHash, long itemId) {
        return playerProtect.getNextSeparateLogPickUp() < currentTime || playerProtect.getPickUpXYZ() != locationHash || playerProtect.getLastPickItemId() != itemId;
    }

    private static void startNewItemGroup(PlayerProtect playerProtect, long itemId, long locationHash, int itemAmount, long currentTime) {
        playerProtect.setNextSeparateLogPickUp(currentTime + 1000L);
        playerProtect.setLastPickItemId(itemId);
        playerProtect.setPickUpXYZ(locationHash);
        playerProtect.setLastPickUpAmount(itemAmount);
    }

    private static void continueCurrentGroup(PlayerProtect playerProtect, int itemAmount) {
        int newTotal = playerProtect.getLastPickUpAmount() + itemAmount;
        playerProtect.setLastPickUpAmount(newTotal);

        long currentTime = System.currentTimeMillis();
        playerProtect.setNextSeparateLogPickUp(currentTime + 1000L);
    }

    private void schedulePickupFlush(PlayerProtect playerProtect, Location location, long itemId, long locationHash) {
        plugin.getStellarTaskHook(() -> {
            if (playerProtect.getLastLocation() == null
                || playerProtect.getLastPickItemId() != itemId
                || playerProtect.getPickUpXYZ() != locationHash) return;
            if (playerProtect.getNextSeparateLogPickUp() < System.currentTimeMillis()) {
                forceFlushCurrentGroup(playerProtect, false);
            } else {
                schedulePickupFlush(playerProtect, location, itemId, locationHash);
            }
        }).runTask(location, 25L);
    }

    private static long calculateLocationHash(Location location) {
        long x = location.getBlockX();
        long y = location.getBlockY();
        long z = location.getBlockZ();

        return x * 73856093L ^ y * 19349663L ^ z * 83492791L ^ location.getWorld().getUID().hashCode();
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onDrop(PlayerDropItemEvent event) {
        PlayerProtect playerProtect = PlayerProtect.getPlayer(event.getPlayer());
        if (playerProtect == null) return;

        ItemStack itemStack = event.getItemDrop().getItemStack();
        if (itemStack.getType().name().equals("AIR")) return;
        if (ActionType.DROP_ITEM.shouldSkipLog(event.getPlayer().getWorld().getName(), itemStack.getType().name())) return;

        ItemReference itemReference = plugin.getItemsManager().getItemReference(itemStack);
        forceFlushCurrentGroup(playerProtect, false);

        LoggerCache.addLog(new PlayerItemLogEntry(playerProtect.getPlayerId(), itemReference, event.getPlayer().getLocation(), ActionType.DROP_ITEM));
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onPickup(PlayerPickupItemEvent event) {
        Player player = event.getPlayer();
        PlayerProtect playerProtect = PlayerProtect.getPlayer(player);
        if (playerProtect == null) return;

        ItemStack itemStack = event.getItem().getItemStack();
        if (itemStack.getType() == Material.AIR) return;
        if (ActionType.PICKUP_ITEM.shouldSkipLog(player.getWorld().getName(), itemStack.getType().name())) return;
        int pickedUp = Math.max(0, itemStack.getAmount() - event.getRemaining());
        if (pickedUp == 0) return;

        Location location = player.getLocation();
        ItemReference itemReference = plugin.getItemsManager().getItemReference(itemStack);
        long currentTime = System.currentTimeMillis();

        long locationHash = calculateLocationHash(location);
        long itemId = itemReference.getTemplateId();

        if (shouldStartNewGroup(playerProtect, currentTime, locationHash, itemId)) {
            if (playerProtect.getLastLocation() != null) {
                saveCurrentGroupIfExists(playerProtect, playerProtect.getLastLocation(), currentTime);
            }
            playerProtect.setLastLocation(location);
            startNewItemGroup(playerProtect, itemId, locationHash, pickedUp, currentTime);
            schedulePickupFlush(playerProtect, location, itemId, locationHash);
        } else {
            continueCurrentGroup(playerProtect, pickedUp);
        }
    }

}