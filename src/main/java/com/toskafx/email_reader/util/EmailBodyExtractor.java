package com.toskafx.email_reader.util;

import jakarta.mail.BodyPart;
import jakarta.mail.MessagingException;
import jakarta.mail.Multipart;
import jakarta.mail.Part;
import lombok.experimental.UtilityClass;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;

/**
 * Utility for extracting readable text content from Jakarta Mail Message parts.
 *
 * Email bodies come in three structural forms:
 *  - Simple text/plain
 *  - Simple text/html
 *  - multipart/alternative or multipart/mixed (nested parts, attachments, etc.)
 *
 * This extractor walks the MIME tree and returns the best available plain text.
 * HTML is stripped to raw text. Attachments are ignored.
 */
@Slf4j
@UtilityClass
public class EmailBodyExtractor {

    /**
     * Recursively extracts a plain-text representation of the email body.
     *
     * @param part the top-level message or a nested body part
     * @return extracted text, or empty string if nothing readable is found
     */
    public String extract(Part part) {
        try {
            // Plain text — ideal, return as-is
            if (part.isMimeType("text/plain")) {
                return (String) part.getContent();
            }

            // HTML — strip tags to get readable text
            if (part.isMimeType("text/html")) {
                String html = (String) part.getContent();
                return stripHtml(html);
            }

            // Multipart: walk each child part recursively
            if (part.isMimeType("multipart/*")) {
                Multipart multipart = (Multipart) part.getContent();
                StringBuilder sb = new StringBuilder();

                for (int i = 0; i < multipart.getCount(); i++) {
                    BodyPart bodyPart = multipart.getBodyPart(i);

                    // Skip attachments — only process inline content
                    String disposition = bodyPart.getDisposition();
                    if (Part.ATTACHMENT.equalsIgnoreCase(disposition)) {
                        continue;
                    }

                    String extracted = extract(bodyPart);
                    if (extracted != null && !extracted.isBlank()) {
                        sb.append(extracted);
                        // For multipart/alternative, first successful result is enough
                        if (part.isMimeType("multipart/alternative")) {
                            break;
                        }
                    }
                }

                return sb.toString().trim();
            }

        } catch (MessagingException | IOException e) {
            log.warn("Failed to extract email body from part of type '{}': {}",
                    safeGetContentType(part), e.getMessage());
        }

        return "";
    }

    /**
     * Minimal HTML stripper — removes tags and decodes common HTML entities.
     * For production use with complex HTML emails, consider Jsoup.
     */
    private String stripHtml(String html) {
        if (html == null || html.isBlank()) return "";
        return html
                .replaceAll("<[^>]+>", "")         // remove tags
                .replace("&amp;", "&")
                .replace("&lt;", "<")
                .replace("&gt;", ">")
                .replace("&nbsp;", " ")
                .replaceAll("\\s{2,}", " ")          // collapse whitespace
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