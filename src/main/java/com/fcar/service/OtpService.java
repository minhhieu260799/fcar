package com.fcar.service;

import com.fcar.domain.OtpToken;
import com.fcar.domain.User;
import com.fcar.domain.UserPhone;
import com.fcar.domain.enums.OtpType;
import com.fcar.repository.OtpTokenRepository;
import com.fcar.repository.UserPhoneRepository;
import com.fcar.repository.UserRepository;
import java.time.LocalDateTime;
import java.util.Locale;
import java.util.Random;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

/** Phân công: Nguyên — gửi/xác thực OTP (quên MK, đăng nhập SĐT, xác minh email/SĐT). */
@Slf4j
@Service
@RequiredArgsConstructor
public class OtpService {

    private static final int OTP_TTL_MINUTES = 15;
    private static final int MAX_SEND_COUNT = 100;
    private static final int MAX_FAIL_COUNT = 3;

    private final OtpTokenRepository otpTokenRepository;
    private final UserRepository userRepository;
    private final UserPhoneRepository userPhoneRepository;
    private final JavaMailSender mailSender;

    @Value("${fcar.otp.log-phone-delivery:true}")
    private boolean logPhoneOtpDelivery;

    public void sendLoginPhoneOtp(String phone) {
        User user = userRepository.findByPhone(phone)
                .orElseGet(() -> {
                    UserPhone up = userPhoneRepository.findFirstByPhone(phone)
                            .orElseThrow(() -> new IllegalArgumentException("Số điện thoại chưa được đăng ký"));
                    return up.getUser();
                });
        OtpToken token = createOrUpdateToken(user, OtpType.LOGIN_PHONE, phone);
        logPhoneOtpIfEnabled(OtpType.LOGIN_PHONE, phone, token.getCode());
    }

    /**
     * Khi {@code fcar.otp.log-phone-delivery=true}, trả mã OTP đăng nhập điện thoại còn hiệu lực để hiển thị trên form (demo).
     */
    public String getActiveLoginPhoneOtpDemoCode(String normalizedPhone) {
        if (!logPhoneOtpDelivery || normalizedPhone == null || normalizedPhone.isBlank()) {
            return null;
        }
        LocalDateTime now = LocalDateTime.now();
        return otpTokenRepository
                .findTopByTypeAndTargetOrderByCreatedAtDesc(OtpType.LOGIN_PHONE, normalizedPhone)
                .filter(t -> !t.isUsed() && t.getExpiresAt() != null && t.getExpiresAt().isAfter(now))
                .map(OtpToken::getCode)
                .orElse(null);
    }

    public void sendForgotPasswordOtp(String email) {
        String emailNorm = email.trim().toLowerCase(Locale.ROOT);
        User user = userRepository.findByEmailIgnoreCase(emailNorm)
                .orElseThrow(() -> new IllegalArgumentException("email.not.registered"));
        OtpToken token = createOrUpdateToken(user, OtpType.FORGOT_PASSWORD, emailNorm);
        SimpleMailMessage message = new SimpleMailMessage();
        message.setTo(user.getEmail());
        message.setSubject("Mã OTP đặt lại mật khẩu FCAR");
        message.setText("Mã OTP của bạn là: " + token.getCode() + " (hết hạn sau 15 phút).");
        mailSender.send(message);
    }

    public void sendVerifyEmailOtp(User user, String email) {
        if (userRepository.existsByEmail(email)) {
            throw new IllegalArgumentException("Email này đã được đăng ký bởi tài khoản khác");
        }
        OtpToken token = createOrUpdateToken(user, OtpType.VERIFY_EMAIL, email);
        SimpleMailMessage message = new SimpleMailMessage();
        message.setTo(email);
        message.setSubject("Mã OTP xác nhận email FCAR");
        message.setText("Mã OTP của bạn là: " + token.getCode() + " (hết hạn sau 15 phút).");
        mailSender.send(message);
    }

    public void sendVerifyPhoneOtp(User user, String phone) {
        OtpToken token = createOrUpdateToken(user, OtpType.VERIFY_PHONE, phone);
        logPhoneOtpIfEnabled(OtpType.VERIFY_PHONE, phone, token.getCode());
    }

    /**
     * Trả về mã OTP trong response API khi {@code fcar.otp.log-phone-delivery=true} (mặc định bật cho môi trường test).
     */
    public String sendVerifyPhoneOtpWithOptionalDemoCode(User user, String phone) {
        OtpToken token = createOrUpdateToken(user, OtpType.VERIFY_PHONE, phone);
        logPhoneOtpIfEnabled(OtpType.VERIFY_PHONE, phone, token.getCode());
        return logPhoneOtpDelivery ? token.getCode() : null;
    }

    public void verifyOtp(String target, OtpType type, String code) {
        String trimmedCode = code != null ? code.trim() : "";
        OtpToken token = otpTokenRepository.findTopByTypeAndTargetOrderByCreatedAtDesc(type, target)
                .orElseThrow(() -> new IllegalArgumentException("OTP không tồn tại hoặc đã hết hạn"));

        LocalDateTime now = LocalDateTime.now();
        if (token.getLockedUntil() != null && token.getLockedUntil().isAfter(now)) {
            throw new IllegalStateException("OTP đã bị khóa, vui lòng thử lại sau 15 phút");
        }
        if (token.isUsed() || token.getExpiresAt().isBefore(now)) {
            throw new IllegalArgumentException("OTP đã hết hạn, vui lòng yêu cầu mã mới");
        }

        if (!token.getCode().equals(trimmedCode)) {
            int fails = token.getFailCount() + 1;
            token.setFailCount(fails);
            if (fails >= MAX_FAIL_COUNT) {
                token.setLockedUntil(now.plusMinutes(OTP_TTL_MINUTES));
            }
            otpTokenRepository.save(token);
            throw new IllegalArgumentException("OTP không chính xác");
        }

        token.setUsed(true);
        otpTokenRepository.save(token);
    }

    private OtpToken createOrUpdateToken(User user, OtpType type, String target) {
        LocalDateTime now = LocalDateTime.now();
        OtpToken token = otpTokenRepository.findTopByTypeAndTargetOrderByCreatedAtDesc(type, target)
                .orElse(null);

        if (token != null) {
            if (token.getLockedUntil() != null && token.getLockedUntil().isAfter(now)) {
                throw new IllegalStateException("OTP đã bị khóa, vui lòng thử lại sau 15 phút");
            }
            if (token.getExpiresAt().isAfter(now) && token.getSendCount() >= MAX_SEND_COUNT) {
                throw new IllegalStateException("Bạn đã yêu cầu OTP quá số lần cho phép");
            }
        } else {
            token = new OtpToken();
            token.setUser(user);
            token.setType(type);
            token.setTarget(target);
            token.setSendCount(0);
            token.setFailCount(0);
        }

        String code = generateCode();
        token.setCode(code);
        token.setExpiresAt(now.plusMinutes(OTP_TTL_MINUTES));
        token.setUsed(false);
        token.setSendCount(token.getSendCount() + 1);
        token.setLockedUntil(null);
        token.setFailCount(0);

        return otpTokenRepository.save(token);
    }

    private String generateCode() {
        Random random = new Random();
        int number = 100000 + random.nextInt(900000);
        return String.valueOf(number);
    }

    private void logPhoneOtpIfEnabled(OtpType type, String target, String code) {
        if (!logPhoneOtpDelivery) {
            return;
        }
        log.warn("[fcar.otp.log-phone-delivery] type={} target={} code={} (demo — no SMS sent)",
                type, target, code);
    }
}

