package com.amongus.bot.game.voting;

import com.amongus.bot.models.Player;

import java.util.Map;
import java.util.Optional;

/**
 * Encapsulates the results of voting.
 */
public class VotingResults {
    
    private final Map<String, Integer> voteCounts;
    private final String mostVotedPlayerId;
    private final boolean isTie;
    private final boolean isSkip;
    private final int totalVotes;
    
    /**
     * Creates a new voting results instance.
     *
     * @param voteTracker The vote tracker with votes
     */
    public VotingResults(VoteTracker voteTracker) {
        this.voteCounts = voteTracker.getVoteCounts();
        this.mostVotedPlayerId = voteTracker.getMostVotedPlayer();
        this.isTie = mostVotedPlayerId == null;
        this.isSkip = mostVotedPlayerId != null && mostVotedPlayerId.equals("skip");
        this.totalVotes = voteCounts.values().stream().mapToInt(Integer::intValue).sum();
    }
    
    /**
     * Gets the vote counts for each target.
     *
     * @return The vote counts map
     */
    public Map<String, Integer> getVoteCounts() {
        return voteCounts;
    }
    
    /**
     * Gets the ID of the most voted player.
     *
     * @return The ID of the most voted player, or empty if there's a tie or skip
     */
    public Optional<String> getMostVotedPlayerId() {
        if (isTie || isSkip) {
            return Optional.empty();
        }
        return Optional.ofNullable(mostVotedPlayerId);
    }
    
    /**
     * Checks if the voting resulted in a tie.
     *
     * @return true if there was a tie, false otherwise
     */
    public boolean isTie() {
        return isTie;
    }
    
    /**
     * Checks if the voting resulted in a skip.
     *
     * @return true if skip was the most voted option, false otherwise
     */
    public boolean isSkip() {
        return isSkip;
    }
    
    /**
     * Gets the total number of votes cast.
     *
     * @return The total number of votes
     */
    public int getTotalVotes() {
        return totalVotes;
    }
    
    /**
     * Checks if a player should be ejected based on the voting results.
     *
     * @return true if a player should be ejected, false otherwise
     */
    public boolean shouldEjectPlayer() {
        return !isTie && !isSkip;
    }
    
    /**
     * Gets the vote count for a specific player.
     *
     * @param player The player
     * @return The number of votes for the player
     */
    public int getVotesForPlayer(Player player) {
        return voteCounts.getOrDefault(String.valueOf(player.getUserId()), 0);
    }
    
    /**
     * Gets the vote count for skipping.
     *
     * @return The number of skip votes
     */
    public int getSkipVotes() {
        return voteCounts.getOrDefault("skip", 0);
    }
} 