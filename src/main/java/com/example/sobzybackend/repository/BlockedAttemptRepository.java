package com.example.sobzybackend.repository;


import com.example.sobzybackend.enums.AlertSeverity;
import com.example.sobzybackend.models.BlockedAttempt;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface BlockedAttemptRepository extends JpaRepository<BlockedAttempt, Long> {

    Page<BlockedAttempt> findByUserId(Long userId, Pageable pageable);

    Page<BlockedAttempt> findByDeviceId(Long deviceId, Pageable pageable);

    List<BlockedAttempt> findBySeverity(AlertSeverity severity);

    @Query("SELECT b FROM BlockedAttempt b WHERE b.attemptedAt BETWEEN :startDate AND :endDate")
    List<BlockedAttempt> findByDateRange(@Param("startDate") LocalDateTime startDate,
                                         @Param("endDate") LocalDateTime endDate);

    @Query("SELECT b.domain, COUNT(b) as count FROM BlockedAttempt b GROUP BY b.domain ORDER BY count DESC")
    List<Object[]> findTopBlockedDomains(Pageable pageable);

    @Query("SELECT COUNT(b) FROM BlockedAttempt b WHERE b.user.id = :userId")
    long countByUserId(@Param("userId") Long userId);

    @Query("SELECT COUNT(b) FROM BlockedAttempt b WHERE b.severity = :severity")
    long countBySeverity(@Param("severity") AlertSeverity severity);

    @Query("SELECT b FROM BlockedAttempt b WHERE b.attemptedAt >= :since ORDER BY b.attemptedAt DESC")
    List<BlockedAttempt> findRecentAttempts(@Param("since") LocalDateTime since, Pageable pageable);

    void deleteByAttemptedAtBefore(LocalDateTime date);
}