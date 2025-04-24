package com.amongus.bot.commands;

import org.telegram.telegrambots.meta.api.objects.Message;

import java.util.Collections;
import java.util.List;

/**
 * Interface for bot commands.
 */
public interface Command {
    /**
     * Executes the command.
     *
     * @param message The message that triggered the command
     * @param args    The arguments for the command
     */
    void execute(Message message, String args);

    /**
     * Gets the command name.
     *
     * @return The command name without the leading slash
     */
    String getName();

    /**
     * Gets the command aliases.
     *
     * @return A list of command aliases without the leading slash
     */
    default List<String> getAliases() {
        return Collections.emptyList();
    }

    /**
     * Gets the command description.
     *
     * @return The command description
     */
    String getDescription();

    /**
     * Checks if the command requires admin privileges.
     *
     * @return True if admin privileges are required, false otherwise
     */
    default boolean isAdminCommand() {
        return false;
    }

    /**
     * Checks if the command is only available in game sessions.
     *
     * @return True if the command is only available in game sessions, false otherwise
     */
    default boolean requiresGameSession() {
        return false;
    }
} 