package com.fcar.domain.enums;

public enum TestDriveStatus {
    PENDING("Chờ xác nhận"),
    APPROVED("Đã duyệt"),
    COMPLETED("Hoàn thành"),
    CANCELED("Đã hủy");

    private final String vietnameseLabel;

    TestDriveStatus(String vietnameseLabel) {
        this.vietnameseLabel = vietnameseLabel;
    }

    /** Nhãn hiển thị cho khách (email, thông báo). */
    public String getVietnameseLabel() {
        return vietnameseLabel;
    }
}

