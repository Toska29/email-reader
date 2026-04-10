package com.toskafx.email_reader.service.outlook;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.toskafx.email_reader.config.EmailAccountProperties;
import com.toskafx.email_reader.enums.EmailProvider;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Instant;

/**
 * Fetches and caches a Microsoft OAuth2 access token using the
 * Client Credentials flow (app-only, no user interaction required).
 * <p>
 * This is appropriate for server-side daemons / background services
 * that read a shared/dedicated mailbox rather than acting on behalf
 * of an interactively-logged-in user.
 * <p>
 * Prerequisites in Azure AD (Entra ID):
 * 1. Register an application (App Registration).
 * 2. Under API Permissions → add Microsoft Graph → Application permission:
 * "Mail.Read" (or "IMAP.AccessAsApp" for IMAP-specific scope).
 * 3. Grant admin consent for the permission.
 * 4. Create a Client Secret and note the clientId, clientSecret, tenantId.
 * 5. Populate email.account.client-id / client-secret / tenant-id in application.yml.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class OutlookOAuthTokenService {

    private final EmailAccountProperties props;
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final HttpClient httpClient = HttpClient.newHttpClient();

    /**
     * Cached token value
     */
    private String cachedToken;

    /**
     * When the cached token expires (we refresh 60 s early for safety)
     */
    private Instant tokenExpiresAt = Instant.EPOCH;

    /**
     * Returns a valid Bearer access token, refreshing from Microsoft if
     * the current token has expired or is about to expire.
     */
    public synchronized String getAccessToken() {
        if (cachedToken != null && Instant.now().isBefore(tokenExpiresAt)) {
            return cachedToken;
        }
        log.info("Fetching new OAuth2 access token from Microsoft");
        return fetchNewToken();
    }

    private String fetchNewToken() {
        String tenantId = props.getTenantId();
        String clientId = props.getClientId();
        String clientSecret = props.getClientSecret();

        String tokenUrl = EmailProvider.OUTLOOK.getOauthTokenEndpoint()
                .replace("{tenantId}", tenantId);

        // Client Credentials grant — no user interaction, suitable for daemons
        String body = "grant_type=client_credentials"
                + "&client_id=" + encode(clientId)
                + "&client_secret=" + encode(clientSecret)
                // IMAP.AccessAsApp requires the .default scope of EWS / IMAP
                + "&scope=" + encode("https://outlook.office365.com/.default");

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(tokenUrl))
                .header("Content-Type", "application/x-www-form-urlencoded")
                .POST(HttpRequest.BodyPublishers.ofString(body))
                .build();

        try {
            HttpResponse<String> response = httpClient.send(request,
                    HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() != 200) {
                throw new RuntimeException(
                        "OAuth2 token request failed — HTTP " + response.statusCode()
                                + ": " + response.body());
            }

            JsonNode json = objectMapper.readTree(response.body());
            String accessToken = json.get("access_token").asText();
            log.info("Token preview: {}", accessToken.substring(0, Math.min(150, accessToken.length())));
//            log.info("Full Access Token: {}", accessToken);
            long expiresIn = json.get("expires_in").asLong();  // seconds

            cachedToken = accessToken;
            // Subtract 60 seconds to refresh slightly before actual expiry
            tokenExpiresAt = Instant.now().plusSeconds(expiresIn - 60);

            log.info("OAuth2 token obtained successfully, expires in {} seconds", expiresIn);
            return cachedToken;

        } catch (IOException | InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("Failed to fetch OAuth2 access token: " + e.getMessage(), e);
        }
    }

    private String encode(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8);
    }
}