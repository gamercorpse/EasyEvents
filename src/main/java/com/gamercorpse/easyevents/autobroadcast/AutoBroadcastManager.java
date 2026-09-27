package com.gamercorpse.easyevents.autobroadcast;

import com.gamercorpse.easyevents.EasyEvents;
import com.gamercorpse.easyevents.utils.ColorUtil;
import io.papermc.paper.threadedregions.scheduler.ScheduledTask;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class AutoBroadcastManager {

    private static final long TICKS_PER_SECOND =
            20L;

    private final EasyEvents plugin;

    private File broadcastFile;
    private FileConfiguration broadcastConfig;

    private ScheduledTask broadcastTask;

    private List<String> messages =
            List.of();

    private int messageIndex = 0;

    private long intervalSeconds = 300L;

    public AutoBroadcastManager(
            EasyEvents plugin
    ) {

        this.plugin = plugin;
    }

    public void start() {

        shutdown();

        loadConfiguration();

        if (messages.isEmpty()) {

            plugin.getLogger().warning(
                    "Auto Broadcast has no messages configured."
            );

            return;
        }

        long intervalTicks =
                Math.max(
                        TICKS_PER_SECOND,
                        intervalSeconds *
                                TICKS_PER_SECOND
                );

        broadcastTask =
                plugin.getServer()
                        .getGlobalRegionScheduler()
                        .runAtFixedRate(
                                plugin,
                                task -> broadcastNext(),
                                intervalTicks,
                                intervalTicks
                        );

        plugin.getLogger().info(
                "Auto Broadcast scheduler started."
        );
    }

    public void reload() {
        start();
    }

    public void shutdown() {

        if (broadcastTask != null) {

            broadcastTask.cancel();
            broadcastTask = null;
        }

        messageIndex = 0;
    }

    private void loadConfiguration() {

        if (!plugin.getDataFolder().exists()) {
            plugin.getDataFolder().mkdirs();
        }

        broadcastFile =
                new File(
                        plugin.getDataFolder(),
                        "broadcasts.yml"
                );

        if (!broadcastFile.exists()) {

            try {

                plugin.saveResource(
                        "broadcasts.yml",
                        false
                );

            } catch (IllegalArgumentException exception) {

                plugin.getLogger().severe(
                        "broadcasts.yml was not found inside the plugin JAR."
                );

                messages = List.of();

                return;
            }
        }

        broadcastConfig =
                YamlConfiguration.loadConfiguration(
                        broadcastFile
                );

        intervalSeconds =
                Math.max(
                        1L,
                        broadcastConfig.getLong(
                                "settings.interval-seconds",
                                300L
                        )
                );

        List<String> configuredMessages =
                new ArrayList<>(
                        broadcastConfig.getStringList(
                                "messages"
                        )
                );

        configuredMessages.removeIf(
                message ->
                        message == null ||
                                message.isBlank()
        );

        messages =
                List.copyOf(
                        configuredMessages
                );

        messageIndex = 0;

        plugin.getLogger().info(
                "Loaded " +
                        messages.size() +
                        " Auto Broadcast message(s)."
        );
    }

    private void broadcastNext() {

        if (messages.isEmpty()) {
            return;
        }

        if (messageIndex >= messages.size()) {
            messageIndex = 0;
        }

        String message =
                messages.get(
                        messageIndex
                );

        messageIndex++;

        plugin.getServer()
                .broadcast(
                        ColorUtil.colorize(
                                message
                        )
                );
    }

    public boolean isRunning() {
        return broadcastTask != null;
    }

    public int getMessageCount() {
        return messages.size();
    }

    public long getIntervalSeconds() {
        return intervalSeconds;
    }

    public File getBroadcastFile() {
        return broadcastFile;
    }

    public FileConfiguration getBroadcastConfig() {
        return broadcastConfig;
    }
}