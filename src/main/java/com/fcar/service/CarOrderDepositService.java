package com.fcar.service;

import com.fcar.domain.CarInventory;
import com.fcar.domain.CarOrder;
import com.fcar.domain.User;
import com.fcar.domain.enums.OrderStatus;
import com.fcar.repository.CarInventoryRepository;
import com.fcar.repository.CarOrderRepository;
import java.math.BigDecimal;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Phân công: Minh — xác nhận cọc, tạo đơn sau thanh toán. */
@Service
@RequiredArgsConstructor
public class CarOrderDepositService {

    private final CarInventoryRepository carInventoryRepository;
    private final CarOrderRepository carOrderRepository;
    private final OrderEmailHtmlService orderEmailHtmlService;

    /**
     * Trừ tồn kho, tạo đơn DEPOSITED, gửi email (dùng cho xác nhận thủ công và PayOS webhook).
     */
    @Transactional
    public CarOrder createDepositedOrder(User user, CarInventory car, BigDecimal depositAmount) {
        int qty = car.getQuantity() != null ? car.getQuantity() : 0;
        if (qty <= 0) {
            throw new IllegalStateException("Xe đã hết hàng.");
        }
        car.setQuantity(qty - 1);
        carInventoryRepository.save(car);

        CarOrder order = new CarOrder();
        order.setUser(user);
        order.setCarInventory(car);
        order.setStatus(OrderStatus.DEPOSITED);
        order.setTotalPrice(car.getCarDefinition().getSalePrice());
        order.setDepositAmount(depositAmount);
        order.setPaidAmount(depositAmount);
        carOrderRepository.save(order);

        orderEmailHtmlService.sendDepositSuccess(order);
        return order;
    }
}
