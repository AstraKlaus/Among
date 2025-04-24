package com.amongus.bot.game.roles;

import com.amongus.bot.models.Player;
import com.amongus.bot.utils.SecurityManager;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;

import java.util.ArrayList;
import java.util.List;

/**
 * Implementation of the Crewmate role.
 */
public class Crewmate implements Role {
    private static final String ROLE_ID = "crewmate";
    private final SecurityManager securityManager;
    
    public Crewmate(SecurityManager securityManager) {
        this.securityManager = securityManager;
    }
    
    @Override
    public String getRoleId() {
        return ROLE_ID;
    }
    
    @Override
    public String getDisplayName() {
        return "Мирный житель";
    }
    
    @Override
    public String getDescription() {
        return "Ваша задача - выполнить все задания и обнаружить импостеров.";
    }

    @Override
    public String getName() {
        return "Член экипажа";
    }
    
    @Override
    public boolean isImpostor() {
        return false;
    }
    
    @Override
    public InlineKeyboardMarkup getAbilitiesKeyboard(Player player) {
        InlineKeyboardMarkup markup = new InlineKeyboardMarkup();
        List<List<InlineKeyboardButton>> keyboard = new ArrayList<>();
        
        // First row: Tasks and Report buttons
        List<InlineKeyboardButton> row1 = new ArrayList<>();
        
        InlineKeyboardButton tasksButton = new InlineKeyboardButton();
        tasksButton.setText("📋 Задания");
        tasksButton.setCallbackData("tasks");
        row1.add(tasksButton);
        
        InlineKeyboardButton reportButton = new InlineKeyboardButton();
        reportButton.setText("⚠️ Сообщить о теле");
        reportButton.setCallbackData("report_body");
        row1.add(reportButton);
        
        // Second row: Emergency meeting and Kill Me buttons
        List<InlineKeyboardButton> row2 = new ArrayList<>();
        
        InlineKeyboardButton emergencyButton = new InlineKeyboardButton();
        if (player.getEmergencyMeetingsLeft() > 0) {
            emergencyButton.setText("🚨 Экстренное собрание (" + player.getEmergencyMeetingsLeft() + ")");
        } else {
            emergencyButton.setText("🚨 Экстренное собрание (0)");
        }
        emergencyButton.setCallbackData("emergency_meeting");
        row2.add(emergencyButton);
        
        InlineKeyboardButton killMeButton = new InlineKeyboardButton();
        killMeButton.setText("💀 Меня убили");
        killMeButton.setCallbackData("killed");
        row2.add(killMeButton);
        
        keyboard.add(row1);
        keyboard.add(row2);
        markup.setKeyboard(keyboard);
        
        return markup;
    }
    
    @Override
    public String getRoleRevealMessage() {
        return "🔵 Вы - *Мирный житель*!\n\n" +
                "Ваша задача - выполнить все задания и найти импостеров среди вас, " +
                "пока они не убили всех.\n\n" +
                "*Это сообщение будет автоматически удалено через несколько секунд!*";
    }
    
    @Override
    public String getEncodedRoleCallback() {
        // Using security manager to encode role information
        return securityManager.encodeRoleCallback(ROLE_ID);
    }
} 