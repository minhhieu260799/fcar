package com.fcar.controller;

/** Phân công: Nguyên — đăng ký tài khoản. */
import com.fcar.repository.UserRepository;
import com.fcar.service.UserService;
import com.fcar.util.VietnamPhoneRules;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Pattern;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.propertyeditors.StringTrimmerEditor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.WebDataBinder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.InitBinder;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;

@Controller
@RequiredArgsConstructor
public class AuthController {

    private final UserService userService;
    private final UserRepository userRepository;

    private static final Pattern PASSWORD_POLICY = Pattern.compile(
            "^(?=.*[A-Z])(?=.*[0-9])(?=.*[^A-Za-z0-9]).{8,}$"
    );

    @InitBinder("form")
    public void registerFormBinder(WebDataBinder binder) {
        binder.registerCustomEditor(String.class, new StringTrimmerEditor(true));
    }

    @GetMapping("/auth/login")
    public String login() {
        return "auth/login";
    }

    @GetMapping("/auth/register")
    public String showRegisterForm(Model model) {
        model.addAttribute("form", new RegisterForm());
        return "auth/register";
    }

    @PostMapping("/auth/register")
    public String register(@ModelAttribute("form") @Valid RegisterForm form, BindingResult bindingResult) {
        if (bindingResult.hasErrors()) {
            return "auth/register";
        }

        Map<String, String> phoneErrors = VietnamPhoneRules.validatePhoneRaw(form.getPhone());
        rejectFieldErrors(bindingResult, phoneErrors);
        if (bindingResult.hasErrors()) {
            return "auth/register";
        }

        String normalizedPhone = VietnamPhoneRules.normalizeDigits(form.getPhone());
        String emailNorm = form.getEmail().trim().toLowerCase(Locale.ROOT);

        if (!PASSWORD_POLICY.matcher(form.getPassword()).matches()) {
            bindingResult.rejectValue(
                    "password",
                    "password.policy",
                    "Mật khẩu phải tối thiểu 8 ký tự, có ít nhất 1 chữ hoa, 1 ký tự đặc biệt và 1 chữ số"
            );
            return "auth/register";
        }
        if (!form.getPassword().equals(form.getConfirmPassword())) {
            bindingResult.rejectValue("confirmPassword", "password.mismatch", "Mật khẩu xác nhận không khớp");
            return "auth/register";
        }

        if (userRepository.existsByEmailIgnoreCase(emailNorm)) {
            bindingResult.rejectValue(
                    "email",
                    "email.duplicate",
                    "Email này đã được đăng ký. Hãy đăng nhập hoặc dùng chức năng quên mật khẩu."
            );
        }
        if (userRepository.existsByPhone(normalizedPhone)) {
            bindingResult.rejectValue(
                    "phone",
                    "phone.duplicate",
                    "Số điện thoại này đã được đăng ký. Hãy đăng nhập bằng số điện thoại."
            );
        }
        if (bindingResult.hasErrors()) {
            return "auth/register";
        }

        try {
            userService.registerCustomer(form.getFullName().trim(), normalizedPhone, emailNorm, form.getPassword());
        } catch (IllegalArgumentException ex) {
            String code = ex.getMessage();
            if ("email.duplicate".equals(code)) {
                bindingResult.rejectValue(
                        "email",
                        "email.duplicate",
                        "Email này đã được đăng ký. Hãy đăng nhập hoặc dùng chức năng quên mật khẩu."
                );
            } else if ("phone.duplicate".equals(code)) {
                bindingResult.rejectValue(
                        "phone",
                        "phone.duplicate",
                        "Số điện thoại này đã được đăng ký. Hãy đăng nhập bằng số điện thoại."
                );
            } else {
                bindingResult.reject("registration.error", code != null ? code : "Đăng ký không thành công.");
            }
            return "auth/register";
        }
        return "redirect:/auth/login?registered";
    }

    private static void rejectFieldErrors(BindingResult bindingResult, Map<String, String> fieldErrors) {
        fieldErrors.forEach((field, message) ->
                bindingResult.rejectValue(field, field + ".invalid", message));
    }

    @Data
    public static class RegisterForm {
        @NotBlank(message = "Vui lòng nhập họ tên")
        @Size(max = 150, message = "Họ tên tối đa 150 ký tự")
        private String fullName;

        @NotBlank(message = "Vui lòng nhập số điện thoại")
        private String phone;

        @NotBlank(message = "Vui lòng nhập email")
        @Email(message = "Email không đúng định dạng")
        @Size(max = 150, message = "Email tối đa 150 ký tự")
        private String email;

        @NotBlank(message = "Vui lòng nhập mật khẩu")
        @Size(min = 8, max = 255, message = "Mật khẩu phải từ 8 đến 255 ký tự")
        private String password;

        @NotBlank(message = "Vui lòng nhập lại mật khẩu xác nhận")
        private String confirmPassword;
    }
}
