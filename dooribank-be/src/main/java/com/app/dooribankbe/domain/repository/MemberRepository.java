package com.app.dooribankbe.domain.repository;

import com.app.dooribankbe.domain.entity.Member;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MemberRepository extends JpaRepository<Member, Long> {
}

