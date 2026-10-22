package io.github.insideranh.stellarprotect.utils;

import de.tr7zw.nbtapi.NBTContainer;
import de.tr7zw.nbtapi.NBTReflectionUtil;
import org.bukkit.block.TileState;
import org.bukkit.Bukkit;
import java.util.concurrent.atomic.AtomicLong;

import java.util.function.Consumer;

public final class NbtUtils {

    private static final AtomicLong lastWarning = new AtomicLong();

    private NbtUtils() {}

    private static void warn(String operation, Throwable error) {
        long now = System.currentTimeMillis();
        long previous = lastWarning.get();
        if (now - previous >= 60000L && lastWarning.compareAndSet(previous, now)) {
            Bukkit.getLogger().warning("[StellarProtect] Block NBT " + operation + " failed: " + error.getMessage());
        }
    }

    public static void writeBlockEntity(TileState ts, Consumer<String> sink) {
        try {
            NBTContainer nbt = new NBTContainer();
            NBTReflectionUtil.setEntityNBTTag(ts, nbt);
            sink.accept(nbt.toString());
        } catch (Throwable error) {
            warn("capture", error);
        }
    }

    public static void applyBlockEntity(TileState ts, String nbtJson) {
        if (nbtJson == null || nbtJson.isEmpty()) return;
        try {
            NBTContainer nbt = new NBTContainer(nbtJson);
            NBTReflectionUtil.setEntityNBTTag(ts, nbt);
            ts.update(true, false);
        } catch (Throwable error) {
            warn("restore", error);
        }
    }

}