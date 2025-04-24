package com.amongus.bot.core;

import com.amongus.bot.handlers.CallbackQueryHandler;
import com.amongus.bot.handlers.CommandHandler;
import com.amongus.bot.handlers.MessageHandler;
import com.amongus.bot.handlers.PhotoHandler;
import com.amongus.bot.models.Config;
import com.amongus.bot.utils.SecurityManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.methods.BotApiMethod;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.methods.updatingmessages.DeleteMessage;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * Main bot class that handles Telegram updates and routes them to appropriate handlers.
 */
public class AmongUsBot extends TelegramLongPollingBot {
    private static final Logger log = LoggerFactory.getLogger(AmongUsBot.class);
    private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(4);
    
    private final CommandHandler commandHandler;
    private final CallbackQueryHandler callbackQueryHandler;
    private final MessageHandler messageHandler;
    private final PhotoHandler photoHandler;
    private final SessionManager sessionManager;
    private final SecurityManager securityManager;
    
    public AmongUsBot() {
        this.securityManager = new SecurityManager();
        this.sessionManager = new SessionManager();
        this.commandHandler = new CommandHandler(this, sessionManager, securityManager);
        this.callbackQueryHandler = new CallbackQueryHandler(this, sessionManager, securityManager);
        this.messageHandler = new MessageHandler(this, sessionManager, securityManager);
        this.photoHandler = new PhotoHandler(this, sessionManager, securityManager);
        
        // Schedule periodic tasks like checking game timeouts, etc.
        scheduler.scheduleAtFixedRate(sessionManager::cleanupInactiveSessions, 10, 5, TimeUnit.MINUTES);
    }

    @Override
    public String getBotUsername() {
        return Config.BOT_USERNAME;
    }

    @Override
    public String getBotToken() {
        return Config.BOT_TOKEN;
    }

    @Override
    public void onUpdateReceived(Update update) {
        try {
            if (update.hasMessage()) {
                if (update.getMessage().hasPhoto()) {
                    photoHandler.handlePhoto(update.getMessage());
                } else if (update.getMessage().hasText()) {
                    String text = update.getMessage().getText();
                    if (text.startsWith("/")) {
                        commandHandler.handleCommand(update.getMessage());
                    } else {
                        messageHandler.handleMessage(update.getMessage());
                    }
                }
            } else if (update.hasCallbackQuery()) {
                callbackQueryHandler.handleCallbackQuery(update.getCallbackQuery());
            }
        } catch (Exception e) {
            log.error("Error processing update: {}", e.getMessage(), e);
        }
    }

    /**
     * Safely sends a message and returns the sent message object.
     */
    public Message sendMessageSafe(SendMessage message) {
        try {
            return execute(message);
        } catch (TelegramApiException e) {
            log.error("Failed to send message: {}", e.getMessage(), e);
            return null;
        }
    }

    /**
     * Safely sends a text message and returns the sent message object.
     */
    public Message sendTextMessageSafe(String chatId, String text) {
        SendMessage message = new SendMessage();
        message.setChatId(chatId);
        message.setText(text);
        message.enableMarkdown(true);
        
        return sendMessageSafe(message);
    }

    /**
     * Sends a message that will be auto-deleted after a specified time.
     * Used for sensitive information like role assignments.
     */
    public void sendTemporaryMessage(String chatId, String text, int secondsToLive) {
        SendMessage message = new SendMessage();
        message.setChatId(chatId);
        message.setText(text);
        
        try {
            Message sentMessage = execute(message);
            
            // Schedule message deletion
            scheduler.schedule(() -> {
                DeleteMessage deleteMessage = new DeleteMessage();
                deleteMessage.setChatId(chatId);
                deleteMessage.setMessageId(sentMessage.getMessageId());
                try {
                    execute(deleteMessage);
                } catch (TelegramApiException e) {
                    log.error("Failed to delete temporary message: {}", e.getMessage(), e);
                }
            }, secondsToLive, TimeUnit.SECONDS);
        } catch (TelegramApiException e) {
            log.error("Failed to send temporary message: {}", e.getMessage(), e);
        }
    }

    /**
     * Execute any Telegram API method safely.
     */
    public <T extends BotApiMethod<?>> T executeSafe(T method) {
        try {
            return execute(method);
        } catch (TelegramApiException e) {
            log.error("Failed to execute method {}: {}", method.getClass().getSimpleName(), e.getMessage(), e);
            return null;
        }
    }

    /**
     * Gets the security manager for this bot.
     * 
     * @return The security manager
     */
    public SecurityManager getSecurityManager() {
        return securityManager;
    }
} 