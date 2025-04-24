package com.amongus.bot.core;

import com.amongus.bot.game.states.GameSession;
import com.amongus.bot.game.lobby.GameLobby;
import com.amongus.bot.models.Config;
import com.amongus.bot.models.Player;
import com.amongus.bot.utils.CodeGenerator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * Manages all active game sessions (lobbies) and player associations.
 */
public class SessionManager {
    private static final Logger log = LoggerFactory.getLogger(SessionManager.class);

    private Map<String, GameSession> activeSessions = new HashMap<>();

    // Map of lobby codes to GameLobby instances
    private final Map<String, GameLobby> lobbies = new ConcurrentHashMap<>();
    
    // Map of user IDs to lobby codes they are in
    private final Map<Long, String> playerLobbyMap = new ConcurrentHashMap<>();
    
    /**
     * Creates a new game lobby and returns its code.
     */
    public String createLobby(Player owner) {
        // First, check if player is already in another lobby
        String existingLobbyCode = playerLobbyMap.get(owner.getUserId());
        if (existingLobbyCode != null) {
            // Remove player from previous lobby
            GameLobby existingLobby = lobbies.get(existingLobbyCode);
            if (existingLobby != null) {
                existingLobby.removePlayer(owner.getUserId());
                
                // If lobby is now empty, remove it
                if (existingLobby.getPlayers().isEmpty()) {
                    lobbies.remove(existingLobbyCode);
                    log.info("Removed empty lobby: {}", existingLobbyCode);
                }
            }
            
            playerLobbyMap.remove(owner.getUserId());
        }
        
        // Generate a unique lobby code
        String lobbyCode;
        do {
            lobbyCode = CodeGenerator.generateGameCode(Config.GAME_CODE_LENGTH);
        } while (lobbies.containsKey(lobbyCode));
        
        // Create new lobby
        GameLobby lobby = new GameLobby(lobbyCode, owner);
        lobbies.put(lobbyCode, lobby);
        
        // Associate player with lobby
        playerLobbyMap.put(owner.getUserId(), lobbyCode);
        
        log.info("Created new lobby with code {} owned by {}", lobbyCode, owner.getUserId());
        return lobbyCode;
    }
    
    /**
     * Adds a player to an existing lobby.
     * 
     * @return true if joining was successful, false otherwise
     */
    public boolean joinLobby(String lobbyCode, Player player) {
        GameLobby lobby = lobbies.get(lobbyCode);
        
        // Check if lobby exists
        if (lobby == null) {
            return false;
        }
        
        // Check if player is already in another lobby
        String existingLobbyCode = playerLobbyMap.get(player.getUserId());
        if (existingLobbyCode != null) {
            // Remove player from previous lobby
            GameLobby existingLobby = lobbies.get(existingLobbyCode);
            if (existingLobby != null) {
                existingLobby.removePlayer(player.getUserId());
                
                // If lobby is now empty, remove it
                if (existingLobby.getPlayers().isEmpty()) {
                    lobbies.remove(existingLobbyCode);
                    log.info("Removed empty lobby: {}", existingLobbyCode);
                }
            }
            
            playerLobbyMap.remove(player.getUserId());
        }
        
        // Add player to lobby
        boolean added = lobby.addPlayer(player);
        if (added) {
            playerLobbyMap.put(player.getUserId(), lobbyCode);
            log.info("Player {} joined lobby {}", player.getUserId(), lobbyCode);
        }
        
        return added;
    }
    
    /**
     * Retrieves the lobby for a given player.
     */
    public Optional<GameLobby> getLobbyForPlayer(long userId) {
        String lobbyCode = playerLobbyMap.get(userId);
        if (lobbyCode == null) {
            return Optional.empty();
        }
        
        return Optional.ofNullable(lobbies.get(lobbyCode));
    }

    public Optional<GameSession> getSessionByLobbyCode(String lobbyCode) {
        return Optional.ofNullable(activeSessions.get(lobbyCode));
    }

    /**
     * Retrieves a lobby by its code.
     */
    public Optional<GameLobby> getLobbyByCode(String lobbyCode) {
        return Optional.ofNullable(lobbies.get(lobbyCode));
    }
    
    /**
     * Removes a player from their current lobby.
     */
    public boolean removePlayerFromLobby(long userId) {
        String lobbyCode = playerLobbyMap.get(userId);
        if (lobbyCode == null) {
            return false;
        }
        
        GameLobby lobby = lobbies.get(lobbyCode);
        if (lobby == null) {
            playerLobbyMap.remove(userId);
            return false;
        }
        
        boolean removed = lobby.removePlayer(userId);
        if (removed) {
            playerLobbyMap.remove(userId);
            log.info("Player {} left lobby {}", userId, lobbyCode);
            
            // If lobby is now empty, remove it
            if (lobby.getPlayers().isEmpty()) {
                lobbies.remove(lobbyCode);
                log.info("Removed empty lobby: {}", lobbyCode);
            }
        }
        
        return removed;
    }
    
    /**
     * Removes an entire lobby.
     */
    public boolean removeLobby(String lobbyCode) {
        GameLobby lobby = lobbies.remove(lobbyCode);
        if (lobby == null) {
            return false;
        }
        
        // Remove all player associations
        lobby.getPlayers().stream()
                .map(Player::getUserId)
                .forEach(playerLobbyMap::remove);
        
        log.info("Removed lobby: {}", lobbyCode);
        return true;
    }
    
    /**
     * Cleans up inactive sessions periodically.
     * This method is called by the scheduler in AmongUsBot.
     */
    public void cleanupInactiveSessions() {
        log.info("Running inactive session cleanup");
        Instant now = Instant.now();
        
        // Find lobbies that have been inactive for too long
        lobbies.entrySet().stream()
                .filter(entry -> {
                    GameLobby lobby = entry.getValue();
                    long inactiveMinutes = lobby.getInactiveMinutes(now);
                    
                    // Apply different timeout rules based on game state
                    if (lobby.isGameInProgress()) {
                        return inactiveMinutes > Config.GAME_INACTIVE_TIMEOUT_MINUTES;
                    } else {
                        return inactiveMinutes > Config.LOBBY_TIMEOUT_MINUTES;
                    }
                })
                .map(Map.Entry::getKey)
                .collect(Collectors.toList()) // Collect to avoid ConcurrentModificationException
                .forEach(this::removeLobby);
    }

    /**
     * Gets the game session for a player.
     * 
     * @param userId The ID of the player
     * @return The game session, or empty if the player is not in a game
     */
    public Optional<GameSession> getSessionForPlayer(long userId) {
        return activeSessions.values().stream()
                .filter(session -> session.getPlayer(userId).isPresent())
                .findFirst();
    }

    public Player getPlayer(long userId) {
        for (GameSession session : activeSessions.values()) {
            Player player = session.getPlayerById(userId);
            if (player != null) {
                return player;
            }
        }
        return null;
    }

    public void updatePlayerChatId(long userId, String chatId) {
        log.info("Updating chat ID for user {} to {}", userId, chatId);
        Player player = getPlayer(userId);
        if (player != null) {
            player.setChatId(Long.parseLong(chatId));
        } else {
            log.warn("Player with ID {} not found when updating chatId", userId);
        }
    }

    public String getPlayerChatId(long userId) {
        Player player = getPlayer(userId);
        if (player != null) {
            long chatIdLong = player.getChatId();
            if (chatIdLong == 0) {
                log.warn("ChatId for player {} is 0!", userId);
            }
            return String.valueOf(chatIdLong);
        }
        log.warn("No player found for userId {} when getting chatId", userId);
        return null;
    }

    // Добавить метод startGame
    public void startGame(String lobbyCode, AmongUsBot bot) {
        log.info("Attempting to start game for lobby: {}", lobbyCode);
        GameSession session = activeSessions.get(lobbyCode);
        if (session == null) {
            log.error("Game session not found for lobby: {}", lobbyCode);
            return;
        }
        session.startGame(bot);
    }

    /**
     * Завершает игровую сессию с указанным кодом лобби.
     * Может быть вызван администратором для принудительного завершения игры
     * или автоматически при выполнении условий победы.
     *
     * @param lobbyCode Код лобби, игру которого нужно завершить
     * @param bot Экземпляр бота для отправки сообщений
     */
    public void endGame(String lobbyCode, AmongUsBot bot) {
        // Проверяем, существует ли сессия с указанным кодом
        GameSession session = activeSessions.get(lobbyCode);
        if (session == null) {
            log.warn("Попытка завершить несуществующую игровую сессию: {}", lobbyCode);
            return;
        }

        try {
            // Определяем победившую команду
            Optional<String> winningTeamOpt = session.checkWinConditions();
            String winningTeam = winningTeamOpt.orElse("unknown");

            // Переводим игру в состояние завершения
            session.endGame(winningTeam, bot);

            // Уведомляем всех игроков
            String endMessage = "Игра окончена!";
            if (winningTeamOpt.isPresent()) {
                if (winningTeam.equals("crewmates")) {
                    endMessage += " Победа членов экипажа!";
                } else if (winningTeam.equals("impostors")) {
                    endMessage += " Победа предателей!";
                }
            } else {
                endMessage += " Игра была принудительно завершена администратором.";
            }

            for (Player player : session.getPlayers()) {
                String finalEndMessage = endMessage;
                session.getPlayerChatId(player.getUserId()).ifPresent(chatId -> {
                    bot.sendTextMessageSafe(chatId, finalEndMessage);
                });
            }

            // Удаляем сессию из активных
            activeSessions.remove(lobbyCode);
            log.info("Игровая сессия {} завершена и удалена", lobbyCode);
        } catch (Exception e) {
            log.error("Ошибка при завершении игровой сессии {}: {}", lobbyCode, e.getMessage(), e);
        }
    }

    public Map<String, GameSession> getActiveSessions() {
        return activeSessions;
    }
}