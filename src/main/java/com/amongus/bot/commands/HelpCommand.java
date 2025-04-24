package com.amongus.bot.commands;

import com.amongus.bot.core.AmongUsBot;
import com.amongus.bot.core.SessionManager;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

import java.util.List;
import java.util.Map;

/**
 * Command that displays help information about all available commands.
 */
public class HelpCommand extends BaseCommand {

    private final Map<String, Command> commands;

    /**
     * Creates a new help command.
     *
     * @param bot            The bot instance
     * @param sessionManager The session manager
     * @param commands       The commands map
     */
    public HelpCommand(AmongUsBot bot, SessionManager sessionManager, Map<String, Command> commands) {
        super(bot, sessionManager);
        this.commands = commands;
    }


    @Override
    public String getName() {
        return "help";
    }

    @Override
    public String getDescription() {
        return "Displays a list of available commands and their descriptions.";
    }

    @Override
    public boolean isAdminCommand() {
        return super.isAdminCommand();
    }

    @Override
    public void execute(Message message, String args) {
        // Реализация метода согласно интерфейсу Command
        SendMessage sendMessage = new SendMessage();
        sendMessage.setChatId(message.getChatId().toString());
        sendMessage.setText("Справка по командам бота:\n" +
                "/create - создать новое лобби\n" +
                "/join [код] - присоединиться к лобби\n" +
                "/start - начать игру\n" +
                "/leave - покинуть лобби");
        execute(sendMessage);
    }

} 