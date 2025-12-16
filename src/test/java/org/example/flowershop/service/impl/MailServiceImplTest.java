package org.example.flowershop.service.impl;

import jakarta.mail.internet.MimeMessage;
import org.example.flowershop.exception.MailSendingException;
import org.example.flowershop.model.entity.User;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mail.MailSendException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MailServiceImplTest {

    @Mock
    private JavaMailSender mailSender;

    @Mock
    private TemplateEngine templateEngine;

    @InjectMocks
    private MailServiceImpl mailServiceImpl;

    @Test
    void sendMail_shouldSendMailMessage() {
        String to = "test@example.com";
        String subject = "Test";
        String text = "Test";

        mailServiceImpl.sendMail(to, subject, text);

        ArgumentCaptor<SimpleMailMessage> captor =
                ArgumentCaptor.forClass(SimpleMailMessage.class);

        verify(mailSender).send(captor.capture());

        SimpleMailMessage mailMessage = captor.getValue();
        assertEquals(to, mailMessage.getTo()[0]);
        assertEquals(subject, mailMessage.getSubject());
        assertEquals(text, mailMessage.getText());
    }

    @Test
    void sendWelcomeMail_shouldSendWelcomeMessage() {
        User user = new User();
        user.setUsername("user");
        user.setEmail("user@example.com");

        MimeMessage mimeMessage = mock(MimeMessage.class);
        when(mailSender.createMimeMessage()).thenReturn(mimeMessage);
        when(templateEngine.process(eq("mail/welcome.html"), any(Context.class)))
                .thenReturn("<html>Welcome, user</html>");

        mailServiceImpl.sendWelcomeMail(user);

        verify(mailSender).send(mimeMessage);
    }

    @Test
    void sendWelcomeMail_shouldThrowMailSendingException_whenMailSenderFails() {
        User user = new User();
        user.setUsername("user");
        user.setEmail("user@example.com");

        when(templateEngine.process(anyString(), any(Context.class)))
                .thenReturn("Welcome");

        MimeMessage mimeMessage = mock(MimeMessage.class);
        when(mailSender.createMimeMessage()).thenReturn(mimeMessage);

        doThrow(new MailSendException("Mail server error"))
                .when(mailSender)
                .send(any(MimeMessage.class));

        MailSendingException ex = assertThrows(
                MailSendingException.class,
                () -> mailServiceImpl.sendWelcomeMail(user)
        );

        assertEquals("Failed to send welcome email", ex.getMessage());
        assertTrue(ex.getCause() instanceof MailSendException);
    }
}