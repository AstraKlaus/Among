package com.amongus.bot.game;

import com.amongus.bot.core.AmongUsBot;
import com.amongus.bot.game.roles.Role;
import com.amongus.bot.models.Config;
import com.amongus.bot.models.Player;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.CallbackQuery;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;

import java.util.Optional;

/**
 * Represents the state when the game is actively running.
 */
public class GameRunningState extends BaseGameState {
    
    @Override
    public String getStateName() {
        return "RUNNING";
    }
    
    @Override
    public void onEnter(GameSession gameSession, AmongUsBot bot) {
        super.onEnter(gameSession, bot);
        
        // Send roles to players
        distributeRoles(gameSession, bot);
        
        // Send tasks to players
        distributeTasks(gameSession, bot);
        
        // Send game started message
        sendMessageToAllPlayers(gameSession, bot, "🎮 *Игра началась!*\n\n" +
                "Проверьте свои личные сообщения с ролью и списком заданий.\n" +
                "Мирные жители: выполняйте задания и находите импостеров.\n" +
                "Импостеры: саботируйте и убивайте мирных жителей.");
    }
    
    @Override
    public GameState handleMessage(GameSession gameSession, AmongUsBot bot, Message message) {
        // Handle text messages during the game
        String text = message.getText();
        long userId = message.getFrom().getId();
        
        Optional<Player> playerOpt = gameSession.getPlayer(userId);
        if (playerOpt.isPresent()) {
            Player player = playerOpt.get();
            
            // Only allow ghost chat for dead players
            if (!player.isAlive() && text != null && !text.startsWith("/")) {
                // Forward message to other ghosts
                String ghostMessage = "👻 *" + player.getDisplayName() + "* (призрак): " + text;
                sendMessageToGhosts(gameSession, bot, ghostMessage);
            }
        }
        
        return this;
    }
    
    @Override
    public GameState handleCallbackQuery(GameSession gameSession, AmongUsBot bot, CallbackQuery callbackQuery) {
        String data = callbackQuery.getData();
        long userId = callbackQuery.getFrom().getId();
        String chatId = callbackQuery.getMessage().getChatId().toString();
        
        Optional<Player> playerOpt = gameSession.getPlayer(userId);
        if (!playerOpt.isPresent()) {
            return this;
        }
        
        Player player = playerOpt.get();
        
        // Handle different button actions
        if (data.equals("tasks")) {
            // Show tasks list
            showTasksList(gameSession, bot, player);
        } else if (data.equals("report_body")) {
            // Report a body
            if (player.isAlive()) {
                gameSession.reportBody(player, bot);
                return new DiscussionState();
            } else {
                bot.sendTextMessageSafe(chatId, "⚠️ Призраки не могут сообщать о телах.");
            }
        } else if (data.equals("emergency_meeting")) {
            // Call emergency meeting
            if (player.isAlive() && player.getEmergencyMeetingsLeft() > 0) {
                boolean called = gameSession.callEmergencyMeeting(player, bot);
                if (called) {
                    return new DiscussionState();
                } else {
                    bot.sendTextMessageSafe(chatId, "⚠️ У вас не осталось экстренных собраний.");
                }
            } else if (!player.isAlive()) {
                bot.sendTextMessageSafe(chatId, "⚠️ Призраки не могут вызывать экстренные собрания.");
            } else {
                bot.sendTextMessageSafe(chatId, "⚠️ У вас не осталось экстренных собраний.");
            }
        } else if (data.equals("killed")) {
            // Handle "I was killed" button
            if (player.isAlive()) {
                // Confirm kill
                String confirmationCode = bot.getSecurityManager().generateConfirmationCode(player.getUserId());
                
                bot.sendTextMessageSafe(chatId, "⚠️ *Подтверждение убийства*\n\n" +
                        "Вы сообщаете, что были убиты. Это действие нельзя отменить.\n\n" +
                        "Если вы уверены, введите код подтверждения: *" + confirmationCode + "*\n\n" +
                        "Для подтверждения отправьте сообщение с текстом:\n" +
                        "/confirm " + confirmationCode);
            } else {
                bot.sendTextMessageSafe(chatId, "👻 Вы уже мертвы.");
            }
        } else if (data.equals("kill")) {
            // Handle kill button (for impostors)
            if (player.isAlive() && player.getRole().isImpostor()) {
                showPlayersToKill(gameSession, bot, player);
            }
        } else if (data.equals("sabotage")) {
            // Handle sabotage button (for impostors)
            if (player.isAlive() && player.getRole().isImpostor()) {
                showSabotageOptions(gameSession, bot, player);
            }
        } else if (data.startsWith("kill_")) {
            // Handle specific player kill
            if (player.isAlive() && player.getRole().isImpostor()) {
                String targetIdStr = data.substring(5);
                try {
                    long targetId = Long.parseLong(targetIdStr);
                    handleKillAttempt(gameSession, bot, player, targetId);
                } catch (NumberFormatException e) {
                    log.error("Invalid target ID: {}", targetIdStr);
                }
            }
        } else if (data.startsWith("sabotage_")) {
            // Handle specific sabotage type
            if (player.isAlive() && player.getRole().isImpostor()) {
                String sabotageType = data.substring(9);
                handleSabotageAttempt(gameSession, bot, player, sabotageType);
            }
        }
        
        return this;
    }
    
    @Override
    public GameState handlePhoto(GameSession gameSession, AmongUsBot bot, Message message) {
        long userId = message.getFrom().getId();
        String chatId = message.getChatId().toString();
        
        Optional<Player> playerOpt = gameSession.getPlayer(userId);
        if (playerOpt.isPresent()) {
            Player player = playerOpt.get();
            
            // Check if lights are sabotaged
            if (!gameSession.canSubmitTaskPhotos()) {
                bot.sendTextMessageSafe(chatId, "⚠️ Свет отключен! Вы не можете отправлять фотографии заданий, пока не восстановится свет.");
                return this;
            }
            
            // Handle task photo submission
            String photoFileId = message.getPhoto().get(0).getFileId();
            
            // For simplicity, we'll just mark the first incomplete task as completed
            for (var task : player.getTasks()) {
                if (!task.isCompleted()) {
                    task.complete(photoFileId);
                    
                    // Only count completed tasks for crewmates
                    if (!player.getRole().isImpostor()) {
                        gameSession.getTaskManager().recordCompletedTask();
                    }
                    
                    bot.sendTextMessageSafe(chatId, "✅ Задание \"" + task.getTitle() + "\" отмечено как выполненное!");
                    
                    // Update the task progress for all players
                    int progress = gameSession.getTaskManager().getOverallTaskCompletionPercentage();
                    sendMessageToAllPlayers(gameSession, bot, "📊 Общий прогресс заданий: " + progress + "%");
                    
                    // Check if all tasks are completed
                    if (gameSession.getTaskManager().areAllTasksCompleted()) {
                        // Crewmates win
                        gameSession.endGame("crewmates", bot);
                        return new GameEndState("crewmates");
                    }
                    
                    break;
                }
            }
        }
        
        return this;
    }
    
    @Override
    public GameState onUpdate(GameSession gameSession, AmongUsBot bot) {
        // Check win conditions
        Optional<String> winningTeam = gameSession.checkWinConditions();
        if (winningTeam.isPresent()) {
            gameSession.endGame(winningTeam.get(), bot);
            return new GameEndState(winningTeam.get());
        }
        
        // Check for active sabotage timeout
        if (gameSession.isSabotageTimedOut()) {
            // Impostors win due to critical sabotage timeout
            gameSession.endGame("impostors", bot);
            return new GameEndState("impostors");
        }
        
        return this;
    }
    
    /**
     * Distributes roles to all players.
     */
    private void distributeRoles(GameSession gameSession, AmongUsBot bot) {
        for (Player player : gameSession.getPlayers()) {
            Role role = player.getRole();
            if (role != null) {
                // Send role information
                gameSession.getPlayerChatId(player.getUserId()).ifPresent(chatId -> {
                    bot.sendTemporaryMessage(chatId, role.getRoleRevealMessage(), Config.ROLE_MESSAGE_AUTO_DELETE_SECONDS);
                });
            }
        }
    }
    
    /**
     * Distributes tasks to all players.
     */
    private void distributeTasks(GameSession gameSession, AmongUsBot bot) {
        for (Player player : gameSession.getPlayers()) {
            // Send tasks list
            showTasksList(gameSession, bot, player);
        }
    }
    
    /**
     * Shows the player's tasks list.
     */
    private void showTasksList(GameSession gameSession, AmongUsBot bot, Player player) {
        gameSession.getPlayerChatId(player.getUserId()).ifPresent(chatId -> {
            StringBuilder sb = new StringBuilder();
            sb.append("📋 *Ваши задания:*\n\n");
            
            if (player.getTasks().isEmpty()) {
                if (player.getRole().isImpostor()) {
                    sb.append("У вас нет заданий. Вы можете притворяться, что выполняете задания, чтобы не вызывать подозрений.");
                } else {
                    sb.append("У вас нет заданий. Это странно, сообщите администратору.");
                }
            } else {
                for (var task : player.getTasks()) {
                    sb.append(task.getFormattedDescription()).append("\n\n");
                }
                
                int completionPercentage = player.getTaskCompletionPercentage();
                sb.append("Ваш прогресс: ").append(completionPercentage).append("%");
                
                if (player.getRole().isImpostor()) {
                    sb.append("\n\n⚠️ *Внимание*: Как импостер, вы можете отправлять фотографии, но ваши задания не засчитываются в общий прогресс.");
                }
            }
            
            // Show overall task progress
            if (!player.getRole().isImpostor()) {
                int overallProgress = gameSession.getTaskManager().getOverallTaskCompletionPercentage();
                sb.append("\n\n📊 Общий прогресс команды: ").append(overallProgress).append("%");
            }
            
            bot.sendTextMessageSafe(chatId, sb.toString());
            
            // Send game controls
            SendMessage controlsMessage = new SendMessage();
            controlsMessage.setChatId(chatId);
            controlsMessage.setText("🎮 *Игровые кнопки:*");
            controlsMessage.enableMarkdown(true);
            
            // Get role-specific keyboard
            InlineKeyboardMarkup keyboard = player.getRole().getAbilitiesKeyboard(player);
            controlsMessage.setReplyMarkup(keyboard);
            
            bot.sendMessageSafe(controlsMessage);
        });
    }
    
    /**
     * Shows the list of players that can be killed.
     */
    private void showPlayersToKill(GameSession gameSession, AmongUsBot bot, Player player) {
        gameSession.getPlayerChatId(player.getUserId()).ifPresent(chatId -> {
            // Check kill cooldown
            if (!player.canKill()) {
                bot.sendTextMessageSafe(chatId, "⏳ Перезарядка способности убийства. Попробуйте позже.");
                return;
            }
            
            StringBuilder sb = new StringBuilder();
            sb.append("🔪 *Выберите игрока для убийства:*\n");
            
            // Create keyboard with living non-impostor players
            InlineKeyboardMarkup markup = createKillTargetsKeyboard(gameSession, player);
            
            SendMessage message = new SendMessage();
            message.setChatId(chatId);
            message.setText(sb.toString());
            message.enableMarkdown(true);
            message.setReplyMarkup(markup);
            
            bot.sendMessageSafe(message);
        });
    }
    
    /**
     * Creates a keyboard with kill targets.
     */
    private InlineKeyboardMarkup createKillTargetsKeyboard(GameSession gameSession, Player player) {
        // Implementation omitted for brevity - would create a keyboard with living non-impostor players
        return new InlineKeyboardMarkup();
    }
    
    /**
     * Shows sabotage options.
     */
    private void showSabotageOptions(GameSession gameSession, AmongUsBot bot, Player player) {
        // Implementation omitted for brevity - would show sabotage options to impostors
    }
    
    /**
     * Handles a kill attempt.
     */
    private void handleKillAttempt(GameSession gameSession, AmongUsBot bot, Player killer, long targetId) {
        // Implementation omitted for brevity - would handle the kill mechanic
    }
    
    /**
     * Handles a sabotage attempt.
     */
    private void handleSabotageAttempt(GameSession gameSession, AmongUsBot bot, Player player, String sabotageType) {
        // Implementation omitted for brevity - would handle the sabotage mechanic
    }
} 