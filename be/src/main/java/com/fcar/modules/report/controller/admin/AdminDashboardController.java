package com.fcar.modules.report.controller.admin;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

/** Phân công: Hiếu — trang chủ admin. */
@Controller
@RequestMapping("/admin")
public class AdminDashboardController {

    @GetMapping
    public String index() {
        return "admin/index";
    }
}
