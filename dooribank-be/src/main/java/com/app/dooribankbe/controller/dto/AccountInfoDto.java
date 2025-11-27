package com.app.dooribankbe.controller.dto;

import lombok.Data;
import java.time.LocalDate;

@Data
public class AccountInfoDto {
    private String accountNumber;
    private String accountPassword;
    private Long balance;
    private LocalDate accountCreateAt;
}