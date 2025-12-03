package com.bmcho.pointservice.repository;

import com.bmcho.pointservice.domain.PointBalance;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;

import java.util.Optional;

public interface PointBalanceRepository extends JpaRepository<PointBalance, Long> {

    @Lock(LockModeType.OPTIMISTIC)
    Optional<PointBalance> findByUserId(Long userId);
}