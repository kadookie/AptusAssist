package net.kadookie.aptusassist.service;

import okhttp3.*;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import net.kadookie.aptusassist.dto.LoginResponse;
import net.kadookie.aptusassist.util.PasswordEncoder;

import java.io.IOException;
import java.net.CookieManager;
import java.util.HashSet;
import java.util.Set;

/**
 * Service handling the complex login flow to the Aptus Portal.
 * <p>
 * This service implements a multi-step authentication process that:
 * <ul>
 * <li>Follows redirects to reach the login page</li>
 * <li>Extracts CSRF tokens and password salts</li>
 * <li>Encrypts passwords using the portal's custom algorithm</li>
 * <li>Handles session cookies and headers</li>
 * <li>Validates successful login by checking the final page</li>
 * </ul>
 * <p>
 * Key Features:
 * <ul>
 * <li>Handles up to {@value #MAX_REDIRECTS} redirects before failing</li>
 * <li>Detects and breaks redirect loops</li>
 * <li>Maintains cookie state across requests</li>
 * <li>Provides detailed error information in {@link LoginResponse}</li>
 * </ul>
 * <p>
 * Performance Characteristics:
 * <ul>
 * <li>Makes 1-30 HTTP requests (due to redirects)</li>
 * <li>Parses HTML with Jsoup</li>
 * <li>Encrypts password with custom algorithm</li>
 * <li>O(n) time complexity where n is number of redirects</li>
 * </ul>
 * <p>
 * Thread Safety:
 * <ul>
 * <li>Instance is thread-safe after construction</li>
 * <li>Each login() call creates new LoginResponse</li>
 * <li>OkHttpClient is thread-safe</li>
 * </ul>
 * <p>
 * Configuration:
 * <ul>
 * <li>Requires APTUS_BASE_URL property</li>
 * <li>Uses OkHttp client with cookie management</li>
 * </ul>
 *
 * @see LoginResponse
 * @see PasswordEncoder
 * @see OkHttpClient
 */
@Service
public class LoginService {
    private static final Logger logger = LoggerFactory.getLogger(LoginService.class);
    private final OkHttpClient client;
    private final String baseUrl;
    private static final int MAX_REDIRECTS = 30;

    /**
     * Constructs a new LoginService with the specified base URL.
     * <p>
     * Initializes:
     * <ul>
     * <li>OkHttpClient with cookie management</li>
     * <li>Base URL for all external requests</li>
     * <li>Manual redirect handling (disabled automatic redirects)</li>
     * </ul>
     *
     * @param baseUrl The base URL of the Aptus Portal (e.g. "https://aptus.assaabloy.com") (required)
     * @throws IllegalArgumentException if baseUrl is null or empty
     * @see OkHttpClient
     */
    public LoginService(@Value("${APTUS_BASE_URL}") String baseUrl) {
        this.baseUrl = baseUrl;
        this.client = new OkHttpClient.Builder()
                .followRedirects(false)
                .cookieJar(new JavaNetCookieJar(new CookieManager()))
                .build();
        logger.info("LoginService initialized with baseUrl: {}", baseUrl);
    }

    /**
     * Performs the complete login flow to the Aptus Portal.
     * <p>
     * The login process involves:
     * <ol>
     * <li>Following redirects to reach the login page</li>
     * <li>Extracting CSRF token and password salt</li>
     * <li>Encrypting the password</li>
     * <li>Submitting the login form</li>
     * <li>Validating the final page</li>
     * </ol>
     * <p>
     * Performance Characteristics:
     * <ul>
     * <li>Makes 1-30 HTTP requests (due to redirects)</li>
     * <li>Parses HTML with Jsoup</li>
     * <li>Encrypts password with custom algorithm</li>
     * <li>O(n) time complexity where n is number of redirects</li>
     * </ul>
     *
     * @param username The Aptus Portal username (required, must match portal requirements)
     * @param password The Aptus Portal password (required, must match portal requirements)
     * @return LoginResponse containing:
     * <ul>
     * <li>Success/failure status</li>
     * <li>Error details if failed</li>
     * <li>Authenticated OkHttpClient if successful</li>
     * <li>Final response body</li>
     * </ul>
     * @throws IllegalArgumentException if:
     * <ul>
     * <li>username is null or empty</li>
     * <li>password is null or empty</li>
     * </ul>
     * @throws IllegalStateException if MAX_REDIRECTS is exceeded
     * @throws IOException if network error occurs
     * @see LoginResponse
     * @see PasswordEncoder#encStr(String, String)
     */
    public LoginResponse login(String username, String password) {
        logger.info("Attempting login for username: {}", username);
        LoginResponse response = new LoginResponse();
        Set<String> visitedUrls = new HashSet<>();
        String loginPageHtml = null;
        int redirectCount = 0;
        String currentUrl = baseUrl + "/";

        try {
            while (redirectCount < MAX_REDIRECTS) {
                if (!visitedUrls.add(currentUrl)) {
                    logger.warn("Possible redirect loop detected at: {}. Attempting fallback.", currentUrl);
                    currentUrl = baseUrl + "/AptusPortal/Account/Login";
                    if (!visitedUrls.add(currentUrl)) {
                        logger.error("Redirect loop persists at fallback URL: {}", currentUrl);
                        response.setSuccess(false);
                        response.setStatus("Redirect loop detected");
                        response.setResponseBody("Redirect loop detected at: " + currentUrl);
                        return response;
                    }
                }

                Request request = new Request.Builder()
                        .url(currentUrl)
                        .header("User-Agent",
                                "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/135.0.0.0 Safari/537.36")
                        .header("Accept",
                                "text/html,application/xhtml+xml,application/xml;q=0.9,image/avif,image/webp,image/apng,*/*;q=0.8,application/signed-exchange;v=b3;q=0.7")
                        .header("Accept-Encoding", "gzip, deflate, br, zstd")
                        .header("Accept-Language", "en-GB,en;q=0.9")
                        .header("Referer", baseUrl + "/")
                        .header("Upgrade-Insecure-Requests", "1")
                        .header("sec-ch-ua",
                                "\"Google Chrome\";v=\"135\", \"Not-A.Brand\";v=\"8\", \"Chromium\";v=\"135\"")
                        .header("sec-ch-ua-mobile", "?0")
                        .header("sec-ch-ua-platform", "\"macOS\"")
                        .header("sec-fetch-dest", "document")
                        .header("sec-fetch-mode", "navigate")
                        .header("sec-fetch-site", "same-origin")
                        .build();

                Response tempResponse = client.newCall(request).execute();
                if (tempResponse.code() == 200) {
                    if (tempResponse.body() == null) {
                        logger.error("Empty response body for URL: {}", currentUrl);
                        response.setSuccess(false);
                        response.setStatus("Empty response body");
                        response.setResponseBody("Empty response body");
                        tempResponse.close();
                        return response;
                    }
                    loginPageHtml = tempResponse.body().string();

                    Document doc = Jsoup.parse(loginPageHtml);
                    String requestVerificationToken = doc.select("input[name=__RequestVerificationToken]")
                            .attr("value");
                    if (!requestVerificationToken.isEmpty()) {
                        tempResponse.close();
                        break;
                    } else {
                        logger.debug("No RequestVerificationToken found, redirecting to: {}", baseUrl
                                + "/AptusPortal");
                        currentUrl = baseUrl + "/AptusPortal";
                        redirectCount++;
                        tempResponse.close();
                        continue;
                    }
                } else if (tempResponse.code() == 302 || tempResponse.code() == 301) {
                    String location = tempResponse.header("Location");
                    if (location == null) {
                        logger.error("No Location header in redirect for URL: {}", currentUrl);
                        response.setSuccess(false);
                        response.setStatus("Missing Location header");
                        response.setResponseBody("Missing Location header");
                        tempResponse.close();
                        return response;
                    }
                    currentUrl = normalizeUrl(resolveUrl(currentUrl, location));
                    logger.debug("Redirecting to: {}", currentUrl);
                    redirectCount++;
                    tempResponse.close();
                } else if (tempResponse.code() == 404) {
                    logger.error("404 Not Found for: {}", currentUrl);
                    if (tempResponse.body() != null) {
                        String errorBody = tempResponse.body().string();
                        logger.debug("Error body snippet: {}",
                                errorBody.length() > 100 ? errorBody.substring(0, 100) + "..." : errorBody);
                        response.setResponseBody(errorBody);
                    }
                    logger.info("Attempting fallback to: {}", baseUrl + "/AptusPortal/Account/Login");
                    currentUrl = baseUrl + "/AptusPortal/Account/Login";
                    redirectCount++;
                    tempResponse.close();
                } else {
                    logger.error("Unexpected status code: {} for URL: {}", tempResponse.code(), currentUrl);
                    if (tempResponse.body() != null) {
                        String errorBody = tempResponse.body().string();
                        logger.debug("Error body snippet: {}",
                                errorBody.length() > 100 ? errorBody.substring(0, 100) + "..." : errorBody);
                        response.setResponseBody(errorBody);
                    }
                    response.setSuccess(false);
                    response.setStatus("Unexpected status: " + tempResponse.code());
                    tempResponse.close();
                    return response;
                }
            }

            if (loginPageHtml == null) {
                logger.error("Too many redirects or fallbacks: {}", redirectCount);
                response.setSuccess(false);
                response.setStatus("Too many redirects or fallbacks: " + redirectCount);
                response.setResponseBody("Too many redirects or fallbacks: " + redirectCount);
                return response;
            }

            Document doc = Jsoup.parse(loginPageHtml);
            String passwordSalt = doc.select("input#PasswordSalt").attr("value");
            String requestVerificationToken = doc.select("input[name=__RequestVerificationToken]").attr("value");
            logger.debug("Parsed PasswordSalt: {}, RequestVerificationToken: {}",
                    passwordSalt,
                    requestVerificationToken);

            if (requestVerificationToken.isEmpty()) {
                logger.error("Failed to parse __RequestVerificationToken");
                response.setSuccess(false);
                response.setStatus("Failed to parse verification token");
                response.setResponseBody(loginPageHtml);
                return response;
            }

            String encryptedPassword = PasswordEncoder.encStr(password, passwordSalt);
            logger.debug("Encrypted password generated");

            RequestBody formBody = new FormBody.Builder()
                    .add("DeviceType", "PC")
                    .add("DesktopSelected", "true")
                    .add("__RequestVerificationToken", requestVerificationToken)
                    .add("UserName", username)
                    .add("Password", password)
                    .add("PwEnc", encryptedPassword)
                    .add("PasswordSalt", passwordSalt)
                    .build();

            Request loginRequest = new Request.Builder()
                    .url(baseUrl + "/AptusPortal/Account/Login?ReturnUrl=%2fAptusPortal%2f")
                    .post(formBody)
                    .header("User-Agent",
                            "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/135.0.0.0 Safari/537.36")
                    .header("Accept",
                            "text/html,application/xhtml+xml,application/xml;q=0.9,image/avif,image/webp,image/apng,*/*;q=0.8,application/signed-exchange;v=b3;q=0.7")
                    .header("Accept-Encoding", "gzip, deflate, br, zstd")
                    .header("Accept-Language", "en-GB,en;q=0.9")
                    .header("Referer", baseUrl + "/AptusPortal")
                    .header("Content-Type", "application/x-www-form-urlencoded")
                    .header("Upgrade-Insecure-Requests", "1")
                    .header("sec-ch-ua", "\"Google Chrome\";v=\"135\", \"Not-A.Brand\";v=\"8\", \"Chromium\";v=\"135\"")
                    .header("sec-ch-ua-mobile", "?0")
                    .header("sec-ch-ua-platform", "\"macOS\"")
                    .header("sec-fetch-dest", "document")
                    .header("sec-fetch-mode", "navigate")
                    .header("sec-fetch-site", "same-origin")
                    .build();

            Response loginResponse = client.newCall(loginRequest).execute();
            String finalResponseBody = null;

            if (loginResponse.code() == 302) {
                String location = loginResponse.header("Location");
                if (location != null) {
                    String finalUrl = normalizeUrl(resolveUrl(currentUrl, location));
                    Request finalRequest = new Request.Builder()
                            .url(finalUrl)
                            .header("User-Agent",
                                    "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/135.0.0.0 Safari/537.36")
                            .header("Accept",
                                    "text/html,application/xhtml+xml,application/xml;q=0.9,image/avif,image/webp,image/apng,*/*;q=0.8,application/signed-exchange;v=b3;q=0.7")
                            .header("Accept-Encoding", "gzip, deflate, br, zstd")
                            .header("Accept-Language", "en-GB,en;q=0.9")
                            .header("Referer", baseUrl + "/AptusPortal")
                            .header("Upgrade-Insecure-Requests", "1")
                            .header("sec-ch-ua",
                                    "\"Google Chrome\";v=\"135\", \"Not-A.Brand\";v=\"8\", \"Chromium\";v=\"135\"")
                            .header("sec-ch-ua-mobile", "?0")
                            .header("sec-ch-ua-platform", "\"macOS\"")
                            .header("sec-fetch-dest", "document")
                            .header("sec-fetch-mode", "navigate")
                            .header("sec-fetch-site", "same-origin")
                            .build();
                    Response finalResponse = client.newCall(finalRequest).execute();

                    if (finalResponse.body() != null) {
                        finalResponseBody = finalResponse.body().string();
                        Document finalDoc = Jsoup.parse(finalResponseBody);
                        String pageTitle = finalDoc.select("title").text();
                        if (pageTitle.contains("Hem - Aptusportal")) {
                            response.setSuccess(true);
                            response.setStatus("Login successful - Portal homepage reached");
                            logger.info("Login successful for username: {}", username);
                        } else {
                            logger.error("Login validation failed: Expected portal homepage, got: {}", pageTitle);
                            response.setSuccess(false);
                            response.setStatus("Login failed - Unexpected page: " + pageTitle);
                        }
                        finalResponse.close();
                    } else {
                        logger.error("Empty final response body");
                        response.setSuccess(false);
                        response.setStatus("Empty final response body");
                        finalResponse.close();
                    }
                    loginResponse.close();
                } else {
                    logger.error("No Location header in login redirect");
                    response.setSuccess(false);
                    response.setStatus("Missing Location header in login redirect");
                    response.setResponseBody("Missing Location header");
                    loginResponse.close();
                    return response;
                }
            } else if (loginResponse.isSuccessful()) {
                if (loginResponse.body() == null) {
                    logger.error("Empty response body from login request");
                    response.setSuccess(false);
                    response.setStatus("Empty login response body");
                    response.setResponseBody("Empty login response body");
                    loginResponse.close();
                    return response;
                }
                finalResponseBody = loginResponse.body().string();
                Document finalDoc = Jsoup.parse(finalResponseBody);
                String pageTitle = finalDoc.select("title").text();
                if (pageTitle.contains("Hem - Aptusportal")) {
                    response.setSuccess(true);
                    response.setStatus("Login successful - Portal homepage reached");
                    logger.info("Login successful for username: {}", username);
                } else {
                    logger.error("Login validation failed: Expected portal homepage, got: {}", pageTitle);
                    response.setSuccess(false);
                    response.setStatus("Login failed - Unexpected page: " + pageTitle);
                }
                loginResponse.close();
            } else {
                logger.error("Login failed, status code: {}", loginResponse.code());
                if (loginResponse.body() != null) {
                    finalResponseBody = loginResponse.body().string();
                    logger.debug("Login error response snippet: {}",
                            finalResponseBody.length() > 100 ? finalResponseBody.substring(0, 100) +
                                    "..."
                                    : finalResponseBody);
                }
                response.setSuccess(false);
                response.setStatus("Login failed: " + loginResponse.code());
                loginResponse.close();
            }

            response.setResponseBody(finalResponseBody);
            response.setOkHttpClient(client);

        } catch (IOException e) {
            logger.error("Error during login for username: {}", username, e);
            response.setSuccess(false);
            response.setStatus("Error: " + e.getMessage());
            response.setResponseBody("IOException: " + e.getMessage());
        }

        return response;
    }

    /**
     * Resolves a relative URL against a base URL.
     *
     * @param baseUrl The base URL to resolve against (required)
     * @param location The relative or absolute location (required)
     * @return Fully resolved absolute URL
     * @throws IllegalArgumentException if:
     * <ul>
     * <li>baseUrl is null</li>
     * <li>location is null</li>
     * </ul>
     * @throws StringIndexOutOfBoundsException if baseUrl is malformed
     */
    private String resolveUrl(String baseUrl, String location) {
        if (location.startsWith("http")) {
            return location;
        }
        if (location.startsWith("/")) {
            return this.baseUrl + location;
        }
        String basePath = baseUrl.substring(0, baseUrl.lastIndexOf('/') + 1);
        return basePath + location;
    }

    /**
     * Normalizes URLs by removing duplicate path segments.
     *
     * @param url The URL to normalize (required)
     * @return Normalized URL with duplicate segments removed
     * @throws IllegalArgumentException if url is null
     * @throws PatternSyntaxException if regex replacement fails
     */
    private String normalizeUrl(String url) {
        String normalized = url.replaceAll("/aptusportal/aptusportal/", "/aptusportal/");
        return normalized;
    }
}