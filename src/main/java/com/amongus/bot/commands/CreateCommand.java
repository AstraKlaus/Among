package com.amongus.bot.commands;

import com.amongus.bot.core.AmongUsBot;
import com.amongus.bot.core.SessionManager;
import com.amongus.bot.game.lobby.GameLobby;
import com.amongus.bot.game.roles.RoleFactory;
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

    /**
     * Creates a new create command.
     *
     * @param bot The bot instance
     * @param sessionManager The session manager
     * @param securityManager The security manager
     */
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

        // Создаем клавиатуру с кнопкой "Готов"
        InlineKeyboardMarkup markup = new InlineKeyboardMarkup();
        List<List<InlineKeyboardButton>> keyboard = new ArrayList<>();
        List<InlineKeyboardButton> row = new ArrayList<>();
        InlineKeyboardButton readyButton = new InlineKeyboardButton();
        readyButton.setText("Готов");
        readyButton.setCallbackData("ready");
        row.add(readyButton);
        keyboard.add(row);

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

        // Логируем создание лобби
        log.info("Создано новое лобби с кодом {} игроком {}", lobbyCode, player.getDisplayName());
    }
}
