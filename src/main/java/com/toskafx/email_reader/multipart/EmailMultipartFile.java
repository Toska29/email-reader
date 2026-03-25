package com.toskafx.email_reader.multipart;

import com.toskafx.email_reader.dto.EmailAttachmentDto;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;

/**
 * Adapter that makes an email attachment look like a Spring MultipartFile.
 *
 * Why this exists:
 * The existing attachmentService.uploadAttachment(MultipartFile) expects
 * Spring's HTTP multipart abstraction. Email attachments arrive as Jakarta
 * Mail BodyPart objects. Rather than modifying the existing upload pipeline,
 * this adapter bridges the two worlds — email attachments are wrapped here
 * and passed straight into the unmodified upload service.
 *
 * Memory model:
 * The attachment bytes are already fully in memory inside EmailAttachmentDto,
 * consistent with how Spring's StandardMultipartFile works for HTTP uploads.
 * This is acceptable because the parent application already has:
 *   spring.servlet.multipart.max-file-size=10MB
 * which sets the same upper bound for HTTP uploads. Email attachments will
 * naturally respect this same limit — oversized attachments should be
 * validated before calling uploadAttachment() if needed.
 */
public class EmailMultipartFile implements MultipartFile {

    private final EmailAttachmentDto attachment;

    public EmailMultipartFile(EmailAttachmentDto attachment) {
        this.attachment = attachment;
    }

    /**
     * The form field name — not meaningful for email attachments,
     * but required by the MultipartFile contract.
     */
    @Override
    public String getName() {
        return attachment.getFilename();
    }

    /** The original filename from the email's Content-Disposition header */
    @Override
    public String getOriginalFilename() {
        return attachment.getFilename();
    }

    /** MIME type from the email part's Content-Type header */
    @Override
    public String getContentType() {
        return attachment.getContentType();
    }

    @Override
    public boolean isEmpty() {
        return attachment.getContent() == null || attachment.getContent().length == 0;
    }

    @Override
    public long getSize() {
        return attachment.getSize();
    }

    @Override
    public byte[] getBytes() throws IOException {
        return attachment.getContent();
    }

    @Override
    public InputStream getInputStream() throws IOException {
        return new ByteArrayInputStream(attachment.getContent());
    }

    /**
     * Transfer to a file destination — not used in the email pipeline
     * since attachmentService.uploadAttachment() reads via getBytes()/getInputStream(),
     * but implemented to satisfy the MultipartFile contract fully.
     */
    @Override
    public void transferTo(java.io.File dest) throws IOException, IllegalStateException {
        try (var out = new java.io.FileOutputStream(dest)) {
            out.write(attachment.getContent());
        }
    }
}