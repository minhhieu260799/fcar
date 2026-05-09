package com.fcar.modules.user.config;

import com.fcar.modules.user.entity.User;
import com.fcar.modules.user.entity.enums.UserRole;
import com.fcar.modules.user.security.FcarUserDetails;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

@ControllerAdvice
public class GlobalModelAttributes {

    /**
     * {@code binding = false}: không merge query/body request vào {@link User}.
     * PayOS (và các cổng khác) redirect về kèm {@code id} dạng UUID — nếu bind sẽ lỗi chuyển sang {@code Long}.
     */
    @ModelAttribute(name = "currentUser", binding = false)
    public User currentUser() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.getPrincipal() instanceof FcarUserDetails details) {
            return details.getUser();
        }
        return null;
    }

    @ModelAttribute("isAdmin")
    public boolean isAdmin(@ModelAttribute("currentUser") User currentUser) {
        if (currentUser == null) {
            return false;
        }
        return currentUser.getRoles().contains(UserRole.ADMIN);
    }

    @ModelAttribute("isAdminSection")
    public boolean isAdminSection(HttpServletRequest request) {
        String uri = request.getRequestURI();
        return uri != null && uri.startsWith("/admin");
    }

    @ModelAttribute("requestURI")
    public String requestURI(HttpServletRequest request) {
        return request != null ? request.getRequestURI() : "";
    }

    @ModelAttribute("needPhoneModal")
    public boolean needPhoneModal(HttpServletRequest request) {
        if (request == null) {
            return false;
        }
        return Boolean.TRUE.equals(request.getAttribute("needPhoneModal"));
    }

    @ModelAttribute("csrfToken")
    public CsrfToken csrfToken(HttpServletRequest request) {
        if (request == null) {
            return null;
        }
        return (CsrfToken) request.getAttribute(CsrfToken.class.getName());
    }
}

