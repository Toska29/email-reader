package com.toskafx.email_reader.service;

import com.toskafx.email_reader.config.EmailAccountProperties;
import com.toskafx.email_reader.dto.InboundEmailDto;
import com.toskafx.email_reader.util.EmailBodyExtractor;
import jakarta.mail.*;
import jakarta.mail.internet.InternetAddress;
import jakarta.mail.search.FlagTerm;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class EmailServiceImpl implements EmailService {

    private final Session session;
    private final EmailAccountProperties emailAccountProperties;

    @Override
    public List<InboundEmailDto> fetchUnreadEmails() {
        String host     = emailAccountProperties.getProvider().getImapHost();
        String username = emailAccountProperties.getUsername();
        String password = emailAccountProperties.getPassword();

        List<InboundEmailDto> result = new ArrayList<>();

        try (Store store = session.getStore("imaps")) {
            store.connect(host, username, password);

            try (Folder inbox = store.getFolder("INBOX")) {
                inbox.open(Folder.READ_WRITE); // READ_WRITE required to mark messages as seen

                // Fetch only UNSEEN messages — no re-processing of already-read emails
                Message[] messages = inbox.search(
                        new FlagTerm(new Flags(Flags.Flag.SEEN), false)
                );

                log.info("Found {} unread email(s) in inbox for [{}]", messages.length, username);

                for (Message message : messages) {
                    try {
                        InboundEmailDto dto = parseMessage(message);
                        result.add(dto);
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
        String password = emailAccountProperties.getPassword();

        try (Store store = session.getStore("imaps")) {
            store.connect(host, username, password);

            try (Folder inbox = store.getFolder("INBOX")) {
                inbox.open(Folder.READ_WRITE);

                // Search all messages and match by Message-ID header
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

    private InboundEmailDto parseMessage(Message message) throws Exception {
        // Subject
        String subject = message.getSubject() != null ? message.getSubject() : "(no subject)";

        // Body — handles plain text, HTML, and multipart
        String body = EmailBodyExtractor.extract(message);

        // Sender — gracefully handle both InternetAddress and plain Address
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

        // Received date — fall back to now if missing
        Instant receivedAt = message.getReceivedDate() != null
                ? message.getReceivedDate().toInstant()
                : Instant.now();

        log.debug("Parsed email — subject: '{}', from: '{}' <{}>", subject, senderName, senderEmail);

        return InboundEmailDto.builder()
                .subject(subject)
                .body(body)
                .senderEmail(senderEmail)
                .senderName(senderName)
                .receivedAt(receivedAt)
                .build();
    }
}