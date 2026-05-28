package com.example.magazyn.service;

import com.example.magazyn.entity.User;
import com.example.magazyn.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class EmailService {

    private static final Logger log = LoggerFactory.getLogger(EmailService.class);

    private final JavaMailSender mailSender;
    private final UserRepository userRepository;
    private final String from;

    public EmailService(JavaMailSender mailSender,
                        UserRepository userRepository,
                        @Value("${spring.mail.from}") String from) {
        this.mailSender = mailSender;
        this.userRepository = userRepository;
        this.from = from;
    }

    public void sendToAdmins(String subject, String text) {
        List<User> admins = userRepository.findByRole("ROLE_ADMIN");
        boolean anySent = false;
        for (User admin : admins) {
            if (admin.getEmail() == null || admin.getEmail().isBlank()) continue;
            try {
                SimpleMailMessage msg = new SimpleMailMessage();
                msg.setFrom(from);
                msg.setTo(admin.getEmail());
                msg.setSubject(subject);
                msg.setText(text);
                mailSender.send(msg);
                anySent = true;
                log.info("Notification email sent to {} ({})", admin.getUsername(), admin.getEmail());
            } catch (Exception e) {
                log.warn("Failed to send email to {} ({}): {}", admin.getUsername(), admin.getEmail(), e.getMessage());
            }
        }
        if (!anySent) {
            log.info("No admin with configured email — notification skipped");
        }
    }
}
