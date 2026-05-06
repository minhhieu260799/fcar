package com.fcar.modules.report.service;

import com.fcar.modules.catalog.entity.CarDefinition;
import java.math.BigDecimal;
import java.util.List;
import lombok.Builder;
import lombok.Value;

/**
 * Phân công: Hiếu — model dữ liệu trang thống kê/báo cáo.
 */
@Value
@Builder
public class ReportDashboardModel {

    int year;
    int month;
    /** Năm chọn trên form (dùng cho biểu đồ theo 12 tháng). */
    int chartYear;

    List<Integer> yearOptions;

    /** Doanh thu: tổng giá xe (totalPrice) các đơn DELIVERED trong tháng. */
    BigDecimal revenueDelivered;

    long countDeposited;
    long countDelivered;
    long countCanceled;
    long countRefunded;

    /** Tổng số tiền hoàn (refundedAmount) các đơn REFUNDED trong tháng. */
    BigDecimal totalRefundedAmount;

    long contactRequestsInMonth;
    long testDriveBookingsInMonth;

    List<TopCarSaleRow> topCars;

    List<String> chartMonthLabels;
    /** Doanh thu (VNĐ) theo từng tháng 1–12 trong {@link #chartYear} — cho Chart.js. */
    List<Double> chartRevenuePoints;
    /** Số đơn DELIVERED theo từng tháng trong {@link #chartYear}. */
    List<Long> chartDeliveredPoints;

    @Value
    @Builder
    public static class TopCarSaleRow {
        CarDefinition definition;
        long soldCount;
    }
}
