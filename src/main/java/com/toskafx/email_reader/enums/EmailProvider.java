package com.toskafx.email_reader.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum EmailProvider {

    GMAIL("imap.gmail.com", 993),
    OUTLOOK("outlook.office365.com", 993);

    private final String imapHost;
    private final int imapPort;
}