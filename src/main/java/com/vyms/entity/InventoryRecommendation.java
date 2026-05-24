package com.vyms.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "inventory_recommendation")
public class InventoryRecommendation {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String brand;

    @Column(name = "recommended_count")
    private Integer recommendedCount;

    private Integer score;

    private String reason;

    @Column(name = "computed_at")
    private LocalDateTime computedAt;

    public InventoryRecommendation() {
    }

    public InventoryRecommendation(String brand, Integer recommendedCount, Integer score, String reason, LocalDateTime computedAt) {
        this.brand = brand;
        this.recommendedCount = recommendedCount;
        this.score = score;
        this.reason = reason;
        this.computedAt = computedAt;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getBrand() {
        return brand;
    }

    public void setBrand(String brand) {
        this.brand = brand;
    }

    public Integer getRecommendedCount() {
        return recommendedCount;
    }

    public void setRecommendedCount(Integer recommendedCount) {
        this.recommendedCount = recommendedCount;
    }

    public Integer getScore() {
        return score;
    }

    public void setScore(Integer score) {
        this.score = score;
    }

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }

    public LocalDateTime getComputedAt() {
        return computedAt;
    }

    public void setComputedAt(LocalDateTime computedAt) {
        this.computedAt = computedAt;
    }
}

