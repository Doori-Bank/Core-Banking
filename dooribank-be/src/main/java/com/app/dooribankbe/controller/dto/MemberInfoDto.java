package com.app.dooribankbe.controller.dto;

import lombok.Data;

@Data
public class MemberInfoDto {
    private Long id;
    private String name;
    private String phone;
    private String memberRegistNum;
    private String accountNumber;  // 첫 번째 계좌번호
    private String accountPassword; // 첫 번째 계좌 비밀번호
}