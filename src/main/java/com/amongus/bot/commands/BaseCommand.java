package com.amongus.bot.commands;

import com.amongus.bot.core.AmongUsBot;
import com.amongus.bot.core.SessionManager;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

/**
 * Base class for bot commands.
 */
public abstract class BaseCommand implements Command {

    protected final AmongUsBot bot;
    protected final SessionManager sessionManager;

    /**
     * Creates a new base command.
     *
     * @param bot            The bot instance
     * @param sessionManager The session manager
     */
    public BaseCommand(AmongUsBot bot, SessionManager sessionManager) {
        this.bot = bot;
        this.sessionManager = sessionManager;
    }

    protected void execute(SendMessage sendMessage) {
        try {
            bot.execute(sendMessage);
        } catch (TelegramApiException e) {
            e.printStackTrace();
        }
    }

    /**
     * Sends a message to the chat.
     *
     * @param chatId  The chat ID
     * @param message The message text
     */
    protected void sendMessage(Long chatId, String message) {
        SendMessage sendMessage = new SendMessage();
        sendMessage.setChatId(chatId);
        sendMessage.setText(message);
        try {
            bot.execute(sendMessage);
        } catch (TelegramApiException e) {
            e.printStackTrace();
        }
    }

    /**
     * Sends a reply to a message.
     *
     * @param message     The message to reply to
     * @param replyText   The reply text
     */
    protected void sendReply(Message message, String replyText) {
        SendMessage sendMessage = new SendMessage();
        sendMessage.setChatId(message.getChatId());
        sendMessage.setReplyToMessageId(message.getMessageId());
        sendMessage.setText(replyText);
        try {
            bot.execute(sendMessage);
        } catch (TelegramApiException e) {
            e.printStackTrace();
        }
    }

    /**
     * Checks if a user is a game admin.
     *
     * @param userId The user ID
     * @return True if the user is a game admin, false otherwise
     */
    protected boolean isAdmin(Long userId) {
        // This would check against a list of admin IDs stored somewhere
        // For now, return false
        return false;
    }
} 