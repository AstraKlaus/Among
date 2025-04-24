package com.amongus.bot.utils;

import org.apache.commons.lang3.RandomStringUtils;

/**
 * Utility class for generating various codes used in the game.
 */
public class CodeGenerator {
    /**
     * Characters allowed in game codes.
     */
    private static final String GAME_CODE_CHARS = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789";
    
    /**
     * Generates a random game code with the specified length.
     * 
     * @param length Length of the code
     * @return The generated code
     */
    public static String generateGameCode(int length) {
        return RandomStringUtils.random(length, GAME_CODE_CHARS);
    }
    
    /**
     * Generates a random numeric code with the specified length.
     * 
     * @param length Length of the code
     * @return The generated numeric code
     */
    public static String generateNumericCode(int length) {
        return RandomStringUtils.randomNumeric(length);
    }
    
    /**
     * Generates a random alphanumeric code with the specified length.
     * 
     * @param length Length of the code
     * @return The generated alphanumeric code
     */
    public static String generateAlphanumericCode(int length) {
        return RandomStringUtils.randomAlphanumeric(length);
    }
} 