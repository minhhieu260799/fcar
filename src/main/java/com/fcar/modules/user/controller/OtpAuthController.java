package com.fcar.modules.user.controller;

/** Phân công: Nguyên — đăng nhập bằng số điện thoại (OTP). */
import com.fcar.modules.user.entity.User;
import com.fcar.modules.user.entity.UserPhone;
import com.fcar.modules.user.entity.enums.OtpType;
import com.fcar.modules.user.repository.UserPhoneRepository;
import com.fcar.modules.user.repository.UserRepository;
import com.fcar.modules.user.security.FcarUserDetails;
import com.fcar.modules.user.security.PostLoginRedirectService;
import com.fcar.modules.user.service.OtpService;
import com.fcar.core.util.VietnamPhoneRules;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import java.util.LinkedHashMap;
import java.util.Map;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.security.web.context.SecurityContextRepository;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;

@Controller
@RequiredArgsConstructor
public class OtpAuthController {

    private final SecurityContextRepository securityContextRepository = new HttpSessionSecurityContextRepository();
    private final OtpService otpService;
    private final UserRepository userRepository;
    private final UserPhoneRepository userPhoneRepository;
    private final PostLoginRedirectService postLoginRedirectService;

    @GetMapping("/auth/login-phone")
    public String showLoginPhoneForm(Model model) {
        model.addAttribute("form", new LoginPhoneForm());
        return "auth/login-phone";
    }

    @PostMapping("/auth/login-phone/send-otp")
    public String sendOtp(@ModelAttribute("form") @Valid LoginPhoneForm form, BindingResult bindingResult, Model model) {
        if (bindingResult.hasErrors()) {
            attachLoginPhoneDemoOtp(model, form);
            return "auth/login-phone";
        }
        Map<String, String> phoneErrors = VietnamPhoneRules.validatePhoneRaw(form.getPhone());
        rejectFieldErrors(bindingResult, phoneErrors);
        if (bindingResult.hasErrors()) {
            attachLoginPhoneDemoOtp(model, form);
            return "auth/login-phone";
        }
        String normalized = VietnamPhoneRules.normalizeDigits(form.getPhone());
        try {
            otpService.sendLoginPhoneOtp(normalized);
        } catch (IllegalArgumentException | IllegalStateException ex) {
            bindingResult.reject("otpError", ex.getMessage());
            attachLoginPhoneDemoOtp(model, form);
            return "auth/login-phone";
        }
        form.setPhone(normalized);
        form.setOtpSent(true);
        form.setCode(null);
        attachLoginPhoneDemoOtp(model, form);
        return "auth/login-phone";
    }

    @PostMapping("/auth/login-phone/verify")
    public String verifyOtp(@ModelAttribute("form") @Valid LoginPhoneForm form, BindingResult bindingResult,
                            Model model, HttpServletRequest request, HttpServletResponse response) {
        if (bindingResult.hasErrors()) {
            form.setOtpSent(true);
            attachLoginPhoneDemoOtp(model, form);
            return "auth/login-phone";
        }
        Map<String, String> phoneErrors = VietnamPhoneRules.validatePhoneRaw(form.getPhone());
        rejectFieldErrors(bindingResult, phoneErrors);
        Map<String, String> codeErrors = new LinkedHashMap<>();
        VietnamPhoneRules.addOtpCodeErrors(form.getCode(), codeErrors);
        rejectFieldErrors(bindingResult, codeErrors);
        if (bindingResult.hasErrors()) {
            form.setOtpSent(true);
            attachLoginPhoneDemoOtp(model, form);
            return "auth/login-phone";
        }

        String normalized = VietnamPhoneRules.normalizeDigits(form.getPhone());
        String code = form.getCode().trim();
        try {
            otpService.verifyOtp(normalized, OtpType.LOGIN_PHONE, code);
        } catch (IllegalArgumentException | IllegalStateException ex) {
            bindingResult.reject("otpError", ex.getMessage());
            form.setOtpSent(true);
            form.setPhone(normalized);
            attachLoginPhoneDemoOtp(model, form);
            return "auth/login-phone";
        }

        User user = userRepository.findByPhone(normalized)
                .orElseGet(() -> {
                    UserPhone up = userPhoneRepository.findFirstByPhone(normalized)
                            .orElseThrow(() -> new IllegalStateException("Số điện thoại chưa được đăng ký"));
                    return up.getUser();
                });
        FcarUserDetails userDetails = new FcarUserDetails(user);
        UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(
                userDetails, null, userDetails.getAuthorities()
        );
        SecurityContext context = SecurityContextHolder.createEmptyContext();
        context.setAuthentication(authentication);
        SecurityContextHolder.setContext(context);
        securityContextRepository.saveContext(context, request, response);

        String path = postLoginRedirectService.resolveMvcRedirectPath(request, response, authentication, null);
        return "redirect:" + path;
    }

    private void attachLoginPhoneDemoOtp(Model model, LoginPhoneForm form) {
        if (!form.isOtpSent() || form.getPhone() == null || form.getPhone().isBlank()) {
            return;
        }
        String normalized = VietnamPhoneRules.normalizeDigits(form.getPhone());
        String demo = otpService.getActiveLoginPhoneOtpDemoCode(normalized);
        if (demo != null) {
            model.addAttribute("demoOtp", demo);
        }
    }

    private static void rejectFieldErrors(BindingResult bindingResult, Map<String, String> fieldErrors) {
        fieldErrors.forEach((field, message) ->
                bindingResult.rejectValue(field, field + ".invalid", message));
    }

    @Data
    public static class LoginPhoneForm {
        @NotBlank(message = "Vui lòng nhập số điện thoại")
        private String phone;

        private String code;

        private boolean otpSent;
    }
}
