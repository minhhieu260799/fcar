package com.fcar.service;

import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.mail.MailException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

/** Phân công: hạ tầng gửi mail — dùng chung (Nguyên: OTP; Minh/Bảo: thông báo đơn & liên hệ). */
@Service
@RequiredArgsConstructor
@Slf4j
public class MailNotificationService {

    private final JavaMailSender mailSender;

    /**
     * Gửi email text thuần; lỗi SMTP không làm hỏng luồng nghiệp vụ (chỉ ghi log).
     */
    public void sendPlain(String to, String subject, String text) {
        if (to == null || to.isBlank()) {
            return;
        }
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setTo(to);
            message.setSubject(subject);
            message.setText(text);
            mailSender.send(message);
        } catch (MailException ex) {
            log.warn("Không gửi được email tới {}: {}", to, ex.getMessage());
        }
    }

    /**
     * Gửi email HTML (UTF-8). Lỗi SMTP không làm hỏng luồng nghiệp vụ.
     */
    public void sendHtml(String to, String subject, String htmlBody) {
        if (to == null || to.isBlank()) {
            return;
        }
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
            helper.setTo(to);
            helper.setSubject(subject);
            helper.setText(htmlBody, true);
            mailSender.send(message);
        } catch (Exception ex) {
            log.warn("Không gửi được email HTML tới {}: {}", to, ex.getMessage());
        }
    }
}
