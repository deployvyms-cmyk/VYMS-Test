package com.vyms.service;

import com.vyms.entity.Sale;
import com.vyms.repository.SaleRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

/**
 * Service layer for sale entity operations.
 */
@Service
public class SaleService {

    private final SaleRepository saleRepository;

    /**
     * Injects sale repository to access database operations.
     */
    @Autowired
    public SaleService(SaleRepository saleRepository) {
        this.saleRepository = saleRepository;
    }

    /**
     * Gets all sale records.
     */
    public List<Sale> findAll() {
        return saleRepository.findAll();
    }

    /**
     * Gets one sale by id.
     */
    public Optional<Sale> findById(Long id) {
        return saleRepository.findById(id);
    }

    /**
     * Persists sale changes.
     */
    public Sale save(Sale sale) {
        return saleRepository.save(sale);
    }

    /**
     * Removes a sale by id.
     */
    public void deleteById(Long id) {
        saleRepository.deleteById(id);
    }

    public boolean existsByVehicleId(Long vehicleId) {
        return saleRepository.existsByVehicle_Id(vehicleId);
    }
}
