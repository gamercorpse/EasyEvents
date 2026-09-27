package com.gamercorpse.easyevents.listeners;

import com.gamercorpse.easyevents.EasyEvents;
import com.gamercorpse.easyevents.utils.ColorUtil;
import org.bukkit.Bukkit;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;

import java.io.File;
import java.util.List;

public class DeathEventListener implements Listener {

    private final EasyEvents plugin;

    private File deathEventsFile;
    private FileConfiguration deathEventsConfig;

    private boolean running;

    public DeathEventListener(
            EasyEvents plugin
    ) {

        this.plugin = plugin;
    }

    public void start() {

        loadConfiguration();

        running = true;

        plugin.getLogger().info(
                "Death Events listener started."
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

        deathEventsFile =
                new File(
                        plugin.getDataFolder(),
                        "death-events.yml"
                );

        if (!deathEventsFile.exists()) {

            try {

                plugin.saveResource(
                        "death-events.yml",
                        false
                );

            } catch (IllegalArgumentException exception) {

                plugin.getLogger().severe(
                        "death-events.yml was not found inside the plugin JAR."
                );

                deathEventsConfig = null;

                return;
            }
        }

        deathEventsConfig =
                YamlConfiguration.loadConfiguration(
                        deathEventsFile
                );
    }

    @EventHandler
    public void onPlayerDeath(
            PlayerDeathEvent event
    ) {

        if (!running ||
                deathEventsConfig == null) {

            return;
        }

        if (!deathEventsConfig.getBoolean(
                "settings.enabled",
                true
        )) {

            return;
        }

        Player player =
                event.getEntity();

        Player killer =
                player.getKiller();

        String killerName =
                killer != null
                        ? killer.getName()
                        : deathEventsConfig.getString(
                        "settings.no-killer-name",
                        "None"
                );

        if (killerName == null) {
            killerName = "None";
        }

        String cause =
                player.getLastDamageCause() != null
                        ? player.getLastDamageCause()
                        .getCause()
                        .name()
                        : "UNKNOWN";

        if (deathEventsConfig.getBoolean(
                "settings.disable-vanilla-death-message",
                false
        )) {

            event.deathMessage(null);
        }

        List<String> announcements =
                deathEventsConfig.getStringList(
                        "announcements"
                );

        List<String> commands =
                deathEventsConfig.getStringList(
                        "commands"
                );

        String finalKillerName =
                killerName;

        plugin.getServer()
                .getGlobalRegionScheduler()
                .execute(
                        plugin,
                        () -> {

                            for (String configuredAnnouncement :
                                    announcements) {

                                if (configuredAnnouncement == null ||
                                        configuredAnnouncement.isBlank()) {

                                    continue;
                                }

                                Bukkit.broadcast(
                                        ColorUtil.colorize(
                                                replacePlaceholders(
                                                        configuredAnnouncement,
                                                        player,
                                                        finalKillerName,
                                                        cause
                                                )
                                        )
                                );
                            }

                            for (String configuredCommand :
                                    commands) {

                                executeConsoleCommand(
                                        configuredCommand,
                                        player,
                                        finalKillerName,
                                        cause
                                );
                            }
                        }
                );
    }

    private void executeConsoleCommand(
            String configuredCommand,
            Player player,
            String killerName,
            String cause
    ) {

        if (configuredCommand == null ||
                configuredCommand.isBlank()) {

            return;
        }

        String command =
                replacePlaceholders(
                        configuredCommand,
                        player,
                        killerName,
                        cause
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
                    "Could not execute Death Events command: " +
                            command
            );

            exception.printStackTrace();
        }
    }

    private String replacePlaceholders(
            String value,
            Player player,
            String killerName,
            String cause
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
                        "%killer%",
                        killerName
                )
                .replace(
                        "%cause%",
                        cause
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

    public File getDeathEventsFile() {
        return deathEventsFile;
    }

    public FileConfiguration getDeathEventsConfig() {
        return deathEventsConfig;
    }
}