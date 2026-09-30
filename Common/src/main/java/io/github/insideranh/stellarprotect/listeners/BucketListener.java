package io.github.insideranh.stellarprotect.listeners;

import io.github.insideranh.stellarprotect.StellarProtect;
import io.github.insideranh.stellarprotect.cache.BlockSourceCache;
import io.github.insideranh.stellarprotect.cache.LoggerCache;
import io.github.insideranh.stellarprotect.callback.CallbackBucket;
import io.github.insideranh.stellarprotect.data.PlayerProtect;
import io.github.insideranh.stellarprotect.database.entries.players.PlayerBlockLogEntry;
import io.github.insideranh.stellarprotect.enums.ActionType;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerBucketEmptyEvent;
import org.bukkit.event.player.PlayerBucketFillEvent;
import org.bukkit.block.BlockFace;
import org.bukkit.block.BlockState;
import org.bukkit.block.data.BlockData;
import org.bukkit.block.data.Waterlogged;

public class BucketListener implements Listener {

    private final StellarProtect plugin = StellarProtect.getInstance();

    @EventHandler
    public void onBucketEmpty(PlayerBucketEmptyEvent event) {
        Player player = event.getPlayer();
        if (ActionType.BUCKET_EMPTY.shouldSkipLog(player.getWorld().getName(), event.getBucket().name())) return;
        PlayerProtect playerProtect = PlayerProtect.getPlayer(player);
        if (playerProtect == null) return;
        Block target = getBucketTarget(event.getBlockClicked(), event.getBlockFace(), event.getBucket(), true);
        BlockState oldState = target.getState();
        scheduleBucket(playerProtect, target, oldState, ActionType.BUCKET_EMPTY);
    }


    @EventHandler
    public void onBucketFill(PlayerBucketFillEvent event) {
        Player player = event.getPlayer();
        if (ActionType.BUCKET_FILL.shouldSkipLog(player.getWorld().getName(), event.getBucket().name())) return;
        PlayerProtect playerProtect = PlayerProtect.getPlayer(player);
        if (playerProtect == null) return;
        Block target = getBucketTarget(event.getBlockClicked(), event.getBlockFace(), event.getBucket(), false);
        BlockState oldState = target.getState();
        scheduleBucket(playerProtect, target, oldState, ActionType.BUCKET_FILL);
    }



    private Block getBucketTarget(Block clicked, BlockFace face, Material bucket, boolean empty) {
        BlockData data = clicked.getBlockData();
        if (data instanceof Waterlogged) {
            Waterlogged waterlogged = (Waterlogged) data;
            if (empty && bucket == Material.WATER_BUCKET && !waterlogged.isWaterlogged()) return clicked;
            if (!empty && waterlogged.isWaterlogged()) return clicked;
        }
        if (empty) return plugin.getProtectNMS().getBucketData(clicked, face, bucket).getBlock();
        return clicked.getRelative(face);
    }

    private void scheduleBucket(PlayerProtect playerProtect, Block block, BlockState oldState, ActionType action) {
        plugin.getStellarTaskHook(() -> finishBucket(playerProtect, block, oldState, action)).runTask(block.getLocation(), 1L);
    }

    private void finishBucket(PlayerProtect playerProtect, Block block, BlockState oldState, ActionType action) {
        BlockState newState = block.getState();
        if (oldState.getBlockData().getAsString().equals(newState.getBlockData().getAsString())) return;
        if (action == ActionType.BUCKET_EMPTY) BlockSourceCache.registerBlockSource(block.getLocation(), playerProtect.getPlayerId());
        else BlockSourceCache.removeBlockSource(block.getLocation());
        LoggerCache.addLog(new PlayerBlockLogEntry(playerProtect.getPlayerId(), oldState, newState, action));
    }

}