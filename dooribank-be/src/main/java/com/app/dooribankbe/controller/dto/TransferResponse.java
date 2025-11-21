package com.app.dooribankbe.controller.dto;

public record TransferResponse(
        Long withdrawalHistoryId,
        Long depositHistoryId,
        Long fromAccountBalance,
        Long toAccountBalance
) {
}

