package com.fcar.controller;

/** Phân công: Nguyên — API OTP gắn/xác minh SĐT (modal hồ sơ). */
import com.fcar.domain.User;
import com.fcar.domain.enums.OtpType;
import com.fcar.repository.UserPhoneRepository;
import com.fcar.repository.UserRepository;
import com.fcar.security.FcarUserDetails;
import com.fcar.service.OtpService;
import com.fcar.util.VietnamPhoneRules;
import com.fasterxml.jackson.annotation.JsonInclude;
import java.util.LinkedHashMap;
import java.util.Map;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/account/api/phone")
@RequiredArgsConstructor
public class AccountPhoneModalApiController {

    private final UserRepository userRepository;
    private final UserPhoneRepository userPhoneRepository;
    private final OtpService otpService;

    @PostMapping("/send-otp")
    public ResponseEntity<PhoneModalResponse> sendOtp(@RequestBody PhoneBody body,
                                                      @AuthenticationPrincipal FcarUserDetails principal) {
        if (principal == null) {
            return ResponseEntity.status(401).body(PhoneModalResponse.error(Map.of("_", "Chưa đăng nhập")));
        }
        User user = userRepository.findById(principal.getUser().getId()).orElseThrow();
        if (user.getPhone() != null && !user.getPhone().isBlank()) {
            return ResponseEntity.badRequest()
                    .body(PhoneModalResponse.error(Map.of("phone", "Bạn đã có số điện thoại chính")));
        }
        if (!userPhoneRepository.findByUser(user).isEmpty()) {
            return ResponseEntity.badRequest()
                    .body(PhoneModalResponse.error(Map.of("phone", "Tài khoản đã có số phụ, vui lòng dùng trang hồ sơ")));
        }

        String raw = body != null ? body.getPhone() : null;
        Map<String, String> errors = VietnamPhoneRules.validatePhoneRaw(raw);
        if (!errors.isEmpty()) {
            return ResponseEntity.badRequest().body(PhoneModalResponse.error(errors));
        }
        String normalized = VietnamPhoneRules.normalizeDigits(raw);

        try {
            assertPhoneNotUsedByAnotherUser(normalized, user);
        } catch (IllegalArgumentException ex) {
            return ResponseEntity.badRequest()
                    .body(PhoneModalResponse.error(Map.of("phone", ex.getMessage())));
        }

        try {
            String demo = otpService.sendVerifyPhoneOtpWithOptionalDemoCode(user, normalized);
            return ResponseEntity.ok(PhoneModalResponse.sent(normalized, demo));
        } catch (IllegalArgumentException | IllegalStateException ex) {
            return ResponseEntity.badRequest()
                    .body(PhoneModalResponse.error(Map.of("phone", ex.getMessage())));
        }
    }

    @PostMapping("/confirm")
    @Transactional
    public ResponseEntity<PhoneModalResponse> confirm(@RequestBody ConfirmBody body,
                                                      @AuthenticationPrincipal FcarUserDetails principal) {
        if (principal == null) {
            return ResponseEntity.status(401).body(PhoneModalResponse.error(Map.of("_", "Chưa đăng nhập")));
        }
        User user = userRepository.findById(principal.getUser().getId()).orElseThrow();
        if (user.getPhone() != null && !user.getPhone().isBlank()) {
            return ResponseEntity.badRequest()
                    .body(PhoneModalResponse.error(Map.of("phone", "Bạn đã có số điện thoại chính")));
        }

        if (body == null) {
            return ResponseEntity.badRequest()
                    .body(PhoneModalResponse.error(Map.of("phone", "Thiếu dữ liệu")));
        }
        String raw = body.getPhone();
        Map<String, String> errors = new LinkedHashMap<>(VietnamPhoneRules.validatePhoneRaw(raw));
        VietnamPhoneRules.addOtpCodeErrors(body.getCode(), errors);
        if (!errors.isEmpty()) {
            return ResponseEntity.badRequest().body(PhoneModalResponse.error(errors));
        }
        String normalized = VietnamPhoneRules.normalizeDigits(raw);
        String code = body.getCode().trim();

        try {
            assertPhoneNotUsedByAnotherUser(normalized, user);
        } catch (IllegalArgumentException ex) {
            return ResponseEntity.badRequest()
                    .body(PhoneModalResponse.error(Map.of("phone", ex.getMessage())));
        }

        try {
            otpService.verifyOtp(normalized, OtpType.VERIFY_PHONE, code);
        } catch (IllegalArgumentException | IllegalStateException ex) {
            return ResponseEntity.badRequest()
                    .body(PhoneModalResponse.error(Map.of("code", ex.getMessage())));
        }

        user.setPhone(normalized);
        userRepository.save(user);

        User fresh = userRepository.findById(user.getId()).orElseThrow();
        FcarUserDetails details = new FcarUserDetails(fresh);
        UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken(
                details, null, details.getAuthorities());
        SecurityContextHolder.getContext().setAuthentication(auth);

        return ResponseEntity.ok(PhoneModalResponse.confirmed());
    }

    private void assertPhoneNotUsedByAnotherUser(String normalized, User current) {
        userRepository.findByPhone(normalized).ifPresent(owner -> {
            if (!owner.getId().equals(current.getId())) {
                throw new IllegalArgumentException("Số điện thoại đã được sử dụng bởi tài khoản khác");
            }
        });
        userPhoneRepository.findFirstByPhone(normalized).ifPresent(up -> {
            if (!up.getUser().getId().equals(current.getId())) {
                throw new IllegalArgumentException("Số điện thoại đã được sử dụng bởi tài khoản khác");
            }
        });
    }

    @Data
    public static class PhoneBody {
        private String phone;
    }

    @Data
    public static class ConfirmBody {
        private String phone;
        private String code;
    }

    @JsonInclude(JsonInclude.Include.NON_NULL)
    public record PhoneModalResponse(boolean ok,
                                     Map<String, String> fieldErrors,
                                     String demoOtp,
                                     String normalizedPhone) {

        static PhoneModalResponse error(Map<String, String> fieldErrors) {
            return new PhoneModalResponse(false, fieldErrors, null, null);
        }

        static PhoneModalResponse sent(String normalizedPhone, String demoOtp) {
            return new PhoneModalResponse(true, Map.of(), demoOtp, normalizedPhone);
        }

        static PhoneModalResponse confirmed() {
            return new PhoneModalResponse(true, Map.of(), null, null);
        }
    }
}
