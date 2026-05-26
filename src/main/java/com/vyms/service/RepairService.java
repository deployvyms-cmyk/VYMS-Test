package com.vyms.service;

import com.vyms.entity.Repair;
import com.vyms.repository.RepairRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

/**
 * Service class that wraps repair repository operations.
 */
@Service
public class RepairService {

    private final RepairRepository repairRepository;

    /**
     * Injects the repair repository bean managed by Spring.
     */
    @Autowired
    public RepairService(RepairRepository repairRepository) {
        this.repairRepository = repairRepository;
    }

    /**
     * Returns all repair records.
     */
    public List<Repair> findAll() {
        return repairRepository.findAll();
    }

    public List<Repair> findActive() {
        return repairRepository.findAllByDeletedAtIsNull();
    }

    public List<Repair> findDeleted() {
        return repairRepository.findAllByDeletedAtIsNotNull();
    }

    /**
     * Returns one repair by id when present.
     */
    public Optional<Repair> findById(Long id) {
        return repairRepository.findById(id);
    }

    /**
     * Saves a repair record.
     */
    public Repair save(Repair repair) {
        return repairRepository.save(repair);
    }

    /**
     * Deletes a repair by id.
     */
    public void deleteById(Long id) {
        repairRepository.deleteById(id);
    }

    public Repair softDelete(Repair repair, String deletedBy) {
        repair.setDeletedAt(java.time.LocalDateTime.now());
        repair.setDeletedBy(deletedBy);
        return repairRepository.save(repair);
    }

    public boolean existsByVehicleId(Long vehicleId) {
        return repairRepository.existsByVehicle_Id(vehicleId);
    }
}
