package io.github.idoomful.assassinscurrencycore.commands;

import org.bukkit.command.CommandSender;

public interface CommandModel {
    void execute(CommandSender player, String[] args);
}
