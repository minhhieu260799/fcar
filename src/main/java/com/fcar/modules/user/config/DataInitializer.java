package com.fcar.modules.user.config;

import com.fcar.modules.user.entity.User;
import com.fcar.modules.user.entity.enums.UserRole;
import com.fcar.modules.user.repository.UserRepository;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
@Slf4j
public class DataInitializer implements CommandLineRunner {

    private final UserRepository userRepository;
    private final org.springframework.security.crypto.password.PasswordEncoder passwordEncoder;

    @Override
    @Transactional
    public void run(String... args) {
        ensureDefaultAdminUser();
    }

    private void ensureDefaultAdminUser() {
        String email = "admin@fcar.com";
        Optional<User> existingOpt = userRepository.findByEmail(email);
        if (existingOpt.isPresent()) {
            User existing = existingOpt.get();
            if (!existing.getRoles().contains(UserRole.ADMIN)) {
                existing.getRoles().add(UserRole.ADMIN);
                userRepository.save(existing);
                log.info("Ensured ADMIN role for existing user {}", email);
            }
            return;
        }

        User admin = new User();
        admin.setFullName("Quản trị hệ thống");
        admin.setPhone("0900000000");
        admin.setEmail(email);
        admin.setPasswordHash(passwordEncoder.encode("Admin@123"));
        admin.setEnabled(true);
        admin.setLockedByAdmin(false);
        admin.getRoles().add(UserRole.ADMIN);

        userRepository.save(admin);
        log.info("Created default admin user {} with role ADMIN", email);
    }
}

