package com.toskafx.email_reader.service.ticket;

import com.toskafx.email_reader.dto.InboundEmailDto;
import com.toskafx.email_reader.multipart.EmailMultipartFile;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

/**
 * Stub showing how to migrate createTicketFromEmail() into the parent application.
 *
 * In the parent application this class will:
 *   1. Map InboundEmailDto fields onto a CreateTicketDto
 *   2. Convert EmailAttachmentDto list into List<MultipartFile> via EmailMultipartFile
 *   3. Delegate to the existing createTicket(List<MultipartFile>, CreateTicketDto)
 *      — which already handles attachment upload via attachmentService.uploadAttachment()
 *
 * The existing createTicket() pipeline is entirely reused, with no modification.
 * The adapter pattern (EmailMultipartFile) is what makes this possible.
 */
@Slf4j
@Service
public class TicketServiceImpl implements TicketService {

    // In the parent application, inject the real dependencies:
    // private final AttachmentService attachmentService;
    // private final TicketRepository ticketRepository;

    @Override
    public Long createTicketFromEmail(InboundEmailDto email) {
        log.info("Creating ticket from email — subject: '{}', sender: '{}' <{}>, attachments: {}",
                email.getSubject(),
                email.getSenderName(),
                email.getSenderEmail(),
                email.getAttachments().size());

        // -----------------------------------------------------------------
        // Step 1: Build CreateTicketDto from the email fields
        // -----------------------------------------------------------------
        // In the parent application:
        //
        // CreateTicketDto dto = new CreateTicketDto();
        // dto.setComplaintDetail(email.getBody());
        // dto.setComplainantEmail(email.getSenderEmail());
        // dto.setComplainantName(email.getSenderName());
        // dto.setSourceOfComplaint(ComplaintSource.EMAIL);
        // ... map any other fields your ticket form requires

        // -----------------------------------------------------------------
        // Step 2: Convert email attachments → List<MultipartFile>
        //
        // EmailMultipartFile adapts each EmailAttachmentDto into a
        // MultipartFile so the existing upload pipeline works unchanged.
        // Your existing loop in createTicket():
        //
        //   for (MultipartFile file : files) {
        //       attachments.add(attachmentService.uploadAttachment(file));
        //   }
        //
        // will receive these and process them identically to HTTP uploads.
        // -----------------------------------------------------------------
        List<MultipartFile> files = email.getAttachments()
                .stream()
                .map(EmailMultipartFile::new)
                .map(f -> (MultipartFile) f)
                .toList();

        // -----------------------------------------------------------------
        // Step 3: Delegate to the existing createTicket() — no changes needed there
        // -----------------------------------------------------------------
        // In the parent application:
        //
        // TicketResponse response = createTicket(files, dto);
        // return response.getId();   // return the ticket ID for the ProcessedEmail record

        // TODO: replace with real implementation — return the created ticket's ID
        return null;
    }
}