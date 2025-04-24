package com.amongus.bot.handlers;

import com.amongus.bot.core.AmongUsBot;
import com.amongus.bot.core.SessionManager;
import com.amongus.bot.utils.SecurityManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.api.objects.Update;

/**
 * Handles all incoming updates from Telegram and dispatches them to the appropriate handlers.
 */
public class UpdateHandler {
    private static final Logger log = LoggerFactory.getLogger(UpdateHandler.class);
    
    private final AmongUsBot bot;
    private final SessionManager sessionManager;
    private final SecurityManager securityManager;
    
    private final CommandHandler commandHandler;
    private final MessageHandler messageHandler;
    private final CallbackQueryHandler callbackQueryHandler;
    private final PhotoHandler photoHandler;
    
    /**
     * Creates a new update handler.
     */
    public UpdateHandler(AmongUsBot bot, SessionManager sessionManager, SecurityManager securityManager) {
        this.bot = bot;
        this.sessionManager = sessionManager;
        this.securityManager = securityManager;
        
        // Initialize handlers
        this.commandHandler = new CommandHandler(bot, sessionManager, securityManager);
        this.messageHandler = new MessageHandler(bot, sessionManager, securityManager);
        this.callbackQueryHandler = new CallbackQueryHandler(bot, sessionManager, securityManager);
        this.photoHandler = new PhotoHandler(bot, sessionManager, securityManager);
    }
    
    /**
     * Processes an incoming update from Telegram.
     */
    public void handleUpdate(Update update) {
        try {
            if (update.hasMessage()) {
                handleMessage(update.getMessage());
            } else if (update.hasCallbackQuery()) {
                callbackQueryHandler.handleCallbackQuery(update.getCallbackQuery());
            } else {
                log.debug("Received unsupported update type: {}", update);
            }
        } catch (Exception e) {
            log.error("Error handling update: {}", e.getMessage(), e);
        }
    }
    
    /**
     * Handles an incoming message.
     */
    private void handleMessage(Message message) {
        if (message.hasText()) {
            String text = message.getText();
            
            if (text.startsWith("/")) {
                // This is a command message
                commandHandler.handleCommand(message);
            } else {
                // This is a regular text message
                messageHandler.handleMessage(message);
            }
        } else if (message.hasPhoto()) {
            // This is a photo message
            photoHandler.handlePhoto(message);
        } else {
            // Unsupported message type
            log.debug("Received unsupported message type from user {}", message.getFrom().getId());
        }
    }
} 