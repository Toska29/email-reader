package com.toskafx.email_reader.dto;

import lombok.Builder;
import lombok.Getter;

import java.time.Instant;

/**
 * Immutable value object representing a parsed inbound email.
 * Passed from the email reading layer to the ticket creation layer.
 *
 * messageId is the RFC 2822 Message-ID header — it is the deduplication
 * key used to prevent the same email from generating multiple tickets,
 * even if the SEEN flag is changed back to unread on the mail server.
 */
@Getter
@Builder
public class InboundEmailDto {

    /** RFC 2822 Message-ID — globally unique, never changes. Deduplication key. */
    private final String messageId;

    private final String subject;
    private final String body;
    private final String senderEmail;
    private final String senderName;
    private final Instant receivedAt;
}