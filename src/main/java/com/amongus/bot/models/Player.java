package com.amongus.bot.models;

import com.amongus.bot.game.roles.Role;
import com.amongus.bot.game.tasks.Task;
import lombok.Data;
import org.telegram.telegrambots.meta.api.objects.User;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import com.amongus.bot.models.Config;

/**
 * Represents a player in the game.
 */
@Data
public class Player {
    // Telegram user data
    private final long userId;
    private final String username;
    private final String firstName;
    private final String lastName;
    private long chatId;
    
    // Game state
    private Role role;
    private boolean ready = false;
    private boolean alive = true;
    private List<Task> tasks = new ArrayList<>();
    private AtomicInteger completedTasks = new AtomicInteger(0);
    private int emergencyMeetingsLeft = Config.EMERGENCY_MEETINGS_PER_PLAYER;
    private Instant lastActivity = Instant.now();
    private Instant lastKillTime;
    private int killCooldownSeconds = Config.DEFAULT_KILL_COOLDOWN_SECONDS;
    
    /**
     * Creates a new Player from a Telegram User.
     */
    public static Player fromTelegramUser(User user) {
        return new Player(
                user.getId(),
                user.getUserName(),
                user.getFirstName(),
                user.getLastName()
        );
    }
    
    /**
     * Constructor with all required fields.
     */
    public Player(long userId, String username, String firstName, String lastName) {
        this.userId = userId;
        this.username = username;
        this.firstName = firstName != null ? firstName : "";
        this.lastName = lastName != null ? lastName : "";
    }
    
    /**
     * Get the player's display name (username if available, or first name).
     */
    public String getDisplayName() {
        if (username != null && !username.isEmpty()) {
            return "@" + username;
        } else if (!firstName.isEmpty()) {
            return firstName;
        } else {
            return "Player " + userId;
        }
    }
    
    /**
     * Marks the player as killed.
     */
    public void kill() {
        this.alive = false;
    }
    
    /**
     * Revives the player (for new games).
     */
    public void revive() {
        this.alive = true;
    }
    
    /**
     * Assigns a role to the player.
     */
    public void assignRole(Role role) {
        this.role = role;
        this.ready = false;
        this.alive = true;
        this.tasks.clear();
        this.completedTasks.set(0);
        this.emergencyMeetingsLeft = Config.EMERGENCY_MEETINGS_PER_PLAYER;
        this.lastKillTime = null;
    }
    
    /**
     * Assigns tasks to the player.
     */
    public void assignTasks(List<Task> tasks) {
        this.tasks = new ArrayList<>(tasks);
        this.completedTasks.set(0);
    }
    
    /**
     * Completes a task.
     */
    public boolean completeTask(long taskId) {
        // Find the task with the given ID
        for (Task task : tasks) {
            if (task.getId() == taskId && !task.isCompleted()) {
                task.complete();
                completedTasks.incrementAndGet();
                return true;
            }
        }
        return false;
    }
    
    /**
     * Checks if this player can kill at the current instant.
     * 
     * @param instant The current timestamp
     * @return true if the player can kill, false otherwise
     */
    public boolean canKill(java.time.Instant instant) {
        // Only impostors can kill
        if (!isAlive() || !getRole().isImpostor()) {
            return false;
        }
        
        // Check if the kill cooldown has passed
        return getLastKillTime() == null || 
               instant.isAfter(getLastKillTime().plus(java.time.Duration.ofSeconds(getKillCooldownSeconds())));
    }
    
    /**
     * Gets the player's last kill time.
     * 
     * @return The last kill time, or null if the player hasn't killed
     */
    public java.time.Instant getLastKillTime() {
        return lastKillTime;
    }
    
    /**
     * Sets the player's last kill time.
     * 
     * @param lastKillTime The new last kill time
     */
    public void setLastKillTime(java.time.Instant lastKillTime) {
        this.lastKillTime = lastKillTime;
    }
    
    /**
     * Gets the kill cooldown in seconds.
     * 
     * @return The kill cooldown in seconds
     */
    public int getKillCooldownSeconds() {
        return killCooldownSeconds;
    }
    
    /**
     * Sets the kill cooldown in seconds.
     * 
     * @param killCooldownSeconds The new kill cooldown in seconds
     */
    public void setKillCooldownSeconds(int killCooldownSeconds) {
        this.killCooldownSeconds = killCooldownSeconds;
    }
    
    /**
     * Updates the player's last activity time.
     */
    public void updateActivity() {
        this.lastActivity = Instant.now();
    }
    
    /**
     * Gets the task completion percentage.
     */
    public int getTaskCompletionPercentage() {
        if (tasks.isEmpty()) {
            return 100; // No tasks to complete
        }
        return (int) ((completedTasks.get() * 100.0) / tasks.size());
    }
    
    /**
     * Uses an emergency meeting.
     * 
     * @return true if player had emergency meetings left and one was used
     */
    public boolean useEmergencyMeeting() {
        if (emergencyMeetingsLeft > 0) {
            emergencyMeetingsLeft--;
            return true;
        }
        return false;
    }
    
    /**
     * Gets the player's role.
     * 
     * @return The player's role
     */
    public Role getRole() {
        return role;
    }
    
    /**
     * Sets the player's role.
     * 
     * @param role The new role
     */
    public void setRole(Role role) {
        this.role = role;
    }

    public void setChatId(long chatId) {
        this.chatId = chatId;
    }

    public long getChatId() {
        return chatId;
    }
} 