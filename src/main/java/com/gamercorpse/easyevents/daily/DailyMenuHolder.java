package com.gamercorpse.easyevents.daily;

import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class DailyMenuHolder implements InventoryHolder {

    private final UUID playerUuid;

    private final Map<Integer, Integer> dayKeysBySlot =
            new HashMap<>();

    private Inventory inventory;

    public DailyMenuHolder(
            UUID playerUuid
    ) {

        this.playerUuid =
                playerUuid;
    }

    public UUID getPlayerUuid() {
        return playerUuid;
    }

    public void setDayKey(
            int slot,
            int dayKey
    ) {

        dayKeysBySlot.put(
                slot,
                dayKey
        );
    }

    public Integer getDayKey(
            int slot
    ) {

        return dayKeysBySlot.get(
                slot
        );
    }

    public void setInventory(
            Inventory inventory
    ) {

        this.inventory =
                inventory;
    }

    @Override
    public Inventory getInventory() {
        return inventory;
    }
}