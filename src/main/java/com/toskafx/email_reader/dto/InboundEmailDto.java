package com.toskafx.email_reader.dto;

import lombok.Builder;
import lombok.Getter;

import java.time.Instant;

/**
 * Immutable value object representing a parsed inbound email.
 * This is what gets passed to the ticket creation layer.
 */
@Getter
@Builder
public class InboundEmailDto {

    private final String subject;
    private final String body;
    private final String senderEmail;
    private final String senderName;
    private final Instant receivedAt;
}