package com.gamercorpse.easyevents;

import com.gamercorpse.easyevents.autobroadcast.AutoBroadcastManager;
import com.gamercorpse.easyevents.calendar.CalendarManager;
import com.gamercorpse.easyevents.commands.DailyCommand;
import com.gamercorpse.easyevents.commands.EasyEventsCommand;
import com.gamercorpse.easyevents.daily.DailyManager;
import com.gamercorpse.easyevents.listeners.AdvancementEventListener;
import com.gamercorpse.easyevents.listeners.DailyMenuListener;
import com.gamercorpse.easyevents.listeners.DeathEventListener;
import com.gamercorpse.easyevents.listeners.JoinEventListener;
import com.gamercorpse.easyevents.milestones.PlayerMilestoneManager;
import com.gamercorpse.easyevents.modules.ModuleManager;
import com.gamercorpse.easyevents.randomevents.RandomEventManager;
import com.gamercorpse.easyevents.storage.MySQLStorage;
import com.gamercorpse.easyevents.storage.YmlStorage;
import org.bstats.bukkit.Metrics;
import org.bukkit.command.PluginCommand;
import org.bukkit.plugin.java.JavaPlugin;

public final class EasyEvents extends JavaPlugin {

    private CalendarManager calendarManager;
    private DailyManager dailyManager;
    private ModuleManager moduleManager;

    private AutoBroadcastManager autoBroadcastManager;
    private PlayerMilestoneManager playerMilestoneManager;
    private RandomEventManager randomEventManager;

    private JoinEventListener joinEventListener;
    private DeathEventListener deathEventListener;
    private AdvancementEventListener advancementEventListener;

    private YmlStorage ymlStorage;
    private MySQLStorage mySQLStorage;

    private String activeStorageType = "yml";

    @Override
    public void onEnable() {

        saveDefaultConfig();

        int pluginId = 32800;
        new Metrics(this, pluginId);

        initializeStorage();

        moduleManager =
                new ModuleManager(this);

        moduleManager.initialize();

        calendarManager =
                new CalendarManager(this);

        dailyManager =
                new DailyManager(this);

        autoBroadcastManager =
                new AutoBroadcastManager(this);

        playerMilestoneManager =
                new PlayerMilestoneManager(this);

        randomEventManager =
                new RandomEventManager(this);

        joinEventListener =
                new JoinEventListener(this);

        deathEventListener =
                new DeathEventListener(this);

        advancementEventListener =
                new AdvancementEventListener(this);

        getServer()
                .getPluginManager()
                .registerEvents(
                        new DailyMenuListener(this),
                        this
                );

        getServer()
                .getPluginManager()
                .registerEvents(
                        joinEventListener,
                        this
                );

        getServer()
                .getPluginManager()
                .registerEvents(
                        deathEventListener,
                        this
                );

        getServer()
                .getPluginManager()
                .registerEvents(
                        advancementEventListener,
                        this
                );

        applyModuleStates(true);

        registerCommands();

        getLogger().info(
                "EasyEvents has been enabled."
        );

        getLogger().info(
                "Active storage type: " +
                        activeStorageType.toUpperCase()
        );
    }

    @Override
    public void onDisable() {

        if (advancementEventListener != null) {
            advancementEventListener.shutdown();
        }

        if (deathEventListener != null) {
            deathEventListener.shutdown();
        }

        if (joinEventListener != null) {
            joinEventListener.shutdown();
        }

        if (randomEventManager != null) {
            randomEventManager.shutdown();
        }

        if (playerMilestoneManager != null) {
            playerMilestoneManager.shutdown();
        }

        if (autoBroadcastManager != null) {
            autoBroadcastManager.shutdown();
        }

        if (dailyManager != null) {
            dailyManager.shutdown();
        }

        if (calendarManager != null) {
            calendarManager.shutdown();
        }

        closeStorage();

        getLogger().info(
                "EasyEvents has been disabled."
        );
    }

    private void registerCommands() {

        PluginCommand easyEventsCommand =
                getCommand("easyevents");

        if (easyEventsCommand == null) {

            getLogger().severe(
                    "Command 'easyevents' is missing from plugin.yml."
            );

        } else {

            EasyEventsCommand command =
                    new EasyEventsCommand(this);

            easyEventsCommand.setExecutor(command);
            easyEventsCommand.setTabCompleter(command);
        }

        PluginCommand dailyCommand =
                getCommand("daily");

        if (dailyCommand == null) {

            getLogger().severe(
                    "Command 'daily' is missing from plugin.yml."
            );

        } else {

            DailyCommand command =
                    new DailyCommand(this);

            dailyCommand.setExecutor(command);
            dailyCommand.setTabCompleter(command);
        }
    }

    private void applyModuleStates(
            boolean initialLoad
    ) {

        applyCalendarModuleState(
                initialLoad
        );

        applyDailyModuleState(
                initialLoad
        );

        applyAutoBroadcastModuleState(
                initialLoad
        );

        applyPlayerMilestoneModuleState(
                initialLoad
        );

        applyRandomEventModuleState(
                initialLoad
        );

        applyJoinEventModuleState(
                initialLoad
        );

        applyDeathEventModuleState(
                initialLoad
        );

        applyAdvancementEventModuleState(
                initialLoad
        );
    }

    private void applyCalendarModuleState(
            boolean initialLoad
    ) {

        if (calendarManager == null) {
            return;
        }

        if (isModuleEnabled(
                ModuleManager.CALENDAR
        )) {

            if (!calendarManager.isRunning()) {

                calendarManager.start();

                getLogger().info(
                        "Calendar module enabled."
                );

            } else if (!initialLoad) {

                calendarManager.reload();

                getLogger().info(
                        "Calendar module reloaded."
                );
            }

        } else {

            if (calendarManager.isRunning()) {
                calendarManager.shutdown();
            }

            getLogger().info(
                    "Calendar module is disabled."
            );
        }
    }

    private void applyDailyModuleState(
            boolean initialLoad
    ) {

        if (dailyManager == null) {
            return;
        }

        if (isModuleEnabled(
                ModuleManager.DAILY_LOGIN
        )) {

            if (initialLoad) {
                dailyManager.initialize();
            } else {
                dailyManager.reload();
            }

            getLogger().info(
                    "Daily Login module enabled."
            );

        } else {

            dailyManager.shutdown();

            getLogger().info(
                    "Daily Login module is disabled."
            );
        }
    }

    private void applyAutoBroadcastModuleState(
            boolean initialLoad
    ) {

        if (autoBroadcastManager == null) {
            return;
        }

        if (isModuleEnabled(
                ModuleManager.AUTO_BROADCAST
        )) {

            if (!autoBroadcastManager.isRunning()) {

                autoBroadcastManager.start();

            } else if (!initialLoad) {

                autoBroadcastManager.reload();
            }

            getLogger().info(
                    "Auto Broadcast module enabled."
            );

        } else {

            autoBroadcastManager.shutdown();

            getLogger().info(
                    "Auto Broadcast module is disabled."
            );
        }
    }

    private void applyPlayerMilestoneModuleState(
            boolean initialLoad
    ) {

        if (playerMilestoneManager == null) {
            return;
        }

        if (isModuleEnabled(
                ModuleManager.PLAYER_MILESTONES
        )) {

            if (!playerMilestoneManager.isRunning()) {

                playerMilestoneManager.start();

            } else if (!initialLoad) {

                playerMilestoneManager.reload();
            }

            getLogger().info(
                    "Player Milestones module enabled."
            );

        } else {

            playerMilestoneManager.shutdown();

            getLogger().info(
                    "Player Milestones module is disabled."
            );
        }
    }

    private void applyRandomEventModuleState(
            boolean initialLoad
    ) {

        if (randomEventManager == null) {
            return;
        }

        if (isModuleEnabled(
                ModuleManager.RANDOM_EVENTS
        )) {

            if (!randomEventManager.isRunning()) {

                randomEventManager.start();

            } else if (!initialLoad) {

                randomEventManager.reload();
            }

            getLogger().info(
                    "Random Events module enabled."
            );

        } else {

            randomEventManager.shutdown();

            getLogger().info(
                    "Random Events module is disabled."
            );
        }
    }

    private void applyJoinEventModuleState(
            boolean initialLoad
    ) {

        if (joinEventListener == null) {
            return;
        }

        if (isModuleEnabled(
                ModuleManager.JOIN_EVENTS
        )) {

            if (!joinEventListener.isRunning()) {

                joinEventListener.start();

            } else if (!initialLoad) {

                joinEventListener.reload();
            }

            getLogger().info(
                    "Join Events module enabled."
            );

        } else {

            joinEventListener.shutdown();

            getLogger().info(
                    "Join Events module is disabled."
            );
        }
    }

    private void applyDeathEventModuleState(
            boolean initialLoad
    ) {

        if (deathEventListener == null) {
            return;
        }

        if (isModuleEnabled(
                ModuleManager.DEATH_EVENTS
        )) {

            if (!deathEventListener.isRunning()) {

                deathEventListener.start();

            } else if (!initialLoad) {

                deathEventListener.reload();
            }

            getLogger().info(
                    "Death Events module enabled."
            );

        } else {

            deathEventListener.shutdown();

            getLogger().info(
                    "Death Events module is disabled."
            );
        }
    }

    private void applyAdvancementEventModuleState(
            boolean initialLoad
    ) {

        if (advancementEventListener == null) {
            return;
        }

        if (isModuleEnabled(
                ModuleManager.ADVANCEMENT_EVENTS
        )) {

            if (!advancementEventListener.isRunning()) {

                advancementEventListener.start();

            } else if (!initialLoad) {

                advancementEventListener.reload();
            }

            getLogger().info(
                    "Advancement Events module enabled."
            );

        } else {

            advancementEventListener.shutdown();

            getLogger().info(
                    "Advancement Events module is disabled."
            );
        }
    }

    private void initializeStorage() {

        closeStorage();

        String configuredStorageType =
                getConfig().getString(
                        "storage.type",
                        "yml"
                );

        if (configuredStorageType == null) {
            configuredStorageType = "yml";
        }

        configuredStorageType =
                configuredStorageType
                        .trim()
                        .toLowerCase();

        switch (configuredStorageType) {

            case "mysql":

                getLogger().info(
                        "MySQL storage has been selected."
                );

                mySQLStorage =
                        new MySQLStorage(this);

                if (mySQLStorage.connect()) {

                    activeStorageType = "mysql";

                    getLogger().info(
                            "Successfully connected to MySQL storage."
                    );

                } else {

                    getLogger().severe(
                            "EasyEvents could not connect to MySQL."
                    );

                    getLogger().warning(
                            "Falling back to YAML storage."
                    );

                    startYamlStorage();
                }

                break;

            case "yml":
            case "yaml":

                startYamlStorage();
                break;

            default:

                getLogger().warning(
                        "Unknown storage type '" +
                                configuredStorageType +
                                "'."
                );

                getLogger().warning(
                        "Valid storage types are 'yml' and 'mysql'."
                );

                getLogger().warning(
                        "Using YAML storage."
                );

                startYamlStorage();

                break;
        }
    }

    private void startYamlStorage() {

        ymlStorage =
                new YmlStorage(this);

        ymlStorage.initialize();

        activeStorageType = "yml";

        getLogger().info(
                "YAML storage initialized."
        );
    }

    private void closeStorage() {

        if (ymlStorage != null) {

            ymlStorage.save();
            ymlStorage = null;
        }

        if (mySQLStorage != null) {

            mySQLStorage.close();
            mySQLStorage = null;
        }
    }

    public void reloadPlugin() {

        getLogger().info(
                "Reloading EasyEvents configuration."
        );

        reloadConfig();

        initializeStorage();

        if (moduleManager != null) {
            moduleManager.reload();
        }

        applyModuleStates(false);

        getLogger().info(
                "EasyEvents configuration reloaded."
        );

        getLogger().info(
                "Active storage type: " +
                        activeStorageType.toUpperCase()
        );
    }

    public boolean isModuleEnabled(
            String moduleName
    ) {

        if (moduleManager == null) {
            return false;
        }

        return moduleManager.isEnabled(
                moduleName
        );
    }

    public boolean isModuleDisabled(
            String moduleName
    ) {

        return !isModuleEnabled(
                moduleName
        );
    }

    public void setStorageValue(
            String key,
            String value
    ) {

        if (key == null ||
                key.isBlank()) {

            return;
        }

        if (isMySQLStorage()) {

            if (mySQLStorage != null) {

                mySQLStorage.set(
                        key,
                        value
                );
            }

            return;
        }

        if (ymlStorage != null) {

            ymlStorage.set(
                    key,
                    value
            );
        }
    }

    public String getStorageValue(
            String key
    ) {

        return getStorageValue(
                key,
                null
        );
    }

    public String getStorageValue(
            String key,
            String defaultValue
    ) {

        if (key == null ||
                key.isBlank()) {

            return defaultValue;
        }

        String value;

        if (isMySQLStorage()) {

            if (mySQLStorage == null) {
                return defaultValue;
            }

            value =
                    mySQLStorage.get(key);

        } else {

            if (ymlStorage == null) {
                return defaultValue;
            }

            value =
                    ymlStorage.get(key);
        }

        return value != null
                ? value
                : defaultValue;
    }

    public boolean containsStorageKey(
            String key
    ) {

        if (key == null ||
                key.isBlank()) {

            return false;
        }

        if (isMySQLStorage()) {

            return mySQLStorage != null &&
                    mySQLStorage.contains(key);
        }

        return ymlStorage != null &&
                ymlStorage.contains(key);
    }

    public void removeStorageValue(
            String key
    ) {

        if (key == null ||
                key.isBlank()) {

            return;
        }

        if (isMySQLStorage()) {

            if (mySQLStorage != null) {
                mySQLStorage.remove(key);
            }

            return;
        }

        if (ymlStorage != null) {
            ymlStorage.remove(key);
        }
    }

    public boolean isMySQLStorage() {

        return activeStorageType.equalsIgnoreCase(
                "mysql"
        );
    }

    public boolean isYamlStorage() {

        return activeStorageType.equalsIgnoreCase(
                "yml"
        );
    }

    public CalendarManager getCalendarManager() {
        return calendarManager;
    }

    public DailyManager getDailyManager() {
        return dailyManager;
    }

    public ModuleManager getModuleManager() {
        return moduleManager;
    }

    public AutoBroadcastManager getAutoBroadcastManager() {
        return autoBroadcastManager;
    }

    public PlayerMilestoneManager getPlayerMilestoneManager() {
        return playerMilestoneManager;
    }

    public RandomEventManager getRandomEventManager() {
        return randomEventManager;
    }

    public JoinEventListener getJoinEventListener() {
        return joinEventListener;
    }

    public DeathEventListener getDeathEventListener() {
        return deathEventListener;
    }

    public AdvancementEventListener getAdvancementEventListener() {
        return advancementEventListener;
    }

    public YmlStorage getYmlStorage() {
        return ymlStorage;
    }

    public MySQLStorage getMySQLStorage() {
        return mySQLStorage;
    }

    public String getActiveStorageType() {
        return activeStorageType;
    }
}