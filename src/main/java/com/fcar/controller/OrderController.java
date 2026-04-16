package com.fcar.controller;

import com.fcar.domain.CarDefinition;
import com.fcar.domain.CarInventory;
import com.fcar.domain.CarOrder;
import com.fcar.domain.PaymentConfig;
import com.fcar.domain.User;
import com.fcar.domain.enums.OrderStatus;
import com.fcar.domain.StoreBankAccount;
import com.fcar.repository.CarDefinitionRepository;
import com.fcar.repository.CarInventoryRepository;
import com.fcar.repository.CarOrderRepository;
import com.fcar.repository.PaymentConfigRepository;
import com.fcar.repository.StoreBankAccountRepository;
import com.fcar.security.AuthenticatedUserResolver;
import com.fcar.security.FcarUserDetails;
import com.fcar.service.CarQueryService;
import com.fcar.service.VietQrImageService;
import com.fcar.service.CarOrderDepositService;
import com.fcar.service.CarOrderRefundRules;
import com.fcar.service.OrderEmailHtmlService;
import com.fcar.service.payos.PayOsDepositService;
import jakarta.transaction.Transactional;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import lombok.Data;
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

    private final CarInventoryRepository carInventoryRepository;
    private final CarDefinitionRepository carDefinitionRepository;
    private final CarOrderRepository carOrderRepository;
    private final PaymentConfigRepository paymentConfigRepository;
    private final AuthenticatedUserResolver authenticatedUserResolver;
    private final CarQueryService carQueryService;
    private final OrderEmailHtmlService orderEmailHtmlService;
    private final StoreBankAccountRepository storeBankAccountRepository;
    private final PayOsDepositService payOsDepositService;
    private final CarOrderDepositService carOrderDepositService;

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
                    "Ồ rất tiếc! xe này đã có khách hàng đặt thành công. Bạn vui lòng liên hệ với chúng tôi");
            return "orders/error";
        }

        PaymentConfig config = paymentConfigRepository.findAll().stream().findFirst()
                .orElseThrow(() -> new IllegalStateException("Chưa cấu hình thanh toán"));

        BigDecimal depositAmount = computeDepositAmount(car, config);

        PaymentViewModel vm = new PaymentViewModel();
        vm.setDefinitionId(definitionId);
        vm.setAmount(depositAmount);
        vm.setDescription("Bạn sẽ đặt cọc giữ xe với "
                + config.getDepositPercent() + "% giá trị tiêu chuẩn của xe.");

        List<StoreBankAccount> activeAccounts = storeBankAccountRepository.findByActiveTrueOrderByIdAsc();
        Optional<StoreBankAccount> bankForDisplay = activeAccounts.stream().findFirst();

        String transferContent = VietQrImageService.sanitizeVietQrText(
                "FCAR coc D" + definitionId + " U" + user.getId(), 50);

        model.addAttribute("car", car);
        model.addAttribute("payment", vm);
        model.addAttribute("depositBankAccount", bankForDisplay.orElse(null));
        model.addAttribute("depositTransferContent", transferContent);
        model.addAttribute("payosConfigured", payOsDepositService.isConfigured());
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
                "Nếu đã thanh toán mà chưa thấy đơn: kiểm tra lại sau vài giây (webhook), hoặc dùng nút xác nhận chuyển khoản thủ công trên trang đặt cọc.");
        return "redirect:/account/history";
    }

    @PostMapping("/deposit/confirm")
    @Transactional
    public String confirmDeposit(@ModelAttribute("payment") PaymentViewModel vm,
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
        Long definitionId = vm.getDefinitionId();
        if (definitionId == null || vm.getAmount() == null) {
            redirectAttributes.addFlashAttribute("flashError", "Thiếu thông tin thanh toán.");
            return "redirect:/account/history";
        }

        CarInventory car = carQueryService.resolveRepresentativeInventory(definitionId)
                .orElseThrow(() -> new IllegalArgumentException("Car not found"));

        if (car.getQuantity() == null || car.getQuantity() <= 0) {
            redirectAttributes.addFlashAttribute("flashError", "Xe đã hết hàng.");
            return "redirect:/cars/" + definitionId;
        }

        PaymentConfig config = paymentConfigRepository.findAll().stream().findFirst()
                .orElseThrow(() -> new IllegalStateException("Chưa cấu hình thanh toán"));

        BigDecimal expectedDeposit = computeDepositAmount(car, config);
        if (expectedDeposit.compareTo(vm.getAmount()) != 0) {
            redirectAttributes.addFlashAttribute("flashError",
                    "Số tiền không khớp. Vui lòng tải lại trang đặt cọc.");
            return "redirect:/orders/deposit/" + definitionId;
        }

        carOrderDepositService.createDepositedOrder(user, car, expectedDeposit);
        authenticatedUserResolver.refreshAuthenticatedPrincipal(user, request, response);
        redirectAttributes.addFlashAttribute("flashSuccess", "Đặt cọc thành công.");
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

    @Data
    public static class PaymentViewModel {
        @NotNull
        private Long definitionId;

        @NotNull
        private BigDecimal amount;

        private String description;
    }

}
