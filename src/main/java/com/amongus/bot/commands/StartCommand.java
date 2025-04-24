package com.amongus.bot.commands;

import com.amongus.bot.core.AmongUsBot;
import com.amongus.bot.core.SessionManager;
import com.amongus.bot.utils.SecurityManager;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Message;

/**
 * Command that displays the welcome message and instructions.
 */
public class StartCommand extends BaseCommand {
    private final SecurityManager securityManager;

    /**
     * Creates a new start command.
     */
    public StartCommand(AmongUsBot bot, SessionManager sessionManager, SecurityManager securityManager) {
        super(bot, sessionManager);
        this.securityManager = securityManager;
    }

    @Override
    public String getName() {
        return "start";
    }

    @Override
    public String getDescription() {
        return "Displays the welcome message and instructions.";
    }

    @Override
    public void execute(Message message, String args) {
        SendMessage sendMessage = new SendMessage();
        sendMessage.setChatId(message.getChatId().toString());
        sendMessage.setText("Добро пожаловать в игру Among Us для дачи!\n\n" +
                "Используйте команду /create для создания новой игры или /join для присоединения к существующей.\n\n" +
                "Для получения списка всех команд используйте /help");
        execute(sendMessage);
    }
}
