package com.fcar.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "store_bank_accounts")
public class StoreBankAccount extends BaseEntity {

    @Column(name = "bank_name", nullable = false, length = 100)
    private String bankName;

    @Column(name = "account_name", nullable = false, length = 150)
    private String accountName;

    @Column(name = "account_number", nullable = false, length = 50)
    private String accountNumber;

    /**
     * Mã BIN ngân hàng 6 số (NAPAS) để tạo VietQR, ví dụ 970436 (Vietcombank).
     * Bắt buộc nếu muốn hiển thị QR trên trang đặt cọc.
     */
    @Column(name = "bank_bin", length = 10)
    private String bankBin;

    @Column(name = "active", nullable = false)
    private boolean active = true;
}

