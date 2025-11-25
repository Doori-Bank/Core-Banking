package com.app.dooribankbe.controller;

import com.app.dooribankbe.domain.entity.Member;
import com.app.dooribankbe.domain.repository.MemberRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
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

    private final MemberRepository memberRepository;

    @GetMapping("/members")
    @Operation(summary = "두리뱅크 회원 목록 조회", 
               description = "두리뱅크의 모든 회원 정보를 반환합니다. 부하 테스트용입니다.")
    public ResponseEntity<List<MemberInfoDto>> getMembers() {
        // 두리뱅크의 모든 회원 조회
        List<Member> members = memberRepository.findAll();
        
        List<MemberInfoDto> memberInfoList = members.stream()
                .map(member -> {
                    MemberInfoDto dto = new MemberInfoDto();
                    dto.setName(member.getName());
                    dto.setPhone(member.getPhone());
                    dto.setMemberRegistNum(member.getMemberRegistNum());
                    return dto;
                })
                .collect(Collectors.toList());
        
        return ResponseEntity.ok(memberInfoList);
    }

    @Data
    public static class MemberInfoDto {
        private String name;
        private String phone;
        private String memberRegistNum;
    }
}

