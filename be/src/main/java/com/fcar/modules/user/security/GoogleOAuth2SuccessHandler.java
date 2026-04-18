package com.fcar.modules.user.security;

import com.fcar.modules.user.entity.User;
import com.fcar.modules.user.entity.enums.UserRole;
import com.fcar.modules.user.repository.UserRepository;
import java.io.IOException;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.security.web.DefaultRedirectStrategy;
import org.springframework.security.web.RedirectStrategy;
import org.springframework.stereotype.Component;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/** Phân công: Nguyên — đăng nhập bằng Google. */
@Component
@RequiredArgsConstructor
public class GoogleOAuth2SuccessHandler implements AuthenticationSuccessHandler {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final PostLoginRedirectService postLoginRedirectService;
    private final RedirectStrategy redirectStrategy = new DefaultRedirectStrategy();

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request,
                                        HttpServletResponse response,
                                        Authentication authentication) throws IOException, ServletException {

        Object principal = authentication.getPrincipal();
        if (!(principal instanceof OAuth2User oauth2User)) {
            response.sendRedirect("/");
            return;
        }

        Map<String, Object> attributes = oauth2User.getAttributes();
        String email = (String) attributes.get("email");
        String name = (String) attributes.getOrDefault("name", "Google User");

        if (email == null) {
            redirectStrategy.sendRedirect(request, response, "/auth/login?oauth2Error");
            return;
        }

        Object emailVerifiedAttr = attributes.get("email_verified");
        boolean emailVerified = Boolean.TRUE.equals(emailVerifiedAttr)
                || "true".equalsIgnoreCase(String.valueOf(emailVerifiedAttr));
        if (!emailVerified) {
            redirectStrategy.sendRedirect(request, response, "/auth/login?oauth2Unverified");
            return;
        }

        User user = userRepository.findByEmail(email).orElseGet(() -> {
            User u = new User();
            u.setEmail(email);
            u.setFullName(name);
            u.setPhone(null);
            // Đặt mật khẩu ngẫu nhiên để không đăng nhập bằng password khi chưa thiết lập
            u.setPasswordHash(passwordEncoder.encode("GOOGLE_" + email));
            u.getRoles().add(UserRole.CUSTOMER);
            u.setEnabled(true);
            return userRepository.save(u);
        });

        FcarUserDetails userDetails = new FcarUserDetails(user);
        Authentication newAuth = new org.springframework.security.authentication.UsernamePasswordAuthenticationToken(
                userDetails, null, userDetails.getAuthorities());
        org.springframework.security.core.context.SecurityContextHolder.getContext().setAuthentication(newAuth);

        postLoginRedirectService.sendRedirectAfterLogin(request, response, newAuth, null);
    }
}

