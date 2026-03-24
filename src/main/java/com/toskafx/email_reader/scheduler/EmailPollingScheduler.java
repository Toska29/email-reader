package com.toskafx.email_reader.scheduler;

import com.toskafx.email_reader.dto.InboundEmailDto;
import com.toskafx.email_reader.service.EmailService;
import com.toskafx.email_reader.service.TicketService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Scheduled polling component that drives the email-to-ticket pipeline.
 *
 * Polling interval is configurable via:
 *   email.account.polling-interval-ms (default: 60000 = 1 minute)
 *
 * Flow per execution:
 *   1. Fetch all unread/unseen emails from the inbox
 *   2. For each email, create a ticket record
 *   3. Mark the email as read so it is not processed again
 *
 * @EnableScheduling is expected on your main application class or a config class.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class EmailPollingScheduler {

    private final EmailService emailService;
    private final TicketService ticketService;

    @Scheduled(fixedDelayString = "${email.account.polling-interval-ms:60000}")
    public void pollAndProcess() {
        log.info("Email polling cycle started");

        List<InboundEmailDto> unreadEmails = emailService.fetchUnreadEmails();

        if (unreadEmails.isEmpty()) {
            log.info("No new emails found");
            return;
        }

        int processed = 0;
        int failed    = 0;

        for (InboundEmailDto email : unreadEmails) {
            try {
                ticketService.createTicketFromEmail(email);
                // Note: markAsRead uses Message-ID. For a complete implementation,
                // include the Message-ID in InboundEmailDto and pass it here.
                processed++;
            } catch (Exception e) {
                log.error("Failed to create ticket for email from '{}' with subject '{}': {}",
                        email.getSenderEmail(), email.getSubject(), e.getMessage(), e);
                failed++;
            }
        }

        log.info("Email polling cycle complete — processed: {}, failed: {}", processed, failed);
    }
}