package com.fcar.controller;

/** Phân công: Nguyên — đổi mật khẩu, quản lý thông tin cá nhân (email/OTP trên trang hồ sơ). */
import com.fcar.domain.User;
import com.fcar.domain.UserPhone;
import com.fcar.domain.enums.OtpType;
import com.fcar.repository.UserPhoneRepository;
import com.fcar.repository.UserRepository;
import com.fcar.security.FcarUserDetails;
import com.fcar.service.OtpService;
import com.fcar.service.SessionService;
import jakarta.transaction.Transactional;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.util.List;
import java.util.regex.Pattern;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.propertyeditors.StringTrimmerEditor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.WebDataBinder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.InitBinder;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequiredArgsConstructor
@RequestMapping("/account")
public class AccountProfileController {

    private static final Pattern PASSWORD_POLICY = Pattern.compile(
            "^(?=.*[A-Z])(?=.*[0-9])(?=.*[^A-Za-z0-9]).{8,}$"
    );

    private final UserRepository userRepository;
    private final UserPhoneRepository userPhoneRepository;
    private final OtpService otpService;
    private final PasswordEncoder passwordEncoder;
    private final SessionService sessionService;

    @InitBinder("form")
    public void changePasswordFormBinder(WebDataBinder binder) {
        binder.registerCustomEditor(String.class, new StringTrimmerEditor(true));
    }

    @GetMapping("/profile")
    public String profile(Model model) {
        User user = getCurrentUser();
        if (user == null) {
            return "redirect:/auth/login";
        }
        EmailForm emailForm = new EmailForm();

        List<UserPhone> phones = userPhoneRepository.findByUser(user);

        model.addAttribute("emailForm", emailForm);
        model.addAttribute("user", user);
        model.addAttribute("phones", phones);
        return "account/profile";
    }

    private User getCurrentUser() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.getPrincipal() instanceof FcarUserDetails details) {
            return details.getUser();
        }
        return null;
    }

    @PostMapping("/email/send-otp")
    public String sendEmailOtp(@ModelAttribute("emailForm") EmailForm form,
                               BindingResult bindingResult,
                               Model model) {
        User user = getCurrentUser();
        if (user == null) return "redirect:/auth/login";
        if (user.getEmail() != null) {
            bindingResult.reject("email.exists", "Email hiện tại không thể thay đổi");
        }
        if (bindingResult.hasErrors()) {
            return profile(model);
        }
        try {
            otpService.sendVerifyEmailOtp(user, form.getEmail());
        } catch (IllegalArgumentException ex) {
            bindingResult.reject("email.error", ex.getMessage());
            return profile(model);
        }
        model.addAttribute("emailSent", true);
        return profile(model);
    }

    @PostMapping("/email/confirm")
    @Transactional
    public String confirmEmail(@ModelAttribute("emailForm") EmailForm form,
                               BindingResult bindingResult,
                               Model model) {
        User user = getCurrentUser();
        if (user == null) return "redirect:/auth/login";
        if (user.getEmail() != null) {
            bindingResult.reject("email.exists", "Email hiện tại không thể thay đổi");
            return profile(model);
        }
        try {
            otpService.verifyOtp(form.getEmail(), OtpType.VERIFY_EMAIL, form.getCode());
        } catch (Exception ex) {
            bindingResult.reject("otp.error", ex.getMessage());
            return profile(model);
        }
        user.setEmail(form.getEmail());
        userRepository.save(user);
        return "redirect:/account/profile?emailVerified";
    }

    @GetMapping("/change-password")
    public String showChangePasswordForm(Model model) {
        model.addAttribute("form", new ChangePasswordForm());
        return "account/change-password";
    }

    @PostMapping("/change-password")
    @Transactional
    public String changePassword(@ModelAttribute("form") @Valid ChangePasswordForm form,
                                 BindingResult bindingResult,
                                 jakarta.servlet.http.HttpSession session) {
        User user = getCurrentUser();
        if (user == null) return "redirect:/auth/login";
        if (bindingResult.hasErrors()) {
            return "account/change-password";
        }
        if (!passwordEncoder.matches(form.getCurrentPassword(), user.getPasswordHash())) {
            bindingResult.rejectValue("currentPassword", "password.current.invalid", "Mật khẩu hiện tại không đúng");
        }
        if (!PASSWORD_POLICY.matcher(form.getNewPassword()).matches()) {
            bindingResult.rejectValue("newPassword", "password.policy",
                    "Mật khẩu phải tối thiểu 8 ký tự, có ít nhất 1 ký tự Hoa, 1 ký tự đặc biệt và 1 số");
        }
        if (!form.getNewPassword().equals(form.getConfirmPassword())) {
            bindingResult.rejectValue("confirmPassword", "password.mismatch", "Mật khẩu xác nhận không khớp");
        }
        if (bindingResult.hasErrors()) {
            return "account/change-password";
        }
        if (passwordEncoder.matches(form.getNewPassword(), user.getPasswordHash())) {
            bindingResult.rejectValue("newPassword", "password.same", "Bạn đã sử dụng mật khẩu này rồi");
            return "account/change-password";
        }
        user.setPasswordHash(passwordEncoder.encode(form.getNewPassword()));
        userRepository.save(user);
        String currentSessionId = session != null ? session.getId() : null;
        sessionService.expireOtherSessions(user.getId(), currentSessionId);
        return "redirect:/account/change-password?changed";
    }

    @Data
    public static class EmailForm {
        @NotBlank
        @Email
        private String email;

        @NotBlank
        private String code;
    }

    @Data
    public static class ChangePasswordForm {
        @NotBlank(message = "Vui lòng nhập mật khẩu hiện tại")
        private String currentPassword;

        @NotBlank(message = "Vui lòng nhập mật khẩu mới")
        @Size(min = 8, max = 255, message = "Mật khẩu mới phải từ 8 đến 255 ký tự")
        private String newPassword;

        @NotBlank(message = "Vui lòng nhập lại mật khẩu xác nhận")
        private String confirmPassword;
    }
}

