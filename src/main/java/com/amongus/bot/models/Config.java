package com.amongus.bot.models;

/**
 * Configuration constants for the game.
 */
public class Config {

    // Bot credentials - should be loaded from environment variables in production
    public static final String BOT_USERNAME = "your_bot_username_here";
    public static final String BOT_TOKEN = "your_bot_token_here";

    // Game settings
    public static final int MIN_PLAYERS = 4;
    public static final int MAX_PLAYERS = 10;
    public static final int DEFAULT_IMPOSTOR_COUNT = 1;
    public static final int MIN_IMPOSTOR_COUNT = 1;
    public static final int MAX_IMPOSTOR_COUNT = 2;
    public static final int DEFAULT_DISCUSSION_TIME_SECONDS = 45;
    public static final int MIN_DISCUSSION_TIME_SECONDS = 30;
    public static final int MAX_DISCUSSION_TIME_SECONDS = 120;
    public static final int DEFAULT_VOTING_TIME_SECONDS = 30;
    public static final int MIN_VOTING_TIME_SECONDS = 15;
    public static final int MAX_VOTING_TIME_SECONDS = 60;
    public static final int DEFAULT_KILL_COOLDOWN_SECONDS = 30;
    public static final int MIN_KILL_COOLDOWN_SECONDS = 10;
    public static final int MAX_KILL_COOLDOWN_SECONDS = 60;
    public static final int DEFAULT_TASKS_PER_PLAYER = 5;
    public static final int MIN_TASKS_PER_PLAYER = 1;
    public static final int MAX_TASKS_PER_PLAYER = 8;
    public static final int EMERGENCY_MEETINGS_PER_PLAYER = 1;

    // Probabilities
    public static final double DEFAULT_IMPOSTOR_PROBABILITY = 0.3; // 30% chance to be impostor

    // Timeouts
    public static final long LOBBY_TIMEOUT_MINUTES = 60; // Inactive lobbies expire after 60 minutes
    public static final long GAME_TIMEOUT_MINUTES = 120; // Games expire after 120 minutes
    public static final long GAME_INACTIVE_TIMEOUT_MINUTES = 120;
    public static final int ROLE_MESSAGE_AUTO_DELETE_SECONDS = 30;

    // Message deletion delays
    public static final int SENSITIVE_INFO_DELETE_SECONDS = 10;
    public static final int NORMAL_MESSAGE_DELETE_SECONDS = 60;

    // Security
    public static final int CONFIRMATION_CODE_LENGTH = 6;
    public static final int CONFIRMATION_CODE_EXPIRY_SECONDS = 300;

    // Game code settings
    public static final int GAME_CODE_LENGTH = 5;

    // Critical sabotage timeout
    public static final int CRITICAL_SABOTAGE_TIMEOUT_SECONDS = 60;

    private Config() {
        // Private constructor to prevent instantiation
    }
} 