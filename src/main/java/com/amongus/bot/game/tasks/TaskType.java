package com.amongus.bot.game.tasks;

/**
 * Enum representing different types of tasks in the game.
 */
public enum TaskType {
    SHORT("Короткое", 1),
    MEDIUM("Среднее", 2),
    LONG("Длинное", 3);
    
    private final String displayName;
    private final int complexity;
    
    TaskType(String displayName, int complexity) {
        this.displayName = displayName;
        this.complexity = complexity;
    }
    
    /**
     * Gets the display name of the task type.
     */
    public String getDisplayName() {
        return displayName;
    }
    
    /**
     * Gets the complexity level of the task type.
     */
    public int getComplexity() {
        return complexity;
    }
} 