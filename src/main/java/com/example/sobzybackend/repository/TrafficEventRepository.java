package com.example.sobzybackend.repository;

import com.example.sobzybackend.models.TrafficEvent;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface TrafficEventRepository extends JpaRepository<TrafficEvent, Long> {

    Page<TrafficEvent> findByIpAddressOrderByTimestampDesc(String ipAddress, Pageable pageable);

    List<TrafficEvent> findTop50ByOrderByTimestampDesc();
}
