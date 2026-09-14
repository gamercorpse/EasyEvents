package com.gamercorpse.easyevents.daily;

import com.gamercorpse.easyevents.EasyEvents;
import com.gamercorpse.easyevents.utils.ColorUtil;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.io.File;
import java.time.DateTimeException;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class DailyManager {

    private final EasyEvents plugin;

    private final Set<UUID> claimInProgress =
            ConcurrentHashMap.newKeySet();

    private File dailyFile;

    private volatile FileConfiguration dailyConfig;
    private volatile ZoneId zoneId =
            ZoneId.systemDefault();

    private volatile List<Integer> dayKeys =
            Collections.emptyList();

    private volatile Map<Integer, List<String>> rewardCommands =
            Collections.emptyMap();

    public DailyManager(EasyEvents plugin) {
        this.plugin = plugin;
    }

    public void initialize() {
        loadDailyConfiguration();
    }

    public void reload() {
        loadDailyConfiguration();
    }

    public void shutdown() {
        claimInProgress.clear();
    }

    private void loadDailyConfiguration() {

        if (!plugin.getDataFolder().exists()) {
            plugin.getDataFolder().mkdirs();
        }

        dailyFile =
                new File(
                        plugin.getDataFolder(),
                        "daily.yml"
                );

        if (!dailyFile.exists()) {

            try {

                plugin.saveResource(
                        "daily.yml",
                        false
                );

            } catch (IllegalArgumentException exception) {

                plugin.getLogger().severe(
                        "daily.yml was not found inside the plugin JAR."
                );

                return;
            }
        }

        FileConfiguration loadedConfig =
                YamlConfiguration.loadConfiguration(
                        dailyFile
                );

        ZoneId loadedZoneId =
                loadTimeZone(loadedConfig);

        List<Integer> loadedDayKeys =
                new ArrayList<>();

        Map<Integer, List<String>> loadedCommands =
                new HashMap<>();

        ConfigurationSection daysSection =
                loadedConfig.getConfigurationSection(
                        "days"
                );

        if (daysSection == null) {

            plugin.getLogger().warning(
                    "No 'days' section was found in daily.yml."
            );

        } else {

            for (String key :
                    daysSection.getKeys(false)) {

                try {

                    int day =
                            Integer.parseInt(key);

                    if (day < 1) {

                        plugin.getLogger().warning(
                                "Ignoring invalid daily reward day: " +
                                        key
                        );

                        continue;
                    }

                    loadedDayKeys.add(day);

                    loadedCommands.put(
                            day,
                            List.copyOf(
                                    loadedConfig.getStringList(
                                            "days." +
                                                    key +
                                                    ".commands"
                                    )
                            )
                    );

                } catch (NumberFormatException exception) {

                    plugin.getLogger().warning(
                            "Ignoring non-numeric daily reward day: " +
                                    key
                    );
                }
            }
        }

        loadedDayKeys.sort(
                Comparator.naturalOrder()
        );

        this.dailyConfig =
                loadedConfig;

        this.zoneId =
                loadedZoneId;

        this.dayKeys =
                List.copyOf(
                        loadedDayKeys
                );

        this.rewardCommands =
                Map.copyOf(
                        loadedCommands
                );

        validateGuiSlots();

        plugin.getLogger().info(
                "Loaded " +
                        dayKeys.size() +
                        " daily reward day(s)."
        );

        plugin.getLogger().info(
                "Daily reward timezone: " +
                        zoneId.getId()
        );
    }

    private ZoneId loadTimeZone(
            FileConfiguration config
    ) {

        String configured =
                config.getString(
                        "settings.timezone",
                        "SYSTEM"
                );

        if (configured == null ||
                configured.isBlank() ||
                configured.equalsIgnoreCase("SYSTEM")) {

            return ZoneId.systemDefault();
        }

        try {

            return ZoneId.of(configured);

        } catch (DateTimeException exception) {

            plugin.getLogger().warning(
                    "Invalid daily reward timezone '" +
                            configured +
                            "'."
            );

            plugin.getLogger().warning(
                    "Using the system timezone instead."
            );

            return ZoneId.systemDefault();
        }
    }

    private void validateGuiSlots() {

        FileConfiguration config =
                dailyConfig;

        if (config == null) {
            return;
        }

        int size =
                getGuiSize(config);

        Map<Integer, Integer> usedSlots =
                new HashMap<>();

        for (Integer dayKey : dayKeys) {

            String path =
                    "days." +
                            dayKey +
                            ".slot";

            int slot =
                    config.getInt(
                            path,
                            -1
                    );

            if (slot < 0 ||
                    slot >= size) {

                plugin.getLogger().warning(
                        "Daily reward day " +
                                dayKey +
                                " uses invalid GUI slot " +
                                slot +
                                ". Valid slots are 0-" +
                                (size - 1) +
                                "."
                );

                continue;
            }

            Integer previous =
                    usedSlots.put(
                            slot,
                            dayKey
                    );

            if (previous != null) {

                plugin.getLogger().warning(
                        "Daily reward days " +
                                previous +
                                " and " +
                                dayKey +
                                " both use GUI slot " +
                                slot +
                                "."
                );
            }
        }
    }

    public void openMenu(Player player) {

        if (player == null ||
                !player.isOnline()) {

            return;
        }

        UUID uuid =
                player.getUniqueId();

        plugin.getServer()
                .getAsyncScheduler()
                .runNow(
                        plugin,
                        task -> {

                            try {

                                DailyStatus status =
                                        calculateStatus(
                                                uuid,
                                                LocalDate.now(
                                                        zoneId
                                                )
                                        );

                                player.getScheduler()
                                        .run(
                                                plugin,
                                                scheduledTask -> {

                                                    if (!player.isOnline()) {
                                                        return;
                                                    }

                                                    openMenuNow(
                                                            player,
                                                            status
                                                    );
                                                },
                                                null
                                        );

                            } catch (Exception exception) {

                                plugin.getLogger().severe(
                                        "Could not open the daily rewards GUI for " +
                                                player.getName() +
                                                "."
                                );

                                exception.printStackTrace();
                            }
                        }
                );
    }

    private void openMenuNow(
            Player player,
            DailyStatus status
    ) {

        FileConfiguration config =
                dailyConfig;

        if (config == null) {

            player.sendMessage(
                    ColorUtil.colorize(
                            "&cThe daily rewards configuration is not loaded."
                    )
            );

            return;
        }

        List<Integer> days =
                dayKeys;

        if (days.isEmpty()) {

            sendMessage(
                    player,
                    "messages.no-rewards",
                    Map.of()
            );

            return;
        }

        int size =
                getGuiSize(config);

        String title =
                config.getString(
                        "gui.title",
                        "&6&lDaily Rewards"
                );

        if (title == null) {
            title = "&6&lDaily Rewards";
        }

        DailyMenuHolder holder =
                new DailyMenuHolder(
                        player.getUniqueId()
                );

        Inventory inventory =
                Bukkit.createInventory(
                        holder,
                        size,
                        ColorUtil.colorize(title)
                );

        holder.setInventory(inventory);

        if (config.getBoolean(
                "gui.filler.enabled",
                true
        )) {

            ItemStack filler =
                    buildItem(
                            config,
                            "gui.filler.item",
                            Map.of(),
                            Collections.emptyList()
                    );

            if (filler != null &&
                    filler.getType() != Material.AIR) {

                for (int slot = 0;
                     slot < size;
                     slot++) {

                    inventory.setItem(
                            slot,
                            filler.clone()
                    );
                }
            }
        }

        for (int position = 1;
             position <= days.size();
             position++) {

            int dayKey =
                    days.get(
                            position - 1
                    );

            int slot =
                    config.getInt(
                            "days." +
                                    dayKey +
                                    ".slot",
                            -1
                    );

            if (slot < 0 ||
                    slot >= size) {

                continue;
            }

            RewardState rewardState =
                    determineRewardState(
                            position,
                            status
                    );

            ItemStack item =
                    buildDayItem(
                            config,
                            dayKey,
                            position,
                            rewardState
                    );

            if (item == null) {
                continue;
            }

            inventory.setItem(
                    slot,
                    item
            );

            holder.setDayKey(
                    slot,
                    dayKey
            );
        }

        player.openInventory(
                inventory
        );
    }

    private RewardState determineRewardState(
            int position,
            DailyStatus status
    ) {

        if (status.alreadyClaimedToday()) {

            if (position <=
                    status.claimedThroughPosition()) {

                return RewardState.CLAIMED;
            }

            return RewardState.LOCKED;
        }

        if (position ==
                status.expectedPosition()) {

            return RewardState.AVAILABLE;
        }

        if (status.expectedPosition() > 1 &&
                position <
                        status.expectedPosition()) {

            return RewardState.CLAIMED;
        }

        return RewardState.LOCKED;
    }

    private ItemStack buildDayItem(
            FileConfiguration config,
            int dayKey,
            int position,
            RewardState state
    ) {

        String basePath =
                "days." + dayKey;

        String stateName =
                state.name()
                        .toLowerCase(
                                Locale.ROOT
                        );

        String stateItemPath =
                basePath +
                        "." +
                        stateName +
                        "-item";

        String itemPath;

        if (config.isConfigurationSection(
                stateItemPath
        )) {

            itemPath =
                    stateItemPath;

        } else {

            itemPath =
                    basePath + ".item";
        }

        Map<String, String> replacements =
                new HashMap<>();

        replacements.put(
                "%day%",
                String.valueOf(dayKey)
        );

        replacements.put(
                "%position%",
                String.valueOf(position)
        );

        replacements.put(
                "%status%",
                stateName
        );

        List<String> additionalLore =
                config.getStringList(
                        "gui.status-lore." +
                                stateName
                );

        return buildItem(
                config,
                itemPath,
                replacements,
                additionalLore
        );
    }

    private ItemStack buildItem(
            FileConfiguration config,
            String path,
            Map<String, String> replacements,
            List<String> additionalLore
    ) {

        String materialName =
                config.getString(
                        path + ".material",
                        "STONE"
                );

        Material material =
                parseMaterial(
                        materialName,
                        path
                );

        if (material == Material.AIR) {

            return new ItemStack(
                    Material.AIR
            );
        }

        int configuredAmount =
                config.getInt(
                        path + ".amount",
                        1
                );

        int amount =
                Math.max(
                        1,
                        Math.min(
                                configuredAmount,
                                material.getMaxStackSize()
                        )
                );

        ItemStack item =
                new ItemStack(
                        material,
                        amount
                );

        ItemMeta meta =
                item.getItemMeta();

        if (meta == null) {
            return item;
        }

        String displayName =
                config.getString(
                        path + ".name"
                );

        if (displayName != null) {

            meta.displayName(
                    ColorUtil.colorize(
                            replace(
                                    displayName,
                                    replacements
                            )
                    )
            );
        }

        List<String> configuredLore =
                new ArrayList<>(
                        config.getStringList(
                                path + ".lore"
                        )
                );

        configuredLore.addAll(
                additionalLore
        );

        if (!configuredLore.isEmpty()) {

            List<net.kyori.adventure.text.Component> lore =
                    new ArrayList<>();

            for (String line :
                    configuredLore) {

                lore.add(
                        ColorUtil.colorize(
                                replace(
                                        line,
                                        replacements
                                )
                        )
                );
            }

            meta.lore(lore);
        }

        if (config.contains(
                path + ".custom-model-data"
        )) {

            int customModelData =
                    config.getInt(
                            path +
                                    ".custom-model-data"
                    );

            meta.setCustomModelData(
                    customModelData
            );
        }

        meta.setUnbreakable(
                config.getBoolean(
                        path + ".unbreakable",
                        false
                )
        );

        for (String flagName :
                config.getStringList(
                        path + ".item-flags"
                )) {

            try {

                ItemFlag itemFlag =
                        ItemFlag.valueOf(
                                flagName.toUpperCase(
                                        Locale.ROOT
                                )
                        );

                meta.addItemFlags(
                        itemFlag
                );

            } catch (IllegalArgumentException exception) {

                plugin.getLogger().warning(
                        "Unknown ItemFlag '" +
                                flagName +
                                "' at " +
                                path +
                                "."
                );
            }
        }

        item.setItemMeta(meta);

        return item;
    }

    private Material parseMaterial(
            String materialName,
            String path
    ) {

        if (materialName == null ||
                materialName.isBlank()) {

            return Material.STONE;
        }

        Material material =
                Material.matchMaterial(
                        materialName
                );

        if (material != null) {
            return material;
        }

        plugin.getLogger().warning(
                "Unknown material '" +
                        materialName +
                        "' at " +
                        path +
                        ". Using STONE."
        );

        return Material.STONE;
    }

    public void handleDayClick(
            Player player,
            int dayKey
    ) {

        if (player == null ||
                !player.isOnline()) {

            return;
        }

        UUID uuid =
                player.getUniqueId();

        if (!claimInProgress.add(uuid)) {

            sendMessage(
                    player,
                    "messages.processing",
                    Map.of()
            );

            return;
        }

        plugin.getServer()
                .getAsyncScheduler()
                .runNow(
                        plugin,
                        task -> {

                            try {

                                processClaim(
                                        player,
                                        dayKey
                                );

                            } catch (Exception exception) {

                                plugin.getLogger().severe(
                                        "An error occurred while processing a daily reward claim for " +
                                                player.getName() +
                                                "."
                                );

                                exception.printStackTrace();

                                claimInProgress.remove(
                                        uuid
                                );

                                schedulePlayerMessage(
                                        player,
                                        "messages.error",
                                        Map.of(),
                                        false
                                );
                            }
                        }
                );
    }

    private void processClaim(
            Player player,
            int dayKey
    ) {

        UUID uuid =
                player.getUniqueId();

        List<Integer> days =
                dayKeys;

        int clickedPosition =
                days.indexOf(dayKey) + 1;

        if (clickedPosition <= 0) {

            claimInProgress.remove(uuid);

            schedulePlayerMessage(
                    player,
                    "messages.reward-no-longer-exists",
                    Map.of(
                            "%day%",
                            String.valueOf(dayKey)
                    ),
                    true
            );

            return;
        }

        LocalDate today =
                LocalDate.now(
                        zoneId
                );

        DailyStatus status =
                calculateStatus(
                        uuid,
                        today
                );

        Map<String, String> replacements =
                new HashMap<>();

        replacements.put(
                "%player%",
                player.getName()
        );

        replacements.put(
                "%uuid%",
                uuid.toString()
        );

        replacements.put(
                "%day%",
                String.valueOf(dayKey)
        );

        replacements.put(
                "%position%",
                String.valueOf(clickedPosition)
        );

        if (status.alreadyClaimedToday()) {

            claimInProgress.remove(uuid);

            schedulePlayerMessage(
                    player,
                    "messages.already-claimed-today",
                    replacements,
                    true
            );

            return;
        }

        if (clickedPosition !=
                status.expectedPosition()) {

            boolean previouslyClaimed =
                    status.expectedPosition() > 1 &&
                            clickedPosition <
                                    status.expectedPosition();

            claimInProgress.remove(uuid);

            schedulePlayerMessage(
                    player,
                    previouslyClaimed
                            ? "messages.already-claimed"
                            : "messages.locked",
                    replacements,
                    false
            );

            return;
        }

        String storageValue =
                today +
                        "|" +
                        clickedPosition;

        plugin.setStorageValue(
                getStorageKey(uuid),
                storageValue
        );

        dispatchRewardCommands(
                player,
                dayKey,
                clickedPosition
        );

        scheduleSuccessfulClaim(
                player,
                replacements
        );
    }

    private void dispatchRewardCommands(
            Player player,
            int dayKey,
            int position
    ) {

        List<String> commands =
                rewardCommands.getOrDefault(
                        dayKey,
                        Collections.emptyList()
                );

        if (commands.isEmpty()) {
            return;
        }

        UUID uuid =
                player.getUniqueId();

        String playerName =
                player.getName();

        plugin.getServer()
                .getGlobalRegionScheduler()
                .run(
                        plugin,
                        task -> {

                            for (String configuredCommand :
                                    commands) {

                                if (configuredCommand == null ||
                                        configuredCommand.isBlank()) {

                                    continue;
                                }

                                String command =
                                        configuredCommand
                                                .replace(
                                                        "%player%",
                                                        playerName
                                                )
                                                .replace(
                                                        "%uuid%",
                                                        uuid.toString()
                                                )
                                                .replace(
                                                        "%day%",
                                                        String.valueOf(
                                                                dayKey
                                                        )
                                                )
                                                .replace(
                                                        "%position%",
                                                        String.valueOf(
                                                                position
                                                        )
                                                )
                                                .trim();

                                while (command.startsWith("/")) {

                                    command =
                                            command.substring(1);
                                }

                                if (command.isBlank()) {
                                    continue;
                                }

                                try {

                                    Bukkit.dispatchCommand(
                                            Bukkit.getConsoleSender(),
                                            command
                                    );

                                } catch (Exception exception) {

                                    plugin.getLogger().severe(
                                            "Could not execute daily reward command: " +
                                                    command
                                    );

                                    exception.printStackTrace();
                                }
                            }
                        }
                );
    }

    private void scheduleSuccessfulClaim(
            Player player,
            Map<String, String> replacements
    ) {

        UUID uuid =
                player.getUniqueId();

        player.getScheduler()
                .run(
                        plugin,
                        task -> {

                            claimInProgress.remove(
                                    uuid
                            );

                            if (!player.isOnline()) {
                                return;
                            }

                            sendMessage(
                                    player,
                                    "messages.claimed",
                                    replacements
                            );

                            FileConfiguration config =
                                    dailyConfig;

                            boolean closeAfterClaim =
                                    config != null &&
                                            config.getBoolean(
                                                    "gui.close-after-claim",
                                                    false
                                            );

                            if (closeAfterClaim) {

                                player.closeInventory();

                            } else {

                                openMenu(player);
                            }
                        },
                        () -> claimInProgress.remove(
                                uuid
                        )
                );
    }

    private void schedulePlayerMessage(
            Player player,
            String path,
            Map<String, String> replacements,
            boolean reopenMenu
    ) {

        UUID uuid =
                player.getUniqueId();

        player.getScheduler()
                .run(
                        plugin,
                        task -> {

                            claimInProgress.remove(
                                    uuid
                            );

                            if (!player.isOnline()) {
                                return;
                            }

                            sendMessage(
                                    player,
                                    path,
                                    replacements
                            );

                            if (reopenMenu) {
                                openMenu(player);
                            }
                        },
                        () -> claimInProgress.remove(
                                uuid
                        )
                );
    }

    public void sendMessage(
            Player player,
            String path,
            Map<String, String> replacements
    ) {

        FileConfiguration config =
                dailyConfig;

        if (config == null) {
            return;
        }

        String message =
                config.getString(path);

        if (message == null ||
                message.isBlank()) {

            return;
        }

        player.sendMessage(
                ColorUtil.colorize(
                        replace(
                                message,
                                replacements
                        )
                )
        );
    }

    private String replace(
            String input,
            Map<String, String> replacements
    ) {

        if (input == null) {
            return "";
        }

        String result =
                input;

        for (Map.Entry<String, String> entry :
                replacements.entrySet()) {

            result =
                    result.replace(
                            entry.getKey(),
                            entry.getValue()
                    );
        }

        return result;
    }

    private DailyStatus calculateStatus(
            UUID uuid,
            LocalDate today
    ) {

        int totalDays =
                dayKeys.size();

        if (totalDays < 1) {

            return new DailyStatus(
                    false,
                    -1,
                    0
            );
        }

        DailyProgress progress =
                loadProgress(uuid);

        if (progress.lastClaimDate() == null ||
                progress.streakPosition() < 1) {

            return new DailyStatus(
                    false,
                    1,
                    0
            );
        }

        int storedPosition =
                Math.max(
                        1,
                        Math.min(
                                progress.streakPosition(),
                                totalDays
                        )
                );

        LocalDate lastClaim =
                progress.lastClaimDate();

        if (lastClaim.equals(today)) {

            return new DailyStatus(
                    true,
                    -1,
                    storedPosition
            );
        }

        if (lastClaim.plusDays(1)
                .equals(today)) {

            int nextPosition;

            if (storedPosition >= totalDays) {
                nextPosition = 1;
            } else {
                nextPosition =
                        storedPosition + 1;
            }

            int claimedThrough =
                    nextPosition == 1
                            ? 0
                            : storedPosition;

            return new DailyStatus(
                    false,
                    nextPosition,
                    claimedThrough
            );
        }

        /*
         * The player missed at least one calendar day,
         * or the stored date is otherwise not the immediately
         * previous day. Reset the streak to Day 1.
         */
        return new DailyStatus(
                false,
                1,
                0
        );
    }

    private DailyProgress loadProgress(
            UUID uuid
    ) {

        String raw =
                plugin.getStorageValue(
                        getStorageKey(uuid)
                );

        if (raw == null ||
                raw.isBlank()) {

            return new DailyProgress(
                    null,
                    0
            );
        }

        String[] split =
                raw.split(
                        "\\|",
                        2
                );

        if (split.length != 2) {

            return new DailyProgress(
                    null,
                    0
            );
        }

        try {

            LocalDate date =
                    LocalDate.parse(
                            split[0]
                    );

            int position =
                    Integer.parseInt(
                            split[1]
                    );

            return new DailyProgress(
                    date,
                    position
            );

        } catch (
                DateTimeParseException |
                NumberFormatException exception
        ) {

            plugin.getLogger().warning(
                    "Invalid daily reward data for player UUID " +
                            uuid +
                            ". The player's streak will reset."
            );

            return new DailyProgress(
                    null,
                    0
            );
        }
    }

    private String getStorageKey(
            UUID uuid
    ) {

        return "daily." +
                uuid +
                ".state";
    }

    private int getGuiSize(
            FileConfiguration config
    ) {

        int configured =
                config.getInt(
                        "gui.size",
                        27
                );

        configured =
                Math.max(
                        9,
                        Math.min(
                                54,
                                configured
                        )
                );

        int remainder =
                configured % 9;

        if (remainder != 0) {

            configured +=
                    9 - remainder;

            if (configured > 54) {
                configured = 54;
            }
        }

        return configured;
    }

    public void clearClaimLock(
            UUID uuid
    ) {

        if (uuid != null) {
            claimInProgress.remove(uuid);
        }
    }

    public FileConfiguration getDailyConfig() {
        return dailyConfig;
    }

    public File getDailyFile() {
        return dailyFile;
    }

    public ZoneId getZoneId() {
        return zoneId;
    }

    public List<Integer> getDayKeys() {
        return dayKeys;
    }

    private enum RewardState {
        AVAILABLE,
        CLAIMED,
        LOCKED
    }

    private record DailyProgress(
            LocalDate lastClaimDate,
            int streakPosition
    ) {
    }

    private record DailyStatus(
            boolean alreadyClaimedToday,
            int expectedPosition,
            int claimedThroughPosition
    ) {
    }
}