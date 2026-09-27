package com.gamercorpse.easyevents.commands;

import com.gamercorpse.easyevents.EasyEvents;
import com.gamercorpse.easyevents.modules.ModuleManager;
import com.gamercorpse.easyevents.utils.ColorUtil;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class EasyEventsCommand
        implements CommandExecutor, TabCompleter {

    private final EasyEvents plugin;

    public EasyEventsCommand(
            EasyEvents plugin
    ) {

        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(
            CommandSender sender,
            Command command,
            String label,
            String[] args
    ) {

        if (args.length == 0) {

            sendHelp(sender);
            return true;
        }

        if (args[0].equalsIgnoreCase(
                "daily"
        )) {

            if (!plugin.isModuleEnabled(
                    ModuleManager.DAILY_LOGIN
            )) {

                sender.sendMessage(
                        ColorUtil.colorize(
                                "&cThe Daily Login module is currently disabled."
                        )
                );

                return true;
            }

            if (!(sender instanceof Player player)) {

                sender.sendMessage(
                        ColorUtil.colorize(
                                "&cOnly players can open the daily rewards menu."
                        )
                );

                return true;
            }

            if (!player.hasPermission(
                    "easyevents.daily"
            )) {

                player.sendMessage(
                        ColorUtil.colorize(
                                "&cYou do not have permission to use daily rewards."
                        )
                );

                return true;
            }

            plugin.getDailyManager()
                    .openMenu(player);

            return true;
        }

        if (args[0].equalsIgnoreCase(
                "reload"
        )) {

            if (!sender.hasPermission(
                    "easyevents.admin"
            )) {

                sender.sendMessage(
                        ColorUtil.colorize(
                                "&cYou do not have permission to use this command."
                        )
                );

                return true;
            }

            plugin.reloadPlugin();

            sender.sendMessage(
                    ColorUtil.colorize(
                            "&aEasyEvents configuration reloaded."
                    )
            );

            sender.sendMessage(
                    ColorUtil.colorize(
                            "&7Storage: &f" +
                                    plugin.getActiveStorageType()
                                            .toUpperCase()
                    )
            );

            sendModuleStates(
                    sender
            );

            return true;
        }

        if (args[0].equalsIgnoreCase(
                "info"
        )) {

            sendInfo(
                    sender
            );

            return true;
        }

        sendHelp(sender);

        return true;
    }

    private void sendInfo(
            CommandSender sender
    ) {

        sender.sendMessage(
                ColorUtil.colorize(
                        "&6&m--------------------------------"
                )
        );

        sender.sendMessage(
                ColorUtil.colorize(
                        "&6&lEasyEvents &7v" +
                                plugin.getPluginMeta()
                                        .getVersion()
                )
        );

        sender.sendMessage(
                ColorUtil.colorize(
                        "&7Storage: &f" +
                                plugin.getActiveStorageType()
                                        .toUpperCase()
                )
        );

        sender.sendMessage(
                ColorUtil.colorize(
                        "&7Calendar: " +
                                getModuleStatus(
                                        ModuleManager.CALENDAR
                                )
                )
        );

        if (plugin.isModuleEnabled(
                ModuleManager.CALENDAR
        )) {

            sender.sendMessage(
                    ColorUtil.colorize(
                            "&7  Loaded events: &f" +
                                    plugin.getCalendarManager()
                                            .getEvents()
                                            .size()
                    )
            );
        }

        sender.sendMessage(
                ColorUtil.colorize(
                        "&7Daily Login: " +
                                getModuleStatus(
                                        ModuleManager.DAILY_LOGIN
                                )
                )
        );

        if (plugin.isModuleEnabled(
                ModuleManager.DAILY_LOGIN
        )) {

            sender.sendMessage(
                    ColorUtil.colorize(
                            "&7  Reward days: &f" +
                                    plugin.getDailyManager()
                                            .getDayKeys()
                                            .size()
                    )
            );
        }

        sender.sendMessage(
                ColorUtil.colorize(
                        "&7Auto Broadcast: " +
                                getModuleStatus(
                                        ModuleManager.AUTO_BROADCAST
                                )
                )
        );

        if (plugin.isModuleEnabled(
                ModuleManager.AUTO_BROADCAST
        )) {

            sender.sendMessage(
                    ColorUtil.colorize(
                            "&7  Messages: &f" +
                                    plugin.getAutoBroadcastManager()
                                            .getMessageCount()
                    )
            );
        }

        sender.sendMessage(
                ColorUtil.colorize(
                        "&7Player Milestones: " +
                                getModuleStatus(
                                        ModuleManager.PLAYER_MILESTONES
                                )
                )
        );

        if (plugin.isModuleEnabled(
                ModuleManager.PLAYER_MILESTONES
        )) {

            sender.sendMessage(
                    ColorUtil.colorize(
                            "&7  Milestones: &f" +
                                    plugin.getPlayerMilestoneManager()
                                            .getMilestoneCount()
                    )
            );
        }

        sender.sendMessage(
                ColorUtil.colorize(
                        "&7Random Events: " +
                                getModuleStatus(
                                        ModuleManager.RANDOM_EVENTS
                                )
                )
        );

        if (plugin.isModuleEnabled(
                ModuleManager.RANDOM_EVENTS
        )) {

            sender.sendMessage(
                    ColorUtil.colorize(
                            "&7  Events: &f" +
                                    plugin.getRandomEventManager()
                                            .getEventCount()
                    )
            );
        }

        sender.sendMessage(
                ColorUtil.colorize(
                        "&6&m--------------------------------"
                )
        );
    }

    private void sendModuleStates(
            CommandSender sender
    ) {

        sender.sendMessage(
                ColorUtil.colorize(
                        "&7Calendar: " +
                                getModuleStatus(
                                        ModuleManager.CALENDAR
                                )
                )
        );

        sender.sendMessage(
                ColorUtil.colorize(
                        "&7Daily Login: " +
                                getModuleStatus(
                                        ModuleManager.DAILY_LOGIN
                                )
                )
        );

        sender.sendMessage(
                ColorUtil.colorize(
                        "&7Auto Broadcast: " +
                                getModuleStatus(
                                        ModuleManager.AUTO_BROADCAST
                                )
                )
        );

        sender.sendMessage(
                ColorUtil.colorize(
                        "&7Player Milestones: " +
                                getModuleStatus(
                                        ModuleManager.PLAYER_MILESTONES
                                )
                )
        );

        sender.sendMessage(
                ColorUtil.colorize(
                        "&7Random Events: " +
                                getModuleStatus(
                                        ModuleManager.RANDOM_EVENTS
                                )
                )
        );
    }

    private void sendHelp(
            CommandSender sender
    ) {

        sender.sendMessage(
                ColorUtil.colorize(
                        "&6&m--------------------------------"
                )
        );

        sender.sendMessage(
                ColorUtil.colorize(
                        "&6&lEasyEvents"
                )
        );

        if (plugin.isModuleEnabled(
                ModuleManager.DAILY_LOGIN
        )) {

            sender.sendMessage(
                    ColorUtil.colorize(
                            "&e/ee daily &7- Open daily rewards."
                    )
            );
        }

        sender.sendMessage(
                ColorUtil.colorize(
                        "&e/ee info &7- View plugin information."
                )
        );

        if (sender.hasPermission(
                "easyevents.admin"
        )) {

            sender.sendMessage(
                    ColorUtil.colorize(
                            "&e/ee reload &7- Reload EasyEvents."
                    )
            );
        }

        sender.sendMessage(
                ColorUtil.colorize(
                        "&6&m--------------------------------"
                )
        );
    }

    private String getModuleStatus(
            String moduleName
    ) {

        if (plugin.isModuleEnabled(
                moduleName
        )) {

            return "&aENABLED";
        }

        return "&cDISABLED";
    }

    @Override
    public List<String> onTabComplete(
            CommandSender sender,
            Command command,
            String alias,
            String[] args
    ) {

        if (args.length != 1) {
            return Collections.emptyList();
        }

        List<String> completions =
                new ArrayList<>();

        if (plugin.isModuleEnabled(
                ModuleManager.DAILY_LOGIN
        )) {

            completions.add(
                    "daily"
            );
        }

        completions.add(
                "info"
        );

        if (sender.hasPermission(
                "easyevents.admin"
        )) {

            completions.add(
                    "reload"
            );
        }

        String input =
                args[0].toLowerCase();

        completions.removeIf(
                value ->
                        !value.toLowerCase()
                                .startsWith(input)
        );

        return completions;
    }
}