package com.amongus.bot.game.states;

import com.amongus.bot.core.AmongUsBot;
import com.amongus.bot.models.Player;
import com.amongus.bot.game.voting.VotingManager;
import com.amongus.bot.game.voting.VotingResults;
import org.telegram.telegrambots.meta.api.objects.CallbackQuery;
import org.telegram.telegrambots.meta.api.objects.Message;

import java.util.*;
import java.util.concurrent.TimeUnit;

/**
 * Represents the state when players are discussing after a body is found or emergency meeting is called.
 */
public class DiscussionState extends BaseGameState {
    
    private final long discussionStartTime;
    private final long discussionDurationMs;
    private final int votingDurationSec;
    private VotingManager votingManager;
    private boolean votingStarted = false;
    private boolean votingEnded = false;
    
    /**
     * Creates a new discussion state with default timings.
     */
    public DiscussionState() {
        this(45, 30); // Default: 45 seconds discussion, 30 seconds voting
    }
    
    /**
     * Creates a new discussion state with custom timings.
     * 
     * @param discussionDurationSec The duration of the discussion phase in seconds
     * @param votingDurationSec The duration of the voting phase in seconds
     */
    public DiscussionState(int discussionDurationSec, int votingDurationSec) {
        this.discussionStartTime = System.currentTimeMillis();
        this.discussionDurationMs = TimeUnit.SECONDS.toMillis(discussionDurationSec);
        this.votingDurationSec = votingDurationSec;
    }
    
    @Override
    public String getStateName() {
        return "DISCUSSION";
    }
    
    @Override
    public void onEnter(GameSession gameSession, AmongUsBot bot) {
        super.onEnter(gameSession, bot);
        
        // Announce discussion start
        sendMessageToAllPlayers(gameSession, bot, "🔔 *ЭКСТРЕННОЕ СОБРАНИЕ*\n\n" +
                "У вас есть " + (discussionDurationMs / 1000) + " секунд для обсуждения, " +
                "затем " + votingDurationSec + " секунд для голосования.\n\n" +
                "Обсудите, кто может быть импостером, и будьте готовы проголосовать!");
    }
    
    @Override
    public GameState handleMessage(GameSession gameSession, AmongUsBot bot, Message message) {
        // Forward messages between players during discussion
        long userId = message.getFrom().getId();
        String text = message.getText();
        
        if (text == null) {
            return this;
        }
        
        Optional<Player> playerOpt = gameSession.getPlayer(userId);
        if (playerOpt.isPresent()) {
            Player player = playerOpt.get();
            
            // Only allow living players to speak during discussion
            if (player.isAlive() && !text.startsWith("/")) {
                String formattedMessage = "*" + player.getDisplayName() + "*: " + text;
                sendMessageToAllPlayers(gameSession, bot, formattedMessage);
            }
            
            // Allow ghost chat for dead players
            if (!player.isAlive() && !text.startsWith("/")) {
                String ghostMessage = "👻 *" + player.getDisplayName() + "* (призрак): " + text;
                sendMessageToGhosts(gameSession, bot, ghostMessage);
            }
        }
        
        return this;
    }
    
    @Override
    public GameState handleCallbackQuery(GameSession gameSession, AmongUsBot bot, CallbackQuery callbackQuery) {
        String data = callbackQuery.getData();
        long userId = callbackQuery.getFrom().getId();
        String chatId = callbackQuery.getMessage().getChatId().toString();
        
        Optional<Player> playerOpt = gameSession.getPlayer(userId);
        if (!playerOpt.isPresent() || !playerOpt.get().isAlive() || !votingStarted || votingEnded) {
            return this;
        }
        
        // Handle voting actions
        if (data.startsWith("vote_")) {
            String targetIdStr = data.substring(5);
            if (targetIdStr.equals("skip")) {
                // Skip vote
                votingManager.castVote(userId, "skip");
                bot.sendTextMessageSafe(chatId, "✅ Вы проголосовали за пропуск.");
                
                // Notify all players that someone voted
                sendMessageToAllPlayers(gameSession, bot, "*" + playerOpt.get().getDisplayName() + "* проголосовал(а).");
            } else {
                try {
                    long targetId = Long.parseLong(targetIdStr);
                    Optional<Player> targetPlayerOpt = gameSession.getPlayer(targetId);
                    
                    if (targetPlayerOpt.isPresent() && targetPlayerOpt.get().isAlive()) {
                        votingManager.castVote(userId, targetIdStr);
                        bot.sendTextMessageSafe(chatId, "✅ Вы проголосовали против *" + 
                                targetPlayerOpt.get().getDisplayName() + "*.");
                        
                        // Notify all players that someone voted
                        sendMessageToAllPlayers(gameSession, bot, "*" + playerOpt.get().getDisplayName() + "* проголосовал(а).");
                    }
                } catch (NumberFormatException e) {
                    // Invalid target ID
                }
            }
            
            // Check if everyone has voted
            if (votingManager.hasEveryoneVoted()) {
                votingEnded = true;
                votingManager.endVoting();
                announceVotingResults(gameSession, bot);
                return processVotingResults(gameSession, bot);
            }
        }
        
        return this;
    }
    
    @Override
    public GameState onUpdate(GameSession gameSession, AmongUsBot bot) {
        long currentTime = System.currentTimeMillis();
        long elapsedTime = currentTime - discussionStartTime;
        
        // Start voting phase after discussion time
        if (!votingStarted && elapsedTime >= discussionDurationMs) {
            votingStarted = true;
            startVoting(gameSession, bot);
        }
        
        // End voting phase after voting time or if voting has ended
        if (votingStarted && !votingEnded) {
            if (votingManager.isVotingEnded()) {
                votingEnded = true;
                announceVotingResults(gameSession, bot);
                return processVotingResults(gameSession, bot);
            }
        }
        
        return this;
    }
    
    /**
     * Starts the voting phase.
     */
    private void startVoting(GameSession gameSession, AmongUsBot bot) {
        // Initialize the voting manager
        votingManager = new VotingManager(votingDurationSec);
        votingManager.registerVoters(gameSession);
        
        sendMessageToAllPlayers(gameSession, bot, "🗳️ *Время голосования!*\n\n" +
                "У вас есть " + votingDurationSec + " секунд, чтобы проголосовать. Выберите игрока, которого вы подозреваете, " +
                "или пропустите голосование.");
        
        // Start the voting process
        votingManager.startVoting(gameSession, bot);
    }
    
    /**
     * Announces voting results to all players.
     */
    private void announceVotingResults(GameSession gameSession, AmongUsBot bot) {
        String resultsMessage = votingManager.getFormattedVotingResults(gameSession);
        sendMessageToAllPlayers(gameSession, bot, resultsMessage);
    }
    
    /**
     * Processes voting results and transitions to next state.
     */
    private GameState processVotingResults(GameSession gameSession, AmongUsBot bot) {
        // Create voting results
        VotingResults results = new VotingResults(votingManager.getVoteTracker());
        
        // If a player should be ejected
        if (results.shouldEjectPlayer()) {
            results.getMostVotedPlayerId().ifPresent(playerId -> {
                try {
                    long id = Long.parseLong(playerId);
                    Optional<Player> ejectedPlayerOpt = gameSession.getPlayer(id);
                    if (ejectedPlayerOpt.isPresent()) {
                        Player ejectedPlayer = ejectedPlayerOpt.get();
                        
                        // Kill the player
                        ejectedPlayer.setAlive(false);
                    }
                } catch (NumberFormatException e) {
                    // Skip invalid entries
                }
            });
            
            // Check win conditions after player ejection
            Optional<String> winningTeam = gameSession.checkWinConditions();
            if (winningTeam.isPresent()) {
                gameSession.endGame(winningTeam.get(), bot);
                return new GameEndState(winningTeam.get());
            }
        }
        
        // Return to the previous game state
        return new GameRunningState();
    }
} 