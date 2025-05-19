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
 * Service for sending Telegram notifications and handling booking commands.
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

    /** Mapping of pass numbers to time slots, copied from BookingController */
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

    public NotificationService(
            SlotService bookingService,
            LoginService loginService,
            @Value("${aptusassist.booking.username}") String username,
            @Value("${aptusassist.booking.password}") String password,
            @Value("${aptusassist.booking.group-id:2}") int bookingGroupId,
            @Value("${telegram.bot-token}") String botToken,
            @Value("${telegram.bot-username}") String botUsername,
            @Value("${telegram.chat-ids}") String chatIds) {
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

    @Override
    public String getBotUsername() {
        return botUsername;
    }

    /**
     * Sends a notification when a slot is freed, with an inline keyboard for
     * booking.
     *
     * @param date   The date of the slot (yyyy-MM-dd)
     * @param passNo The pass number
     * @param time   The human-readable time (e.g., "12:00 - 14:00")
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
                    sendMessage(chatId, "Welcome! Click a 'Book Now' button to book a slot.");
                    logger.info("Handled /start command for chatId: {}", chatId);
                } else if (messageText.equals("/test")) {
                    sendMessage(chatId, "Test command received!");
                    logger.info("Handled /test command for chatId: {}", chatId);
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
     * Sends an error message to the user.
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