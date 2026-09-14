package com.gamercorpse.easyevents.calendar;

import java.time.LocalDate;
import java.time.MonthDay;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class CalendarEvent {

    private final String name;
    private final boolean enabled;
    private final String date;
    private final long frequencySeconds;
    private final List<String> announcements;

    private int announcementIndex;
    private long nextBroadcastTime;
    private LocalDate activeDate;

    public CalendarEvent(
            String name,
            boolean enabled,
            String date,
            long frequencySeconds,
            List<String> announcements
    ) {

        this.name = name;
        this.enabled = enabled;
        this.date = date;
        this.frequencySeconds = frequencySeconds;

        this.announcements =
                announcements == null
                        ? new ArrayList<>()
                        : new ArrayList<>(announcements);

        this.announcementIndex = 0;
        this.nextBroadcastTime = 0L;
        this.activeDate = null;
    }

    public boolean matchesDate(LocalDate currentDate) {

        if (currentDate == null) {
            return false;
        }

        if (date.matches("\\d{2}-\\d{2}")) {

            MonthDay configuredDate =
                    MonthDay.parse(
                            date,
                            DateTimeFormatter.ofPattern("MM-dd")
                    );

            MonthDay today =
                    MonthDay.from(currentDate);

            return configuredDate.equals(today);
        }

        LocalDate configuredDate =
                LocalDate.parse(
                        date,
                        DateTimeFormatter.ISO_LOCAL_DATE
                );

        return configuredDate.equals(currentDate);
    }

    public void activateForDate(LocalDate currentDate) {

        if (currentDate == null) {
            return;
        }

        if (activeDate == null ||
                !activeDate.equals(currentDate)) {

            activeDate = currentDate;
            announcementIndex = 0;
            nextBroadcastTime = 0L;
        }
    }

    public void resetIfInactive() {

        if (activeDate != null) {

            activeDate = null;
            announcementIndex = 0;
            nextBroadcastTime = 0L;
        }
    }

    public boolean isBroadcastDue(long currentTime) {
        return currentTime >= nextBroadcastTime;
    }

    public void scheduleNextBroadcast(long currentTime) {

        long delayMilliseconds =
                frequencySeconds * 1000L;

        nextBroadcastTime =
                currentTime + delayMilliseconds;
    }

    public String getNextAnnouncement() {

        if (announcements.isEmpty()) {
            return null;
        }

        if (announcementIndex >= announcements.size()) {
            announcementIndex = 0;
        }

        String announcement =
                announcements.get(announcementIndex);

        announcementIndex++;

        if (announcementIndex >= announcements.size()) {
            announcementIndex = 0;
        }

        return announcement;
    }

    public boolean hasAnnouncements() {
        return !announcements.isEmpty();
    }

    public String getName() {
        return name;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public String getDate() {
        return date;
    }

    public long getFrequencySeconds() {
        return frequencySeconds;
    }

    public List<String> getAnnouncements() {
        return Collections.unmodifiableList(announcements);
    }

    public int getAnnouncementIndex() {
        return announcementIndex;
    }

    public long getNextBroadcastTime() {
        return nextBroadcastTime;
    }

    public LocalDate getActiveDate() {
        return activeDate;
    }
}