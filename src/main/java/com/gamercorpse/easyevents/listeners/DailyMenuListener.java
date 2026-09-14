package com.gamercorpse.easyevents.listeners;

import com.gamercorpse.easyevents.EasyEvents;
import com.gamercorpse.easyevents.daily.DailyMenuHolder;
import com.gamercorpse.easyevents.modules.ModuleManager;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryAction;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.Inventory;

public class DailyMenuListener implements Listener {

    private final EasyEvents plugin;

    public DailyMenuListener(
            EasyEvents plugin
    ) {

        this.plugin =
                plugin;
    }

    @EventHandler
    public void onInventoryClick(
            InventoryClickEvent event
    ) {

        Inventory topInventory =
                event.getView()
                        .getTopInventory();

        if (!(topInventory.getHolder()
                instanceof DailyMenuHolder holder)) {

            return;
        }

        /*
         * Always protect an existing Daily GUI even if the
         * module has just been disabled.
         */
        event.setCancelled(true);

        if (!(event.getWhoClicked()
                instanceof Player player)) {

            return;
        }

        if (!player.getUniqueId()
                .equals(
                        holder.getPlayerUuid()
                )) {

            return;
        }

        if (!plugin.isModuleEnabled(
                ModuleManager.DAILY_LOGIN
        )) {

            player.closeInventory();

            return;
        }

        int rawSlot =
                event.getRawSlot();

        int topSize =
                topInventory.getSize();

        if (rawSlot >= 0 &&
                rawSlot < topSize) {

            Integer dayKey =
                    holder.getDayKey(
                            rawSlot
                    );

            if (dayKey == null) {
                return;
            }

            plugin.getDailyManager()
                    .handleDayClick(
                            player,
                            dayKey
                    );

            return;
        }

        /*
         * The event is already cancelled, which also prevents
         * shift-clicking, double-click collection, hotbar swaps,
         * and other inventory transfer methods while the GUI
         * is open.
         */
        if (event.isShiftClick()) {
            return;
        }

        if (event.getClick() ==
                ClickType.DOUBLE_CLICK) {

            return;
        }

        if (event.getAction() ==
                InventoryAction.COLLECT_TO_CURSOR) {

            return;
        }
    }

    @EventHandler
    public void onInventoryDrag(
            InventoryDragEvent event
    ) {

        Inventory topInventory =
                event.getView()
                        .getTopInventory();

        if (!(topInventory.getHolder()
                instanceof DailyMenuHolder)) {

            return;
        }

        int topSize =
                topInventory.getSize();

        for (Integer rawSlot :
                event.getRawSlots()) {

            if (rawSlot >= 0 &&
                    rawSlot < topSize) {

                event.setCancelled(true);
                return;
            }
        }
    }

    @EventHandler
    public void onPlayerQuit(
            PlayerQuitEvent event
    ) {

        plugin.getDailyManager()
                .clearClaimLock(
                        event.getPlayer()
                                .getUniqueId()
                );
    }
}