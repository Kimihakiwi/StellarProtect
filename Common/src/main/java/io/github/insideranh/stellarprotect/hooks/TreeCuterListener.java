package io.github.insideranh.stellarprotect.hooks;

import io.github.insideranh.stellarprotect.StellarProtect;
import io.github.insideranh.stellarprotect.listeners.BlockListener;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.*;
import org.bukkit.plugin.EventExecutor;
import java.lang.reflect.Method;

public class TreeCuterListener implements Listener {
    public TreeCuterListener() {
        StellarProtect plugin = StellarProtect.getInstance();
        try {
            Class<? extends Event> eventClass =
                Class.forName("pl.norbit.treecuter.api.listeners.TreeCutEvent").asSubclass(Event.class);
            Method getPlayer = eventClass.getMethod("getPlayer");
            Method getBlocks = eventClass.getMethod("getBlocks");

            EventExecutor executor = (listener, event) -> {
                try {
                    Object p = getPlayer.invoke(event);
                    Object b = getBlocks.invoke(event);
                    if (!(p instanceof Player) || !(b instanceof Iterable)) return;
                    for (Object o : (Iterable<?>) b) {
                        if (o instanceof Block) {
                            BlockListener.processBlockBreak((Block) o, (Player) p, -2);
                        }
                    }
                } catch (ReflectiveOperationException ex) {
                    throw new EventException(ex);
                }
            };

            plugin.getServer().getPluginManager().registerEvent(
                eventClass, this, EventPriority.NORMAL, executor, plugin, false
            );
        } catch (ReflectiveOperationException | LinkageError ex) {
            plugin.getLogger().warning("TreeCuter hook API load failed: " + ex.getMessage());
        }
    }
}
