package com.fcar.config;

import com.fcar.domain.User;
import com.fcar.domain.enums.UserRole;
import com.fcar.repository.UserPhoneRepository;
import com.fcar.security.FcarUserDetails;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

/** Phân công: Nguyên — bắt buộc có SĐT trước khi dùng một số tính năng. */
@Component
@RequiredArgsConstructor
public class PhoneNumberRequiredInterceptor implements HandlerInterceptor {

    private final UserPhoneRepository userPhoneRepository;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler)
            throws Exception {
        String path = pathWithoutContext(request);
        if (isBypassPath(path)) {
            return true;
        }

        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !(auth.getPrincipal() instanceof FcarUserDetails fcarUserDetails)) {
            return true;
        }
        User user = fcarUserDetails.getUser();

        if (user.getRoles().contains(UserRole.ADMIN)) {
            return true;
        }

        if (user.getPhone() != null && !user.getPhone().isBlank()) {
            return true;
        }

        boolean hasExtraPhone = !userPhoneRepository.findByUser(user).isEmpty();
        if (hasExtraPhone) {
            return true;
        }

        request.setAttribute("needPhoneModal", Boolean.TRUE);
        return true;
    }

    private static String pathWithoutContext(HttpServletRequest request) {
        String uri = request.getRequestURI();
        String cp = request.getContextPath();
        if (cp != null && !cp.isEmpty() && uri.startsWith(cp)) {
            uri = uri.substring(cp.length());
        }
        if (uri.isEmpty()) {
            return "/";
        }
        return uri.startsWith("/") ? uri : "/" + uri;
    }

    private static boolean isBypassPath(String path) {
        return path.startsWith("/css")
                || path.startsWith("/js")
                || path.startsWith("/images")
                || path.startsWith("/uploads")
                || path.startsWith("/webjars")
                || path.startsWith("/auth")
                || path.startsWith("/oauth2")
                || path.startsWith("/account/api/")
                || path.startsWith("/logout");
    }
}
