package com.fcar.modules.order.controller;

import com.fcar.modules.catalog.entity.CarDefinition;
import com.fcar.modules.catalog.entity.CarInventory;
import com.fcar.modules.order.entity.CarOrder;
import com.fcar.modules.payment.entity.PaymentConfig;
import com.fcar.modules.user.entity.User;
import com.fcar.modules.order.entity.enums.OrderStatus;
import com.fcar.modules.catalog.repository.CarDefinitionRepository;
import com.fcar.modules.order.repository.CarOrderRepository;
import com.fcar.modules.payment.repository.PaymentConfigRepository;
import com.fcar.modules.user.security.AuthenticatedUserResolver;
import com.fcar.modules.user.security.FcarUserDetails;
import com.fcar.modules.catalog.service.CarQueryService;
import com.fcar.modules.order.service.CarOrderRefundRules;
import com.fcar.modules.order.service.OrderEmailHtmlService;
import com.fcar.modules.payment.service.payos.PayOsDepositService;
import jakarta.transaction.Transactional;
import java.math.BigDecimal;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/** Phân công: Minh — đặt cọc mua xe, thanh toán cọc, chi tiết đơn, hóa đơn. */
@Controller
@RequiredArgsConstructor
@RequestMapping("/orders")
public class OrderController {

    private final CarDefinitionRepository carDefinitionRepository;
    private final CarOrderRepository carOrderRepository;
    private final PaymentConfigRepository paymentConfigRepository;
    private final AuthenticatedUserResolver authenticatedUserResolver;
    private final CarQueryService carQueryService;
    private final OrderEmailHtmlService orderEmailHtmlService;
    private final PayOsDepositService payOsDepositService;

    @GetMapping("/buy/{definitionId}")
    public String choosePurchaseType(@PathVariable("definitionId") Long definitionId,
                                     @AuthenticationPrincipal FcarUserDetails principal) {
        if (principal == null) {
            return "redirect:/auth/login?redirect=/orders/deposit/" + definitionId;
        }
        return "redirect:/orders/deposit/" + definitionId;
    }

    @GetMapping("/deposit/{definitionId}")
    public String showDepositPage(@PathVariable("definitionId") Long definitionId,
                                  @AuthenticationPrincipal FcarUserDetails principal,
                                  RedirectAttributes redirectAttributes,
                                  HttpServletRequest request,
                                  HttpServletResponse response,
                                  Model model) {
        if (principal == null) {
            return "redirect:/auth/login?redirect=/orders/deposit/" + definitionId;
        }
        User user;
        try {
            user = authenticatedUserResolver.resolve(principal);
            authenticatedUserResolver.refreshAuthenticatedPrincipal(user, request, response);
        } catch (IllegalStateException ex) {
            redirectAttributes.addFlashAttribute("flashError", ex.getMessage());
            return "redirect:/auth/login";
        }

        CarInventory car = carQueryService.resolveRepresentativeInventory(definitionId)
                .orElseThrow(() -> new IllegalArgumentException("Car not found"));

        if (car.getQuantity() == null || car.getQuantity() <= 0) {
            model.addAttribute("message",
                    "Rất tiếc, mẫu xe này đã có khách đặt thành công. Vui lòng liên hệ với chúng tôi.");
            return "orders/error";
        }

        PaymentConfig config = paymentConfigRepository.findAll().stream().findFirst()
                .orElseThrow(() -> new IllegalStateException("Chưa cấu hình thanh toán"));

        BigDecimal depositAmount = computeDepositAmount(car, config);

        if (!payOsDepositService.isConfigured()) {
            redirectAttributes.addFlashAttribute("flashError",
                    "Hệ thống chưa cấu hình PayOS, tạm thời chưa hỗ trợ đặt cọc.");
            return "redirect:/cars/" + definitionId;
        }

        model.addAttribute("car", car);
        model.addAttribute("definitionId", definitionId);
        model.addAttribute("depositAmount", depositAmount);
        model.addAttribute("depositDescription", "Bạn sẽ đặt cọc giữ xe với "
                + config.getDepositPercent() + "% giá trị tiêu chuẩn của xe.");
        return "orders/deposit";
    }

    /**
     * Chuyển sang trang thanh toán PayOS (tự động tạo đơn khi PayOS gọi webhook sau khi thành công).
     */
    @PostMapping("/deposit/payos/start")
    public String startPayOsDeposit(@RequestParam("definitionId") Long definitionId,
                                    @AuthenticationPrincipal FcarUserDetails principal,
                                    RedirectAttributes redirectAttributes,
                                    HttpServletRequest request,
                                    HttpServletResponse response) {
        if (principal == null) {
            return "redirect:/auth/login?redirect=/orders/deposit/" + definitionId;
        }
        User user;
        try {
            user = authenticatedUserResolver.resolve(principal);
            authenticatedUserResolver.refreshAuthenticatedPrincipal(user, request, response);
        } catch (IllegalStateException ex) {
            redirectAttributes.addFlashAttribute("flashError", ex.getMessage());
            return "redirect:/auth/login";
        }
        try {
            if (!payOsDepositService.isConfigured()) {
                throw new IllegalStateException("Hệ thống chưa cấu hình PayOS.");
            }
            PaymentConfig config = paymentConfigRepository.findAll().stream().findFirst()
                    .orElseThrow(() -> new IllegalStateException("Chưa cấu hình thanh toán"));
            CarInventory car = carQueryService.resolveRepresentativeInventory(definitionId)
                    .orElseThrow(() -> new IllegalArgumentException("Car not found"));
            if (car.getQuantity() == null || car.getQuantity() <= 0) {
                redirectAttributes.addFlashAttribute("flashError", "Xe đã hết hàng.");
                return "redirect:/cars/" + definitionId;
            }
            BigDecimal depositAmount = computeDepositAmount(car, config);
            String checkoutUrl = payOsDepositService.startPayment(user, definitionId, depositAmount);
            return "redirect:" + checkoutUrl;
        } catch (IllegalStateException | IllegalArgumentException ex) {
            redirectAttributes.addFlashAttribute("flashError", ex.getMessage());
            return "redirect:/orders/deposit/" + definitionId;
        }
    }

    @GetMapping("/deposit/payos/return")
    public String payOsReturn(@RequestParam(value = "id", required = false) String paymentLinkIdFromQuery,
                              @RequestParam(value = "paymentLinkId", required = false) String paymentLinkIdAlt,
                              @AuthenticationPrincipal FcarUserDetails principal,
                              RedirectAttributes redirectAttributes,
                              HttpServletRequest request,
                              HttpServletResponse response) {
        if (principal == null) {
            return "redirect:/auth/login";
        }
        User user;
        try {
            user = authenticatedUserResolver.resolve(principal);
        } catch (IllegalStateException ex) {
            redirectAttributes.addFlashAttribute("flashError", ex.getMessage());
            return "redirect:/auth/login";
        }
        String linkId = paymentLinkIdFromQuery != null && !paymentLinkIdFromQuery.isBlank()
                ? paymentLinkIdFromQuery
                : paymentLinkIdAlt;

        if (payOsDepositService.isConfigured()) {
            boolean done = payOsDepositService.tryCompleteAfterReturn(linkId, user);
            authenticatedUserResolver.refreshAuthenticatedPrincipal(user, request, response);
            if (done) {
                redirectAttributes.addFlashAttribute("flashSuccess", "Đặt cọc qua PayOS thành công.");
                return "redirect:/account/history";
            }
        }
        redirectAttributes.addFlashAttribute("flashSuccess",
                "Nếu đã thanh toán mà chưa thấy đơn: kiểm tra lại sau vài giây (webhook).");
        return "redirect:/account/history";
    }

    /**
     * Khách hủy đơn (DEPOSITED → CANCELED). Tồn kho chỉ hoàn khi admin xác nhận REFUNDED sau này.
     */
    @PostMapping("/{orderId}/cancel")
    @Transactional
    public String cancelOrderByCustomer(@PathVariable("orderId") Long orderId,
                                        @AuthenticationPrincipal FcarUserDetails principal,
                                        RedirectAttributes redirectAttributes,
                                        HttpServletRequest request,
                                        HttpServletResponse response) {
        if (principal == null) {
            return "redirect:/auth/login?redirect=/orders/detail/" + orderId;
        }
        User user;
        try {
            user = authenticatedUserResolver.resolve(principal);
        } catch (IllegalStateException ex) {
            redirectAttributes.addFlashAttribute("flashError", ex.getMessage());
            return "redirect:/auth/login";
        }
        CarOrder order = carOrderRepository.findByIdAndUserWithCarDetails(orderId, user)
                .orElseThrow(() -> new IllegalArgumentException("Order not found"));
        if (order.getStatus() != OrderStatus.DEPOSITED) {
            redirectAttributes.addFlashAttribute("flashError", "Không thể hủy đơn ở trạng thái hiện tại.");
            return "redirect:/orders/detail/" + orderId;
        }
        CarOrderRefundRules.applyCanceled(order);
        carOrderRepository.save(order);
        orderEmailHtmlService.sendCustomerCanceled(order);
        authenticatedUserResolver.refreshAuthenticatedPrincipal(user, request, response);
        redirectAttributes.addFlashAttribute("flashSuccess", "Đã hủy đơn hàng.");
        return "redirect:/orders/detail/" + orderId;
    }

    @GetMapping("/detail/{orderId}")
    public String orderDetailForCustomer(@PathVariable("orderId") Long orderId,
                                         @AuthenticationPrincipal FcarUserDetails principal,
                                         RedirectAttributes redirectAttributes,
                                         Model model) {
        if (principal == null) {
            return "redirect:/auth/login?redirect=/orders/detail/" + orderId;
        }
        User user;
        try {
            user = authenticatedUserResolver.resolve(principal);
        } catch (IllegalStateException ex) {
            redirectAttributes.addFlashAttribute("flashError", ex.getMessage());
            return "redirect:/auth/login";
        }
        return carOrderRepository.findByIdAndUserWithCarDetails(orderId, user)
                .map(order -> {
                    Long definitionId = order.getCarInventory().getCarDefinition().getId();
                    String coverUrl = carDefinitionRepository.findByIdWithDetailsForStorefront(definitionId)
                            .map(CarDefinition::getCoverImageUrl)
                            .orElse(null);
                    model.addAttribute("title", "Đơn hàng #" + orderId + " | FCAR");
                    model.addAttribute("order", order);
                    model.addAttribute("orderCarCoverUrl", coverUrl);
                    return "orders/order-detail";
                })
                .orElseGet(() -> {
                    redirectAttributes.addFlashAttribute("flashError", "Không tìm thấy đơn hàng hoặc bạn không có quyền xem.");
                    return "redirect:/account/history";
                });
    }

    @GetMapping("/{orderId}/invoice")
    public String invoice(@PathVariable("orderId") Long orderId,
                          @AuthenticationPrincipal FcarUserDetails principal,
                          RedirectAttributes redirectAttributes) {
        if (principal == null) {
            return "redirect:/auth/login";
        }
        redirectAttributes.addFlashAttribute("flashError",
                "In hóa đơn không khả dụng trên tài khoản khách hàng. Vui lòng liên hệ showroom nếu cần chứng từ.");
        return "redirect:/account/history";
    }

    private static BigDecimal computeDepositAmount(CarInventory car, PaymentConfig config) {
        BigDecimal depositPercent = config.getDepositPercent().divide(BigDecimal.valueOf(100));
        return car.getCarDefinition().getSalePrice().multiply(depositPercent);
    }
}
