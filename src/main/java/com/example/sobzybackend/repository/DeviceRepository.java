package com.example.sobzybackend.repository;


import com.example.sobzybackend.enums.DeviceStatus;
import com.example.sobzybackend.models.Device;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface DeviceRepository extends JpaRepository<Device, Long> {

    Optional<Device> findByMacAddress(String macAddress);

    List<Device> findByUserId(Long userId);

    List<Device> findByStatus(DeviceStatus status);

    List<Device> findByUserIdAndStatus(Long userId, DeviceStatus status);

    @Query("SELECT d FROM Device d WHERE d.ipAddress = :ipAddress")
    List<Device> findByIpAddress(@Param("ipAddress") String ipAddress);

    @Query("SELECT d FROM Device d WHERE d.lastSeen < :date")
    List<Device> findInactiveDevicesSince(@Param("date") LocalDateTime date);

    @Query("SELECT COUNT(d) FROM Device d WHERE d.status = :status")
    long countByStatus(@Param("status") DeviceStatus status);

    @Query("SELECT d FROM Device d WHERE d.user.id = :userId ORDER BY d.totalBandwidthUsed DESC")
    List<Device> findTopBandwidthUsersByUser(@Param("userId") Long userId);

    boolean existsByMacAddress(String macAddress);
}

