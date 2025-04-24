package com.amongus.bot.game;

import com.amongus.bot.core.AmongUsBot;
import com.amongus.bot.game.roles.Role;
import com.amongus.bot.game.roles.RoleFactory;
import com.amongus.bot.game.sabotage.Sabotage;
import com.amongus.bot.game.sabotage.SabotageManager;
import com.amongus.bot.game.tasks.TaskManager;
import com.amongus.bot.models.Config;
import com.amongus.bot.models.GameSettings;
import com.amongus.bot.models.Player;
import com.amongus.bot.utils.SecurityManager;
import lombok.Data;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.telegram.telegrambots.meta.api.objects.CallbackQuery;
import org.telegram.telegrambots.meta.api.objects.Message;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;

/**
 * Represents a game session with its state and players.
 */
@Data
public class GameSession {
    private static final Logger log = LoggerFactory.getLogger(GameSession.class);
    private static final AtomicLong ID_GENERATOR = new AtomicLong(1);
    
    // Core game properties
    private final long id;
    private final String lobbyCode;
    private final Player owner;
    private final Instant creationTime;
    private Instant lastActivityTime;
    private Instant gameStartTime;
    private GameState currentState;
    
    // Player management
    private final List<Player> players = new ArrayList<>();
    private final Map<Long, String> playerChatIds = new ConcurrentHashMap<>();
    
    // Game components
    private final TaskManager taskManager;
    private final SabotageManager sabotageManager;
    private final RoleFactory roleFactory;
    private final SecurityManager securityManager;
    private final GameSettings settings;
    
    // Reporting and meetings
    private Player bodyReporter;
    private Player meetingCaller;
    private final Map<Long, Long> votes = new ConcurrentHashMap<>();
    private Instant discussionEndTime;
    private Instant votingEndTime;
    
    /**
     * Creates a new game session.
     */
    public GameSession(String lobbyCode, Player owner, ScheduledExecutorService scheduler, SecurityManager securityManager) {
        this.id = ID_GENERATOR.getAndIncrement();
        this.lobbyCode = lobbyCode;
        this.owner = owner;
        this.creationTime = Instant.now();
        this.lastActivityTime = Instant.now();
        this.securityManager = securityManager;
        
        // Add the owner as the first player
        this.players.add(owner);
        
        // Initialize game components
        this.taskManager = new TaskManager();
        this.sabotageManager = new SabotageManager(scheduler);
        this.roleFactory = new RoleFactory(securityManager);
        this.settings = new GameSettings();
        
        // Set the initial state
        this.currentState = new LobbyState();
    }
    
    /**
     * Records activity in the game session.
     */
    public void recordActivity() {
        this.lastActivityTime = Instant.now();
    }
    
    /**
     * Gets the minutes since the last activity.
     */
    public long getInactiveMinutes(Instant now) {
        return (now.getEpochSecond() - lastActivityTime.getEpochSecond()) / 60;
    }
    
    /**
     * Checks if a game is in progress.
     */
    public boolean isGameInProgress() {
        return gameStartTime != null;
    }
    
    /**
     * Adds a player to the game.
     */
    public boolean addPlayer(Player player) {
        // Check if player already exists
        if (players.stream().anyMatch(p -> p.getUserId() == player.getUserId())) {
            return false;
        }
        
        // Add the player
        players.add(player);
        
        // Notify the current state
        if (currentState != null) {
            currentState.onPlayerJoin(this, null, player);
        }
        
        return true;
    }
    
    /**
     * Removes a player from the game.
     */
    public boolean removePlayer(long userId) {
        Optional<Player> playerOpt = players.stream()
                .filter(p -> p.getUserId() == userId)
                .findFirst();
        
        if (playerOpt.isPresent()) {
            Player player = playerOpt.get();
            players.remove(player);
            playerChatIds.remove(userId);
            
            // Notify the current state
            if (currentState != null) {
                currentState.onPlayerLeave(this, null, player);
            }
            
            return true;
        }
        
        return false;
    }
    
    /**
     * Gets a player by their user ID.
     */
    public Optional<Player> getPlayer(long userId) {
        return players.stream()
                .filter(p -> p.getUserId() == userId)
                .findFirst();
    }

    public Player getPlayerById(long userId) {
        Optional<Player> playerOpt = getPlayer(userId);
        return playerOpt.orElse(null);
    }
    
    /**
     * Sets the player's chat ID for sending direct messages.
     */
    public void setPlayerChatId(long userId, String chatId) {
        playerChatIds.put(userId, chatId);
    }
    
    /**
     * Gets a player's chat ID for sending direct messages.
     */
    public Optional<String> getPlayerChatId(long userId) {
        return Optional.ofNullable(playerChatIds.get(userId));
    }
    
    /**
     * Marks a player as ready.
     */
    public boolean markPlayerReady(long userId) {
        Optional<Player> playerOpt = getPlayer(userId);
        if (playerOpt.isPresent()) {
            Player player = playerOpt.get();
            player.setReady(true);
            return true;
        }
        return false;
    }
    
    /**
     * Checks if all players are ready.
     */
    public boolean areAllPlayersReady() {
        return !players.isEmpty() && players.stream().allMatch(Player::isReady);
    }
    
    /**
     * Gets all living players.
     */
    public List<Player> getLivingPlayers() {
        return players.stream()
                .filter(Player::isAlive)
                .collect(Collectors.toList());
    }
    
    /**
     * Gets all dead players (ghosts).
     */
    public List<Player> getGhosts() {
        return players.stream()
                .filter(p -> !p.isAlive())
                .collect(Collectors.toList());
    }
    
    /**
     * Kills a player.
     */
    public boolean killPlayer(long userId) {
        Optional<Player> playerOpt = getPlayer(userId);
        if (playerOpt.isPresent() && playerOpt.get().isAlive()) {
            Player player = playerOpt.get();
            player.kill();
            return true;
        }
        return false;
    }
    
    /**
     * Starts a new game.
     */
    public void startGame(AmongUsBot bot) {
        // Reset all players
        for (Player player : players) {
            player.revive();
            player.setReady(false);
        }
        
        // Reset game components
        taskManager.reset();
        
        // Record the start time
        gameStartTime = Instant.now();
        
        // Assign roles to players
        roleFactory.assignRoles(players, settings.getImpostorCount());
        
        // Assign tasks to crewmates
        for (Player player : roleFactory.getCrewmates(players)) {
            taskManager.assignTasksToPlayer(player, settings.getTasksPerPlayer());
        }
        
        // Transition to the GameRunningState
        transitionToState(new GameRunningState(), bot);
    }
    
    /**
     * Reports a body and starts an emergency meeting.
     */
    public void reportBody(Player reporter, AmongUsBot bot) {
        this.bodyReporter = reporter;
        startMeeting(reporter, bot);
    }
    
    /**
     * Calls an emergency meeting.
     */
    public boolean callEmergencyMeeting(Player caller, AmongUsBot bot) {
        if (caller.useEmergencyMeeting()) {
            this.meetingCaller = caller;
            startMeeting(caller, bot);
            return true;
        }
        return false;
    }
    
    /**
     * Starts a meeting (discussion and voting).
     */
    private void startMeeting(Player initiator, AmongUsBot bot) {
        // Reset votes
        votes.clear();
        
        // Set discussion end time
        discussionEndTime = Instant.now().plusSeconds(settings.getDiscussionTimeSeconds());
        
        // Transition to the discussion state
        transitionToState(new DiscussionState(), bot);
    }
    
    /**
     * Records a vote.
     */
    public void recordVote(long voterId, long targetId) {
        votes.put(voterId, targetId);
    }
    
    /**
     * Gets the player with the most votes.
     * If there's a tie, returns Optional.empty().
     */
    public Optional<Player> getMostVotedPlayer() {
        Map<Long, Long> voteCounts = new ConcurrentHashMap<>();
        
        // Count votes for each player
        for (long targetId : votes.values()) {
            if (targetId > 0) { // Skip skipped votes (targetId == 0)
                voteCounts.put(targetId, voteCounts.getOrDefault(targetId, 0L) + 1);
            }
        }
        
        if (voteCounts.isEmpty()) {
            return Optional.empty();
        }
        
        // Find the player with the most votes
        long maxVotes = 0;
        long mostVotedId = 0;
        boolean tie = false;
        
        for (Map.Entry<Long, Long> entry : voteCounts.entrySet()) {
            long playerId = entry.getKey();
            long voteCount = entry.getValue();
            
            if (voteCount > maxVotes) {
                maxVotes = voteCount;
                mostVotedId = playerId;
                tie = false;
            } else if (voteCount == maxVotes) {
                tie = true;
            }
        }
        
        // If there's a tie, no one is ejected
        if (tie) {
            return Optional.empty();
        }
        
        // Return the player with the most votes
        return getPlayer(mostVotedId);
    }
    
    /**
     * Handles a message in the current state.
     */
    public void handleMessage(AmongUsBot bot, Message message) {
        recordActivity();
        
        if (currentState != null) {
            GameState nextState = currentState.handleMessage(this, bot, message);
            if (nextState != currentState) {
                transitionToState(nextState, bot);
            }
        }
    }
    
    /**
     * Handles a callback query in the current state.
     */
    public void handleCallbackQuery(AmongUsBot bot, CallbackQuery callbackQuery) {
        recordActivity();
        
        if (currentState != null) {
            GameState nextState = currentState.handleCallbackQuery(this, bot, callbackQuery);
            if (nextState != currentState) {
                transitionToState(nextState, bot);
            }
        }
    }
    
    /**
     * Handles a photo message in the current game state.
     * 
     * @param bot The bot instance
     * @param message The photo message to handle
     */
    public void handlePhoto(AmongUsBot bot, Message message) {
        // Delegate to the current state
        GameState nextState = currentState.handlePhoto(this, bot, message);
        if (nextState != currentState) {
            transitionToState(nextState, bot);
        }
    }
    
    /**
     * Updates the game state.
     */
    public void update(AmongUsBot bot) {
        if (currentState != null) {
            GameState nextState = currentState.onUpdate(this, bot);
            if (nextState != currentState) {
                transitionToState(nextState, bot);
            }
        }
    }
    
    /**
     * Transitions to a new state.
     */
    private void transitionToState(GameState newState, AmongUsBot bot) {
        if (currentState != null) {
            log.info("Game {} transitioning from {} to {}", id, currentState.getStateName(), newState.getStateName());
            currentState.onExit(this, bot);
        } else {
            log.info("Game {} initializing with state {}", id, newState.getStateName());
        }
        
        currentState = newState;
        currentState.onEnter(this, bot);
    }
    
    /**
     * Checks win conditions and returns the winning team, if any.
     */
    public Optional<String> checkWinConditions() {
        // Get living players by role
        List<Player> livingImpostors = getLivingPlayers().stream()
                .filter(p -> p.getRole().isImpostor())
                .collect(Collectors.toList());
        
        List<Player> livingCrewmates = getLivingPlayers().stream()
                .filter(p -> !p.getRole().isImpostor())
                .collect(Collectors.toList());
        
        // Check if all tasks are completed
        if (taskManager.areAllTasksCompleted()) {
            return Optional.of("crewmates");
        }
        
        // Check if all impostors are eliminated
        if (livingImpostors.isEmpty()) {
            return Optional.of("crewmates");
        }
        
        // Check if impostors match or outnumber crewmates
        if (livingImpostors.size() >= livingCrewmates.size()) {
            return Optional.of("impostors");
        }
        
        // No win condition met yet
        return Optional.empty();
    }
    
    /**
     * Ends the game with the specified winning team.
     */
    public void endGame(String winningTeam, AmongUsBot bot) {
        // Transition to the game end state
        transitionToState(new GameEndState(winningTeam), bot);
    }
    
    /**
     * Checks if the game session allows submitting task photos.
     * This is used during sabotages like lights out.
     * 
     * @return true if players can submit task photos, false otherwise
     */
    public boolean canSubmitTaskPhotos() {
        // By default, allow task photos unless a sabotage is active
        return !isSabotageActive() || !isLightsOut();
    }
    
    /**
     * Checks if a sabotage is currently active.
     * 
     * @return true if a sabotage is active, false otherwise
     */
    public boolean isSabotageActive() {
        // Logic to check if any sabotage is active
        return false; // Placeholder, implement actual logic
    }
    
    /**
     * Checks if the lights are currently sabotaged.
     * 
     * @return true if lights are out, false otherwise
     */
    public boolean isLightsOut() {
        // Logic to check if lights sabotage is active
        return false; // Placeholder, implement actual logic
    }
    
    /**
     * Called when a sabotage timer runs out.
     * 
     * @return true if the sabotage was successfully completed, false otherwise
     */
    public boolean isSabotageTimedOut() {
        // Logic to check if a timed sabotage has completed
        return false; // Placeholder, implement actual logic
    }
    
    /**
     * Checks if a player is the owner of this game session.
     * 
     * @param userId The ID of the player to check
     * @return true if the player is the owner, false otherwise
     */
    public boolean isOwner(long userId) {
        return owner != null && owner.getUserId() == userId;
    }
    
    /**
     * Gets the owner of this game session.
     * 
     * @return The owner player
     */
    public Player getOwner() {
        return owner;
    }
} 