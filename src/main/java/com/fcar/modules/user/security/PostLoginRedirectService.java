package com.fcar.modules.user.security;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.web.DefaultRedirectStrategy;
import org.springframework.security.web.RedirectStrategy;
import org.springframework.security.web.savedrequest.DefaultSavedRequest;
import org.springframework.security.web.savedrequest.HttpSessionRequestCache;
import org.springframework.security.web.savedrequest.RequestCache;
import org.springframework.security.web.savedrequest.SavedRequest;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

/** Phân công: Nguyên — điều hướng an toàn sau đăng nhập (redirect param / saved request). */
@Component
public class PostLoginRedirectService {

    private final RequestCache requestCache = new HttpSessionRequestCache();
    private final RedirectStrategy redirectStrategy = new DefaultRedirectStrategy();

    public void sendRedirectAfterLogin(HttpServletRequest request,
                                       HttpServletResponse response,
                                       Authentication authentication,
                                       String explicitRedirect) throws IOException {
        boolean isAdmin = isAdmin(authentication);

        if (isSafeRelativeRedirect(explicitRedirect)) {
            requestCache.removeRequest(request, response);
            redirectStrategy.sendRedirect(request, response, request.getContextPath() + explicitRedirect.trim());
            return;
        }

        SavedRequest saved = requestCache.getRequest(request, response);
        if (saved != null) {
            requestCache.removeRequest(request, response);
            redirectStrategy.sendRedirect(request, response, saved.getRedirectUrl());
            return;
        }

        String fallback = isAdmin ? "/admin" : "/";
        redirectStrategy.sendRedirect(request, response, request.getContextPath() + fallback);
    }

    /**
     * Dùng với {@code return "redirect:" + path} (Spring MVC tự thêm context path).
     */
    public String resolveMvcRedirectPath(HttpServletRequest request,
                                         HttpServletResponse response,
                                         Authentication authentication,
                                         String explicitRedirect) {
        boolean isAdmin = isAdmin(authentication);

        if (isSafeRelativeRedirect(explicitRedirect)) {
            requestCache.removeRequest(request, response);
            return explicitRedirect.trim();
        }

        SavedRequest saved = requestCache.getRequest(request, response);
        if (saved != null) {
            requestCache.removeRequest(request, response);
            return toApplicationRelativePath(saved, request);
        }

        return isAdmin ? "/admin" : "/";
    }

    private static boolean isAdmin(Authentication authentication) {
        if (authentication == null) {
            return false;
        }
        return authentication.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .anyMatch("ROLE_ADMIN"::equals);
    }

    static boolean isSafeRelativeRedirect(String target) {
        if (!StringUtils.hasText(target)) {
            return false;
        }
        String t = target.trim();
        if (!t.startsWith("/")) {
            return false;
        }
        if (t.startsWith("//")) {
            return false;
        }
        if (t.contains("://")) {
            return false;
        }
        if (t.contains("\\") || t.contains("\n") || t.contains("\r")) {
            return false;
        }
        if (t.contains("@")) {
            return false;
        }
        return true;
    }

    private static String toApplicationRelativePath(SavedRequest saved, HttpServletRequest request) {
        if (saved instanceof DefaultSavedRequest dr) {
            String uri = dr.getRequestURI();
            String cp = request.getContextPath();
            if (cp != null && !cp.isEmpty() && uri.startsWith(cp)) {
                uri = uri.substring(cp.length());
            }
            if (uri.isEmpty()) {
                uri = "/";
            }
            String qs = dr.getQueryString();
            if (StringUtils.hasText(qs)) {
                return uri + "?" + qs;
            }
            return uri;
        }
        try {
            java.net.URL u = new java.net.URL(saved.getRedirectUrl());
            String path = u.getPath();
            String cp = request.getContextPath();
            if (cp != null && !cp.isEmpty() && path.startsWith(cp)) {
                path = path.substring(cp.length());
            }
            if (path.isEmpty()) {
                path = "/";
            }
            String q = u.getQuery();
            return StringUtils.hasText(q) ? path + "?" + q : path;
        } catch (Exception e) {
            return "/";
        }
    }
}
