package com.fcar.controller.admin;

import com.fcar.service.report.ReportDashboardModel;
import com.fcar.service.report.ReportService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

/** Phân công: Hiếu — thống kê & báo cáo (admin). */
@Controller
@RequiredArgsConstructor
@RequestMapping("/admin/reports")
public class AdminReportController {

    private final ReportService reportService;

    @GetMapping
    public String dashboard(@RequestParam(value = "year", required = false) Integer year,
                            @RequestParam(value = "month", required = false) Integer month,
                            Model model) {
        ReportDashboardModel report = reportService.buildDashboard(year, month);
        model.addAttribute("report", report);
        model.addAttribute("title", "Thống kê — báo cáo | FCAR Admin");
        return "admin/reports/dashboard";
    }
}
