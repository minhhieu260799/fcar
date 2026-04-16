package com.fcar.controller;

import com.fcar.domain.User;
import com.fcar.repository.CarOrderRepository;
import com.fcar.repository.ContactRequestRepository;
import com.fcar.repository.TestDriveBookingRepository;
import com.fcar.security.AuthenticatedUserResolver;
import com.fcar.security.FcarUserDetails;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/**
 * Phân công: Minh — lịch sử mua xe (tab đơn hàng);
 * Bảo — lịch sử liên hệ tư vấn & đăng ký lái thử (tab còn lại).
 */
@Controller
@RequiredArgsConstructor
@RequestMapping("/account")
public class AccountController {

    private final CarOrderRepository carOrderRepository;
    private final ContactRequestRepository contactRequestRepository;
    private final TestDriveBookingRepository testDriveBookingRepository;
    private final AuthenticatedUserResolver authenticatedUserResolver;

    @GetMapping("/history")
    public String history(@AuthenticationPrincipal FcarUserDetails principal,
                          RedirectAttributes redirectAttributes,
                          HttpServletRequest request,
                          HttpServletResponse response,
                          Model model,
                          @RequestParam(value = "tab", required = false) String tab) {
        if (principal == null) {
            return "redirect:/auth/login";
        }
        User user;
        try {
            user = authenticatedUserResolver.resolve(principal);
            authenticatedUserResolver.refreshAuthenticatedPrincipal(user, request, response);
        } catch (IllegalStateException ex) {
            redirectAttributes.addFlashAttribute("flashError", ex.getMessage());
            return "redirect:/auth/login";
        }
        String historyTab = null;
        if (tab != null && !tab.isBlank()) {
            String t = tab.trim().toLowerCase();
            if ("orders".equals(t) || "contacts".equals(t) || "testdrives".equals(t)) {
                historyTab = t;
            }
        }
        model.addAttribute("historyTab", historyTab);
        model.addAttribute("orders", carOrderRepository.findByUserWithCarDetails(user));
        model.addAttribute("contacts", contactRequestRepository.findByUserWithCarDetails(user));
        model.addAttribute("testDrives", testDriveBookingRepository.findByUserWithCarDetails(user));
        return "account/history";
    }
}

