package com.amongus.bot.game.states;

import com.amongus.bot.core.AmongUsBot;
import com.amongus.bot.models.Config;
import com.amongus.bot.models.Player;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.CallbackQuery;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Represents the lobby state where players join and prepare for the game.
 */
public class LobbyState extends BaseGameState {
    
    @Override
    public String getStateName() {
        return "LOBBY";
    }
    
    @Override
    public void onEnter(GameSession gameSession, AmongUsBot bot) {
        super.onEnter(gameSession, bot);
        
        // Send lobby information to all players
        sendLobbyInfoToAllPlayers(gameSession, bot);
    }
    
    @Override
    public GameState handleMessage(GameSession gameSession, AmongUsBot bot, Message message) {
        String text = message.getText();
        String chatId = message.getChatId().toString();
        long userId = message.getFrom().getId();
        
        // Handle chat messages in lobby
        if (text != null && !text.startsWith("/")) {
            // Find the player
            Optional<Player> playerOpt = gameSession.getPlayer(userId);
            
            if (playerOpt.isPresent()) {
                // Forward lobby chat message to all players
                Player sender = playerOpt.get();
                String lobbyMessage = "💬 *" + sender.getDisplayName() + "*: " + text;
                
                for (Player player : gameSession.getPlayers()) {
                    gameSession.getPlayerChatId(player.getUserId()).ifPresent(playerChatId -> {
                        if (!playerChatId.equals(chatId)) { // Don't echo back to sender
                            bot.sendTextMessageSafe(playerChatId, lobbyMessage);
                        }
                    });
                }
            }
        }
        
        return this;
    }
    
    @Override
    public GameState handleCallbackQuery(GameSession gameSession, AmongUsBot bot, CallbackQuery callbackQuery) {
        String data = callbackQuery.getData();
        long userId = callbackQuery.getFrom().getId();
        String chatId = callbackQuery.getMessage().getChatId().toString();
        
        if (data.equals("ready")) {
            // Handle ready button press
            Optional<Player> playerOpt = gameSession.getPlayer(userId);
            
            if (playerOpt.isPresent()) {
                Player player = playerOpt.get();
                
                // Mark player as ready
                if (!player.isReady()) {
                    player.setReady(true);
                    
                    // Send confirmation
                    bot.sendTextMessageSafe(chatId, "✅ Вы отмечены как готовый к игре!");
                    
                    // Send updated player list to all players
                    sendPlayerStatusToAllPlayers(gameSession, bot);
                    
                    // Check if all players are ready to start the game
                    if (gameSession.areAllPlayersReady() && gameSession.getPlayers().size() >= Config.MIN_PLAYERS) {
                        // Start the game
                        sendMessageToAllPlayers(gameSession, bot, "🚀 Все игроки готовы! Игра начинается...");
                        gameSession.startGame(bot);
                        return new GameRunningState();
                    }
                } else {
                    // Player is already ready
                    bot.sendTextMessageSafe(chatId, "✅ Вы уже отмечены как готовый к игре!");
                }
            }
        } else if (data.startsWith("settings_")) {
            // Handle settings callbacks
            handleSettingsCallback(gameSession, bot, callbackQuery, data);
        }
        
        return this;
    }
    
    @Override
    public void onPlayerJoin(GameSession gameSession, AmongUsBot bot, Player player) {
        // Send welcome message to the new player
        gameSession.getPlayerChatId(player.getUserId()).ifPresent(chatId -> {
            SendMessage message = createLobbyWelcomeMessage(gameSession, chatId);
            bot.sendMessageSafe(message);
        });
        
        // Send notification to all other players
        for (Player p : gameSession.getPlayers()) {
            if (p.getUserId() != player.getUserId()) {
                gameSession.getPlayerChatId(p.getUserId()).ifPresent(chatId -> {
                    bot.sendTextMessageSafe(chatId, "🔔 " + player.getDisplayName() + " присоединился к лобби!");
                    sendPlayerStatusToPlayer(gameSession, bot, p);
                });
            }
        }
    }
    
    @Override
    public void onPlayerLeave(GameSession gameSession, AmongUsBot bot, Player player) {
        // Send notification to all remaining players
        for (Player p : gameSession.getPlayers()) {
            gameSession.getPlayerChatId(p.getUserId()).ifPresent(chatId -> {
                bot.sendTextMessageSafe(chatId, "👋 " + player.getDisplayName() + " покинул лобби.");
                sendPlayerStatusToPlayer(gameSession, bot, p);
            });
        }
    }
    
    /**
     * Handles settings-related callbacks.
     */
    private void handleSettingsCallback(GameSession gameSession, AmongUsBot bot, CallbackQuery callbackQuery, String data) {
        long userId = callbackQuery.getFrom().getId();
        String chatId = callbackQuery.getMessage().getChatId().toString();
        
        // Only the owner can change settings
        if (!gameSession.isOwner(userId)) {
            bot.sendTextMessageSafe(chatId, "❌ Только создатель лобби может изменять настройки.");
            return;
        }
        
        // Handle different settings
        if (data.startsWith("settings_impostor_")) {
            if (data.equals("settings_impostor_plus")) {
                gameSession.getSettings().setImpostorCount(gameSession.getSettings().getImpostorCount() + 1);
            } else if (data.equals("settings_impostor_minus")) {
                gameSession.getSettings().setImpostorCount(gameSession.getSettings().getImpostorCount() - 1);
            }
        } else if (data.startsWith("settings_discussion_")) {
            if (data.equals("settings_discussion_plus")) {
                gameSession.getSettings().setDiscussionTimeSeconds(gameSession.getSettings().getDiscussionTimeSeconds() + 15);
            } else if (data.equals("settings_discussion_minus")) {
                gameSession.getSettings().setDiscussionTimeSeconds(gameSession.getSettings().getDiscussionTimeSeconds() - 15);
            }
        } else if (data.startsWith("settings_voting_")) {
            if (data.equals("settings_voting_plus")) {
                gameSession.getSettings().setVotingTimeSeconds(gameSession.getSettings().getVotingTimeSeconds() + 15);
            } else if (data.equals("settings_voting_minus")) {
                gameSession.getSettings().setVotingTimeSeconds(gameSession.getSettings().getVotingTimeSeconds() - 15);
            }
        } else if (data.startsWith("settings_killcooldown_")) {
            if (data.equals("settings_killcooldown_plus")) {
                gameSession.getSettings().setKillCooldownSeconds(gameSession.getSettings().getKillCooldownSeconds() + 5);
            } else if (data.equals("settings_killcooldown_minus")) {
                gameSession.getSettings().setKillCooldownSeconds(gameSession.getSettings().getKillCooldownSeconds() - 5);
            }
        } else if (data.startsWith("settings_tasks_")) {
            if (data.equals("settings_tasks_plus")) {
                gameSession.getSettings().setTasksPerPlayer(gameSession.getSettings().getTasksPerPlayer() + 1);
            } else if (data.equals("settings_tasks_minus")) {
                gameSession.getSettings().setTasksPerPlayer(gameSession.getSettings().getTasksPerPlayer() - 1);
            }
        }
        
        // Update settings display for all players
        sendSettingsToAllPlayers(gameSession, bot);
    }
    
    /**
     * Sends lobby information to all players.
     */
    private void sendLobbyInfoToAllPlayers(GameSession gameSession, AmongUsBot bot) {
        for (Player player : gameSession.getPlayers()) {
            gameSession.getPlayerChatId(player.getUserId()).ifPresent(chatId -> {
                SendMessage message = new SendMessage();
                message.setChatId(chatId);
                message.setText("🎮 *Информация о лобби*\n\n" +
                        "Код: *" + gameSession.getLobbyCode() + "*\n" +
                        "Владелец: " + gameSession.getOwner().getDisplayName() + "\n\n" +
                        gameSession.getSettings().getFormattedSettings());
                message.enableMarkdown(true);
                
                bot.sendMessageSafe(message);
                
                // Send player status separately
                sendPlayerStatusToPlayer(gameSession, bot, player);
            });
        }
    }
    
    /**
     * Sends player status to all players.
     */
    private void sendPlayerStatusToAllPlayers(GameSession gameSession, AmongUsBot bot) {
        for (Player player : gameSession.getPlayers()) {
            sendPlayerStatusToPlayer(gameSession, bot, player);
        }
    }
    
    /**
     * Sends player status to a specific player.
     */
    private void sendPlayerStatusToPlayer(GameSession gameSession, AmongUsBot bot, Player player) {
        gameSession.getPlayerChatId(player.getUserId()).ifPresent(chatId -> {
            StringBuilder sb = new StringBuilder();
            
            sb.append("👥 *Игроки* (").append(gameSession.getPlayers().size()).append("/").append(Config.MAX_PLAYERS).append("):\n");
            
            for (Player p : gameSession.getPlayers()) {
                String readyStatus = p.isReady() ? "✅" : "⬜";
                String ownerLabel = gameSession.isOwner(p.getUserId()) ? " 👑" : "";
                
                sb.append(readyStatus).append(" ")
                        .append(p.getDisplayName())
                        .append(ownerLabel)
                        .append("\n");
            }
            
            // Add ready button if player is not ready
            if (!player.isReady()) {
                InlineKeyboardMarkup markup = new InlineKeyboardMarkup();
                List<List<InlineKeyboardButton>> keyboard = new ArrayList<>();
                
                List<InlineKeyboardButton> row = new ArrayList<>();
                InlineKeyboardButton readyButton = new InlineKeyboardButton();
                readyButton.setText("Готов");
                readyButton.setCallbackData("ready");
                row.add(readyButton);
                
                keyboard.add(row);
                markup.setKeyboard(keyboard);
                
                SendMessage message = new SendMessage();
                message.setChatId(chatId);
                message.setText(sb.toString());
                message.enableMarkdown(true);
                message.setReplyMarkup(markup);
                
                bot.sendMessageSafe(message);
            } else {
                bot.sendTextMessageSafe(chatId, sb.toString());
            }
        });
    }
    
    /**
     * Sends settings to all players.
     */
    private void sendSettingsToAllPlayers(GameSession gameSession, AmongUsBot bot) {
        for (Player player : gameSession.getPlayers()) {
            gameSession.getPlayerChatId(player.getUserId()).ifPresent(chatId -> {
                SendMessage message = new SendMessage();
                message.setChatId(chatId);
                message.setText(gameSession.getSettings().getFormattedSettings());
                message.enableMarkdown(true);
                
                // Only include settings controls for the owner
                if (gameSession.isOwner(player.getUserId())) {
                    message.setReplyMarkup(createSettingsKeyboard(gameSession));
                }
                
                bot.sendMessageSafe(message);
            });
        }
    }
    
    /**
     * Creates the welcome message for a player joining the lobby.
     */
    private SendMessage createLobbyWelcomeMessage(GameSession gameSession, String chatId) {
        SendMessage message = new SendMessage();
        message.setChatId(chatId);
        message.setText("✅ Вы присоединились к лобби *" + gameSession.getLobbyCode() + "*!\n\n" +
                "Владелец: " + gameSession.getOwner().getDisplayName() + "\n\n" +
                "Нажмите кнопку \"Готов\", когда будете готовы к игре.");
        message.enableMarkdown(true);
        
        // Add ready button
        InlineKeyboardMarkup markup = new InlineKeyboardMarkup();
        List<List<InlineKeyboardButton>> keyboard = new ArrayList<>();
        
        List<InlineKeyboardButton> row = new ArrayList<>();
        InlineKeyboardButton readyButton = new InlineKeyboardButton();
        readyButton.setText("Готов");
        readyButton.setCallbackData("ready");
        row.add(readyButton);
        
        keyboard.add(row);
        markup.setKeyboard(keyboard);
        
        message.setReplyMarkup(markup);
        
        return message;
    }
    
    /**
     * Creates the settings keyboard.
     */
    private InlineKeyboardMarkup createSettingsKeyboard(GameSession gameSession) {
        InlineKeyboardMarkup markup = new InlineKeyboardMarkup();
        List<List<InlineKeyboardButton>> keyboard = new ArrayList<>();
        
        // Impostor count row
        List<InlineKeyboardButton> impostorRow = new ArrayList<>();
        
        InlineKeyboardButton impostorMinus = new InlineKeyboardButton();
        impostorMinus.setText("➖");
        impostorMinus.setCallbackData("settings_impostor_minus");
        impostorRow.add(impostorMinus);
        
        InlineKeyboardButton impostorCount = new InlineKeyboardButton();
        impostorCount.setText("Импостеры: " + gameSession.getSettings().getImpostorCount());
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
        discussionTime.setText("Обсуждение: " + gameSession.getSettings().getDiscussionTimeSeconds() + "с");
        discussionTime.setCallbackData("settings_discussion_info");
        discussionRow.add(discussionTime);
        
        InlineKeyboardButton discussionPlus = new InlineKeyboardButton();
        discussionPlus.setText("➕");
        discussionPlus.setCallbackData("settings_discussion_plus");
        discussionRow.add(discussionPlus);
        
        keyboard.add(discussionRow);
        
        // Add more settings rows...
        
        markup.setKeyboard(keyboard);
        return markup;
    }
} 