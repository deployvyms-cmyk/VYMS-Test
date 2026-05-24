package com.vyms.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

/**
 * Legacy entry point kept to redirect to the manager repair page.
 */
@Controller
public class RepairController {

    /**
     * Redirects legacy /repairs traffic to the manager repair dashboard.
     */
    @GetMapping("/repairs")
    public String listRepairs() {
        return "redirect:/manager/repair";
    }
}
