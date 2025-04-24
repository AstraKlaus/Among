package com.amongus.bot.game.voting;

import com.amongus.bot.core.AmongUsBot;
import com.amongus.bot.game.states.GameSession;
import com.amongus.bot.models.Player;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;

import java.util.*;
import java.util.concurrent.TimeUnit;

/**
 * Manages the voting process during discussions.
 */
public class VotingManager {
    
    private final VoteTracker voteTracker;
    private final long votingStartTime;
    private final long votingDurationMs;
    private boolean votingEnded = false;
    
    /**
     * Creates a new voting manager.
     *
     * @param votingDurationSeconds The duration of the voting phase in seconds
     */
    public VotingManager(int votingDurationSeconds) {
        this.voteTracker = new VoteTracker();
        this.votingStartTime = System.currentTimeMillis();
        this.votingDurationMs = TimeUnit.SECONDS.toMillis(votingDurationSeconds);
    }
    
    /**
     * Checks if the voting phase has ended.
     *
     * @return true if voting has ended, false otherwise
     */
    public boolean isVotingEnded() {
        return votingEnded || getRemainingVotingTimeMs() <= 0;
    }
    
    /**
     * Gets the remaining voting time in milliseconds.
     *
     * @return The remaining voting time in milliseconds
     */
    public long getRemainingVotingTimeMs() {
        long elapsedTime = System.currentTimeMillis() - votingStartTime;
        return Math.max(0, votingDurationMs - elapsedTime);
    }
    
    /**
     * Gets the remaining voting time in seconds.
     *
     * @return The remaining voting time in seconds
     */
    public int getRemainingVotingTimeSec() {
        return (int) TimeUnit.MILLISECONDS.toSeconds(getRemainingVotingTimeMs());
    }
    
    /**
     * Registers all living players as voters.
     *
     * @param gameSession The game session
     */
    public void registerVoters(GameSession gameSession) {
        for (Player player : gameSession.getPlayers()) {
            if (player.isAlive()) {
                voteTracker.addVoter(player.getUserId(), player.getDisplayName());
            }
        }
    }
    
    /**
     * Records a vote from a player.
     *
     * @param voterId  The ID of the voter
     * @param targetId The ID of the target
     * @return true if the vote was recorded, false otherwise
     */
    public boolean castVote(long voterId, String targetId) {
        return voteTracker.vote(voterId, targetId);
    }
    
    /**
     * Checks if everyone has voted.
     *
     * @return true if everyone has voted, false otherwise
     */
    public boolean hasEveryoneVoted() {
        return voteTracker.hasEveryoneVoted();
    }
    
    /**
     * Ends the voting phase.
     */
    public void endVoting() {
        votingEnded = true;
    }
    
    /**
     * Gets the vote tracker.
     *
     * @return The vote tracker
     */
    public VoteTracker getVoteTracker() {
        return voteTracker;
    }
    
    /**
     * Creates a voting keyboard for a player.
     *
     * @param gameSession The game session
     * @param voter       The player who is voting
     * @return The voting keyboard
     */
    public InlineKeyboardMarkup createVotingKeyboard(GameSession gameSession, Player voter) {
        List<List<InlineKeyboardButton>> rows = new ArrayList<>();
        
        // Add button for each living player except the voter
        for (Player player : gameSession.getPlayers()) {
            if (player.isAlive() && player.getUserId() != voter.getUserId()) {
                InlineKeyboardButton button = new InlineKeyboardButton();
                button.setText(player.getDisplayName());
                button.setCallbackData("vote_" + player.getUserId());
                
                List<InlineKeyboardButton> row = new ArrayList<>();
                row.add(button);
                rows.add(row);
            }
        }
        
        // Add skip vote button
        InlineKeyboardButton skipButton = new InlineKeyboardButton();
        skipButton.setText("⏭️ Пропустить");
        skipButton.setCallbackData("vote_skip");
        
        List<InlineKeyboardButton> skipRow = new ArrayList<>();
        skipRow.add(skipButton);
        rows.add(skipRow);
        
        InlineKeyboardMarkup markup = new InlineKeyboardMarkup();
        markup.setKeyboard(rows);
        
        return markup;
    }
    
    /**
     * Starts the voting phase by sending voting keyboards to all living players.
     *
     * @param gameSession The game session
     * @param bot         The bot instance
     */
    public void startVoting(GameSession gameSession, AmongUsBot bot) {
        // Send voting keyboard to all living players
        for (Player player : gameSession.getPlayers()) {
            if (player.isAlive()) {
                gameSession.getPlayerChatId(player.getUserId()).ifPresent(chatId -> {
                    SendMessage voteMessage = new SendMessage();
                    voteMessage.setChatId(chatId);
                    voteMessage.setText("🗳️ *Голосование*\n\nВыберите игрока или пропустите:");
                    voteMessage.enableMarkdown(true);
                    
                    InlineKeyboardMarkup markup = createVotingKeyboard(gameSession, player);
                    voteMessage.setReplyMarkup(markup);
                    
                    bot.sendMessageSafe(voteMessage);
                });
            }
        }
    }
    
    /**
     * Gets a formatted string with voting results.
     *
     * @param gameSession The game session
     * @return The formatted voting results
     */
    public String getFormattedVotingResults(GameSession gameSession) {
        Map<String, Integer> voteCount = voteTracker.getVoteCounts();
        List<Player> players = gameSession.getPlayers();
        
        StringBuilder sb = new StringBuilder();
        sb.append("📊 *Результаты голосования:*\n\n");
        
        for (Map.Entry<String, Integer> entry : voteCount.entrySet()) {
            String targetId = entry.getKey();
            int count = entry.getValue();
            
            if (targetId.equals("skip")) {
                sb.append("⏭️ *Пропуск*: ").append(count).append(" голосов\n");
            } else {
                try {
                    long id = Long.parseLong(targetId);
                    Optional<Player> targetPlayerOpt = gameSession.getPlayer(id);
                    if (targetPlayerOpt.isPresent()) {
                        sb.append("*").append(targetPlayerOpt.get().getDisplayName())
                          .append("*: ").append(count).append(" голосов\n");
                    }
                } catch (NumberFormatException e) {
                    // Skip invalid entries
                }
            }
        }
        
        // List players who didn't vote
        List<String> nonVoters = voteTracker.getNonVoters();
        if (!nonVoters.isEmpty()) {
            sb.append("\n⚠️ *Не проголосовали:*\n");
            for (String voterId : nonVoters) {
                try {
                    long id = Long.parseLong(voterId);
                    Optional<Player> playerOpt = gameSession.getPlayer(id);
                    if (playerOpt.isPresent()) {
                        sb.append("- ").append(playerOpt.get().getDisplayName()).append("\n");
                    }
                } catch (NumberFormatException e) {
                    // Skip invalid entries
                }
            }
        }
        
        // Add the player with most votes (if any)
        String mostVotedId = voteTracker.getMostVotedPlayer();
        if (mostVotedId != null && !mostVotedId.equals("skip")) {
            try {
                long id = Long.parseLong(mostVotedId);
                Optional<Player> ejectedPlayerOpt = gameSession.getPlayer(id);
                if (ejectedPlayerOpt.isPresent()) {
                    Player ejectedPlayer = ejectedPlayerOpt.get();
                    sb.append("\n🚀 *").append(ejectedPlayer.getDisplayName())
                      .append("* был(а) выброшен(а) с корабля!");
                    
                    // Reveal role
                    sb.append("\nРоль: ").append(ejectedPlayer.getRole().isImpostor() ? "Импостер! 😈" : "Член экипажа 👨‍🚀");
                }
            } catch (NumberFormatException e) {
                // Skip invalid entries
            }
        } else if (mostVotedId != null && mostVotedId.equals("skip")) {
            sb.append("\n⏭️ *Никто не был выброшен с корабля.*");
        } else {
            sb.append("\n⚖️ *Ничья в голосовании. Никто не был выброшен с корабля.*");
        }
        
        return sb.toString();
    }
} 