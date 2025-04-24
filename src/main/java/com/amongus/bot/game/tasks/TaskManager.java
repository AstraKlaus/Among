package com.amongus.bot.game.tasks;

import com.amongus.bot.models.Config;
import com.amongus.bot.models.Player;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

/**
 * Manages tasks for the game, including generation, assignment, and verification.
 */
public class TaskManager {
    private static final Logger log = LoggerFactory.getLogger(TaskManager.class);
    
    // Pre-defined tasks that can be assigned
    private final List<Task> taskPool = new ArrayList<>();
    
    // Keeps track of total task completion across all players
    private final AtomicInteger totalTasksAssigned = new AtomicInteger(0);
    private final AtomicInteger totalTasksCompleted = new AtomicInteger(0);
    
    /**
     * Initializes the task manager with default tasks.
     */
    public TaskManager() {
        initializeDefaultTasks();
    }
    
    /**
     * Initializes a pool of default tasks that can be assigned.
     */
    private void initializeDefaultTasks() {
        // Short tasks
        taskPool.add(new Task("Очистить трубу", 
                "Найдите трубу на участке и очистите ее от листьев/мусора. Сделайте фото до и после.", 
                "Ливневая труба", 
                TaskType.SHORT));
        
        taskPool.add(new Task("Выбросить мусор", 
                "Соберите мусор из одного из мусорных ведер в доме и вынесите его в общий контейнер. Сделайте фото.", 
                "Дом -> Мусорный контейнер", 
                TaskType.SHORT));
        
        taskPool.add(new Task("Разложить карты", 
                "Найдите колоду карт в доме и разложите пасьянс. Сделайте фото результата.", 
                "Гостиная", 
                TaskType.SHORT));
        
        // Medium tasks
        taskPool.add(new Task("Полить растения", 
                "Найдите лейку, наполните ее водой и полейте растения в доме или на участке. Сделайте фото.", 
                "Сад/Внутренние растения", 
                TaskType.MEDIUM));
        
        taskPool.add(new Task("Починить проводку", 
                "Найдите распределительный щиток и приведите переключатели в порядок (один должен быть выключен). Сделайте фото.", 
                "Распределительный щиток", 
                TaskType.MEDIUM));
        
        taskPool.add(new Task("Растопить печь", 
                "Сложите дрова в печь/мангал и подготовьте ее к растопке (без реального огня!). Сделайте фото.", 
                "Печь/Мангал", 
                TaskType.MEDIUM));
        
        // Long tasks
        taskPool.add(new Task("Вскопать грядку", 
                "Найдите лопату и вскопайте небольшой участок земли. Сделайте фото результата.", 
                "Огород", 
                TaskType.LONG));
        
        taskPool.add(new Task("Разгадать кроссворд", 
                "Найдите газету с кроссвордом и решите хотя бы 5 слов. Сделайте фото результата.", 
                "Газеты в гостиной", 
                TaskType.LONG));
        
        taskPool.add(new Task("Сортировка инструментов", 
                "Найдите место хранения инструментов и отсортируйте их по типу. Сделайте фото до и после.", 
                "Сарай/Кладовка", 
                TaskType.LONG));
        
        log.info("Initialized task pool with {} tasks", taskPool.size());
    }
    
    /**
     * Adds a custom task to the pool.
     */
    public void addCustomTask(Task task) {
        taskPool.add(task);
        log.info("Added custom task: {}", task.getTitle());
    }
    
    /**
     * Assigns tasks to a player.
     */
    public List<Task> assignTasksToPlayer(Player player, int count) {
        if (count < Config.MIN_TASKS_PER_PLAYER || count > Config.MAX_TASKS_PER_PLAYER) {
            count = Config.DEFAULT_TASKS_PER_PLAYER;
        }
        
        // Create a list of tasks with proper distribution of types
        List<Task> assignedTasks = new ArrayList<>();
        
        // Try to assign at least one of each type if possible
        Map<TaskType, List<Task>> tasksByType = taskPool.stream()
                .collect(Collectors.groupingBy(Task::getType));
        
        // Assign one short task if available
        if (tasksByType.containsKey(TaskType.SHORT) && !tasksByType.get(TaskType.SHORT).isEmpty()) {
            Task shortTask = getRandomTask(tasksByType.get(TaskType.SHORT));
            assignedTasks.add(cloneTask(shortTask));
        }
        
        // Assign one medium task if available
        if (tasksByType.containsKey(TaskType.MEDIUM) && !tasksByType.get(TaskType.MEDIUM).isEmpty()) {
            Task mediumTask = getRandomTask(tasksByType.get(TaskType.MEDIUM));
            assignedTasks.add(cloneTask(mediumTask));
        }
        
        // Assign one long task if available
        if (tasksByType.containsKey(TaskType.LONG) && !tasksByType.get(TaskType.LONG).isEmpty()) {
            Task longTask = getRandomTask(tasksByType.get(TaskType.LONG));
            assignedTasks.add(cloneTask(longTask));
        }
        
        // If we need more tasks, add random ones from the pool
        List<Task> allTasks = new ArrayList<>(taskPool);
        Collections.shuffle(allTasks);
        
        while (assignedTasks.size() < count && !allTasks.isEmpty()) {
            Task task = allTasks.remove(0);
            
            // Skip tasks that are already assigned
            if (assignedTasks.stream().anyMatch(t -> t.getTitle().equals(task.getTitle()))) {
                continue;
            }
            
            assignedTasks.add(cloneTask(task));
        }
        
        // Update counters
        totalTasksAssigned.addAndGet(assignedTasks.size());
        
        // Assign tasks to player
        player.assignTasks(assignedTasks);
        
        log.info("Assigned {} tasks to player {}", assignedTasks.size(), player.getUserId());
        return assignedTasks;
    }
    
    /**
     * Gets a random task from a list.
     */
    private Task getRandomTask(List<Task> tasks) {
        return tasks.get(new Random().nextInt(tasks.size()));
    }
    
    /**
     * Creates a clone of a task for assignment.
     */
    private Task cloneTask(Task original) {
        return new Task(
                original.getTitle(),
                original.getDescription(),
                original.getLocation(),
                original.getType()
        );
    }
    
    /**
     * Gets the overall task completion percentage.
     */
    public int getOverallTaskCompletionPercentage() {
        if (totalTasksAssigned.get() == 0) {
            return 0;
        }
        return (int) ((totalTasksCompleted.get() * 100.0) / totalTasksAssigned.get());
    }
    
    /**
     * Records a completed task.
     */
    public void recordCompletedTask() {
        totalTasksCompleted.incrementAndGet();
    }
    
    /**
     * Checks if all tasks are completed.
     */
    public boolean areAllTasksCompleted() {
        return totalTasksAssigned.get() > 0 && totalTasksCompleted.get() >= totalTasksAssigned.get();
    }
    
    /**
     * Resets all task tracking (for new games).
     */
    public void reset() {
        totalTasksAssigned.set(0);
        totalTasksCompleted.set(0);
    }
} 