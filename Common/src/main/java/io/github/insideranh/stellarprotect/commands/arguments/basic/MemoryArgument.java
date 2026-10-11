package io.github.insideranh.stellarprotect.commands.arguments.basic;

import io.github.insideranh.stellarprotect.StellarProtect;
import io.github.insideranh.stellarprotect.cache.LoggerCache;
import io.github.insideranh.stellarprotect.commands.StellarArgument;
import org.bukkit.command.CommandSender;

import java.util.*;

public class MemoryArgument extends StellarArgument {

    private final StellarProtect plugin = StellarProtect.getInstance();

    @Override
    public void onCommand(CommandSender sender, String[] args) {
        Runtime runtime = Runtime.getRuntime();
        long used = runtime.totalMemory() - runtime.freeMemory();
        sender.sendMessage("§aMemory analysis");
        sender.sendMessage("§7JVM used: §f" + formatMemorySize(used));
        sender.sendMessage("§7JVM committed: §f" + formatMemorySize(runtime.totalMemory()));
        sender.sendMessage("§7JVM max: §f" + formatMemorySize(runtime.maxMemory()));
        sender.sendMessage("§7Item templates: §f" + StellarProtect.getInstance().getItemsManager().getItemCache().size());
        sender.sendMessage("§7Query cache: §f" + LoggerCache.getQueryCache().size());
    }


    @Override
    public List<String> onTabComplete(CommandSender sender, String[] args) {
        return Collections.emptyList();
    }


    private String formatMemorySize(long bytes) {
        if (bytes < 1024) {
            return "§f" + bytes + " §7bytes";
        }

        double kb = bytes / 1024.0;
        if (kb < 1024) {
            return String.format("§f%.2f §7KB §8(§f%d §7bytes§8)", kb, bytes);
        }

        double mb = kb / 1024.0;
        if (mb < 1024) {
            return String.format("§f%.2f §7MB §8(§f%.2f §7KB§8)", mb, kb);
        }

        double gb = mb / 1024.0;
        return String.format("§f%.2f §7GB §8(§f%.2f §7MB§8)", gb, mb);
    }

}