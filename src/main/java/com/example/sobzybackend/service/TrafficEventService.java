package com.example.sobzybackend.service;

import com.example.sobzybackend.models.TrafficEvent;
import com.example.sobzybackend.repository.TrafficEventRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class TrafficEventService {

    private final TrafficEventRepository trafficEventRepository;

    @Transactional
    public TrafficEvent logEvent(TrafficEvent event) {
        if (event.getTimestamp() == null) {
            event.setTimestamp(LocalDateTime.now());
        }
        return trafficEventRepository.save(event);
    }

    @Transactional(readOnly = true)
    public Page<TrafficEvent> getEventsByIp(String ipAddress, Pageable pageable) {
        return trafficEventRepository.findByIpAddressOrderByTimestampDesc(ipAddress, pageable);
    }

    @Transactional(readOnly = true)
    public List<TrafficEvent> getRecentEvents() {
        return trafficEventRepository.findTop50ByOrderByTimestampDesc();
    }
}
