package com.cristian.particleforge.command;

import org.bukkit.command.CommandSender;

import java.util.List;

/**
 * Internal SPI for {@code /pf} subcommands. Implementations live in
 * {@code com.cristian.particleforge.command.sub}.
 */
public interface Subcommand {
    String name();
    /** Required permission node; null means no permission gate. */
    String permission();
    /** messages.yml key for the help line description. */
    String descriptionKey();
    void execute(CommandSender sender, String[] args);
    List<String> tabComplete(CommandSender sender, String[] args);
}
