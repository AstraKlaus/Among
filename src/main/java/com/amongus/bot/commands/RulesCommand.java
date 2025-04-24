package com.amongus.bot.commands;

import com.amongus.bot.core.AmongUsBot;
import com.amongus.bot.core.SessionManager;
import com.amongus.bot.utils.SecurityManager;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Message;

/**
 * Command that displays the rules of the game.
 */
public class RulesCommand extends BaseCommand {
    private final SecurityManager securityManager;

    public RulesCommand(AmongUsBot bot, SessionManager sessionManager, SecurityManager securityManager) {
        super(bot, sessionManager);
        this.securityManager = securityManager;
    }

    @Override
    public String getName() {
        return "rules";
    }

    @Override
    public String getDescription() {
        return "Displays the rules of the game.";
    }

    @Override
    public void execute(Message message, String args) {
        SendMessage sendMessage = new SendMessage();
        sendMessage.setChatId(message.getChatId().toString());
        sendMessage.setText("📋 *Правила игры Among Us для дачи*\n\n" +
                "*Основные правила:*\n" +
                "1. Игра состоит из двух команд: членов экипажа и предателей-импостеров\n" +
                "2. Члены экипажа должны выполнять задания и искать предателей\n" +
                "3. Предатели должны устранить всех членов экипажа\n\n" +
                "*Ход игры:*\n" +
                "1. Игроки перемещаются по территории дачи и выполняют задания\n" +
                "2. Импостеры могут \"убивать\" членов экипажа\n" +
                "3. При обнаружении \"трупа\" или через кнопку экстренного собрания можно вызвать всех на совещание\n" +
                "4. Во время совещания игроки обсуждают, кто может быть предателем\n" +
                "5. После обсуждения проводится голосование для изгнания подозреваемого");
        sendMessage.enableMarkdown(true);
        execute(sendMessage);
    }
}
