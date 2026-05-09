package com.fcar.modules.testdrive.controller.admin;

import com.fcar.modules.testdrive.entity.TestDriveBooking;
import com.fcar.modules.testdrive.entity.enums.TestDriveStatus;
import com.fcar.modules.testdrive.repository.TestDriveBookingRepository;
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

/** Phân công: Bảo — quản lý đăng ký lái thử (admin). */
@Controller
@RequiredArgsConstructor
@RequestMapping("/admin/test-drives")
public class AdminTestDriveController {

    private final TestDriveBookingRepository testDriveBookingRepository;
    private final ContactTestDriveEmailHtmlService contactTestDriveEmailHtmlService;

    @GetMapping
    public String list(@RequestParam(value = "year", required = false) Integer year,
                       @RequestParam(value = "month", required = false) Integer month,
                       @RequestParam(value = "status", required = false) TestDriveStatus status,
                       Model model) {
        List<TestDriveBooking> all = testDriveBookingRepository.findAllWithUserAndCarDetails();
        List<TestDriveBooking> filtered = all.stream()
                .filter(t -> t.getCreatedAt() != null && matchesYearMonthFilter(t.getCreatedAt(), year, month))
                .filter(t -> status == null || t.getStatus() == status)
                .collect(Collectors.toList());

        int currentYear = YearMonth.now().getYear();
        List<Integer> yearOptions = IntStream.rangeClosed(currentYear - 6, currentYear + 1)
                .boxed()
                .collect(Collectors.toList());

        model.addAttribute("bookings", filtered);
        model.addAttribute("filterYear", year);
        model.addAttribute("filterMonth", month);
        model.addAttribute("status", status);
        model.addAttribute("allStatuses", TestDriveStatus.values());
        model.addAttribute("yearOptions", yearOptions);
        return "admin/testdrives/list";
    }

    /** Cùng quy tắc lọc năm/tháng như quản lý đơn / liên hệ. */
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
        TestDriveBooking booking = testDriveBookingRepository.findByIdWithUserAndCarDetails(id)
                .orElseThrow(() -> new IllegalArgumentException("Booking not found"));
        model.addAttribute("booking", booking);
        return "admin/testdrives/detail";
    }

    @PostMapping("/{id}/status")
    @Transactional
    public String updateStatus(@PathVariable Long id,
                               @RequestParam("status") TestDriveStatus newStatus,
                               RedirectAttributes redirectAttributes) {
        TestDriveBooking booking = testDriveBookingRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Booking not found"));

        if (booking.getStatus() == TestDriveStatus.COMPLETED || booking.getStatus() == TestDriveStatus.CANCELED) {
            redirectAttributes.addFlashAttribute("error",
                    "Lịch lái thử đã kết thúc, không thể đổi trạng thái.");
            return "redirect:/admin/test-drives/" + id;
        }

        boolean updated = false;
        if (newStatus == TestDriveStatus.APPROVED && booking.getStatus() == TestDriveStatus.PENDING) {
            booking.setStatus(TestDriveStatus.APPROVED);
            updated = true;
        } else if (newStatus == TestDriveStatus.COMPLETED && booking.getStatus() == TestDriveStatus.APPROVED) {
            booking.setStatus(TestDriveStatus.COMPLETED);
            updated = true;
        } else if (newStatus == TestDriveStatus.CANCELED
                && (booking.getStatus() == TestDriveStatus.PENDING || booking.getStatus() == TestDriveStatus.APPROVED)) {
            booking.setStatus(TestDriveStatus.CANCELED);
            updated = true;
        }

        if (!updated) {
            redirectAttributes.addFlashAttribute("error",
                    "Không thể chuyển sang trạng thái đã chọn từ trạng thái hiện tại.");
            return "redirect:/admin/test-drives/" + id;
        }

        testDriveBookingRepository.save(booking);

        contactTestDriveEmailHtmlService.sendTestDriveBookingStatusUpdated(booking);

        redirectAttributes.addFlashAttribute("success", "Đã cập nhật trạng thái đăng ký lái thử.");
        return "redirect:/admin/test-drives/" + id;
    }
}
