package com.amongus.bot.game.roles;

import com.amongus.bot.models.Player;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;

/**
 * Interface representing a player role in the game.
 */
public interface Role {
    /**
     * Gets the name of the role.
     * 
     * @return The role name
     */
    String getName();
    
    /**
     * Checks if this role is an impostor.
     * 
     * @return true if this is an impostor role, false otherwise
     */
    boolean isImpostor();
    
    /**
     * Gets the internal role ID (used for security).
     */
    String getRoleId();
    
    /**
     * Gets the display name of the role.
     */
    String getDisplayName();
    
    /**
     * Gets the role description.
     */
    String getDescription();
    
    /**
     * Gets the special abilities keyboard for this role.
     * Each role has its own custom keyboard for abilities.
     * The keyboard layout should be similar for all roles for security.
     */
    InlineKeyboardMarkup getAbilitiesKeyboard(Player player);
    
    /**
     * Gets the secure role reveal message for this role.
     * This message is shown only once at the start of the game.
     */
    String getRoleRevealMessage();
    
    /**
     * Gets the encoded role callback data for buttons.
     * This is used to hide the actual role from anyone seeing the screen.
     */
    String getEncodedRoleCallback();
} 