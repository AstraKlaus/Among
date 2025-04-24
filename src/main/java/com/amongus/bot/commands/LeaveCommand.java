package com.amongus.bot.commands;

import com.amongus.bot.core.AmongUsBot;
import com.amongus.bot.core.SessionManager;
import com.amongus.bot.models.Player;
import com.amongus.bot.utils.SecurityManager;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.api.objects.User;

/**
 * Command that allows a player to leave the current game lobby.
 */
public class LeaveCommand extends BaseCommand {
    private final SecurityManager securityManager;

    public LeaveCommand(AmongUsBot bot, SessionManager sessionManager, SecurityManager securityManager) {
        super(bot, sessionManager);
        this.securityManager = securityManager;
    }

    @Override
    public String getName() {
        return "leave";
    }

    @Override
    public String getDescription() {
        return "Leaves the current game lobby.";
    }

    @Override
    public void execute(Message message, String args) {
        String chatId = message.getChatId().toString();
        User telegramUser = message.getFrom();
        Player player = Player.fromTelegramUser(telegramUser);

        boolean left = sessionManager.removePlayerFromLobby(player.getUserId());
        if (left) {
            SendMessage sendMessage = new SendMessage();
            sendMessage.setChatId(chatId);
            sendMessage.setText("👋 Вы покинули лобби.");
            execute(sendMessage);
        } else {
            SendMessage sendMessage = new SendMessage();
            sendMessage.setChatId(chatId);
            sendMessage.setText("❌ Вы не находитесь в лобби.");
            execute(sendMessage);
        }
    }
}
