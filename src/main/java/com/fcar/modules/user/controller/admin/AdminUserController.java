package com.fcar.modules.user.controller.admin;

import com.fcar.modules.user.entity.User;
import com.fcar.modules.user.entity.UserPhone;
import com.fcar.modules.user.entity.enums.UserRole;
import com.fcar.modules.user.repository.UserPhoneRepository;
import com.fcar.modules.user.repository.UserRepository;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.server.ResponseStatusException;

/** Phân công: Hiếu — quản lý người dùng (admin). */
@Controller
@RequiredArgsConstructor
@RequestMapping("/admin/users")
public class AdminUserController {

    private final UserRepository userRepository;
    private final UserPhoneRepository userPhoneRepository;

    @GetMapping
    public String list(Model model) {
        List<User> users = userRepository.findAll();
        model.addAttribute("users", users);
        return "admin/users/list";
    }

    @GetMapping("/{id}")
    public String detail(@PathVariable Long id, Model model) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        List<UserPhone> extraPhones = userPhoneRepository.findByUser(user);
        model.addAttribute("user", user);
        model.addAttribute("extraPhones", extraPhones);
        return "admin/users/detail";
    }

    @PostMapping("/{id}/lock")
    public String lock(@PathVariable Long id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));
        if (user.getRoles().contains(UserRole.ADMIN)) {
            return "redirect:/admin/users";
        }
        user.setLockedByAdmin(true);
        userRepository.save(user);
        return "redirect:/admin/users";
    }

    @PostMapping("/{id}/unlock")
    public String unlock(@PathVariable Long id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));
        user.setLockedByAdmin(false);
        userRepository.save(user);
        return "redirect:/admin/users";
    }
}
