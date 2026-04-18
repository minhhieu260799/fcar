package com.fcar.core.email;

/**
 * Khung HTML chung cho email gửi khách (cùng style với đơn hàng).
 */
public final class CustomerEmailHtmlShell {

    private CustomerEmailHtmlShell() {
    }

    public static String wrap(String titleColor, String badge, String headline, String innerHtml) {
        return """
                <!DOCTYPE html>
                <html><head><meta charset="UTF-8"></head>
                <body style="margin:0;padding:0;background:#f4f6f9;">
                <table role="presentation" width="100%%" cellspacing="0" cellpadding="0" style="background:#f4f6f9;padding:24px 12px;">
                <tr><td align="center">
                <table role="presentation" width="600" cellspacing="0" cellpadding="0" style="max-width:600px;background:#ffffff;border-radius:12px;overflow:hidden;box-shadow:0 4px 24px rgba(15,23,42,0.08);font-family:Segoe UI,Roboto,Helvetica,Arial,sans-serif;color:#1e293b;">
                <tr><td style="background:%s;padding:28px 32px 20px;">
                <div style="font-size:22px;font-weight:700;color:#ffffff;letter-spacing:0.06em;">FCAR</div>
                <div style="font-size:13px;color:rgba(255,255,255,0.9);margin-top:4px;">Hệ thống đặt mua xe trực tuyến</div>
                <div style="display:inline-block;margin-top:14px;padding:6px 12px;background:rgba(255,255,255,0.2);border-radius:999px;font-size:12px;font-weight:600;color:#fff;">%s</div>
                </td></tr>
                <tr><td style="padding:28px 32px 8px;">
                <h1 style="margin:0 0 8px;font-size:20px;font-weight:600;color:#0f172a;">%s</h1>
                </td></tr>
                <tr><td style="padding:0 32px 24px;font-size:15px;line-height:1.65;color:#334155;">
                %s
                </td></tr>
                <tr><td style="padding:20px 32px 28px;border-top:1px solid #e2e8f0;background:#f8fafc;">
                <p style="margin:0 0 8px;font-size:12px;color:#64748b;">Email được gửi tự động từ FCAR. Vui lòng không trả lời trực tiếp email này.</p>
                <p style="margin:0;font-size:12px;color:#94a3b8;">Nếu cần hỗ trợ, vui lòng liên hệ showroom hoặc hotline trên website.</p>
                </td></tr>
                </table>
                </td></tr>
                </table>
                </body></html>
                """.formatted(titleColor, badge, headline, innerHtml);
    }
}
