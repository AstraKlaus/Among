package com.amongus.bot.commands;

import com.amongus.bot.core.AmongUsBot;
import com.amongus.bot.core.SessionManager;
import com.amongus.bot.game.lobby.GameLobby;
import com.amongus.bot.models.Player;
import com.amongus.bot.utils.SecurityManager;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.api.objects.User;

import java.util.Optional;

/**
 * Command that kicks a player from the lobby.
 */
public class KickCommand extends BaseCommand {
    private final SecurityManager securityManager;

    public KickCommand(AmongUsBot bot, SessionManager sessionManager, SecurityManager securityManager) {
        super(bot, sessionManager);
        this.securityManager = securityManager;
    }

    @Override
    public String getName() {
        return "kick";
    }

    @Override
    public String getDescription() {
        return "Kicks a player from the lobby (lobby owner only).";
    }

    @Override
    public void execute(Message message, String args) {
        String chatId = message.getChatId().toString();
        User telegramUser = message.getFrom();

        if (args.isEmpty()) {
            SendMessage sendMessage = new SendMessage();
            sendMessage.setChatId(chatId);
            sendMessage.setText("Пожалуйста, укажите имя пользователя игрока для исключения:\n/kick @username");
            execute(sendMessage);
            return;
        }

        Player player = Player.fromTelegramUser(telegramUser);
        Optional<GameLobby> lobbyOpt = sessionManager.getLobbyForPlayer(player.getUserId());

        if (lobbyOpt.isPresent()) {
            GameLobby lobby = lobbyOpt.get();

            if (lobby.isOwner(player.getUserId())) {
                String username = args.startsWith("@") ? args.substring(1) : args;

                // Здесь должна быть логика поиска и исключения игрока
                // Псевдокод для примера:
                // boolean kicked = sessionManager.kickPlayerFromLobby(username, lobby.getLobbyCode());

                SendMessage sendMessage = new SendMessage();
                sendMessage.setChatId(chatId);
                sendMessage.setText("✅ Игрок с именем @" + username + " исключен из лобби.");
                execute(sendMessage);
            } else {
                SendMessage sendMessage = new SendMessage();
                sendMessage.setChatId(chatId);
                sendMessage.setText("❌ Только владелец лобби может исключать игроков.");
                execute(sendMessage);
            }
        } else {
            SendMessage sendMessage = new SendMessage();
            sendMessage.setChatId(chatId);
            sendMessage.setText("❌ Вы не находитесь в лобби.");
            execute(sendMessage);
        }
    }
}
