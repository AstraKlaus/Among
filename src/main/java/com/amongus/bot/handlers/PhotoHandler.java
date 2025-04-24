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
 * Handles photo messages from users, which might be used for sharing game maps or other media.
 */
public class PhotoHandler {
    private static final Logger log = LoggerFactory.getLogger(PhotoHandler.class);
    
    private final AmongUsBot bot;
    private final SessionManager sessionManager;
    private final SecurityManager securityManager;
    
    /**
     * Creates a new photo handler.
     */
    public PhotoHandler(AmongUsBot bot, SessionManager sessionManager, SecurityManager securityManager) {
        this.bot = bot;
        this.sessionManager = sessionManager;
        this.securityManager = securityManager;
    }
    
    /**
     * Handles a photo message.
     */
    public void handlePhoto(Message message) {
        User telegramUser = message.getFrom();
        if (telegramUser == null) {
            log.warn("Received photo with no sender");
            return;
        }
        
        long userId = telegramUser.getId();
        String chatId = message.getChatId().toString();
        
        log.debug("Received photo from user {}", userId);
        
        // Convert the Telegram user to our Player model
        Player player = Player.fromTelegramUser(telegramUser);
        
        // Check if the user is in a game session
        Optional<GameSession> gameSessionOpt = sessionManager.getSessionForPlayer(userId);
        if (gameSessionOpt.isPresent()) {
            // Store the chat ID if not already set
            GameSession gameSession = gameSessionOpt.get();
            gameSession.setPlayerChatId(userId, chatId);
            
            // Let the game session handle the photo based on the current state
            gameSession.handlePhoto(bot, message);
        } else {
            // User is not in a game, handle general photo
            handleGeneralPhoto(message, player);
        }
    }
    
    /**
     * Handles photos when the player is not in a game.
     */
    private void handleGeneralPhoto(Message message, Player player) {
        String chatId = message.getChatId().toString();
        
        // For now, just inform the user that photos are not processed
        bot.sendTextMessageSafe(chatId, "Фотографии пока не обрабатываются. " +
                "Пожалуйста, используйте текстовые команды для взаимодействия с ботом.");
    }
    
    /**
     * Informs the user that photos are ignored during the game.
     */
    private void ignoredPhotoInGame(String chatId) {
        bot.sendTextMessageSafe(chatId, "Во время игры фотографии не обрабатываются.");
    }
} 