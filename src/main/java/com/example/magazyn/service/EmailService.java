package com.example.magazyn.service;

import com.example.magazyn.entity.User;
import com.example.magazyn.repository.UserRepository;
import jakarta.mail.internet.MimeMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
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

    /**
     * Send an HTML email to a single recipient.
     */
    public void sendHtml(String to, String subject, String htmlContent) {
        try {
            MimeMessage mime = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(mime, true, "UTF-8");
            helper.setFrom(from);
            helper.setTo(to);
            helper.setSubject(subject);
            helper.setText(htmlContent, true);
            mailSender.send(mime);
            log.info("HTML email sent to {}", to);
        } catch (Exception e) {
            log.warn("Failed to send HTML email to {}: {}", to, e.getMessage());
        }
    }

    /**
     * Send an HTML email to all administrators.
     */
    public void sendHtmlToAdmins(String subject, String htmlContent) {
        List<User> admins = userRepository.findByRole("ROLE_ADMIN");
        boolean anySent = false;
        for (User admin : admins) {
            if (admin.getEmail() == null || admin.getEmail().isBlank()) continue;
            sendHtml(admin.getEmail(), subject, htmlContent);
            anySent = true;
        }
        if (!anySent) {
            log.info("No admin with configured email — notification skipped");
        }
    }

    /**
     * Build a styled HTML email body with a header, content, and footer.
     */
    public static String buildTemplate(String title, String bodyHtml) {
        return "<!DOCTYPE html>\n" +
            "<html>\n" +
            "<head><meta charset=\"UTF-8\"></head>\n" +
            "<body style=\"margin:0;padding:0;background:#f5f6fa;font-family:'Inter',system-ui,sans-serif;\">\n" +
            "  <table width=\"100%\" cellpadding=\"0\" cellspacing=\"0\">\n" +
            "    <tr>\n" +
            "      <td align=\"center\" style=\"padding:40px 16px;\">\n" +
            "        <table width=\"560\" cellpadding=\"0\" cellspacing=\"0\" style=\"background:#ffffff;border-radius:12px;overflow:hidden;box-shadow:0 2px 12px rgba(0,0,0,0.06);\">\n" +
            "          <tr>\n" +
            "            <td style=\"padding:32px 32px 0;background:linear-gradient(135deg,#6c8cff,#8b5cf6);\">\n" +
            "              <h1 style=\"margin:0 0 4px;font-size:20px;font-weight:600;color:#ffffff;\">" + title + "</h1>\n" +
            "              <p style=\"margin:0 0 24px;font-size:13px;color:rgba(255,255,255,0.8);\">System Magazyn</p>\n" +
            "            </td>\n" +
            "          </tr>\n" +
            "          <tr>\n" +
            "            <td style=\"padding:32px;font-size:14px;line-height:1.6;color:#1a1d27;\">\n" +
            "              " + bodyHtml + "\n" +
            "            </td>\n" +
            "          </tr>\n" +
            "          <tr>\n" +
            "            <td style=\"padding:16px 32px;border-top:1px solid #e2e4ea;font-size:12px;color:#8b8fa3;\">\n" +
            "              <p style=\"margin:0;\">" + "Wiadomo\u015B\u0107 wygenerowana automatycznie przez system Magazyn.</p>\n" +
            "            </td>\n" +
            "          </tr>\n" +
            "        </table>\n" +
            "      </td>\n" +
            "    </tr>\n" +
            "  </table>\n" +
            "</body>\n" +
            "</html>\n";
    }

    /**
     * Convenience: build a simple text paragraph body.
     */
    public static String bodyText(String text) {
        return "<p style=\"margin:0 0 16px;\">" + text + "</p>";
    }

    /**
     * Convenience: build a warning alert box.
     */
    public static String alertBox(String text) {
        return """
            <table style="background:#fef2f2;border:1px solid #fecaca;border-radius:8px;padding:12px 16px;margin:0 0 16px;">
              <tr>
                <td style="font-size:14px;color:#dc2626;">\u26A0\uFE0F """ + text + "</td></tr></table>";
    }
}
