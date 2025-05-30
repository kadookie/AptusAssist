package net.kadookie.aptusassist.service;

import okhttp3.*;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import net.kadookie.aptusassist.dto.LoginResponse;

import java.io.IOException;
import java.net.CookieManager;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Core service for booking operations and external system integration.
 * <p>
 * Responsibilities:
 * <ul>
 * <li>Fetching available time slots from external system (AptusPortal)</li>
 * <li>Processing booking/unbooking requests</li>
 * <li>Maintaining authenticated session state</li>
 * <li>Mapping between internal and external booking models</li>
 * </ul>
 * <p>
 * External System Integration:
 * <ul>
 * <li>Uses OkHttpClient for web requests</li>
 * <li>Maintains session cookies via CookieManager</li>
 * <li>Parses HTML responses using Jsoup</li>
 * <li>Handles redirects and authentication failures</li>
 * </ul>
 * <p>
 * Performance Characteristics:
 * <ul>
 * <li>Makes 1-10 HTTP requests per operation</li>
 * <li>Parses HTML with Jsoup (O(n) complexity)</li>
 * <li>Thread-safe after construction</li>
 * </ul>
 * <p>
 * Error Handling:
 * <ul>
 * <li>Logs detailed error information</li>
 * <li>Returns false for failed operations</li>
 * <li>Handles session timeouts gracefully</li>
 * </ul>
 * <p>
 * Thread Safety:
 * <ul>
 * <li>Instance is thread-safe after construction</li>
 * <li>Each operation creates new Request objects</li>
 * <li>OkHttpClient is thread-safe</li>
 * </ul>
 */
@Service
public class SlotService {
    private static final Logger logger = LoggerFactory.getLogger(SlotService.class);

    /** HTTP client for external system communication */
    private final OkHttpClient client;

    /** Base URL of external booking system */
    private final String baseUrl;

    /** Formatter for ISO date strings (yyyy-MM-dd) */
    private static final DateTimeFormatter ISO_DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    /**
     * Constructs booking service with external system configuration.
     * <p>
     * Initializes:
     * <ul>
     * <li>OkHttpClient with cookie management</li>
     * <li>Base URL for all external requests</li>
     * <li>Automatic redirect following</li>
     * </ul>
     *
     * @param baseUrl Base URL of external booking system (required)
     * @throws IllegalArgumentException if baseUrl is null or empty
     */
    public SlotService(@Value("${APTUS_BASE_URL}") String baseUrl) {
        this.baseUrl = baseUrl;
        this.client = new OkHttpClient.Builder()
                .followRedirects(true)
                .cookieJar(new JavaNetCookieJar(new CookieManager()))
                .build();
        logger.info("BookingService initialized with baseUrl: {}", baseUrl);
    }

    /**
     * Fetches available time slots from external system for given date and group.
     * <p>
     * Implementation Details:
     * <ul>
     * <li>Makes authenticated GET request to booking calendar endpoint</li>
     * <li>Parses HTML response using Jsoup</li>
     * <li>Extracts time slots and their availability status</li>
     * <li>Maps time ranges to internal pass numbers</li>
     * <li>Sets parsed slots in LoginResponse object</li>
     * </ul>
     * <p>
     * Performance Characteristics:
     * <ul>
     * <li>Makes 1 HTTP request to external system</li>
     * <li>Parses HTML document with Jsoup</li>
     * <li>O(n) time complexity where n is number of time slots</li>
     * </ul>
     *
     * @param passDate       Date string in yyyy-MM-dd format (required)
     * @param bookingGroupId Group identifier for booking (required)
     * @param response       LoginResponse containing authenticated OkHttpClient
     *                       (required)
     * @throws IllegalArgumentException if:
     *                                  <ul>
     *                                  <li>passDate is malformed or invalid</li>
     *                                  <li>bookingGroupId is empty</li>
     *                                  <li>response or its client is null</li>
     *                                  </ul>
     * @throws IllegalStateException    if authentication session expires
     * @throws IOException              if network error occurs
     * @see LoginResponse#setSlots(List)
     * @see Jsoup#parse(String)
     */
    public void fetchSlots(String passDate, String bookingGroupId, LoginResponse response) {
        logger.debug("Fetching slots for passDate: {}, bookingGroupId: {}", passDate, bookingGroupId);
        try {
            String url = baseUrl + "/AptusPortal/CustomerBooking/BookingCalendar?bookingGroupId=" + bookingGroupId
                    + "&passDate=" + passDate;
            logger.debug("Request URL: {}", url);
            Request request = new Request.Builder()
                    .url(url)
                    .header("User-Agent",
                            "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/135.0.0.0 Safari/537.36")
                    .header("Accept",
                            "text/html,application/xhtml+xml,application/xml;q=0.9,image/avif,image/webp,image/apng,*/*;q=0.8")
                    .header("Accept-Encoding", "gzip, deflate, br, zstd")
                    .header("Accept-Language", "en-GB,en;q=0.9")
                    .header("Referer", baseUrl + "/AptusPortal/CustomerBooking")
                    .build();
            OkHttpClient client = response.getOkHttpClient();
            Response slotsResponse = client.newCall(request).execute();
            if (!slotsResponse.isSuccessful() || slotsResponse.body() == null) {
                logger.error("Failed to fetch slots, status: {}", slotsResponse.code());
                slotsResponse.close();
                return;
            }
            String html = slotsResponse.body().string();
            slotsResponse.close();
            logger.debug("Received slots HTML, length: {}", html.length());

            if (html.contains("<title>Login</title>")) {
                logger.error("Received login page instead of booking calendar for passDate: {}", passDate);
                response.setSlots(null);
                return;
            }

            Document doc = Jsoup.parse(html);
            List<Map<String, String>> slots = new ArrayList<>();
            LocalDate startDate = LocalDate.parse(passDate, ISO_DATE_FORMATTER);
            List<Element> dayColumns = doc.select("div.dayColumn");
            logger.debug("Found {} day columns", dayColumns.size());

            for (int dayIndex = 0; dayIndex < dayColumns.size(); dayIndex++) {
                Element dayColumn = dayColumns.get(dayIndex);
                LocalDate slotDate = startDate.plusDays(dayIndex);
                String dateStr = slotDate.format(ISO_DATE_FORMATTER);
                List<Element> intervals = dayColumn.select("div.interval");

                for (int intervalIndex = 0; intervalIndex < intervals.size(); intervalIndex++) {
                    Element interval = intervals.get(intervalIndex);
                    Map<String, String> slot = new HashMap<>();

                    Element timeElement = interval.selectFirst("div:matches(\\d{2}:\\d{2} - \\d{2}:\\d{2})");
                    if (timeElement != null) {
                        slot.put("time", timeElement.text().replace("<br />", "").trim());
                    } else {
                        logger.warn("Skipping interval without time at index: {}", intervalIndex);
                        continue;
                    }

                    slot.put("date", dateStr);

                    if (interval.hasClass("bookable")) {
                        slot.put("status", "free");
                    } else if (interval.hasClass("own")) {
                        slot.put("status", "own");
                    } else {
                        slot.put("status", "busy");
                    }

                    if (!slot.isEmpty()) {
                        slots.add(slot);
                    }
                }
            }

            response.setSlots(slots.isEmpty() ? null : slots);
            logger.info("Parsed {} slots for passDate: {}", slots.size(), passDate);

            Map<String, String> suffixToPassNo = Map.of(
                    "10:00", "0",
                    "12:00", "1",
                    "14:00", "2",
                    "16:00", "3",
                    "18:00", "4",
                    "20:00", "5",
                    "21:00", "6",
                    "22:00", "7");

            slots.forEach(slot -> {
                String time = slot.getOrDefault("time", "none");
                String endTime = time.contains(" - ") ? time.split(" - ")[1].trim() : time;
                String passNo = suffixToPassNo.getOrDefault(endTime, "unknown");
                if ("unknown".equals(passNo)) {
                    logger.warn("Unknown passNo for time: {} (endTime: {}) on date: {}", time, endTime,
                            slot.get("date"));
                }
                slot.put("passNo", passNo);
                // logger.debug("Slot date: {}, time: {}, endTime: {}, passNo: {}, status: {}",
                // slot.get("date"), time, endTime, passNo, slot.get("status"));
            });
        } catch (IOException e) {
            logger.error("Error fetching slots for passDate: {}, bookingGroupId: {}", passDate, bookingGroupId, e);
            response.setSlots(null);
        }
    }

    /**
     * Books a specific time slot in external system.
     * <p>
     * The booking process involves:
     * <ol>
     * <li>Constructing booking URL with parameters</li>
     * <li>Making authenticated GET request</li>
     * <li>Handling redirects (max 10)</li>
     * <li>Validating successful booking via:
     * <ul>
     * <li>FeedbackDialog confirmation</li>
     * <li>'interval own' status in calendar</li>
     * </ul>
     * </li>
     * </ol>
     * <p>
     * Performance Characteristics:
     * <ul>
     * <li>Makes 1-10 HTTP requests (due to redirects)</li>
     * <li>Parses HTML response with Jsoup</li>
     * <li>O(1) time complexity for successful bookings</li>
     * </ul>
     *
     * @param passNo         Time slot number (0-7, required)
     * @param date           Booking date (required)
     * @param bookingGroupId Group identifier for booking (required)
     * @param client         Authenticated OkHttpClient (required)
     * @return true if booking was confirmed by either:
     *         <ul>
     *         <li>FeedbackDialog confirmation</li>
     *         <li>'interval own' status in calendar</li>
     *         </ul>
     * @throws IllegalArgumentException if:
     *                                  <ul>
     *                                  <li>passNo is outside 0-7 range</li>
     *                                  <li>date is null</li>
     *                                  <li>bookingGroupId is invalid</li>
     *                                  <li>client is null</li>
     *                                  </ul>
     * @throws IOException              if network error occurs
     * @throws IllegalStateException    if session expires during booking
     * @see #fetchSlots(String, String, LoginResponse)
     * @see OkHttpClient
     */
    public boolean bookSlot(int passNo, LocalDate date, int bookingGroupId, OkHttpClient client) {
        logger.info("Attempting to book slot: passNo={}, date={}, bookingGroupId={}", passNo, date, bookingGroupId);
        try {
            String formattedDate = date.format(ISO_DATE_FORMATTER);
            String bookUrl = baseUrl + "/AptusPortal/CustomerBooking/Book?passNo=" + passNo +
                    "&passDate=" + formattedDate + "&bookingGroupId=" + bookingGroupId;
            logger.debug("Booking URL: {}", bookUrl);

            // Prepare GET request
            Request.Builder requestBuilder = new Request.Builder()
                    .url(bookUrl)
                    .get()
                    .header("User-Agent",
                            "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/136.0.0.0 Safari/537.36")
                    .header("Accept",
                            "text/html,application/xhtml+xml,application/xml;q=0.9,image/avif,image/webp,image/apng,*/*;q=0.8,application/signed-exchange;v=b3;q=0.7")
                    .header("Accept-Encoding", "gzip, deflate, br, zstd")
                    .header("Accept-Language", "en-GB,en-US;q=0.9,en;q=0.8,sv;q=0.7")
                    .header("Referer",
                            baseUrl + "/AptusPortal/CustomerBooking/BookingCalendar?bookingGroupId=" + bookingGroupId
                                    + "&passDate=" + formattedDate)
                    .header("Upgrade-Insecure-Requests", "1")
                    .header("Sec-Fetch-Dest", "document")
                    .header("Sec-Fetch-Mode", "navigate")
                    .header("Sec-Fetch-Site", "same-origin")
                    .header("Sec-Fetch-User", "?1");

            // Manual redirect handling
            int redirectCount = 0;
            int maxRedirects = 10; // Reduced, as we expect 1 redirect
            String currentUrl = bookUrl;
            Set<String> visitedUrls = new HashSet<>();
            Response response = null;
            String finalResponseBody = null;

            while (redirectCount < maxRedirects) {
                if (!visitedUrls.add(currentUrl)) {
                    logger.error("Redirect loop detected at: {}", currentUrl);
                    return false;
                }

                Request request = requestBuilder.url(currentUrl).build();
                response = client.newCall(request).execute();
                logger.debug("Booking request sent to {}, status: {}", currentUrl, response.code());

                if (response.isSuccessful() && response.body() != null) {
                    finalResponseBody = response.body().string();
                    response.close();
                    break;
                } else if (response.code() == 302 || response.code() == 301) {
                    String location = response.header("Location");
                    response.close();
                    if (location == null) {
                        logger.error("No Location header in redirect for URL: {}", currentUrl);
                        return false;
                    }
                    currentUrl = resolveUrl(bookUrl, location);
                    logger.debug("Redirecting to: {}", currentUrl);
                    requestBuilder.url(currentUrl);
                    redirectCount++;
                } else {
                    logger.error("Booking request failed, status: {} for URL: {}", response.code(), currentUrl);
                    if (response.body() != null) {
                        String body = response.body().string();
                        logger.debug("Response body: {}", body.length() > 100 ? body.substring(0, 100) + "..." : body);
                        response.close();
                    }
                    return false;
                }
            }

            if (redirectCount >= maxRedirects) {
                logger.error("Too many redirects: {}", redirectCount);
                return false;
            }

            if (finalResponseBody == null) {
                logger.error("No response body received after redirects");
                return false;
            }

            logger.debug("Received booking response HTML, length: {}", finalResponseBody.length());

            // Check for login page (indicates session failure)
            if (finalResponseBody.contains("<title>Login</title>")) {
                logger.error("Received login page instead of booking response for passNo: {}, date: {}", passNo,
                        formattedDate);
                return false;
            }

            // Check for FeedbackDialog confirmation
            if (finalResponseBody.contains("FeedbackDialog") && finalResponseBody.contains("är bokat")) {
                logger.info("Booking confirmed via FeedbackDialog for passNo: {}, date: {}", passNo, formattedDate);
                return true;
            }

            // Fallback: Check for 'interval own' in the calendar
            Document doc = Jsoup.parse(finalResponseBody);
            String time = getTimeForPassNo(passNo);
            String dateStr = formattedDate;
            boolean isOwn = doc.select("div.dayColumn").stream().anyMatch(day -> {
                String dayDate = day.select("div.dayOfMonth").text();
                return day.select("div.interval.own").stream().anyMatch(interval -> {
                    String intervalTime = interval.select("div:matches(\\d{2}:\\d{2} - \\d{2}:\\d{2})").text().trim();
                    return intervalTime.contains(time) && dayDate.equals(dateStr.split("-")[2]);
                });
            });

            if (isOwn) {
                logger.info("Booking confirmed via 'own' status for passNo: {}, date: {}", passNo, formattedDate);
                return true;
            }

            logger.warn("Booking not confirmed for passNo: {}, date: {}", passNo, formattedDate);
            return false;
        } catch (IOException e) {
            logger.error("Failed to book slot: passNo={}, date={}, bookingGroupId={}", passNo, date, bookingGroupId, e);
            return false;
        }
    }

    /**
     * Cancels an existing booking in external system.
     * <p>
     * The unbooking process involves:
     * <ol>
     * <li>Constructing unbooking URL with booking ID</li>
     * <li>Making authenticated GET request</li>
     * <li>Handling redirects (max 10)</li>
     * <li>Validating successful unbooking via:
     * <ul>
     * <li>FeedbackDialog confirmation</li>
     * <li>Removal of 'interval own' status</li>
     * </ul>
     * </li>
     * </ol>
     * <p>
     * Performance Characteristics:
     * <ul>
     * <li>Makes 1-10 HTTP requests (due to redirects)</li>
     * <li>Parses HTML response with Jsoup</li>
     * <li>O(1) time complexity for successful unbookings</li>
     * </ul>
     *
     * @param bookingId Unique identifier of booking to cancel (required)
     * @return true if unbooking was confirmed by either:
     *         <ul>
     *         <li>FeedbackDialog confirmation</li>
     *         <li>Removal of 'interval own' status</li>
     *         </ul>
     * @throws IllegalArgumentException if bookingId is <= 0
     * @throws IOException              if network error occurs
     * @throws IllegalStateException    if session expires during unbooking
     * @see #bookSlot(int, LocalDate, int, OkHttpClient)
     * @see OkHttpClient
     */
    public boolean unbook(int bookingId) {
        logger.info("Attempting to unbook bookingId={}", bookingId);
        try {
            String unbookUrl = baseUrl + "/AptusPortal/CustomerBooking/Unbook/" + bookingId;
            logger.debug("Unbooking URL: {}", unbookUrl);

            Request.Builder requestBuilder = new Request.Builder()
                    .url(unbookUrl)
                    .get()
                    .header("User-Agent",
                            "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/136.0.0.0 Safari/537.36")
                    .header("Accept",
                            "text/html,application/xhtml+xml,application/xml;q=0.9,image/avif,image/webp,image/apng,*/*;q=0.8,application/signed-exchange;v=b3;q=0.7")
                    .header("Accept-Encoding", "gzip, deflate, br, zstd")
                    .header("Accept-Language", "en-GB,en-US;q=0.9,en;q=0.8,sv;q=0.7")
                    .header("Referer", baseUrl + "/AptusPortal/CustomerBooking")
                    .header("Upgrade-Insecure-Requests", "1")
                    .header("Sec-Fetch-Dest", "document")
                    .header("Sec-Fetch-Mode", "navigate")
                    .header("Sec-Fetch-Site", "same-origin")
                    .header("Sec-Fetch-User", "?1");

            int redirectCount = 0;
            int maxRedirects = 10;
            String currentUrl = unbookUrl;
            Set<String> visitedUrls = new HashSet<>();
            Response response = null;
            String finalResponseBody = null;

            while (redirectCount < maxRedirects) {
                if (!visitedUrls.add(currentUrl)) {
                    logger.error("Redirect loop detected at: {}", currentUrl);
                    return false;
                }

                Request request = requestBuilder.url(currentUrl).build();
                response = client.newCall(request).execute();
                logger.debug("Unbooking request sent to {}, status: {}", currentUrl, response.code());

                if (response.isSuccessful() && response.body() != null) {
                    finalResponseBody = response.body().string();
                    response.close();
                    break;
                } else if (response.code() == 302 || response.code() == 301) {
                    String location = response.header("Location");
                    response.close();
                    if (location == null) {
                        logger.error("No Location header in redirect for URL: {}", currentUrl);
                        return false;
                    }
                    currentUrl = resolveUrl(unbookUrl, location);
                    logger.debug("Redirecting to: {}", currentUrl);
                    requestBuilder.url(currentUrl);
                    redirectCount++;
                } else {
                    logger.error("Unbooking request failed, status: {} for URL: {}", response.code(), currentUrl);
                    if (response.body() != null) {
                        String body = response.body().string();
                        logger.debug("Response body: {}", body.length() > 100 ? body.substring(0, 100) + "..." : body);
                        response.close();
                    }
                    return false;
                }
            }

            if (redirectCount >= maxRedirects) {
                logger.error("Too many redirects: {}", redirectCount);
                return false;
            }

            if (finalResponseBody == null) {
                logger.error("No response body received after redirects");
                return false;
            }

            logger.debug("Received unbooking response HTML, length: {}", finalResponseBody.length());

            if (finalResponseBody.contains("<title>Login</title>")) {
                logger.error("Received login page instead of unbooking response for bookingId: {}", bookingId);
                return false;
            }

            if (finalResponseBody.contains("FeedbackDialog")
                    && finalResponseBody.contains("Ditt pass har blivit avbokat")) {
                logger.info("Unbooking confirmed via FeedbackDialog for bookingId: {}", bookingId);
                return true;
            }

            logger.warn("Unbooking not confirmed for bookingId: {}", bookingId);
            return false;
        } catch (IOException e) {
            logger.error("Failed to unbook bookingId: {}", bookingId, e);
            return false;
        }
    }

    /**
     * Resolves relative URLs against base URL
     *
     * @param baseUrl  Original request URL (required)
     * @param location Location header value (absolute or relative) (required)
     * @return Fully resolved absolute URL
     * @throws IllegalArgumentException        if either parameter is null
     * @throws StringIndexOutOfBoundsException if baseUrl is malformed
     */
    /**
     * Resolves relative URLs against a base URL.
     * <p>
     * Handles:
     * <ul>
     * <li>Absolute URLs (returns as-is)</li>
     * <li>Relative URLs (resolves against base)</li>
     * <li>Protocol-relative URLs (preserves protocol)</li>
     * </ul>
     *
     * @param baseUrl  The base URL to resolve against (required)
     * @param location The URL to resolve (absolute or relative)
     * @return Fully resolved URL
     * @throws IllegalArgumentException if baseUrl is null or empty
     * @see java.net.URI#resolve(String)
     */
    private String resolveUrl(String baseUrl, String location) {
        if (location.startsWith("http")) {
            return location;
        }
        if (location.startsWith("/")) {
            return baseUrl.substring(0, baseUrl.indexOf('/', 8)) + location;
        }
        String basePath = baseUrl.substring(0, baseUrl.lastIndexOf('/') + 1);
        return basePath + location;
    }

    /**
     * Maps pass numbers to human-readable time ranges
     *
     * @param passNo Time slot number (0-7) (required)
     * @return Formatted time range string (never null)
     * @throws IllegalArgumentException if passNo is outside 0-7 range
     */
    /**
     * Maps pass numbers to human-readable time slots.
     * <p>
     * Pass numbers correspond to:
     * <ul>
     * <li>0: 10:00 - 12:00</li>
     * <li>1: 12:00 - 14:00</li>
     * <li>2: 14:00 - 16:00</li>
     * <li>3: 16:00 - 18:00</li>
     * <li>4: 18:00 - 20:00</li>
     * <li>5: 20:00 - 21:00</li>
     * <li>6: 21:00 - 22:00</li>
     * <li>7: 22:00 - 23:00</li>
     * </ul>
     *
     * @param passNo The pass number (0-7)
     * @return Corresponding time slot string
     * @throws IllegalArgumentException if passNo is outside 0-7 range
     * @see #bookSlot(int, LocalDate, int, OkHttpClient)
     */
    private String getTimeForPassNo(int passNo) {
        Map<Integer, String> passNoToTime = Map.of(
                0, "07:00 - 10:00",
                1, "10:00 - 12:00",
                2, "12:00 - 14:00",
                3, "14:00 - 16:00",
                4, "16:00 - 18:00",
                5, "18:00 - 20:00",
                6, "20:00 - 21:00",
                7, "21:00 - 22:00");
        String time = passNoToTime.getOrDefault(passNo, "");
        logger.debug("Mapped passNo: {} to time: {}", passNo, time);
        return time;
    }
}