package com.vyms.repository;

import com.vyms.entity.InventoryRecommendation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface InventoryRecommendationRepository extends JpaRepository<InventoryRecommendation, Long> {
    List<InventoryRecommendation> findTop10ByComputedAtOrderByScoreDesc(LocalDateTime computedAt);
}

