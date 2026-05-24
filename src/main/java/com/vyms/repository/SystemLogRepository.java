package com.vyms.repository;

import com.vyms.entity.SystemLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/**
 * Repository for reading and writing system logs.
 */
@Repository
public interface SystemLogRepository extends JpaRepository<SystemLog, Long> {
}
