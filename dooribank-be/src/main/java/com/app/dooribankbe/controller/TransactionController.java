package com.app.dooribankbe.controller;

import com.app.dooribankbe.service.AccountService;
import com.app.dooribankbe.controller.dto.PaymentRequest;
import com.app.dooribankbe.controller.dto.PaymentResponse;
import com.app.dooribankbe.controller.dto.TransferRequest;
import com.app.dooribankbe.controller.dto.TransferResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@Validated
@RequiredArgsConstructor
@RequestMapping("/api/transactions")
@Tag(name = "거래", description = "결제 및 계좌 이체 API")
public class TransactionController {

    private final AccountService accountService;

    @PostMapping("/payment")
    @Operation(summary = "결제 처리", description = "카드 결제를 처리하고 거래 내역을 생성합니다.")
    public ResponseEntity<PaymentResponse> payment(@Valid @RequestBody PaymentRequest request) {
        return ResponseEntity.ok(accountService.processPayment(request));
    }

    @PostMapping("/transfer")
    @Operation(summary = "계좌 이체", description = "출금 계좌의 비밀번호를 검증한 뒤, 다른 계좌로 금액을 이체합니다.")
    public ResponseEntity<TransferResponse> transfer(@Valid @RequestBody TransferRequest request) {
        return ResponseEntity.ok(accountService.transfer(request));
    }
}

