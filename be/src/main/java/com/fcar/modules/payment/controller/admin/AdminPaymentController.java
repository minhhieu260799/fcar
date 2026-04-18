package com.fcar.modules.payment.controller.admin;

import com.fcar.modules.payment.entity.PaymentConfig;
import com.fcar.modules.payment.entity.StoreBankAccount;
import com.fcar.modules.branch.entity.SupportHotline;
import com.fcar.modules.payment.repository.PaymentConfigRepository;
import com.fcar.modules.payment.repository.StoreBankAccountRepository;
import com.fcar.modules.branch.repository.SupportHotlineRepository;
import jakarta.validation.constraints.NotBlank;
import java.math.BigDecimal;
import java.util.List;
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
import org.springframework.web.bind.annotation.RequestParam;

/** Phân công: Minh — cấu hình thanh toán (cọc %, TK ngân hàng, hotline). */
@Controller
@RequiredArgsConstructor
@RequestMapping("/admin/payment")
public class AdminPaymentController {

    private final PaymentConfigRepository paymentConfigRepository;
    private final SupportHotlineRepository supportHotlineRepository;
    private final StoreBankAccountRepository storeBankAccountRepository;

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
        model.addAttribute("accounts", storeBankAccountRepository.findAll());
        model.addAttribute("hotlineForm", new HotlineForm());
        model.addAttribute("accountForm", new BankAccountForm());
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

    @PostMapping("/accounts")
    public String addAccount(@ModelAttribute("accountForm") BankAccountForm form,
                             BindingResult bindingResult) {
        if (bindingResult.hasErrors()) {
            return "redirect:/admin/payment";
        }
        StoreBankAccount acc = new StoreBankAccount();
        acc.setBankName(form.getBankName());
        acc.setAccountName(form.getAccountName());
        acc.setAccountNumber(form.getAccountNumber());
        if (form.getBankBin() != null && !form.getBankBin().isBlank()) {
            acc.setBankBin(form.getBankBin().trim());
        }
        acc.setActive(true);
        storeBankAccountRepository.save(acc);
        return "redirect:/admin/payment";
    }

    @PostMapping("/accounts/{id}/bin")
    public String updateAccountBankBin(@PathVariable Long id,
                                       @RequestParam(value = "bankBin", required = false) String bankBin) {
        StoreBankAccount acc = storeBankAccountRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Account not found"));
        if (bankBin == null || bankBin.isBlank()) {
            acc.setBankBin(null);
        } else {
            acc.setBankBin(bankBin.trim());
        }
        storeBankAccountRepository.save(acc);
        return "redirect:/admin/payment";
    }

    @PostMapping("/accounts/{id}/toggle")
    public String toggleAccount(@PathVariable Long id) {
        StoreBankAccount acc = storeBankAccountRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Account not found"));
        acc.setActive(!acc.isActive());
        storeBankAccountRepository.save(acc);
        return "redirect:/admin/payment";
    }

    @Data
    public static class HotlineForm {
        @NotBlank
        private String phoneNumber;
    }

    @Data
    public static class BankAccountForm {
        @NotBlank
        private String bankName;
        @NotBlank
        private String accountName;
        @NotBlank
        private String accountNumber;

        /** Mã BIN 6 số (NAPAS) để VietQR, ví dụ 970436 */
        private String bankBin;
    }
}

