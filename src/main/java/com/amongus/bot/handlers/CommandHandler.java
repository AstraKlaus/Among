package com.amongus.bot.handlers;

import com.amongus.bot.commands.*;
import com.amongus.bot.core.AmongUsBot;
import com.amongus.bot.commands.*;
import com.amongus.bot.core.SessionManager;
import com.amongus.bot.game.lobby.GameLobby;
import com.amongus.bot.models.Config;
import com.amongus.bot.models.GameSettings;
import com.amongus.bot.models.Player;
import com.amongus.bot.utils.SecurityManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.api.objects.User;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Handles command messages from users.
 */
public class CommandHandler {
    private static final Logger log = LoggerFactory.getLogger(CommandHandler.class);
    
    private final AmongUsBot bot;
    private final SessionManager sessionManager;
    private final SecurityManager securityManager;
    private final Map<String, Command> commands;
    
    /**
     * Creates a new command handler.
     */
    public CommandHandler(AmongUsBot bot, SessionManager sessionManager, SecurityManager securityManager) {
        this.bot = bot;
        this.sessionManager = sessionManager;
        this.securityManager = securityManager;
        // Initialize commands
        this.commands = new HashMap<>();
        registerCommands();
    }
    
    /**
     * Registers all available commands.
     */
    private void registerCommands() {
        // Game creation commands
        registerCommand(new StartCommand(bot, sessionManager, securityManager));
        registerCommand(new JoinCommand(bot, sessionManager, securityManager));
        registerCommand(new LeaveCommand(bot, sessionManager, securityManager));
        registerCommand(new StartGameCommand(bot, sessionManager, securityManager));
        
        // Information commands
        registerCommand(new HelpCommand(bot, sessionManager, securityManager));
        registerCommand(new PlayersCommand(bot, sessionManager, securityManager));
        registerCommand(new RulesCommand(bot, sessionManager, securityManager));
        
        // Admin commands
        registerCommand(new EndGameCommand(bot, sessionManager, securityManager));
        registerCommand(new KickCommand(bot, sessionManager, securityManager));
    }
    
    /**
     * Registers a command.
     */
    private void registerCommand(Command command) {
        commands.put(command.getName(), command);
        for (String alias : command.getAliases()) {
            commands.put(alias, command);
        }
    }
    
    /**
     * Handles a command message.
     */
    public void handleCommand(Message message) {
        String text = message.getText();
        String[] parts = text.split("\\s+", 2);
        String commandName = parts[0].substring(1).toLowerCase(); // Remove the leading '/'
        String args = parts.length > 1 ? parts[1] : "";
        
        Command command = commands.get(commandName);
        
        if (command != null) {
            try {
                log.debug("Executing command: {} from user {}", commandName, message.getFrom().getId());
                command.execute(message, args);
            } catch (Exception e) {
                log.error("Error executing command {}: {}", commandName, e.getMessage(), e);
                sendErrorMessage(message.getChatId(), "An error occurred while executing the command.");
            }
        } else {
            log.debug("Unknown command: {} from user {}", commandName, message.getFrom().getId());
            sendErrorMessage(message.getChatId(), "Unknown command. Type /help for a list of available commands.");
        }
    }
    
    /**
     * Sends an error message to the user.
     */
    private void sendErrorMessage(Long chatId, String text) {
        SendMessage message = new SendMessage();
        message.setChatId(chatId.toString());
        message.setText(text);
        
        try {
            bot.execute(message);
        } catch (TelegramApiException e) {
            log.error("Failed to send error message: {}", e.getMessage(), e);
        }
    }
    
    /**
     * Handles the /start command.
     */
    private void handleStartCommand(String chatId) {
        SendMessage message = new SendMessage();
        message.setChatId(chatId);
        message.setText("Добро пожаловать в игру Among Us для дачи!\n\n" +
                "Используйте команду /create для создания новой игры или /join для присоединения к существующей.\n\n" +
                "Для получения списка всех команд используйте /help");
        
        bot.sendMessageSafe(message);
    }
    
    /**
     * Handles the /help command.
     */
    private void handleHelpCommand(String chatId) {
        SendMessage message = new SendMessage();
        message.setChatId(chatId);
        message.setText("*Доступные команды:*\n\n" +
                "/create - Создать новую игру\n" +
                "/join XXXXX - Присоединиться к игре по коду\n" +
                "/leave - Покинуть текущую игру\n" +
                "/ready - Отметиться как готовый к игре\n" +
                "/settings - Настройки игры (только для владельца лобби)\n" +
                "/lobby - Информация о текущем лобби\n" +
                "/help - Показать этот текст\n\n" +
                "*Как играть:*\n" +
                "1. Создайте лобби или присоединитесь к существующему\n" +
                "2. Когда все участники готовы, игра начнется автоматически\n" +
                "3. Выполняйте задания, которые вам будут выданы\n" +
                "4. Импостеры должны незаметно убивать членов экипажа\n" +
                "5. Вызывайте экстренные собрания для обсуждения и голосования");
        message.enableMarkdown(true);
        
        bot.sendMessageSafe(message);
    }
    
    /**
     * Handles the /create command.
     */
    private void handleCreateCommand(String chatId, Player player) {
        // Store the player's chat ID for direct messages
        player.updateActivity();
        
        // Create new lobby
        String lobbyCode = sessionManager.createLobby(player);
        
        // Create an inline keyboard with the Ready button
        InlineKeyboardMarkup markup = new InlineKeyboardMarkup();
        List<List<InlineKeyboardButton>> keyboard = new ArrayList<>();
        
        List<InlineKeyboardButton> row = new ArrayList<>();
        InlineKeyboardButton readyButton = new InlineKeyboardButton();
        readyButton.setText("Готов");
        readyButton.setCallbackData("ready");
        row.add(readyButton);
        
        keyboard.add(row);
        markup.setKeyboard(keyboard);
        
        // Send the welcome message
        SendMessage message = new SendMessage();
        message.setChatId(chatId);
        message.setText("🎮 Создано новое лобби!\n\n" +
                "Код лобби: *" + lobbyCode + "*\n\n" +
                "Поделитесь этим кодом с другими игроками, чтобы они могли присоединиться с помощью команды:\n" +
                "/join " + lobbyCode + "\n\n" +
                "Когда все будут готовы, игра начнется автоматически.");
        message.enableMarkdown(true);
        message.setReplyMarkup(markup);
        
        bot.sendMessageSafe(message);
        
        // Update the player's chat ID in the session
        Optional<GameLobby> lobby = sessionManager.getLobbyByCode(lobbyCode);
        lobby.ifPresent(l -> {
            l.getPlayer(player.getUserId()).ifPresent(p -> {
                // Update chat ID for direct messages
                sessionManager.updatePlayerChatId(p.getUserId(), chatId);
            });
        });
    }
    
    /**
     * Handles the /join command.
     */
    private void handleJoinCommand(String chatId, Player player, String[] args) {
        if (args.length == 0) {
            SendMessage message = new SendMessage();
            message.setChatId(chatId);
            message.setText("Пожалуйста, укажите код лобби:\n/join XXXXX");
            bot.sendMessageSafe(message);
            return;
        }
        
        String lobbyCode = args[0].toUpperCase();
        player.updateActivity();
        
        // Try to join the lobby
        boolean joined = sessionManager.joinLobby(lobbyCode, player);
        
        if (joined) {
            // Create an inline keyboard with the Ready button
            InlineKeyboardMarkup markup = new InlineKeyboardMarkup();
            List<List<InlineKeyboardButton>> keyboard = new ArrayList<>();
            
            List<InlineKeyboardButton> row = new ArrayList<>();
            InlineKeyboardButton readyButton = new InlineKeyboardButton();
            readyButton.setText("Готов");
            readyButton.setCallbackData("ready");
            row.add(readyButton);
            
            keyboard.add(row);
            markup.setKeyboard(keyboard);
            
            // Get the lobby information
            Optional<GameLobby> lobbyOpt = sessionManager.getLobbyByCode(lobbyCode);
            String playerStatus = lobbyOpt.map(GameLobby::getPlayerStatusString).orElse("");
            
            // Send the welcome message
            SendMessage message = new SendMessage();
            message.setChatId(chatId);
            message.setText("✅ Вы присоединились к лобби *" + lobbyCode + "*!\n\n" +
                    playerStatus + "\n" +
                    "Нажмите кнопку \"Готов\", когда будете готовы к игре.");
            message.enableMarkdown(true);
            message.setReplyMarkup(markup);
            
            bot.sendMessageSafe(message);
            
            // Update the player's chat ID in the session
            sessionManager.updatePlayerChatId(player.getUserId(), chatId);
            
            // Notify other players about the new player
            lobbyOpt.ifPresent(lobby -> {
                for (Player p : lobby.getPlayers()) {
                    if (p.getUserId() != player.getUserId()) {
                        Optional<String> playerChatId = sessionManager.getPlayerChatId(p.getUserId());
                        playerChatId.ifPresent(pcid -> {
                            SendMessage notifyMessage = new SendMessage();
                            notifyMessage.setChatId(pcid);
                            notifyMessage.setText("🔔 " + player.getDisplayName() + " присоединился к лобби!\n\n" +
                                    playerStatus);
                            notifyMessage.enableMarkdown(true);
                            bot.sendMessageSafe(notifyMessage);
                        });
                    }
                }
            });
        } else {
            // Send error message
            SendMessage message = new SendMessage();
            message.setChatId(chatId);
            message.setText("❌ Не удалось присоединиться к лобби с кодом *" + lobbyCode + "*!\n" +
                    "Возможные причины:\n" +
                    "- Неверный код лобби\n" +
                    "- Лобби заполнено\n" +
                    "- Игра уже началась");
            message.enableMarkdown(true);
            bot.sendMessageSafe(message);
        }
    }
    
    /**
     * Handles the /leave command.
     */
    private void handleLeaveCommand(String chatId, Player player) {
        // Try to leave the current lobby
        boolean left = sessionManager.removePlayerFromLobby(player.getUserId());
        
        if (left) {
            SendMessage message = new SendMessage();
            message.setChatId(chatId);
            message.setText("👋 Вы покинули лобби.");
            bot.sendMessageSafe(message);
        } else {
            SendMessage message = new SendMessage();
            message.setChatId(chatId);
            message.setText("❌ Вы не находитесь в лобби.");
            bot.sendMessageSafe(message);
        }
    }
    
    /**
     * Handles the /ready command.
     */
    private void handleReadyCommand(String chatId, Player player) {
        Optional<GameLobby> lobbyOpt = sessionManager.getLobbyForPlayer(player.getUserId());
        
        if (lobbyOpt.isPresent()) {
            GameLobby lobby = lobbyOpt.get();
            
            // Mark the player as ready
            Optional<Player> playerOpt = lobby.getPlayer(player.getUserId());
            if (playerOpt.isPresent()) {
                Player p = playerOpt.get();
                p.setReady(true);
                lobby.recordActivity();
                
                // Get updated player status
                String playerStatus = lobby.getPlayerStatusString();
                
                // Send confirmation message
                SendMessage message = new SendMessage();
                message.setChatId(chatId);
                message.setText("✅ Вы отмечены как готовый к игре!\n\n" + playerStatus);
                message.enableMarkdown(true);
                bot.sendMessageSafe(message);
                
                // Notify other players
                for (Player otherPlayer : lobby.getPlayers()) {
                    if (otherPlayer.getUserId() != player.getUserId()) {
                        Optional<String> playerChatId = sessionManager.getPlayerChatId(otherPlayer.getUserId());
                        playerChatId.ifPresent(pcid -> {
                            SendMessage notifyMessage = new SendMessage();
                            notifyMessage.setChatId(pcid);
                            notifyMessage.setText("🔔 " + player.getDisplayName() + " готов к игре!\n\n" + playerStatus);
                            notifyMessage.enableMarkdown(true);
                            bot.sendMessageSafe(notifyMessage);
                        });
                    }
                }
                
                // Check if all players are ready to start the game
                if (lobby.areAllPlayersReady() && lobby.getPlayers().size() >= Config.MIN_PLAYERS) {
                    // Send game starting message to all players
                    for (Player readyPlayer : lobby.getPlayers()) {
                        Optional<String> playerChatId = sessionManager.getPlayerChatId(readyPlayer.getUserId());
                        playerChatId.ifPresent(pcid -> {
                            SendMessage startMessage = new SendMessage();
                            startMessage.setChatId(pcid);
                            startMessage.setText("🚀 Все игроки готовы! Игра начинается...");
                            bot.sendMessageSafe(startMessage);
                        });
                    }
                    
                    // Start the game
                    sessionManager.startGame(lobby.getLobbyCode());
                }
            }
        } else {
            SendMessage message = new SendMessage();
            message.setChatId(chatId);
            message.setText("❌ Вы не находитесь в лобби.");
            bot.sendMessageSafe(message);
        }
    }
    
    /**
     * Handles the /settings command.
     */
    private void handleSettingsCommand(String chatId, Player player) {
        Optional<GameLobby> lobbyOpt = sessionManager.getLobbyForPlayer(player.getUserId());
        
        if (lobbyOpt.isPresent()) {
            GameLobby lobby = lobbyOpt.get();
            
            // Check if the player is the owner
            if (lobby.isOwner(player.getUserId())) {
                GameSettings settings = lobby.getSettings();
                
                // Create settings keyboard
                InlineKeyboardMarkup markup = createSettingsKeyboard(settings);
                
                // Send settings message
                SendMessage message = new SendMessage();
                message.setChatId(chatId);
                message.setText(settings.getFormattedSettings() + "\n\n" +
                        "Используйте кнопки ниже для изменения настроек.");
                message.enableMarkdown(true);
                message.setReplyMarkup(markup);
                
                bot.sendMessageSafe(message);
            } else {
                SendMessage message = new SendMessage();
                message.setChatId(chatId);
                message.setText("❌ Только создатель лобби может изменять настройки.");
                bot.sendMessageSafe(message);
            }
        } else {
            SendMessage message = new SendMessage();
            message.setChatId(chatId);
            message.setText("❌ Вы не находитесь в лобби.");
            bot.sendMessageSafe(message);
        }
    }
    
    /**
     * Creates the settings keyboard.
     */
    private InlineKeyboardMarkup createSettingsKeyboard(GameSettings settings) {
        InlineKeyboardMarkup markup = new InlineKeyboardMarkup();
        List<List<InlineKeyboardButton>> keyboard = new ArrayList<>();
        
        // Impostor count row
        List<InlineKeyboardButton> impostorRow = new ArrayList<>();
        
        InlineKeyboardButton impostorMinus = new InlineKeyboardButton();
        impostorMinus.setText("➖");
        impostorMinus.setCallbackData("settings_impostor_minus");
        impostorRow.add(impostorMinus);
        
        InlineKeyboardButton impostorCount = new InlineKeyboardButton();
        impostorCount.setText("Импостеры: " + settings.getImpostorCount());
        impostorCount.setCallbackData("settings_impostor_info");
        impostorRow.add(impostorCount);
        
        InlineKeyboardButton impostorPlus = new InlineKeyboardButton();
        impostorPlus.setText("➕");
        impostorPlus.setCallbackData("settings_impostor_plus");
        impostorRow.add(impostorPlus);
        
        keyboard.add(impostorRow);
        
        // Discussion time row
        List<InlineKeyboardButton> discussionRow = new ArrayList<>();
        
        InlineKeyboardButton discussionMinus = new InlineKeyboardButton();
        discussionMinus.setText("➖");
        discussionMinus.setCallbackData("settings_discussion_minus");
        discussionRow.add(discussionMinus);
        
        InlineKeyboardButton discussionTime = new InlineKeyboardButton();
        discussionTime.setText("Обсуждение: " + settings.getDiscussionTimeSeconds() + "с");
        discussionTime.setCallbackData("settings_discussion_info");
        discussionRow.add(discussionTime);
        
        InlineKeyboardButton discussionPlus = new InlineKeyboardButton();
        discussionPlus.setText("➕");
        discussionPlus.setCallbackData("settings_discussion_plus");
        discussionRow.add(discussionPlus);
        
        keyboard.add(discussionRow);
        
        // Voting time row
        List<InlineKeyboardButton> votingRow = new ArrayList<>();
        
        InlineKeyboardButton votingMinus = new InlineKeyboardButton();
        votingMinus.setText("➖");
        votingMinus.setCallbackData("settings_voting_minus");
        votingRow.add(votingMinus);
        
        InlineKeyboardButton votingTime = new InlineKeyboardButton();
        votingTime.setText("Голосование: " + settings.getVotingTimeSeconds() + "с");
        votingTime.setCallbackData("settings_voting_info");
        votingRow.add(votingTime);
        
        InlineKeyboardButton votingPlus = new InlineKeyboardButton();
        votingPlus.setText("➕");
        votingPlus.setCallbackData("settings_voting_plus");
        votingRow.add(votingPlus);
        
        keyboard.add(votingRow);
        
        // Kill cooldown row
        List<InlineKeyboardButton> killCooldownRow = new ArrayList<>();
        
        InlineKeyboardButton killCooldownMinus = new InlineKeyboardButton();
        killCooldownMinus.setText("➖");
        killCooldownMinus.setCallbackData("settings_killcooldown_minus");
        killCooldownRow.add(killCooldownMinus);
        
        InlineKeyboardButton killCooldown = new InlineKeyboardButton();
        killCooldown.setText("Перезарядка убийства: " + settings.getKillCooldownSeconds() + "с");
        killCooldown.setCallbackData("settings_killcooldown_info");
        killCooldownRow.add(killCooldown);
        
        InlineKeyboardButton killCooldownPlus = new InlineKeyboardButton();
        killCooldownPlus.setText("➕");
        killCooldownPlus.setCallbackData("settings_killcooldown_plus");
        killCooldownRow.add(killCooldownPlus);
        
        keyboard.add(killCooldownRow);
        
        // Tasks per player row
        List<InlineKeyboardButton> tasksRow = new ArrayList<>();
        
        InlineKeyboardButton tasksMinus = new InlineKeyboardButton();
        tasksMinus.setText("➖");
        tasksMinus.setCallbackData("settings_tasks_minus");
        tasksRow.add(tasksMinus);
        
        InlineKeyboardButton tasks = new InlineKeyboardButton();
        tasks.setText("Задания на игрока: " + settings.getTasksPerPlayer());
        tasks.setCallbackData("settings_tasks_info");
        tasksRow.add(tasks);
        
        InlineKeyboardButton tasksPlus = new InlineKeyboardButton();
        tasksPlus.setText("➕");
        tasksPlus.setCallbackData("settings_tasks_plus");
        tasksRow.add(tasksPlus);
        
        keyboard.add(tasksRow);
        
        markup.setKeyboard(keyboard);
        return markup;
    }
    
    /**
     * Handles the /lobby command.
     */
    private void handleLobbyCommand(String chatId, Player player) {
        Optional<GameLobby> lobbyOpt = sessionManager.getLobbyForPlayer(player.getUserId());
        
        if (lobbyOpt.isPresent()) {
            GameLobby lobby = lobbyOpt.get();
            
            // Send lobby information
            SendMessage message = new SendMessage();
            message.setChatId(chatId);
            message.setText("🎮 *Информация о лобби*\n\n" +
                    "Код: *" + lobby.getLobbyCode() + "*\n" +
                    "Владелец: " + lobby.getOwner().getDisplayName() + "\n\n" +
                    lobby.getSettings().getFormattedSettings() + "\n\n" +
                    lobby.getPlayerStatusString());
            message.enableMarkdown(true);
            
            bot.sendMessageSafe(message);
        } else {
            SendMessage message = new SendMessage();
            message.setChatId(chatId);
            message.setText("❌ Вы не находитесь в лобби.");
            bot.sendMessageSafe(message);
        }
    }
} 