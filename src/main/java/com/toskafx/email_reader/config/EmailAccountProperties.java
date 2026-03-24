package com.toskafx.email_reader.config;

import com.toskafx.email_reader.enums.EmailProvider;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;
import org.springframework.validation.annotation.Validated;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

@Getter
@Setter
@Validated
@Component
@ConfigurationProperties(prefix = "email.account")
public class EmailAccountProperties {

    @NotBlank(message = "Email username must not be blank")
    private String username;

    @NotBlank(message = "Email password must not be blank")
    private String password;

    @NotNull(message = "Email provider must be specified (GMAIL or OUTLOOK)")
    private EmailProvider provider;

    /**
     * How often (in milliseconds) to poll for new emails.
     * Default: 60000 (1 minute)
     */
    private long pollingIntervalMs = 60_000;
}