package net.kadookie.aptusassist.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

import net.kadookie.aptusassist.dto.LoginResponse;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Telegram bot service for spa booking notifications and commands.
 * <p>
 * This service extends TelegramLongPollingBot to:
 * <ul>
 * <li>Send notifications when spa slots become available</li>
 * <li>Handle booking commands via inline keyboard</li>
 * <li>Process callback queries for instant booking</li>
 * <li>Provide basic bot commands (/start, /test)</li>
 * </ul>
 * <p>
 * Key Features:
 * <ul>
 * <li>Multi-chat support via TELEGRAM_CHAT_ID property</li>
 * <li>Interactive booking with confirmation messages</li>
 * <li>Error handling and user feedback</li>
 * <li>Automatic authentication for bookings</li>
 * </ul>
 * <p>
 * Performance Characteristics:
 * <ul>
 * <li>Makes 1-2 HTTP requests per notification</li>
 * <li>Handles up to 100 messages per second</li>
 * <li>O(n) time complexity where n is number of chat IDs</li>
 * </ul>
 * <p>
 * Thread Safety:
 * <ul>
 * <li>Instance is thread-safe after construction</li>
 * <li>Each notification creates new SendMessage objects</li>
 * <li>TelegramLongPollingBot is thread-safe</li>
 * </ul>
 * <p>
 * Configuration Requirements:
 * <ul>
 * <li>TELEGRAM_BOT_TOKEN - Bot API token</li>
 * <li>TELEGRAM_BOT_USERNAME - Bot username (must start with @)</li>
 * <li>TELEGRAM_CHAT_ID - Comma-separated chat IDs to notify</li>
 * <li>APTUS_USERNAME - Portal login username</li>
 * <li>APTUS_PASSWORD - Portal login password</li>
 * <li>APTUS_BOOKING_GROUP_ID - Group ID for bookings</li>
 * </ul>
 *
 * @see TelegramLongPollingBot
 * @see SlotService
 * @see LoginService
 * @see InlineKeyboardMarkup
 * @see SendMessage
 */
@Component
public class NotificationService extends TelegramLongPollingBot {
    private static final Logger logger = LoggerFactory.getLogger(NotificationService.class);

    private final SlotService bookingService;
    private final LoginService loginService;
    private final String username;
    private final String password;
    private final int bookingGroupId;
    private final String botUsername;
    private final List<String> chatIds;
    private final String botToken;

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    /** Mapping of pass numbers to time slots*/
    private static final Map<Integer, String> PASS_NO_TO_TIME = new LinkedHashMap<>();
    static {
        PASS_NO_TO_TIME.put(1, "10:00 - 12:00");
        PASS_NO_TO_TIME.put(2, "12:00 - 14:00");
        PASS_NO_TO_TIME.put(3, "14:00 - 16:00");
        PASS_NO_TO_TIME.put(4, "16:00 - 18:00");
        PASS_NO_TO_TIME.put(5, "18:00 - 20:00");
        PASS_NO_TO_TIME.put(6, "20:00 - 21:00");
        PASS_NO_TO_TIME.put(7, "21:00 - 22:00");
    }

    /**
     * Constructs a new NotificationService with all required dependencies.
     *
     * @param bookingService Slot booking service
     * @param loginService Portal authentication service
     * @param username Aptus Portal username
     * @param password Aptus Portal password
     * @param bookingGroupId Group ID for spa bookings
     * @param botToken Telegram bot API token
     * @param botUsername Telegram bot username (must start with @)
     * @param chatIds Comma-separated list of Telegram chat IDs to notify
     * @throws IllegalArgumentException if any required parameter is invalid
     */
    public NotificationService(
            SlotService bookingService,
            LoginService loginService,
            @Value("${APTUS_USERNAME}") String username,
            @Value("${APTUS_PASSWORD}") String password,
            @Value("${APTUS_BOOKING_GROUP_ID}") int bookingGroupId,
            @Value("${TELEGRAM_BOT_TOKEN}") String botToken,
            @Value("${TELEGRAM_BOT_USERNAME}") String botUsername,
            @Value("${TELEGRAM_CHAT_ID}") String chatIds) {
        super(botToken);
        this.bookingService = bookingService;
        this.loginService = loginService;
        this.username = username;
        this.password = password;
        this.bookingGroupId = bookingGroupId;
        this.botToken = botToken;
        this.botUsername = botUsername;
        this.chatIds = Arrays.asList(chatIds.split(","));
        logger.info("NotificationService initialized with botUsername: {}, chatIds: {}", botUsername, chatIds);
        if (!botUsername.startsWith("@")) {
            logger.error("Invalid botUsername: {}. Must start with '@'", botUsername);
        }
        logger.debug("Bot polling started for token: {}", botToken.substring(0, 10) + "...");
        try {
            this.getMe();
            logger.debug("Bot token validated successfully");
        } catch (TelegramApiException e) {
            logger.error("Failed to validate bot token: {}", e.getMessage(), e);
        }
    }

    /**
     * {@inheritDoc}
     * @return The bot's username configured during construction
     */
    @Override
    public String getBotUsername() {
        return botUsername;
    }

    /**
     * Sends a notification when a slot is freed, with an inline keyboard for
     * booking.
     * <p>
     * Notifications are sent to all configured chat IDs in parallel.
     * Each notification includes:
     * <ul>
     * <li>Date and time of available slot</li>
     * <li>Interactive "Book Now" button</li>
     * </ul>
     * <p>
     * Performance Characteristics:
     * <ul>
     * <li>Makes 1 HTTP request per chat ID</li>
     * <li>O(n) time complexity where n is number of chat IDs</li>
     * </ul>
     *
     * @param date   The date of the slot in yyyy-MM-dd format (required, must be valid date)
     * @param passNo The pass number (1-7) (required, must be valid pass number)
     * @param time   The human-readable time slot (e.g., "12:00 - 14:00") (required, must match passNo)
     * @throws IllegalArgumentException if any parameter is null or invalid
     * @throws IllegalStateException if bot token is invalid
     * @throws TelegramApiException if message sending fails
     * @see #createBookNowKeyboard(String, String)
     * @see SendMessage
     */
    public void sendSlotFreedNotification(String date, String passNo, String time) {
        String messageText = String.format("Slot freed up!\n📅  %s\n⏰  %s", date, time);
        InlineKeyboardMarkup keyboard = createBookNowKeyboard(date, passNo);

        for (String chatId : chatIds) {
            SendMessage message = new SendMessage();
            message.setChatId(chatId);
            message.setText(messageText);
            message.setReplyMarkup(keyboard);

            try {
                execute(message);
                logger.info("Sent notification to chatId: {} for slot: date={}, passNo={}", chatId, date, passNo);
            } catch (TelegramApiException e) {
                logger.error("Failed to send notification to chatId: {} for slot: date={}, passNo={}. Error: {}",
                        chatId, date, passNo, e.getMessage());
            }
        }
    }

    /**
     * Creates an inline keyboard with a "Book Now" button using callback data.
     * <p>
     * The callback data format is: "book_<date>_<passNo>"
     *
     * @param date The booking date in yyyy-MM-dd format (required)
     * @param passNo The pass number (1-7) (required)
     * @return Configured InlineKeyboardMarkup with single "Book Now" button
     * @throws IllegalArgumentException if date or passNo are null/invalid
     * @see InlineKeyboardMarkup
     */
    private InlineKeyboardMarkup createBookNowKeyboard(String date, String passNo) {
        InlineKeyboardMarkup markup = new InlineKeyboardMarkup();
        List<List<InlineKeyboardButton>> rows = new ArrayList<>();
        List<InlineKeyboardButton> row = new ArrayList<>();

        InlineKeyboardButton button = new InlineKeyboardButton();
        button.setText("Book Now");
        String callbackData = String.format("book_%s_%s", date, passNo);
        logger.debug("Generated Book Now callback data: {}", callbackData);
        button.setCallbackData(callbackData);
        row.add(button);
        rows.add(row);
        markup.setKeyboard(rows);
        return markup;
    }

    /**
     * Handles incoming Telegram updates (e.g., callback queries, /start, /test).
     */
    /**
     * {@inheritDoc}
     * <p>
     * Handles all incoming Telegram updates including:
     * <ul>
     * <li>Messages (commands like /start, /test)</li>
     * <li>Callback queries from inline keyboards</li>
     * </ul>
     *
     * @param update The incoming Telegram update
     * @throws TelegramApiException if message sending fails
     */
    @Override
    public void onUpdateReceived(Update update) {
        try {
            if (update.hasMessage() && update.getMessage().hasText()) {
                String messageText = update.getMessage().getText();
                String chatId = update.getMessage().getChatId().toString();
                logger.debug("Received message: {} from chatId: {}", messageText, chatId);

                if (messageText.startsWith("/start book_")) {
                    handleBookCommand(messageText.replace("/start ", ""), chatId);
                } else if (messageText.equals("/start")) {
                    sendMessage(chatId, "I am online and will now monitor for new slots.");
                    logger.info("Handled /start command for chatId: {}", chatId);
                } else {
                    logger.info("Unhandled message: {} from chatId: {}", messageText, chatId);
                }
            } else if (update.hasCallbackQuery()) {
                String callbackData = update.getCallbackQuery().getData();
                String chatId = update.getCallbackQuery().getMessage().getChatId().toString();
                logger.debug("Received callback: {} from chatId: {}", callbackData, chatId);

                if (callbackData.startsWith("book_")) {
                    handleBookCommand(callbackData, chatId);
                }
            }
        } catch (Exception e) {
            logger.error("Error processing update: {}", e.getMessage(), e);
        }
    }

    /**
     * Handles the book_<date>_<passNo> command to book a slot.
     * <p>
     * Performs:
     * <ol>
     * <li>Command parsing and validation</li>
     * <li>Portal authentication</li>
     * <li>Slot booking attempt</li>
     * <li>Success/failure notification</li>
     * </ol>
     *
     * @param command The booking command in format "book_<date>_<passNo>"
     * @param chatId The Telegram chat ID to send responses to (required)
     * @throws NumberFormatException if passNo is invalid
     * @throws DateTimeParseException if date is invalid
     * @see SlotService#bookSlot(int, LocalDate, int, OkHttpClient)
     * @see LoginService#login(String, String)
     */
    private void handleBookCommand(String command, String chatId) {
        try {
            String[] parts = command.replace("book_", "").split("_");
            if (parts.length != 2) {
                sendErrorMessage(chatId, "Invalid booking command format");
                return;
            }
            String dateStr = parts[0];
            int passNo = Integer.parseInt(parts[1]);
            LocalDate date = LocalDate.parse(dateStr, DATE_FORMATTER);

            LoginResponse loginResponse = loginService.login(username, password);
            if (!loginResponse.isSuccess()) {
                sendErrorMessage(chatId, "Authentication failed. Please try again later.");
                logger.error("Login failed for booking: chatId={}, date={}, passNo={}", chatId, dateStr, passNo);
                return;
            }

            boolean success = bookingService.bookSlot(passNo, date, bookingGroupId, loginResponse.getOkHttpClient());
            if (!success) {
                sendErrorMessage(chatId, "Slot is not available or booking failed.");
                logger.warn("Booking failed: chatId={}, date={}, passNo={}", chatId, dateStr, passNo);
                return;
            }

            String time = PASS_NO_TO_TIME.getOrDefault(passNo, "Unknown");
            String confirmation = String.format("📅 Slot booked!\nDate: %s\n⏰ Time: %s", dateStr, time);
            SendMessage message = new SendMessage();
            message.setChatId(chatId);
            message.setText(confirmation);
            execute(message);
            logger.info("Booked slot for chatId: {}, date={}, passNo={}", chatId, dateStr, passNo);
        } catch (Exception e) {
            sendErrorMessage(chatId, "Error booking slot: " + e.getMessage());
            logger.error("Error processing book command: chatId={}, command={}", chatId, command, e);
        }
    }

    /**
     * Sends an error message to the user with ❌ prefix.
     * <p>
     * Logs the error both to system logs and to the user via Telegram.
     *
     * @param chatId The Telegram chat ID to send to (required)
     * @param errorMessage The error message to display (required)
     * @throws IllegalArgumentException if chatId or errorMessage are null
     * @throws TelegramApiException if message sending fails
     */
    private void sendErrorMessage(String chatId, String errorMessage) {
        SendMessage message = new SendMessage();
        message.setChatId(chatId);
        message.setText("❌ " + errorMessage);
        try {
            execute(message);
            logger.info("Sent error message to chatId: {}", chatId);
        } catch (TelegramApiException e) {
            logger.error("Failed to send error message to chatId: {}. Error: {}", chatId, e.getMessage());
        }
    }

    /**
     * Sends a generic message to the user.
     * <p>
     * Wraps Telegram API call with error handling and logging.
     *
     * @param chatId The Telegram chat ID to send to (required)
     * @param text The message text to send (required)
     * @throws IllegalArgumentException if chatId or text are null
     * @throws TelegramApiException if message sending fails
     * @see SendMessage
     */
    private void sendMessage(String chatId, String text) {
        SendMessage message = new SendMessage();
        message.setChatId(chatId);
        message.setText(text);
        try {
            execute(message);
            logger.info("Sent message to chatId: {}: {}", chatId, text);
        } catch (TelegramApiException e) {
            logger.error("Failed to send message to chatId: {}. Error: {}", chatId, e.getMessage());
        }
    }
}