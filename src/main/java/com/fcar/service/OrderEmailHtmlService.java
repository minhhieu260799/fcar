package com.fcar.service;

import com.fcar.domain.CarOrder;
import com.fcar.domain.User;
import com.fcar.service.email.CustomerEmailHtmlShell;
import com.fcar.service.display.CarDefinitionLabelFormatter;
import java.math.BigDecimal;
import java.text.NumberFormat;
import java.util.Locale;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.util.HtmlUtils;

/**
 * Phân công: Minh — email HTML các sự kiện đơn hàng (gửi khách).
 */
@Service
@RequiredArgsConstructor
public class OrderEmailHtmlService {

    private final MailNotificationService mailNotificationService;
    private final CarDefinitionLabelFormatter carDefinitionLabelFormatter;

    private static String money(BigDecimal v) {
        if (v == null) {
            return "—";
        }
        NumberFormat nf = NumberFormat.getNumberInstance(Locale.forLanguageTag("vi-VN"));
        return nf.format(v) + " đ";
    }

    private static String esc(String s) {
        if (s == null) {
            return "";
        }
        return HtmlUtils.htmlEscape(s);
    }

    private String carLine(CarOrder order) {
        if (order.getCarInventory() == null || order.getCarInventory().getCarDefinition() == null) {
            return "—";
        }
        return esc(carDefinitionLabelFormatter.format(order.getCarInventory().getCarDefinition()));
    }

    private String orderSummaryBlock(CarOrder order) {
        String khach = order.getUser() != null ? esc(order.getUser().getFullName()) : "—";
        String email = order.getUser() != null ? esc(order.getUser().getEmail()) : "—";
        String sdt = order.getUser() != null && order.getUser().getPhone() != null
                ? esc(order.getUser().getPhone()) : "—";
        return """
                <table role="presentation" width="100%%" style="border-collapse:collapse;background:#f8fafc;border-radius:10px;margin:16px 0;">
                <tr><td style="padding:16px 18px;">
                <table role="presentation" width="100%%" style="font-size:14px;">
                <tr><td style="color:#64748b;padding:4px 0;width:38%%;">Mã đơn</td><td style="font-weight:600;">#%d</td></tr>
                <tr><td style="color:#64748b;padding:4px 0;">Khách hàng</td><td>%s</td></tr>
                <tr><td style="color:#64748b;padding:4px 0;">Email</td><td>%s</td></tr>
                <tr><td style="color:#64748b;padding:4px 0;">Điện thoại</td><td>%s</td></tr>
                <tr><td style="color:#64748b;padding:4px 0;">Xe</td><td style="font-weight:500;">%s</td></tr>
                <tr><td style="color:#64748b;padding:4px 0;">Giá niêm yết</td><td>%s</td></tr>
                <tr><td style="color:#64748b;padding:4px 0;">Tiền đặt cọc</td><td style="font-weight:600;color:#0d6efd;">%s</td></tr>
                </table>
                </td></tr>
                </table>
                """.formatted(
                order.getId(),
                khach,
                email,
                sdt,
                carLine(order),
                money(order.getTotalPrice()),
                money(order.getDepositAmount() != null ? order.getDepositAmount() : order.getPaidAmount()));
    }

    public void sendDepositSuccess(CarOrder order) {
        User u = order.getUser();
        if (u == null || u.getEmail() == null || u.getEmail().isBlank()) {
            return;
        }
        String inner = orderSummaryBlock(order)
                + "<p style=\"margin:16px 0 0;\">Cảm ơn bạn đã tin tưởng FCAR. Chúng tôi đã <strong>ghi nhận khoản đặt cọc</strong> và giữ xe theo chính sách hiện hành. "
                + "Nhân viên showroom sẽ liên hệ để xác nhận và hướng dẫn các bước tiếp theo (giao xe, thủ tục).</p>"
                + "<p style=\"margin:12px 0 0;\">Bạn có thể xem lại chi tiết đơn bất cứ lúc nào trong mục <strong>Lịch sử</strong> trên tài khoản.</p>";
        String html = CustomerEmailHtmlShell.wrap("#0d6efd", "ĐÃ ĐẶT CỌC", "Đặt cọc giữ xe thành công", inner);
        mailNotificationService.sendHtml(u.getEmail(),
                "FCAR — Đặt cọc thành công · Đơn #" + order.getId(),
                html);
    }

    public void sendCustomerCanceled(CarOrder order) {
        User u = order.getUser();
        if (u == null || u.getEmail() == null || u.getEmail().isBlank()) {
            return;
        }
        String inner = orderSummaryBlock(order)
                + "<p style=\"margin:16px 0 0;\">Bạn vừa <strong>hủy đơn hàng</strong> này trên hệ thống. Số tiền hoàn cho bạn là <strong>toàn bộ số tiền đã đặt cọc</strong> ("
                + money(CarOrderRefundRules.fullRefundAmount(order)) + ").</p>"
                + "<p style=\"margin:12px 0 0;\"><strong>Bước tiếp theo:</strong> vui lòng chờ nhân viên FCAR liên hệ (điện thoại/email) để xác nhận thông tin nhận hoàn và thời gian xử lý. "
                + "Khi showroom đã chuyển khoản hoàn tất, trạng thái đơn sẽ được cập nhật tương ứng.</p>";
        String html = CustomerEmailHtmlShell.wrap("#dc2626", "ĐÃ HỦY", "Xác nhận hủy đơn hàng", inner);
        mailNotificationService.sendHtml(u.getEmail(),
                "FCAR — Đơn #" + order.getId() + " đã được hủy",
                html);
    }

    public void sendDelivered(CarOrder order) {
        User u = order.getUser();
        if (u == null || u.getEmail() == null || u.getEmail().isBlank()) {
            return;
        }
        String inner = orderSummaryBlock(order)
                + "<p style=\"margin:16px 0 0;\">Showroom đã <strong>hoàn tất giao xe</strong> cho đơn hàng của bạn theo thông tin đã thống nhất.</p>"
                + "<p style=\"margin:12px 0 0;\">Cảm ơn bạn đã lựa chọn FCAR. Chúc bạn an toàn trên mọi hành trình. "
                + "Mọi thắc mắc sau giao xe, đừng ngại liên hệ bộ phận chăm sóc khách hàng.</p>";
        String html = CustomerEmailHtmlShell.wrap("#059669", "ĐÃ GIAO XE", "Đã giao xe — cảm ơn bạn!", inner);
        mailNotificationService.sendHtml(u.getEmail(),
                "FCAR — Đơn #" + order.getId() + " · Đã giao xe",
                html);
    }

    public void sendAdminCanceled(CarOrder order) {
        User u = order.getUser();
        if (u == null || u.getEmail() == null || u.getEmail().isBlank()) {
            return;
        }
        String inner = orderSummaryBlock(order)
                + "<p style=\"margin:16px 0 0;\">Đơn hàng của bạn đã được <strong>điều chỉnh sang trạng thái hủy</strong> từ phía showroom (ví dụ: thay đổi kế hoạch, thỏa thuận với khách).</p>"
                + "<p style=\"margin:12px 0 0;\">Số tiền hoàn: <strong>" + money(CarOrderRefundRules.fullRefundAmount(order))
                + "</strong> (toàn bộ tiền đặt cọc). Vui lòng <strong>liên hệ FCAR</strong> để được hướng dẫn nhận hoàn tiền và thời gian xử lý.</p>";
        String html = CustomerEmailHtmlShell.wrap("#b45309", "ĐÃ HỦY (SHOWROOM)", "Thông báo hủy đơn hàng", inner);
        mailNotificationService.sendHtml(u.getEmail(),
                "FCAR — Đơn #" + order.getId() + " · Đơn đã được hủy",
                html);
    }

    public void sendRefundCompleted(CarOrder order) {
        User u = order.getUser();
        if (u == null || u.getEmail() == null || u.getEmail().isBlank()) {
            return;
        }
        String inner = orderSummaryBlock(order)
                + "<p style=\"margin:16px 0 0;\">FCAR đã <strong>hoàn tất chuyển khoản hoàn tiền đặt cọc</strong> cho đơn hàng của bạn.</p>"
                + "<p style=\"margin:12px 0 0;\">Số tiền đã hoàn: <strong style=\"color:#059669;\">"
                + money(order.getRefundedAmount()) + "</strong>. "
                + "Giao dịch đơn hàng này được coi là <strong>đã kết thúc</strong>.</p>"
                + "<p style=\"margin:12px 0 0;\">Cảm ơn bạn đã tìm hiểu và sử dụng dịch vụ FCAR. Rất mong được phục vụ bạn trong tương lai.</p>";
        String html = CustomerEmailHtmlShell.wrap("#2563eb", "ĐÃ HOÀN TIỀN", "Xác nhận hoàn tiền đặt cọc", inner);
        mailNotificationService.sendHtml(u.getEmail(),
                "FCAR — Đơn #" + order.getId() + " · Đã hoàn tiền",
                html);
    }
}
