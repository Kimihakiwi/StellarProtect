package io.github.insideranh.stellarprotect.commands.arguments;

import io.github.insideranh.stellarprotect.StellarProtect;
import io.github.insideranh.stellarprotect.arguments.ArgumentsParser;
import io.github.insideranh.stellarprotect.arguments.DatabaseFilters;
import io.github.insideranh.stellarprotect.arguments.HashTagsArg;
import io.github.insideranh.stellarprotect.arguments.RadiusArg;
import io.github.insideranh.stellarprotect.arguments.TimeArg;
import io.github.insideranh.stellarprotect.cache.keys.LocationCache;
import io.github.insideranh.stellarprotect.commands.StellarArgument;
import io.github.insideranh.stellarprotect.data.PlayerProtect;
import io.github.insideranh.stellarprotect.database.entries.LogEntry;
import io.github.insideranh.stellarprotect.database.entries.players.PlayerInventorySnapshotEntry;
import io.github.insideranh.stellarprotect.database.entries.players.PlayerItemLogEntry;
import io.github.insideranh.stellarprotect.enums.ActionType;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.jetbrains.annotations.NotNull;

import java.util.*;
import java.util.stream.Collectors;

public class ItemRestoreArgument extends StellarArgument {

    private final StellarProtect plugin = StellarProtect.getInstance();

    @Override
    public void onCommand(@NotNull org.bukkit.command.CommandSender sender, String[] arguments) {
        if (!(sender instanceof Player)) {
            plugin.getLangManager().sendMessage(sender, "messages.onlyPlayer");
            return;
        }
        Player player = (Player) sender;
        PlayerProtect playerProtect = PlayerProtect.getPlayer(player);
        if (playerProtect == null) return;

        HashTagsArg hashTagsArg = new HashTagsArg(arguments);
        TimeArg timeArg = ArgumentsParser.parseTime(arguments);
        RadiusArg radiusArg = ArgumentsParser.parseRadiusOrNull(player, arguments, player.getLocation());
        if (radiusArg == null) {
            radiusArg = new RadiusArg(player.getLocation(), 10, -1);
            plugin.getLangManager().sendMessage(player, "messages.specifyRadius");
        }

        List<ActionType> actionTypes = Arrays.asList(ActionType.DROP_ITEM, ActionType.PICKUP_ITEM, ActionType.CRAFT, ActionType.ENCHANT, ActionType.SMITH, ActionType.CONSUME, ActionType.BREWING);
        List<String> includesArg = ArgumentsParser.parseIncludesMaterials(arguments);
        List<String> excludesArg = ArgumentsParser.parseExcludesMaterials(arguments);

        RadiusArg finalRadiusArg = radiusArg;
        ArgumentsParser.parseUsers(arguments).thenAccept(usersArg -> {
            DatabaseFilters filters = new DatabaseFilters();
            filters.setTimeFilter(timeArg);
            filters.setRadiusFilter(finalRadiusArg);
            filters.setActionTypesFilter(actionTypes.stream().map(ActionType::getId).collect(Collectors.toCollection(ArrayList::new)));
            filters.setUserFilters(usersArg);
            filters.setIncludeMaterialFilters(plugin.getItemsManager().getItemCache().findIdsByTypeNameContains(includesArg, io.github.insideranh.stellarprotect.cache.ItemsCache.FieldType.LOWER_TYPE_NAME));
            filters.setExcludeMaterialFilters(plugin.getItemsManager().getItemCache().findIdsByTypeNameContains(excludesArg, io.github.insideranh.stellarprotect.cache.ItemsCache.FieldType.LOWER_TYPE_NAME));
            filters.setMinAmount(ArgumentsParser.parseMinAmount(arguments));
            filters.setMaxAmount(ArgumentsParser.parseMaxAmount(arguments));

            plugin.getProtectDatabase().getRestoreActions(filters, 0, 10000).thenAccept(cb -> {
                List<PlayerItemLogEntry> itemLogs = new ArrayList<>();
                for (Set<LogEntry> locationLogs : cb.getLogs().values()) {
                    for (LogEntry log : locationLogs) {
                        if (log instanceof PlayerItemLogEntry) itemLogs.add((PlayerItemLogEntry) log);
                    }
                }
                boolean remove = hashTagsArg.isUndoMode() || hashTagsArg.isRedo();
                plugin.getStellarTaskHook(() -> restoreBatch(player, itemLogs, 0, 0, 0, remove)).runEntity(player);
            });
        });
    }

    private void restoreBatch(Player player, List<PlayerItemLogEntry> logs, int offset, int restored, int removed, boolean remove) {
        if (!player.isOnline()) return;
        int end = Math.min(logs.size(), offset + 64);
        for (int i = offset; i < end; i++) {
            PlayerItemLogEntry log = logs.get(i);
            try {
                var template = plugin.getItemsManager().getItemTemplate(log.getItemReferenceId());
                if (template == null || template.getBukkitItem() == null) continue;
                ItemStack stack = template.getBukkitItem().clone();
                stack.setAmount(log.getAmount());
                int changed = giveOrRemove(player, stack, !remove);
                if (remove) removed += changed;
                else restored += changed;
            } catch (RuntimeException exception) {
                plugin.getLogger().warning("Failed to restore item " + log.getItemReferenceId() + ": " + exception.getMessage());
            }
        }
        if (end < logs.size()) {
            int given = restored;
            int taken = removed;
            plugin.getStellarTaskHook(() -> restoreBatch(player, logs, end, given, taken, remove)).runEntity(player);
            return;
        }
        player.sendMessage("§aRestaurados: §f" + restored + " §7items.");
        player.sendMessage("§eDevueltos al suelo: §f" + removed);
    }

    private int giveOrRemove(Player player, ItemStack stack, boolean give) {
        int requested = stack.getAmount();
        PlayerInventory inventory = player.getInventory();
        HashMap<Integer, ItemStack> remaining;
        if (give) {
            remaining = inventory.addItem(stack.clone());
            Location location = player.getLocation();
            for (ItemStack leftover : remaining.values()) {
                player.getWorld().dropItemNaturally(location, leftover);
            }
            return requested;
        }
        remaining = inventory.removeItem(stack.clone());
        int notRemoved = 0;
        for (ItemStack leftover : remaining.values()) notRemoved += leftover.getAmount();
        return Math.max(0, requested - notRemoved);
    }

    @Override
    public List<String> onTabComplete(@NotNull org.bukkit.command.CommandSender sender, String[] arguments) {
        return java.util.Arrays.asList("t:1h", "t:1d", "t:30m", "r:10", "i:diamond", "e:stone", "b:64", "u:player", "#undo");
    }

}