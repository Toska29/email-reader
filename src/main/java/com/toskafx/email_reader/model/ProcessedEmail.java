package com.toskafx.email_reader.model;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;

/**
 * Idempotency record — one row per email that has been successfully
 * processed into a ticket.
 *
 * The messageId column stores the RFC 2822 Message-ID header value,
 * which is globally unique per email and never changes regardless of
 * what happens to IMAP flags on the server.
 *
 * Before any ticket is created, this table is checked first.
 * If a row exists for a given messageId, the email is skipped — even if
 * someone flipped the SEEN flag back to unread on the mail server.
 */
@Entity
@Table(
        name = "processed_email",
        indexes = {
                @Index(name = "idx_processed_email_message_id", columnList = "messageId", unique = true)
        }
)
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProcessedEmail {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * RFC 2822 Message-ID header — e.g. <unique-string@domain.com>
     * This is the deduplication key.
     */
    @Column(nullable = false, unique = true, length = 512)
    private String messageId;

    /** The email subject — stored for auditability */
    @Column(length = 998)
    private String subject;

    /** Sender address — stored for auditability */
    @Column(length = 320)
    private String senderEmail;

    /** When this email was first processed */
    @Column(nullable = false, updatable = false)
    private Instant processedAt;

    /** FK to the ticket that was created from this email */
    @Column
    private Long ticketId;
}