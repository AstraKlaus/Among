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
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.api.objects.User;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Command that creates a new game lobby.
 */
public class CreateCommand extends BaseCommand {
    private static final Logger log = LoggerFactory.getLogger(CreateCommand.class);
    private final SecurityManager securityManager;

    public CreateCommand(AmongUsBot bot, SessionManager sessionManager, SecurityManager securityManager) {
        super(bot, sessionManager);
        this.securityManager = securityManager;
    }

    @Override
    public String getName() {
        return "create";
    }

    @Override
    public String getDescription() {
        return "Creates a new game lobby.";
    }

    @Override
    public void execute(Message message, String args) {
        String chatId = message.getChatId().toString();
        User telegramUser = message.getFrom();
        Player player = Player.fromTelegramUser(telegramUser);

        // Проверяем, не находится ли игрок уже в лобби
        Optional<GameLobby> existingLobby = sessionManager.getLobbyForPlayer(player.getUserId());
        if (existingLobby.isPresent()) {
            SendMessage sendMessage = new SendMessage();
            sendMessage.setChatId(chatId);
            sendMessage.setText("❌ Вы уже находитесь в лобби. Покиньте текущее лобби с помощью команды /leave, чтобы создать новое.");
            execute(sendMessage);
            return;
        }

        // Обновляем активность игрока
        player.updateActivity();

        // Создаем новое лобби
        String lobbyCode = sessionManager.createLobby(player);

        // Создаем клавиатуру с кнопками "Готов" и "Настройки"
        InlineKeyboardMarkup markup = new InlineKeyboardMarkup();
        List<List<InlineKeyboardButton>> keyboard = new ArrayList<>();

        List<InlineKeyboardButton> readyRow = new ArrayList<>();
        InlineKeyboardButton readyButton = new InlineKeyboardButton();
        readyButton.setText("Готов");
        readyButton.setCallbackData("ready");
        readyRow.add(readyButton);
        keyboard.add(readyRow);

        List<InlineKeyboardButton> settingsRow = new ArrayList<>();
        InlineKeyboardButton settingsButton = new InlineKeyboardButton();
        settingsButton.setText("⚙️ Настройки");
        settingsButton.setCallbackData("settings");
        settingsRow.add(settingsButton);
        keyboard.add(settingsRow);

        markup.setKeyboard(keyboard);

        // Отправляем приветственное сообщение
        SendMessage sendMessage = new SendMessage();
        sendMessage.setChatId(chatId);
        sendMessage.setText("🎮 Создано новое лобби!\n\n" +
                "Код лобби: *" + lobbyCode + "*\n\n" +
                "Поделитесь этим кодом с другими игроками, чтобы они могли присоединиться с помощью команды:\n" +
                "/join " + lobbyCode + "\n\n" +
                "Когда все будут готовы, игра начнется автоматически.");
        sendMessage.enableMarkdown(true);
        sendMessage.setReplyMarkup(markup);
        execute(sendMessage);

        // Обновляем chat ID игрока в сессии
        sessionManager.updatePlayerChatId(player.getUserId(), chatId);

        // Отправляем сообщение со статусом владельцу лобби
        Optional<GameLobby> lobbyOpt = sessionManager.getLobbyByCode(lobbyCode);
        if (lobbyOpt.isPresent()) {
            GameLobby lobby = lobbyOpt.get();

            // Строим текст со статусом игроков
            StringBuilder statusSB = new StringBuilder();
            statusSB.append("👥 *Игроки* (").append(lobby.getPlayers().size()).append("/").append(Config.MAX_PLAYERS).append("):\n");

            // Добавляем только владельца (он единственный в лобби на этом этапе)
            for (Player p : lobby.getPlayers()) {
                String readyStatus = p.isReady() ? "✅" : "⬜";
                String ownerLabel = lobby.isOwner(p.getUserId()) ? " 👑" : "";
                statusSB.append(readyStatus).append(" ")
                        .append(p.getDisplayName())
                        .append(ownerLabel)
                        .append("\n");
            }

            // Создаем клавиатуру с кнопками
            InlineKeyboardMarkup statusMarkup = new InlineKeyboardMarkup();
            List<List<InlineKeyboardButton>> statusKeyboard = new ArrayList<>();

            // Кнопка "Готов"
            List<InlineKeyboardButton> statusReadyRow = new ArrayList<>();
            InlineKeyboardButton statusReadyButton = new InlineKeyboardButton();
            statusReadyButton.setText("Готов");
            statusReadyButton.setCallbackData("ready");
            statusReadyRow.add(statusReadyButton);
            statusKeyboard.add(statusReadyRow);

            // Кнопка настроек для владельца
            List<InlineKeyboardButton> statusSettingsRow = new ArrayList<>();
            InlineKeyboardButton statusSettingsButton = new InlineKeyboardButton();
            statusSettingsButton.setText("⚙️ Настройки");
            statusSettingsButton.setCallbackData("settings");
            statusSettingsRow.add(statusSettingsButton);
            statusKeyboard.add(statusSettingsRow);

            statusMarkup.setKeyboard(statusKeyboard);

            // Отправляем сообщение со статусом и сохраняем его ID
            Integer statusMessageId = bot.sendMessageWithReturnIdSafe(chatId, statusSB.toString(), statusMarkup);
            if (statusMessageId != null) {
                lobby.setStatusMessageId(player.getUserId(), statusMessageId);
            }
        }

        // Логируем создание лобби
        log.info("Создано новое лобби с кодом {} игроком {}", lobbyCode, player.getDisplayName());
    }
}
