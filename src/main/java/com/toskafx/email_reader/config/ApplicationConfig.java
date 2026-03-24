package com.toskafx.email_reader.config;

import jakarta.mail.Authenticator;
import jakarta.mail.PasswordAuthentication;
import jakarta.mail.Session;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.Properties;

@Configuration
@RequiredArgsConstructor
public class ApplicationConfig {

    private final EmailAccountProperties emailAccountProperties;

    /**
     * Builds a Jakarta Mail Session configured for IMAP reading.
     * Provider-specific host/port are resolved dynamically from EmailProvider enum,
     * so switching between Gmail and Outlook only requires a config change.
     */
    @Bean
    public Session emailSession() {
        Properties props = new Properties();

        String host = emailAccountProperties.getProvider().getImapHost();
        int port    = emailAccountProperties.getProvider().getImapPort();

        props.put("mail.imaps.host", host);
        props.put("mail.imaps.port", String.valueOf(port));
        props.put("mail.imaps.ssl.enable", "true");
        props.put("mail.imaps.auth", "true");
        props.put("mail.imaps.connectiontimeout", "10000");
        props.put("mail.imaps.timeout", "10000");
        props.put("mail.debug", "false");

        String username = emailAccountProperties.getUsername();
        String password = emailAccountProperties.getPassword();

        return Session.getInstance(props, new Authenticator() {
            @Override
            protected PasswordAuthentication getPasswordAuthentication() {
                return new PasswordAuthentication(username, password);
            }
        });
    }
}