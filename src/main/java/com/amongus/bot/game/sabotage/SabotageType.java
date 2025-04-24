package com.amongus.bot.game.sabotage;

/**
 * Enum representing different types of sabotages that impostors can perform.
 */
public enum SabotageType {
    LIGHTS("Отключение света", 
            "Игроки не могут отправлять фотографии заданий, пока свет не будет восстановлен.", 
            false),
    
    COMMUNICATIONS("Помехи связи", 
            "Игроки не могут просматривать свои задания, пока помехи не будут устранены.", 
            false),
    
    REACTOR("Авария в реакторе", 
            "Критическая ситуация! Если не починить в течение минуты, импостеры побеждают.", 
            true),
    
    OXYGEN("Утечка кислорода", 
            "Критическая ситуация! Если не починить в течение минуты, импостеры побеждают.", 
            true);
    
    private final String displayName;
    private final String description;
    private final boolean isCritical;
    
    SabotageType(String displayName, String description, boolean isCritical) {
        this.displayName = displayName;
        this.description = description;
        this.isCritical = isCritical;
    }
    
    /**
     * Gets the display name of the sabotage.
     */
    public String getDisplayName() {
        return displayName;
    }
    
    /**
     * Gets the description of the sabotage.
     */
    public String getDescription() {
        return description;
    }
    
    /**
     * Checks if the sabotage is critical (game-ending if not fixed).
     */
    public boolean isCritical() {
        return isCritical;
    }
} 