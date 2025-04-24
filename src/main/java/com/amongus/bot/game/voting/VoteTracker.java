package com.amongus.bot.game.voting;

import java.util.*;

/**
 * Tracks votes during the discussion phase.
 */
public class VoteTracker {
    
    // Maps voter ID to vote
    private final Map<String, Vote> votes;
    
    // List of all voters (with name for display purposes)
    private final Map<String, String> voters;
    
    public VoteTracker() {
        this.votes = new HashMap<>();
        this.voters = new HashMap<>();
    }
    
    /**
     * Adds a voter to the tracker.
     * 
     * @param voterId The ID of the voter
     * @param voterName The name of the voter for display
     */
    public void addVoter(long voterId, String voterName) {
        this.voters.put(String.valueOf(voterId), voterName);
    }
    
    /**
     * Records a vote from a voter to a target.
     * 
     * @param voterId The ID of the voter
     * @param targetId The ID of the target (can be "skip" for skipping)
     * @return true if the vote was recorded, false if the voter is not eligible
     */
    public boolean vote(long voterId, String targetId) {
        String voterIdStr = String.valueOf(voterId);
        
        if (!voters.containsKey(voterIdStr)) {
            return false;
        }
        
        String voterName = voters.get(voterIdStr);
        Vote vote = new Vote(voterIdStr, voterName, targetId);
        votes.put(voterIdStr, vote);
        return true;
    }
    
    /**
     * Checks if everyone has voted.
     * 
     * @return true if all registered voters have voted
     */
    public boolean hasEveryoneVoted() {
        return votes.size() >= voters.size();
    }
    
    /**
     * Gets a list of voters who have not yet voted.
     * 
     * @return List of voter IDs who haven't voted
     */
    public List<String> getNonVoters() {
        List<String> nonVoters = new ArrayList<>();
        
        for (String voterId : voters.keySet()) {
            if (!votes.containsKey(voterId)) {
                nonVoters.add(voterId);
            }
        }
        
        return nonVoters;
    }
    
    /**
     * Gets the vote counts for each target.
     * 
     * @return Map of target ID to vote count
     */
    public Map<String, Integer> getVoteCounts() {
        Map<String, Integer> voteCounts = new HashMap<>();
        
        for (Vote vote : votes.values()) {
            String targetId = vote.getTargetId();
            voteCounts.put(targetId, voteCounts.getOrDefault(targetId, 0) + 1);
        }
        
        return voteCounts;
    }
    
    /**
     * Gets the player with the most votes.
     * 
     * @return ID of the most voted player, or null if there's a tie
     */
    public String getMostVotedPlayer() {
        Map<String, Integer> voteCounts = getVoteCounts();
        
        if (voteCounts.isEmpty()) {
            return null;
        }
        
        // Find the maximum vote count
        int maxVotes = Collections.max(voteCounts.values());
        
        // Find all players with the maximum vote count
        List<String> mostVotedPlayers = new ArrayList<>();
        for (Map.Entry<String, Integer> entry : voteCounts.entrySet()) {
            if (entry.getValue() == maxVotes) {
                mostVotedPlayers.add(entry.getKey());
            }
        }
        
        // If there's a tie, return null
        if (mostVotedPlayers.size() > 1) {
            return null;
        }
        
        return mostVotedPlayers.get(0);
    }
    
    /**
     * Gets the vote of a specific voter.
     * 
     * @param voterId The ID of the voter
     * @return The vote cast by the voter, or null if they haven't voted
     */
    public Vote getVote(long voterId) {
        return votes.get(String.valueOf(voterId));
    }
    
    /**
     * Gets the target ID voted for by a specific voter.
     * 
     * @param voterId The ID of the voter
     * @return The ID of the target they voted for, or null if they haven't voted
     */
    public String getVoteTargetId(long voterId) {
        Vote vote = getVote(voterId);
        return vote != null ? vote.getTargetId() : null;
    }
    
    /**
     * Gets all votes.
     * 
     * @return Collection of all votes
     */
    public Collection<Vote> getAllVotes() {
        return votes.values();
    }
    
    /**
     * Clears all votes.
     */
    public void clearVotes() {
        votes.clear();
    }
} 