package com.toskafx.email_reader.config;

import com.toskafx.email_reader.enums.EmailProvider;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Getter
@Setter
@Validated
@ConfigurationProperties(prefix = "email.account")
public class EmailAccountProperties {

    @NotBlank(message = "Email username must not be blank")
    private String username;

    /**
     * For GMAIL (APP_PASSWORD): the App Password generated from Google Account settings.
     * For OUTLOOK (OAUTH2): leave blank — authentication uses clientId + clientSecret + tenantId instead.
     */
    private String password;

    @NotNull(message = "Email provider must be specified (GMAIL or OUTLOOK)")
    private EmailProvider provider;

    /**
     * OAuth2 fields — required only when provider uses AuthMechanism.OAUTH2 (i.e. OUTLOOK).
     * Register an app in Azure AD (Entra ID) to obtain these values.
     */
    private String clientId;
    private String clientSecret;
    private String tenantId;

    /**
     * How often (in milliseconds) to poll for new emails.
     * Default: 60000 (1 minute)
     */
    private long pollingIntervalMs = 60_000;
}