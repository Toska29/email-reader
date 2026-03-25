package com.toskafx.email_reader.repository;

import com.toskafx.email_reader.model.ProcessedEmail;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProcessedEmailRepository extends JpaRepository<ProcessedEmail, Long> {

    /**
     * The core deduplication check — returns true if this Message-ID
     * has already been processed and a ticket created for it.
     */
    boolean existsByMessageId(String messageId);
}