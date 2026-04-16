package com.fcar.service.payos;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/** Phân công: Minh — ký/verify chữ ký PayOS. */
@Service
@RequiredArgsConstructor
public class PayOsSignatureService {

    private static final char[] HEX = "0123456789abcdef".toCharArray();

    /**
     * Chữ ký tạo link thanh toán (payOS v2): các field sort alphabet.
     * amount=$amount&cancelUrl=$cancelUrl&description=$description&orderCode=$orderCode&returnUrl=$returnUrl
     */
    public String signCreatePaymentLink(
            int amount,
            String cancelUrl,
            String description,
            int orderCode,
            String returnUrl,
            String checksumKey) {
        String data = "amount=" + amount
                + "&cancelUrl=" + cancelUrl
                + "&description=" + description
                + "&orderCode=" + orderCode
                + "&returnUrl=" + returnUrl;
        return hmacSha256Hex(checksumKey, data);
    }

    /**
     * Xác thực webhook: HMAC_SHA256 trên chuỗi key=value, key sort A-Z (theo tài liệu PayOS).
     */
    public boolean verifyWebhookData(ObjectNode data, String expectedSignature, String checksumKey) {
        if (expectedSignature == null || expectedSignature.isBlank()) {
            return false;
        }
        String dataStr = buildSortedKeyValueString(data);
        String computed = hmacSha256Hex(checksumKey, dataStr);
        return computed.equalsIgnoreCase(expectedSignature.trim());
    }

    private static String buildSortedKeyValueString(ObjectNode node) {
        List<String> keys = new ArrayList<>();
        node.fieldNames().forEachRemaining(keys::add);
        Collections.sort(keys);
        StringBuilder sb = new StringBuilder();
        for (String key : keys) {
            JsonNode val = node.get(key);
            String v = jsonNodeToSignatureValue(val);
            if (sb.length() > 0) {
                sb.append('&');
            }
            sb.append(key).append('=').append(v);
        }
        return sb.toString();
    }

    private static String jsonNodeToSignatureValue(JsonNode val) {
        if (val == null || val.isNull() || val.isMissingNode()) {
            return "";
        }
        if (val.isBoolean()) {
            return val.asBoolean() ? "true" : "false";
        }
        if (val.isNumber()) {
            return val.asText();
        }
        if (val.isTextual()) {
            String t = val.asText();
            if ("null".equals(t) || "undefined".equals(t)) {
                return "";
            }
            return t;
        }
        if (val.isArray()) {
            ArrayNode arr = (ArrayNode) val;
            StringBuilder sb = new StringBuilder();
            sb.append('[');
            for (int i = 0; i < arr.size(); i++) {
                if (i > 0) {
                    sb.append(',');
                }
                JsonNode el = arr.get(i);
                if (el != null && el.isObject()) {
                    sb.append(buildSortedKeyValueString((ObjectNode) el));
                } else {
                    sb.append(jsonNodeToSignatureValue(el));
                }
            }
            sb.append(']');
            return sb.toString();
        }
        if (val.isObject()) {
            return buildSortedKeyValueString((ObjectNode) val);
        }
        return val.asText("");
    }

    private static String hmacSha256Hex(String key, String data) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(key.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            byte[] raw = mac.doFinal(data.getBytes(StandardCharsets.UTF_8));
            return toHex(raw);
        } catch (Exception e) {
            throw new IllegalStateException("HMAC-SHA256 failed", e);
        }
    }

    private static String toHex(byte[] bytes) {
        char[] out = new char[bytes.length * 2];
        for (int i = 0; i < bytes.length; i++) {
            int v = bytes[i] & 0xff;
            out[i * 2] = HEX[v >>> 4];
            out[i * 2 + 1] = HEX[v & 0x0f];
        }
        return new String(out);
    }
}
