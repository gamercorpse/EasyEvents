package com.gamercorpse.easyevents.milestones;

import com.gamercorpse.easyevents.EasyEvents;
import com.gamercorpse.easyevents.utils.ColorUtil;
import io.papermc.paper.threadedregions.scheduler.ScheduledTask;
import org.bukkit.Bukkit;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class PlayerMilestoneManager {

    private static final long TICKS_PER_SECOND =
            20L;

    private final EasyEvents plugin;

    private final Map<String, Milestone> milestones =
            new LinkedHashMap<>();

    private final Map<String, Boolean> armedStates =
            new HashMap<>();

    private File milestoneFile;
    private FileConfiguration milestoneConfig;

    private ScheduledTask milestoneTask;

    private long checkIntervalSeconds = 5L;

    public PlayerMilestoneManager(
            EasyEvents plugin
    ) {

        this.plugin = plugin;
    }

    public void start() {

        shutdown();

        loadConfiguration();

        if (milestones.isEmpty()) {

            plugin.getLogger().warning(
                    "Player Milestones has no milestones configured."
            );

            return;
        }

        long intervalTicks =
                Math.max(
                        TICKS_PER_SECOND,
                        checkIntervalSeconds *
                                TICKS_PER_SECOND
                );

        milestoneTask =
                plugin.getServer()
                        .getGlobalRegionScheduler()
                        .runAtFixedRate(
                                plugin,
                                task -> checkMilestones(),
                                intervalTicks,
                                intervalTicks
                        );

        plugin.getLogger().info(
                "Player Milestones scheduler started."
        );
    }

    public void reload() {
        start();
    }

    public void shutdown() {

        if (milestoneTask != null) {

            milestoneTask.cancel();
            milestoneTask = null;
        }

        milestones.clear();
        armedStates.clear();
    }

    private void loadConfiguration() {

        if (!plugin.getDataFolder().exists()) {
            plugin.getDataFolder().mkdirs();
        }

        milestoneFile =
                new File(
                        plugin.getDataFolder(),
                        "milestones.yml"
                );

        if (!milestoneFile.exists()) {

            try {

                plugin.saveResource(
                        "milestones.yml",
                        false
                );

            } catch (IllegalArgumentException exception) {

                plugin.getLogger().severe(
                        "milestones.yml was not found inside the plugin JAR."
                );

                return;
            }
        }

        milestoneConfig =
                YamlConfiguration.loadConfiguration(
                        milestoneFile
                );

        checkIntervalSeconds =
                Math.max(
                        1L,
                        milestoneConfig.getLong(
                                "settings.check-interval-seconds",
                                5L
                        )
                );

        milestones.clear();
        armedStates.clear();

        ConfigurationSection section =
                milestoneConfig.getConfigurationSection(
                        "milestones"
                );

        if (section == null) {

            plugin.getLogger().warning(
                    "No milestones section was found in milestones.yml."
            );

            return;
        }

        List<Milestone> loaded =
                new ArrayList<>();

        for (String name :
                section.getKeys(false)) {

            ConfigurationSection milestoneSection =
                    section.getConfigurationSection(
                            name
                    );

            if (milestoneSection == null) {
                continue;
            }

            boolean enabled =
                    milestoneSection.getBoolean(
                            "enabled",
                            true
                    );

            int players =
                    milestoneSection.getInt(
                            "players",
                            0
                    );

            if (players < 1) {

                plugin.getLogger().warning(
                        "Player milestone '" +
                                name +
                                "' has an invalid player count."
                );

                continue;
            }

            List<String> announcements =
                    List.copyOf(
                            milestoneSection.getStringList(
                                    "announcements"
                            )
                    );

            List<String> commands =
                    List.copyOf(
                            milestoneSection.getStringList(
                                    "commands"
                            )
                    );

            loaded.add(
                    new Milestone(
                            name,
                            enabled,
                            players,
                            announcements,
                            commands
                    )
            );
        }

        loaded.sort(
                Comparator.comparingInt(
                        Milestone::players
                )
        );

        for (Milestone milestone :
                loaded) {

            milestones.put(
                    milestone.name(),
                    milestone
            );

            armedStates.put(
                    milestone.name(),
                    true
            );
        }

        plugin.getLogger().info(
                "Loaded " +
                        milestones.size() +
                        " player milestone(s)."
        );
    }

    private void checkMilestones() {

        int onlinePlayers =
                Bukkit.getOnlinePlayers()
                        .size();

        for (Milestone milestone :
                milestones.values()) {

            if (!milestone.enabled()) {
                continue;
            }

            boolean armed =
                    armedStates.getOrDefault(
                            milestone.name(),
                            true
                    );

            if (onlinePlayers <
                    milestone.players()) {

                /*
                 * The count has dropped below this threshold.
                 * Arm it so it may trigger again next time
                 * the server reaches the milestone.
                 */
                armedStates.put(
                        milestone.name(),
                        true
                );

                continue;
            }

            if (!armed) {
                continue;
            }

            triggerMilestone(
                    milestone,
                    onlinePlayers
            );

            armedStates.put(
                    milestone.name(),
                    false
            );
        }
    }

    private void triggerMilestone(
            Milestone milestone,
            int onlinePlayers
    ) {

        for (String announcement :
                milestone.announcements()) {

            if (announcement == null ||
                    announcement.isBlank()) {

                continue;
            }

            String message =
                    replacePlaceholders(
                            announcement,
                            milestone,
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
                milestone.commands()) {

            if (configuredCommand == null ||
                    configuredCommand.isBlank()) {

                continue;
            }

            String command =
                    replacePlaceholders(
                            configuredCommand,
                            milestone,
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
                        "Could not execute command for player milestone '" +
                                milestone.name() +
                                "': " +
                                command
                );

                exception.printStackTrace();
            }
        }
    }

    private String replacePlaceholders(
            String value,
            Milestone milestone,
            int onlinePlayers
    ) {

        return value
                .replace(
                        "%milestone%",
                        milestone.name()
                )
                .replace(
                        "%required_players%",
                        String.valueOf(
                                milestone.players()
                        )
                )
                .replace(
                        "%online_players%",
                        String.valueOf(
                                onlinePlayers
                        )
                );
    }

    public boolean isRunning() {
        return milestoneTask != null;
    }

    public int getMilestoneCount() {
        return milestones.size();
    }

    public long getCheckIntervalSeconds() {
        return checkIntervalSeconds;
    }

    public File getMilestoneFile() {
        return milestoneFile;
    }

    public FileConfiguration getMilestoneConfig() {
        return milestoneConfig;
    }

    private record Milestone(
            String name,
            boolean enabled,
            int players,
            List<String> announcements,
            List<String> commands
    ) {
    }
}