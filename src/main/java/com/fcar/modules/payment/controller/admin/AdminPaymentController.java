package com.fcar.modules.payment.controller.admin;

import com.fcar.modules.payment.entity.PaymentConfig;
import com.fcar.modules.branch.entity.SupportHotline;
import com.fcar.modules.payment.repository.PaymentConfigRepository;
import com.fcar.modules.branch.repository.SupportHotlineRepository;
import jakarta.validation.constraints.NotBlank;
import java.math.BigDecimal;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;

/** Phân công: Minh — cấu hình thanh toán (cọc %, hotline, PayOS-only). */
@Controller
@RequiredArgsConstructor
@RequestMapping("/admin/payment")
public class AdminPaymentController {

    private final PaymentConfigRepository paymentConfigRepository;
    private final SupportHotlineRepository supportHotlineRepository;

    @GetMapping
    public String show(Model model) {
        PaymentConfig config = paymentConfigRepository.findAll().stream().findFirst()
                .orElseGet(() -> {
                    PaymentConfig c = new PaymentConfig();
                    c.setDepositPercent(BigDecimal.valueOf(20));
                    return paymentConfigRepository.save(c);
                });
        model.addAttribute("config", config);
        model.addAttribute("hotlines", supportHotlineRepository.findAll());
        model.addAttribute("hotlineForm", new HotlineForm());
        return "admin/payment/config";
    }

    @PostMapping("/config")
    public String updateConfig(@ModelAttribute("config") PaymentConfig config) {
        paymentConfigRepository.save(config);
        return "redirect:/admin/payment";
    }

    @PostMapping("/hotlines")
    public String addHotline(@ModelAttribute("hotlineForm") HotlineForm form,
                             BindingResult bindingResult) {
        if (bindingResult.hasErrors()) {
            return "redirect:/admin/payment";
        }
        SupportHotline h = new SupportHotline();
        h.setPhoneNumber(form.getPhoneNumber());
        h.setActive(true);
        supportHotlineRepository.save(h);
        return "redirect:/admin/payment";
    }

    @PostMapping("/hotlines/{id}/toggle")
    public String toggleHotline(@PathVariable Long id) {
        SupportHotline h = supportHotlineRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Hotline not found"));
        h.setActive(!h.isActive());
        supportHotlineRepository.save(h);
        return "redirect:/admin/payment";
    }

    @Data
    public static class HotlineForm {
        @NotBlank
        private String phoneNumber;
    }
}

