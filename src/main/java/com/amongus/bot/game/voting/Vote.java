package com.amongus.bot.game.voting;

/**
 * Represents a vote cast by a player during a discussion.
 */
public class Vote {
    private final String voterId;
    private final String voterName;
    private final String targetId;
    private final long timestamp;

    /**
     * Creates a new vote.
     *
     * @param voterId   The ID of the player who voted
     * @param voterName The name of the player who voted
     * @param targetId  The ID of the target (player ID or "skip")
     */
    public Vote(String voterId, String voterName, String targetId) {
        this.voterId = voterId;
        this.voterName = voterName;
        this.targetId = targetId;
        this.timestamp = System.currentTimeMillis();
    }

    /**
     * Gets the ID of the player who voted.
     *
     * @return The voter ID
     */
    public String getVoterId() {
        return voterId;
    }

    /**
     * Gets the name of the player who voted.
     *
     * @return The voter name
     */
    public String getVoterName() {
        return voterName;
    }

    /**
     * Gets the ID of the target.
     *
     * @return The target ID
     */
    public String getTargetId() {
        return targetId;
    }

    /**
     * Gets the timestamp when the vote was cast.
     *
     * @return The timestamp
     */
    public long getTimestamp() {
        return timestamp;
    }

    /**
     * Checks if this vote is a skip vote.
     *
     * @return true if this is a skip vote, false otherwise
     */
    public boolean isSkipVote() {
        return "skip".equals(targetId);
    }
} 