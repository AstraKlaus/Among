package com.amongus.bot.commands;

import com.amongus.bot.core.AmongUsBot;
import com.amongus.bot.core.SessionManager;
import com.amongus.bot.game.lobby.GameLobby;
import com.amongus.bot.models.Player;
import com.amongus.bot.utils.SecurityManager;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.api.objects.User;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Command that allows a player to join an existing game lobby.
 */
public class JoinCommand extends BaseCommand {
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
        String chatId = message.getChatId().toString();
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
            InlineKeyboardMarkup markup = new InlineKeyboardMarkup();
            List<List<InlineKeyboardButton>> keyboard = new ArrayList<>();
            List<InlineKeyboardButton> row = new ArrayList<>();
            InlineKeyboardButton readyButton = new InlineKeyboardButton();
            readyButton.setText("Готов");
            readyButton.setCallbackData("ready");
            row.add(readyButton);
            keyboard.add(row);
            markup.setKeyboard(keyboard);

            Optional<GameLobby> lobbyOpt = sessionManager.getLobbyByCode(lobbyCode);
            String playerStatus = lobbyOpt.map(GameLobby::getPlayerStatusString).orElse("");

            SendMessage sendMessage = new SendMessage();
            sendMessage.setChatId(chatId);
            sendMessage.setText("✅ Вы присоединились к лобби *" + lobbyCode + "*!\n\n" +
                    playerStatus + "\n" +
                    "Нажмите кнопку \"Готов\", когда будете готовы к игре.");
            sendMessage.enableMarkdown(true);
            sendMessage.setReplyMarkup(markup);
            execute(sendMessage);

            sessionManager.updatePlayerChatId(player.getUserId(), chatId);
        } else {
            SendMessage sendMessage = new SendMessage();
            sendMessage.setChatId(chatId);
            sendMessage.setText("❌ Не удалось присоединиться к лобби с кодом *" + lobbyCode + "*!");
            sendMessage.enableMarkdown(true);
            execute(sendMessage);
        }
    }
}
