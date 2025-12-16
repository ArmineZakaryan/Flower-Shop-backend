package org.example.flowershop.service.impl;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.flowershop.exception.MailSendingException;
import org.example.flowershop.model.entity.User;
import org.example.flowershop.service.MailService;
import org.springframework.mail.MailException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

@Service
@RequiredArgsConstructor
@Slf4j
public class MailServiceImpl implements MailService {

    private final JavaMailSender mailSender;
    private final TemplateEngine templateEngine;

    @Override
    public void sendMail(String to, String subject, String text) {
        log.info("Sending mail to {} with subject {}", to, subject);
        SimpleMailMessage message = new SimpleMailMessage();
        message.setTo(to);
        message.setSubject(subject);
        message.setText(text);

        try {
            mailSender.send(message);
            log.info("Mail successfully sent to {}", to);
        } catch (MailException e) {
            log.error("Failed to send email to {} with subject {}", to, subject, e);
            throw e;
        }
    }

    @Override
    public void sendWelcomeMail(User user) {
        log.info("Sending welcome mail to {}", user.getEmail());

        Context ctx = new Context();
        ctx.setVariable("user", user.getUsername());

        String htmlContent = templateEngine.process("mail/welcome.html", ctx);
        MimeMessage mimeMessage = mailSender.createMimeMessage();

        try {
            MimeMessageHelper message =
                    new MimeMessageHelper(mimeMessage, true, "UTF-8");

            message.setSubject("Welcome flower shop");
            message.setTo(user.getEmail());
            message.setText(htmlContent, true);

            mailSender.send(mimeMessage);
            log.info("Mail successfully sent to {}", user.getEmail());

        } catch (MessagingException | MailException e) {
            log.info("Failed to send welcome mail to {}", user.getEmail());
            throw new MailSendingException("Failed to send welcome email", e);
        }
    }
}