package com.amongus.bot.game.states;

import com.amongus.bot.core.AmongUsBot;
import com.amongus.bot.models.Player;
import org.telegram.telegrambots.meta.api.objects.CallbackQuery;
import org.telegram.telegrambots.meta.api.objects.Message;

/**
 * Interface representing a state in the game state machine.
 * Implements the State design pattern for game state management.
 */
public interface GameState {
    /**
     * Gets the name of this state.
     */
    String getStateName();
    
    /**
     * Handles a message when in this state.
     * 
     * @param gameSession the game session
     * @param bot the bot instance
     * @param message the message to handle
     * @return the next state, or this state if no transition
     */
    GameState handleMessage(GameSession gameSession, AmongUsBot bot, Message message);
    
    /**
     * Handles a callback query when in this state.
     * 
     * @param gameSession the game session
     * @param bot the bot instance
     * @param callbackQuery the callback query to handle
     * @return the next state, or this state if no transition
     */
    GameState handleCallbackQuery(GameSession gameSession, AmongUsBot bot, CallbackQuery callbackQuery);
    
    /**
     * Handles a photo message when in this state.
     * 
     * @param gameSession the game session
     * @param bot the bot instance
     * @param message the message containing the photo
     * @return the next state, or this state if no transition
     */
    GameState handlePhoto(GameSession gameSession, AmongUsBot bot, Message message);
    
    /**
     * Called when entering this state.
     * 
     * @param gameSession the game session
     * @param bot the bot instance
     */
    void onEnter(GameSession gameSession, AmongUsBot bot);
    
    /**
     * Called when leaving this state.
     * 
     * @param gameSession the game session
     * @param bot the bot instance
     */
    void onExit(GameSession gameSession, AmongUsBot bot);
    
    /**
     * Called when a player joins the game in this state.
     * 
     * @param gameSession the game session
     * @param bot the bot instance
     * @param player the player who joined
     */
    void onPlayerJoin(GameSession gameSession, AmongUsBot bot, Player player);
    
    /**
     * Called when a player leaves the game in this state.
     * 
     * @param gameSession the game session
     * @param bot the bot instance
     * @param player the player who left
     */
    void onPlayerLeave(GameSession gameSession, AmongUsBot bot, Player player);
    
    /**
     * Called periodically to update the state.
     * 
     * @param gameSession the game session
     * @param bot the bot instance
     * @return the next state, or this state if no transition
     */
    GameState onUpdate(GameSession gameSession, AmongUsBot bot);
} 