package com.example.sobzybackend.service;

import com.example.sobzybackend.models.BlockedSite;
import com.example.sobzybackend.repository.BlockedSiteRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class BlockedSiteService {
    private final BlockedSiteRepository repository;

    public List<BlockedSite> getAllBlockedSites() {
        return repository.findAll();
    }

    @Transactional
    public BlockedSite addBlockedSite(BlockedSite site) {
        log.info("Adding blocked site: {}", site.getUrl());
        return repository.save(site);
    }

    @Transactional
    public void deleteBlockedSite(Long id) {
        log.info("Deleting blocked site: {}", id);
        repository.deleteById(id);
    }

    @Transactional
    public BlockedSite toggleStatus(Long id) {
        BlockedSite site = repository.findById(id).orElseThrow();
        site.setActive(!site.isActive());
        return repository.save(site);
    }
}
