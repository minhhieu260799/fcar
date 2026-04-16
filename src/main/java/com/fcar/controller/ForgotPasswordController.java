package com.fcar.controller;

/** Phân công: Nguyên — quên mật khẩu (OTP email). */
import com.fcar.domain.User;
import com.fcar.domain.enums.OtpType;
import com.fcar.repository.UserRepository;
import com.fcar.service.OtpService;
import com.fcar.service.SessionService;
import com.fcar.service.UserService;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.util.Locale;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.propertyeditors.StringTrimmerEditor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.WebDataBinder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.InitBinder;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;

@Controller
@Validated
@RequiredArgsConstructor
public class ForgotPasswordController {

    private static final java.util.regex.Pattern PASSWORD_POLICY = java.util.regex.Pattern.compile(
            "^(?=.*[A-Z])(?=.*[0-9])(?=.*[^A-Za-z0-9]).{8,}$"
    );

    /** Chỉ validate email khi gửi OTP. */
    public interface OnSendOtp {}

    /** Validate email + OTP + mật khẩu khi đặt lại. */
    public interface OnReset {}

    private final OtpService otpService;
    private final UserService userService;
    private final UserRepository userRepository;
    private final SessionService sessionService;

    @InitBinder("form")
    public void formBinder(WebDataBinder binder) {
        binder.registerCustomEditor(String.class, new StringTrimmerEditor(true));
    }

    @GetMapping("/auth/forgot-password")
    public String showForgotPasswordForm(Model model) {
        model.addAttribute("form", new ForgotPasswordForm());
        return "auth/forgot-password";
    }

    @PostMapping("/auth/forgot-password/send-otp")
    public String sendOtp(@Validated(OnSendOtp.class) @ModelAttribute("form") ForgotPasswordForm form,
                          BindingResult bindingResult) {
        if (bindingResult.hasErrors()) {
            return "auth/forgot-password";
        }
        String emailNorm = form.getEmail().trim().toLowerCase(Locale.ROOT);
        form.setEmail(emailNorm);

        if (!userRepository.existsByEmailIgnoreCase(emailNorm)) {
            bindingResult.rejectValue(
                    "email",
                    "email.not.registered",
                    "Email này chưa được đăng ký.vui lòng kiểm tra lại."
            );
            return "auth/forgot-password";
        }

        try {
            otpService.sendForgotPasswordOtp(emailNorm);
            form.setOtpSent(true);
        } catch (IllegalArgumentException ex) {
            if ("email.not.registered".equals(ex.getMessage())) {
                bindingResult.rejectValue(
                        "email",
                        "email.not.registered",
                        "Email này chưa được đăng ký. Kiểm tra chính tả hoặc tạo tài khoản mới."
                );
            } else {
                bindingResult.reject("otp.error", ex.getMessage() != null ? ex.getMessage() : "Không thể gửi OTP.");
            }
        } catch (IllegalStateException ex) {
            bindingResult.reject("otp.error", ex.getMessage());
        } catch (Exception ex) {
            bindingResult.reject("otp.error", "Không thể gửi email. Thử lại sau.");
        }
        return "auth/forgot-password";
    }

    @PostMapping("/auth/forgot-password/reset")
    public String resetPassword(@Validated(OnReset.class) @ModelAttribute("form") ForgotPasswordForm form,
                                BindingResult bindingResult) {
        if (!form.isOtpSent()) {
            bindingResult.reject("otp.required", "Vui lòng yêu cầu mã OTP trước.");
            return "auth/forgot-password";
        }
        if (bindingResult.hasErrors()) {
            return "auth/forgot-password";
        }

        String emailNorm = form.getEmail().trim().toLowerCase(Locale.ROOT);
        form.setEmail(emailNorm);

        if (!PASSWORD_POLICY.matcher(form.getNewPassword()).matches()) {
            bindingResult.rejectValue(
                    "newPassword",
                    "password.policy",
                    "Mật khẩu phải tối thiểu 8 ký tự, có ít nhất 1 chữ hoa, 1 ký tự đặc biệt và 1 chữ số"
            );
            return "auth/forgot-password";
        }
        if (!form.getNewPassword().equals(form.getConfirmPassword())) {
            bindingResult.rejectValue("confirmPassword", "password.mismatch", "Mật khẩu xác nhận không khớp");
            return "auth/forgot-password";
        }

        User user = userRepository.findByEmailIgnoreCase(emailNorm).orElse(null);
        if (user == null) {
            bindingResult.rejectValue("email", "email.not.registered", "Email không còn trong hệ thống.");
            return "auth/forgot-password";
        }
        try {
            otpService.verifyOtp(emailNorm, OtpType.FORGOT_PASSWORD, form.getCode().trim());
        } catch (IllegalArgumentException | IllegalStateException ex) {
            bindingResult.rejectValue("code", "otp.invalid", ex.getMessage());
            return "auth/forgot-password";
        }

        userService.changePasswordWithoutCurrent(user, form.getNewPassword());
        sessionService.expireOtherSessions(user.getId(), null);
        return "redirect:/auth/login?passwordReset";
    }

    @Data
    public static class ForgotPasswordForm {
        @NotBlank(groups = {OnSendOtp.class, OnReset.class}, message = "Vui lòng nhập email")
        @Email(groups = {OnSendOtp.class, OnReset.class}, message = "Email không đúng định dạng")
        @Size(max = 150, groups = {OnSendOtp.class, OnReset.class}, message = "Email tối đa 150 ký tự")
        private String email;

        private boolean otpSent;

        @NotBlank(groups = OnReset.class, message = "Vui lòng nhập mã OTP")
        @Pattern(regexp = "^\\d{6}$", groups = OnReset.class, message = "Mã OTP gồm 6 chữ số")
        private String code;

        @NotBlank(groups = OnReset.class, message = "Vui lòng nhập mật khẩu mới")
        @Size(min = 8, max = 255, groups = OnReset.class, message = "Mật khẩu phải từ 8 đến 255 ký tự")
        private String newPassword;

        @NotBlank(groups = OnReset.class, message = "Vui lòng nhập lại mật khẩu xác nhận")
        private String confirmPassword;
    }
}
