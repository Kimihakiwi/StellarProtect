package io.github.insideranh.stellarprotect.hooks;

import io.github.insideranh.stellarprotect.StellarProtect;
import io.github.insideranh.stellarprotect.cache.LoggerCache;
import io.github.insideranh.stellarprotect.data.PlayerProtect;
import io.github.insideranh.stellarprotect.database.entries.hooks.PlayerXKitEventLogEntry;
import io.github.insideranh.stellarprotect.enums.ActionType;
import io.github.insideranh.stellarprotect.utils.PlayerUtils;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.*;
import org.bukkit.plugin.EventExecutor;
import java.lang.reflect.Method;

public class XPlayerKitsListener implements Listener {
    public XPlayerKitsListener() {
        registerClaim();
        registerGive();
    }

    private void registerClaim() {
        StellarProtect plugin = StellarProtect.getInstance();
        try {
            Class<? extends Event> eventClass =
                Class.forName("io.github.InsiderAnh.xPlayerKits.api.events.ClaimXKitEvent").asSubclass(Event.class);
            Method getPlayer = eventClass.getMethod("getPlayer");
            Method getKitName = eventClass.getMethod("getKitName");

            EventExecutor executor = (listener, event) -> {
                try {
                    Object p = getPlayer.invoke(event);
                    Object k = getKitName.invoke(event);
                    if (!(p instanceof Player) || k == null) return;

                    Player player = (Player) p;
                    String kitName = String.valueOf(k);
                    if (ActionType.X_KIT_EVENT.shouldSkipLog(player.getWorld().getName(), kitName)) return;

                    PlayerProtect pp = PlayerProtect.getPlayer(player);
                    if (pp == null) return;

                    LoggerCache.addLog(new PlayerXKitEventLogEntry(
                        pp.getPlayerId(), player.getLocation(), (byte) 0, kitName
                    ));
                } catch (ReflectiveOperationException ex) {
                    throw new EventException(ex);
                }
            };

            plugin.getServer().getPluginManager().registerEvent(
                eventClass, this, EventPriority.MONITOR, executor, plugin, true
            );
        } catch (ReflectiveOperationException | LinkageError ex) {
            plugin.getLogger().warning("XPlayerKits Claim event API load failed: " + ex.getMessage());
        }
    }

    private void registerGive() {
        StellarProtect plugin = StellarProtect.getInstance();
        try {
            Class<? extends Event> eventClass =
                Class.forName("io.github.InsiderAnh.xPlayerKits.api.events.GiveXKitEvent").asSubclass(Event.class);
            Method getReceiver = eventClass.getMethod("getReceiver");
            Method getKitName = eventClass.getMethod("getKitName");
            Method getGiver = eventClass.getMethod("getGiver");

            EventExecutor executor = (listener, event) -> {
                try {
                    Object r = getReceiver.invoke(event);
                    Object k = getKitName.invoke(event);
                    Object g = getGiver.invoke(event);
                    if (!(r instanceof Player) || k == null || !(g instanceof CommandSender)) return;

                    Player receiver = (Player) r;
                    String kitName = String.valueOf(k);
                    if (ActionType.X_KIT_EVENT.shouldSkipLog(receiver.getWorld().getName(), kitName)) return;

                    long playerId = PlayerUtils.getPlayerOrConsoleId((CommandSender) g);
                    LoggerCache.addLog(new PlayerXKitEventLogEntry(
                        playerId, receiver.getLocation(), (byte) 1, kitName
                    ));
                } catch (ReflectiveOperationException ex) {
                    throw new EventException(ex);
                }
            };

            plugin.getServer().getPluginManager().registerEvent(
                eventClass, this, EventPriority.MONITOR, executor, plugin, true
            );
        } catch (ReflectiveOperationException | LinkageError ex) {
            plugin.getLogger().warning("XPlayerKits Give event API load failed: " + ex.getMessage());
        }
    }
}
