package com.amongus.bot.game.sabotage;

import com.amongus.bot.models.Player;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

/**
 * Manages active sabotages in the game.
 */
public class SabotageManager {
    private static final Logger log = LoggerFactory.getLogger(SabotageManager.class);
    
    // Map of game IDs to their active sabotage
    private final Map<Long, Sabotage> activeSabotages = new HashMap<>();
    
    // Map of game IDs to their sabotage timeout tasks
    private final Map<Long, ScheduledFuture<?>> sabotageTimeoutTasks = new HashMap<>();
    
    // Scheduler from the main bot class
    private final ScheduledExecutorService scheduler;
    
    // Callback when a critical sabotage times out
    private final Map<Long, Consumer<Sabotage>> timeoutCallbacks = new HashMap<>();
    
    /**
     * Creates a new sabotage manager.
     */
    public SabotageManager(ScheduledExecutorService scheduler) {
        this.scheduler = scheduler;
    }
    
    /**
     * Initiates a new sabotage for a game.
     * 
     * @param gameId       ID of the game
     * @param type         Type of sabotage
     * @param initiator    Player who initiated the sabotage
     * @param timeoutCallback Callback to execute if a critical sabotage times out
     * @return the created Sabotage, or empty if there's already an active sabotage
     */
    public synchronized Optional<Sabotage> initiateSabotage(
            long gameId, 
            SabotageType type, 
            Player initiator,
            Consumer<Sabotage> timeoutCallback) {
        
        // Check if there's already an active sabotage for this game
        if (activeSabotages.containsKey(gameId)) {
            return Optional.empty();
        }
        
        // Create and store the new sabotage
        Sabotage sabotage = new Sabotage(type, initiator, gameId);
        activeSabotages.put(gameId, sabotage);
        
        // Store the timeout callback
        if (timeoutCallback != null) {
            timeoutCallbacks.put(gameId, timeoutCallback);
        }
        
        // If it's a critical sabotage, schedule a timeout task
        if (type.isCritical()) {
            // Cancel any existing timeout task for this game
            cancelTimeoutTask(gameId);
            
            // Schedule a new timeout task
            ScheduledFuture<?> timeoutTask = scheduler.schedule(
                    () -> handleSabotageTimeout(gameId),
                    sabotage.getRemainingTimeSeconds(),
                    TimeUnit.SECONDS
            );
            
            sabotageTimeoutTasks.put(gameId, timeoutTask);
        }
        
        log.info("Initiated {} sabotage for game {}", type, gameId);
        return Optional.of(sabotage);
    }
    
    /**
     * Handles the timeout of a critical sabotage.
     */
    private synchronized void handleSabotageTimeout(long gameId) {
        Sabotage sabotage = activeSabotages.get(gameId);
        if (sabotage != null && !sabotage.isFixed() && sabotage.getType().isCritical()) {
            log.info("Critical sabotage timed out for game {}: {}", gameId, sabotage.getType());
            
            // Execute the timeout callback if available
            Consumer<Sabotage> callback = timeoutCallbacks.get(gameId);
            if (callback != null) {
                callback.accept(sabotage);
            }
            
            // Remove the sabotage and its tasks
            activeSabotages.remove(gameId);
            sabotageTimeoutTasks.remove(gameId);
            timeoutCallbacks.remove(gameId);
        }
    }
    
    /**
     * Attempts to fix a sabotage.
     * 
     * @param gameId ID of the game
     * @param player Player attempting to fix
     * @param location Location where the fix is being attempted
     * @return true if the sabotage was fixed (completely), false otherwise
     */
    public synchronized boolean attemptFix(long gameId, Player player, String location) {
        Sabotage sabotage = activeSabotages.get(gameId);
        if (sabotage == null) {
            return false;
        }
        
        boolean fixed = sabotage.recordFix(player, location);
        
        if (fixed) {
            log.info("Sabotage {} fixed for game {}", sabotage.getType(), gameId);
            
            // Cancel the timeout task if it exists
            cancelTimeoutTask(gameId);
            
            // Remove the sabotage
            activeSabotages.remove(gameId);
            timeoutCallbacks.remove(gameId);
        }
        
        return fixed;
    }
    
    /**
     * Cancels any active sabotage for a game.
     */
    public synchronized void cancelSabotage(long gameId) {
        // Cancel the timeout task if it exists
        cancelTimeoutTask(gameId);
        
        // Remove the sabotage and callback
        activeSabotages.remove(gameId);
        timeoutCallbacks.remove(gameId);
        
        log.info("Cancelled sabotage for game {}", gameId);
    }
    
    /**
     * Cancels the timeout task for a game.
     */
    private void cancelTimeoutTask(long gameId) {
        ScheduledFuture<?> task = sabotageTimeoutTasks.remove(gameId);
        if (task != null && !task.isDone()) {
            task.cancel(false);
        }
    }
    
    /**
     * Gets the active sabotage for a game.
     */
    public synchronized Optional<Sabotage> getActiveSabotage(long gameId) {
        return Optional.ofNullable(activeSabotages.get(gameId));
    }
    
    /**
     * Checks if there's an active sabotage of a specific type for a game.
     */
    public synchronized boolean hasActiveSabotageOfType(long gameId, SabotageType type) {
        Sabotage sabotage = activeSabotages.get(gameId);
        return sabotage != null && sabotage.getType() == type;
    }
    
    /**
     * Checks if a player can perform tasks based on active sabotages.
     */
    public synchronized boolean canPerformTasks(long gameId, Player player) {
        Sabotage sabotage = activeSabotages.get(gameId);
        if (sabotage == null) {
            return true;
        }
        
        // Cannot perform tasks if communications are sabotaged
        return sabotage.getType() != SabotageType.COMMUNICATIONS;
    }
    
    /**
     * Checks if a player can submit task photos based on active sabotages.
     */
    public synchronized boolean canSubmitTaskPhotos(long gameId, Player player) {
        Sabotage sabotage = activeSabotages.get(gameId);
        if (sabotage == null) {
            return true;
        }
        
        // Cannot submit photos if lights are out
        return sabotage.getType() != SabotageType.LIGHTS;
    }
} 