package com.gamercorpse.easyevents.calendar;

import com.gamercorpse.easyevents.EasyEvents;
import com.gamercorpse.easyevents.utils.ColorUtil;
import io.papermc.paper.threadedregions.scheduler.ScheduledTask;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.time.DateTimeException;
import java.time.LocalDate;
import java.time.MonthDay;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class CalendarManager {

    private static final long TICKS_PER_SECOND = 20L;

    private final EasyEvents plugin;
    private final Map<String, CalendarEvent> events;

    private File calendarFile;
    private FileConfiguration calendarConfig;

    private ScheduledTask calendarTask;

    private ZoneId zoneId;

    public CalendarManager(EasyEvents plugin) {

        this.plugin = plugin;

        this.events =
                new LinkedHashMap<>();

        this.zoneId =
                ZoneId.systemDefault();
    }

    public void start() {

        /*
         * Prevent duplicate schedulers if start() is ever
         * called while already running.
         */
        if (calendarTask != null) {
            calendarTask.cancel();
            calendarTask = null;
        }

        loadCalendar();

        calendarTask =
                plugin.getServer()
                        .getGlobalRegionScheduler()
                        .runAtFixedRate(
                                plugin,
                                task -> checkCalendar(),
                                TICKS_PER_SECOND,
                                TICKS_PER_SECOND
                        );

        plugin.getLogger().info(
                "Calendar scheduler started."
        );
    }

    public void shutdown() {

        if (calendarTask != null) {

            calendarTask.cancel();
            calendarTask = null;

            plugin.getLogger().info(
                    "Calendar scheduler stopped."
            );
        }

        events.clear();
    }

    public void reload() {

        if (!isRunning()) {

            start();
            return;
        }

        loadCalendar();
    }

    public boolean isRunning() {
        return calendarTask != null;
    }

    public void loadCalendar() {

        if (!plugin.getDataFolder().exists()) {
            plugin.getDataFolder().mkdirs();
        }

        calendarFile =
                new File(
                        plugin.getDataFolder(),
                        "calendar.yml"
                );

        if (!calendarFile.exists()) {

            try {

                plugin.saveResource(
                        "calendar.yml",
                        false
                );

            } catch (IllegalArgumentException exception) {

                plugin.getLogger().severe(
                        "calendar.yml was not found inside the plugin JAR."
                );

                return;
            }
        }

        calendarConfig =
                YamlConfiguration.loadConfiguration(
                        calendarFile
                );

        loadTimeZone();

        events.clear();

        ConfigurationSection eventsSection =
                calendarConfig.getConfigurationSection(
                        "events"
                );

        if (eventsSection == null) {

            plugin.getLogger().warning(
                    "No events section was found in calendar.yml."
            );

            return;
        }

        int loaded = 0;

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

            String date =
                    section.getString(
                            "date",
                            ""
                    );

            long frequencySeconds =
                    section.getLong(
                            "frequency-seconds",
                            300L
                    );

            List<String> announcements =
                    section.getStringList(
                            "announcements"
                    );

            if (date == null ||
                    date.isBlank()) {

                plugin.getLogger().warning(
                        "Calendar event '" +
                                eventName +
                                "' does not have a valid date."
                );

                continue;
            }

            if (!isValidDate(date)) {

                plugin.getLogger().warning(
                        "Calendar event '" +
                                eventName +
                                "' has an invalid date: " +
                                date
                );

                plugin.getLogger().warning(
                        "Use MM-dd for recurring dates " +
                                "or yyyy-MM-dd for exact dates."
                );

                continue;
            }

            if (frequencySeconds < 1L) {

                plugin.getLogger().warning(
                        "Calendar event '" +
                                eventName +
                                "' has a broadcast frequency below 1 second."
                );

                plugin.getLogger().warning(
                        "The frequency has been changed to 1 second."
                );

                frequencySeconds = 1L;
            }

            if (announcements.isEmpty()) {

                plugin.getLogger().warning(
                        "Calendar event '" +
                                eventName +
                                "' has no announcements."
                );
            }

            CalendarEvent event =
                    new CalendarEvent(
                            eventName,
                            enabled,
                            date,
                            frequencySeconds,
                            announcements
                    );

            events.put(
                    eventName.toLowerCase(),
                    event
            );

            loaded++;
        }

        plugin.getLogger().info(
                "Loaded " +
                        loaded +
                        " calendar event(s)."
        );

        plugin.getLogger().info(
                "Calendar timezone: " +
                        zoneId.getId()
        );
    }

    private void loadTimeZone() {

        String configuredTimeZone =
                calendarConfig.getString(
                        "settings.timezone",
                        "SYSTEM"
                );

        if (configuredTimeZone == null ||
                configuredTimeZone.isBlank() ||
                configuredTimeZone.equalsIgnoreCase(
                        "SYSTEM"
                )) {

            zoneId =
                    ZoneId.systemDefault();

            return;
        }

        try {

            zoneId =
                    ZoneId.of(
                            configuredTimeZone
                    );

        } catch (DateTimeException exception) {

            zoneId =
                    ZoneId.systemDefault();

            plugin.getLogger().warning(
                    "Invalid timezone '" +
                            configuredTimeZone +
                            "' in calendar.yml."
            );

            plugin.getLogger().warning(
                    "Using system timezone instead: " +
                            zoneId.getId()
            );
        }
    }

    private boolean isValidDate(
            String date
    ) {

        if (date.matches(
                "\\d{2}-\\d{2}"
        )) {

            try {

                MonthDay.parse(
                        date,
                        DateTimeFormatter.ofPattern(
                                "MM-dd"
                        )
                );

                return true;

            } catch (DateTimeParseException exception) {

                return false;
            }
        }

        if (date.matches(
                "\\d{4}-\\d{2}-\\d{2}"
        )) {

            try {

                LocalDate.parse(
                        date,
                        DateTimeFormatter.ISO_LOCAL_DATE
                );

                return true;

            } catch (DateTimeParseException exception) {

                return false;
            }
        }

        return false;
    }

    private void checkCalendar() {

        LocalDate currentDate =
                LocalDate.now(
                        zoneId
                );

        long currentTime =
                System.currentTimeMillis();

        for (CalendarEvent event :
                events.values()) {

            try {

                if (!event.isEnabled()) {

                    event.resetIfInactive();
                    continue;
                }

                if (!event.matchesDate(
                        currentDate
                )) {

                    event.resetIfInactive();
                    continue;
                }

                if (!event.hasAnnouncements()) {
                    continue;
                }

                event.activateForDate(
                        currentDate
                );

                if (!event.isBroadcastDue(
                        currentTime
                )) {

                    continue;
                }

                String announcement =
                        event.getNextAnnouncement();

                if (announcement == null ||
                        announcement.isBlank()) {

                    event.scheduleNextBroadcast(
                            currentTime
                    );

                    continue;
                }

                plugin.getServer()
                        .broadcast(
                                ColorUtil.colorize(
                                        announcement
                                )
                        );

                event.scheduleNextBroadcast(
                        currentTime
                );

            } catch (Exception exception) {

                plugin.getLogger().severe(
                        "An error occurred while processing calendar event '" +
                                event.getName() +
                                "'."
                );

                exception.printStackTrace();
            }
        }
    }

    public Map<String, CalendarEvent> getEvents() {

        return Map.copyOf(
                events
        );
    }

    public ZoneId getZoneId() {
        return zoneId;
    }

    public FileConfiguration getCalendarConfig() {
        return calendarConfig;
    }

    public File getCalendarFile() {
        return calendarFile;
    }

    public void saveCalendar() {

        if (calendarConfig == null ||
                calendarFile == null) {

            return;
        }

        try {

            calendarConfig.save(
                    calendarFile
            );

        } catch (IOException exception) {

            plugin.getLogger().severe(
                    "Could not save calendar.yml."
            );

            exception.printStackTrace();
        }
    }
}