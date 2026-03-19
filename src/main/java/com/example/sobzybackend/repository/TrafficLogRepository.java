package com.example.sobzybackend.repository;

import com.example.sobzybackend.models.TrafficLog;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface TrafficLogRepository extends JpaRepository<TrafficLog, Long> {

    Page<TrafficLog> findByUserId(Long userId, Pageable pageable);

    Page<TrafficLog> findByDeviceId(Long deviceId, Pageable pageable);

    Page<TrafficLog> findByIsBlocked(Boolean isBlocked, Pageable pageable);

    @Query("SELECT t FROM TrafficLog t WHERE t.requestTimestamp BETWEEN :startDate AND :endDate")
    List<TrafficLog> findByDateRange(@Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate);

    @Query("SELECT t FROM TrafficLog t WHERE t.user.id = :userId AND t.requestTimestamp BETWEEN :startDate AND :endDate")
    List<TrafficLog> findByUserAndDateRange(@Param("userId") Long userId,
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate);

    @Query("SELECT t FROM TrafficLog t WHERE t.domain = :domain ORDER BY t.requestTimestamp DESC")
    List<TrafficLog> findByDomain(@Param("domain") String domain, Pageable pageable);

    @Query("SELECT t.domain, COUNT(t) as count FROM TrafficLog t GROUP BY t.domain ORDER BY count DESC")
    List<Object[]> findTopDomains(Pageable pageable);

    @Query("SELECT t.category.name, COUNT(t) as count FROM TrafficLog t WHERE t.category IS NOT NULL GROUP BY t.category.name")
    List<Object[]> countByCategory();

    @Query("SELECT COUNT(t) FROM TrafficLog t WHERE t.isBlocked = true")
    long countBlockedTraffic();

    @Query("SELECT COUNT(t) FROM TrafficLog t WHERE t.user.id = :userId")
    long countByUserId(@Param("userId") Long userId);

    @Query("SELECT SUM(t.requestSize + t.responseSize) FROM TrafficLog t WHERE t.user.id = :userId")
    Long getTotalBandwidthByUser(@Param("userId") Long userId);

    @Query("SELECT SUM(t.requestSize + t.responseSize) FROM TrafficLog t WHERE t.device.id = :deviceId")
    Long getTotalBandwidthByDevice(@Param("deviceId") Long deviceId);

    @Query("SELECT t FROM TrafficLog t WHERE t.requestTimestamp >= :since ORDER BY t.requestTimestamp DESC")
    List<TrafficLog> findRecentTraffic(@Param("since") LocalDateTime since, Pageable pageable);

    @Query("SELECT COALESCE(SUM(t.requestSize + t.responseSize), 0) FROM TrafficLog t")
    long sumTotalSize();

    void deleteByRequestTimestampBefore(LocalDateTime date);
}
