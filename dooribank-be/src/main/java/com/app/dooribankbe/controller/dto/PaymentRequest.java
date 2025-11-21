package com.app.dooribankbe.controller.dto;

import com.app.dooribankbe.domain.entity.HistoryCategory;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record PaymentRequest(
        @NotBlank(message = "계좌 번호는 필수입니다.")
        String accountNumber,

        @NotBlank(message = "계좌 비밀번호는 필수입니다.")
        String password,

        @NotNull(message = "결제 금액은 필수입니다.")
        @Positive(message = "결제 금액은 0보다 커야 합니다.")
        Long amount,

        @NotNull(message = "카테고리를 선택해주세요.")
        HistoryCategory category,

        @NotBlank(message = "가맹점명을 입력해주세요.")
        String merchantName
) {
}

