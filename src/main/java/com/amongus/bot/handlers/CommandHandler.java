package com.amongus.bot.handlers;

import com.amongus.bot.commands.*;
import com.amongus.bot.core.AmongUsBot;
import com.amongus.bot.core.SessionManager;
import com.amongus.bot.utils.SecurityManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

import java.util.HashMap;
import java.util.Map;

/**
 * Handles command messages from users.
 * Delegates command execution to specific command classes.
 */
public class CommandHandler {
    private static final Logger log = LoggerFactory.getLogger(CommandHandler.class);
    private final AmongUsBot bot;
    private final SessionManager sessionManager;
    private final SecurityManager securityManager;
    private final Map<String, Command> commands;

    /**
     * Creates a new command handler.
     */
    public CommandHandler(AmongUsBot bot, SessionManager sessionManager, SecurityManager securityManager) {
        this.bot = bot;
        this.sessionManager = sessionManager;
        this.securityManager = securityManager;
        this.commands = new HashMap<>();
        registerCommands();
    }

    /**
     * Registers all available commands.
     */
    private void registerCommands() {
        // Game creation commands
        registerCommand(new CreateCommand(bot, sessionManager, securityManager));
        registerCommand(new StartCommand(bot, sessionManager, securityManager));
        registerCommand(new JoinCommand(bot, sessionManager, securityManager));
        registerCommand(new LeaveCommand(bot, sessionManager, securityManager));
        registerCommand(new StartGameCommand(bot, sessionManager, securityManager));

        // Information commands
        registerCommand(new HelpCommand(bot, sessionManager, commands));
        registerCommand(new PlayersCommand(bot, sessionManager, securityManager));
        registerCommand(new RulesCommand(bot, sessionManager, securityManager));

        // Admin commands
        registerCommand(new EndGameCommand(bot, sessionManager, securityManager));
        registerCommand(new KickCommand(bot, sessionManager, securityManager));
    }

    /**
     * Registers a command.
     */
    private void registerCommand(Command command) {
        commands.put(command.getName(), command);
        for (String alias : command.getAliases()) {
            commands.put(alias, command);
        }
    }

    /**
     * Handles a command message.
     */
    public void handleCommand(Message message) {
        String text = message.getText();
        String[] parts = text.split("\\s+", 2);
        String commandName = parts[0].substring(1).toLowerCase(); // Remove the leading '/'
        String args = parts.length > 1 ? parts[1] : "";

        Command command = commands.get(commandName);
        if (command != null) {
            try {
                log.debug("Executing command: {} from user {}", commandName, message.getFrom().getId());
                command.execute(message, args);
            } catch (Exception e) {
                log.error("Error executing command {}: {}", commandName, e.getMessage(), e);
                sendErrorMessage(message.getChatId(), "Произошла ошибка при выполнении команды.");
            }
        } else {
            log.debug("Unknown command: {} from user {}", commandName, message.getFrom().getId());
            sendErrorMessage(message.getChatId(), "Неизвестная команда. Напишите /help для получения списка доступных команд.");
        }
    }

    /**
     * Sends an error message to the user.
     */
    private void sendErrorMessage(Long chatId, String text) {
        SendMessage message = new SendMessage();
        message.setChatId(chatId.toString());
        message.setText(text);
        try {
            bot.execute(message);
        } catch (TelegramApiException e) {
            log.error("Failed to send error message: {}", e.getMessage(), e);
        }
    }
}
