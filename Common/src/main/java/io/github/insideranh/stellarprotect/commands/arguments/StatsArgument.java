package io.github.insideranh.stellarprotect.commands.arguments;

import io.github.insideranh.stellarprotect.StellarProtect;
import io.github.insideranh.stellarprotect.arguments.*;
import io.github.insideranh.stellarprotect.cache.BlocksCache;
import io.github.insideranh.stellarprotect.cache.ItemsCache;
import io.github.insideranh.stellarprotect.commands.StellarArgument;
import io.github.insideranh.stellarprotect.database.entries.LogEntry;
import io.github.insideranh.stellarprotect.enums.ActionType;
import org.bukkit.command.CommandSender;
import org.jetbrains.annotations.NotNull;

import java.util.*;
import java.util.stream.Collectors;

public class StatsArgument extends StellarArgument {

    private final StellarProtect plugin = StellarProtect.getInstance();

    @Override
    public void onCommand(@NotNull CommandSender sender, String[] arguments) {
        if (arguments.length > 0 && arguments[0].equalsIgnoreCase("db")) {
            plugin.getLookupExecutor().execute(() -> {
                io.github.insideranh.stellarprotect.database.ProtectDatabase db = plugin.getProtectDatabase();
                boolean healthy = db.isDatabaseConnected();
                long pending = db.getPendingRecoveryLogs();
                long lastSave = db.getLastSaveAt();
                long failures = db.getSaveFailureCount();
                plugin.getStellarTaskHook(() -> {
                    sender.sendMessage("§6StellarProtect database status");
                    sender.sendMessage("§eDatabase: §f" + plugin.getConfig().getString("databases.type", "h2"));
                    sender.sendMessage("§eConnection: §f" + (healthy ? "connected" : "unavailable"));
                    sender.sendMessage("§eRecovery queue: §f" + (pending < 0 ? "unknown" : pending));
                    sender.sendMessage("§eLast successful save: §f" + (lastSave == 0 ? "not observed since startup" : new java.util.Date(lastSave).toString()));
                    sender.sendMessage("§eSave failures since startup: §f" + failures);
                }).runTask();
            });
            return;
        }

        TimeArg timeArg = ArgumentsParser.parseTime(arguments);
        RadiusArg radiusArg = ArgumentsParser.parseRadiusOrNull(sender instanceof org.bukkit.entity.Player ? (org.bukkit.entity.Player) sender : null, arguments, sender instanceof org.bukkit.entity.Player ? ((org.bukkit.entity.Player) sender).getLocation() : null);
        List<ActionType> actionTypesArg = ArgumentsParser.parseActionTypes(arguments);

        DatabaseFilters filters = new DatabaseFilters();
        filters.setTimeFilter(timeArg);
        if (radiusArg != null) filters.setRadiusFilter(radiusArg);
        if (!actionTypesArg.isEmpty()) {
            filters.setActionTypesFilter(actionTypesArg.stream().map(ActionType::getId).collect(Collectors.toCollection(ArrayList::new)));
        }

        plugin.getProtectDatabase().getLogs(filters, true, 0, 100000).thenAccept(cb -> {
            Map<Long, Integer> playerCount = new HashMap<>();
            Map<Integer, Integer> actionCount = new HashMap<>();
            Map<Long, Integer> blockCount = new HashMap<>();
            Map<Long, Integer> itemCount = new HashMap<>();

            for (Set<LogEntry> set : cb.getLogs().values()) {
                for (LogEntry e : set) {
                    playerCount.merge(e.getPlayerId(), 1, Integer::sum);
                    actionCount.merge(e.getActionType(), 1, Integer::sum);
                    if (e.getBlockId() != null) blockCount.merge(e.getBlockId().longValue(), 1, Integer::sum);
                    if (e.getItemId() != null) itemCount.merge(e.getItemId(), 1, Integer::sum);
                }
            }

            sender.sendMessage("§6═══ Estadísticas ═══");
            sender.sendMessage("§eTotal logs: §f" + playerCount.values().stream().mapToInt(Integer::intValue).sum());

            sender.sendMessage("§eTop 5 jugadores:");
            playerCount.entrySet().stream()
                .sorted(Map.Entry.<Long, Integer>comparingByValue().reversed())
                .limit(5)
                .forEach(e -> sender.sendMessage("  §7- §f" + io.github.insideranh.stellarprotect.utils.PlayerUtils.getNameOfEntity(e.getKey()) + " §7: " + e.getValue()));

            sender.sendMessage("§eTop 5 acciones:");
            actionCount.entrySet().stream()
                .sorted(Map.Entry.<Integer, Integer>comparingByValue().reversed())
                .limit(5)
                .forEach(e -> {
                    ActionType at = ActionType.getById(e.getKey());
                    sender.sendMessage("  §7- §f" + (at == null ? "?" : at.name()) + " §7: " + e.getValue());
                });

            sender.sendMessage("§eTop 5 bloques:");
            blockCount.entrySet().stream()
                .sorted(Map.Entry.<Long, Integer>comparingByValue().reversed())
                .limit(5)
                .forEach(e -> sender.sendMessage("  §7- §f#" + e.getKey() + " §7: " + e.getValue()));
        });
    }

    @Override
    public List<String> onTabComplete(@NotNull CommandSender sender, String[] arguments) {
        return java.util.Arrays.asList("db", "t:1h", "t:1d", "t:30m", "r:10", "a:block_break");
    }

}