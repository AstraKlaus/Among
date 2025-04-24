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
        InlineKeyboardMarkup markup = null;
        if (!player.isReady()) {
            markup = new InlineKeyboardMarkup();
            List<List<InlineKeyboardButton>> keyboard = new ArrayList<>();

            // Кнопка "Готов"
            List<InlineKeyboardButton> readyRow = new ArrayList<>();
            InlineKeyboardButton readyButton = new InlineKeyboardButton();
            readyButton.setText("Готов");
            readyButton.setCallbackData("ready");
            readyRow.add(readyButton);
            keyboard.add(readyRow);

            // Если игрок - владелец, добавляем кнопку настроек
            if (lobby.isOwner(player.getUserId())) {
                List<InlineKeyboardButton> settingsRow = new ArrayList<>();
                InlineKeyboardButton settingsButton = new InlineKeyboardButton();
                settingsButton.setText("⚙️ Настройки");
                settingsButton.setCallbackData("settings");
                settingsRow.add(settingsButton);
                keyboard.add(settingsRow);
            }

            markup.setKeyboard(keyboard);
        }

        if (statusMessageId != null) {
            // Редактируем существующее сообщение
            boolean success = bot.editMessageTextSafe(chatId, statusMessageId, sb.toString(), markup);
            if (!success) {
                // Если редактирование не удалось, отправляем новое сообщение
                Integer newMessageId = markup != null
                        ? bot.sendMessageWithReturnIdSafe(chatId, sb.toString(), markup)
                        : bot.sendTextMessageWithReturnIdSafe(chatId, sb.toString());
                lobby.setStatusMessageId(player.getUserId(), newMessageId);
            }
        } else {
            // Отправляем новое сообщение и сохраняем его ID
            Integer newMessageId = markup != null
                    ? bot.sendMessageWithReturnIdSafe(chatId, sb.toString(), markup)
                    : bot.sendTextMessageWithReturnIdSafe(chatId, sb.toString());
            lobby.setStatusMessageId(player.getUserId(), newMessageId);
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
        impostorCount.setText("Импостеры: " + lobby.getSettings().getImpostorCount());
        impostorCount.setCallbackData("settings_impostor_info");
        impostorRow.add(impostorCount);

        InlineKeyboardButton impostorPlus = new InlineKeyboardButton();
        impostorPlus.setText("➕");
        impostorPlus.setCallbackData("settings_impostor_plus");
        impostorRow.add(impostorPlus);
        keyboard.add(impostorRow);

        // Добавление других настроек (время обсуждения, голосования и т.д.)
        // ...

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

        // Обработка различных настроек
        if (data.startsWith("settings_impostor_")) {
            if (data.equals("settings_impostor_plus")) {
                lobby.getSettings().setImpostorCount(lobby.getSettings().getImpostorCount() + 1);
            } else if (data.equals("settings_impostor_minus")) {
                lobby.getSettings().setImpostorCount(lobby.getSettings().getImpostorCount() - 1);
            }
        }
        // Добавьте обработку других настроек

        // Обработка кнопки "Назад"
        if (data.equals("settings_back")) {
            updatePlayerStatus(lobby, Objects.requireNonNull(lobby.getPlayer(userId).orElse(null)));
            return;
        }

        // Обновляем сообщение с настройками
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
