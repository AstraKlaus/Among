package com.amongus.bot.game.states;

import com.amongus.bot.core.AmongUsBot;
import com.amongus.bot.models.Player;
import org.telegram.telegrambots.meta.api.objects.CallbackQuery;
import org.telegram.telegrambots.meta.api.objects.Message;

import java.util.Optional;

/**
 * Represents the state when the game has ended.
 */
public class GameEndState extends BaseGameState {
    
    private final String winningTeam;
    
    /**
     * Creates a new game end state.
     *
     * @param winningTeam The winning team ("crewmates" or "impostors")
     */
    public GameEndState(String winningTeam) {
        this.winningTeam = winningTeam;
    }
    
    @Override
    public String getStateName() {
        return "GAME_END";
    }
    
    @Override
    public void onEnter(GameSession gameSession, AmongUsBot bot) {
        super.onEnter(gameSession, bot);
        
        // Announce game end
        String winMessage;
        
        if (winningTeam.equalsIgnoreCase("crewmates")) {
            winMessage = "🎉 *ПОБЕДА ЧЛЕНОВ ЭКИПАЖА!*\n\n" +
                    "Члены экипажа успешно выполнили все задания или выбросили всех импостеров!";
        } else {
            winMessage = "👹 *ПОБЕДА ИМПОСТЕРОВ!*\n\n" +
                    "Импостеры успешно устранили большинство членов экипажа!";
        }
        
        sendMessageToAllPlayers(gameSession, bot, winMessage);
        
        // Show all players and their roles
        StringBuilder playerList = new StringBuilder();
        playerList.append("\n\n*Роли игроков:*\n\n");
        
        for (Player player : gameSession.getPlayers()) {
            String roleEmoji = player.getRole().isImpostor() ? "🔪" : "👨‍🚀";
            String status = player.isAlive() ? " (выжил)" : " (погиб)";
            
            playerList.append(roleEmoji)
                    .append(" *")
                    .append(player.getDisplayName())
                    .append("*: ")
                    .append(player.getRole().getName())
                    .append(status)
                    .append("\n");
        }
        
        sendMessageToAllPlayers(gameSession, bot, playerList.toString());
        
        // Message about starting a new game
        sendMessageToAllPlayers(gameSession, bot, "\nИспользуйте команду /newgame, чтобы начать новую игру!");
    }
    
    @Override
    public GameState handleMessage(GameSession gameSession, AmongUsBot bot, Message message) {
        // Allow chat after game end
        long userId = message.getFrom().getId();
        String text = message.getText();
        
        if (text != null && !text.startsWith("/")) {
            Optional<Player> playerOpt = gameSession.getPlayer(userId);
            if (playerOpt.isPresent()) {
                Player player = playerOpt.get();
                String formattedMessage = "*" + player.getDisplayName() + "*: " + text;
                sendMessageToAllPlayers(gameSession, bot, formattedMessage);
            }
        }
        
        return this;
    }
    
    @Override
    public GameState handleCallbackQuery(GameSession gameSession, AmongUsBot bot, CallbackQuery callbackQuery) {
        // No callback queries in the end state
        return this;
    }
    
    /**
     * Gets the winning team.
     *
     * @return The winning team
     */
    public String getWinningTeam() {
        return winningTeam;
    }
} 