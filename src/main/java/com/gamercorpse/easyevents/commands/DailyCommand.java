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

public class DailyCommand
        implements CommandExecutor, TabCompleter {

    private final EasyEvents plugin;

    public DailyCommand(
            EasyEvents plugin
    ) {

        this.plugin =
                plugin;
    }

    @Override
    public boolean onCommand(
            CommandSender sender,
            Command command,
            String label,
            String[] args
    ) {

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

        if (args.length == 0) {

            plugin.getDailyManager()
                    .openMenu(player);

            return true;
        }

        if (args[0].equalsIgnoreCase("r") ||
                args[0].equalsIgnoreCase("reward") ||
                args[0].equalsIgnoreCase("rewards") ||
                args[0].equalsIgnoreCase("open")) {

            plugin.getDailyManager()
                    .openMenu(player);

            return true;
        }

        plugin.getDailyManager()
                .openMenu(player);

        return true;
    }

    @Override
    public List<String> onTabComplete(
            CommandSender sender,
            Command command,
            String alias,
            String[] args
    ) {

        if (!plugin.isModuleEnabled(
                ModuleManager.DAILY_LOGIN
        )) {

            return Collections.emptyList();
        }

        if (args.length != 1) {
            return Collections.emptyList();
        }

        List<String> completions =
                new ArrayList<>();

        completions.add("r");
        completions.add("rewards");

        String input =
                args[0]
                        .toLowerCase();

        completions.removeIf(
                value ->
                        !value.toLowerCase()
                                .startsWith(input)
        );

        return completions;
    }
}