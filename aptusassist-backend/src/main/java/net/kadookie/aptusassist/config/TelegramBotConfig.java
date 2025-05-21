package net.kadookie.aptusassist.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.telegram.telegrambots.meta.TelegramBotsApi;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import org.telegram.telegrambots.updatesreceivers.DefaultBotSession;

import net.kadookie.aptusassist.service.NotificationService;

/**
 * Configuration class for Telegram Bot integration.
 * <p>
 * Responsibilities:
 * <ul>
 * <li>Initializes Telegram Bots API with default session type</li>
 * <li>Registers {@link NotificationService} as the bot handler</li>
 * <li>Handles bot registration lifecycle</li>
 * </ul>
 * <p>
 * Configuration Requirements:
 * <ul>
 * <li>Telegram bot token must be set in application.properties</li>
 * <li>NotificationService must implement TelegramLongPollingBot</li>
 * </ul>
 * <p>
 * Error Handling:
 * <ul>
 * <li>Throws {@link TelegramApiException} if bot token is invalid</li>
 * <li>Fails fast if bot username is already registered</li>
 * </ul>
 * <p>
 * Example Properties:
 * <pre>{@code
 * telegram.bot.token=your_bot_token
 * telegram.bot.username=your_bot_username
 * }</pre>
 *
 * @see NotificationService
 * @see <a href="https://core.telegram.org/bots/api">Telegram Bot API</a>
 */
@Configuration
public class TelegramBotConfig {

    /**
     * Creates and configures the Telegram Bots API bean.
     * <p>
     * Implementation Details:
     * <ul>
     * <li>Uses {@link DefaultBotSession} for long polling</li>
     * <li>Automatically starts message polling</li>
     * <li>Supports both commands and callbacks</li>
     * </ul>
     *
     * @param notificationService The notification service implementing bot logic
     * @return Configured TelegramBotsApi instance ready for use
     * @throws TelegramApiException if:
     * <ul>
     * <li>Bot token is invalid</li>
     * <li>Bot username is already registered</li>
     * <li>Network connectivity issues occur</li>
     * </ul>
     * @see DefaultBotSession
     */
    @Bean
    public TelegramBotsApi telegramBotsApi(NotificationService notificationService) throws TelegramApiException {
        TelegramBotsApi telegramBotsApi = new TelegramBotsApi(DefaultBotSession.class);
        telegramBotsApi.registerBot(notificationService);
        return telegramBotsApi;
    }
}
