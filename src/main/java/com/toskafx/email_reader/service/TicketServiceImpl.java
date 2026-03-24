package com.toskafx.email_reader.service;

import com.toskafx.email_reader.dto.InboundEmailDto;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * Stub implementation of TicketService.
 *
 * In your main project, replace this with the real implementation that
 * maps InboundEmailDto fields to your existing Ticket model and persists
 * via your TicketRepository.
 *
 * Example mapping:
 *   ticket.setTitle(email.getSubject());
 *   ticket.setDescription(email.getBody());
 *   ticket.setRequesterEmail(email.getSenderEmail());
 *   ticket.setRequesterName(email.getSenderName());
 *   ticket.setCreatedAt(email.getReceivedAt());
 *   ticket.setStatus(TicketStatus.OPEN);
 *   ticketRepository.save(ticket);
 */
@Slf4j
@Service
public class TicketServiceImpl implements TicketService {

    @Override
    public void createTicketFromEmail(InboundEmailDto email) {
        log.info("Creating ticket from email — subject: '{}', sender: '{}' <{}>",
                email.getSubject(),
                email.getSenderName(),
                email.getSenderEmail());

        // TODO: Replace with real ticket persistence in your main project
        // Ticket ticket = new Ticket();
        // ticket.setTitle(email.getSubject());
        // ticket.setDescription(email.getBody());
        // ticket.setRequesterEmail(email.getSenderEmail());
        // ticket.setRequesterName(email.getSenderName());
        // ticket.setCreatedAt(email.getReceivedAt());
        // ticket.setStatus(TicketStatus.OPEN);
        // ticketRepository.save(ticket);
    }
}