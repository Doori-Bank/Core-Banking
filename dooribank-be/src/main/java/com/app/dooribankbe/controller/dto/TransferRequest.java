package com.app.dooribankbe.controller.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record TransferRequest(
        @NotBlank(message = "출금 계좌 번호는 필수입니다.")
        String fromAccountNumber,

        @NotBlank(message = "출금 계좌 비밀번호는 필수입니다.")
        String password,

        @NotBlank(message = "입금 계좌 번호는 필수입니다.")
        String toAccountNumber,

        @NotNull(message = "이체 금액은 필수입니다.")
        @Positive(message = "이체 금액은 0보다 커야 합니다.")
        Long amount,

        String memo
) {
}

