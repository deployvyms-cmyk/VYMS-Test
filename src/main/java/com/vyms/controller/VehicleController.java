package com.vyms.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

/**
 * Legacy entry point kept to redirect to the manager inventory page.
 */
@Controller
public class VehicleController {

    /**
     * Redirects legacy /vehicles traffic to the manager inventory dashboard.
     */
    @GetMapping("/vehicles")
    public String listVehicles() {
        return "redirect:/manager/inventory";
    }
}
