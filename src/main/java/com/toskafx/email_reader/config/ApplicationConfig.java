package com.toskafx.email_reader.config;

import com.toskafx.email_reader.enums.EmailProvider.AuthMechanism;
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
     * Builds a Jakarta Mail IMAP Session.
     * <p>
     * For APP_PASSWORD providers (Gmail):
     * Standard username + app-password authentication.
     * <p>
     * For OAUTH2 providers (Outlook):
     * Enables XOAUTH2 SASL mechanism. The actual Bearer token is injected
     * at connect-time in EmailServiceImpl, not here — because tokens expire
     * and must be refreshed per-connection, whereas this Session bean is
     * a singleton created once at startup.
     */
    @Bean
    public Session emailSession() {
        Properties props = new Properties();

        String host = emailAccountProperties.getProvider().getImapHost();
        int port = emailAccountProperties.getProvider().getImapPort();

        props.put("mail.imaps.host", host);
        props.put("mail.imaps.port", String.valueOf(port));
        props.put("mail.imaps.ssl.enable", "true");
        props.put("mail.imaps.auth", "true");
        props.put("mail.imaps.connectiontimeout", "10000");
        props.put("mail.imaps.timeout", "10000");
        props.put("mail.debug", "false");

        AuthMechanism mechanism = emailAccountProperties.getProvider().getAuthMechanism();

        if (mechanism == AuthMechanism.OAUTH2) {
            // Enable SASL and specifically the XOAUTH2 mechanism required by Microsoft
            props.put("mail.imaps.auth.mechanisms", "XOAUTH2");
            props.put("mail.imaps.sasl.enable", "true");
            props.put("mail.imaps.sasl.mechanisms", "XOAUTH2");
            props.put("mail.imaps.auth.login.disable", "true");
            props.put("mail.imaps.auth.plain.disable", "true");

            // Session with no Authenticator — OAuth2 token is passed at connect-time
            return Session.getInstance(props);
        }

        // APP_PASSWORD: standard username + password authenticator
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