package com.app.dooribankbe.service;

import com.app.dooribankbe.domain.entity.MemberAccount;
import com.app.dooribankbe.domain.repository.MemberAccountRepository;
import com.app.dooribankbe.domain.entity.AccountHistory;
import com.app.dooribankbe.domain.repository.AccountHistoryRepository;
import com.app.dooribankbe.domain.entity.HistoryCategory;
import com.app.dooribankbe.domain.entity.TransactionType;
import com.app.dooribankbe.controller.dto.PaymentRequest;
import com.app.dooribankbe.controller.dto.PaymentResponse;
import com.app.dooribankbe.controller.dto.TransferRequest;
import com.app.dooribankbe.controller.dto.TransferResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class AccountService {

    private final MemberAccountRepository memberAccountRepository;
    private final AccountHistoryRepository accountHistoryRepository;
    private final WooriDooriSyncService wooriDooriSyncService;

    @Transactional
    public PaymentResponse processPayment(PaymentRequest request) {
        MemberAccount account = getAccountByNumber(request.accountNumber());
        validatePassword(account, request.password());
        withdraw(account, request.amount());

        AccountHistory history = accountHistoryRepository.save(AccountHistory.builder()
                .account(account)
                .historyPrice(request.amount())
                .historyStatus(TransactionType.PAYMENT)
                .historyCategory(request.category())
                .historyName(request.merchantName())
                .historyTransferTarget(null)
                .build());

        // 즉시 DB에 반영
        accountHistoryRepository.flush();

        // 트랜잭션 커밋 후 8080 서버로 동기화 요청 전송
        // 트랜잭션 내에서 필요한 데이터를 미리 추출 (LAZY 로딩 문제 방지)
        if (history != null && history.getId() != null) {
            final Long historyId = history.getId();
            final String accountNumber = history.getAccount().getAccountNumber(); // 트랜잭션 내에서 로드
            final LocalDateTime historyDate = history.getHistoryDate();
            final Long historyPrice = history.getHistoryPrice();
            final String historyStatus = history.getHistoryStatus().name();
            final String historyCategory = history.getHistoryCategory().name();
            final String historyName = history.getHistoryName();
            final String historyTransferTarget = history.getHistoryTransferTarget();
            
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    // 트랜잭션 커밋 후 실행 (이미 추출한 데이터 사용)
                    wooriDooriSyncService.syncToWooriDoori(
                            historyId, accountNumber, historyDate, historyPrice, 
                            historyStatus, historyCategory, historyName, historyTransferTarget
                    );
                }
            });
        }

        return new PaymentResponse(history.getId(), account.getBalance());
    }

    @Transactional
    public TransferResponse transfer(TransferRequest request) {
        MemberAccount fromAccount = getAccountByNumber(request.fromAccountNumber());
        if (fromAccount.getAccountNumber().equals(request.toAccountNumber())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "같은 계좌로는 이체할 수 없습니다.");
        }
        MemberAccount toAccount = getAccountByNumber(request.toAccountNumber());

        validatePassword(fromAccount, request.password());
        withdraw(fromAccount, request.amount());
        deposit(toAccount, request.amount());

        AccountHistory withdrawHistory = accountHistoryRepository.save(AccountHistory.builder()
                .account(fromAccount)
                .historyPrice(request.amount())
                .historyStatus(TransactionType.TRANSFER_OUT)
                .historyCategory(HistoryCategory.TRANSFER)
                .historyName(request.memo() != null ? request.memo() : "계좌이체 출금")
                .historyTransferTarget(toAccount.getAccountNumber())
                .build());

        AccountHistory depositHistory = accountHistoryRepository.save(AccountHistory.builder()
                .account(toAccount)
                .historyPrice(request.amount())
                .historyStatus(TransactionType.TRANSFER_IN)
                .historyCategory(HistoryCategory.TRANSFER)
                .historyName(request.memo() != null ? request.memo() : "계좌이체 입금")
                .historyTransferTarget(fromAccount.getAccountNumber())
                .build());

        // 즉시 DB에 반영
        accountHistoryRepository.flush();

        // 트랜잭션 커밋 후 8080 서버로 동기화 요청 전송 (출금 내역만 전송)
        // 트랜잭션 내에서 필요한 데이터를 미리 추출 (LAZY 로딩 문제 방지)
        if (withdrawHistory != null && withdrawHistory.getId() != null) {
            final Long historyId = withdrawHistory.getId();
            final String accountNumber = withdrawHistory.getAccount().getAccountNumber(); // 트랜잭션 내에서 로드
            final LocalDateTime historyDate = withdrawHistory.getHistoryDate();
            final Long historyPrice = withdrawHistory.getHistoryPrice();
            final String historyStatus = withdrawHistory.getHistoryStatus().name();
            final String historyCategory = withdrawHistory.getHistoryCategory().name();
            final String historyName = withdrawHistory.getHistoryName();
            final String historyTransferTarget = withdrawHistory.getHistoryTransferTarget();
            
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    // 트랜잭션 커밋 후 실행 (이미 추출한 데이터 사용)
                    wooriDooriSyncService.syncToWooriDoori(
                            historyId, accountNumber, historyDate, historyPrice, 
                            historyStatus, historyCategory, historyName, historyTransferTarget
                    );
                }
            });
        }

        return new TransferResponse(
                withdrawHistory.getId(),
                depositHistory.getId(),
                fromAccount.getBalance(),
                toAccount.getBalance()
        );
    }

    private MemberAccount getAccountByNumber(String accountNumber) {
        return memberAccountRepository.findByAccountNumber(accountNumber)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "계좌를 찾을 수 없습니다."));
    }

    private void validatePassword(MemberAccount account, String password) {
        if (!account.matchPassword(password)) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "계좌 비밀번호가 일치하지 않습니다.");
        }
    }

    private void withdraw(MemberAccount account, Long amount) {
        try {
            account.withdraw(amount);
        } catch (IllegalArgumentException | IllegalStateException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, e.getMessage());
        }
    }

    private void deposit(MemberAccount account, Long amount) {
        try {
            account.deposit(amount);
        } catch (IllegalArgumentException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, e.getMessage());
        }
    }

}

