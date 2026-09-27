package com.gamercorpse.easyevents.randomevents;

import com.gamercorpse.easyevents.EasyEvents;
import com.gamercorpse.easyevents.utils.ColorUtil;
import io.papermc.paper.threadedregions.scheduler.ScheduledTask;
import org.bukkit.Bukkit;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

public class RandomEventManager {

    private static final long TICKS_PER_SECOND =
            20L;

    private final EasyEvents plugin;

    private File randomEventsFile;
    private FileConfiguration randomEventsConfig;

    private ScheduledTask randomEventTask;

    private List<RandomEvent> events =
            List.of();

    private long checkIntervalSeconds = 300L;
    private int minimumPlayers = 1;

    public RandomEventManager(
            EasyEvents plugin
    ) {

        this.plugin = plugin;
    }

    public void start() {

        shutdown();

        loadConfiguration();

        if (events.isEmpty()) {

            plugin.getLogger().warning(
                    "Random Events has no events configured."
            );

            return;
        }

        long intervalTicks =
                Math.max(
                        TICKS_PER_SECOND,
                        checkIntervalSeconds *
                                TICKS_PER_SECOND
                );

        randomEventTask =
                plugin.getServer()
                        .getGlobalRegionScheduler()
                        .runAtFixedRate(
                                plugin,
                                task -> rollEvents(),
                                intervalTicks,
                                intervalTicks
                        );

        plugin.getLogger().info(
                "Random Events scheduler started."
        );
    }

    public void reload() {
        start();
    }

    public void shutdown() {

        if (randomEventTask != null) {

            randomEventTask.cancel();
            randomEventTask = null;
        }

        events = List.of();
    }

    private void loadConfiguration() {

        if (!plugin.getDataFolder().exists()) {
            plugin.getDataFolder().mkdirs();
        }

        randomEventsFile =
                new File(
                        plugin.getDataFolder(),
                        "random-events.yml"
                );

        if (!randomEventsFile.exists()) {

            try {

                plugin.saveResource(
                        "random-events.yml",
                        false
                );

            } catch (IllegalArgumentException exception) {

                plugin.getLogger().severe(
                        "random-events.yml was not found inside the plugin JAR."
                );

                events = List.of();

                return;
            }
        }

        randomEventsConfig =
                YamlConfiguration.loadConfiguration(
                        randomEventsFile
                );

        checkIntervalSeconds =
                Math.max(
                        1L,
                        randomEventsConfig.getLong(
                                "settings.check-interval-seconds",
                                300L
                        )
                );

        minimumPlayers =
                Math.max(
                        0,
                        randomEventsConfig.getInt(
                                "settings.minimum-online-players",
                                1
                        )
                );

        List<RandomEvent> loadedEvents =
                new ArrayList<>();

        ConfigurationSection eventsSection =
                randomEventsConfig.getConfigurationSection(
                        "events"
                );

        if (eventsSection == null) {

            plugin.getLogger().warning(
                    "No events section was found in random-events.yml."
            );

            events = List.of();

            return;
        }

        for (String eventName :
                eventsSection.getKeys(false)) {

            ConfigurationSection eventSection =
                    eventsSection.getConfigurationSection(
                            eventName
                    );

            if (eventSection == null) {
                continue;
            }

            boolean enabled =
                    eventSection.getBoolean(
                            "enabled",
                            true
                    );

            double chance =
                    eventSection.getDouble(
                            "chance-percent",
                            10.0D
                    );

            chance =
                    Math.max(
                            0.0D,
                            Math.min(
                                    100.0D,
                                    chance
                            )
                    );

            int eventMinimumPlayers =
                    Math.max(
                            0,
                            eventSection.getInt(
                                    "minimum-online-players",
                                    minimumPlayers
                            )
                    );

            List<String> announcements =
                    List.copyOf(
                            eventSection.getStringList(
                                    "announcements"
                            )
                    );

            List<String> commands =
                    List.copyOf(
                            eventSection.getStringList(
                                    "commands"
                            )
                    );

            loadedEvents.add(
                    new RandomEvent(
                            eventName,
                            enabled,
                            chance,
                            eventMinimumPlayers,
                            announcements,
                            commands
                    )
            );
        }

        events =
                List.copyOf(
                        loadedEvents
                );

        plugin.getLogger().info(
                "Loaded " +
                        events.size() +
                        " random event(s)."
        );
    }

    private void rollEvents() {

        int onlinePlayers =
                Bukkit.getOnlinePlayers()
                        .size();

        if (onlinePlayers <
                minimumPlayers) {

            return;
        }

        /*
         * Each configured event receives its own independent
         * chance roll every check interval.
         *
         * This means multiple events can technically trigger
         * during the same check if their rolls succeed.
         */
        for (RandomEvent event :
                events) {

            if (!event.enabled()) {
                continue;
            }

            if (onlinePlayers <
                    event.minimumPlayers()) {

                continue;
            }

            double roll =
                    ThreadLocalRandom.current()
                            .nextDouble(
                                    0.0D,
                                    100.0D
                            );

            if (roll >=
                    event.chancePercent()) {

                continue;
            }

            triggerEvent(
                    event,
                    onlinePlayers
            );
        }
    }

    private void triggerEvent(
            RandomEvent event,
            int onlinePlayers
    ) {

        for (String announcement :
                event.announcements()) {

            if (announcement == null ||
                    announcement.isBlank()) {

                continue;
            }

            String message =
                    replacePlaceholders(
                            announcement,
                            event,
                            onlinePlayers
                    );

            plugin.getServer()
                    .broadcast(
                            ColorUtil.colorize(
                                    message
                            )
                    );
        }

        for (String configuredCommand :
                event.commands()) {

            if (configuredCommand == null ||
                    configuredCommand.isBlank()) {

                continue;
            }

            String command =
                    replacePlaceholders(
                            configuredCommand,
                            event,
                            onlinePlayers
                    ).trim();

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
                        "Could not execute command for random event '" +
                                event.name() +
                                "': " +
                                command
                );

                exception.printStackTrace();
            }
        }
    }

    private String replacePlaceholders(
            String value,
            RandomEvent event,
            int onlinePlayers
    ) {

        return value
                .replace(
                        "%event%",
                        event.name()
                )
                .replace(
                        "%online_players%",
                        String.valueOf(
                                onlinePlayers
                        )
                );
    }

    public boolean isRunning() {
        return randomEventTask != null;
    }

    public int getEventCount() {
        return events.size();
    }

    public long getCheckIntervalSeconds() {
        return checkIntervalSeconds;
    }

    public File getRandomEventsFile() {
        return randomEventsFile;
    }

    public FileConfiguration getRandomEventsConfig() {
        return randomEventsConfig;
    }

    private record RandomEvent(
            String name,
            boolean enabled,
            double chancePercent,
            int minimumPlayers,
            List<String> announcements,
            List<String> commands
    ) {
    }
}