package com.toskafx.email_reader.service.ticket;

import com.toskafx.email_reader.dto.InboundEmailDto;

public interface TicketService {

    /**
     * Creates a ticket record from a parsed inbound email.
     *
     * @param email the parsed email DTO
     */
    void createTicketFromEmail(InboundEmailDto email);
}
