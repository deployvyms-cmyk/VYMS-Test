package com.vyms.controller;

import com.vyms.service.SystemLogService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

/**
 * Admin controller for viewing activity/system logs.
 *
 * {@code @RequestMapping("/admin/logs")} sets the common URL path for all
 * methods in this class.
 */
@Controller
@RequestMapping("/admin/logs")
public class SystemLogController {

    private final SystemLogService logService;

    /**
     * Injects log service used to read log rows from the database.
     */
    @Autowired
    public SystemLogController(SystemLogService logService) {
        this.logService = logService;
    }

    /**
     * Loads all logs and renders the admin logs page.
     */
    @GetMapping
    public String listLogs(Model model) {
        model.addAttribute("logs", logService.findAllLogs());
        return "admin/logs";
    }
}
