package com.fcar.modules.order.service;

import com.fcar.modules.order.entity.CarOrder;
import com.fcar.modules.order.entity.enums.OrderStatus;
import java.math.BigDecimal;

/**
 * Phân công: Minh — quy tắc hoàn tiền đơn (số tiền hoàn theo cọc/đã thanh toán).
 */
public final class CarOrderRefundRules {

    private CarOrderRefundRules() {
    }

    public static BigDecimal fullRefundAmount(CarOrder order) {
        if (order.getDepositAmount() != null) {
            return order.getDepositAmount();
        }
        if (order.getPaidAmount() != null) {
            return order.getPaidAmount();
        }
        return BigDecimal.ZERO;
    }

    /** Chuyển đơn DEPOSITED → CANCELED: ghi số hoàn dự kiến = tiền cọc, không đụng tồn kho. */
    public static void applyCanceled(CarOrder order) {
        order.setStatus(OrderStatus.CANCELED);
        order.setRefundedAmount(fullRefundAmount(order));
        order.setRefundMethod(null);
        order.setRefundBankInfo(null);
        order.setRefundProofImageUrl(null);
    }
}
