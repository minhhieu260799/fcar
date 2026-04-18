package com.fcar.modules.payment.service.payos;

import com.fasterxml.jackson.databind.JsonNode;
import com.fcar.modules.payment.config.FcarPayOsProperties;
import com.fcar.modules.catalog.entity.CarDefinition;
import com.fcar.modules.catalog.entity.CarInventory;
import com.fcar.modules.order.entity.CarOrder;
import com.fcar.modules.payment.entity.PayosDepositSession;
import com.fcar.modules.user.entity.User;
import com.fcar.modules.payment.entity.enums.PayosDepositSessionStatus;
import com.fcar.modules.catalog.repository.CarDefinitionRepository;
import com.fcar.modules.payment.repository.PayosDepositSessionRepository;
import com.fcar.modules.user.repository.UserRepository;
import com.fcar.modules.order.service.CarOrderDepositService;
import com.fcar.modules.catalog.service.CarQueryService;
import java.math.BigDecimal;
import java.math.RoundingMode;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestClient;

/** Phân công: Minh — tạo link PayOS, xử lý thanh toán đặt cọc. */

@Service
@RequiredArgsConstructor
public class PayOsDepositService {

    private static final Logger log = LoggerFactory.getLogger(PayOsDepositService.class);

    @Data
    private static class CreatePaymentRequest {
        private int orderCode;
        private int amount;
        private String description;
        private String buyerName;
        private String buyerEmail;
        private String buyerPhone;
        private String cancelUrl;
        private String returnUrl;
        private String signature;
    }

    private final FcarPayOsProperties payOsProperties;
    private final PayOsSignatureService signatureService;
    private final PayosDepositSessionRepository sessionRepository;
    private final CarDefinitionRepository carDefinitionRepository;
    private final CarQueryService carQueryService;
    private final UserRepository userRepository;
    private final CarOrderDepositService carOrderDepositService;
    private final RestClient payOsRestClient;

    public boolean isConfigured() {
        return payOsProperties.isEnabled()
                && !payOsProperties.getClientId().isBlank()
                && !payOsProperties.getApiKey().isBlank()
                && !payOsProperties.getChecksumKey().isBlank();
    }

    /**
     * Tạo phiên PayOS và trả URL chuyển sang trang thanh toán PayOS.
     */
    public String startPayment(User user, Long definitionId, BigDecimal depositAmount) {
        if (!isConfigured()) {
            throw new IllegalStateException("PayOS chưa được cấu hình.");
        }
        CarInventory car = carQueryService.resolveRepresentativeInventory(definitionId)
                .orElseThrow(() -> new IllegalArgumentException("Car not found"));
        if (car.getQuantity() == null || car.getQuantity() <= 0) {
            throw new IllegalStateException("Xe đã hết hàng.");
        }
        CarDefinition def = carDefinitionRepository.findById(definitionId)
                .orElseThrow(() -> new IllegalArgumentException("Definition not found"));

        int orderCode = sessionRepository.nextPayOsOrderCode();
        int amountVnd = depositAmount.setScale(0, RoundingMode.HALF_UP).intValueExact();
        if (amountVnd <= 0) {
            throw new IllegalArgumentException("Số tiền không hợp lệ.");
        }

        String base = payOsProperties.getPublicBaseUrl().replaceAll("/+$", "");
        String cancelUrl = base + "/orders/deposit/" + definitionId;
        String returnUrl = base + "/orders/deposit/payos/return";

        String description = shortDescription(orderCode);
        String signature = signatureService.signCreatePaymentLink(
                amountVnd,
                cancelUrl,
                description,
                orderCode,
                returnUrl,
                payOsProperties.getChecksumKey());

        CreatePaymentRequest req = new CreatePaymentRequest();
        req.setOrderCode(orderCode);
        req.setAmount(amountVnd);
        req.setDescription(description);
        req.setCancelUrl(cancelUrl);
        req.setReturnUrl(returnUrl);
        req.setSignature(signature);
        if (user.getFullName() != null) {
            req.setBuyerName(user.getFullName());
        }
        if (user.getEmail() != null) {
            req.setBuyerEmail(user.getEmail());
        }
        if (user.getPhone() != null) {
            req.setBuyerPhone(user.getPhone());
        }

        JsonNode root = payOsRestClient.post()
                .uri("/v2/payment-requests")
                .header("x-client-id", payOsProperties.getClientId())
                .header("x-api-key", payOsProperties.getApiKey())
                .contentType(MediaType.APPLICATION_JSON)
                .body(req)
                .retrieve()
                .body(JsonNode.class);

        if (root == null || !"00".equals(root.path("code").asText())) {
            String msg = root != null ? root.path("desc").asText("Lỗi PayOS") : "Không có phản hồi từ PayOS";
            throw new IllegalStateException("PayOS: " + msg);
        }
        JsonNode data = root.get("data");
        if (data == null || !data.hasNonNull("checkoutUrl")) {
            throw new IllegalStateException("PayOS: thiếu checkoutUrl trong phản hồi.");
        }
        String checkoutUrl = data.path("checkoutUrl").asText();
        String paymentLinkId = data.path("paymentLinkId").asText(null);

        PayosDepositSession session = new PayosDepositSession();
        session.setPayosOrderCode(orderCode);
        session.setUser(user);
        session.setCarDefinition(def);
        session.setAmount(depositAmount);
        session.setStatus(PayosDepositSessionStatus.PENDING);
        session.setPaymentLinkId(paymentLinkId);
        session.setCheckoutUrl(checkoutUrl);
        sessionRepository.save(session);

        return checkoutUrl;
    }

    private static String shortDescription(int orderCode) {
        String s = Integer.toString(orderCode);
        if (s.length() <= 9) {
            return s;
        }
        return s.substring(0, 9);
    }

    /**
     * Webhook PayOS — tạo đơn khi nhận được thông báo (server phải gọi được từ internet).
     */
    @Transactional
    public void handlePaidWebhook(com.fasterxml.jackson.databind.node.ObjectNode dataNode) {
        if (!isConfigured()) {
            return;
        }
        if (!dataNode.hasNonNull("orderCode") || !dataNode.has("amount")) {
            log.warn("PayOS webhook: thiếu orderCode hoặc amount");
            return;
        }
        int orderCode = dataNode.get("orderCode").asInt();
        int amount = dataNode.get("amount").asInt();

        PayosDepositSession session = sessionRepository.findByPayosOrderCodeForUpdate(orderCode)
                .orElse(null);
        if (session == null) {
            log.warn("PayOS webhook: không tìm thấy phiên orderCode={}", orderCode);
            return;
        }
        completeLockedSessionIfPending(session, amount);
    }

    /**
     * Khi user quay lại từ PayOS: webhook thường không tới localhost — gọi API PayOS để xác nhận đã thanh toán.
     *
     * @param paymentLinkId mã link (query {@code id} PayOS gắn vào returnUrl)
     * @return true nếu đã tạo đơn
     */
    @Transactional
    public boolean tryCompleteAfterReturn(String paymentLinkId, User user) {
        if (!isConfigured()) {
            return false;
        }
        String trimmed = paymentLinkId != null ? paymentLinkId.trim() : "";
        if (trimmed.isEmpty()) {
            PayosDepositSession latestPending = sessionRepository
                    .findTopByUser_IdAndStatusOrderByIdDesc(user.getId(), PayosDepositSessionStatus.PENDING)
                    .orElse(null);
            if (latestPending == null || latestPending.getPaymentLinkId() == null
                    || latestPending.getPaymentLinkId().isBlank()) {
                log.debug("PayOS return: không có id trên URL và không có phiên PENDING có paymentLinkId");
                return false;
            }
            trimmed = latestPending.getPaymentLinkId();
        }

        PayosDepositSession found = sessionRepository.findByPaymentLinkIdAndUser_Id(trimmed, user.getId())
                .orElse(null);
        if (found == null) {
            PayosDepositSession latest = sessionRepository
                    .findTopByUser_IdAndStatusOrderByIdDesc(user.getId(), PayosDepositSessionStatus.PENDING)
                    .orElse(null);
            if (latest != null && trimmed.equals(latest.getPaymentLinkId())) {
                found = latest;
            }
        }
        if (found == null) {
            log.debug("PayOS return: không có phiên PENDING cho paymentLinkId={} user={}", trimmed, user.getId());
            return false;
        }

        PayosDepositSession session = sessionRepository.findByPayosOrderCodeForUpdate(found.getPayosOrderCode())
                .orElse(null);
        if (session == null || session.getStatus() != PayosDepositSessionStatus.PENDING) {
            return session != null && session.getStatus() == PayosDepositSessionStatus.PAID;
        }

        JsonNode root = payOsRestClient.get()
                .uri("/v2/payment-requests/{id}", trimmed)
                .header("x-client-id", payOsProperties.getClientId())
                .header("x-api-key", payOsProperties.getApiKey())
                .retrieve()
                .body(JsonNode.class);

        if (root == null || !"00".equals(root.path("code").asText())) {
            log.warn("PayOS GET payment-requests: code={} desc={}",
                    root != null ? root.path("code").asText() : "null",
                    root != null ? root.path("desc").asText() : "");
            return false;
        }
        JsonNode data = root.get("data");
        if (data == null || !data.has("amount")) {
            return false;
        }
        int amount = data.path("amount").asInt();
        int amountPaid = data.path("amountPaid").asInt(0);
        int amountRemaining = data.path("amountRemaining").asInt(-1);
        String status = data.path("status").asText("");

        if (!isPaidAccordingToApi(amount, amountPaid, amountRemaining, status)) {
            log.info("PayOS return: chưa coi là đã thanh toán — status={} amountPaid={} remaining={}",
                    status, amountPaid, amountRemaining);
            return false;
        }

        return completeLockedSessionIfPending(session, amount);
    }

    /**
     * Theo phản hồi GET /v2/payment-requests: thường có amountPaid / amountRemaining / status.
     */
    private static boolean isPaidAccordingToApi(int amount, int amountPaid, int amountRemaining, String status) {
        String s = status != null ? status.toUpperCase() : "";
        if ("PAID".equals(s) || "SUCCESS".equals(s) || "COMPLETED".equals(s)) {
            return true;
        }
        if (amount <= 0) {
            return false;
        }
        if (amountPaid >= amount) {
            return amountRemaining == 0 || amountRemaining < 0;
        }
        return false;
    }

    private boolean completeLockedSessionIfPending(PayosDepositSession session, int amountVnd) {
        if (session.getStatus() != PayosDepositSessionStatus.PENDING) {
            return session.getStatus() == PayosDepositSessionStatus.PAID;
        }
        BigDecimal expected = session.getAmount().setScale(0, RoundingMode.HALF_UP);
        if (expected.intValueExact() != amountVnd) {
            log.warn("PayOS: số tiền không khớp phiên — expected={} got={}", expected, amountVnd);
            return false;
        }

        User user = userRepository.findById(session.getUser().getId())
                .orElseThrow(() -> new IllegalStateException("User not found"));
        Long definitionId = session.getCarDefinition().getId();
        CarInventory car = carQueryService.resolveRepresentativeInventory(definitionId)
                .orElseThrow(() -> new IllegalStateException("Car not found"));

        CarOrder order = carOrderDepositService.createDepositedOrder(user, car, session.getAmount());
        session.setStatus(PayosDepositSessionStatus.PAID);
        session.setCarOrder(order);
        sessionRepository.save(session);
        log.info("PayOS: đã tạo đơn #{} từ phiên orderCode={}", order.getId(), session.getPayosOrderCode());
        return true;
    }
}
