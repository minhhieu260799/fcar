package com.fcar.domain.enums;

public enum ContactStatus {
    PENDING("Chờ xử lý"),
    CONTACTED("Đã liên hệ"),
    CANCELED("Đã hủy");

    private final String vietnameseLabel;

    ContactStatus(String vietnameseLabel) {
        this.vietnameseLabel = vietnameseLabel;
    }

    /** Nhãn hiển thị cho khách (email, thông báo). */
    public String getVietnameseLabel() {
        return vietnameseLabel;
    }
}

