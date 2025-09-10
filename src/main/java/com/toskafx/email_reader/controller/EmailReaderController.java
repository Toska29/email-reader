package com.toskafx.email_reader.controller;

import com.toskafx.email_reader.service.EmailService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/email")
public class EmailReaderController {

    @Autowired
    private EmailService emailService;

    @GetMapping("/retrieve")
    public void retrieveEmails() {
        emailService.retrieveEmails();
    }
}
