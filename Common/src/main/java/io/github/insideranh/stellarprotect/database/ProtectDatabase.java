package io.github.insideranh.stellarprotect.database;

import io.github.insideranh.stellarprotect.StellarProtect;
import io.github.insideranh.stellarprotect.arguments.DatabaseFilters;
import io.github.insideranh.stellarprotect.blocks.BlockTemplate;
import io.github.insideranh.stellarprotect.cache.keys.LocationCache;
import io.github.insideranh.stellarprotect.callback.CallbackLookup;
import io.github.insideranh.stellarprotect.data.PlayerProtect;
import io.github.insideranh.stellarprotect.database.entries.LogEntry;
import io.github.insideranh.stellarprotect.database.entries.QueuedLog;
import io.github.insideranh.stellarprotect.database.entries.players.PlayerTransactionEntry;
import io.github.insideranh.stellarprotect.enums.ActionType;
import com.google.gson.JsonObject;
import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import java.util.Map;
import java.util.HashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import io.github.insideranh.stellarprotect.database.entries.items.ItemLogEntry;
import io.github.insideranh.stellarprotect.database.repositories.DatabaseConnection;
import io.github.insideranh.stellarprotect.database.types.MySQLConnection;
import io.github.insideranh.stellarprotect.database.types.SQLConnection;
import io.github.insideranh.stellarprotect.database.types.SQLQueueConnection;
import io.github.insideranh.stellarprotect.items.ItemTemplate;
import io.github.insideranh.stellarprotect.utils.Debugger;
import io.github.insideranh.stellarprotect.cache.LoggerCache;
import lombok.NonNull;
import org.bukkit.Location;
import org.bukkit.entity.Player;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;
import java.util.UUID;

public class ProtectDatabase {

    private final StellarProtect stellarProtect = StellarProtect.getInstance();
    private DatabaseConnection databaseConnection;
    private SQLQueueConnection temporalConnection;

    public void connect() {
        String databaseType = stellarProtect.getConfig().getString("databases.type", "h2");
        if (databaseType.equalsIgnoreCase("mysql")) {
            this.databaseConnection = new MySQLConnection();
        } else {
            this.databaseConnection = new SQLConnection();
        }
        this.temporalConnection = new SQLQueueConnection();
        this.databaseConnection.connect();
        this.temporalConnection.connect();
        this.stellarProtect.getLookupExecutor().execute(() -> this.databaseConnection.createIndexes());
    }

    public void load() {
        this.databaseConnection.getIdsRepository().loadWorlds();
        this.databaseConnection.getIdsRepository().loadEntityIds();

        this.stellarProtect.getLookupExecutor().execute(() -> this.databaseConnection.getItemsRepository().loadMostUsedItems());
        this.stellarProtect.getLookupExecutor().execute(() -> this.databaseConnection.getBlocksRepository().loadBlockDatas());
    }

    public void vacuum() {
        this.databaseConnection.vacuum();
    }

    public void close() {
        if (this.databaseConnection != null) this.databaseConnection.close();
        if (this.temporalConnection != null) this.temporalConnection.close();
    }


    public void clearOldLogs() {
        this.databaseConnection.getLoggerRepository().clearOldLogs();
    }

    public void purgeLogs(@NonNull DatabaseFilters databaseFilters, Consumer<Long> onFinished) {
        this.databaseConnection.getLoggerRepository().purgeLogs(databaseFilters, onFinished);
    }

    public void saveQueue(List<LogEntry> logEntries) {
        if (logEntries.isEmpty()) return;

        this.temporalConnection.save(logEntries);
    }


    public CompletableFuture<CallbackLookup<List<ItemLogEntry>, Long>> getChestTransactions(@NonNull Location location, int skip, int limit) {
        return databaseConnection.getLoggerRepository().getChestTransactions(location, skip, limit);
    }

    public CompletableFuture<CallbackLookup<Map<LocationCache, Set<LogEntry>>, Long>> getLogs(@NonNull DatabaseFilters databaseFilters, boolean ignoreCache, int skip, int limit) {
        return databaseConnection.getLoggerRepository().getLogs(databaseFilters, ignoreCache, skip, limit);
    }

    public CompletableFuture<CallbackLookup<Map<LocationCache, Set<LogEntry>>, Long>> getRestoreActions(@NonNull DatabaseFilters filters, int skip, int limit) {
        return databaseConnection.getRestoreRepository().getRestoreActions(filters, skip, limit);
    }

    public CompletableFuture<Long> countRestoreActions(@NonNull DatabaseFilters filters) {
        return databaseConnection.getRestoreRepository().countRestoreActions(filters);
    }

    public CompletableFuture<CallbackLookup<Set<LogEntry>, Long>> getLogs(@NonNull Location location, int skip, int limit) {
        return databaseConnection.getLoggerRepository().getLogs(location, skip, limit);
    }

    public List<Long> getIdsByNames(List<String> names) {
        return databaseConnection.getPlayerRepository().getIdsByNames(names);
    }

    public PlayerProtect loadOrCreatePlayer(Player player) {
        return databaseConnection.getPlayerRepository().loadOrCreatePlayer(player);
    }

    public void save(List<LogEntry> logEntries) {
        if (logEntries.isEmpty()) return;

        this.databaseConnection.getLoggerRepository().save(logEntries);
    }

    public void saveWorld(String world, int id) {
        this.databaseConnection.getIdsRepository().saveWorld(world, id);
    }

    public void saveEntityId(String entityType, long id) {
        this.databaseConnection.getIdsRepository().saveEntityId(entityType, id);
    }

    public void saveItems(List<ItemTemplate> itemTemplates) {
        this.databaseConnection.getItemsRepository().saveItems(itemTemplates);
    }

    public void saveBlocks(List<BlockTemplate> blockTemplates) {
        this.databaseConnection.getBlocksRepository().saveBlocks(blockTemplates);
    }

    private final AtomicBoolean queueProcessing = new AtomicBoolean();
    private final AtomicBoolean queueDeletionFailed = new AtomicBoolean();

    public void processQueueLogs() {
        if (queueDeletionFailed.get() || !queueProcessing.compareAndSet(false, true)) return;
        try {
            stellarProtect.getLookupExecutor().execute(() -> {
                try {
                    int batchSize = stellarProtect.getConfigManager().getBatchQueueSize();
                    List<QueuedLog> queuedLogs = temporalConnection.getLogs(batchSize);
                    if (queuedLogs.isEmpty()) return;

                    List<LogEntry> entries = new java.util.ArrayList<>(queuedLogs.size());
                    for (QueuedLog queued : queuedLogs) {
                        LogEntry entry;
                        if (queued.getActionType() == ActionType.INVENTORY_TRANSACTION.getId() && queued.getExtraJson() != null) {
                            JsonObject json = new JsonParser().parse(queued.getExtraJson()).getAsJsonObject();
                            Map<Long, Integer> added = readTransactionItems(json, "ai");
                            Map<Long, Integer> removed = readTransactionItems(json, "ri");
                            entry = new PlayerTransactionEntry(queued.getPlayerId(), added, removed,
                                queued.getWorldId(), queued.getX(), queued.getY(), queued.getZ(), queued.getCreatedAt());
                        } else {
                            entry = new LogEntry(queued.getPlayerId(), queued.getActionType(), queued.getWorldId(),
                                queued.getX(), queued.getY(), queued.getZ(), queued.getCreatedAt());
                        }
                        entry.setRestored(queued.isRestored());
                        entry.setBlockId(queued.getBlockId());
                        entry.setOldBlockId(queued.getOldBlockId());
                        entry.setItemId(queued.getItemId());
                        if (queued.getAmount() != null) entry.setAmount(queued.getAmount());
                        if (queued.getItemId() == null && isItemAction(queued.getActionType()) && queued.getExtraJson() != null) {
                            JsonObject itemJson = new JsonParser().parse(queued.getExtraJson()).getAsJsonObject();
                            if (itemJson.has("id")) entry.setItemId(itemJson.get("id").getAsLong());
                            else if (itemJson.has("it64")) entry.setItemId(itemJson.get("it64").getAsLong());
                            if (queued.getAmount() == null && itemJson.has("a")) entry.setAmount(itemJson.get("a").getAsInt());
                        }
                        entry.setEntityType(queued.getEntityType());
                        if (queued.getExtraJson() != null) entry.setForcedJson(queued.getExtraJson());
                        entries.add(entry);
                    }

                    if (!databaseConnection.getLoggerRepository().saveQueuedBatch(entries)) {
                        stellarProtect.getLogger().warning("Recovery queue batch was not committed; retained for retry");
                        return;
                    }
                    long maxId = queuedLogs.get(queuedLogs.size() - 1).getId();
                    boolean deleted = temporalConnection.deleteProcessedLogs(maxId).get();
                    if (!deleted) {
                        queueDeletionFailed.set(true);
                        stellarProtect.getLogger().severe("Recovery queue deletion failed after database commit; processing paused to prevent immediate replay");
                    }
                } catch (InterruptedException exception) {
                    Thread.currentThread().interrupt();
                    stellarProtect.getLogger().warning("Recovery queue processing interrupted");
                } catch (Exception exception) {
                    stellarProtect.getLogger().warning("Error processing queue logs: " + exception.getMessage());
                } finally {
                    queueProcessing.set(false);
                }
            });
        } catch (java.util.concurrent.RejectedExecutionException exception) {
            queueProcessing.set(false);
            stellarProtect.getLogger().warning("Recovery queue executor is unavailable");
        }
    }

    private boolean isItemAction(int id) {
        return id == ActionType.DROP_ITEM.getId()
            || id == ActionType.PICKUP_ITEM.getId()
            || id == ActionType.FURNACE_EXTRACT.getId()
            || id == ActionType.CRAFT.getId()
            || id == ActionType.ENCHANT.getId()
            || id == ActionType.SMITH.getId()
            || id == ActionType.CONSUME.getId();
    }

    private Map<Long, Integer> readTransactionItems(JsonObject json, String key) {
        Map<Long, Integer> items = new HashMap<>();
        if (!json.has(key) || !json.get(key).isJsonObject()) return items;
        for (Map.Entry<String, JsonElement> item : json.getAsJsonObject(key).entrySet()) {
            items.put(Long.parseLong(item.getKey()), item.getValue().getAsInt());
        }
        return items;
    }

    public PlayerProtect loadOrCreatePlayer(UUID uuid, String name) {
        return databaseConnection.getPlayerRepository().loadOrCreatePlayer(uuid, name);
    }

    public void flushLogsSync() {
        List<LogEntry> logs = LoggerCache.getFlushLogsToDatabase();
        if (!logs.isEmpty()) databaseConnection.getLoggerRepository().saveSync(logs);
    }

}