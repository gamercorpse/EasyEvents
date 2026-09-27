package com.gamercorpse.easyevents.listeners;

import com.gamercorpse.easyevents.EasyEvents;
import com.gamercorpse.easyevents.utils.ColorUtil;
import org.bukkit.Bukkit;
import org.bukkit.NamespacedKey;
import org.bukkit.advancement.Advancement;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerAdvancementDoneEvent;

import java.io.File;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class AdvancementEventListener implements Listener {

    private final EasyEvents plugin;

    private final Map<String, AdvancementEvent> events =
            new LinkedHashMap<>();

    private File advancementEventsFile;
    private FileConfiguration advancementEventsConfig;

    private boolean running;

    public AdvancementEventListener(
            EasyEvents plugin
    ) {

        this.plugin = plugin;
    }

    public void start() {

        loadConfiguration();

        running = true;

        plugin.getLogger().info(
                "Advancement Events listener started."
        );
    }

    public void reload() {

        loadConfiguration();

        running = true;
    }

    public void shutdown() {

        running = false;

        events.clear();
    }

    private void loadConfiguration() {

        if (!plugin.getDataFolder().exists()) {
            plugin.getDataFolder().mkdirs();
        }

        advancementEventsFile =
                new File(
                        plugin.getDataFolder(),
                        "advancement-events.yml"
                );

        if (!advancementEventsFile.exists()) {

            try {

                plugin.saveResource(
                        "advancement-events.yml",
                        false
                );

            } catch (IllegalArgumentException exception) {

                plugin.getLogger().severe(
                        "advancement-events.yml was not found inside the plugin JAR."
                );

                advancementEventsConfig = null;
                events.clear();

                return;
            }
        }

        advancementEventsConfig =
                YamlConfiguration.loadConfiguration(
                        advancementEventsFile
                );

        events.clear();

        ConfigurationSection eventsSection =
                advancementEventsConfig.getConfigurationSection(
                        "events"
                );

        if (eventsSection == null) {

            plugin.getLogger().warning(
                    "No events section was found in advancement-events.yml."
            );

            return;
        }

        for (String eventName :
                eventsSection.getKeys(false)) {

            ConfigurationSection section =
                    eventsSection.getConfigurationSection(
                            eventName
                    );

            if (section == null) {
                continue;
            }

            boolean enabled =
                    section.getBoolean(
                            "enabled",
                            true
                    );

            String advancement =
                    section.getString(
                            "advancement",
                            ""
                    );

            if (advancement == null ||
                    advancement.isBlank()) {

                plugin.getLogger().warning(
                        "Advancement event '" +
                                eventName +
                                "' does not define an advancement."
                );

                continue;
            }

            String normalizedAdvancement =
                    normalizeAdvancementKey(
                            advancement
                    );

            List<String> playerMessages =
                    List.copyOf(
                            section.getStringList(
                                    "player-messages"
                            )
                    );

            List<String> announcements =
                    List.copyOf(
                            section.getStringList(
                                    "announcements"
                            )
                    );

            List<String> commands =
                    List.copyOf(
                            section.getStringList(
                                    "commands"
                            )
                    );

            AdvancementEvent advancementEvent =
                    new AdvancementEvent(
                            eventName,
                            enabled,
                            normalizedAdvancement,
                            playerMessages,
                            announcements,
                            commands
                    );

            events.put(
                    normalizedAdvancement,
                    advancementEvent
            );
        }

        plugin.getLogger().info(
                "Loaded " +
                        events.size() +
                        " advancement event(s)."
        );
    }

    @EventHandler
    public void onAdvancementDone(
            PlayerAdvancementDoneEvent event
    ) {

        if (!running ||
                advancementEventsConfig == null) {

            return;
        }

        Advancement advancement =
                event.getAdvancement();

        NamespacedKey key =
                advancement.getKey();

        String advancementKey =
                key.getNamespace() +
                        ":" +
                        key.getKey();

        AdvancementEvent configuredEvent =
                events.get(
                        normalizeAdvancementKey(
                                advancementKey
                        )
                );

        if (configuredEvent == null ||
                !configuredEvent.enabled()) {

            return;
        }

        Player player =
                event.getPlayer();

        for (String configuredMessage :
                configuredEvent.playerMessages()) {

            if (configuredMessage == null ||
                    configuredMessage.isBlank()) {

                continue;
            }

            player.sendMessage(
                    ColorUtil.colorize(
                            replacePlaceholders(
                                    configuredMessage,
                                    player,
                                    configuredEvent,
                                    advancementKey
                            )
                    )
            );
        }

        if (configuredEvent.announcements().isEmpty() &&
                configuredEvent.commands().isEmpty()) {

            return;
        }

        plugin.getServer()
                .getGlobalRegionScheduler()
                .execute(
                        plugin,
                        () -> {

                            for (String configuredAnnouncement :
                                    configuredEvent.announcements()) {

                                if (configuredAnnouncement == null ||
                                        configuredAnnouncement.isBlank()) {

                                    continue;
                                }

                                Bukkit.broadcast(
                                        ColorUtil.colorize(
                                                replacePlaceholders(
                                                        configuredAnnouncement,
                                                        player,
                                                        configuredEvent,
                                                        advancementKey
                                                )
                                        )
                                );
                            }

                            for (String configuredCommand :
                                    configuredEvent.commands()) {

                                executeConsoleCommand(
                                        configuredCommand,
                                        player,
                                        configuredEvent,
                                        advancementKey
                                );
                            }
                        }
                );
    }

    private void executeConsoleCommand(
            String configuredCommand,
            Player player,
            AdvancementEvent configuredEvent,
            String advancementKey
    ) {

        if (configuredCommand == null ||
                configuredCommand.isBlank()) {

            return;
        }

        String command =
                replacePlaceholders(
                        configuredCommand,
                        player,
                        configuredEvent,
                        advancementKey
                ).trim();

        while (command.startsWith("/")) {

            command =
                    command.substring(1);
        }

        if (command.isBlank()) {
            return;
        }

        try {

            Bukkit.dispatchCommand(
                    Bukkit.getConsoleSender(),
                    command
            );

        } catch (Exception exception) {

            plugin.getLogger().severe(
                    "Could not execute command for advancement event '" +
                            configuredEvent.name() +
                            "': " +
                            command
            );

            exception.printStackTrace();
        }
    }

    private String replacePlaceholders(
            String value,
            Player player,
            AdvancementEvent configuredEvent,
            String advancementKey
    ) {

        return value
                .replace(
                        "%player%",
                        player.getName()
                )
                .replace(
                        "%uuid%",
                        player.getUniqueId()
                                .toString()
                )
                .replace(
                        "%world%",
                        player.getWorld()
                                .getName()
                )
                .replace(
                        "%event%",
                        configuredEvent.name()
                )
                .replace(
                        "%advancement%",
                        advancementKey
                )
                .replace(
                        "%online_players%",
                        String.valueOf(
                                Bukkit.getOnlinePlayers()
                                        .size()
                        )
                );
    }

    private String normalizeAdvancementKey(
            String value
    ) {

        String normalized =
                value.trim()
                        .toLowerCase();

        if (!normalized.contains(":")) {

            normalized =
                    "minecraft:" +
                            normalized;
        }

        return normalized;
    }

    public boolean isRunning() {
        return running;
    }

    public int getEventCount() {
        return events.size();
    }

    public File getAdvancementEventsFile() {
        return advancementEventsFile;
    }

    public FileConfiguration getAdvancementEventsConfig() {
        return advancementEventsConfig;
    }

    private record AdvancementEvent(
            String name,
            boolean enabled,
            String advancement,
            List<String> playerMessages,
            List<String> announcements,
            List<String> commands
    ) {
    }
}