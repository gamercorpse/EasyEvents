package com.gamercorpse.easyevents.storage;

import com.gamercorpse.easyevents.EasyEvents;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public class YmlStorage {

    private final EasyEvents plugin;

    private File dataFile;
    private FileConfiguration dataConfig;

    public YmlStorage(EasyEvents plugin) {
        this.plugin = plugin;
    }

    public synchronized void initialize() {

        if (!plugin.getDataFolder().exists()) {

            boolean created =
                    plugin.getDataFolder().mkdirs();

            if (!created &&
                    !plugin.getDataFolder().exists()) {

                plugin.getLogger().severe(
                        "Could not create the EasyEvents data folder."
                );
            }
        }

        dataFile =
                new File(
                        plugin.getDataFolder(),
                        "data.yml"
                );

        if (!dataFile.exists()) {

            try {

                boolean created =
                        dataFile.createNewFile();

                if (!created &&
                        !dataFile.exists()) {

                    plugin.getLogger().severe(
                            "Could not create data.yml."
                    );
                }

            } catch (IOException exception) {

                plugin.getLogger().severe(
                        "Could not create data.yml."
                );

                exception.printStackTrace();
            }
        }

        dataConfig =
                YamlConfiguration.loadConfiguration(
                        dataFile
                );

        if (!dataConfig.contains("data")) {

            dataConfig.createSection("data");
            save();
        }
    }

    public synchronized void reload() {

        if (dataFile == null) {
            initialize();
            return;
        }

        dataConfig =
                YamlConfiguration.loadConfiguration(
                        dataFile
                );
    }

    public synchronized void save() {

        if (dataConfig == null ||
                dataFile == null) {

            return;
        }

        try {

            dataConfig.save(dataFile);

        } catch (IOException exception) {

            plugin.getLogger().severe(
                    "Could not save data.yml."
            );

            exception.printStackTrace();
        }
    }

    public synchronized void set(
            String key,
            String value
    ) {

        if (!isValidKey(key)) {
            return;
        }

        ensureInitialized();

        dataConfig.set(
                getPath(key),
                value
        );

        save();
    }

    public synchronized String get(
            String key
    ) {

        if (!isValidKey(key)) {
            return null;
        }

        ensureInitialized();

        return dataConfig.getString(
                getPath(key)
        );
    }

    public synchronized String get(
            String key,
            String defaultValue
    ) {

        String value =
                get(key);

        return value != null
                ? value
                : defaultValue;
    }

    public synchronized boolean contains(
            String key
    ) {

        if (!isValidKey(key)) {
            return false;
        }

        ensureInitialized();

        return dataConfig.contains(
                getPath(key)
        );
    }

    public synchronized void remove(
            String key
    ) {

        if (!isValidKey(key)) {
            return;
        }

        ensureInitialized();

        dataConfig.set(
                getPath(key),
                null
        );

        save();
    }

    public synchronized Map<String, String> getAll() {

        ensureInitialized();

        ConfigurationSection section =
                dataConfig.getConfigurationSection(
                        "data"
                );

        if (section == null) {
            return Collections.emptyMap();
        }

        Map<String, String> values =
                new HashMap<>();

        for (String key :
                section.getKeys(true)) {

            if (section.isConfigurationSection(key)) {
                continue;
            }

            Object value =
                    section.get(key);

            if (value == null) {
                continue;
            }

            values.put(
                    key,
                    String.valueOf(value)
            );
        }

        return Collections.unmodifiableMap(
                values
        );
    }

    public synchronized void clear() {

        ensureInitialized();

        dataConfig.set(
                "data",
                null
        );

        dataConfig.createSection(
                "data"
        );

        save();
    }

    public File getDataFile() {
        return dataFile;
    }

    public FileConfiguration getDataConfig() {
        return dataConfig;
    }

    private void ensureInitialized() {

        if (dataConfig == null ||
                dataFile == null) {

            initialize();
        }
    }

    private boolean isValidKey(
            String key
    ) {

        return key != null &&
                !key.isBlank();
    }

    private String getPath(
            String key
    ) {

        return "data." + key;
    }
}