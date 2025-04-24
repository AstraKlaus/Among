package com.amongus.bot.commands;

import com.amongus.bot.core.AmongUsBot;
import com.amongus.bot.core.SessionManager;
import com.amongus.bot.game.lobby.GameLobby;
import com.amongus.bot.models.Config;
import com.amongus.bot.models.Player;
import com.amongus.bot.utils.SecurityManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.methods.updatingmessages.EditMessageText;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.api.objects.User;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Command that allows a player to join an existing game lobby.
 */
public class JoinCommand extends BaseCommand {
    private static final Logger log = LoggerFactory.getLogger(JoinCommand.class);
    private final SecurityManager securityManager;

    public JoinCommand(AmongUsBot bot, SessionManager sessionManager, SecurityManager securityManager) {
        super(bot, sessionManager);
        this.securityManager = securityManager;
    }

    @Override
    public String getName() {
        return "join";
    }

    @Override
    public String getDescription() {
        return "Joins an existing game lobby using a lobby code.";
    }

    @Override
    public void execute(Message message, String args) {
        String chatId = String.valueOf(message.getFrom().getId());
        User telegramUser = message.getFrom();

        if (args.isEmpty()) {
            SendMessage sendMessage = new SendMessage();
            sendMessage.setChatId(chatId);
            sendMessage.setText("Пожалуйста, укажите код лобби:\n/join XXXXX");
            execute(sendMessage);
            return;
        }

        String lobbyCode = args.toUpperCase();
        Player player = Player.fromTelegramUser(telegramUser);

        boolean joined = sessionManager.joinLobby(lobbyCode, player);
        if (joined) {
            // Создаем клавиатуру с кнопкой "Готов"
            InlineKeyboardMarkup markup = new InlineKeyboardMarkup();
            List<List<InlineKeyboardButton>> keyboard = new ArrayList<>();
            List<InlineKeyboardButton> row = new ArrayList<>();
            InlineKeyboardButton readyButton = new InlineKeyboardButton();
            readyButton.setText("Готов");
            readyButton.setCallbackData("ready");
            row.add(readyButton);
            keyboard.add(row);
            markup.setKeyboard(keyboard);

            // Отправляем приветственное сообщение
            SendMessage welcomeMessage = new SendMessage();
            welcomeMessage.setChatId(chatId);
            welcomeMessage.setText("✅ Вы присоединились к лобби *" + lobbyCode + "*!");
            welcomeMessage.enableMarkdown(true);
            execute(welcomeMessage);

            // Сохраняем ID чата игрока
            sessionManager.updatePlayerChatId(player.getUserId(), chatId);

            // Отправляем статус игроков с кнопкой
            Optional<GameLobby> lobbyOpt = sessionManager.getLobbyByCode(lobbyCode);
            if (lobbyOpt.isPresent()) {
                GameLobby lobby = lobbyOpt.get();

                // Создаем и отправляем сообщение со статусом для нового игрока
                StringBuilder statusBuilder = new StringBuilder();
                statusBuilder.append("👥 *Игроки* (").append(lobby.getPlayers().size()).append("/").append(Config.MAX_PLAYERS).append("):\n");
                for (Player p : lobby.getPlayers()) {
                    String readyStatus = p.isReady() ? "✅" : "⬜";
                    String ownerLabel = lobby.isOwner(p.getUserId()) ? " 👑" : "";
                    statusBuilder.append(readyStatus).append(" ")
                            .append(p.getDisplayName())
                            .append(ownerLabel)
                            .append("\n");
                }

                // Отправляем сообщение со статусом и сохраняем его ID
                Integer statusMessageId = bot.sendMessageWithReturnIdSafe(chatId, statusBuilder.toString(), markup);
                if (statusMessageId != null) {
                    lobby.setStatusMessageId(player.getUserId(), statusMessageId);
                }

                // Обновляем статус для всех игроков в лобби
                for (Player p : lobby.getPlayers()) {
                    // Не обновляем статус для присоединившегося игрока, так как мы только что отправили ему сообщение
                    if (p.getUserId() != player.getUserId()) {
                        updatePlayerStatus(lobby, p);
                    }
                }
            }
        } else {
            // Отправляем сообщение об ошибке, если не удалось присоединиться
            SendMessage sendMessage = new SendMessage();
            sendMessage.setChatId(chatId);
            sendMessage.setText("❌ Не удалось присоединиться к лобби с кодом *" + lobbyCode + "*!");
            sendMessage.enableMarkdown(true);
            execute(sendMessage);
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
}
