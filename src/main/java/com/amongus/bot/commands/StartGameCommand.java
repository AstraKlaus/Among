package com.amongus.bot.commands;

import com.amongus.bot.core.AmongUsBot;
import com.amongus.bot.core.SessionManager;
import com.amongus.bot.game.lobby.GameLobby;
import com.amongus.bot.models.Config;
import com.amongus.bot.models.Player;
import com.amongus.bot.utils.SecurityManager;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.api.objects.User;

import java.util.Optional;

/**
 * Command that starts a game from the lobby.
 */
public class StartGameCommand extends BaseCommand {
    private final SecurityManager securityManager;

    public StartGameCommand(AmongUsBot bot, SessionManager sessionManager, SecurityManager securityManager) {
        super(bot, sessionManager);
        this.securityManager = securityManager;
    }

    @Override
    public String getName() {
        return "startgame";
    }

    @Override
    public String getDescription() {
        return "Starts the game if all players are ready.";
    }

    @Override
    public void execute(Message message, String args) {
        String chatId = message.getChatId().toString();
        User telegramUser = message.getFrom();
        Player player = Player.fromTelegramUser(telegramUser);

        Optional<GameLobby> lobbyOpt = sessionManager.getLobbyForPlayer(player.getUserId());
        if (lobbyOpt.isPresent()) {
            GameLobby lobby = lobbyOpt.get();

            if (lobby.isOwner(player.getUserId())) {
                if (lobby.getPlayers().size() < Config.MIN_PLAYERS) {
                    SendMessage sendMessage = new SendMessage();
                    sendMessage.setChatId(chatId);
                    sendMessage.setText("❌ Недостаточно игроков для начала игры. Минимальное количество: " + Config.MIN_PLAYERS);
                    execute(sendMessage);
                    return;
                }

                SendMessage sendMessage = new SendMessage();
                sendMessage.setChatId(chatId);
                sendMessage.setText("🚀 Игра запускается принудительно...");
                execute(sendMessage);

                sessionManager.startGame(lobby.getLobbyCode(), bot);
            } else {
                SendMessage sendMessage = new SendMessage();
                sendMessage.setChatId(chatId);
                sendMessage.setText("❌ Только владелец лобби может принудительно запустить игру.");
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
