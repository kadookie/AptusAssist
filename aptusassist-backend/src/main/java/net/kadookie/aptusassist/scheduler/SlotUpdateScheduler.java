package net.kadookie.aptusassist.scheduler;

import net.kadookie.aptusassist.entity.Slot;
import net.kadookie.aptusassist.dto.LoginResponse;
import net.kadookie.aptusassist.service.SlotService;
import net.kadookie.aptusassist.service.SlotDbService;
import net.kadookie.aptusassist.service.LoginService;
import net.kadookie.aptusassist.service.NotificationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Scheduled task for periodically updating spa booking slot availability.
 * <p>
 * Scheduling Behavior:
 * <ul>
 * <li>Runs immediately on startup (initialDelay=0)</li>
 * <li>Subsequent runs every 5 minutes (fixedRate=300000ms)</li>
 * <li>Single-threaded execution (no concurrent runs)</li>
 * <li>Runs regardless of previous execution completion</li>
 * </ul>
 * <p>
 * Error Handling:
 * <ul>
 * <li>Login retries (MAX_LOGIN_RETRIES=3)</li>
 * <li>Graceful degradation on failures</li>
 * <li>Per-week processing isolation</li>
 * <li>Detailed error logging</li>
 * </ul>
 * <p>
 * Performance Considerations:
 * <ul>
 * <li>Processes weeks sequentially</li>
 * <li>Bulk database operations</li>
 * <li>Minimal memory retention</li>
 * <li>External API rate limits respected</li>
 * </ul>
 * <p>
 * Monitoring Recommendations:
 * <ul>
 * <li>Track execution duration</li>
 * <li>Monitor login success rate</li>
 * <li>Alert on consecutive failures</li>
 * <li>Log freed slot events</li>
 * </ul>
 */
@Component
public class SlotUpdateScheduler {
    private static final Logger logger = LoggerFactory.getLogger(SlotUpdateScheduler.class);

    /** Service for authentication with external system */
    private final LoginService loginService;

    /** Service for fetching slot availability */
    private final SlotService bookingService;

    /** Service for database operations */
    private final SlotDbService dbService;

    /** Service for sending Telegram notifications */
    private final NotificationService notificationService;

    /** External system username */
    private final String username;

    /** External system password */
    private final String password;

    /** Number of weeks to fetch data for */
    private final int weeksToFetch;

    /** Formatter for date strings (yyyy-MM-dd) */
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    /** Maximum authentication retry attempts */
    private static final int MAX_LOGIN_RETRIES = 3;

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

    /**
     * Constructs scheduler with required dependencies
     *
     * @param loginService        Authentication service
     * @param bookingService      Booking data service
     * @param dbService           Database service
     * @param notificationService Telegram notification service
     * @param username            External system username
     * @param password            External system password
     * @param weeksToFetch        Number of weeks to fetch (default: 3)
     */
    public SlotUpdateScheduler(
            LoginService loginService,
            SlotService bookingService,
            SlotDbService dbService,
            NotificationService notificationService,
            @Value("${aptusassist.booking.username}") String username,
            @Value("${aptusassist.booking.password}") String password,
            @Value("${aptusassist.booking.weeks:3}") int weeksToFetch) {
        this.loginService = loginService;
        this.bookingService = bookingService;
        this.dbService = dbService;
        this.notificationService = notificationService;
        this.username = username;
        this.password = password;
        this.weeksToFetch = weeksToFetch;
        logger.info("SlotUpdateScheduler initialized with username: {}, weeksToFetch: {}", username, weeksToFetch);
    }

    /**
     * Scheduled task that runs every 5 minutes to update slot availability.
     * <p>
     * Execution flow:
     * <ol>
     * <li>Authenticate with external system (with retries)</li>
     * <li>Fetch slots for configured number of weeks</li>
     * <li>Compare with existing database records</li>
     * <li>Notify users of newly freed slots via Telegram</li>
     * <li>Update database with current availability</li>
     * </ol>
     */
    @Scheduled(initialDelay = 0, fixedRate = 300000) // Run immediately and every 5 minutes
    public void updateSlots() {
        logger.info("Starting scheduled slot update for {} weeks", weeksToFetch);

        // Retry login up to MAX_LOGIN_RETRIES times
        LoginResponse response = null;
        int loginAttempts = 0;
        while (loginAttempts < MAX_LOGIN_RETRIES) {
            try {
                response = loginService.login(username, password);
                if (response.isSuccess()) {
                    logger.info("Login successful after {} attempt(s)", loginAttempts + 1);
                    break;
                } else {
                    logger.warn("Login failed on attempt {}: {}. Retrying...", loginAttempts + 1, response.getStatus());
                    loginAttempts++;
                    if (loginAttempts == MAX_LOGIN_RETRIES) {
                        logger.error("Max login retries reached. Skipping slot update.");
                        return;
                    }
                    Thread.sleep(1000);
                }
            } catch (Exception e) {
                logger.error("Login error on attempt {}: {}", loginAttempts + 1, e.getMessage(), e);
                loginAttempts++;
                if (loginAttempts == MAX_LOGIN_RETRIES) {
                    logger.error("Max login retries reached. Skipping slot update.");
                    return;
                }
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException ie) {
                    logger.warn("Interrupted during retry delay", ie);
                    Thread.currentThread().interrupt();
                }
            }
        }

        if (response == null || !response.isSuccess()) {
            logger.error("Failed to obtain successful login response. Skipping slot update.");
            return;
        }

        LocalDate startDate = LocalDate.now()
                .with(java.time.temporal.TemporalAdjusters.previousOrSame(java.time.DayOfWeek.MONDAY));
        List<Slot> currentDbBookings = dbService.findAllSlots();

        Map<String, String> dbSlotsMap = currentDbBookings.stream()
                .collect(Collectors.toMap(
                        b -> b.getDate() + "-" + b.getPassNo(),
                        Slot::getStatus,
                        (oldValue, newValue) -> oldValue));

        for (int i = 0; i < weeksToFetch; i++) {
            LocalDate weekStart = startDate.plusWeeks(i);
            String dateStr = weekStart.format(DATE_FORMATTER);
            logger.info("Fetching slots for week starting: {}", dateStr);
            try {
                bookingService.fetchSlots(dateStr, "2", response);
                List<Map<String, String>> newSlots = response.getSlots();
                if (newSlots == null) {
                    logger.error("No slots returned for week starting: {}. Skipping update.", dateStr);
                    continue;
                }

                // Compare new slots with existing ones
                for (Map<String, String> newSlot : newSlots) {
                    String key = newSlot.get("date") + "-" + newSlot.get("passNo");
                    String existingStatus = dbSlotsMap.get(key);
                    String newStatus = newSlot.get("status");

                    if (existingStatus != null && !"free".equals(existingStatus)
                            && "free".equals(newStatus)) {
                        String time = PASS_NO_TO_TIME.getOrDefault(Integer.parseInt(newSlot.get("passNo")), "Unknown");
                        notificationService.sendSlotFreedNotification(
                                newSlot.get("date"), newSlot.get("passNo"), time);
                    }
                }

                dbService.saveSlots(newSlots);
                logger.debug("Updated slots for week starting: {}", dateStr);
            } catch (Exception e) {
                logger.error("Failed to fetch or process slots for week starting: {}. Error: {}", dateStr,
                        e.getMessage(), e);
            }
        }
        logger.info("Scheduled slot update completed");
    }
}