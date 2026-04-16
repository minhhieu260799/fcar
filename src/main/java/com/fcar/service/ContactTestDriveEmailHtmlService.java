package com.fcar.service;

import com.fcar.domain.Branch;
import com.fcar.domain.CarInventory;
import com.fcar.domain.ContactRequest;
import com.fcar.domain.TestDriveBooking;
import com.fcar.domain.User;
import com.fcar.domain.enums.ContactStatus;
import com.fcar.service.display.CarDefinitionLabelFormatter;
import com.fcar.service.email.CustomerEmailHtmlShell;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.util.HtmlUtils;

/**
 * Phân công: Bảo — email HTML liên hệ tư vấn & đăng ký lái thử (gửi khách).
 */
@Service
@RequiredArgsConstructor
public class ContactTestDriveEmailHtmlService {

    private static final DateTimeFormatter DT_DISPLAY =
            DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    private final MailNotificationService mailNotificationService;
    private final CarDefinitionLabelFormatter carDefinitionLabelFormatter;

    private static String esc(String s) {
        if (s == null) {
            return "";
        }
        return HtmlUtils.htmlEscape(s);
    }

    private static String formatDt(LocalDateTime t) {
        if (t == null) {
            return "—";
        }
        return esc(t.format(DT_DISPLAY));
    }

    private String carLine(CarInventory inv) {
        if (inv == null || inv.getCarDefinition() == null) {
            return "—";
        }
        return esc(carDefinitionLabelFormatter.format(inv.getCarDefinition()));
    }

    private String branchLine(Branch b) {
        if (b == null || b.getName() == null || b.getName().isBlank()) {
            return "—";
        }
        return esc(b.getName());
    }

    private String contactSummaryRows(ContactRequest c) {
        User u = c.getUser();
        String khach = u != null ? esc(u.getFullName()) : "—";
        String email = u != null ? esc(u.getEmail()) : "—";
        String sdt = u != null && u.getPhone() != null ? esc(u.getPhone()) : "—";
        return """
                <tr><td style="color:#64748b;padding:4px 0;width:38%%;">Mã yêu cầu</td><td style="font-weight:600;">#%d</td></tr>
                <tr><td style="color:#64748b;padding:4px 0;">Khách hàng</td><td>%s</td></tr>
                <tr><td style="color:#64748b;padding:4px 0;">Email</td><td>%s</td></tr>
                <tr><td style="color:#64748b;padding:4px 0;">Điện thoại</td><td>%s</td></tr>
                <tr><td style="color:#64748b;padding:4px 0;">Xe quan tâm</td><td style="font-weight:500;">%s</td></tr>
                <tr><td style="color:#64748b;padding:4px 0;">Ngày tạo</td><td>%s</td></tr>
                <tr><td style="color:#64748b;padding:4px 0;">Trạng thái</td><td style="font-weight:600;">%s</td></tr>
                """.formatted(
                c.getId(),
                khach,
                email,
                sdt,
                carLine(c.getCarInventory()),
                formatDt(c.getCreatedAt()),
                esc(c.getStatus().getVietnameseLabel()));
    }

    private String contactSummaryBlock(ContactRequest c) {
        return """
                <table role="presentation" width="100%%" style="border-collapse:collapse;background:#f8fafc;border-radius:10px;margin:16px 0;">
                <tr><td style="padding:16px 18px;">
                <table role="presentation" width="100%%" style="font-size:14px;">
                %s
                </table>
                </td></tr>
                </table>
                """.formatted(contactSummaryRows(c));
    }

    private String testDriveSummaryRows(TestDriveBooking b) {
        User u = b.getUser();
        String khach = u != null ? esc(u.getFullName()) : "—";
        String email = u != null ? esc(u.getEmail()) : "—";
        String sdt = u != null && u.getPhone() != null ? esc(u.getPhone()) : "—";
        return """
                <tr><td style="color:#64748b;padding:4px 0;width:38%%;">Mã đăng ký</td><td style="font-weight:600;">#%d</td></tr>
                <tr><td style="color:#64748b;padding:4px 0;">Khách hàng</td><td>%s</td></tr>
                <tr><td style="color:#64748b;padding:4px 0;">Email</td><td>%s</td></tr>
                <tr><td style="color:#64748b;padding:4px 0;">Điện thoại</td><td>%s</td></tr>
                <tr><td style="color:#64748b;padding:4px 0;">Xe</td><td style="font-weight:500;">%s</td></tr>
                <tr><td style="color:#64748b;padding:4px 0;">Chi nhánh</td><td>%s</td></tr>
                <tr><td style="color:#64748b;padding:4px 0;">Giờ lái thử</td><td style="font-weight:600;color:#0d6efd;">%s</td></tr>
                <tr><td style="color:#64748b;padding:4px 0;">Ngày tạo yêu cầu</td><td>%s</td></tr>
                <tr><td style="color:#64748b;padding:4px 0;">Trạng thái</td><td style="font-weight:600;">%s</td></tr>
                """.formatted(
                b.getId(),
                khach,
                email,
                sdt,
                carLine(b.getCarInventory()),
                branchLine(b.getBranch()),
                formatDt(b.getTestDateTime()),
                formatDt(b.getCreatedAt()),
                esc(b.getStatus().getVietnameseLabel()));
    }

    private String testDriveSummaryBlock(TestDriveBooking b) {
        return """
                <table role="presentation" width="100%%" style="border-collapse:collapse;background:#f8fafc;border-radius:10px;margin:16px 0;">
                <tr><td style="padding:16px 18px;">
                <table role="presentation" width="100%%" style="font-size:14px;">
                %s
                </table>
                </td></tr>
                </table>
                """.formatted(testDriveSummaryRows(b));
    }

    private static boolean canSend(User u) {
        return u != null && u.getEmail() != null && !u.getEmail().isBlank();
    }

    public void sendContactRequestSubmitted(ContactRequest c) {
        if (!canSend(c.getUser())) {
            return;
        }
        String inner = contactSummaryBlock(c)
                + "<p style=\"margin:16px 0 0;\">FCAR đã <strong>ghi nhận yêu cầu liên hệ tư vấn</strong> của bạn. "
                + "Đội ngũ tư vấn sẽ liên hệ qua điện thoại hoặc email trong thời gian sớm nhất có thể.</p>"
                + "<p style=\"margin:12px 0 0;\">Bạn có thể theo dõi trạng thái trong mục <strong>Lịch sử</strong> trên tài khoản.</p>";
        String html = CustomerEmailHtmlShell.wrap("#0d6efd", "LIÊN HỆ TƯ VẤN", "Đã ghi nhận yêu cầu liên hệ", inner);
        mailNotificationService.sendHtml(c.getUser().getEmail(),
                "FCAR — Liên hệ tư vấn · Yêu cầu #" + c.getId(),
                html);
    }

    public void sendContactRequestCanceledByCustomer(ContactRequest c) {
        if (!canSend(c.getUser())) {
            return;
        }
        String inner = contactSummaryBlock(c)
                + "<p style=\"margin:16px 0 0;\">Yêu cầu liên hệ tư vấn của bạn đã được <strong>hủy trên hệ thống</strong>.</p>"
                + "<p style=\"margin:12px 0 0;\">Nếu cần hỗ trợ thêm, bạn có thể gửi yêu cầu mới bất cứ lúc nào từ trang chi tiết xe.</p>";
        String html = CustomerEmailHtmlShell.wrap("#dc2626", "ĐÃ HỦY", "Yêu cầu liên hệ đã được hủy", inner);
        mailNotificationService.sendHtml(c.getUser().getEmail(),
                "FCAR — Yêu cầu liên hệ #" + c.getId() + " đã hủy",
                html);
    }

    public void sendContactRequestStatusUpdated(ContactRequest c) {
        if (!canSend(c.getUser())) {
            return;
        }
        boolean contacted = c.getStatus() == ContactStatus.CONTACTED;
        String titleColor = contacted ? "#059669" : "#64748b";
        String badge = contacted ? "ĐÃ LIÊN HỆ" : "ĐÃ HỦY";
        String headline = contacted ? "Showroom đã liên hệ với bạn" : "Yêu cầu liên hệ đã được đóng";
        String extra = contacted
                ? "<p style=\"margin:16px 0 0;\">Trạng thái yêu cầu của bạn hiện là <strong>Đã liên hệ</strong>. "
                + "Hy vọng bạn đã nhận được tư vấn thuận lợi. Mọi thắc mắc, vui lòng trả lời trực tiếp nhân viên đã liên hệ hoặc liên hệ lại showroom.</p>"
                : "<p style=\"margin:16px 0 0;\">Yêu cầu liên hệ tư vấn của bạn đã được cập nhật sang <strong>Đã hủy</strong>.</p>"
                + "<p style=\"margin:12px 0 0;\">Nếu bạn cần được tư vấn lại, bạn có thể tạo yêu cầu mới trên website.</p>";
        String inner = contactSummaryBlock(c) + extra;
        String html = CustomerEmailHtmlShell.wrap(titleColor, badge, headline, inner);
        mailNotificationService.sendHtml(c.getUser().getEmail(),
                "FCAR — Cập nhật yêu cầu liên hệ #" + c.getId(),
                html);
    }

    public void sendTestDriveBookingSubmitted(TestDriveBooking b) {
        if (!canSend(b.getUser())) {
            return;
        }
        String inner = testDriveSummaryBlock(b)
                + "<p style=\"margin:16px 0 0;\">FCAR đã <strong>ghi nhận đăng ký lái thử</strong> của bạn. "
                + "Nhân viên sẽ xác nhận lịch và liên hệ nếu cần điều chỉnh.</p>"
                + "<p style=\"margin:12px 0 0;\">Bạn có thể xem hoặc hủy lịch (khi còn cho phép) trong mục <strong>Lịch sử</strong>.</p>";
        String html = CustomerEmailHtmlShell.wrap("#0284c7", "LÁI THỬ", "Đăng ký lái thử thành công", inner);
        mailNotificationService.sendHtml(b.getUser().getEmail(),
                "FCAR — Lái thử · Đăng ký #" + b.getId(),
                html);
    }

    public void sendTestDriveBookingCanceledByCustomer(TestDriveBooking b) {
        if (!canSend(b.getUser())) {
            return;
        }
        String inner = testDriveSummaryBlock(b)
                + "<p style=\"margin:16px 0 0;\">Lịch lái thử của bạn đã được <strong>hủy trên hệ thống</strong>.</p>"
                + "<p style=\"margin:12px 0 0;\">Khi cần, bạn có thể đăng ký lại lịch lái thử phù hợp.</p>";
        String html = CustomerEmailHtmlShell.wrap("#dc2626", "ĐÃ HỦY", "Đăng ký lái thử đã được hủy", inner);
        mailNotificationService.sendHtml(b.getUser().getEmail(),
                "FCAR — Lái thử #" + b.getId() + " đã hủy",
                html);
    }

    public void sendTestDriveBookingStatusUpdated(TestDriveBooking b) {
        if (!canSend(b.getUser())) {
            return;
        }
        String titleColor;
        String badge;
        String headline;
        String extra;
        switch (b.getStatus()) {
            case APPROVED -> {
                titleColor = "#0ea5e9";
                badge = "ĐÃ DUYỆT";
                headline = "Lịch lái thử đã được xác nhận";
                extra = "<p style=\"margin:16px 0 0;\">Lịch lái thử của bạn đã được <strong>duyệt</strong>. "
                        + "Vui lòng đến đúng <strong>giờ và chi nhánh</strong> đã chọn (hoặc theo hướng dẫn từ showroom).</p>"
                        + "<p style=\"margin:12px 0 0;\">Nếu có thay đổi đột xuất, hãy liên hệ showroom sớm nhất có thể.</p>";
            }
            case COMPLETED -> {
                titleColor = "#059669";
                badge = "HOÀN THÀNH";
                headline = "Lái thử đã hoàn thành";
                extra = "<p style=\"margin:16px 0 0;\">Showroom đã <strong>hoàn tất buổi lái thử</strong> theo lịch đăng ký.</p>"
                        + "<p style=\"margin:12px 0 0;\">Cảm ơn bạn đã trải nghiệm. Rất mong được đồng hành cùng bạn khi quyết định chọn xe.</p>";
            }
            case CANCELED -> {
                titleColor = "#64748b";
                badge = "ĐÃ HỦY";
                headline = "Đăng ký lái thử đã được hủy";
                extra = "<p style=\"margin:16px 0 0;\">Lịch lái thử của bạn đã được cập nhật sang <strong>Đã hủy</strong> từ phía showroom hoặc theo thỏa thuận.</p>"
                        + "<p style=\"margin:12px 0 0;\">Bạn có thể tạo đăng ký lái thử mới khi phù hợp.</p>";
            }
            default -> {
                titleColor = "#64748b";
                badge = "CẬP NHẬT";
                headline = "Cập nhật trạng thái lái thử";
                extra = "<p style=\"margin:16px 0 0;\">Trạng thái đăng ký lái thử của bạn đã được cập nhật.</p>";
            }
        }
        String inner = testDriveSummaryBlock(b) + extra;
        String html = CustomerEmailHtmlShell.wrap(titleColor, badge, headline, inner);
        mailNotificationService.sendHtml(b.getUser().getEmail(),
                "FCAR — Cập nhật lái thử #" + b.getId(),
                html);
    }
}
