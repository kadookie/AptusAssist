package net.kadookie.aptusassist.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Simple password encoding utility using XOR operation.
 * <p>
 * <strong>Security Warning:</strong>
 * <ul>
 * <li>XOR provides trivial obfuscation only</li>
 * <li>Vulnerable to known-plaintext attacks</li>
 * <li>No protection against brute force</li>
 * <li>Salt is not properly randomized</li>
 * </ul>
 * <p>
 * Migration Recommendations:
 * <ul>
 * <li>Replace with BCryptPasswordEncoder for production</li>
 * <li>Existing passwords must be re-encoded</li>
 * <li>Consider Spring Security's password encoding</li>
 * </ul>
 * <p>
 * Performance Characteristics:
 * <ul>
 * <li>Very fast (O(n) where n is password length)</li>
 * <li>No memory overhead</li>
 * <li>No external dependencies</li>
 * </ul>
 * <p>
 * Example Usage:
 * 
 * <pre>{@code
 * // Temporary development use only
 * String encoded = PasswordEncoder.encStr("password", "12345");
 * }</pre>
 */
public class PasswordEncoder {
    private static final Logger logger = LoggerFactory.getLogger(PasswordEncoder.class);

    /**
     * Encodes password using XOR operation with numeric salt.
     * <p>
     * If salt is invalid (non-numeric or empty), returns plain password.
     *
     * @param password Plaintext password to encode
     * @param salt     Numeric string used as XOR key
     * @return Encoded string or original password on error
     */
    public static String encStr(String password, String salt) {
        if (salt == null || salt.trim().isEmpty()) {
            logger.warn("Salt is empty; returning plain password");
            return password;
        }

        try {
            int key = Integer.parseInt(salt);
            StringBuilder result = new StringBuilder();
            for (char c : password.toCharArray()) {
                result.append((char) (key ^ (int) c));
            }
            String encrypted = result.toString();
            // logger.debug("Encrypted password with salt: {}", salt);
            return encrypted;
        } catch (NumberFormatException e) {
            logger.error("Invalid salt format: {}", salt, e);
            return password;
        }
    }
}