package com.vyms.service;

import com.vyms.entity.SystemLog;
import com.vyms.repository.SystemLogRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Creates and reads system activity logs.
 */
@Service
public class SystemLogService {

    private final SystemLogRepository systemLogRepository;

    /**
     * Injects the repository used to store logs.
     */
    @Autowired
    public SystemLogService(SystemLogRepository systemLogRepository) {
        this.systemLogRepository = systemLogRepository;
    }

    /**
     * Returns all logs for admin views.
     */
    public List<SystemLog> findAllLogs() {
        return systemLogRepository.findAll();
    }

    /**
     * Creates one log entry for an action.
     */
    public void createLog(String type, String description, String username, String status) {
        SystemLog log = new SystemLog();
        // Save basic event details for audit and troubleshooting.
        log.setType(type);
        log.setDescription(description);
        log.setUser(username);
        log.setTimestamp(LocalDateTime.now());
        log.setStatus(status);
        systemLogRepository.save(log);
    }
}
