package com.toskafx.email_reader.service.ticket;

import com.toskafx.email_reader.dto.InboundEmailDto;

public interface TicketService {

    /**
     * Creates a ticket record from a parsed inbound email.
     *
     * @param email the parsed email DTO
     * @return the ID of the newly created ticket, used to populate the
     *         ProcessedEmail audit record for traceability
     */
    Long createTicketFromEmail(InboundEmailDto email);
}