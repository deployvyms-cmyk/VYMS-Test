package com.vyms.service;

import com.vyms.entity.InventoryRecommendation;
import com.vyms.entity.Sale;
import com.vyms.entity.Vehicle;
import com.vyms.repository.InventoryRecommendationRepository;
import com.vyms.repository.SaleRepository;
import com.vyms.repository.VehicleRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class InventoryAnalyticsService {

    private static final Set<String> KNOWN_BRANDS = new LinkedHashSet<>(Arrays.asList(
        // Japanese brands
        "Toyota", "Honda", "Nissan", "Mazda", "Subaru", "Mitsubishi", "Suzuki",
        "Lexus", "Daihatsu", "Isuzu", "Acura", "Infiniti",
        // Other common brands
        "Jeep", "Ford", "BMW", "Mercedes", "Chevrolet", "Volkswagen",
        "Hyundai", "Kia", "Audi", "Volvo", "Jaguar", "Land", "Range",
        "Peugeot", "Renault", "Fiat", "Ferrari", "Porsche"
    ));

    private final VehicleRepository vehicleRepository;
    private final SaleRepository saleRepository;
    private final InventoryRecommendationRepository recommendationRepository;

    @Autowired
    public InventoryAnalyticsService(VehicleRepository vehicleRepository,
            SaleRepository saleRepository,
            InventoryRecommendationRepository recommendationRepository) {
        this.vehicleRepository = vehicleRepository;
        this.saleRepository = saleRepository;
        this.recommendationRepository = recommendationRepository;
    }

    @Transactional
    public InventoryAnalyticsResult buildInsights() {
        List<Vehicle> vehicles = vehicleRepository.findAll();
        List<Sale> sales = saleRepository.findAll();

        LocalDate cutoff = LocalDate.now().minusMonths(6);
        Map<String, Long> salesByBrand = sales.stream()
            .filter(s -> s.getSaleDate() != null && !s.getSaleDate().isBefore(cutoff))
            .filter(s -> s.getSaleStatus() != null && "FINALIZED".equalsIgnoreCase(s.getSaleStatus()))
            .map(Sale::getVehicle)
            .filter(Objects::nonNull)
            .map(v -> extractBrand(v.getVehicleModel()))
            .collect(Collectors.groupingBy(b -> b, Collectors.counting()));

        Map<String, Long> stockByBrand = vehicles.stream()
            .filter(v -> v.getStatus() == null || !"SOLD".equalsIgnoreCase(v.getStatus()))
            .map(v -> extractBrand(v.getVehicleModel()))
            .collect(Collectors.groupingBy(b -> b, Collectors.counting()));

        LocalDateTime computedAt = LocalDateTime.now();
        List<InventoryRecommendation> recommendations = salesByBrand.entrySet().stream()
            .map(entry -> {
                String brand = entry.getKey();
                long salesCount = entry.getValue();
                long stockCount = stockByBrand.getOrDefault(brand, 0L);
                long score = salesCount - stockCount;
                if (score <= 0) return null;
                int recommended = (int) Math.min(score, 5);
                String reason = "High demand in last 6 months with low current stock.";
                return new InventoryRecommendation(brand, recommended, (int) score, reason, computedAt);
            })
            .filter(Objects::nonNull)
            .sorted(Comparator.comparing(InventoryRecommendation::getScore).reversed())
            .limit(5)
            .collect(Collectors.toList());

        if (!recommendations.isEmpty()) {
            recommendationRepository.saveAll(recommendations);
        }

        List<Map.Entry<String, Long>> topBrands = salesByBrand.entrySet().stream()
            .sorted(Map.Entry.<String, Long>comparingByValue().reversed())
            .limit(8)
            .collect(Collectors.toList());

        List<String> demandLabels = topBrands.stream().map(Map.Entry::getKey).collect(Collectors.toList());
        List<Long> demandData = topBrands.stream().map(Map.Entry::getValue).collect(Collectors.toList());

        return new InventoryAnalyticsResult(recommendations, demandLabels, demandData, computedAt);
    }

    private String extractBrand(String vehicleModel) {
        if (vehicleModel == null || vehicleModel.isBlank()) return "Other";
        String first = vehicleModel.trim().split("[\\s-]+")[0];
        for (String brand : KNOWN_BRANDS) {
            if (brand.equalsIgnoreCase(first)) return brand;
        }
        return first.substring(0, 1).toUpperCase() + first.substring(1).toLowerCase();
    }

    public static class InventoryAnalyticsResult {
        private final List<InventoryRecommendation> recommendations;
        private final List<String> demandLabels;
        private final List<Long> demandData;
        private final LocalDateTime computedAt;

        public InventoryAnalyticsResult(List<InventoryRecommendation> recommendations,
                                        List<String> demandLabels,
                                        List<Long> demandData,
                                        LocalDateTime computedAt) {
            this.recommendations = recommendations;
            this.demandLabels = demandLabels;
            this.demandData = demandData;
            this.computedAt = computedAt;
        }

        public List<InventoryRecommendation> getRecommendations() {
            return recommendations;
        }

        public List<String> getDemandLabels() {
            return demandLabels;
        }

        public List<Long> getDemandData() {
            return demandData;
        }

        public LocalDateTime getComputedAt() {
            return computedAt;
        }
    }
}

