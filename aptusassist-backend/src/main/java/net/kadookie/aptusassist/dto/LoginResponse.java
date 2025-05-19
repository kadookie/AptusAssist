package net.kadookie.aptusassist.dto;

import lombok.Data;
import okhttp3.OkHttpClient;

import java.util.List;
import java.util.Map;

/**
 * Response model for authentication operations with external booking system.
 * <p>
 * Usage:
 * <ul>
 * <li>Returned by
 * {@link net.kadookie.aptusassist.data.service.LoginService}</li>
 * <li>Passed to
 * {@link net.kadookie.aptusassist.data.service.BookingService}</li>
 * <li>Serialized to JSON for API responses</li>
 * </ul>
 * <p>
 * Security Considerations:
 * <ul>
 * <li>Contains authenticated HTTP client with session cookies</li>
 * <li>Should not be exposed directly to frontend</li>
 * <li>Response body may contain sensitive debug info</li>
 * </ul>
 * <p>
 * Serialization:
 * <ul>
 * <li>Uses Lombok {@code @Data} for getters/setters</li>
 * <li>Fields follow standard JSON naming</li>
 * <li>OkHttpClient is transient (not serialized)</li>
 * </ul>
 */
@Data
public class LoginResponse {
    /**
     * Indicates if authentication was successful
     * <p>
     * True if valid credentials and session established
     * </p>
     */
    private boolean success;
    /**
     * Detailed authentication status message
     * <p>
     * May contain error details if success=false
     * </p>
     */
    private String status;
    // private List<String> cookies;
    /**
     * Raw response body from authentication request
     * <p>
     * Used for debugging and error analysis
     * </p>
     */
    private String responseBody;
    /**
     * Authenticated HTTP client with session cookies
     * <p>
     * Required for all subsequent authenticated requests
     * </p>
     */
    private OkHttpClient okHttpClient;
    /**
     * Available booking slots (populated after successful authentication)
     * <p>
     * Each slot contains date, time, passNo and status fields
     * </p>
     */
    private List<Map<String, String>> slots;
}