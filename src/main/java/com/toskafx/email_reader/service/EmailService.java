package com.toskafx.email_reader.service;

import com.toskafx.email_reader.dto.InboundEmailDto;

import java.util.List;

public interface EmailService {

    /**
     * Fetches all unread/unseen emails from the configured inbox.
     *
     * @return list of parsed email DTOs ready for downstream processing
     */
    List<InboundEmailDto> fetchUnreadEmails();

    /**
     * Marks a message as seen/read on the mail server by its Message-ID header.
     *
     * @param messageId the RFC 2822 Message-ID of the email to mark as read
     */
    void markAsRead(String messageId);
}