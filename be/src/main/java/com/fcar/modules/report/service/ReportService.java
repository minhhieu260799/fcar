package com.fcar.modules.report.service;

import com.fcar.modules.catalog.entity.CarDefinition;
import com.fcar.modules.order.entity.enums.OrderStatus;
import com.fcar.modules.catalog.repository.CarDefinitionRepository;
import com.fcar.modules.order.repository.CarOrderRepository;
import com.fcar.modules.contact.repository.ContactRequestRepository;
import com.fcar.modules.testdrive.repository.TestDriveBookingRepository;
import com.fcar.modules.report.service.ReportDashboardModel.TopCarSaleRow;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Phân công: Hiếu — dữ liệu thống kê/báo cáo dashboard. */
@Service
@RequiredArgsConstructor
public class ReportService {

    private static final int MIN_YEAR = 2020;
    private static final int MAX_YEAR_OFFSET = 1;

    private final CarOrderRepository carOrderRepository;
    private final ContactRequestRepository contactRequestRepository;
    private final TestDriveBookingRepository testDriveBookingRepository;
    private final CarDefinitionRepository carDefinitionRepository;

    @Transactional(readOnly = true)
    public ReportDashboardModel buildDashboard(Integer yearParam, Integer monthParam) {
        int year = clampYear(yearParam);
        int month = clampMonth(monthParam);
        int chartYear = year;

        LocalDateTime monthStart = YearMonth.of(year, month).atDay(1).atStartOfDay();
        LocalDateTime monthEnd = YearMonth.of(year, month).atEndOfMonth().atTime(23, 59, 59);

        BigDecimal revenue = carOrderRepository.sumTotalPriceByStatusAndCreatedAtBetween(
                OrderStatus.DELIVERED, monthStart, monthEnd);

        long deposited = carOrderRepository.countByStatusAndCreatedAtBetween(
                OrderStatus.DEPOSITED, monthStart, monthEnd);
        long delivered = carOrderRepository.countByStatusAndCreatedAtBetween(
                OrderStatus.DELIVERED, monthStart, monthEnd);
        long canceled = carOrderRepository.countByStatusAndCreatedAtBetween(
                OrderStatus.CANCELED, monthStart, monthEnd);
        long refunded = carOrderRepository.countByStatusAndCreatedAtBetween(
                OrderStatus.REFUNDED, monthStart, monthEnd);

        BigDecimal totalRefunded = carOrderRepository.sumRefundedAmountByStatusAndCreatedAtBetween(
                OrderStatus.REFUNDED, monthStart, monthEnd);

        long contacts = contactRequestRepository.countByCreatedAtBetween(monthStart, monthEnd);
        long testDrives = testDriveBookingRepository.countByCreatedAtBetween(monthStart, monthEnd);

        List<TopCarSaleRow> topCars = loadTopCarDefinitions(monthStart, monthEnd);

        List<String> chartLabels = IntStream.rangeClosed(1, 12)
                .mapToObj(m -> "T" + m)
                .collect(Collectors.toList());

        List<BigDecimal> monthlyRevenue = new ArrayList<>(12);
        List<Long> monthlyDelivered = new ArrayList<>(12);
        for (int m = 1; m <= 12; m++) {
            LocalDateTime start = YearMonth.of(chartYear, m).atDay(1).atStartOfDay();
            LocalDateTime end = YearMonth.of(chartYear, m).atEndOfMonth().atTime(23, 59, 59);
            monthlyRevenue.add(carOrderRepository.sumTotalPriceByStatusAndCreatedAtBetween(
                    OrderStatus.DELIVERED, start, end));
            monthlyDelivered.add(carOrderRepository.countByStatusAndCreatedAtBetween(
                    OrderStatus.DELIVERED, start, end));
        }
        List<Double> revenuePoints = monthlyRevenue.stream()
                .map(b -> b != null ? b.doubleValue() : 0d)
                .collect(Collectors.toList());

        int currentYear = YearMonth.now().getYear();
        List<Integer> yearOptions = IntStream.rangeClosed(MIN_YEAR, currentYear + MAX_YEAR_OFFSET)
                .boxed()
                .collect(Collectors.toList());

        return ReportDashboardModel.builder()
                .year(year)
                .month(month)
                .chartYear(chartYear)
                .yearOptions(yearOptions)
                .revenueDelivered(revenue != null ? revenue : BigDecimal.ZERO)
                .countDeposited(deposited)
                .countDelivered(delivered)
                .countCanceled(canceled)
                .countRefunded(refunded)
                .totalRefundedAmount(totalRefunded != null ? totalRefunded : BigDecimal.ZERO)
                .contactRequestsInMonth(contacts)
                .testDriveBookingsInMonth(testDrives)
                .topCars(topCars)
                .chartMonthLabels(chartLabels)
                .chartRevenuePoints(revenuePoints)
                .chartDeliveredPoints(monthlyDelivered)
                .build();
    }

    private List<TopCarSaleRow> loadTopCarDefinitions(LocalDateTime monthStart, LocalDateTime monthEnd) {
        List<Object[]> rows = carOrderRepository.countDeliveredByDefinitionId(
                OrderStatus.DELIVERED, monthStart, monthEnd, PageRequest.of(0, 5));
        if (rows.isEmpty()) {
            return List.of();
        }
        List<Long> ids = rows.stream()
                .map(r -> (Long) r[0])
                .filter(Objects::nonNull)
                .toList();
        List<CarDefinition> defs = carDefinitionRepository.findByIdsWithBrandModelSegment(ids);
        Map<Long, CarDefinition> byId = defs.stream()
                .collect(Collectors.toMap(CarDefinition::getId, d -> d, (a, b) -> a, LinkedHashMap::new));
        List<TopCarSaleRow> out = new ArrayList<>();
        for (Object[] row : rows) {
            Long id = (Long) row[0];
            long cnt = row[1] instanceof Long l ? l : ((Number) row[1]).longValue();
            CarDefinition def = byId.get(id);
            if (def != null) {
                out.add(TopCarSaleRow.builder().definition(def).soldCount(cnt).build());
            }
        }
        return out;
    }

    private static int clampYear(Integer y) {
        int current = YearMonth.now().getYear();
        if (y == null) {
            return current;
        }
        return Math.min(Math.max(y, MIN_YEAR), current + MAX_YEAR_OFFSET);
    }

    private static int clampMonth(Integer m) {
        if (m == null) {
            return YearMonth.now().getMonthValue();
        }
        return Math.min(Math.max(m, 1), 12);
    }
}
