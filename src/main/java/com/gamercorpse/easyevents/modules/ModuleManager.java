package com.gamercorpse.easyevents.modules;

import com.gamercorpse.easyevents.EasyEvents;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

public class ModuleManager {

    public static final String CALENDAR = "calendar";
    public static final String DAILY_LOGIN = "daily-login";

    private final EasyEvents plugin;

    private final Map<String, Boolean> modules =
            new LinkedHashMap<>();

    private File modulesFile;
    private FileConfiguration modulesConfig;

    public ModuleManager(EasyEvents plugin) {
        this.plugin = plugin;
    }

    public void initialize() {
        loadModules();
    }

    public void reload() {
        loadModules();
    }

    private void loadModules() {

        if (!plugin.getDataFolder().exists()) {
            plugin.getDataFolder().mkdirs();
        }

        modulesFile =
                new File(
                        plugin.getDataFolder(),
                        "modules.yml"
                );

        if (!modulesFile.exists()) {

            try {

                plugin.saveResource(
                        "modules.yml",
                        false
                );

            } catch (IllegalArgumentException exception) {

                plugin.getLogger().severe(
                        "modules.yml was not found inside the plugin JAR."
                );

                /*
                 * Safe fallback:
                 * existing functionality remains enabled.
                 */
                modules.clear();
                modules.put(CALENDAR, true);
                modules.put(DAILY_LOGIN, true);

                return;
            }
        }

        modulesConfig =
                YamlConfiguration.loadConfiguration(
                        modulesFile
                );

        modules.clear();

        loadModule(
                CALENDAR,
                true
        );

        loadModule(
                DAILY_LOGIN,
                true
        );

        plugin.getLogger().info(
                "Loaded " +
                        modules.size() +
                        " EasyEvents module(s)."
        );

        for (Map.Entry<String, Boolean> entry :
                modules.entrySet()) {

            plugin.getLogger().info(
                    "Module '" +
                            entry.getKey() +
                            "': " +
                            (
                                    entry.getValue()
                                            ? "ENABLED"
                                            : "DISABLED"
                            )
            );
        }
    }

    private void loadModule(
            String moduleName,
            boolean defaultState
    ) {

        if (modulesConfig == null) {

            modules.put(
                    normalizeModuleName(moduleName),
                    defaultState
            );

            return;
        }

        String path =
                "modules." +
                        moduleName +
                        ".enabled";

        boolean enabled =
                modulesConfig.getBoolean(
                        path,
                        defaultState
                );

        modules.put(
                normalizeModuleName(moduleName),
                enabled
        );
    }

    public boolean isEnabled(
            String moduleName
    ) {

        if (moduleName == null ||
                moduleName.isBlank()) {

            return false;
        }

        return modules.getOrDefault(
                normalizeModuleName(moduleName),
                false
        );
    }

    public boolean isDisabled(
            String moduleName
    ) {

        return !isEnabled(moduleName);
    }

    public Map<String, Boolean> getModules() {

        return Collections.unmodifiableMap(
                modules
        );
    }

    public File getModulesFile() {
        return modulesFile;
    }

    public FileConfiguration getModulesConfig() {
        return modulesConfig;
    }

    private String normalizeModuleName(
            String moduleName
    ) {

        return moduleName
                .trim()
                .toLowerCase()
                .replace('_', '-')
                .replace(' ', '-');
    }
}