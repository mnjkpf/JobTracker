package com.jobtracker.backendJobTracker.email;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.MailException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class EmailService {
 
    private static final Logger LOGGER = LoggerFactory.getLogger(EmailService.class);
 
    
    private final JavaMailSender mailSender;
 
    @Value("${spring.mail.properties.mail.from:noreply@jobtracker.local}")
    private String fromAddress;
 
    /**
     * Async було б краще — щоб HTTP запит не чекав на SMTP. Але вимагає
     * @EnableAsync і обробку failures. Залишаємо sync на MVP — Mailpit
     * локально відповідає миттєво, real SMTP перевіримо у W11 (deploy).
     */
    public void sendEmail(String to, String subject, String text) {
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(fromAddress);
            message.setTo(to);
            message.setSubject(subject);
            message.setText(text);
            mailSender.send(message);
            LOGGER.info("Email sent to {} ({})", to, subject);
        } catch (MailException e) {
            
            LOGGER.error("Failed to send email to {}: {}", to, e.getMessage());
            throw new EmailSendException("Failed to send email", e);
        }
    }
}

