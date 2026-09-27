package com.gamercorpse.easyevents.listeners;

import com.gamercorpse.easyevents.EasyEvents;
import com.gamercorpse.easyevents.utils.ColorUtil;
import org.bukkit.Bukkit;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;

import java.io.File;
import java.util.List;

public class JoinEventListener implements Listener {

    private final EasyEvents plugin;

    private File joinEventsFile;
    private FileConfiguration joinEventsConfig;

    private boolean running;

    public JoinEventListener(
            EasyEvents plugin
    ) {

        this.plugin = plugin;
    }

    public void start() {

        loadConfiguration();

        running = true;

        plugin.getLogger().info(
                "Join Events listener started."
        );
    }

    public void reload() {

        loadConfiguration();

        running = true;
    }

    public void shutdown() {

        running = false;
    }

    private void loadConfiguration() {

        if (!plugin.getDataFolder().exists()) {
            plugin.getDataFolder().mkdirs();
        }

        joinEventsFile =
                new File(
                        plugin.getDataFolder(),
                        "join-events.yml"
                );

        if (!joinEventsFile.exists()) {

            try {

                plugin.saveResource(
                        "join-events.yml",
                        false
                );

            } catch (IllegalArgumentException exception) {

                plugin.getLogger().severe(
                        "join-events.yml was not found inside the plugin JAR."
                );

                joinEventsConfig = null;

                return;
            }
        }

        joinEventsConfig =
                YamlConfiguration.loadConfiguration(
                        joinEventsFile
                );
    }

    @EventHandler
    public void onPlayerJoin(
            PlayerJoinEvent event
    ) {

        if (!running ||
                joinEventsConfig == null) {

            return;
        }

        Player player =
                event.getPlayer();

        String sectionPath;

        if (!player.hasPlayedBefore()) {

            sectionPath =
                    "first-join";

        } else {

            sectionPath =
                    "returning-player";
        }

        ConfigurationSection section =
                joinEventsConfig.getConfigurationSection(
                        sectionPath
                );

        if (section == null ||
                !section.getBoolean(
                        "enabled",
                        true
                )) {

            return;
        }

        List<String> playerMessages =
                section.getStringList(
                        "player-messages"
                );

        for (String configuredMessage :
                playerMessages) {

            if (configuredMessage == null ||
                    configuredMessage.isBlank()) {

                continue;
            }

            player.sendMessage(
                    ColorUtil.colorize(
                            replacePlaceholders(
                                    configuredMessage,
                                    player
                            )
                    )
            );
        }

        List<String> broadcasts =
                section.getStringList(
                        "broadcasts"
                );

        List<String> commands =
                section.getStringList(
                        "commands"
                );

        if (broadcasts.isEmpty() &&
                commands.isEmpty()) {

            return;
        }

        plugin.getServer()
                .getGlobalRegionScheduler()
                .execute(
                        plugin,
                        () -> {

                            for (String configuredBroadcast :
                                    broadcasts) {

                                if (configuredBroadcast == null ||
                                        configuredBroadcast.isBlank()) {

                                    continue;
                                }

                                Bukkit.broadcast(
                                        ColorUtil.colorize(
                                                replacePlaceholders(
                                                        configuredBroadcast,
                                                        player
                                                )
                                        )
                                );
                            }

                            for (String configuredCommand :
                                    commands) {

                                executeConsoleCommand(
                                        configuredCommand,
                                        player
                                );
                            }
                        }
                );
    }

    private void executeConsoleCommand(
            String configuredCommand,
            Player player
    ) {

        if (configuredCommand == null ||
                configuredCommand.isBlank()) {

            return;
        }

        String command =
                replacePlaceholders(
                        configuredCommand,
                        player
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
                    "Could not execute Join Events command: " +
                            command
            );

            exception.printStackTrace();
        }
    }

    private String replacePlaceholders(
            String value,
            Player player
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
                        "%online_players%",
                        String.valueOf(
                                Bukkit.getOnlinePlayers()
                                        .size()
                        )
                );
    }

    public boolean isRunning() {
        return running;
    }

    public File getJoinEventsFile() {
        return joinEventsFile;
    }

    public FileConfiguration getJoinEventsConfig() {
        return joinEventsConfig;
    }
}