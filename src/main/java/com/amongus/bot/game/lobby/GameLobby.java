package com.amongus.bot.game.lobby;

import com.amongus.bot.models.Config;
import com.amongus.bot.models.GameSettings;
import com.amongus.bot.models.Player;
import lombok.Data;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Represents a game lobby where players gather before starting a game.
 */
@Data
public class GameLobby {
    private final String lobbyCode;
    private final Player owner;
    private final List<Player> players = new ArrayList<>();
    private final Instant creationTime;
    private Instant lastActivityTime;
    private final GameSettings settings = new GameSettings();
    private final Map<Long, Integer> playerStatusMessageIds = new ConcurrentHashMap<>();
    private boolean gameInProgress = false;
    
    /**
     * Creates a new game lobby with the specified owner.
     */
    public GameLobby(String lobbyCode, Player owner) {
        this.lobbyCode = lobbyCode;
        this.owner = owner;
        this.creationTime = Instant.now();
        this.lastActivityTime = Instant.now();
        
        // Add owner as the first player
        this.players.add(owner);
    }
    
    /**
     * Adds a player to the lobby.
     * 
     * @return true if player was added, false if lobby is full or game is in progress
     */
    public boolean addPlayer(Player player) {
        // Check if lobby is full
        if (players.size() >= Config.MAX_PLAYERS) {
            return false;
        }
        
        // Check if game is in progress
        if (gameInProgress) {
            return false;
        }
        
        // Check if player is already in the lobby
        if (players.stream().anyMatch(p -> p.getUserId() == player.getUserId())) {
            return false;
        }
        
        // Add player
        players.add(player);
        recordActivity();
        return true;
    }
    
    /**
     * Removes a player from the lobby.
     * 
     * @return true if player was removed, false if player was not in the lobby
     */
    public boolean removePlayer(long userId) {
        Optional<Player> playerOpt = players.stream()
                .filter(p -> p.getUserId() == userId)
                .findFirst();
        
        if (playerOpt.isPresent()) {
            players.remove(playerOpt.get());
            recordActivity();
            return true;
        }
        
        return false;
    }
    
    /**
     * Gets a player from the lobby by their user ID.
     */
    public Optional<Player> getPlayer(long userId) {
        return players.stream()
                .filter(p -> p.getUserId() == userId)
                .findFirst();
    }
    
    /**
     * Checks if a player is the owner of the lobby.
     */
    public boolean isOwner(long userId) {
        return owner.getUserId() == userId;
    }
    
    /**
     * Checks if all players are ready.
     */
    public boolean areAllPlayersReady() {
        return !players.isEmpty() && players.stream().allMatch(Player::isReady);
    }
    
    /**
     * Gets the number of ready players.
     */
    public int getReadyPlayerCount() {
        return (int) players.stream().filter(Player::isReady).count();
    }
    
    /**
     * Gets a string representation of players and their ready status.
     */
    public String getPlayerStatusString() {
        StringBuilder sb = new StringBuilder();
        
        sb.append("👥 *Игроки* (").append(players.size()).append("/").append(Config.MAX_PLAYERS).append("):\n");
        
        for (Player player : players) {
            String readyStatus = player.isReady() ? "✅" : "⬜";
            String ownerLabel = isOwner(player.getUserId()) ? " 👑" : "";
            
            sb.append(readyStatus).append(" ")
                    .append(player.getDisplayName())
                    .append(ownerLabel)
                    .append("\n");
        }
        
        return sb.toString();
    }
    
    /**
     * Records activity in the lobby.
     */
    public void recordActivity() {
        this.lastActivityTime = Instant.now();
    }
    
    /**
     * Gets the number of minutes since the last activity.
     */
    public long getInactiveMinutes(Instant now) {
        return (now.getEpochSecond() - lastActivityTime.getEpochSecond()) / 60;
    }

    public void setStatusMessageId(long userId, Integer messageId) {
        playerStatusMessageIds.put(userId, messageId);
    }

    public Integer getStatusMessageId(long userId) {
        return playerStatusMessageIds.get(userId);
    }
} 