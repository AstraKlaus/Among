package com.amongus.bot.handlers;

import com.amongus.bot.core.AmongUsBot;
import com.amongus.bot.core.SessionManager;
import com.amongus.bot.game.states.GameSession;
import com.amongus.bot.models.Player;
import com.amongus.bot.utils.SecurityManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.api.objects.User;

import java.util.Optional;

/**
 * Handles text messages from users.
 */
public class MessageHandler {
    private static final Logger log = LoggerFactory.getLogger(MessageHandler.class);
    
    private final AmongUsBot bot;
    private final SessionManager sessionManager;
    private final SecurityManager securityManager;
    
    /**
     * Creates a new message handler.
     */
    public MessageHandler(AmongUsBot bot, SessionManager sessionManager, SecurityManager securityManager) {
        this.bot = bot;
        this.sessionManager = sessionManager;
        this.securityManager = securityManager;
    }
    
    /**
     * Handles a text message.
     */
    public void handleMessage(Message message) {
        User telegramUser = message.getFrom();
        if (telegramUser == null) {
            log.warn("Received message with no sender");
            return;
        }
        
        long userId = telegramUser.getId();
        String chatId = message.getChatId().toString();
        String text = message.getText();
        
        log.debug("Received message from user {}: {}", userId, text);
        
        // Convert the Telegram user to our Player model
        Player player = Player.fromTelegramUser(telegramUser);
        
        // Check if the user is in a game session
        Optional<GameSession> gameSessionOpt = sessionManager.getSessionForPlayer(userId);
        if (gameSessionOpt.isPresent()) {
            // Store the chat ID so we can send messages to this player
            GameSession gameSession = gameSessionOpt.get();
            gameSession.setPlayerChatId(userId, chatId);
            
            // Let the game session handle the message based on the current state
            gameSession.handleMessage(bot, message);
        } else {
            // User is not in a game, handle general messages
            handleGeneralMessage(message, player);
        }
    }
    
    /**
     * Handles general messages not related to a specific game.
     */
    private void handleGeneralMessage(Message message, Player player) {
        String chatId = message.getChatId().toString();
        String text = message.getText();
        
        // For now, just send a message suggesting to use commands
        if (!text.startsWith("/")) {
            bot.sendTextMessageSafe(chatId, "Пожалуйста, используйте команды для взаимодействия с ботом. " +
                    "Отправьте /help для списка доступных команд.");
        }
    }
} 