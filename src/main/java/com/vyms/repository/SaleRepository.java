package com.vyms.repository;

import com.vyms.entity.Sale;
import com.vyms.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface SaleRepository extends JpaRepository<Sale, Long> {

    boolean existsByVehicle_Id(Long vehicleId);

    /**
     * Fetch FINALIZED sales by a specific seller within a date range (for
     * commissions)
     */
    List<Sale> findBySellerAndSaleDateBetweenAndSaleStatus(
            User seller, LocalDate start, LocalDate end, String saleStatus);
}
