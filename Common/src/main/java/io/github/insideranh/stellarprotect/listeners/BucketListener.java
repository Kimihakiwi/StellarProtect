package io.github.insideranh.stellarprotect.listeners;

import io.github.insideranh.stellarprotect.StellarProtect;
import io.github.insideranh.stellarprotect.cache.BlockSourceCache;
import io.github.insideranh.stellarprotect.cache.LoggerCache;
import io.github.insideranh.stellarprotect.data.PlayerProtect;
import io.github.insideranh.stellarprotect.database.entries.players.PlayerBlockLogEntry;
import io.github.insideranh.stellarprotect.enums.ActionType;
import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.block.BlockState;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerBucketEmptyEvent;
import org.bukkit.event.player.PlayerBucketEvent;
import org.bukkit.event.player.PlayerBucketFillEvent;

import java.util.LinkedHashMap;
import java.util.Map;

public class BucketListener implements Listener {

    private final StellarProtect plugin = StellarProtect.getInstance();

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onBucketEmpty(PlayerBucketEmptyEvent event) {
        Player player = event.getPlayer();
        if (ActionType.BUCKET_EMPTY.shouldSkipLog(player.getWorld().getName(), event.getBucket().name())) return;
        PlayerProtect playerProtect = PlayerProtect.getPlayer(player);
        if (playerProtect == null) return;
        scheduleBucket(event, playerProtect, ActionType.BUCKET_EMPTY);
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onBucketFill(PlayerBucketFillEvent event) {
        Player player = event.getPlayer();
        if (ActionType.BUCKET_FILL.shouldSkipLog(player.getWorld().getName(), event.getBucket().name())) return;
        PlayerProtect playerProtect = PlayerProtect.getPlayer(player);
        if (playerProtect == null) return;
        scheduleBucket(event, playerProtect, ActionType.BUCKET_FILL);
    }

    private Block getEventBlock(PlayerBucketEvent event) {
        try {
            Object affected = event.getClass().getMethod("getBlock").invoke(event);
            return affected instanceof Block ? (Block) affected : null;
        } catch (ReflectiveOperationException ignored) {
            return null;
        }
    }

    private void capture(Map<Location, BlockState> states, Block block) {
        if (block != null) states.computeIfAbsent(block.getLocation(), location -> block.getState());
    }

    private void scheduleBucket(PlayerBucketEvent event, PlayerProtect playerProtect, ActionType action) {
        Map<Location, BlockState> states = new LinkedHashMap<>();
        Block clicked = event.getBlockClicked();
        capture(states, getEventBlock(event));
        capture(states, clicked);
        capture(states, clicked.getRelative(event.getBlockFace()));
        if (action == ActionType.BUCKET_EMPTY) {
            capture(states, plugin.getProtectNMS().getBucketData(clicked, event.getBlockFace(), event.getBucket()).getBlock());
        }
        for (Map.Entry<Location, BlockState> entry : states.entrySet()) {
            BlockState previous = entry.getValue();
            plugin.getStellarTaskHook(() -> finishBucket(playerProtect, previous, action)).runTask(entry.getKey(), 1L);
        }
    }

    private void finishBucket(PlayerProtect playerProtect, BlockState oldState, ActionType action) {
        Block block = oldState.getBlock();
        BlockState newState = block.getState();
        String oldData = plugin.getProtectNMS().getBlockData(oldState);
        String newData = plugin.getProtectNMS().getBlockData(newState);
        if (oldData.equals(newData)) return;
        if (action == ActionType.BUCKET_EMPTY) BlockSourceCache.registerBlockSource(block.getLocation(), playerProtect.getPlayerId());
        else BlockSourceCache.removeBlockSource(block.getLocation());
        LoggerCache.addLog(new PlayerBlockLogEntry(playerProtect.getPlayerId(), oldState, newState, action));
    }

}
