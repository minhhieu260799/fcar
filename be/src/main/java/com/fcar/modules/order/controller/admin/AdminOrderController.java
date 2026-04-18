package com.fcar.modules.order.controller.admin;

import com.fcar.modules.catalog.entity.CarDefinition;
import com.fcar.modules.catalog.entity.CarInventory;
import com.fcar.modules.order.entity.CarOrder;
import com.fcar.modules.order.entity.enums.OrderStatus;
import com.fcar.modules.catalog.repository.CarDefinitionRepository;
import com.fcar.modules.catalog.repository.CarInventoryRepository;
import com.fcar.modules.order.repository.CarOrderRepository;
import com.fcar.modules.order.service.CarOrderRefundRules;
import com.fcar.modules.order.service.OrderEmailHtmlService;
import jakarta.transaction.Transactional;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

/** Phân công: Minh — quản lý đơn đặt cọc / mua xe (admin). */
@Controller
@RequiredArgsConstructor
@RequestMapping("/admin/orders")
public class AdminOrderController {

    private final CarOrderRepository carOrderRepository;
    private final CarDefinitionRepository carDefinitionRepository;
    private final CarInventoryRepository carInventoryRepository;
    private final OrderEmailHtmlService orderEmailHtmlService;

    @GetMapping
    public String list(@RequestParam(value = "year", required = false) Integer year,
                       @RequestParam(value = "month", required = false) Integer month,
                       @RequestParam(value = "status", required = false) OrderStatus status,
                       Model model) {
        List<CarOrder> all = carOrderRepository.findAllWithFetchGraph();
        List<CarOrder> filtered = all.stream()
                .filter(o -> o.getCreatedAt() != null && matchesYearMonthFilter(o.getCreatedAt(), year, month))
                .filter(o -> status == null || o.getStatus() == status)
                .collect(Collectors.toList());

        int currentYear = YearMonth.now().getYear();
        List<Integer> yearOptions = IntStream.rangeClosed(currentYear - 6, currentYear + 1)
                .boxed()
                .collect(Collectors.toList());

        model.addAttribute("orders", filtered);
        model.addAttribute("filterYear", year);
        model.addAttribute("filterMonth", month);
        model.addAttribute("status", status);
        model.addAttribute("allStatuses", OrderStatus.values());
        model.addAttribute("yearOptions", yearOptions);
        return "admin/orders/list";
    }

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
        CarOrder order = carOrderRepository.findByIdWithDetailsForAdmin(id)
                .orElseThrow(() -> new IllegalArgumentException("Order not found"));
        Long definitionId = order.getCarInventory().getCarDefinition().getId();
        String coverUrl = carDefinitionRepository.findByIdWithDetailsForStorefront(definitionId)
                .map(CarDefinition::getCoverImageUrl)
                .orElse(null);
        model.addAttribute("title", "Đơn #" + id + " | Admin FCAR");
        model.addAttribute("order", order);
        model.addAttribute("orderCarCoverUrl", coverUrl);
        return "admin/orders/detail";
    }

    /**
     * Trang in chứng từ / hóa đơn nội bộ (chỉ admin). Mở tab mới và Ctrl+P để in.
     */
    @GetMapping("/{id}/invoice")
    public String invoicePrint(@PathVariable Long id, Model model) {
        CarOrder order = carOrderRepository.findByIdWithDetailsForAdmin(id)
                .orElseThrow(() -> new IllegalArgumentException("Order not found"));
        model.addAttribute("order", order);
        model.addAttribute("title", "Chứng từ đơn #" + id + " | Admin FCAR");
        return "admin/orders/invoice-print";
    }

    /**
     * Đánh dấu đã giao xe: DEPOSITED → DELIVERED. Form trên chi tiết đơn POST {@code /admin/orders/{id}/status}.
     */
    @PostMapping("/{id}/status")
    @Transactional
    public String markDelivered(@PathVariable Long id,
                                @RequestParam("status") OrderStatus newStatus,
                                RedirectAttributes redirectAttributes) {
        if (newStatus != OrderStatus.DELIVERED) {
            redirectAttributes.addFlashAttribute("flashError", "Thao tác không hợp lệ.");
            return "redirect:/admin/orders/" + id;
        }
        CarOrder order = carOrderRepository.findByIdWithDetailsForAdmin(id)
                .orElseThrow(() -> new IllegalArgumentException("Order not found"));
        if (order.getStatus() != OrderStatus.DEPOSITED) {
            redirectAttributes.addFlashAttribute("flashError",
                    "Chỉ đánh dấu giao xe khi đơn đang ở trạng thái Đã đặt cọc.");
            return "redirect:/admin/orders/" + id;
        }
        order.setStatus(OrderStatus.DELIVERED);
        carOrderRepository.save(order);
        orderEmailHtmlService.sendDelivered(order);
        redirectAttributes.addFlashAttribute("flashSuccess", "Đã cập nhật: đã giao xe.");
        return "redirect:/admin/orders/" + id;
    }

    /**
     * Admin hủy đơn (DEPOSITED → CANCELED). Không hoàn tồn kho cho đến khi xác nhận REFUNDED.
     */
    @PostMapping("/{id}/cancel")
    @Transactional
    public String cancelByAdmin(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        CarOrder order = carOrderRepository.findByIdWithDetailsForAdmin(id)
                .orElseThrow(() -> new IllegalArgumentException("Order not found"));
        if (order.getStatus() != OrderStatus.DEPOSITED) {
            redirectAttributes.addFlashAttribute("flashError", "Chỉ hủy được đơn đang ở trạng thái Đã đặt cọc.");
            return "redirect:/admin/orders/" + id;
        }
        CarOrderRefundRules.applyCanceled(order);
        carOrderRepository.save(order);
        orderEmailHtmlService.sendAdminCanceled(order);
        redirectAttributes.addFlashAttribute("flashSuccess", "Đã hủy đơn hàng.");
        return "redirect:/admin/orders/" + id;
    }

    /**
     * CANCELED → REFUNDED: bắt buộc có ảnh chứng từ; hoàn +1 tồn kho.
     */
    @PostMapping("/{id}/confirm-refund")
    @Transactional
    public String confirmRefund(@PathVariable Long id,
                                @RequestParam("proof") MultipartFile proofFile,
                                RedirectAttributes redirectAttributes) {
        CarOrder order = carOrderRepository.findByIdWithDetailsForAdmin(id)
                .orElseThrow(() -> new IllegalArgumentException("Order not found"));
        if (order.getStatus() != OrderStatus.CANCELED) {
            redirectAttributes.addFlashAttribute("flashError", "Chỉ xác nhận hoàn tiền khi đơn đang ở trạng thái Đã hủy.");
            return "redirect:/admin/orders/" + id;
        }
        if (proofFile == null || proofFile.isEmpty()) {
            redirectAttributes.addFlashAttribute("flashError", "Vui lòng chọn ảnh chứng từ hoàn tiền.");
            return "redirect:/admin/orders/" + id;
        }
        String ct = proofFile.getContentType();
        if (ct == null || !ct.startsWith("image/")) {
            redirectAttributes.addFlashAttribute("flashError", "Chỉ chấp nhận file ảnh (JPG, PNG, …).");
            return "redirect:/admin/orders/" + id;
        }

        final String url;
        try {
            url = storeRefundProof(proofFile);
        } catch (IOException e) {
            redirectAttributes.addFlashAttribute("flashError", "Không lưu được file. Vui lòng thử lại.");
            return "redirect:/admin/orders/" + id;
        }
        CarInventory car = order.getCarInventory();
        int qty = car.getQuantity() != null ? car.getQuantity() : 0;
        car.setQuantity(qty + 1);
        carInventoryRepository.save(car);

        order.setRefundProofImageUrl(url);
        order.setStatus(OrderStatus.REFUNDED);
        order.setRefundedAmount(CarOrderRefundRules.fullRefundAmount(order));
        carOrderRepository.save(order);

        orderEmailHtmlService.sendRefundCompleted(order);
        redirectAttributes.addFlashAttribute("flashSuccess", "Đã xác nhận hoàn tiền và cập nhật tồn kho.");
        return "redirect:/admin/orders/" + id;
    }

    private String storeRefundProof(MultipartFile file) throws IOException {
        Path uploadRoot = Paths.get("uploads", "refunds").toAbsolutePath().normalize();
        Files.createDirectories(uploadRoot);

        String originalFilename = StringUtils.cleanPath(file.getOriginalFilename());
        String ext = "";
        int dot = originalFilename.lastIndexOf('.');
        if (dot != -1) {
            ext = originalFilename.substring(dot);
        }
        String filename = UUID.randomUUID() + ext;
        Path target = uploadRoot.resolve(filename);
        file.transferTo(target.toFile());
        return "/uploads/refunds/" + filename;
    }

}
