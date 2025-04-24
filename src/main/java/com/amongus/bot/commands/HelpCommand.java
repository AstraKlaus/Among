package com.amongus.bot.commands;

import com.amongus.bot.AmongUsBot;
import com.amongus.bot.session.SessionManager;
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
    public void execute(Message message, List<String> args) {
        StringBuilder helpText = new StringBuilder("*Among Us Bot Commands:*\n\n");
        
        // Get all commands and sort them alphabetically
        commands.values().stream()
                .distinct() // Remove duplicates (commands registered with multiple aliases)
                .sorted((c1, c2) -> c1.getName().compareTo(c2.getName()))
                .forEach(command -> {
                    helpText.append("*").append(command.getName()).append("*");
                    
                    // Add aliases if any
                    List<String> aliases = command.getAliases();
                    if (!aliases.isEmpty()) {
                        helpText.append(" (Aliases: ");
                        helpText.append(String.join(", ", aliases));
                        helpText.append(")");
                    }
                    
                    helpText.append("\n");
                    helpText.append(command.getDescription()).append("\n");
                    
                    // Add admin or game session requirements
                    if (command.requiresAdmin()) {
                        helpText.append("_Requires admin privileges_\n");
                    }
                    
                    if (command.requiresGameSession()) {
                        helpText.append("_Requires active game session_\n");
                    }
                    
                    helpText.append("\n");
                });
        
        helpText.append("Use /help <command> for more detailed information about a specific command.");
        
        // Send the message with markdown formatting
        SendMessage sendMessage = new SendMessage();
        sendMessage.setChatId(message.getChatId());
        sendMessage.setText(helpText.toString());
        sendMessage.enableMarkdown(true);
        
        try {
            bot.execute(sendMessage);
        } catch (TelegramApiException e) {
            e.printStackTrace();
            sendMessage(message.getChatId(), "Error generating help message.");
        }
    }

    @Override
    public String getName() {
        return "help";
    }

    @Override
    public String getDescription() {
        return "Displays a list of available commands and their descriptions.";
    }
} 