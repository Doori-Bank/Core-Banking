package com.app.dooribankbe.service;

import com.app.dooribankbe.controller.dto.*;
import com.app.dooribankbe.domain.entity.Member;
import com.app.dooribankbe.domain.entity.MemberAccount;
import com.app.dooribankbe.domain.repository.MemberAccountRepository;
import com.app.dooribankbe.domain.entity.AccountHistory;
import com.app.dooribankbe.domain.repository.AccountHistoryRepository;
import com.app.dooribankbe.domain.repository.MemberRepository;
import com.app.dooribankbe.domain.entity.HistoryCategory;
import com.app.dooribankbe.domain.entity.TransactionType;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class AccountService {

    private final MemberAccountRepository memberAccountRepository;
    private final AccountHistoryRepository accountHistoryRepository;
    private final MemberRepository memberRepository;
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

        accountHistoryRepository.flush();

        if (history != null && history.getId() != null) {
            final Long historyId = history.getId();
            final String accountNumber = history.getAccount().getAccountNumber();
            final LocalDateTime historyDate = history.getHistoryDate();
            final Long historyPrice = history.getHistoryPrice();
            final String historyStatus = history.getHistoryStatus().name();
            final String historyCategory = history.getHistoryCategory().name();
            final String historyName = history.getHistoryName();
            final String historyTransferTarget = history.getHistoryTransferTarget();

            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
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

        accountHistoryRepository.flush();

        if (withdrawHistory != null && withdrawHistory.getId() != null) {
            final Long historyId = withdrawHistory.getId();
            final String accountNumber = withdrawHistory.getAccount().getAccountNumber();
            final LocalDateTime historyDate = withdrawHistory.getHistoryDate();
            final Long historyPrice = withdrawHistory.getHistoryPrice();
            final String historyStatus = withdrawHistory.getHistoryStatus().name();
            final String historyCategory = withdrawHistory.getHistoryCategory().name();
            final String historyName = withdrawHistory.getHistoryName();
            final String historyTransferTarget = withdrawHistory.getHistoryTransferTarget();

            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
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

    // ========== 테스트용 메서드 ==========

    /**
     * 모든 회원 정보 조회 (계좌 정보 포함)
     */
    @Transactional(readOnly = true)
    public List<MemberInfoDto> getAllMembers() {
        log.info("모든 회원 정보 조회 시작");
        List<Member> members = memberRepository.findAll();
        log.info("총 {}명의 회원 조회 완료", members.size());

        return members.stream()
                .map(this::convertToMemberInfoDto)
                .collect(Collectors.toList());
    }

    /**
     * 회원 이름으로 모든 계좌 조회
     */
    @Transactional(readOnly = true)
    public List<AccountInfoDto> getMemberAccountsByName(String memberName) {
        log.info("회원 이름으로 계좌 조회: {}", memberName);

        List<Member> members = memberRepository.findByName(memberName);

        if (members.isEmpty()) {
            log.warn("해당 이름의 회원을 찾을 수 없습니다: {}", memberName);
            return List.of();
        }

        log.info("이름이 '{}'인 회원 {}명 발견", memberName, members.size());

        List<AccountInfoDto> accounts = members.stream()
                .flatMap(member -> {
                    List<MemberAccount> memberAccounts = memberAccountRepository.findByMember(member);
                    log.info("회원 '{}' (ID: {})의 계좌 {}개 조회", member.getName(), member.getId(), memberAccounts.size());
                    return memberAccounts.stream();
                })
                .map(this::convertToAccountInfoDto)
                .collect(Collectors.toList());

        log.info("총 {}개의 계좌 반환", accounts.size());
        return accounts;
    }

    private MemberInfoDto convertToMemberInfoDto(Member member) {
        MemberInfoDto dto = new MemberInfoDto();
        dto.setId(member.getId());
        dto.setName(member.getName());
        dto.setPhone(member.getPhone());
        dto.setMemberRegistNum(member.getMemberRegistNum());

        List<MemberAccount> accounts = memberAccountRepository.findByMember(member);
        if (accounts != null && !accounts.isEmpty()) {
            MemberAccount account = accounts.get(0);
            dto.setAccountNumber(account.getAccountNumber());
            dto.setAccountPassword(account.getAccountPassword());
        }

        return dto;
    }

    private AccountInfoDto convertToAccountInfoDto(MemberAccount account) {
        AccountInfoDto dto = new AccountInfoDto();
        dto.setAccountNumber(account.getAccountNumber());
        dto.setAccountPassword(account.getAccountPassword());
        dto.setBalance(account.getBalance());
        dto.setAccountCreateAt(account.getAccountCreateAt());
        return dto;
    }

    // ========== Private 헬퍼 메서드 ==========

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