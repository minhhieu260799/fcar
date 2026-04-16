package com.fcar.controller;

/** Phân công: Nguyên — API cập nhật hồ sơ, avatar, số điện thoại phụ. */
import com.fcar.domain.User;
import com.fcar.domain.UserPhone;
import com.fcar.domain.enums.OtpType;
import com.fcar.repository.UserPhoneRepository;
import com.fcar.repository.UserRepository;
import com.fcar.security.FcarUserDetails;
import com.fcar.service.OtpService;
import com.fcar.util.VietnamPhoneRules;
import com.fasterxml.jackson.annotation.JsonInclude;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/account/api/profile")
@RequiredArgsConstructor
public class AccountProfileApiController {

    private static final long MAX_AVATAR_BYTES = 2 * 1024 * 1024;

    private final UserRepository userRepository;
    private final UserPhoneRepository userPhoneRepository;
    private final OtpService otpService;

    @PostMapping("/update")
    @Transactional
    public ResponseEntity<ProfileApiResponse> updateProfile(@Valid @RequestBody ProfileUpdateRequest body,
                                                            BindingResult bindingResult,
                                                            @AuthenticationPrincipal FcarUserDetails principal) {
        if (principal == null) {
            return ResponseEntity.status(401).body(ProfileApiResponse.fail(Map.of("_", "Chưa đăng nhập")));
        }
        if (bindingResult.hasErrors()) {
            Map<String, String> fe = new LinkedHashMap<>();
            for (FieldError e : bindingResult.getFieldErrors()) {
                fe.put(e.getField(), e.getDefaultMessage() != null ? e.getDefaultMessage() : "Không hợp lệ");
            }
            return ResponseEntity.badRequest().body(ProfileApiResponse.fail(fe));
        }
        User user = userRepository.findById(principal.getUser().getId()).orElseThrow();
        user.setFullName(body.getFullName().trim());
        user.setGender(StringUtils.hasText(body.getGender()) ? body.getGender().trim() : null);
        user.setBirthDate(body.getBirthDate());
        user.setAddress(StringUtils.hasText(body.getAddress()) ? body.getAddress().trim() : null);
        userRepository.save(user);

        refreshPrincipal(user.getId());
        return ResponseEntity.ok(ProfileApiResponse.success());
    }

    @PostMapping(value = "/avatar", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Transactional
    public ResponseEntity<AvatarApiResponse> uploadAvatar(@RequestParam("file") MultipartFile file,
                                                          @AuthenticationPrincipal FcarUserDetails principal) {
        if (principal == null) {
            return ResponseEntity.status(401).body(AvatarApiResponse.bad("Chưa đăng nhập"));
        }
        if (file == null || file.isEmpty()) {
            return ResponseEntity.badRequest().body(AvatarApiResponse.bad("Vui lòng chọn ảnh"));
        }
        if (file.getSize() > MAX_AVATAR_BYTES) {
            return ResponseEntity.badRequest().body(AvatarApiResponse.bad("Ảnh tối đa 2MB"));
        }
        String ct = file.getContentType();
        if (ct == null || !ct.startsWith("image/")) {
            return ResponseEntity.badRequest().body(AvatarApiResponse.bad("Chỉ chấp nhận file ảnh"));
        }

        try {
            String url = storeAvatarFile(file);
            User user = userRepository.findById(principal.getUser().getId()).orElseThrow();
            user.setAvatarUrl(url);
            userRepository.save(user);
            refreshPrincipal(user.getId());
            return ResponseEntity.ok(AvatarApiResponse.good(url));
        } catch (IOException ex) {
            return ResponseEntity.badRequest().body(AvatarApiResponse.bad("Không lưu được ảnh, thử lại sau"));
        }
    }

    @PostMapping("/extra-phone/send-otp")
    public ResponseEntity<AccountPhoneModalApiController.PhoneModalResponse> sendExtraPhoneOtp(
            @RequestBody AccountPhoneModalApiController.PhoneBody body,
            @AuthenticationPrincipal FcarUserDetails principal) {
        if (principal == null) {
            return ResponseEntity.status(401)
                    .body(AccountPhoneModalApiController.PhoneModalResponse.error(Map.of("_", "Chưa đăng nhập")));
        }
        User user = userRepository.findById(principal.getUser().getId()).orElseThrow();
        if (user.getPhone() == null || user.getPhone().isBlank()) {
            return ResponseEntity.badRequest()
                    .body(AccountPhoneModalApiController.PhoneModalResponse.error(
                            Map.of("phone", "Vui lòng bổ sung số điện thoại chính trước")));
        }

        String raw = body != null ? body.getPhone() : null;
        Map<String, String> errors = VietnamPhoneRules.validatePhoneRaw(raw);
        if (!errors.isEmpty()) {
            return ResponseEntity.badRequest()
                    .body(AccountPhoneModalApiController.PhoneModalResponse.error(errors));
        }
        String normalized = VietnamPhoneRules.normalizeDigits(raw);

        if (normalized.equals(user.getPhone())) {
            return ResponseEntity.badRequest()
                    .body(AccountPhoneModalApiController.PhoneModalResponse.error(
                            Map.of("phone", "Không thể thêm số trùng với số chính")));
        }
        if (userPhoneRepository.existsByUserAndPhone(user, normalized)) {
            return ResponseEntity.badRequest()
                    .body(AccountPhoneModalApiController.PhoneModalResponse.error(
                            Map.of("phone", "Bạn đã thêm số này rồi")));
        }

        try {
            assertPhoneNotUsedByAnotherUser(normalized, user);
        } catch (IllegalArgumentException ex) {
            return ResponseEntity.badRequest()
                    .body(AccountPhoneModalApiController.PhoneModalResponse.error(Map.of("phone", ex.getMessage())));
        }

        try {
            String demo = otpService.sendVerifyPhoneOtpWithOptionalDemoCode(user, normalized);
            return ResponseEntity.ok(AccountPhoneModalApiController.PhoneModalResponse.sent(normalized, demo));
        } catch (IllegalArgumentException | IllegalStateException ex) {
            return ResponseEntity.badRequest()
                    .body(AccountPhoneModalApiController.PhoneModalResponse.error(Map.of("phone", ex.getMessage())));
        }
    }

    @PostMapping("/extra-phone/confirm")
    @Transactional
    public ResponseEntity<AccountPhoneModalApiController.PhoneModalResponse> confirmExtraPhone(
            @RequestBody AccountPhoneModalApiController.ConfirmBody body,
            @AuthenticationPrincipal FcarUserDetails principal) {
        if (principal == null) {
            return ResponseEntity.status(401)
                    .body(AccountPhoneModalApiController.PhoneModalResponse.error(Map.of("_", "Chưa đăng nhập")));
        }
        User user = userRepository.findById(principal.getUser().getId()).orElseThrow();
        if (user.getPhone() == null || user.getPhone().isBlank()) {
            return ResponseEntity.badRequest()
                    .body(AccountPhoneModalApiController.PhoneModalResponse.error(
                            Map.of("phone", "Chưa có số điện thoại chính")));
        }

        if (body == null) {
            return ResponseEntity.badRequest()
                    .body(AccountPhoneModalApiController.PhoneModalResponse.error(Map.of("phone", "Thiếu dữ liệu")));
        }
        String raw = body.getPhone();
        Map<String, String> errors = new LinkedHashMap<>(VietnamPhoneRules.validatePhoneRaw(raw));
        VietnamPhoneRules.addOtpCodeErrors(body.getCode(), errors);
        if (!errors.isEmpty()) {
            return ResponseEntity.badRequest()
                    .body(AccountPhoneModalApiController.PhoneModalResponse.error(errors));
        }
        String normalized = VietnamPhoneRules.normalizeDigits(raw);
        String code = body.getCode().trim();

        if (normalized.equals(user.getPhone())) {
            return ResponseEntity.badRequest()
                    .body(AccountPhoneModalApiController.PhoneModalResponse.error(
                            Map.of("phone", "Trùng số chính")));
        }
        if (userPhoneRepository.existsByUserAndPhone(user, normalized)) {
            return ResponseEntity.badRequest()
                    .body(AccountPhoneModalApiController.PhoneModalResponse.error(
                            Map.of("phone", "Bạn đã thêm số này rồi")));
        }

        try {
            assertPhoneNotUsedByAnotherUser(normalized, user);
        } catch (IllegalArgumentException ex) {
            return ResponseEntity.badRequest()
                    .body(AccountPhoneModalApiController.PhoneModalResponse.error(Map.of("phone", ex.getMessage())));
        }

        try {
            otpService.verifyOtp(normalized, OtpType.VERIFY_PHONE, code);
        } catch (IllegalArgumentException | IllegalStateException ex) {
            return ResponseEntity.badRequest()
                    .body(AccountPhoneModalApiController.PhoneModalResponse.error(Map.of("code", ex.getMessage())));
        }

        if (!userPhoneRepository.existsByUserAndPhone(user, normalized)) {
            UserPhone up = new UserPhone();
            up.setUser(user);
            up.setPhone(normalized);
            up.setVerified(true);
            userPhoneRepository.save(up);
        }

        refreshPrincipal(user.getId());
        return ResponseEntity.ok(AccountPhoneModalApiController.PhoneModalResponse.confirmed());
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

    private void refreshPrincipal(Long userId) {
        User fresh = userRepository.findById(userId).orElseThrow();
        FcarUserDetails details = new FcarUserDetails(fresh);
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(details, null, details.getAuthorities()));
    }

    private static String storeAvatarFile(MultipartFile file) throws IOException {
        Path uploadRoot = Paths.get("uploads", "avatars").toAbsolutePath().normalize();
        Files.createDirectories(uploadRoot);

        String originalFilename = org.springframework.util.StringUtils.cleanPath(
                file.getOriginalFilename() != null ? file.getOriginalFilename() : "avatar");
        String ext = "";
        int dot = originalFilename.lastIndexOf('.');
        if (dot != -1) {
            ext = originalFilename.substring(dot).toLowerCase();
            if (!ext.matches("\\.(jpg|jpeg|png|gif|webp)")) {
                ext = ".jpg";
            }
        } else {
            ext = ".jpg";
        }
        String filename = UUID.randomUUID() + ext;
        Path target = uploadRoot.resolve(filename);
        file.transferTo(target.toFile());
        return "/uploads/avatars/" + filename;
    }

    @Data
    public static class ProfileUpdateRequest {
        @NotBlank(message = "Họ tên không được để trống")
        @Size(max = 150, message = "Họ tên tối đa 150 ký tự")
        private String fullName;

        @Size(max = 50, message = "Giới tính tối đa 50 ký tự")
        private String gender;

        private java.time.LocalDate birthDate;

        @Size(max = 255, message = "Địa chỉ tối đa 255 ký tự")
        private String address;
    }

    @JsonInclude(JsonInclude.Include.NON_NULL)
    public record ProfileApiResponse(boolean ok, Map<String, String> fieldErrors) {
        static ProfileApiResponse success() {
            return new ProfileApiResponse(true, Map.of());
        }

        static ProfileApiResponse fail(Map<String, String> fieldErrors) {
            return new ProfileApiResponse(false, fieldErrors);
        }
    }

    @JsonInclude(JsonInclude.Include.NON_NULL)
    public record AvatarApiResponse(boolean ok, String avatarUrl, String message) {
        static AvatarApiResponse good(String url) {
            return new AvatarApiResponse(true, url, null);
        }

        static AvatarApiResponse bad(String message) {
            return new AvatarApiResponse(false, null, message);
        }
    }
}
