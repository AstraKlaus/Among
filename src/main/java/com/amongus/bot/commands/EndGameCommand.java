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
 * Command that forcefully ends the current game.
 */
public class EndGameCommand extends BaseCommand {
    private final SecurityManager securityManager;

    public EndGameCommand(AmongUsBot bot, SessionManager sessionManager, SecurityManager securityManager) {
        super(bot, sessionManager);
        this.securityManager = securityManager;
    }

    @Override
    public String getName() {
        return "endgame";
    }

    @Override
    public String getDescription() {
        return "Принудительно завершает текущую игру (только для администраторов).";
    }

    @Override
    public boolean isAdminCommand() {
        return true;
    }

    @Override
    public void execute(Message message, String args) {
        String chatId = message.getChatId().toString();
        User telegramUser = message.getFrom();
        long userId = telegramUser.getId();

        // Проверка, авторизован ли пользователь для использования команды
        if (!securityManager.isUserAuthorized(userId)) {
            SendMessage sendMessage = new SendMessage();
            sendMessage.setChatId(chatId);
            sendMessage.setText("❌ Только администраторы могут использовать эту команду.");
            execute(sendMessage);
            return;
        }

        Player player = Player.fromTelegramUser(telegramUser);
        Optional<GameLobby> lobbyOpt = sessionManager.getLobbyForPlayer(player.getUserId());

        if (lobbyOpt.isPresent()) {
            GameLobby lobby = lobbyOpt.get();

            // Завершаем игру
            try {
                // Предполагаем, что метод endGame существует в SessionManager
                sessionManager.endGame(lobby.getLobbyCode(), bot);

                // Отправляем подтверждение администратору
                SendMessage sendMessage = new SendMessage();
                sendMessage.setChatId(chatId);
                sendMessage.setText("✅ Игра успешно завершена.");
                execute(sendMessage);

                // Уведомляем всех игроков о завершении игры
                for (Player p : lobby.getPlayers()) {
                    if (p.getUserId() != userId) {
                        Optional<String> playerChatId = Optional.ofNullable(sessionManager.getPlayerChatId(p.getUserId()));
                        playerChatId.ifPresent(pcid -> {
                            SendMessage notification = new SendMessage();
                            notification.setChatId(pcid);
                            notification.setText("⚠️ Игра была принудительно завершена администратором.");
                            execute(notification);
                        });
                    }
                }
            } catch (Exception e) {
                // Обработка ошибок
                SendMessage sendMessage = new SendMessage();
                sendMessage.setChatId(chatId);
                sendMessage.setText("❌ Ошибка при завершении игры: " + e.getMessage());
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
