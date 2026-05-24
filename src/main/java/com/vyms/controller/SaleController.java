package com.vyms.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

/**
 * Legacy entry point kept to redirect to the manager sales page.
 */
@Controller
public class SaleController {

    /**
     * Redirects legacy /sales traffic to the manager sales dashboard.
     */
    @GetMapping("/sales")
    public String listSales() {
        return "redirect:/manager/sales";
    }
}
