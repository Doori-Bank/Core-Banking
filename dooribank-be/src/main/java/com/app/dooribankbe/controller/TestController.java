package com.app.dooribankbe.controller;

import com.app.dooribankbe.controller.dto.AccountInfoDto;
import com.app.dooribankbe.controller.dto.MemberInfoDto;
import com.app.dooribankbe.domain.entity.Member;
import com.app.dooribankbe.domain.entity.MemberAccount;
import com.app.dooribankbe.domain.repository.MemberRepository;
import com.app.dooribankbe.domain.repository.MemberAccountRepository;
import com.app.dooribankbe.service.AccountService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.stream.Collectors;

/**
 * 부하 테스트용 컨트롤러
 * 두리뱅크의 모든 회원 데이터를 반환합니다.
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/test")
@Tag(name = "테스트", description = "부하 테스트용 API")
public class TestController {

    private final AccountService accountService;

    @GetMapping("/members")
    @Operation(summary = "두리뱅크 회원 목록 조회", 
               description = "두리뱅크의 모든 회원 정보와 계좌 정보를 반환합니다. 부하 테스트용입니다.")
    public ResponseEntity<List<MemberInfoDto>> getMembers() {
        List<MemberInfoDto> members = accountService.getAllMembers();
        return ResponseEntity.ok(members);
    }

    @GetMapping("/member-accounts")
    @Operation(summary = "회원의 모든 계좌 조회", 
               description = "회원 이름으로 해당 회원의 모든 계좌 정보를 반환합니다. 부하 테스트용입니다.")
    public ResponseEntity<List<AccountInfoDto>> getMemberAccounts(@RequestParam String memberName) {
        List<AccountInfoDto> accounts = accountService.getMemberAccountsByName(memberName);
        return ResponseEntity.ok(accounts);
    }
}

