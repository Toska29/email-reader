package com.toskafx.email_reader.service.ticket;

import com.toskafx.email_reader.dto.InboundEmailDto;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * Stub implementation of TicketService.
 *
 * In your main project replace this body with real persistence:
 *
 *   Ticket ticket = new Ticket();
 *   ticket.setTitle(email.getSubject());
 *   ticket.setDescription(email.getBody());
 *   ticket.setRequesterEmail(email.getSenderEmail());
 *   ticket.setRequesterName(email.getSenderName());
 *   ticket.setCreatedAt(email.getReceivedAt());
 *   ticket.setStatus(TicketStatus.OPEN);
 *   Ticket saved = ticketRepository.save(ticket);
 *   return saved.getId();   // <-- return the persisted ID
 */
@Slf4j
@Service
public class TicketServiceImpl implements TicketService {

    @Override
    public Long createTicketFromEmail(InboundEmailDto email) {
        log.info("Creating ticket — subject: '{}', sender: '{}' <{}>",
                email.getSubject(), email.getSenderName(), email.getSenderEmail());

        // TODO: replace with real ticket persistence in your main project
        // Return the saved ticket's ID so it can be stored in ProcessedEmail
        return null;
    }
}