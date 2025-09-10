package com.toskafx.email_reader.service;

import jakarta.mail.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class EmailServiceImpl implements EmailService{

    private static final String IMAP_HOST = "imap.gmail.com";

    @Value("${account.username}")
    private String username;

    @Value("${account.password}")
    private String password;

    @Autowired
    private Session session;

    @Override
    public void retrieveEmails() {
        try(Store store = session.getStore("imaps")) {
            store.connect(IMAP_HOST, username, password);

            Folder inbox = store.getFolder("inbox");
            inbox.open(Folder.READ_ONLY);
            Message[] messages = inbox.getMessages();
            for (Message message : messages) {
                System.out.println(message.getSubject());
            }
        } catch (MessagingException e) {
            throw new RuntimeException(e);
        }
    }
}
