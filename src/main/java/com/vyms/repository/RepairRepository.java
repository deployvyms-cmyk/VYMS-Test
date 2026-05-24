package com.vyms.repository;

import com.vyms.entity.Repair;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/**
 * Repository for repair entities.
 *
 * By extending {@code JpaRepository}, this interface gets create/read/update/
 * delete methods without writing SQL manually.
 */
@Repository
public interface RepairRepository extends JpaRepository<Repair, Long> {
	boolean existsByVehicle_Id(Long vehicleId);
}
