package com.fcar.security;

import com.fcar.domain.User;
import com.fcar.repository.UserRepository;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.security.web.context.SecurityContextRepository;
import org.springframework.stereotype.Component;

/**
 * Phân công: dùng chung — đồng bộ {@link User} trong phiên với DB (đặt cọc, lịch sử, hồ sơ).
 * Tránh lỗi FK và User cũ trong {@link FcarUserDetails}.
 */
@Component
@RequiredArgsConstructor
public class AuthenticatedUserResolver {

    private final UserRepository userRepository;
    private final SecurityContextRepository securityContextRepository = new HttpSessionSecurityContextRepository();

    public User resolve(FcarUserDetails principal) {
        User cached = principal.getUser();
        if (cached.getId() != null) {
            return userRepository.findById(cached.getId())
                    .orElseGet(() -> reloadUserByLoginKey(principal.getUsername()));
        }
        return reloadUserByLoginKey(principal.getUsername());
    }

    private User reloadUserByLoginKey(String loginKey) {
        if (loginKey == null || loginKey.isBlank()) {
            throw new IllegalStateException("Không xác định được tài khoản. Vui lòng đăng nhập lại.");
        }
        return userRepository.findByEmailIgnoreCase(loginKey)
                .or(() -> userRepository.findByPhone(loginKey))
                .orElseThrow(() -> new IllegalStateException(
                        "Tài khoản không còn trong hệ thống. Vui lòng đăng xuất và đăng nhập lại."));
    }

    /**
     * Ghi đè SecurityContext (và session) bằng user mới load từ DB — dùng sau thao tác đã resolve user khớp DB.
     */
    public void refreshAuthenticatedPrincipal(User canonicalUser, HttpServletRequest request, HttpServletResponse response) {
        User fresh = userRepository.findById(canonicalUser.getId())
                .orElseThrow(() -> new IllegalStateException("User not found"));
        FcarUserDetails details = new FcarUserDetails(fresh);
        SecurityContext context = SecurityContextHolder.createEmptyContext();
        context.setAuthentication(new UsernamePasswordAuthenticationToken(details, null, details.getAuthorities()));
        SecurityContextHolder.setContext(context);
        securityContextRepository.saveContext(context, request, response);
    }
}
