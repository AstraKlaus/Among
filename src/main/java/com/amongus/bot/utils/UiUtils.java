package com.amongus.bot.utils;

import com.amongus.bot.game.states.GameSession;
import com.amongus.bot.models.Config;
import com.amongus.bot.models.Player;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;

import java.util.ArrayList;
import java.util.List;

public class UiUtils {
    /**
     * Создает клавиатуру с кнопкой "Готов"
     */
    public static InlineKeyboardMarkup createReadyButton() {
        InlineKeyboardMarkup markup = new InlineKeyboardMarkup();
        List<List<InlineKeyboardButton>> keyboard = new ArrayList<>();
        List<InlineKeyboardButton> row = new ArrayList<>();
        InlineKeyboardButton readyButton = new InlineKeyboardButton();
        readyButton.setText("Готов");
        readyButton.setCallbackData("ready");
        row.add(readyButton);
        keyboard.add(row);
        markup.setKeyboard(keyboard);
        return markup;
    }

    /**
     * Форматирует список игроков со статусом
     */
    public static String formatPlayerList(GameSession gameSession) {
        StringBuilder sb = new StringBuilder();
        sb.append("👥 *Игроки* (").append(gameSession.getPlayers().size()).append("/").append(Config.MAX_PLAYERS).append("):\n");
        for (Player p : gameSession.getPlayers()) {
            String readyStatus = p.isReady() ? "✅" : "⬜";
            String ownerLabel = gameSession.isOwner(p.getUserId()) ? " 👑" : "";
            sb.append(readyStatus).append(" ")
                    .append(p.getDisplayName())
                    .append(ownerLabel)
                    .append("\n");
        }
        return sb.toString();
    }
}

