package com.toskafx.email_reader.scheduler;

import com.toskafx.email_reader.dto.InboundEmailDto;
import com.toskafx.email_reader.model.ProcessedEmail;
import com.toskafx.email_reader.repository.ProcessedEmailRepository;
import com.toskafx.email_reader.service.email.EmailService;
import com.toskafx.email_reader.service.ticket.TicketService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class EmailPollingScheduler {

    private final EmailService emailService;
    private final TicketService ticketService;
    private final ProcessedEmailRepository processedEmailRepository;

    @Scheduled(fixedDelayString = "${email.account.polling-interval-ms:60000}")
    public void pollAndProcess() {
        log.info("Email polling cycle started");

        List<InboundEmailDto> unreadEmails = emailService.fetchUnreadEmails();

        if (unreadEmails.isEmpty()) {
            log.info("No new emails found");
            return;
        }

        int processed = 0;
        int skipped   = 0;
        int failed    = 0;

        for (InboundEmailDto email : unreadEmails) {
            String messageId = email.getMessageId();

            // ----------------------------------------------------------------
            // Deduplication check — consult our own database first.
            // The IMAP SEEN flag alone is not reliable: a human or another mail
            // client could flip it back to unread at any time. A row in
            // processed_emails is our authoritative record that a ticket was
            // already created for this email.
            // ----------------------------------------------------------------
            if (processedEmailRepository.existsByMessageId(messageId)) {
                log.debug("Skipping already-processed email [{}] — subject: '{}'",
                        messageId, email.getSubject());
                skipped++;
                continue;
            }

            try {
                // Create the ticket and capture its ID for the audit record
                Long ticketId = ticketService.createTicketFromEmail(email);

                // Record that this email has been processed — this is what
                // prevents duplicates on all future poll cycles
                processedEmailRepository.save(
                        ProcessedEmail.builder()
                                .messageId(messageId)
                                .subject(email.getSubject())
                                .senderEmail(email.getSenderEmail())
                                .processedAt(Instant.now())
                                .ticketId(ticketId)
                                .build()
                );

                // Best-effort: also mark as read on the mail server.
                // This reduces noise in the inbox but is NOT our deduplication
                // mechanism — the processed_emails table is.
                emailService.markAsRead(messageId);

                processed++;

            } catch (Exception e) {
                log.error("Failed to process email [{}] subject '{}': {}",
                        messageId, email.getSubject(), e.getMessage(), e);
                failed++;
                // Do NOT save to processed_emails on failure — we want to
                // retry this email on the next poll cycle
            }
        }

        log.info("Email polling cycle complete — processed: {}, skipped (duplicates): {}, failed: {}",
                processed, skipped, failed);
    }
}