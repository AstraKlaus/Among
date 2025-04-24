package com.amongus.bot;

import com.amongus.bot.core.AmongUsBot;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.telegram.telegrambots.meta.TelegramBotsApi;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import org.telegram.telegrambots.updatesreceivers.DefaultBotSession;

/**
 * Main entry point for the Among Us Telegram Bot.
 */
public class Main {
    private static final Logger log = LoggerFactory.getLogger(Main.class);

    public static void main(String[] args) {
        log.info("Starting Among Us Telegram Bot...");
        
        try {
            TelegramBotsApi botsApi = new TelegramBotsApi(DefaultBotSession.class);
            
            // Initialize and register the bot
            AmongUsBot bot = new AmongUsBot();
            botsApi.registerBot(bot);
            
            log.info("Among Us Telegram Bot successfully started!");
        } catch (TelegramApiException e) {
            log.error("Error starting Among Us Telegram Bot: {}", e.getMessage(), e);
        }
    }
} 