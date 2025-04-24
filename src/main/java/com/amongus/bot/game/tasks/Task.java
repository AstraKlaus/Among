package com.amongus.bot.game.tasks;

import lombok.Data;

import java.time.Instant;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Represents a task in the game that players need to complete.
 */
@Data
public class Task {
    // Used to generate unique task IDs
    private static final AtomicLong ID_GENERATOR = new AtomicLong(1);
    
    // Task metadata
    private final long id;
    private final String title;
    private final String description;
    private final String location;
    private final TaskType type;
    
    // Task state
    private boolean completed = false;
    private Instant completionTime;
    private String photoFileId; // ID of the photo submitted as proof
    private boolean verified = false; // Whether the admin has verified this task
    
    /**
     * Creates a new task with the specified details.
     */
    public Task(String title, String description, String location, TaskType type) {
        this.id = ID_GENERATOR.getAndIncrement();
        this.title = title;
        this.description = description;
        this.location = location;
        this.type = type;
    }
    
    /**
     * Marks the task as completed.
     */
    public void complete() {
        this.completed = true;
        this.completionTime = Instant.now();
    }
    
    /**
     * Marks the task as completed with photo proof.
     */
    public void complete(String photoFileId) {
        this.completed = true;
        this.completionTime = Instant.now();
        this.photoFileId = photoFileId;
    }
    
    /**
     * Verifies the task as properly completed by an admin.
     */
    public void verify() {
        this.verified = true;
    }
    
    /**
     * Rejects the task completion (admin determines it wasn't properly completed).
     */
    public void reject() {
        this.completed = false;
        this.completionTime = null;
        this.photoFileId = null;
        this.verified = false;
    }
    
    /**
     * Gets a formatted description of the task.
     */
    public String getFormattedDescription() {
        String status = completed ? "✅" : "⬜";
        return String.format("%s *%s* (%s)\n📌 %s\n%s",
                status, title, type.getDisplayName(), location, description);
    }
    
    /**
     * Gets a short description of the task (for lists).
     */
    public String getShortDescription() {
        String status = completed ? "✅" : "⬜";
        return String.format("%s *%s* - %s", status, title, location);
    }
} 