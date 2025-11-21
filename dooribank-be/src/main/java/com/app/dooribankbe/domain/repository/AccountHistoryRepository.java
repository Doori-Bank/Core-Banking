package com.app.dooribankbe.domain.repository;

import com.app.dooribankbe.domain.entity.AccountHistory;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AccountHistoryRepository extends JpaRepository<AccountHistory, Long> {
}

