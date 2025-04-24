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
 * Command that displays the list of players in the current lobby.
 */
public class PlayersCommand extends BaseCommand {
    private final SecurityManager securityManager;

    public PlayersCommand(AmongUsBot bot, SessionManager sessionManager, SecurityManager securityManager) {
        super(bot, sessionManager);
        this.securityManager = securityManager;
    }

    @Override
    public String getName() {
        return "players";
    }

    @Override
    public String getDescription() {
        return "Displays the list of players in the current lobby.";
    }

    @Override
    public void execute(Message message, String args) {
        String chatId = message.getChatId().toString();
        User telegramUser = message.getFrom();
        Player player = Player.fromTelegramUser(telegramUser);

        Optional<GameLobby> lobbyOpt = sessionManager.getLobbyForPlayer(player.getUserId());
        if (lobbyOpt.isPresent()) {
            GameLobby lobby = lobbyOpt.get();
            String playerStatus = lobby.getPlayerStatusString();

            SendMessage sendMessage = new SendMessage();
            sendMessage.setChatId(chatId);
            sendMessage.setText("👥 *Список игроков*\n\n" + playerStatus);
            sendMessage.enableMarkdown(true);
            execute(sendMessage);
        } else {
            SendMessage sendMessage = new SendMessage();
            sendMessage.setChatId(chatId);
            sendMessage.setText("❌ Вы не находитесь в лобби.");
            execute(sendMessage);
        }
    }
}
