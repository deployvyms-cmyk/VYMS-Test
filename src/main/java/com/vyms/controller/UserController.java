package com.vyms.controller;

import com.vyms.entity.User;
import com.vyms.entity.Vehicle;
import com.vyms.entity.Sale;
import com.vyms.service.UserService;
import com.vyms.service.VehicleService;
import com.vyms.service.SaleService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;

import jakarta.servlet.http.HttpSession;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Controller
public class UserController {

    private final UserService userService;
    private final VehicleService vehicleService;
    private final SaleService saleService;

    @Autowired
    public UserController(UserService userService, VehicleService vehicleService, SaleService saleService) {
        this.userService = userService;
        this.vehicleService = vehicleService;
        this.saleService = saleService;
    }

    @GetMapping("/users")
    public String listUsers(Model model) {
        List<User> users = userService.findAll();
        model.addAttribute("users", users);
        return "user-list";
    }

    @GetMapping("/login")
    public String login(Model model) {
        model.addAttribute("user", new User());
        return "login";
    }

    @PostMapping("/login")
    public String processLogin(@ModelAttribute("user") User user, HttpSession session) {
        Optional<User> authenticatedUser = userService.getAuthenticatedUser(user.getEmail(), user.getPassword());

        if (authenticatedUser.isPresent()) {
            User authed = authenticatedUser.get();
            if (authed.getRole() == null) {
                return "redirect:/login?error";
            }
            session.setAttribute("currentUserId", authed.getId());
            session.setAttribute("currentUserRole", authed.getRole().name());
            session.setAttribute("currentUserName", authed.getUsername());
            String role = authed.getRole().name();

            switch (role.toUpperCase()) {
                case "ADMIN":
                    return "redirect:/admin";
                case "MANAGER":
                    return "redirect:/manager";
                default:
                    return "redirect:/login?error";
            }
        }

        return "redirect:/login?error";
    }

    @GetMapping("/logout")
    public String logout(HttpSession session) {
        session.invalidate();
        return "redirect:/login";
    }

    // --- Dashboard GET Mappings ---

    @GetMapping("/admin")
    public String adminDashboard(Model model) {
        List<Vehicle> vehicles = vehicleService.findAll();
        List<Sale> sales = saleService.findAll();

        long totalVehicles = vehicles.size();
        long totalSales = sales.size();

        BigDecimal totalRevenue = sales.stream()
                .map(s -> s.getSalePrice() != null ? s.getSalePrice() : BigDecimal.ZERO)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal totalProfit = sales.stream()
                .map(s -> s.getProfit() != null ? s.getProfit() : BigDecimal.ZERO)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        // Calculate Cost Breakdown for all sold vehicles
        BigDecimal totalPurchase = sales.stream()
                .map(s -> s.getVehicle() != null && s.getVehicle().getPurchasePrice() != null ? s.getVehicle().getPurchasePrice() : BigDecimal.ZERO)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal totalRepair = sales.stream()
                .map(s -> s.getVehicle() != null && s.getVehicle().getRepairCost() != null ? s.getVehicle().getRepairCost() : BigDecimal.ZERO)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        // Sales Pipeline Status
        long countDraft = sales.stream().filter(s -> "DRAFT".equalsIgnoreCase(s.getSaleStatus())).count();
        long countFinalized = sales.stream().filter(s -> "FINALIZED".equalsIgnoreCase(s.getSaleStatus())).count();

        model.addAttribute("totalVehicles", totalVehicles);
        model.addAttribute("totalSales", totalSales);
        model.addAttribute("totalRevenue", totalRevenue);
        model.addAttribute("totalProfit", totalProfit);
        
        // Chart 1: Financials
        model.addAttribute("chartTotalRevenue", totalRevenue);
        model.addAttribute("chartTotalPurchase", totalPurchase);
        model.addAttribute("chartTotalRepair", totalRepair);

        // Chart 2: Pipeline
        model.addAttribute("countDraft", countDraft);
        model.addAttribute("countFinalized", countFinalized);

        // Recent Sales (latest 8)
        List<Sale> recentSales = sales.stream()
                .sorted(Comparator.comparing(Sale::getSaleDate, Comparator.nullsLast(Comparator.reverseOrder())))
                .limit(8)
                .collect(Collectors.toList());
        model.addAttribute("recentSales", recentSales);

        // Total Users
        model.addAttribute("totalUsers", (long) userService.findAll().size());

        // Brand Stock Share
        Map<String, Long> brandCounts = vehicles.stream()
                .filter(v -> v.getMake() != null && !v.getMake().isBlank())
                .collect(Collectors.groupingBy(Vehicle::getMake, Collectors.counting()));
        model.addAttribute("brandLabels", new ArrayList<>(brandCounts.keySet()));
        model.addAttribute("brandData", new ArrayList<>(brandCounts.values()));

        return "admin";
    }

}
