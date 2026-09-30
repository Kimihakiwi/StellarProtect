package io.github.insideranh.stellarprotect.listeners;

import io.github.insideranh.stellarprotect.StellarProtect;
import io.github.insideranh.stellarprotect.cache.LoggerCache;
import io.github.insideranh.stellarprotect.cache.PlayerCache;
import io.github.insideranh.stellarprotect.data.PlayerProtect;
import io.github.insideranh.stellarprotect.database.entries.players.PlayerSessionEntry;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.Location;
import java.util.UUID;

public class JoinQuitListener implements Listener {

    private final StellarProtect plugin = StellarProtect.getInstance();

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        UUID uuid = player.getUniqueId();
        String name = player.getName();
        Location location = player.getLocation();
        boolean admin = player.hasPermission("stellarprotect.admin");
        plugin.getJoinExecutor().execute(() -> loadPlayerAsync(player, uuid, name, location, admin));
    }

    private void loadPlayerAsync(Player player, UUID uuid, String name, Location location, boolean admin) {
        PlayerProtect playerProtect = plugin.getProtectDatabase().loadOrCreatePlayer(uuid, name);
        if (playerProtect == null) return;
        plugin.getStellarTaskHook(() -> applyJoin(player, playerProtect, location, name, admin)).runEntity(player);
    }

    private void applyJoin(Player player, PlayerProtect playerProtect, Location location, String name, boolean admin) {
        if (!player.isOnline() || !player.getUniqueId().equals(playerProtect.getUuid())) return;
        playerProtect.create();
        playerProtect.setLoginTime(System.currentTimeMillis());
        LoggerCache.addLog(new PlayerSessionEntry(playerProtect.getPlayerId(), location, (byte) 1, 0));
        PlayerCache.cacheName(playerProtect.getPlayerId(), name);
        plugin.getVaultHook().joinPlayer(player, playerProtect);
        if (plugin.getConfigManager().isCheckUpdates() && admin && plugin.getUpdateChecker() != null) {
            plugin.getUpdateChecker().sendUpdateMessage(player);
        }
    }


    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();
        PlayerProtect playerProtect = PlayerProtect.removePlayer(player);
        if (playerProtect == null) return;

        PickUpDropListener.forceFlushCurrentGroup(playerProtect, true);

        long time = (System.currentTimeMillis() - playerProtect.getLoginTime()) / 1000L;
        if (time > 0) {
            LoggerCache.addLog(new PlayerSessionEntry(playerProtect.getPlayerId(), player.getLocation(), (byte) 0, time));
        }

        PlayerCache.removeCacheName(playerProtect.getPlayerId());
    }

}