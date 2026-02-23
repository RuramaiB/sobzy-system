package com.example.sobzybackend.repository;

import com.example.sobzybackend.models.BlockedSite;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;

@Repository
public interface BlockedSiteRepository extends JpaRepository<BlockedSite, Long> {
    Optional<BlockedSite> findByUrl(String url);

    List<BlockedSite> findByActive(boolean active);
}
