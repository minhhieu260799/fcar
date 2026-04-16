package com.fcar.service;

import com.fcar.domain.User;
import com.fcar.domain.enums.UserRole;
import com.fcar.repository.UserRepository;
import jakarta.transaction.Transactional;
import java.util.Locale;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

/** Phân công: Nguyên — đăng ký, đổi mật khẩu (nghiệp vụ user). */
@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Transactional
    public User registerCustomer(String fullName, String phone, String email, String rawPassword) {
        String emailNorm = email.trim().toLowerCase(Locale.ROOT);
        if (userRepository.existsByEmailIgnoreCase(emailNorm)) {
            throw new IllegalArgumentException("email.duplicate");
        }
        if (userRepository.existsByPhone(phone)) {
            throw new IllegalArgumentException("phone.duplicate");
        }
        User user = new User();
        user.setFullName(fullName.trim());
        user.setPhone(phone);
        user.setEmail(emailNorm);
        user.setPasswordHash(passwordEncoder.encode(rawPassword));
        user.getRoles().add(UserRole.CUSTOMER);
        user.setEnabled(true);
        return userRepository.save(user);
    }

    @Transactional
    public void changePasswordWithoutCurrent(User user, String newRawPassword) {
        if (passwordEncoder.matches(newRawPassword, user.getPasswordHash())) {
            throw new IllegalArgumentException("Bạn đã sử dụng mật khẩu này rồi");
        }
        user.setPasswordHash(passwordEncoder.encode(newRawPassword));
        userRepository.save(user);
    }
}

