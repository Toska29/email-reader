package com.toskafx.email_reader.util;

import com.toskafx.email_reader.dto.EmailAttachmentDto;
import jakarta.mail.BodyPart;
import jakarta.mail.MessagingException;
import jakarta.mail.Multipart;
import jakarta.mail.Part;
import lombok.experimental.UtilityClass;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.IOUtils;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

/**
 * Utility for extracting both the readable body text and attachments from
 * a Jakarta Mail Message.
 *
 * Email MIME structures come in three forms:
 *   - Simple:          text/plain or text/html at the top level
 *   - Alternative:     multipart/alternative — multiple formats of the same content
 *                      (plain + html), pick the best one
 *   - Mixed/related:   multipart/mixed or multipart/related — body + attachments
 *
 * This extractor walks the entire MIME tree in one pass, collecting both
 * the best available body text and all file attachments simultaneously.
 */
@Slf4j
@UtilityClass
public class EmailBodyExtractor {

    /**
     * Result container holding both the extracted body and any attachments
     * found in the same MIME tree traversal.
     */
    public record ExtractionResult(String body, List<EmailAttachmentDto> attachments) {
        public static ExtractionResult empty() {
            return new ExtractionResult("", new ArrayList<>());
        }
    }

    /**
     * Entry point — extracts body text and attachments from a message Part in one pass.
     *
     * @param part the top-level message or nested body part
     * @return ExtractionResult containing body text and list of attachments
     */
    public ExtractionResult extract(Part part) {
        List<EmailAttachmentDto> attachments = new ArrayList<>();
        String body = extractPart(part, attachments);
        return new ExtractionResult(body, attachments);
    }

    // -------------------------------------------------------------------------
    // Private recursive MIME walker
    // -------------------------------------------------------------------------

    private String extractPart(Part part, List<EmailAttachmentDto> attachments) {
        try {
            // --- Attachment detection ---
            // A part is an attachment if it has a Content-Disposition of "attachment"
            // OR if it has a filename declared (some clients omit the disposition header)
            String disposition = part.getDisposition();
            String filename    = part.getFileName();

            boolean isAttachment = Part.ATTACHMENT.equalsIgnoreCase(disposition)
                    || (filename != null && !filename.isBlank());

            if (isAttachment) {
                extractAttachment(part, filename, attachments);
                return "";  // Attachment parts have no body text to contribute
            }

            // --- Plain text — preferred body format ---
            if (part.isMimeType("text/plain")) {
                return (String) part.getContent();
            }

            // --- HTML — strip tags to produce readable text ---
            if (part.isMimeType("text/html")) {
                return stripHtml((String) part.getContent());
            }

            // --- Multipart: recurse into child parts ---
            if (part.isMimeType("multipart/*")) {
                return extractMultipart(part, attachments);
            }

        } catch (MessagingException | IOException e) {
            log.warn("Failed to extract part (contentType='{}'): {}",
                    safeGetContentType(part), e.getMessage());
        }

        return "";
    }

    private String extractMultipart(Part part, List<EmailAttachmentDto> attachments)
            throws MessagingException, IOException {

        Multipart multipart = (Multipart) part.getContent();
        StringBuilder bodyAccumulator = new StringBuilder();

        for (int i = 0; i < multipart.getCount(); i++) {
            BodyPart childPart = multipart.getBodyPart(i);
            String extracted = extractPart(childPart, attachments);

            if (!extracted.isBlank()) {
                bodyAccumulator.append(extracted);

                // For multipart/alternative (e.g. plain + html versions of the same content),
                // stop after the first non-empty part — prefer plain text over HTML
                // since plain text appears first in well-formed alternative messages.
                if (part.isMimeType("multipart/alternative")) {
                    break;
                }
            }
        }

        return bodyAccumulator.toString().trim();
    }

    private void extractAttachment(Part part, String filename,
                                   List<EmailAttachmentDto> attachments) {
        try {
            String contentType = sanitizeContentType(part.getContentType());

            // Guard: skip attachments with no filename — they can't be saved meaningfully
            if (filename == null || filename.isBlank()) {
                log.debug("Skipping unnamed attachment of type '{}'", contentType);
                return;
            }

            byte[] content;
            try (InputStream is = part.getInputStream()) {
                content = IOUtils.toByteArray(is);
            }

            if (content.length == 0) {
                log.warn("Skipping empty attachment '{}'", filename);
                return;
            }

            attachments.add(EmailAttachmentDto.builder()
                    .filename(filename)
                    .contentType(contentType)
                    .content(content)
                    .build());

            log.debug("Extracted attachment '{}' ({} bytes, type: {})",
                    filename, content.length, contentType);

        } catch (MessagingException | IOException e) {
            log.error("Failed to extract attachment '{}': {}", filename, e.getMessage(), e);
            // Do not rethrow — a failed attachment should not abort body extraction
            // or prevent the ticket from being created
        }
    }

    /**
     * Strips MIME type parameters (e.g. "application/pdf; name=file.pdf" → "application/pdf").
     * The upload service expects a clean MIME type string.
     */
    private String sanitizeContentType(String rawContentType) {
        if (rawContentType == null) return "application/octet-stream";
        int semicolon = rawContentType.indexOf(';');
        return (semicolon > 0 ? rawContentType.substring(0, semicolon) : rawContentType).trim();
    }

    private String stripHtml(String html) {
        if (html == null || html.isBlank()) return "";
        return html
                .replaceAll("<[^>]+>", "")
                .replace("&amp;", "&")
                .replace("&lt;", "<")
                .replace("&gt;", ">")
                .replace("&nbsp;", " ")
                .replaceAll("\\s{2,}", " ")
                .trim();
    }

    private String safeGetContentType(Part part) {
        try {
            return part.getContentType();
        } catch (MessagingException e) {
            return "unknown";
        }
    }
}