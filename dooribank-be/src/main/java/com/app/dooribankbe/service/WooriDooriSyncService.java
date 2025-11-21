package com.app.dooribankbe.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class WooriDooriSyncService {

    private final RestTemplate restTemplate;

    @Value("${wooridoori.api.url:http://localhost:8080}")
    private String wooriDooriApiUrl;

    /**
     * 8080 서버(WooriDoori-BE)에 Payment/Transfer 동기화 요청을 보냅니다.
     * 트랜잭션 커밋 후 호출되므로 필요한 데이터를 파라미터로 받습니다.
     * 
     * @param historyId AccountHistory ID
     * @param accountNumber 계좌번호
     * @param historyDate 거래 일시
     * @param historyPrice 거래 금액
     * @param historyStatus 거래 상태
     * @param historyCategory 거래 카테고리
     * @param historyName 거래명
     * @param historyTransferTarget 이체 대상 계좌번호 (이체인 경우)
     * @return 동기화 성공 여부
     */
    public boolean syncToWooriDoori(
            Long historyId,
            String accountNumber,
            LocalDateTime historyDate,
            Long historyPrice,
            String historyStatus,
            String historyCategory,
            String historyName,
            String historyTransferTarget
    ) {
        try {
            String url = wooriDooriApiUrl + "/history/calendar/sync";
            
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            
            Map<String, Object> requestBody = new HashMap<>();
            requestBody.put("accountNumber", accountNumber);
            requestBody.put("historyId", historyId);
            requestBody.put("historyDate", historyDate.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));
            requestBody.put("historyPrice", historyPrice);
            requestBody.put("historyStatus", historyStatus);
            requestBody.put("historyCategory", historyCategory);
            requestBody.put("historyName", historyName);
            requestBody.put("historyTransferTarget", historyTransferTarget);
            
            HttpEntity<Map<String, Object>> request = new HttpEntity<>(requestBody, headers);
            
            log.info("8080 서버로 동기화 요청 전송: accountNumber={}, historyId={}, historyName={}, url={}", 
                    accountNumber, historyId, historyName, url);
            
            ResponseEntity<?> response = restTemplate.postForEntity(url, request, Object.class);
            
            if (response.getStatusCode().is2xxSuccessful()) {
                log.info("8080 서버 동기화 성공: accountNumber={}, historyId={}", accountNumber, historyId);
                return true;
            } else {
                log.warn("8080 서버 동기화 실패: status={}, accountNumber={}, historyId={}", 
                        response.getStatusCode(), accountNumber, historyId);
                return false;
            }
        } catch (Exception e) {
            log.error("8080 서버 동기화 중 오류 발생: accountNumber={}, historyId={}", accountNumber, historyId, e);
            return false;
        }
    }
}

