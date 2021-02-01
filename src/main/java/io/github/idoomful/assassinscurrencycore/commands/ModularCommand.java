package io.github.idoomful.assassinscurrencycore.commands;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.ConsoleCommandSender;
import org.bukkit.entity.Player;

import java.util.function.BiConsumer;

/*

    Once instantiated, it will automatically register the command
    {@link CommandSettings} must have a {@param name}

    @param settings  Contains all settings for command
    @param action    Callback that executes on command

    @author Liviu Cocianu
    @version October 11, 2019

*/

public class ModularCommand implements CommandExecutor {
    private final CommandSettings settings;
    private final BiConsumer<CommandSender, String[]> action;

    public ModularCommand(CommandSettings settings, BiConsumer<CommandSender, String[]> action) {
        this.settings = settings;
        this.action = action;
        CommandUtils.registerCommand(settings.aliases, this);
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {

        switch(settings.cmdRunner) {
            case BOTH:
                exec(sender, args); break;
            case PLAYER:
                if(sender instanceof Player) exec(sender, args);
                else sender.sendMessage(settings.permMessage);
                break;
            case CONSOLE:
                if(sender instanceof ConsoleCommandSender) exec(sender, args);
                else sender.sendMessage(settings.permMessage);
                break;
        }

        return false;
    }

    private boolean requiresPermission() {
        return !settings.permission.equals("");
    }

    private void execute(CommandSender player, String[] args) {
        action.accept(player, args);
    }

    private void exec(CommandSender sender, String[] args) {
        if(requiresPermission()) {
            if(sender.hasPermission(settings.permission)) execute(sender, args);
            else sender.sendMessage(settings.permMessage);
        } else {
            execute(sender, args);
        }
    }
}
