package com.amongus.bot.models;

import lombok.Data;

/**
 * Represents the settings for a game.
 */
@Data
public class GameSettings {
    private int impostorCount = Config.DEFAULT_IMPOSTOR_COUNT;
    private int discussionTimeSeconds = Config.DEFAULT_DISCUSSION_TIME_SECONDS;
    private int votingTimeSeconds = Config.DEFAULT_VOTING_TIME_SECONDS;
    private int killCooldownSeconds = Config.DEFAULT_KILL_COOLDOWN_SECONDS;
    private int tasksPerPlayer = Config.DEFAULT_TASKS_PER_PLAYER;
    
    /**
     * Sets the number of impostors with validation.
     */
    public void setImpostorCount(int count) {
        if (count >= Config.MIN_IMPOSTOR_COUNT && count <= Config.MAX_IMPOSTOR_COUNT) {
            this.impostorCount = count;
        }
    }
    
    /**
     * Sets the discussion time with validation.
     */
    public void setDiscussionTimeSeconds(int seconds) {
        if (seconds >= Config.MIN_DISCUSSION_TIME_SECONDS && seconds <= Config.MAX_DISCUSSION_TIME_SECONDS) {
            this.discussionTimeSeconds = seconds;
        }
    }
    
    /**
     * Sets the voting time with validation.
     */
    public void setVotingTimeSeconds(int seconds) {
        if (seconds >= Config.MIN_VOTING_TIME_SECONDS && seconds <= Config.MAX_VOTING_TIME_SECONDS) {
            this.votingTimeSeconds = seconds;
        }
    }
    
    /**
     * Sets the kill cooldown with validation.
     */
    public void setKillCooldownSeconds(int seconds) {
        if (seconds >= Config.MIN_KILL_COOLDOWN_SECONDS && seconds <= Config.MAX_KILL_COOLDOWN_SECONDS) {
            this.killCooldownSeconds = seconds;
        }
    }
    
    /**
     * Sets the number of tasks per player with validation.
     */
    public void setTasksPerPlayer(int count) {
        if (count >= Config.MIN_TASKS_PER_PLAYER && count <= Config.MAX_TASKS_PER_PLAYER) {
            this.tasksPerPlayer = count;
        }
    }
    
    /**
     * Gets a formatted string representation of the settings.
     */
    public String getFormattedSettings() {
        return "⚙️ *Настройки игры:*\n" +
                "🔴 Количество импостеров: " + impostorCount + "\n" +
                "💬 Время обсуждения: " + discussionTimeSeconds + " сек.\n" +
                "🗳️ Время голосования: " + votingTimeSeconds + " сек.\n" +
                "🔪 Перезарядка убийства: " + killCooldownSeconds + " сек.\n" +
                "📋 Заданий на игрока: " + tasksPerPlayer;
    }
} 