package io.github.insideranh.stellarprotect.database.entries.world;

import com.google.gson.JsonObject;
import io.github.insideranh.stellarprotect.StellarProtect;
import io.github.insideranh.stellarprotect.database.entries.LogEntry;
import io.github.insideranh.stellarprotect.enums.ActionType;
import io.github.insideranh.stellarprotect.items.ItemReference;
import lombok.Getter;
import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.sql.ResultSet;

@Getter
public class BrewingLogEntry extends LogEntry {

    private final Long ingredientId;
    private final Long fuelId;
    private final Long resultId;
    private final Integer amount;

    public BrewingLogEntry(ResultSet resultSet, JsonObject jsonObject) {
        super(resultSet);
        this.ingredientId = jsonObject.has("i") ? jsonObject.get("i").getAsLong() : -1L;
        this.fuelId = jsonObject.has("f") ? jsonObject.get("f").getAsLong() : -1L;
        this.resultId = jsonObject.has("r") ? jsonObject.get("r").getAsLong() : -1L;
        this.amount = jsonObject.has("a") ? jsonObject.get("a").getAsInt() : 1;
        setAmount(this.amount);
        if (this.resultId != null && this.resultId > 0) setItemId(this.resultId);
    }

    public BrewingLogEntry(Player player, ItemStack ingredient, ItemStack fuel, ItemStack result) {
        super(player != null ? io.github.insideranh.stellarprotect.utils.PlayerUtils.getPlayerOrConsoleId(player) : -2L,
            ActionType.BREWING.getId(),
            player != null ? player.getLocation() : new Location(null, 0, 0, 0),
            System.currentTimeMillis());
        long ingId = -1L;
        long fId = -1L;
        long rId = -1L;
        if (ingredient != null && !ingredient.getType().name().equals("AIR")) {
            ItemReference ing = StellarProtect.getInstance().getItemsManager().getItemReference(ingredient);
            ingId = ing.getTemplateId();
        }
        if (fuel != null && !fuel.getType().name().equals("AIR")) {
            ItemReference fl = StellarProtect.getInstance().getItemsManager().getItemReference(fuel);
            fId = fl.getTemplateId();
        }
        if (result != null && !result.getType().name().equals("AIR")) {
            ItemReference rs = StellarProtect.getInstance().getItemsManager().getItemReference(result);
            rId = rs.getTemplateId();
        }
        this.ingredientId = ingId;
        this.fuelId = fId;
        this.resultId = rId;
        this.amount = result != null ? result.getAmount() : 1;
        setItemId(this.resultId);
        setAmount(this.amount);
    }

    @Override
    public String toSaveJson() {
        JsonObject obj = new JsonObject();
        obj.addProperty("i", ingredientId);
        obj.addProperty("f", fuelId);
        obj.addProperty("r", resultId);
        obj.addProperty("a", amount);
        return obj.toString();
    }


    public BrewingLogEntry(Location location, ItemStack ingredient,
                           ItemStack fuel, ItemStack result) {
        super(-2L, ActionType.BREWING.getId(), location, System.currentTimeMillis());
        long ingredientId = -1L;
        long fuelId = -1L;
        long resultId = -1L;
        StellarProtect plugin = StellarProtect.getInstance();
        if (ingredient != null && ingredient.getType() != org.bukkit.Material.AIR)
            ingredientId = plugin.getItemsManager().getItemReference(ingredient).getTemplateId();
        if (fuel != null && fuel.getType() != org.bukkit.Material.AIR)
            fuelId = plugin.getItemsManager().getItemReference(fuel).getTemplateId();
        if (result != null && result.getType() != org.bukkit.Material.AIR)
            resultId = plugin.getItemsManager().getItemReference(result).getTemplateId();
        this.ingredientId = ingredientId;
        this.fuelId = fuelId;
        this.resultId = resultId;
        this.amount = result == null ? 1 : result.getAmount();
        setItemId(resultId);
        setAmount(this.amount);
    }

}