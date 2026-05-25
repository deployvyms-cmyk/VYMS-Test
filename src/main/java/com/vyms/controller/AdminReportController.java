package com.vyms.controller;

import com.vyms.entity.BuyerType;

import com.vyms.entity.Repair;
import com.vyms.entity.Sale;
import com.vyms.entity.Vehicle;
import com.vyms.service.PdfReportService;
import com.vyms.service.RepairService;
import com.vyms.service.SaleService;
import com.vyms.service.VehicleService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@Controller
@RequestMapping("/admin/reports")
public class AdminReportController {

        private final SaleService saleService;
        private final VehicleService vehicleService;
        private final RepairService repairService;
        private final PdfReportService pdfReportService;

        @Autowired
        public AdminReportController(SaleService saleService,
                        VehicleService vehicleService,
                        RepairService repairService,
                        PdfReportService pdfReportService) {
                this.saleService = saleService;
                this.vehicleService = vehicleService;
                this.repairService = repairService;
                this.pdfReportService = pdfReportService;
        }

        @GetMapping
        public String showReports(Model model) {

                List<Sale> sales = saleService.findAll();
                List<Vehicle> vehicles = vehicleService.findAll();
                List<Repair> repairs = repairService.findAll();

                // ── Sales Metrics ──────────────────────────────────────────────────────
                BigDecimal totalRevenue = sales.stream()
                                .map(s -> s.getSalePrice() != null ? s.getSalePrice() : BigDecimal.ZERO)
                                .reduce(BigDecimal.ZERO, BigDecimal::add);

                BigDecimal totalExpenses = sales.stream()
                                .filter(s -> s.getVehicle() != null || s.getTotalCost() != null)
                                .map(s -> {
                                        if (s.getTotalCost() != null)
                                                return s.getTotalCost();
                                        Vehicle v = s.getVehicle();
                                        BigDecimal purchase = v.getPurchasePrice() != null ? v.getPurchasePrice()
                                                        : BigDecimal.ZERO;
                                        BigDecimal repair = v.getRepairCost() != null ? v.getRepairCost()
                                                        : BigDecimal.ZERO;
                                        return purchase.add(repair);
                                })
                                .reduce(BigDecimal.ZERO, BigDecimal::add);

                BigDecimal totalProfit = sales.stream()
                                .map(Sale::getProfit)
                                .reduce(BigDecimal.ZERO, BigDecimal::add);

                BigDecimal avgProfitPerSale = sales.isEmpty() ? BigDecimal.ZERO
                                : totalProfit.divide(BigDecimal.valueOf(sales.size()), 2, RoundingMode.HALF_UP);

                model.addAttribute("totalRevenue", totalRevenue);
                model.addAttribute("totalExpenses", totalExpenses);
                model.addAttribute("totalProfit", totalProfit);
                model.addAttribute("avgProfitPerSale", avgProfitPerSale);
                model.addAttribute("totalSales", sales.size());

                // ── Inventory Metrics ──────────────────────────────────────────────────
                long totalVehicles = vehicles.size();
                long soldVehicles = vehicles.stream().filter(v -> "SOLD".equalsIgnoreCase(v.getStatus())).count();
                long unsoldVehicles = totalVehicles - soldVehicles;

                BigDecimal totalRepairCost = repairs.stream()
                                .map(r -> r.getCost() != null ? r.getCost() : BigDecimal.ZERO)
                                .reduce(BigDecimal.ZERO, BigDecimal::add);

                model.addAttribute("totalVehicles", totalVehicles);
                model.addAttribute("soldVehicles", soldVehicles);
                model.addAttribute("unsoldVehicles", unsoldVehicles);
                model.addAttribute("totalRepairCost", totalRepairCost);
                model.addAttribute("totalRepairs", repairs.size());

                // ── Buyer Type Breakdown (for pie chart) ──────────────────────────────
                Map<String, Long> buyerTypeCounts = new LinkedHashMap<>();
                for (BuyerType bt : BuyerType.values())
                        buyerTypeCounts.put(bt.name(), 0L);
                sales.stream()
                                .filter(s -> s.getBuyerType() != null)
                                .forEach(s -> buyerTypeCounts.merge(s.getBuyerType().name(), 1L, Long::sum));

                model.addAttribute("buyerTypeLabels", new ArrayList<>(buyerTypeCounts.keySet()));
                model.addAttribute("buyerTypeValues", new ArrayList<>(buyerTypeCounts.values()));

                // ── Monthly Revenue, Profit & Repair Cost (last 6 months) ────────────
                LocalDate now = LocalDate.now();
                DateTimeFormatter labelFmt = DateTimeFormatter.ofPattern("MMM yyyy");
                List<String> monthLabels = new ArrayList<>();
                List<BigDecimal> monthlyRevenue = new ArrayList<>();
                List<BigDecimal> monthlyProfit = new ArrayList<>();
                List<BigDecimal> monthlyRepairCost = new ArrayList<>();

                for (int i = 5; i >= 0; i--) {
                        LocalDate month = now.minusMonths(i);
                        int y = month.getYear(), m = month.getMonthValue();
                        monthLabels.add(month.format(labelFmt));

                        BigDecimal rev = sales.stream()
                                        .filter(s -> s.getSaleDate() != null
                                                        && s.getSaleDate().getYear() == y
                                                        && s.getSaleDate().getMonthValue() == m)
                                        .map(s -> s.getSalePrice() != null ? s.getSalePrice() : BigDecimal.ZERO)
                                        .reduce(BigDecimal.ZERO, BigDecimal::add);

                        BigDecimal prof = sales.stream()
                                        .filter(s -> s.getSaleDate() != null
                                                        && s.getSaleDate().getYear() == y
                                                        && s.getSaleDate().getMonthValue() == m)
                                        .map(Sale::getProfit)
                                        .reduce(BigDecimal.ZERO, BigDecimal::add);

                        BigDecimal repCost = repairs.stream()
                                        .filter(r -> r.getRepairDate() != null
                                                        && r.getRepairDate().getYear() == y
                                                        && r.getRepairDate().getMonthValue() == m)
                                        .map(r -> r.getCost() != null ? r.getCost() : BigDecimal.ZERO)
                                        .reduce(BigDecimal.ZERO, BigDecimal::add);

                        monthlyRevenue.add(rev);
                        monthlyProfit.add(prof);
                        monthlyRepairCost.add(repCost);
                }

                model.addAttribute("monthLabels", monthLabels);
                model.addAttribute("monthlyRevenue", monthlyRevenue);
                model.addAttribute("monthlyProfit", monthlyProfit);
                model.addAttribute("monthlyRepairCost", monthlyRepairCost);

                // ── Detailed Profit Analysis Table ────────────────────────────────────
                List<Map<String, Object>> profitRows = sales.stream()
                                .filter(s -> s.getVehicle() != null && s.getSalePrice() != null)
                                .map(s -> {
                                        Map<String, Object> row = new LinkedHashMap<>();
                                        Vehicle v = s.getVehicle();
                                        BigDecimal purchase = v.getPurchasePrice() != null ? v.getPurchasePrice()
                                                        : BigDecimal.ZERO;
                                        BigDecimal repair = v.getRepairCost() != null ? v.getRepairCost()
                                                        : BigDecimal.ZERO;
                                        BigDecimal profit = s.getProfit();
                                        BigDecimal totalCost = purchase.add(repair);
                                        BigDecimal margin = totalCost.compareTo(BigDecimal.ZERO) > 0
                                                        ? profit.multiply(BigDecimal.valueOf(100)).divide(
                                                                        s.getSalePrice(), 1, RoundingMode.HALF_UP)
                                                        : BigDecimal.ZERO;
                                        row.put("vehicle", v.getVehicleModel() != null ? v.getVehicleModel() : "—");
                                        row.put("buyerType", s.getBuyerType() != null ? s.getBuyerType().name() : "—");
                                        row.put("purchase", purchase);
                                        row.put("repair", repair);
                                        row.put("salePrice", s.getSalePrice());
                                        row.put("profit", profit);
                                        row.put("margin", margin);
                                        row.put("saleDate", s.getSaleDate() != null ? s.getSaleDate().toString() : "—");
                                        return row;
                                })
                                .collect(Collectors.toList());

                model.addAttribute("profitRows", profitRows);

                // ── Recent Repairs ─────────────────────────────────────────────────────
                List<Repair> recentRepairs = repairs.stream()
                                .filter(r -> r.getRepairDate() != null)
                                .sorted(Comparator.comparing(Repair::getRepairDate).reversed())
                                .limit(5)
                                .collect(Collectors.toList());
                model.addAttribute("recentRepairs", recentRepairs);

                return "admin/reports";
        }

        // =========================================================================
        // Download: Full Business Report PDF
        // =========================================================================
        @GetMapping("/download")
        public ResponseEntity<byte[]> downloadFullReport() {
                List<Sale>    sales    = saleService.findAll();
                List<Vehicle> vehicles = vehicleService.findAll();
                List<Repair>  repairs  = repairService.findAll();
                byte[] pdf = pdfReportService.fullBusinessReport(sales, vehicles, repairs);

                return ResponseEntity.ok()
                                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"full-business-report.pdf\"")
                                .contentType(MediaType.APPLICATION_PDF)
                                .body(pdf);
        }
}



// sri lankan