package io.github.insideranh.stellarprotect.nms.v1_13_R2;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import io.github.insideranh.stellarprotect.enums.ExtraDataType;
import io.github.insideranh.stellarprotect.restore.BlockRestore;
import io.github.insideranh.stellarprotect.utils.SerializerUtils;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockState;
import org.bukkit.block.data.BlockData;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;

public class BlockRestore_v1_13_R2 extends BlockRestore {

    public BlockRestore_v1_13_R2(String data) {
        super(data);
    }

    public BlockRestore_v1_13_R2(String data, byte extraType, String extraData) {
        super(data, extraType, extraData);
    }

    public BlockRestore_v1_13_R2(String data, byte extraType, String extraData, boolean isPlace) {
        super(data, extraType, extraData, isPlace);
    }

    public BlockRestore_v1_13_R2(
            String data,
            byte extraType,
            String extraData,
            boolean isPlace,
            String oldData,
            String blockEntityNbt,
            String oldBlockEntityNbt
    ) {
        super(data, extraType, extraData, isPlace, oldData, blockEntityNbt, oldBlockEntityNbt);
    }

    @Override
    public void reset(Gson gson, Location location) {
        Block block = location.getBlock();
        if (isPlace) {
            applyBlockData(block, oldData);
        } else {
            applyBlockData(block, data);
            applyContainer(block);
        }
    }

    @Override
    public void undoPlace(Gson gson, Location location) {
        Block block = location.getBlock();
        applyBlockData(block, data);
        applyContainer(block);
    }

    private void applyBlockData(Block block, String blockDataString) {
        if (blockDataString == null || blockDataString.isEmpty()) {
            block.setType(Material.AIR);
            return;
        }

        try {
            BlockData blockData = Bukkit.createBlockData(blockDataString);
            block.setBlockData(blockData, false);
        } catch (Exception ignored) {
            block.setType(Material.AIR);
        }
    }

    private void applyContainer(Block block) {
        if (extraType != ExtraDataType.INVENTORY_CONTENT.getId()) return;
        if (extraData == null || extraData.isEmpty()) return;

        try {
            BlockState state = block.getState();
            if (!(state instanceof InventoryHolder)) return;

            JsonObject jsonInventory = new JsonParser().parse(extraData).getAsJsonObject();
            Inventory inventory = ((InventoryHolder) state).getInventory();
            SerializerUtils.setInventoryContent(inventory, jsonInventory);
        } catch (Exception ignored) {
        }
    }

    @Override
    public void preview(Player player, Gson gson, Location location) {
        try {
            player.sendBlockChange(location, Bukkit.createBlockData(data));
        } catch (Exception ignored) {
        }
    }

    @Override
    public void previewRemove(Player player, Location location) {
        player.sendBlockChange(location, Material.AIR.createBlockData());
    }
}
