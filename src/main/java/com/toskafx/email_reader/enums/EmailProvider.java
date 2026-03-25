package com.toskafx.email_reader.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum EmailProvider {

    /**
     * Gmail uses App Passwords over Basic Auth (XOAUTH2 is also supported but
     * App Password is simpler for service/daemon accounts without a UI login flow).
     * Requires: Google Account → Security → 2FA enabled → App Passwords → generate one.
     */
    GMAIL(
            "imap.gmail.com",
            993,
            AuthMechanism.APP_PASSWORD,
            null
    ),

    /**
     * Microsoft 365 / Outlook has fully blocked Basic Auth for managed business accounts.
     * OAuth2 with the XOAUTH2 SASL mechanism is mandatory.
     * Token endpoint uses the tenant ID — use "common" for multi-tenant or personal accounts,
     * or your specific Azure AD tenant UUID for single-tenant business apps.
     */
    OUTLOOK(
            "outlook.office365.com",
            993,
            AuthMechanism.OAUTH2,
            "https://login.microsoftonline.com/{tenantId}/oauth2/v2.0/token"
    );

    private final String imapHost;
    private final int imapPort;
    private final AuthMechanism authMechanism;

    /** OAuth2 token endpoint — null for providers that use App Password auth. */
    private final String oauthTokenEndpoint;

    public enum AuthMechanism {
        APP_PASSWORD,
        OAUTH2
    }
}