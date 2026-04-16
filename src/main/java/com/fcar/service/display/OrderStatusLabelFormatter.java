package com.fcar.service.display;

import com.fcar.domain.enums.OrderStatus;
import org.springframework.stereotype.Component;

/**
 * Phân công: Minh — nhãn tiếng Việt trạng thái đơn mua xe (Thymeleaf/UI).
 */
@Component
public class OrderStatusLabelFormatter {

    public String format(OrderStatus status) {
        if (status == null) {
            return "—";
        }
        return switch (status) {
            case DEPOSITED -> "Đã đặt cọc";
            case DELIVERED -> "Đã giao xe";
            case CANCELED -> "Đã hủy";
            case REFUNDED -> "Đã hoàn tiền";
        };
    }
}
