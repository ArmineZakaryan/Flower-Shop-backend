package org.example.flowershop.service;

import org.example.flowershop.model.entity.User;

public interface MailService {
    void sendMail(String to, String subject, String text);

    void sendWelcomeMail(User user);
}