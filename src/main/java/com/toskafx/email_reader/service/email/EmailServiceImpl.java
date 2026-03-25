package com.toskafx.email_reader.service.email;

import com.toskafx.email_reader.config.EmailAccountProperties;
import com.toskafx.email_reader.dto.InboundEmailDto;
import com.toskafx.email_reader.enums.EmailProvider.AuthMechanism;
import com.toskafx.email_reader.service.outlook.OutlookOAuthTokenService;
import com.toskafx.email_reader.util.EmailBodyExtractor;
import jakarta.mail.*;
import jakarta.mail.internet.InternetAddress;
import jakarta.mail.search.AndTerm;
import jakarta.mail.search.ComparisonTerm;
import jakarta.mail.search.FlagTerm;
import jakarta.mail.search.ReceivedDateTerm;
import jakarta.mail.search.SearchTerm;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class EmailServiceImpl implements EmailService {

    private final Session session;
    private final EmailAccountProperties emailAccountProperties;
    private final OutlookOAuthTokenService oAuthTokenService;

    @Override
    public List<InboundEmailDto> fetchUnreadEmails() {
        String host     = emailAccountProperties.getProvider().getImapHost();
        String username = emailAccountProperties.getUsername();

        List<InboundEmailDto> result = new ArrayList<>();

        try (Store store = session.getStore("imaps")) {
            connectStore(store, host, username);

            try (Folder inbox = store.getFolder("INBOX")) {
                inbox.open(Folder.READ_WRITE);

                // Compound search: UNSEEN AND received today
                SearchTerm unseenTerm    = new FlagTerm(new Flags(Flags.Flag.SEEN), false);
                SearchTerm receivedToday = todayReceivedDateTerm();
                SearchTerm combinedTerm  = new AndTerm(unseenTerm, receivedToday);

                Message[] messages = inbox.search(combinedTerm);

                log.info("Found {} unread email(s) today in inbox for [{}]", messages.length, username);

                for (Message message : messages) {
                    try {
                        result.add(parseMessage(message));
                    } catch (Exception e) {
                        log.error("Skipping message due to parse error: {}", e.getMessage(), e);
                    }
                }
            }

        } catch (MessagingException e) {
            log.error("Failed to connect to mail server [{}]: {}", host, e.getMessage(), e);
        }

        return Collections.unmodifiableList(result);
    }

    @Override
    public void markAsRead(String messageId) {
        String host     = emailAccountProperties.getProvider().getImapHost();
        String username = emailAccountProperties.getUsername();

        try (Store store = session.getStore("imaps")) {
            connectStore(store, host, username);

            try (Folder inbox = store.getFolder("INBOX")) {
                inbox.open(Folder.READ_WRITE);

                Message[] allMessages = inbox.getMessages();
                for (Message message : allMessages) {
                    String[] headers = message.getHeader("Message-ID");
                    if (headers != null && Arrays.asList(headers).contains(messageId)) {
                        message.setFlag(Flags.Flag.SEEN, true);
                        log.info("Marked message [{}] as read", messageId);
                        return;
                    }
                }

                log.warn("No message found with Message-ID [{}] to mark as read", messageId);
            }

        } catch (MessagingException e) {
            log.error("Failed to mark message as read [{}]: {}", messageId, e.getMessage(), e);
        }
    }

    // -------------------------------------------------------------------------
    // Private helpers
    // -------------------------------------------------------------------------

    private void connectStore(Store store, String host, String username) throws MessagingException {
        AuthMechanism mechanism = emailAccountProperties.getProvider().getAuthMechanism();

        if (mechanism == AuthMechanism.OAUTH2) {
            String accessToken = oAuthTokenService.getAccessToken();
            store.connect(host, username, accessToken);
            log.debug("Connected to [{}] via OAuth2/XOAUTH2", host);
        } else {
            String password = emailAccountProperties.getPassword();
            store.connect(host, username, password);
            log.debug("Connected to [{}] via App Password", host);
        }
    }

    private SearchTerm todayReceivedDateTerm() {
        Date startOfToday = Date.from(
                LocalDate.now(ZoneId.systemDefault())
                        .atStartOfDay(ZoneId.systemDefault())
                        .toInstant()
        );
        return new ReceivedDateTerm(ComparisonTerm.GE, startOfToday);
    }

    private InboundEmailDto parseMessage(Message message) throws Exception {
        // Message-ID is the RFC 2822 unique identifier — this is our deduplication key.
        // Every compliant email server sets this. We fall back to a synthetic ID
        // only as a last resort for malformed/non-standard messages.
        String[] messageIdHeaders = message.getHeader("Message-ID");
        String messageId = (messageIdHeaders != null && messageIdHeaders.length > 0)
                ? messageIdHeaders[0].trim()
                : generateFallbackId(message);

        String subject = message.getSubject() != null ? message.getSubject() : "(no subject)";
        String body    = EmailBodyExtractor.extract(message);

        Address[] from = message.getFrom();
        String senderEmail = "";
        String senderName  = "";

        if (from != null && from.length > 0) {
            Address sender = from[0];
            if (sender instanceof InternetAddress ia) {
                senderEmail = ia.getAddress() != null ? ia.getAddress() : "";
                senderName  = ia.getPersonal() != null ? ia.getPersonal() : senderEmail;
            } else {
                senderEmail = sender.toString();
                senderName  = sender.toString();
            }
        }

        Instant receivedAt = message.getReceivedDate() != null
                ? message.getReceivedDate().toInstant()
                : Instant.now();

        log.debug("Parsed email — messageId: '{}', subject: '{}', from: '{}' <{}>",
                messageId, subject, senderName, senderEmail);

        return InboundEmailDto.builder()
                .messageId(messageId)
                .subject(subject)
                .body(body)
                .senderEmail(senderEmail)
                .senderName(senderName)
                .receivedAt(receivedAt)
                .build();
    }

    /**
     * Safety net for the rare case where a message has no Message-ID header.
     * Produces a deterministic ID from available fields so the same malformed
     * email always maps to the same fallback ID across poll cycles.
     */
    private String generateFallbackId(Message message) {
        try {
            String subject = message.getSubject() != null ? message.getSubject() : "";
            String date    = message.getReceivedDate() != null
                    ? message.getReceivedDate().toString() : "";
            Address[] from = message.getFrom();
            String sender  = (from != null && from.length > 0) ? from[0].toString() : "";
            return "fallback-" + Integer.toHexString((subject + sender + date).hashCode());
        } catch (MessagingException e) {
            return "fallback-" + System.nanoTime();
        }
    }
}