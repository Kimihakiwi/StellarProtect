package io.github.insideranh.stellarprotect.database.repositories;

import io.github.insideranh.stellarprotect.data.PlayerProtect;
import org.bukkit.entity.Player;
import org.bukkit.Bukkit;

import java.util.List;
import java.util.UUID;

public interface PlayerRepository {

    PlayerProtect loadOrCreatePlayer(Player player);

    default PlayerProtect loadOrCreatePlayer(UUID uuid, String name) {
        Player online = Bukkit.getPlayer(uuid);
        if (online != null) return loadOrCreatePlayer(online);
        return null;
    }

    List<Long> getIdsByNames(List<String> names);

    long generateNextId();

}