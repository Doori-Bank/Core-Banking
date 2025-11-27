package com.app.dooribankbe.domain.repository;

import com.app.dooribankbe.domain.entity.Member;
import com.app.dooribankbe.domain.entity.MemberAccount;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface MemberAccountRepository extends JpaRepository<MemberAccount, Long> {

    Optional<MemberAccount> findByAccountNumber(String accountNumber);
    
    List<MemberAccount> findByMember(Member member);
}

