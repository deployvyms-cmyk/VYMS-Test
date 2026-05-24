package com.vyms.service;

import com.vyms.entity.Vehicle;
import com.vyms.repository.VehicleRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

/**
 * Service class for vehicle operations and validation helpers.
 */
@Service
public class VehicleService {

    private final VehicleRepository vehicleRepository;

    /**
     * Constructor injection for repository dependency.
     */
    @Autowired
    public VehicleService(VehicleRepository vehicleRepository) {
        this.vehicleRepository = vehicleRepository;
    }

    /**
     * Returns all stored vehicles.
     */
    public List<Vehicle> findAll() {
        return vehicleRepository.findAll();
    }

    /**
     * Finds one vehicle by id.
     */
    public Optional<Vehicle> findById(Long id) {
        return vehicleRepository.findById(id);
    }

    /**
     * Saves vehicle changes.
     */
    public Vehicle save(Vehicle vehicle) {
        return vehicleRepository.save(vehicle);
    }

    /**
     * Deletes a vehicle by id.
     */
    public void deleteById(Long id) {
        vehicleRepository.deleteById(id);
    }

    /**
     * Checks duplicate chassis number when creating a vehicle.
     */
    public boolean existsByChassisNumber(String chassisNumber) {
        return chassisNumber != null && vehicleRepository.existsByChassisNumberIgnoreCase(chassisNumber.trim());
    }

    /**
     * Checks duplicate license plate when creating a vehicle.
     */
    public boolean existsByLicensePlate(String licensePlate) {
        return licensePlate != null && vehicleRepository.existsByLicensePlateIgnoreCase(licensePlate.trim());
    }

    /**
     * Checks duplicate chassis number for update flow, excluding current record.
     */
    public boolean existsOtherByChassisNumber(String chassisNumber, Long currentId) {
        if (chassisNumber == null) return false;
        return vehicleRepository.findByChassisNumberIgnoreCase(chassisNumber.trim())
                .map(v -> !v.getId().equals(currentId))
                .orElse(false);
    }

    /**
     * Checks duplicate license plate for update flow, excluding current record.
     */
    public boolean existsOtherByLicensePlate(String licensePlate, Long currentId) {
        if (licensePlate == null) return false;
        return vehicleRepository.findByLicensePlateIgnoreCase(licensePlate.trim())
                .map(v -> !v.getId().equals(currentId))
                .orElse(false);
    }
}
