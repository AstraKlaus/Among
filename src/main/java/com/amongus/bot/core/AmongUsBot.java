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
import org.telegram.telegrambots.meta.api.methods.updatingmessages.EditMessageText;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

import java.io.Serializable;
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

    public Integer sendMessageWithReturnIdSafe(String chatId, String text, InlineKeyboardMarkup markup) {
        SendMessage message = new SendMessage();
        message.setChatId(chatId);
        message.setText(text);
        message.enableMarkdown(true);
        message.setReplyMarkup(markup);
        try {
            return execute(message).getMessageId();
        } catch (TelegramApiException e) {
            log.error("Failed to send message", e);
            return null;
        }
    }

    public boolean editMessageTextSafe(String chatId, Integer messageId, String text, InlineKeyboardMarkup markup) {
        EditMessageText edit = new EditMessageText();
        edit.setChatId(chatId);
        edit.setMessageId(messageId);
        edit.setText(text);
        edit.enableMarkdown(true);
        edit.setReplyMarkup(markup);
        try {
            execute(edit);
            return true;
        } catch (TelegramApiException e) {
            log.error("Failed to edit message", e);
            return false;
        }
    }

    /**
     * Safely sends a text message and returns the message ID.
     * Used for messages that need to be tracked for later editing.
     *
     * @param chatId The chat ID to send the message to
     * @param text The text to send
     * @return The message ID of the sent message, or null if sending failed
     */
    public Integer sendTextMessageWithReturnIdSafe(String chatId, String text) {
        SendMessage message = new SendMessage();
        message.setChatId(chatId);
        message.setText(text);
        message.enableMarkdown(true);
        try {
            return execute(message).getMessageId();
        } catch (TelegramApiException e) {
            log.error("Failed to send message", e);
            return null;
        }
    }

    public boolean deleteMessage(String chatId, Integer messageId) {
        DeleteMessage deleteMessage = new DeleteMessage(chatId, messageId);
        try {
            execute(deleteMessage);
            return true;
        } catch (TelegramApiException e) {
            log.error("Failed to delete message", e);
            return false;
        }
    }

    /**
     * Execute any Telegram API method safely.
     */
    @Override
    public <T extends Serializable, Method extends BotApiMethod<T>> T execute(Method method) throws TelegramApiException {
        return super.execute(method);
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