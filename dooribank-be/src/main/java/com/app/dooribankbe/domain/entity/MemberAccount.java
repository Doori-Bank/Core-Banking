package com.app.dooribankbe.domain.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Entity
@Table(name = "tbl_member_account")
@Getter
@Builder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class MemberAccount {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "member_id")
    private Member member;

    @Column(name = "account_num", nullable = false, unique = true, length = 20)
    private String accountNumber;

    @Column(name = "account_password", nullable = false, length = 4)
    private String accountPassword;

    @Column(name = "account_create_at", nullable = false)
    private LocalDate accountCreateAt;

    @Column(nullable = false)
    private Long balance;

    @PrePersist
    void onCreate() {
        if (accountCreateAt == null) {
            accountCreateAt = LocalDate.now();
        }
        if (balance == null) {
            balance = 0L;
        }
    }

    public void withdraw(long amount) {
        if (amount <= 0) {
            throw new IllegalArgumentException("출금 금액은 0보다 커야 합니다.");
        }
        if (balance < amount) {
            throw new IllegalStateException("계좌 잔액이 부족합니다.");
        }
        balance -= amount;
    }

    public void deposit(long amount) {
        if (amount <= 0) {
            throw new IllegalArgumentException("입금 금액은 0보다 커야 합니다.");
        }
        balance += amount;
    }

    public boolean matchPassword(String password) {
        return this.accountPassword.equals(password);
    }
}

