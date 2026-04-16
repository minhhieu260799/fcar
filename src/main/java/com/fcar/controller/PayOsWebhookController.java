package com.fcar.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fcar.config.FcarPayOsProperties;
import com.fcar.service.payos.PayOsDepositService;
import com.fcar.service.payos.PayOsSignatureService;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

/** Phân công: Minh — webhook PayOS xác nhận thanh toán đặt cọc. */
@RestController
@RequiredArgsConstructor
public class PayOsWebhookController {

    private static final Logger log = LoggerFactory.getLogger(PayOsWebhookController.class);

    private final FcarPayOsProperties payOsProperties;
    private final PayOsSignatureService signatureService;
    private final PayOsDepositService payOsDepositService;

    /**
     * PayOS gọi POST khi có giao dịch; cấu hình URL này trên my.payos.vn (HTTPS khi production).
     */
    @PostMapping("/api/webhooks/payos")
    public ResponseEntity<Void> receive(@RequestBody JsonNode body) {
        if (!payOsProperties.isEnabled()) {
            return ResponseEntity.notFound().build();
        }
        JsonNode dataRaw = body.get("data");
        if (dataRaw == null || !dataRaw.isObject()) {
            return ResponseEntity.badRequest().build();
        }
        ObjectNode data = (ObjectNode) dataRaw;
        String signature = body.path("signature").asText(null);
        if (!signatureService.verifyWebhookData(data, signature, payOsProperties.getChecksumKey())) {
            log.warn("PayOS webhook: chữ ký không hợp lệ (kiểm tra checksum-key trên my.payos.vn)");
            return ResponseEntity.status(401).build();
        }

        if (!"00".equals(body.path("code").asText())) {
            return ResponseEntity.ok().build();
        }
        if (!body.path("success").asBoolean(false)) {
            return ResponseEntity.ok().build();
        }

        payOsDepositService.handlePaidWebhook(data);
        log.debug("PayOS webhook: đã xử lý data orderCode={}", data.path("orderCode").asText());
        return ResponseEntity.ok().build();
    }
}
