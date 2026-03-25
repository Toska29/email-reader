package com.toskafx.email_reader.dto;

import lombok.Builder;
import lombok.Getter;

import java.time.Instant;
import java.util.Collections;
import java.util.List;

/**
 * Immutable value object representing a fully parsed inbound email,
 * including any file attachments extracted from the MIME structure.
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

    /**
     * Attachments extracted from the email's MIME structure.
     * Empty list if the email had no attachments — never null.
     */
    @Builder.Default
    private final List<EmailAttachmentDto> attachments = Collections.emptyList();
}