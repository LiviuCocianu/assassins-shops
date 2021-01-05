package io.github.idoomful.assassinseconomy.commands;

import org.bukkit.command.CommandSender;

public interface CommandModel {
    void execute(CommandSender player, String[] args);
}
