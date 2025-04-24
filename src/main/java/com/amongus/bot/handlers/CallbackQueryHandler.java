package com.amongus.bot.handlers;

import com.amongus.bot.core.AmongUsBot;
import com.amongus.bot.core.SessionManager;
import com.amongus.bot.game.lobby.GameLobby;
import com.amongus.bot.game.states.GameSession;
import com.amongus.bot.models.Config;
import com.amongus.bot.models.Player;
import com.amongus.bot.utils.SecurityManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.telegram.telegrambots.meta.api.methods.AnswerCallbackQuery;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.methods.updatingmessages.EditMessageText;
import org.telegram.telegrambots.meta.api.objects.CallbackQuery;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * Handles callback queries from inline keyboard buttons.
 */
public class CallbackQueryHandler {
    private static final Logger log = LoggerFactory.getLogger(CallbackQueryHandler.class);
    private final AmongUsBot bot;
    private final SessionManager sessionManager;
    private final SecurityManager securityManager;

    /**
     * Creates a new callback query handler.
     */
    public CallbackQueryHandler(AmongUsBot bot, SessionManager sessionManager, SecurityManager securityManager) {
        this.bot = bot;
        this.sessionManager = sessionManager;
        this.securityManager = securityManager;
    }

    /**
     * Handles a callback query from an inline keyboard button.
     */
    public void handleCallbackQuery(CallbackQuery callbackQuery) {
        String callbackData = callbackQuery.getData();
        long userId = callbackQuery.getFrom().getId();
        String queryId = callbackQuery.getId();
        String chatId = callbackQuery.getMessage().getChatId().toString();

        log.debug("Received callback query from user {}: {}", userId, callbackData);

        // First, we should acknowledge the callback query to stop the loading indicator
        acknowledgeCallbackQuery(queryId);

        // Check if the user is authorized to perform actions
        if (!securityManager.isUserAuthorized(userId)) {
            log.warn("Unauthorized user {} attempted to use callback query", userId);
            sendCallbackResponse(queryId, "Вы не авторизованы для использования этой функции.");
            return;
        }

        // Сначала проверяем, находится ли пользователь в лобби
        Optional<GameLobby> lobbyOpt = sessionManager.getLobbyForPlayer(userId);
        if (lobbyOpt.isPresent()) {
            GameLobby lobby = lobbyOpt.get();

            // Обработка коллбэков лобби
            handleLobbyCallback(callbackQuery, lobby);
            return;
        }

        // Затем проверяем, находится ли пользователь в игровой сессии
        Optional<GameSession> gameSessionOpt = sessionManager.getSessionForPlayer(userId);
        if (gameSessionOpt.isPresent()) {
            GameSession gameSession = gameSessionOpt.get();
            // Let the game session handle the callback query based on the current state
            gameSession.handleCallbackQuery(bot, callbackQuery);
        } else {
            // Handle general callback queries (not related to an active game)
            handleGeneralCallbackQuery(callbackQuery);
        }
    }

    /**
     * Обрабатывает callback-запросы от игроков в лобби
     */
    private void handleLobbyCallback(CallbackQuery callbackQuery, GameLobby lobby) {
        String data = callbackQuery.getData();
        long userId = callbackQuery.getFrom().getId();
        String chatId = callbackQuery.getMessage().getChatId().toString();

        if (data.equals("ready")) {
            Optional<Player> playerOpt = lobby.getPlayer(userId);
            if (playerOpt.isPresent()) {
                Player player = playerOpt.get();

                if (!player.isReady()) {
                    // Отмечаем игрока как готового
                    player.setReady(true);
                    bot.sendTextMessageSafe(chatId, "✅ Вы отмечены как готовый к игре!");

                    // Обновляем статус всех игроков
                    for (Player p : lobby.getPlayers()) {
                        updatePlayerStatus(lobby, p);
                    }

                    // Проверяем, готовы ли все игроки начать игру
                    if (lobby.areAllPlayersReady() && lobby.getPlayers().size() >= Config.MIN_PLAYERS) {
                        // Начинаем игру
                        for (Player p : lobby.getPlayers()) {
                            String pChatId = sessionManager.getPlayerChatId(p.getUserId());
                            if (pChatId != null) {
                                bot.sendTextMessageSafe(pChatId, "🚀 Все игроки готовы! Игра начинается...");
                            }
                        }
                        sessionManager.startGame(lobby.getLobbyCode(), bot);
                    }
                } else {
                    bot.sendTextMessageSafe(chatId, "✅ Вы уже отмечены как готовый к игре!");
                }
            }
        } else if (data.equals("settings")) {
            if (lobby.isOwner(userId)) {
                SendMessage settingsMessage = new SendMessage();
                settingsMessage.setChatId(chatId);
                settingsMessage.setText(lobby.getSettings().getFormattedSettings());
                settingsMessage.enableMarkdown(true);
                settingsMessage.setReplyMarkup(createSettingsKeyboard(lobby));
                bot.sendMessageSafe(settingsMessage);
            } else {
                bot.sendTextMessageSafe(chatId, "❌ Только владелец лобби может изменять настройки.");
            }
        } else if (data.equals("start_game")) {
            if (lobby.isOwner(userId)) {
                if (lobby.getPlayers().size() < Config.MIN_PLAYERS) {
                    bot.sendTextMessageSafe(chatId, "❌ Недостаточно игроков для начала игры. Минимальное количество: " + Config.MIN_PLAYERS);
                    return;
                }
                if (lobby.isGameStarted()) {
                    bot.sendTextMessageSafe(chatId, "Игра уже запущена!");
                    return;
                }
                // Получаем или создаём GameSession
                Optional<GameSession> sessionOpt = sessionManager.getSessionByLobbyCode(lobby.getLobbyCode());
                GameSession session;
                if (sessionOpt.isPresent()) {
                    session = sessionOpt.get();
                } else {
                    session = new GameSession(lobby.getLobbyCode(), lobby.getOwner(), bot.getScheduler(), bot.getSecurityManager());
                    for (Player p : lobby.getPlayers()) session.addPlayer(p);
                    sessionManager.getActiveSessions().put(lobby.getLobbyCode(), session);
                }
                session.startGame(bot);
                lobby.setGameStarted(true);
                for (Player p : lobby.getPlayers()) {
                    String pChatId = sessionManager.getPlayerChatId(p.getUserId());
                    if (pChatId != null) {
                        log.info("Sending start message to {}", pChatId);
                        bot.sendTextMessageSafe(pChatId, "🚀 Игра началась! Проверьте вашу роль в личных сообщениях.");
                        log.info("Sent start message to {}", pChatId);
                    }
                }
            } else {
                bot.sendTextMessageSafe(chatId, "❌ Только владелец лобби может запустить игру.");
            }
    } else if (data.startsWith("settings_")) {
            handleSettingsCallback(lobby, callbackQuery, data);
        }
    }

    /**
     * Обновляет статус игрока в лобби
     */
    private void updatePlayerStatus(GameLobby lobby, Player player) {
        String chatId = sessionManager.getPlayerChatId(player.getUserId());
        if (chatId == null) {
            return;
        }

        StringBuilder sb = new StringBuilder();
        sb.append("👥 *Игроки* (").append(lobby.getPlayers().size()).append("/").append(Config.MAX_PLAYERS).append("):\n");
        for (Player p : lobby.getPlayers()) {
            String readyStatus = p.isReady() ? "✅" : "⬜";
            String ownerLabel = lobby.isOwner(p.getUserId()) ? " 👑" : "";
            sb.append(readyStatus).append(" ")
                    .append(p.getDisplayName())
                    .append(ownerLabel)
                    .append("\n");
        }

        // Получаем текущий ID сообщения из лобби
        Integer statusMessageId = lobby.getStatusMessageId(player.getUserId());

        // Создаем клавиатуру
        InlineKeyboardMarkup markup = new InlineKeyboardMarkup();
        List<List<InlineKeyboardButton>> keyboard = new ArrayList<>();

        // Кнопка "Готов" если игрок не готов
        if (!player.isReady()) {
            List<InlineKeyboardButton> readyRow = new ArrayList<>();
            InlineKeyboardButton readyButton = new InlineKeyboardButton();
            readyButton.setText("Готов");
            readyButton.setCallbackData("ready");
            readyRow.add(readyButton);
            keyboard.add(readyRow);
        }

        // Если игрок - владелец, добавляем кнопки настроек и запуска игры
        if (lobby.isOwner(player.getUserId())) {
            // Кнопка настроек
            List<InlineKeyboardButton> settingsRow = new ArrayList<>();
            InlineKeyboardButton settingsButton = new InlineKeyboardButton();
            settingsButton.setText("⚙️ Настройки");
            settingsButton.setCallbackData("settings");
            settingsRow.add(settingsButton);
            keyboard.add(settingsRow);

            // Кнопка запуска игры
            List<InlineKeyboardButton> startGameRow = new ArrayList<>();
            InlineKeyboardButton startGameButton = new InlineKeyboardButton();
            startGameButton.setText("🚀 Начать игру");
            startGameButton.setCallbackData("start_game");
            startGameRow.add(startGameButton);
            keyboard.add(startGameRow);
        }

        markup.setKeyboard(keyboard);

        // Если ни одна кнопка не добавлена, установим null для клавиатуры
        if (keyboard.isEmpty()) {
            markup = null;
        }

        try {
            if (statusMessageId != null) {
                // Редактируем существующее сообщение
                try {
                    EditMessageText editMessage = new EditMessageText();
                    editMessage.setChatId(chatId);
                    editMessage.setMessageId(statusMessageId);
                    editMessage.setText(sb.toString());
                    editMessage.enableMarkdown(true);
                    editMessage.setReplyMarkup(markup);
                    bot.execute(editMessage);
                } catch (TelegramApiException e) {
                    // Если редактирование не удалось, отправляем новое сообщение
                    Integer newMessageId = markup != null
                            ? bot.sendMessageWithReturnIdSafe(chatId, sb.toString(), markup)
                            : bot.sendTextMessageWithReturnIdSafe(chatId, sb.toString());
                    if (newMessageId != null) {
                        lobby.setStatusMessageId(player.getUserId(), newMessageId);
                    }
                }
            } else {
                // Отправляем новое сообщение и сохраняем его ID
                Integer newMessageId = markup != null
                        ? bot.sendMessageWithReturnIdSafe(chatId, sb.toString(), markup)
                        : bot.sendTextMessageWithReturnIdSafe(chatId, sb.toString());
                if (newMessageId != null) {
                    lobby.setStatusMessageId(player.getUserId(), newMessageId);
                }
            }
        } catch (Exception e) {
            log.error("Error updating player status: " + e.getMessage(), e);
        }
    }


    /**
     * Создает клавиатуру настроек для лобби
     */
    private InlineKeyboardMarkup createSettingsKeyboard(GameLobby lobby) {
        InlineKeyboardMarkup markup = new InlineKeyboardMarkup();
        List<List<InlineKeyboardButton>> keyboard = new ArrayList<>();

        // Количество импостеров
        List<InlineKeyboardButton> impostorRow = new ArrayList<>();
        InlineKeyboardButton impostorMinus = new InlineKeyboardButton();
        impostorMinus.setText("➖");
        impostorMinus.setCallbackData("settings_impostor_minus");
        impostorRow.add(impostorMinus);

        InlineKeyboardButton impostorCount = new InlineKeyboardButton();
        impostorCount.setText("Импостеры: " + lobby.getSettings().getImpostorCount() + " →");
        impostorCount.setCallbackData("settings_next");
        impostorRow.add(impostorCount);

        InlineKeyboardButton impostorPlus = new InlineKeyboardButton();
        impostorPlus.setText("➕");
        impostorPlus.setCallbackData("settings_impostor_plus");
        impostorRow.add(impostorPlus);
        keyboard.add(impostorRow);

        // Время обсуждения
        List<InlineKeyboardButton> discussionRow = new ArrayList<>();
        InlineKeyboardButton discussionMinus = new InlineKeyboardButton();
        discussionMinus.setText("➖");
        discussionMinus.setCallbackData("settings_discussion_minus");
        discussionRow.add(discussionMinus);

        InlineKeyboardButton discussionTime = new InlineKeyboardButton();
        discussionTime.setText("Обсуждение: " + lobby.getSettings().getDiscussionTimeSeconds() + "с");
        discussionTime.setCallbackData("settings_discussion_info");
        discussionRow.add(discussionTime);

        InlineKeyboardButton discussionPlus = new InlineKeyboardButton();
        discussionPlus.setText("➕");
        discussionPlus.setCallbackData("settings_discussion_plus");
        discussionRow.add(discussionPlus);
        keyboard.add(discussionRow);

        // Время голосования
        List<InlineKeyboardButton> votingRow = new ArrayList<>();
        InlineKeyboardButton votingMinus = new InlineKeyboardButton();
        votingMinus.setText("➖");
        votingMinus.setCallbackData("settings_voting_minus");
        votingRow.add(votingMinus);

        InlineKeyboardButton votingTime = new InlineKeyboardButton();
        votingTime.setText("Голосование: " + lobby.getSettings().getVotingTimeSeconds() + "с");
        votingTime.setCallbackData("settings_voting_info");
        votingRow.add(votingTime);

        InlineKeyboardButton votingPlus = new InlineKeyboardButton();
        votingPlus.setText("➕");
        votingPlus.setCallbackData("settings_voting_plus");
        votingRow.add(votingPlus);
        keyboard.add(votingRow);

        // Перезарядка убийства
        List<InlineKeyboardButton> killCooldownRow = new ArrayList<>();
        InlineKeyboardButton killCooldownMinus = new InlineKeyboardButton();
        killCooldownMinus.setText("➖");
        killCooldownMinus.setCallbackData("settings_killcooldown_minus");
        killCooldownRow.add(killCooldownMinus);

        InlineKeyboardButton killCooldown = new InlineKeyboardButton();
        killCooldown.setText("Перезарядка: " + lobby.getSettings().getKillCooldownSeconds() + "с");
        killCooldown.setCallbackData("settings_killcooldown_info");
        killCooldownRow.add(killCooldown);

        InlineKeyboardButton killCooldownPlus = new InlineKeyboardButton();
        killCooldownPlus.setText("➕");
        killCooldownPlus.setCallbackData("settings_killcooldown_plus");
        killCooldownRow.add(killCooldownPlus);
        keyboard.add(killCooldownRow);

        // Задания на игрока
        List<InlineKeyboardButton> tasksRow = new ArrayList<>();
        InlineKeyboardButton tasksMinus = new InlineKeyboardButton();
        tasksMinus.setText("➖");
        tasksMinus.setCallbackData("settings_tasks_minus");
        tasksRow.add(tasksMinus);

        InlineKeyboardButton tasks = new InlineKeyboardButton();
        tasks.setText("Задания: " + lobby.getSettings().getTasksPerPlayer());
        tasks.setCallbackData("settings_tasks_info");
        tasksRow.add(tasks);

        InlineKeyboardButton tasksPlus = new InlineKeyboardButton();
        tasksPlus.setText("➕");
        tasksPlus.setCallbackData("settings_tasks_plus");
        tasksRow.add(tasksPlus);
        keyboard.add(tasksRow);

        // Кнопка "Назад"
        List<InlineKeyboardButton> backRow = new ArrayList<>();
        InlineKeyboardButton backButton = new InlineKeyboardButton();
        backButton.setText("◀️ Назад");
        backButton.setCallbackData("settings_back");
        backRow.add(backButton);
        keyboard.add(backRow);

        markup.setKeyboard(keyboard);
        return markup;
    }

    /**
     * Обрабатывает настройки лобби
     */
    private void handleSettingsCallback(GameLobby lobby, CallbackQuery callbackQuery, String data) {
        long userId = callbackQuery.getFrom().getId();
        String chatId = callbackQuery.getMessage().getChatId().toString();

        // Только владелец может менять настройки
        if (!lobby.isOwner(userId)) {
            bot.sendTextMessageSafe(chatId, "❌ Только создатель лобби может изменять настройки.");
            return;
        }

        boolean settingsChanged = false;

        // Обработка различных настроек
        if (data.startsWith("settings_impostor_")) {
            if (data.equals("settings_impostor_plus")) {
                int newValue = lobby.getSettings().getImpostorCount() + 1;
                lobby.getSettings().setImpostorCount(newValue);
                settingsChanged = true;
            } else if (data.equals("settings_impostor_minus")) {
                int newValue = lobby.getSettings().getImpostorCount() - 1;
                lobby.getSettings().setImpostorCount(newValue);
                settingsChanged = true;
            }
        } else if (data.startsWith("settings_discussion_")) {
            if (data.equals("settings_discussion_plus")) {
                int newValue = lobby.getSettings().getDiscussionTimeSeconds() + 15;
                lobby.getSettings().setDiscussionTimeSeconds(newValue);
                settingsChanged = true;
            } else if (data.equals("settings_discussion_minus")) {
                int newValue = lobby.getSettings().getDiscussionTimeSeconds() - 15;
                lobby.getSettings().setDiscussionTimeSeconds(newValue);
                settingsChanged = true;
            }
        } else if (data.startsWith("settings_voting_")) {
            if (data.equals("settings_voting_plus")) {
                int newValue = lobby.getSettings().getVotingTimeSeconds() + 15;
                lobby.getSettings().setVotingTimeSeconds(newValue);
                settingsChanged = true;
            } else if (data.equals("settings_voting_minus")) {
                int newValue = lobby.getSettings().getVotingTimeSeconds() - 15;
                lobby.getSettings().setVotingTimeSeconds(newValue);
                settingsChanged = true;
            }
        } else if (data.startsWith("settings_killcooldown_")) {
            if (data.equals("settings_killcooldown_plus")) {
                int newValue = lobby.getSettings().getKillCooldownSeconds() + 5;
                lobby.getSettings().setKillCooldownSeconds(newValue);
                settingsChanged = true;
            } else if (data.equals("settings_killcooldown_minus")) {
                int newValue = lobby.getSettings().getKillCooldownSeconds() - 5;
                lobby.getSettings().setKillCooldownSeconds(newValue);
                settingsChanged = true;
            }
        } else if (data.startsWith("settings_tasks_")) {
            if (data.equals("settings_tasks_plus")) {
                int newValue = lobby.getSettings().getTasksPerPlayer() + 1;
                lobby.getSettings().setTasksPerPlayer(newValue);
                settingsChanged = true;
            } else if (data.equals("settings_tasks_minus")) {
                int newValue = lobby.getSettings().getTasksPerPlayer() - 1;
                lobby.getSettings().setTasksPerPlayer(newValue);
                settingsChanged = true;
            }
        } else if (data.equals("settings_back")) {
            Player player = lobby.getPlayer(userId).orElse(null);
            if (player != null) {
                // Принудительно отправляем новое сообщение со статусом вместо редактирования
                String statusText = buildPlayerStatusText(lobby);
                InlineKeyboardMarkup markup = createPlayerStatusKeyboard(lobby, player);

                Integer newMessageId = bot.sendMessageWithReturnIdSafe(chatId, statusText, markup);
                if (newMessageId != null) {
                    lobby.setStatusMessageId(player.getUserId(), newMessageId);
                }

                // Удаляем сообщение с настройками
                try {
                    bot.deleteMessage(chatId, callbackQuery.getMessage().getMessageId());
                } catch (Exception e) {
                    log.error("Error deleting settings message", e);
                }
            }
            return;
        } else if (data.equals("settings_next")) {
            // Здесь можно реализовать переключение между различными страницами настроек
            // Для простоты просто обновляем текущую страницу
            settingsChanged = true;
        }

        // Обновляем сообщение с настройками, только если изменения произошли
        if (settingsChanged) {
            try {
                EditMessageText editMessage = new EditMessageText();
                editMessage.setChatId(chatId);
                editMessage.setMessageId(callbackQuery.getMessage().getMessageId());
                editMessage.setText(lobby.getSettings().getFormattedSettings());
                editMessage.enableMarkdown(true);
                editMessage.setReplyMarkup(createSettingsKeyboard(lobby));
                bot.execute(editMessage);
            } catch (TelegramApiException e) {
                log.error("Error updating settings message", e);
            }
        }
    }

    /**
     * Строит текст статуса игроков в лобби
     */
    private String buildPlayerStatusText(GameLobby lobby) {
        StringBuilder sb = new StringBuilder();
        sb.append("👥 *Игроки* (").append(lobby.getPlayers().size()).append("/").append(Config.MAX_PLAYERS).append("):\n");
        for (Player p : lobby.getPlayers()) {
            String readyStatus = p.isReady() ? "✅" : "⬜";
            String ownerLabel = lobby.isOwner(p.getUserId()) ? " 👑" : "";
            sb.append(readyStatus).append(" ")
                    .append(p.getDisplayName())
                    .append(ownerLabel)
                    .append("\n");
        }
        return sb.toString();
    }

    /**
     * Создает клавиатуру для статуса игрока
     */
    private InlineKeyboardMarkup createPlayerStatusKeyboard(GameLobby lobby, Player player) {
        InlineKeyboardMarkup markup = new InlineKeyboardMarkup();
        List<List<InlineKeyboardButton>> keyboard = new ArrayList<>();

        // Кнопка "Готов" если игрок не готов
        if (!player.isReady()) {
            List<InlineKeyboardButton> readyRow = new ArrayList<>();
            InlineKeyboardButton readyButton = new InlineKeyboardButton();
            readyButton.setText("Готов");
            readyButton.setCallbackData("ready");
            readyRow.add(readyButton);
            keyboard.add(readyRow);
        }

        // Если игрок - владелец, добавляем кнопки настроек и запуска игры
        if (lobby.isOwner(player.getUserId())) {
            // Кнопка настроек
            List<InlineKeyboardButton> settingsRow = new ArrayList<>();
            InlineKeyboardButton settingsButton = new InlineKeyboardButton();
            settingsButton.setText("⚙️ Настройки");
            settingsButton.setCallbackData("settings");
            settingsRow.add(settingsButton);
            keyboard.add(settingsRow);

            // Кнопка запуска игры
            List<InlineKeyboardButton> startGameRow = new ArrayList<>();
            InlineKeyboardButton startGameButton = new InlineKeyboardButton();
            startGameButton.setText("🚀 Начать игру");
            startGameButton.setCallbackData("start_game");
            startGameRow.add(startGameButton);
            keyboard.add(startGameRow);
        }

        markup.setKeyboard(keyboard);
        return markup;
    }


    /**
     * Handles general callback queries not related to a specific game session.
     */
    private void handleGeneralCallbackQuery(CallbackQuery callbackQuery) {
        String callbackData = callbackQuery.getData();
        String queryId = callbackQuery.getId();
        // For now, we'll just acknowledge the callback
        log.debug("Handling general callback query: {}", callbackData);
        // In a real implementation, we would parse the callback data and take appropriate action
        // This might include creating games, joining lobbies, etc.
        // Just acknowledge for now
        sendCallbackResponse(queryId, "Обработка команды...");
    }

    /**
     * Acknowledges a callback query without showing a notification to the user.
     */
    private void acknowledgeCallbackQuery(String queryId) {
        AnswerCallbackQuery answer = new AnswerCallbackQuery();
        answer.setCallbackQueryId(queryId);
        try {
            bot.execute(answer);
        } catch (TelegramApiException e) {
            log.error("Failed to acknowledge callback query", e);
        }
    }

    /**
     * Sends a response to a callback query with a notification to the user.
     */
    private void sendCallbackResponse(String queryId, String text) {
        AnswerCallbackQuery answer = new AnswerCallbackQuery();
        answer.setCallbackQueryId(queryId);
        answer.setText(text);
        answer.setShowAlert(false);
        try {
            bot.execute(answer);
        } catch (TelegramApiException e) {
            log.error("Failed to send callback response", e);
        }
    }
}
