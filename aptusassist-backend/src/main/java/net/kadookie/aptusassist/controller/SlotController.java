package net.kadookie.aptusassist.controller;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

import net.kadookie.aptusassist.entity.Slot;
import net.kadookie.aptusassist.dto.LoginResponse;
import net.kadookie.aptusassist.service.SlotService;
import net.kadookie.aptusassist.service.SlotDbService;
import net.kadookie.aptusassist.service.LoginService;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.temporal.WeekFields;
import java.util.*;
import java.util.stream.Collectors;

/**
 * REST controller for booking operations.
 * <p>
 * Provides endpoints for:
 * <ul>
 * <li>Retrieving available time slots (GET /slots)</li>
 * <li>Booking specific time slots (POST /book)</li>
 * <li>Synchronizing with external booking system</li>
 * </ul>
 * <p>
 * API Features:
 * <ul>
 * <li>Week-based slot viewing</li>
 * <li>Filtering for free slots only</li>
 * <li>Time slot mapping to human-readable times</li>
 * <li>External system integration</li>
 * </ul>
 * <p>
 * Security:
 * <ul>
 * <li>Requires valid credentials for external system</li>
 * <li>Validates slot availability before booking</li>
 * </ul>
 */
@Controller
public class SlotController {
    private static final Logger logger = LoggerFactory.getLogger(SlotController.class);

    /** Service for database operations */
    private final SlotDbService dbService;

    /** Service for booking business logic */
    private final SlotService bookingService;

    /** Service for authentication */
    private final LoginService loginService;

    /** Username for external system authentication */
    private final String username;

    /** Password for external system authentication */
    private final String password;

    /** Group ID for booking operations */
    private final int bookingGroupId;
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    /**
     * Mapping of pass numbers to time slots.
     * <p>
     * Defines the schedule configuration with:
     * <ul>
     * <li>Pass numbers as keys (1-7)</li>
     * <li>Human-readable time ranges as values</li>
     * <li>Maintains order of time slots</li>
     * </ul>
     * Special pass number 0 represents early morning slots.
     */
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
     * Constructs a new SlotController with required dependencies
     *
     * @param dbService      Service for database operations
     * @param bookingService Service for booking business logic
     * @param loginService   Service for authentication
     * @param username       External system username from configuration
     * @param password       External system password from configuration
     * @param bookingGroupId Group ID for bookings (default: 2)
     */
    public SlotController(
            SlotDbService dbService,
            SlotService bookingService,
            LoginService loginService,

            @Value("${APTUS_USERNAME}") String username,
            @Value("${APTUS_PASSWORD}") String password,
            @Value("${APTUS_BOOKING_GROUP_ID}") int bookingGroupId) {
        // TODO:
        // if (username.isEmpty()) {
        // throw new IllegalArgumentException("... is required");
        // }
        this.dbService = dbService;
        this.bookingService = bookingService;
        this.loginService = loginService;
        this.username = username;
        this.password = password;
        this.bookingGroupId = bookingGroupId;
        logger.info("BookingController initialized with username: {}, bookingGroupId: {}", username, bookingGroupId);
    }

    /**
     * Retrieves available booking slots for a given week
     *
     * @param passDateStr Optional date string in yyyy-MM-dd format (defaults to
     *                    current week)
     * @param freeOnly    Whether to return only free slots (default: false)
     * @return ResponseEntity containing:
     *         <ul>
     *         <li>weekDays: List of day objects with slots</li>
     *         <li>currentWeek: Week number</li>
     *         <li>prevWeekDate: Previous week start date</li>
     *         <li>nextWeekDate: Next week start date</li>
     *         </ul>
     */
    @GetMapping("/slots")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> getSlots(
            @RequestParam(value = "passDate", required = false) String passDateStr,
            @RequestParam(value = "freeOnly", defaultValue = "false") boolean freeOnly) {
        logger.debug("GET /slots called with passDate: {}, freeOnly: {}", passDateStr, freeOnly);
        LocalDate startDate;
        try {
            startDate = LocalDate.parse(passDateStr, DATE_FORMATTER);
        } catch (Exception e) {
            startDate = LocalDate.of(2025, 5, 12);
            logger.warn("Invalid passDate: {}, defaulting to 2025-05-12", passDateStr);
        }

        startDate = startDate.with(DayOfWeek.MONDAY);
        logger.debug("Adjusted startDate to Monday: {}", startDate);

        List<Slot> bookings = dbService.findAllSlots();
        logger.debug("Fetched {} bookings from database", bookings.size());

        if (freeOnly) {
            bookings = bookings.stream()
                    .filter(b -> "free".equals(b.getStatus()))
                    .collect(Collectors.toList());
            logger.debug("Filtered to {} free bookings", bookings.size());
        }

        Map<LocalDate, List<Slot>> bookingsByDate = bookings.stream()
                .collect(Collectors.groupingBy(Slot::getDate));
        logger.debug("Grouped bookings by date, size: {}", bookingsByDate.size());

        List<Map<String, Object>> weekDays = new ArrayList<>();
        for (int i = 0; i < 7; i++) {
            LocalDate date = startDate.plusDays(i);
            DayOfWeek dayOfWeek = date.getDayOfWeek();
            Map<String, Object> dayData = new HashMap<>();
            dayData.put("date", date.format(DATE_FORMATTER));
            dayData.put("dayName", dayOfWeek.toString().substring(0, 3));
            List<Map<String, String>> slots = new ArrayList<>();
            List<Slot> dayBookings = bookingsByDate.getOrDefault(date, Collections.emptyList());

            for (Slot booking : dayBookings) {
                int passNo = booking.getPassNo();
                if (passNo == 0 && dayOfWeek == DayOfWeek.MONDAY) {
                    continue;
                }

                Map<String, String> slot = new HashMap<>();
                String time;
                if (passNo == 0) {
                    if (dayOfWeek == DayOfWeek.TUESDAY || dayOfWeek == DayOfWeek.FRIDAY) {
                        time = "09:00 - 10:00";
                    } else {
                        time = "07:00 - 10:00";
                    }
                } else {
                    time = PASS_NO_TO_TIME.getOrDefault(passNo, "Unknown");
                }
                slot.put("time", time);
                slot.put("status", booking.getStatus());
                slot.put("passNo", String.valueOf(passNo));
                slot.put("passDate", date.format(DATE_FORMATTER));
                slots.add(slot);
            }

            slots.sort(Comparator.comparing(s -> {
                String time = s.get("time");
                if (time.equals("Unknown"))
                    return "23:59";
                return time.split(" - ")[0];
            }));
            dayData.put("slots", slots);
            weekDays.add(dayData);
            logger.debug("Processed day: {}, slots: {}", date, slots.size());
        }

        Map<String, Object> response = new HashMap<>();
        response.put("weekDays", weekDays);
        response.put("currentWeek", startDate.get(WeekFields.ISO.weekOfWeekBasedYear()));
        response.put("prevWeekDate", startDate.minusWeeks(1).format(DATE_FORMATTER));
        response.put("nextWeekDate", startDate.plusWeeks(1).format(DATE_FORMATTER));
        response.put("freeOnly", freeOnly);
        response.put("passDate", startDate.format(DATE_FORMATTER));

        logger.info("Returning slots data for week starting: {}, freeOnly: {}", startDate, freeOnly);
        return ResponseEntity.ok(response);
    }

    /**
     * Books a specific time slot
     *
     * @param passDate Date string in yyyy-MM-dd format
     * @param passNo   Time slot number (1-7)
     * @return ResponseEntity with status and message:
     *         <ul>
     *         <li>success: Booking confirmation</li>
     *         <li>error: Failure reason</li>
     *         </ul>
     * @throws IllegalStateException if external booking fails
     */
    @PostMapping("/book")
    @ResponseBody
    public ResponseEntity<Map<String, String>> bookSlot(
            @RequestParam("passDate") String passDate,
            @RequestParam("passNo") int passNo) {
        logger.info("POST /book called with passDate: {}, passNo: {}", passDate, passNo);
        try {
            LocalDate date = LocalDate.parse(passDate, DATE_FORMATTER);
            logger.debug("Parsed date: {}", date);

            // Perform login to get OkHttpClient with valid session
            LoginResponse loginResponse = loginService.login(username, password);
            if (!loginResponse.isSuccess()) {
                logger.error("Login failed for booking: status={}", loginResponse.getStatus());
                return ResponseEntity.status(401)
                        .body(Map.of("status", "error", "message", "Authentication failed"));
            }

            List<Slot> bookings = dbService.findSlotsByDate(date);
            Slot targetBooking = bookings.stream()
                    .filter(b -> b.getPassNo() == passNo && "free".equals(b.getStatus()))
                    .findFirst()
                    .orElse(null);

            if (targetBooking == null) {
                logger.warn("Slot not available: passNo={}, date={}", passNo, date);
                return ResponseEntity.badRequest()
                        .body(Map.of("status", "error", "message", "Slot is not available"));
            }

            boolean success = bookingService.bookSlot(passNo, date, bookingGroupId, loginResponse.getOkHttpClient());
            if (!success) {
                logger.error("External booking failed for passNo: {}, date: {}", passNo, date);
                return ResponseEntity.status(500)
                        .body(Map.of("status", "error", "message", "Failed to book slot"));
            }

            dbService.updateSlotStatus(date, passNo, "own");
            logger.info("Booking successful, updated status to 'own' for passNo: {}, date: {}", passNo, date);

            return ResponseEntity.ok(Map.of("status", "success", "message", "Slot booked successfully"));
        } catch (Exception e) {
            logger.error("Error booking slot: passDate={}, passNo={}", passDate, passNo, e);
            return ResponseEntity.status(500)
                    .body(Map.of("status", "error", "message", "Error booking slot: " + e.getMessage()));
        }
    }
}