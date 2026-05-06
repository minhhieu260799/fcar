package com.fcar.modules.contact.controller.admin;

import com.fcar.modules.contact.entity.ContactRequest;
import com.fcar.modules.contact.entity.enums.ContactStatus;
import com.fcar.modules.contact.repository.ContactRequestRepository;
import com.fcar.modules.testdrive.service.ContactTestDriveEmailHtmlService;
import jakarta.transaction.Transactional;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

/** Phân công: Bảo — quản lý yêu cầu liên hệ tư vấn (admin). */
@Controller
@RequiredArgsConstructor
@RequestMapping("/admin/contacts")
public class AdminContactRequestController {

    private final ContactRequestRepository contactRequestRepository;
    private final ContactTestDriveEmailHtmlService contactTestDriveEmailHtmlService;

    @GetMapping
    public String list(@RequestParam(value = "year", required = false) Integer year,
                       @RequestParam(value = "month", required = false) Integer month,
                       @RequestParam(value = "status", required = false) ContactStatus status,
                       Model model) {
        List<ContactRequest> all = contactRequestRepository.findAllWithUserAndCarDetails();
        List<ContactRequest> filtered = all.stream()
                .filter(c -> c.getCreatedAt() != null && matchesYearMonthFilter(c.getCreatedAt(), year, month))
                .filter(c -> status == null || c.getStatus() == status)
                .collect(Collectors.toList());

        int currentYear = YearMonth.now().getYear();
        List<Integer> yearOptions = IntStream.rangeClosed(currentYear - 6, currentYear + 1)
                .boxed()
                .collect(Collectors.toList());

        model.addAttribute("requests", filtered);
        model.addAttribute("filterYear", year);
        model.addAttribute("filterMonth", month);
        model.addAttribute("status", status);
        model.addAttribute("allStatuses", ContactStatus.values());
        model.addAttribute("yearOptions", yearOptions);
        return "admin/contacts/list";
    }

    /** Cùng quy tắc lọc năm/tháng như trang quản lý đơn đặt mua xe. */
    private static boolean matchesYearMonthFilter(LocalDateTime createdAt, Integer year, Integer month) {
        if (year == null && month == null) {
            return true;
        }
        if (year != null && month == null) {
            return createdAt.getYear() == year;
        }
        if (year == null) {
            return createdAt.getMonthValue() == month;
        }
        YearMonth ym = YearMonth.of(year, month);
        LocalDateTime start = ym.atDay(1).atStartOfDay();
        LocalDateTime end = ym.atEndOfMonth().atTime(23, 59, 59);
        return !createdAt.isBefore(start) && !createdAt.isAfter(end);
    }

    @GetMapping("/{id}")
    public String detail(@PathVariable Long id, Model model) {
        ContactRequest contactRequest = contactRequestRepository.findByIdWithUserAndCarDetails(id)
                .orElseThrow(() -> new IllegalArgumentException("Contact not found"));
        model.addAttribute("contactRequest", contactRequest);
        return "admin/contacts/detail";
    }

    @PostMapping("/{id}/status")
    @Transactional
    public String updateStatus(@PathVariable Long id,
                               @RequestParam("status") ContactStatus newStatus,
                               RedirectAttributes redirectAttributes) {
        ContactRequest req = contactRequestRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Contact not found"));

        if (req.getStatus() == ContactStatus.CONTACTED || req.getStatus() == ContactStatus.CANCELED) {
            redirectAttributes.addFlashAttribute("error",
                    "Yêu cầu đã được xử lý, không thể đổi trạng thái.");
            return "redirect:/admin/contacts/" + id;
        }

        if (newStatus != ContactStatus.CONTACTED && newStatus != ContactStatus.CANCELED) {
            redirectAttributes.addFlashAttribute("error",
                    "Chỉ có thể cập nhật sang Đã liên hệ hoặc Đã hủy.");
            return "redirect:/admin/contacts/" + id;
        }

        if (newStatus == ContactStatus.CONTACTED) {
            req.setStatus(ContactStatus.CONTACTED);
        } else {
            req.setStatus(ContactStatus.CANCELED);
        }
        contactRequestRepository.save(req);

        contactTestDriveEmailHtmlService.sendContactRequestStatusUpdated(req);

        redirectAttributes.addFlashAttribute("success", "Đã cập nhật trạng thái yêu cầu.");
        return "redirect:/admin/contacts/" + id;
    }
}

