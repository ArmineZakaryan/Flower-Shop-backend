package org.example.flowershop.service;

import org.example.flowershop.model.entity.User;
import org.springframework.scheduling.annotation.Async;

public interface MailService {
    void sendMail(String to, String subject, String text);

    @Async
    void sendWelcomeMail(User user);
}
