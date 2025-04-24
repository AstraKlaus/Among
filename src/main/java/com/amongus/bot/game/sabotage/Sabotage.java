package com.amongus.bot.game.sabotage;

import com.amongus.bot.models.Config;
import com.amongus.bot.models.Player;
import lombok.Data;

import java.time.Duration;
import java.time.Instant;
import java.util.HashSet;
import java.util.Set;

/**
 * Represents an active sabotage in the game.
 */
@Data
public class Sabotage {
    private final SabotageType type;
    private final Instant startTime;
    private Instant fixedTime;
    private final Player initiator;
    private final long gameId;
    private final Set<Long> fixedByUserIds = new HashSet<>();
    private final Set<String> requiredLocations = new HashSet<>();
    private boolean fixed = false;
    
    /**
     * Creates a new sabotage of the specified type.
     */
    public Sabotage(SabotageType type, Player initiator, long gameId) {
        this.type = type;
        this.initiator = initiator;
        this.gameId = gameId;
        this.startTime = Instant.now();
        
        // Set required locations for fixing based on sabotage type
        setupRequiredLocations();
    }
    
    /**
     * Sets up the required locations for fixing this sabotage.
     */
    private void setupRequiredLocations() {
        switch (type) {
            case LIGHTS:
                requiredLocations.add("Электрощит");
                break;
            case COMMUNICATIONS:
                requiredLocations.add("Пункт связи");
                break;
            case REACTOR:
                requiredLocations.add("Реактор (левая сторона)");
                requiredLocations.add("Реактор (правая сторона)");
                break;
            case OXYGEN:
                requiredLocations.add("Кислородный баллон (дом)");
                requiredLocations.add("Кислородный баллон (сад)");
                break;
        }
    }
    
    /**
     * Gets the time remaining before a critical sabotage ends the game.
     * 
     * @return remaining time in seconds, or 0 if not a critical sabotage or already fixed
     */
    public long getRemainingTimeSeconds() {
        if (!type.isCritical() || fixed) {
            return 0;
        }
        
        Instant now = Instant.now();
        Duration elapsed = Duration.between(startTime, now);
        long remainingSeconds = Config.CRITICAL_SABOTAGE_TIMEOUT_SECONDS - elapsed.getSeconds();
        
        return Math.max(0, remainingSeconds);
    }
    
    /**
     * Checks if this critical sabotage has timed out.
     * 
     * @return true if the sabotage is critical, not fixed, and has timed out
     */
    public boolean isTimedOut() {
        return type.isCritical() && !fixed && getRemainingTimeSeconds() <= 0;
    }
    
    /**
     * Records that a player has contributed to fixing the sabotage.
     * 
     * @param player the player who is fixing
     * @param location the location they're fixing
     * @return true if the sabotage is now completely fixed
     */
    public boolean recordFix(Player player, String location) {
        if (fixed) {
            return true;
        }
        
        // Record that this player has attempted to fix
        fixedByUserIds.add(player.getUserId());
        
        // For now, just consider it fixed if at least 2 different players have tried to fix
        // In a real implementation, we would need to match locations and possibly other criteria
        if (fixedByUserIds.size() >= 2) {
            fixed = true;
            fixedTime = Instant.now();
        }
        
        return fixed;
    }
    
    /**
     * Gets the formatted message describing this sabotage.
     */
    public String getFormattedMessage() {
        StringBuilder sb = new StringBuilder();
        
        if (type.isCritical()) {
            sb.append("‼️ *КРИТИЧЕСКИЙ САБОТАЖ* ‼️\n");
        } else {
            sb.append("⚠️ *САБОТАЖ* ⚠️\n");
        }
        
        sb.append("*").append(type.getDisplayName()).append("*\n");
        sb.append(type.getDescription()).append("\n\n");
        
        if (type.isCritical() && !fixed) {
            sb.append("⏱ Осталось времени: ").append(getRemainingTimeSeconds()).append(" сек.\n\n");
        }
        
        sb.append("📍 Необходимо починить в следующих местах:\n");
        for (String location : requiredLocations) {
            sb.append("- ").append(location).append("\n");
        }
        
        return sb.toString();
    }
} 