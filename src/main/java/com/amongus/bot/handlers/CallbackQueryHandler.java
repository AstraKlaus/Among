package com.amongus.bot.handlers;

import com.amongus.bot.core.AmongUsBot;
import com.amongus.bot.core.SessionManager;
import com.amongus.bot.game.states.GameSession;
import com.amongus.bot.utils.SecurityManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.telegram.telegrambots.meta.api.methods.AnswerCallbackQuery;
import org.telegram.telegrambots.meta.api.objects.CallbackQuery;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

import java.util.Optional;

/**
 * Handles callback queries from inline keyboard buttons.
 */
public class CallbackQueryHandler {
    private static final Logger log = LoggerFactory.getLogger(CallbackQueryHandler.class);
    
    private final AmongUsBot bot;
    private final SessionManager sessionManager;
    private final SecurityManager securityManager;
    
    /**
     * Creates a new callback query handler.
     */
    public CallbackQueryHandler(AmongUsBot bot, SessionManager sessionManager, SecurityManager securityManager) {
        this.bot = bot;
        this.sessionManager = sessionManager;
        this.securityManager = securityManager;
    }
    
    /**
     * Handles a callback query from an inline keyboard button.
     */
    public void handleCallbackQuery(CallbackQuery callbackQuery) {
        String callbackData = callbackQuery.getData();
        long userId = callbackQuery.getFrom().getId();
        String queryId = callbackQuery.getId();
        
        log.debug("Received callback query from user {}: {}", userId, callbackData);
        
        // First, we should acknowledge the callback query to stop the loading indicator
        acknowledgeCallbackQuery(queryId);
        
        // Check if the user is authorized to perform actions
        if (!securityManager.isUserAuthorized(userId)) {
            log.warn("Unauthorized user {} attempted to use callback query", userId);
            sendCallbackResponse(queryId, "Вы не авторизованы для использования этой функции.");
            return;
        }
        
        // Check if the user is in a game session
        Optional<GameSession> gameSessionOpt = sessionManager.getSessionForPlayer(userId);
        if (gameSessionOpt.isPresent()) {
            GameSession gameSession = gameSessionOpt.get();
            
            // Let the game session handle the callback query based on the current state
            gameSession.handleCallbackQuery(bot, callbackQuery);
        } else {
            // Handle general callback queries (not related to an active game)
            handleGeneralCallbackQuery(callbackQuery);
        }
    }
    
    /**
     * Handles callback queries that are not related to a specific game session.
     */
    private void handleGeneralCallbackQuery(CallbackQuery callbackQuery) {
        String callbackData = callbackQuery.getData();
        String queryId = callbackQuery.getId();
        
        // For now, we'll just acknowledge the callback
        log.debug("Handling general callback query: {}", callbackData);
        
        // In a real implementation, we would parse the callback data and take appropriate action
        // This might include creating games, joining lobbies, etc.
        
        // Just acknowledge for now
        sendCallbackResponse(queryId, "Обработка команды...");
    }
    
    /**
     * Acknowledges a callback query without showing a notification to the user.
     */
    private void acknowledgeCallbackQuery(String queryId) {
        AnswerCallbackQuery answer = new AnswerCallbackQuery();
        answer.setCallbackQueryId(queryId);
        
        try {
            bot.execute(answer);
        } catch (TelegramApiException e) {
            log.error("Failed to acknowledge callback query", e);
        }
    }
    
    /**
     * Sends a response to a callback query with a notification to the user.
     */
    private void sendCallbackResponse(String queryId, String text) {
        AnswerCallbackQuery answer = new AnswerCallbackQuery();
        answer.setCallbackQueryId(queryId);
        answer.setText(text);
        answer.setShowAlert(false);
        
        try {
            bot.execute(answer);
        } catch (TelegramApiException e) {
            log.error("Failed to send callback response", e);
        }
    }
} 