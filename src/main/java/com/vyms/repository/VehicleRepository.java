package com.vyms.repository;

import com.vyms.entity.Vehicle;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/**
 * Repository for vehicle records and duplicate-check helper queries.
 */
@Repository
public interface VehicleRepository extends JpaRepository<Vehicle, Long> {
	/**
	 * Checks whether a vehicle with this chassis number already exists.
	 */
	boolean existsByChassisNumberIgnoreCase(String chassisNumber);
	/**
	 * Checks whether a vehicle with this license plate already exists.
	 */
	boolean existsByLicensePlateIgnoreCase(String licensePlate);
	/**
	 * Finds a vehicle by chassis number for update-time duplicate validation.
	 */
	java.util.Optional<Vehicle> findByChassisNumberIgnoreCase(String chassisNumber);
	/**
	 * Finds a vehicle by license plate for update-time duplicate validation.
	 */
	java.util.Optional<Vehicle> findByLicensePlateIgnoreCase(String licensePlate);
}
