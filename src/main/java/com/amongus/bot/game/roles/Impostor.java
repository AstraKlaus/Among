package com.amongus.bot.game.roles;

import com.amongus.bot.models.Player;
import com.amongus.bot.utils.SecurityManager;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

/**
 * Implementation of the Impostor role.
 */
public class Impostor implements Role {
    private static final String ROLE_ID = "impostor";
    private final SecurityManager securityManager;
    
    public Impostor(SecurityManager securityManager) {
        this.securityManager = securityManager;
    }
    
    @Override
    public String getRoleId() {
        return ROLE_ID;
    }
    
    @Override
    public String getDisplayName() {
        return "Импостер";
    }
    
    @Override
    public String getDescription() {
        return "Ваша задача - убить всех членов экипажа, оставаясь незамеченным.";
    }

    @Override
    public String getName() {
        return "Предатель";
    }

    @Override
    public boolean isImpostor() {
        return true;
    }
    
    @Override
    public InlineKeyboardMarkup getAbilitiesKeyboard(Player player) {
        InlineKeyboardMarkup markup = new InlineKeyboardMarkup();
        List<List<InlineKeyboardButton>> keyboard = new ArrayList<>();
        
        // First row: Fake Tasks and Report buttons
        List<InlineKeyboardButton> row1 = new ArrayList<>();
        
        InlineKeyboardButton tasksButton = new InlineKeyboardButton();
        tasksButton.setText("📋 Задания");
        tasksButton.setCallbackData("fake_tasks");  // Note the difference for impostors
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
        
        // Third row: Kill and Sabotage buttons
        List<InlineKeyboardButton> row3 = new ArrayList<>();
        
        InlineKeyboardButton killButton = new InlineKeyboardButton();
        boolean canKill = player.canKill(Instant.now());
        if (canKill) {
            killButton.setText("🔪 Убить");
        } else {
            killButton.setText("🔪 Убить (перезарядка)");
        }
        killButton.setCallbackData("kill");
        row3.add(killButton);
        
        InlineKeyboardButton sabotageButton = new InlineKeyboardButton();
        sabotageButton.setText("⚡ Саботаж");
        sabotageButton.setCallbackData("sabotage");
        row3.add(sabotageButton);
        
        keyboard.add(row1);
        keyboard.add(row2);
        keyboard.add(row3);
        markup.setKeyboard(keyboard);
        
        return markup;
    }
    
    @Override
    public String getRoleRevealMessage() {
        return "🔴 Вы - *Импостер*!\n\n" +
                "Ваша задача - убить всех членов экипажа, оставаясь незамеченным. Используйте кнопку \"Убить\" для устранения членов экипажа " +
                "и \"Саботаж\" для создания проблем.\n\n" +
                "*ВНИМАНИЕ*: Кнопка \"Задания\" позволяет вам притворяться, что вы выполняете задания, но они не будут учтены в общем прогрессе.\n\n" +
                "*Это сообщение будет автоматически удалено через несколько секунд!*";
    }
    
    @Override
    public String getEncodedRoleCallback() {
        // Using security manager to encode role information
        return securityManager.encodeRoleCallback(ROLE_ID);
    }
} 