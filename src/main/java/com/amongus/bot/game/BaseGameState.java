package com.amongus.bot.game;

import com.amongus.bot.core.AmongUsBot;
import com.amongus.bot.models.Player;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.telegram.telegrambots.meta.api.objects.CallbackQuery;
import org.telegram.telegrambots.meta.api.objects.Message;

/**
 * Base implementation of GameState with default behavior for all methods.
 * Other states can extend this class and override only the methods they need.
 */
public abstract class BaseGameState implements GameState {
    protected final Logger log = LoggerFactory.getLogger(getClass());

    @Override
    public abstract String getStateName();

    @Override
    public GameState handleMessage(GameSession gameSession, AmongUsBot bot, Message message) {
        // Default implementation: do nothing and stay in the same state
        return this;
    }

    @Override
    public GameState handleCallbackQuery(GameSession gameSession, AmongUsBot bot, CallbackQuery callbackQuery) {
        // Default implementation: do nothing and stay in the same state
        return this;
    }

    @Override
    public GameState handlePhoto(GameSession gameSession, AmongUsBot bot, Message message) {
        // Default implementation: do nothing and stay in the same state
        return this;
    }

    @Override
    public void onEnter(GameSession gameSession, AmongUsBot bot) {
        // Default implementation: log state transition
        log.info("Entering state {} for game {}", getStateName(), gameSession.getId());
    }

    @Override
    public void onExit(GameSession gameSession, AmongUsBot bot) {
        // Default implementation: log state transition
        log.info("Exiting state {} for game {}", getStateName(), gameSession.getId());
    }

    @Override
    public void onPlayerJoin(GameSession gameSession, AmongUsBot bot, Player player) {
        // Default implementation: do nothing
    }

    @Override
    public void onPlayerLeave(GameSession gameSession, AmongUsBot bot, Player player) {
        // Default implementation: do nothing
    }

    @Override
    public GameState onUpdate(GameSession gameSession, AmongUsBot bot) {
        // Default implementation: do nothing and stay in the same state
        return this;
    }

    /**
     * Sends a message to all players in the game.
     */
    protected void sendMessageToAllPlayers(GameSession gameSession, AmongUsBot bot, String message) {
        for (Player player : gameSession.getPlayers()) {
            gameSession.getPlayerChatId(player.getUserId()).ifPresent(chatId -> {
                bot.sendTextMessageSafe(chatId, message);
            });
        }
    }

    /**
     * Sends a message to all living players in the game.
     */
    protected void sendMessageToLivingPlayers(GameSession gameSession, AmongUsBot bot, String message) {
        for (Player player : gameSession.getLivingPlayers()) {
            gameSession.getPlayerChatId(player.getUserId()).ifPresent(chatId -> {
                bot.sendTextMessageSafe(chatId, message);
            });
        }
    }

    /**
     * Sends a message to all ghost players in the game.
     */
    protected void sendMessageToGhosts(GameSession gameSession, AmongUsBot bot, String message) {
        for (Player player : gameSession.getGhosts()) {
            gameSession.getPlayerChatId(player.getUserId()).ifPresent(chatId -> {
                bot.sendTextMessageSafe(chatId, message);
            });
        }
    }

    /**
     * Sends a message to a specific player.
     */
    protected void sendMessageToPlayer(GameSession gameSession, AmongUsBot bot, Player player, String message) {
        gameSession.getPlayerChatId(player.getUserId()).ifPresent(chatId -> {
            bot.sendTextMessageSafe(chatId, message);
        });
    }
} 