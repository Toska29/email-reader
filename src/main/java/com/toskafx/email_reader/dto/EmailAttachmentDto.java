package com.toskafx.email_reader.dto;

import lombok.Builder;
import lombok.Getter;

/**
 * Immutable value object carrying a single email attachment's raw data.
 *
 * This is the intermediate form between a Jakarta Mail BodyPart (IMAP layer)
 * and a MultipartFile (Spring upload layer). The conversion between these
 * two forms happens in EmailMultipartFile.
 */
@Getter
@Builder
public class EmailAttachmentDto {

    /** Original filename as declared in the Content-Disposition header */
    private final String filename;

    /** MIME type — e.g. "application/pdf", "image/jpeg" */
    private final String contentType;

    /** Raw file bytes — held in memory, consistent with Spring's MultipartFile contract */
    private final byte[] content;

    /** File size in bytes */
    public long getSize() {
        return content != null ? content.length : 0;
    }
}