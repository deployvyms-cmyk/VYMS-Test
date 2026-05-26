package com.vyms.controller;

import com.vyms.entity.*;
import com.vyms.service.*;
import com.vyms.repository.UploadedFileRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.io.IOException;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;
import java.util.regex.Pattern;

@Controller
@RequestMapping("/manager")
public class ManagerController {

    // These patterns protect data quality even if browser validation is bypassed.
    private static final Pattern PHONE_PATTERN = Pattern.compile("^(?:\\+81|0)(?:[\\s-]?\\d){9,10}$");
    private static final Pattern EMAIL_PATTERN = Pattern.compile("^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$");

    private final UserService userService;
    private final VehicleService vehicleService;
    private final SaleService saleService;
    private final RepairService repairService;
    private final PdfReportService pdfReportService;
    private final InventoryAnalyticsService inventoryAnalyticsService;
    private final InvoiceEmailService invoiceEmailService;
    private final UploadedFileRepository uploadedFileRepository;
    private final SystemLogService systemLogService;

    @Value("${app.upload-dir}")
    private String uploadDir;

    @Autowired
    public ManagerController(UserService userService,
            VehicleService vehicleService,
            SaleService saleService,
            RepairService repairService,
            PdfReportService pdfReportService,
            InventoryAnalyticsService inventoryAnalyticsService,
            InvoiceEmailService invoiceEmailService,
            UploadedFileRepository uploadedFileRepository,
            SystemLogService systemLogService) {
        this.userService = userService;
        this.vehicleService = vehicleService;
        this.saleService = saleService;
        this.repairService = repairService;
        this.pdfReportService = pdfReportService;
        this.inventoryAnalyticsService = inventoryAnalyticsService;
        this.invoiceEmailService = invoiceEmailService;
        this.uploadedFileRepository = uploadedFileRepository;
        this.systemLogService = systemLogService;
    }

    private String normalize(String value) {
        return value == null ? null : value.trim();
    }

    private boolean hasText(String value) {
        return value != null && !value.trim().isEmpty();
    }

    private boolean isValidEmail(String email) {
        return !hasText(email) || EMAIL_PATTERN.matcher(email.trim()).matches();
    }

    private boolean isValidPhone(String phone) {
        return hasText(phone) && PHONE_PATTERN.matcher(phone.trim()).matches();
    }

    private boolean isValidPaymentMethod(String method) {
        if (!hasText(method)) {
            return false;
        }
        String normalized = method.trim().toUpperCase();
        return normalized.equals("CASH") || normalized.equals("BANK_TRANSFER");
    }

    private boolean hasInvalidSaleInputs(BuyerType buyerType,
            BigDecimal salePrice,
            String customerName,
            String contactNumber,
            String email,
            String companyName,
            String companyAddress,
            String companyContactNumber,
            String exportCountry,
            String auctionHouseName,
            String location) {
        if (salePrice == null || salePrice.compareTo(BigDecimal.ZERO) <= 0) {
            return true;
        }
        if (buyerType == null) {
            return true;
        }
        if (buyerType == BuyerType.REGULAR_CUSTOMER) {
            return !(hasText(customerName)
                    && customerName.trim().length() >= 2
                    && isValidPhone(contactNumber)
                    && isValidEmail(email));
        }
        if (buyerType == BuyerType.REGULAR_COMPANY) {
            return !(hasText(companyName)
                    && companyName.trim().length() >= 2
                    && hasText(companyAddress)
                    && companyAddress.trim().length() >= 2
                    && isValidPhone(companyContactNumber));
        }
        if (buyerType == BuyerType.EXPORT) {
            return !(hasText(companyName)
                    && hasText(exportCountry)
                    && isValidEmail(email));
        }
        if (buyerType == BuyerType.AUCTION) {
            return !(hasText(auctionHouseName)
                    && hasText(location));
        }
        return true;
    }

    private static final Set<String> KNOWN_BRANDS = new LinkedHashSet<>(Arrays.asList(
        "Toyota", "Honda", "Nissan", "Mazda", "Subaru", "Mitsubishi", "Suzuki",
        "Lexus", "Daihatsu", "Isuzu", "Acura", "Infiniti",
        "Jeep", "Ford", "BMW", "Mercedes", "Chevrolet", "Volkswagen",
        "Hyundai", "Kia", "Audi", "Volvo", "Jaguar", "Land Rover", "Range Rover",
        "Peugeot", "Renault", "Fiat", "Ferrari", "Porsche"
    ));

    private String extractBrand(String vehicleModel) {
        if (vehicleModel == null || vehicleModel.isBlank()) return "Other";
        String normalized = vehicleModel.trim();
        String lower = normalized.toLowerCase();

        String matched = null;
        int matchedWords = 0;
        for (String brand : KNOWN_BRANDS) {
            String brandLower = brand.toLowerCase();
            boolean match = lower.equals(brandLower)
                    || lower.startsWith(brandLower + " ")
                    || lower.startsWith(brandLower + "-");
            if (match) {
                int words = brand.split("\\s+").length;
                if (words > matchedWords) {
                    matched = brand;
                    matchedWords = words;
                }
            }
        }
        if (matched != null) {
            return matched;
        }

        String first = normalized.split("[\\s-]+", 2)[0];
        for (String brand : KNOWN_BRANDS) {
            if (brand.equalsIgnoreCase(first)) return brand;
        }
        return first.substring(0, 1).toUpperCase() + first.substring(1).toLowerCase();
    }

    // =========================================================================
    // Dashboard
    // =========================================================================
    @GetMapping
    public String dashboard(Model model) {
        List<Vehicle> vehicles = vehicleService.findAll();
        List<Sale> sales = saleService.findAll();

        BigDecimal totalRevenue = sales.stream()
                .map(s -> s.getSalePrice() != null ? s.getSalePrice() : BigDecimal.ZERO)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal totalProfit = sales.stream()
                .map(Sale::getProfit)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        long soldCount = vehicles.stream().filter(v -> "SOLD".equalsIgnoreCase(v.getStatus())).count();
        long unsoldCount = vehicles.size() - soldCount;

        BigDecimal totalInvestment = vehicles.stream()
                .filter(v -> !"SOLD".equalsIgnoreCase(v.getStatus()))
                .map(v -> {
                    BigDecimal p = v.getPurchasePrice() != null ? v.getPurchasePrice() : BigDecimal.ZERO;
                    BigDecimal r = v.getRepairCost() != null ? v.getRepairCost() : BigDecimal.ZERO;
                    return p.add(r);
                })
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        Map<String, Long> brandCounts = vehicles.stream()
                .collect(Collectors.groupingBy(v -> extractBrand(v.getVehicleModel()), Collectors.counting()))
                .entrySet().stream()
                .sorted(Map.Entry.<String, Long>comparingByValue().reversed())
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue, (e1, e2) -> e1, LinkedHashMap::new));

        InventoryAnalyticsService.InventoryAnalyticsResult analytics = inventoryAnalyticsService.buildInsights();

        model.addAttribute("totalVehicles", vehicles.size());
        model.addAttribute("totalSales", sales.size());
        model.addAttribute("totalRevenue", totalRevenue);
        model.addAttribute("totalProfit", totalProfit);
        
        model.addAttribute("soldCount", soldCount);
        model.addAttribute("unsoldCount", unsoldCount);
        model.addAttribute("totalInvestment", totalInvestment);

        model.addAttribute("brandLabels", brandCounts.keySet());
        model.addAttribute("brandData", brandCounts.values());

        model.addAttribute("inventoryRecommendations", analytics.getRecommendations());
        model.addAttribute("demandLabels", analytics.getDemandLabels());
        model.addAttribute("demandData", analytics.getDemandData());
        model.addAttribute("recommendationsComputedAt", analytics.getComputedAt());

        model.addAttribute("recentSales", sales.stream()
                .sorted(Comparator.comparing(s -> s.getSaleDate() == null ? LocalDate.MIN : s.getSaleDate(),
                        Comparator.reverseOrder()))
                .limit(5).collect(Collectors.toList()));
        return "manager/dashboard";
    }

    // Attendance features removed for deployment.

    // =========================================================================
    // Inventory
    // =========================================================================
    @GetMapping("/inventory")
    public String inventory(Model model) {
        model.addAttribute("vehicles", vehicleService.findAll());
        return "manager/inventory-reports";
    }

    @PostMapping("/inventory/add")
    public String addVehicle(
            @RequestParam("chassisNumber") String chassisNumber,
            @RequestParam("licensePlate") String licensePlate,
            @RequestParam(name = "vehicleModel", required = false) String vehicleModel,
            @RequestParam(name = "brandTyped", required = false) String brandTyped,
            @RequestParam(name = "modelInput", required = false) String modelInput,
            @RequestParam(name = "purchasePrice", required = false) BigDecimal purchasePrice,
            @RequestParam(name = "buyerName") String buyerName,
            @RequestParam(name = "purchasePaymentMethod") String purchasePaymentMethod,
            @RequestParam(name = "importantNote", required = false) String importantNote,
            @RequestParam(name = "imageFile", required = false) MultipartFile imageFile,
            RedirectAttributes ra) throws IOException {

        String chassis = chassisNumber != null ? chassisNumber.trim().toUpperCase() : "";
        String plate = licensePlate != null ? licensePlate.trim().toUpperCase() : "";
        String normalizedBuyer = normalize(buyerName);
        String normalizedPayment = normalize(purchasePaymentMethod);

        if (vehicleService.existsByChassisNumber(chassis)) {
            ra.addFlashAttribute("errorMsg", "A vehicle with this chassis number already exists.");
            return "redirect:/manager/inventory";
        }
        if (vehicleService.existsByLicensePlate(plate)) {
            ra.addFlashAttribute("errorMsg", "A vehicle with this license plate already exists.");
            return "redirect:/manager/inventory";
        }

        if (!hasText(normalizedBuyer)) {
            ra.addFlashAttribute("errorMsg", "Buyer name is required.");
            return "redirect:/manager/inventory";
        }
        if (!isValidPaymentMethod(normalizedPayment)) {
            ra.addFlashAttribute("errorMsg", "Payment method must be Cash or Bank Transfer.");
            return "redirect:/manager/inventory";
        }

        String brand = brandTyped != null ? brandTyped.trim() : "";
        if (brand.isBlank()) {
            ra.addFlashAttribute("errorMsg", "Vehicle brand is required.");
            return "redirect:/manager/inventory";
        }

        String resolvedModel = resolveVehicleModel(vehicleModel, brandTyped, modelInput, null);
        if (resolvedModel == null || resolvedModel.isBlank()) {
            ra.addFlashAttribute("errorMsg", "Vehicle model is required.");
            return "redirect:/manager/inventory";
        }

        Vehicle v = new Vehicle();
        v.setChassisNumber(chassis);
        v.setLicensePlate(plate);
        v.setVehicleModel(resolvedModel);
        v.setPurchasePrice(purchasePrice != null ? purchasePrice : BigDecimal.ZERO);
        v.setBuyerName(normalizedBuyer);
        v.setPurchasePaymentMethod(normalizedPayment.toUpperCase());
        v.setRepairCost(BigDecimal.ZERO);
        v.setStatus("UNSOLD");
        v.setAddedDate(LocalDate.now());
        v.setImportantNote(hasText(importantNote) ? importantNote.trim() : null);

        if (imageFile != null && !imageFile.isEmpty()) {
            String imagePath = saveImage(imageFile);
            v.setImagePath(imagePath);
        }

        vehicleService.save(v);
        ra.addFlashAttribute("successMsg", "Vehicle added successfully.");
        return "redirect:/manager/inventory";
    }

    @GetMapping("/inventory/edit/{id}")
    public String editVehicleForm(@PathVariable("id") Long id, Model model) {
        Optional<Vehicle> vehicleOpt = vehicleService.findById(id);
        if (vehicleOpt.isPresent()) {
            model.addAttribute("vehicle", vehicleOpt.get());
            return "manager/vehicle-edit";
        }
        return "redirect:/manager/inventory";
    }

    @PostMapping("/inventory/edit/{id}")
    public String updateVehicle(
            @PathVariable("id") Long id,
            @RequestParam("chassisNumber") String chassisNumber,
            @RequestParam("licensePlate") String licensePlate,
            @RequestParam(name = "vehicleModel", required = false) String vehicleModel,
            @RequestParam(name = "brandTyped", required = false) String brandTyped,
            @RequestParam(name = "modelInput", required = false) String modelInput,
            @RequestParam(name = "purchasePrice", required = false) BigDecimal purchasePrice,
            @RequestParam(name = "repairCost", required = false) BigDecimal repairCost,
            @RequestParam(name = "salePrice", required = false) BigDecimal salePrice,
            @RequestParam(name = "status", required = false) String status,
            @RequestParam(name = "buyerName") String buyerName,
            @RequestParam(name = "purchasePaymentMethod") String purchasePaymentMethod,
            @RequestParam(name = "importantNote", required = false) String importantNote,
            @RequestParam(name = "imageFile", required = false) MultipartFile imageFile,
            RedirectAttributes ra) {

        String chassis = chassisNumber != null ? chassisNumber.trim().toUpperCase() : "";
        String plate = licensePlate != null ? licensePlate.trim().toUpperCase() : "";
        String normalizedBuyer = normalize(buyerName);
        String normalizedPayment = normalize(purchasePaymentMethod);

        Optional<Vehicle> vehicleOpt = vehicleService.findById(id);
        if (vehicleOpt.isEmpty()) {
            ra.addFlashAttribute("errorMsg", "Vehicle not found.");
            return "redirect:/manager/inventory";
        }

        Vehicle current = vehicleOpt.get();
        if ("SOLD".equalsIgnoreCase(current.getStatus())) {
            ra.addFlashAttribute("errorMsg", "Sold vehicles are locked and cannot be edited.");
            return "redirect:/manager/inventory/edit/" + id;
        }

        if (!hasText(normalizedBuyer)) {
            ra.addFlashAttribute("errorMsg", "Buyer name is required.");
            return "redirect:/manager/inventory/edit/" + id;
        }
        if (!isValidPaymentMethod(normalizedPayment)) {
            ra.addFlashAttribute("errorMsg", "Payment method must be Cash or Bank Transfer.");
            return "redirect:/manager/inventory/edit/" + id;
        }

        if (vehicleService.existsOtherByChassisNumber(chassis, id)) {
            ra.addFlashAttribute("errorMsg", "Another vehicle already has this chassis number.");
            return "redirect:/manager/inventory";
        }
        if (vehicleService.existsOtherByLicensePlate(plate, id)) {
            ra.addFlashAttribute("errorMsg", "Another vehicle already has this license plate.");
            return "redirect:/manager/inventory";
        }

        try {
            Vehicle v = current;
            String resolvedModel = resolveVehicleModel(vehicleModel, brandTyped, modelInput, v.getVehicleModel());
            if (resolvedModel == null || resolvedModel.isBlank()) {
                ra.addFlashAttribute("errorMsg", "Vehicle brand/model cannot be empty.");
                return "redirect:/manager/inventory/edit/" + id;
            }

            v.setChassisNumber(chassis);
            v.setLicensePlate(plate);
            v.setVehicleModel(resolvedModel);
            v.setPurchasePrice(purchasePrice != null ? purchasePrice : BigDecimal.ZERO);
            v.setBuyerName(normalizedBuyer);
            v.setPurchasePaymentMethod(normalizedPayment.toUpperCase());
            v.setRepairCost(repairCost != null ? repairCost : BigDecimal.ZERO);
            v.setSalePrice(salePrice);
            if (status != null)
                v.setStatus(status);
            v.setImportantNote(hasText(importantNote) ? importantNote.trim() : null);

            if (imageFile != null && !imageFile.isEmpty()) {
                String imagePath = saveImage(imageFile);
                v.setImagePath(imagePath);
            }

            vehicleService.save(v);
            ra.addFlashAttribute("successMsg", "Vehicle updated successfully.");
            return "redirect:/manager/inventory";
        } catch (IOException ex) {
            ra.addFlashAttribute("errorMsg", "Image upload failed while updating vehicle.");
            return "redirect:/manager/inventory/edit/" + id;
        } catch (RuntimeException ex) {
            ra.addFlashAttribute("errorMsg", "Update failed. Please check the entered details and try again.");
            return "redirect:/manager/inventory/edit/" + id;
        }
    }

    @GetMapping("/inventory/delete/{id}")
    public String deleteVehicle(@PathVariable("id") Long id, RedirectAttributes ra) {
        if (vehicleService.findById(id).isEmpty()) {
            ra.addFlashAttribute("errorMsg", "Vehicle not found or already deleted.");
            return "redirect:/manager/inventory";
        }

        if (saleService.existsByVehicleId(id)) {
            ra.addFlashAttribute("errorMsg", "Cannot delete this vehicle because it has a sale record.");
            return "redirect:/manager/inventory";
        }
        if (repairService.existsByVehicleId(id)) {
            ra.addFlashAttribute("errorMsg", "Cannot delete this vehicle because repair records exist.");
            return "redirect:/manager/inventory";
        }

        try {
            vehicleService.deleteById(id);
            ra.addFlashAttribute("successMsg", "Vehicle deleted successfully.");
        } catch (org.springframework.dao.DataIntegrityViolationException ex) {
            ra.addFlashAttribute("errorMsg", "Vehicle cannot be deleted because it is linked to other records.");
        } catch (RuntimeException ex) {
            ra.addFlashAttribute("errorMsg", "Delete failed. Please try again.");
        }
        return "redirect:/manager/inventory";
    }

    @GetMapping("/inventory/download")
    public ResponseEntity<byte[]> downloadInventoryReport() {
        List<Vehicle> vehicles = vehicleService.findAll();
        byte[] pdf = pdfReportService.inventoryStockReport(vehicles);
        return ResponseEntity.ok()
            .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=inventory-stock-report.pdf")
            .contentType(MediaType.APPLICATION_PDF)
            .body(pdf);
    }

    private String saveImage(MultipartFile imageFile) throws IOException {
        String ext = "";
        String original = imageFile.getOriginalFilename();
        if (original != null && original.contains(".")) {
            ext = original.substring(original.lastIndexOf("."));
        }
        String filename = UUID.randomUUID() + ext;

        UploadedFile file = new UploadedFile();
        file.setFilename(filename);
        file.setContentType(imageFile.getContentType());
        file.setFileData(imageFile.getBytes());
        uploadedFileRepository.save(file);

        return "/uploads/" + filename;
    }


    private String resolveVehicleModel(String hiddenModel, String brandTyped, String modelInput, String fallback) {
        String hidden = hiddenModel != null ? hiddenModel.trim() : "";
        if (!hidden.isBlank()) {
            return hidden;
        }

        String brand = brandTyped != null ? brandTyped.trim() : "";
        String model = modelInput != null ? modelInput.trim() : "";
        if (!brand.isBlank() && !model.isBlank()) {
            return brand + " " + model;
        }
        if (!brand.isBlank()) {
            return brand;
        }
        if (!model.isBlank()) {
            return model;
        }
        return fallback;
    }

    // =========================================================================
    // Repair & Maintenance
    // =========================================================================
    @GetMapping("/repair")
    public String repair(Model model, @RequestParam(name = "vehicleId", required = false) Long vehicleId) {
        List<Repair> repairs = repairService.findActive();
        repairs.sort(Comparator
                .comparing((Repair r) -> r.getVehicle() != null && "SOLD".equalsIgnoreCase(r.getVehicle().getStatus()))
                .thenComparing(Repair::getRepairDate, Comparator.nullsLast(Comparator.reverseOrder()))
                .thenComparing(Repair::getId, Comparator.nullsLast(Comparator.reverseOrder())));
        List<Vehicle> vehicles = vehicleService.findAll().stream()
                .filter(v -> !"SOLD".equalsIgnoreCase(v.getStatus()))
                .collect(Collectors.toList());

        long totalRepairs = repairs.size();
        long pendingRepairs = repairs.stream().filter(r -> "PENDING".equalsIgnoreCase(r.getStatus())).count();
        long inspectedRepairs = repairs.stream().filter(r -> "INSPECTED".equalsIgnoreCase(r.getStatus())).count();
        BigDecimal totalRepairCost = repairs.stream()
                .map(r -> r.getCost() != null ? r.getCost() : BigDecimal.ZERO)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        model.addAttribute("repairs", repairs);
        model.addAttribute("vehicles", vehicles);
        model.addAttribute("totalRepairs", totalRepairs);
        model.addAttribute("pendingRepairs", pendingRepairs);
        model.addAttribute("inspectedRepairs", inspectedRepairs);
        model.addAttribute("totalRepairCost", totalRepairCost);
        model.addAttribute("selectedVehicleId", vehicleId);
        return "manager/repair";
    }

    @PostMapping("/repair/log")
    public String logRepair(
            @RequestParam("vehicleId") Long vehicleId,
            @RequestParam("description") String description,
            @RequestParam("cost") BigDecimal cost,
            @RequestParam("repairType") String repairType,
            RedirectAttributes ra) {

        Optional<Vehicle> vehicleOpt = vehicleService.findById(vehicleId);
        if (vehicleOpt.isEmpty()) {
            ra.addFlashAttribute("errorMsg", "Vehicle not found.");
            return "redirect:/manager/repair";
        }

        Vehicle vehicle = vehicleOpt.get();
        if ("SOLD".equalsIgnoreCase(vehicle.getStatus())) {
            ra.addFlashAttribute("errorMsg", "Sold vehicles are locked and cannot receive new repairs.");
            return "redirect:/manager/repair";
        }

        Repair repair = new Repair();
        repair.setVehicle(vehicle);
        repair.setDescription(description);
        repair.setCost(cost);
        repair.setRepairType(repairType.toUpperCase());
        repair.setStatus("PENDING");
        repair.setRepairDate(LocalDate.now());
        repairService.save(repair);

        BigDecimal current = vehicle.getRepairCost() != null ? vehicle.getRepairCost() : BigDecimal.ZERO;
        vehicle.setRepairCost(current.add(cost));
        vehicleService.save(vehicle);
        return "redirect:/manager/repair";
    }

    @GetMapping("/repair/inspect/{id}")
    public String markInspected(@PathVariable("id") Long id, RedirectAttributes ra) {
        Optional<Repair> repOpt = repairService.findById(id);
        repOpt.ifPresent(r -> {
            if (r.getDeletedAt() != null) {
                ra.addFlashAttribute("errorMsg", "This repair record was deleted.");
                return;
            }
            if (r.getVehicle() != null && "SOLD".equalsIgnoreCase(r.getVehicle().getStatus())) {
                ra.addFlashAttribute("errorMsg", "Sold vehicle repairs are locked.");
                return;
            }
            r.setStatus("INSPECTED");
            repairService.save(r);
        });
        return "redirect:/manager/repair";
    }

    @PostMapping("/repair/edit/{id}")
    public String editRepairFromRepairPage(@PathVariable("id") Long id,
            @RequestParam("description") String description,
            @RequestParam("cost") BigDecimal cost,
            @RequestParam("repairType") String repairType,
            @RequestParam("status") String status,
            RedirectAttributes ra) {
        Optional<Repair> repOpt = repairService.findById(id);
        repOpt.ifPresent(r -> {
            if (r.getDeletedAt() != null) {
                ra.addFlashAttribute("errorMsg", "This repair record was deleted.");
                return;
            }
            if (r.getVehicle() != null && "SOLD".equalsIgnoreCase(r.getVehicle().getStatus())) {
                ra.addFlashAttribute("errorMsg", "Sold vehicle repairs are locked.");
                return;
            }
            BigDecimal oldCost = r.getCost() != null ? r.getCost() : BigDecimal.ZERO;
            BigDecimal newCost = cost != null ? cost : BigDecimal.ZERO;

            r.setDescription(description);
            r.setCost(newCost);
            r.setRepairType(repairType);
            r.setStatus(status);
            repairService.save(r);

            if (r.getVehicle() != null) {
                Vehicle v = r.getVehicle();
                BigDecimal currentTotal = v.getRepairCost() != null ? v.getRepairCost() : BigDecimal.ZERO;
                v.setRepairCost(currentTotal.subtract(oldCost).add(newCost));
                vehicleService.save(v);
            }
        });
        return "redirect:/manager/repair";
    }

    @PostMapping("/repair/delete/{id}")
    public String deleteRepair(@PathVariable("id") Long id,
            jakarta.servlet.http.HttpSession session,
            RedirectAttributes ra) {
        Optional<Repair> repOpt = repairService.findById(id);
        if (repOpt.isEmpty()) {
            ra.addFlashAttribute("errorMsg", "Repair record not found.");
            return "redirect:/manager/repair";
        }

        Repair repair = repOpt.get();
        if (repair.getDeletedAt() != null) {
            ra.addFlashAttribute("errorMsg", "This repair record was already deleted.");
            return "redirect:/manager/repair";
        }
        if (repair.getVehicle() != null && "SOLD".equalsIgnoreCase(repair.getVehicle().getStatus())) {
            ra.addFlashAttribute("errorMsg", "Sold vehicle repairs are locked.");
            return "redirect:/manager/repair";
        }

        String username = Optional.ofNullable(session.getAttribute("currentUserName"))
                .map(Object::toString)
                .filter(name -> !name.isBlank())
                .orElse("Unknown");

        repairService.softDelete(repair, username);

        Vehicle vehicle = repair.getVehicle();
        if (vehicle != null) {
            BigDecimal costValue = repair.getCost() != null ? repair.getCost() : BigDecimal.ZERO;
            BigDecimal current = vehicle.getRepairCost() != null ? vehicle.getRepairCost() : BigDecimal.ZERO;
            BigDecimal updated = current.subtract(costValue);
            if (updated.compareTo(BigDecimal.ZERO) < 0) {
                updated = BigDecimal.ZERO;
            }
            vehicle.setRepairCost(updated);
            vehicleService.save(vehicle);
        }

        String vehicleLabel = repair.getVehicle() != null && repair.getVehicle().getVehicleModel() != null
                ? repair.getVehicle().getVehicleModel()
                : "Unknown vehicle";
        systemLogService.createLog(
                "REPAIR_DELETED",
                "Repair record " + repair.getId() + " deleted for " + vehicleLabel,
                username,
                "SUCCESS");
        ra.addFlashAttribute("successMsg", "Repair record deleted.");
        return "redirect:/manager/repair";
    }

    @GetMapping("/repair/download")
    public ResponseEntity<byte[]> downloadRepairReport() {
        List<Repair> repairs = repairService.findActive();
        byte[] pdf = pdfReportService.repairReport(repairs);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"repair-report.pdf\"")
                .contentType(MediaType.APPLICATION_PDF)
                .body(pdf);
    }

    // =========================================================================
    // Reports
    // =========================================================================
    @GetMapping("/reports")
    public String reports(Model model) {
        List<Sale> sales = saleService.findAll();
        List<Vehicle> vehicles = vehicleService.findAll();
        List<Repair> repairs = repairService.findActive();

        BigDecimal totalRevenue = sales.stream()
                .map(s -> s.getSalePrice() != null ? s.getSalePrice() : BigDecimal.ZERO)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal totalExpenses = sales.stream()
                .filter(s -> s.getVehicle() != null || s.getTotalCost() != null)
                .map(s -> {
                    if (s.getTotalCost() != null)
                        return s.getTotalCost();
                    Vehicle v = s.getVehicle();
                    return (v.getPurchasePrice() != null ? v.getPurchasePrice() : BigDecimal.ZERO)
                            .add(v.getRepairCost() != null ? v.getRepairCost() : BigDecimal.ZERO);
                })
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal totalProfit = sales.stream().map(Sale::getProfit).reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal avgProfitPerSale = sales.isEmpty() ? BigDecimal.ZERO
                : totalProfit.divide(BigDecimal.valueOf(sales.size()), 2, java.math.RoundingMode.HALF_UP);

        model.addAttribute("totalRevenue", totalRevenue);
        model.addAttribute("totalExpenses", totalExpenses);
        model.addAttribute("totalProfit", totalProfit);
        model.addAttribute("avgProfitPerSale", avgProfitPerSale);
        model.addAttribute("totalSales", sales.size());

        long totalVehicles = vehicles.size();
        long soldVehicles = vehicles.stream().filter(v -> "SOLD".equalsIgnoreCase(v.getStatus())).count();
        BigDecimal totalRepairCost = repairs.stream()
                .map(r -> r.getCost() != null ? r.getCost() : BigDecimal.ZERO)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        model.addAttribute("totalVehicles", totalVehicles);
        model.addAttribute("soldVehicles", soldVehicles);
        model.addAttribute("unsoldVehicles", totalVehicles - soldVehicles);
        model.addAttribute("totalRepairCost", totalRepairCost);
        model.addAttribute("totalRepairs", repairs.size());

        Map<String, Long> buyerTypeCounts = new LinkedHashMap<>();
        for (BuyerType bt : BuyerType.values())
            buyerTypeCounts.put(bt.name(), 0L);
        sales.stream().filter(s -> s.getBuyerType() != null)
                .forEach(s -> buyerTypeCounts.merge(s.getBuyerType().name(), 1L, Long::sum));
        model.addAttribute("buyerTypeLabels", new ArrayList<>(buyerTypeCounts.keySet()));
        model.addAttribute("buyerTypeValues", new ArrayList<>(buyerTypeCounts.values()));
        model.addAttribute("buyerTypes", BuyerType.values());

        LocalDate now = LocalDate.now();
        java.time.format.DateTimeFormatter labelFmt = java.time.format.DateTimeFormatter.ofPattern("MMM yyyy");
        List<String> monthLabels = new ArrayList<>();
        List<BigDecimal> monthlyRevenue = new ArrayList<>(), monthlyProfit = new ArrayList<>(),
                monthlyRepairCost = new ArrayList<>();
        for (int i = 5; i >= 0; i--) {
            LocalDate month = now.minusMonths(i);
            int y = month.getYear(), m = month.getMonthValue();
            monthLabels.add(month.format(labelFmt));
            monthlyRevenue.add(sales.stream()
                    .filter(s -> s.getSaleDate() != null && s.getSaleDate().getYear() == y
                            && s.getSaleDate().getMonthValue() == m)
                    .map(s -> s.getSalePrice() != null ? s.getSalePrice() : BigDecimal.ZERO)
                    .reduce(BigDecimal.ZERO, BigDecimal::add));
            monthlyProfit.add(sales.stream()
                    .filter(s -> s.getSaleDate() != null && s.getSaleDate().getYear() == y
                            && s.getSaleDate().getMonthValue() == m)
                    .map(Sale::getProfit).reduce(BigDecimal.ZERO, BigDecimal::add));
            monthlyRepairCost.add(repairs.stream()
                    .filter(r -> r.getRepairDate() != null && r.getRepairDate().getYear() == y
                            && r.getRepairDate().getMonthValue() == m)
                    .map(r -> r.getCost() != null ? r.getCost() : BigDecimal.ZERO)
                    .reduce(BigDecimal.ZERO, BigDecimal::add));
        }
        model.addAttribute("monthLabels", monthLabels);
        model.addAttribute("monthlyRevenue", monthlyRevenue);
        model.addAttribute("monthlyProfit", monthlyProfit);
        model.addAttribute("monthlyRepairCost", monthlyRepairCost);

        List<Map<String, Object>> profitRows = sales.stream()
                .filter(s -> s.getVehicle() != null && s.getSalePrice() != null)
                .map(s -> {
                    Map<String, Object> row = new LinkedHashMap<>();
                    Vehicle v = s.getVehicle();
                    BigDecimal profit = s.getProfit();
                    BigDecimal margin = s.getSalePrice().compareTo(BigDecimal.ZERO) > 0
                            ? profit.multiply(BigDecimal.valueOf(100)).divide(s.getSalePrice(), 1,
                                    java.math.RoundingMode.HALF_UP)
                            : BigDecimal.ZERO;
                    row.put("vehicle", v.getVehicleModel() != null ? v.getVehicleModel() : "—");
                    row.put("buyerType", s.getBuyerType() != null ? s.getBuyerType().name() : "—");
                    row.put("purchase", v.getPurchasePrice() != null ? v.getPurchasePrice() : BigDecimal.ZERO);
                    row.put("repair", v.getRepairCost() != null ? v.getRepairCost() : BigDecimal.ZERO);
                    row.put("salePrice", s.getSalePrice());
                    row.put("profit", profit);
                    row.put("margin", margin);
                    row.put("saleDate", s.getSaleDate() != null ? s.getSaleDate().toString() : "—");
                    return row;
                }).collect(Collectors.toList());
        model.addAttribute("profitRows", profitRows);

        List<Repair> recentRepairs = repairs.stream()
                .filter(r -> r.getRepairDate() != null)
                .sorted(Comparator.comparing(Repair::getRepairDate).reversed())
                .limit(5).collect(Collectors.toList());
        model.addAttribute("recentRepairs", recentRepairs);
        model.addAttribute("repairs", repairs);
        model.addAttribute("sales", sales);
        return "manager/mechanic-sale-reports";
    }

    @PostMapping("/reports/repair/edit/{id}")
    public String editRepair(@PathVariable("id") Long id,
            @RequestParam("description") String description,
            @RequestParam("cost") BigDecimal cost,
            @RequestParam("repairType") String repairType,
            @RequestParam("status") String status) {
        Optional<Repair> repOpt = repairService.findById(id);
        repOpt.ifPresent(r -> {
            r.setDescription(description);
            r.setCost(cost);
            r.setRepairType(repairType);
            r.setStatus(status);
            repairService.save(r);
        });
        return "redirect:/manager/reports";
    }

    @PostMapping("/reports/sale/edit/{id}")
    public String editSaleFromReports(@PathVariable("id") Long id,
            @RequestParam("salePrice") BigDecimal salePrice,
            @RequestParam("saleStatus") String saleStatus,
            @RequestParam("buyerType") String buyerType,
            @RequestParam("customerName") String customerName) {
        Optional<Sale> saleOpt = saleService.findById(id);
        saleOpt.ifPresent(s -> {
            s.setSalePrice(salePrice);
            s.setSaleStatus(saleStatus);
            s.setBuyerType(BuyerType.valueOf(buyerType));
            s.setCustomerName(customerName);
            saleService.save(s);
        });
        return "redirect:/manager/reports";
    }

    // =========================================================================
    // Sales Management
    // =========================================================================
    @GetMapping("/sales")
    public String salesManagement(Model model) {
        List<Sale> sales = saleService.findAll();
        List<Vehicle> vehicles = vehicleService.findAll();
        List<Vehicle> unsold = vehicles.stream().filter(v -> !"SOLD".equalsIgnoreCase(v.getStatus()))
                .collect(Collectors.toList());
        BigDecimal totalRevenue = sales.stream()
                .map(s -> s.getSalePrice() != null ? s.getSalePrice() : BigDecimal.ZERO)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        model.addAttribute("sales", sales);
        model.addAttribute("vehicles", unsold);
        model.addAttribute("buyerTypes", BuyerType.values());
        model.addAttribute("totalSales", sales.size());
        model.addAttribute("totalRevenue", totalRevenue);
        model.addAttribute("availableVehicles", unsold.size());
        return "manager/sales-management";
    }

    @PostMapping("/sales/record")
    public String recordSale(@RequestParam("vehicleId") Long vehicleId,
            @RequestParam("salePrice") BigDecimal salePrice,
            @RequestParam("buyerType") String buyerType,
            @RequestParam(name = "customerName", required = false) String customerName,
            @RequestParam(name = "contactNumber", required = false) String contactNumber,
            @RequestParam(name = "email", required = false) String email,
            @RequestParam(name = "companyName", required = false) String companyName,
            @RequestParam(name = "companyAddress", required = false) String companyAddress,
            @RequestParam(name = "companyContactNumber", required = false) String companyContactNumber,
            @RequestParam(name = "exportCountry", required = false) String exportCountry,
            @RequestParam(name = "auctionHouseName", required = false) String auctionHouseName,
            @RequestParam(name = "location", required = false) String location,
            @RequestParam(name = "lotNumber", required = false) String lotNumber) {
        BuyerType resolvedBuyerType;
        try {
            resolvedBuyerType = BuyerType.valueOf(buyerType);
        } catch (IllegalArgumentException ex) {
            return "redirect:/manager/sales";
        }

        if (hasInvalidSaleInputs(resolvedBuyerType, salePrice, customerName, contactNumber, email,
                companyName, companyAddress, companyContactNumber, exportCountry, auctionHouseName, location)) {
            return "redirect:/manager/sales";
        }

        Optional<Vehicle> vehicleOpt = vehicleService.findById(vehicleId);
        if (vehicleOpt.isPresent()) {
            Vehicle v = vehicleOpt.get();
            BigDecimal snapshotCost = (v.getPurchasePrice() != null ? v.getPurchasePrice() : BigDecimal.ZERO)
                    .add(v.getRepairCost() != null ? v.getRepairCost() : BigDecimal.ZERO);
            Sale sale = new Sale();
            sale.setVehicle(v);
            sale.setSalePrice(salePrice);
            sale.setBuyerType(resolvedBuyerType);
            sale.setSaleDate(LocalDate.now());
            sale.setSaleStatus("FINALIZED");
            sale.setTotalCost(snapshotCost);

            if (resolvedBuyerType == BuyerType.REGULAR_CUSTOMER) {
                sale.setCustomerName(normalize(customerName));
                sale.setContactNumber(normalize(contactNumber));
                sale.setEmail(normalize(email));
            } else if (resolvedBuyerType == BuyerType.REGULAR_COMPANY) {
                sale.setCompanyName(normalize(companyName));
                sale.setCompanyAddress(normalize(companyAddress));
                sale.setCompanyContactNumber(normalize(companyContactNumber));
            } else if (resolvedBuyerType == BuyerType.EXPORT) {
                sale.setCompanyName(normalize(companyName));
                sale.setExportCountry(normalize(exportCountry));
                sale.setEmail(normalize(email));
            } else if (resolvedBuyerType == BuyerType.AUCTION) {
                sale.setAuctionHouseName(normalize(auctionHouseName));
                sale.setLocation(normalize(location));
                sale.setLotNumber(normalize(lotNumber));
            }

            v.setStatus("SOLD");
            v.setSalePrice(salePrice);
            vehicleService.save(v);
            saleService.save(sale);
        }
        return "redirect:/manager/sales";
    }

    @PostMapping("/sales/edit/{id}")
    public String editSale(@PathVariable("id") Long id,
            @RequestParam("salePrice") BigDecimal salePrice,
            @RequestParam("buyerType") String buyerType,
            @RequestParam(name = "customerName", required = false) String customerName,
            @RequestParam(name = "contactNumber", required = false) String contactNumber,
            @RequestParam(name = "email", required = false) String email,
            @RequestParam(name = "companyName", required = false) String companyName,
            @RequestParam(name = "companyAddress", required = false) String companyAddress,
            @RequestParam(name = "companyContactNumber", required = false) String companyContactNumber,
            @RequestParam(name = "exportCountry", required = false) String exportCountry,
            @RequestParam(name = "auctionHouseName", required = false) String auctionHouseName,
            @RequestParam(name = "location", required = false) String location,
            @RequestParam(name = "lotNumber", required = false) String lotNumber,
            @RequestParam(name = "saleStatus", required = false) String saleStatus) {
        BuyerType resolvedBuyerType;
        try {
            resolvedBuyerType = BuyerType.valueOf(buyerType);
        } catch (IllegalArgumentException ex) {
            return "redirect:/manager/sales";
        }

        if (hasInvalidSaleInputs(resolvedBuyerType, salePrice, customerName, contactNumber, email,
                companyName, companyAddress, companyContactNumber, exportCountry, auctionHouseName, location)) {
            return "redirect:/manager/sales";
        }

        Optional<Sale> saleOpt = saleService.findById(id);
        saleOpt.ifPresent(s -> {
            s.setSalePrice(salePrice);
            s.setBuyerType(resolvedBuyerType);

            // Clear stale values so table data always matches selected buyer type.
            s.setCustomerName(null);
            s.setContactNumber(null);
            s.setEmail(null);
            s.setCompanyName(null);
            s.setCompanyAddress(null);
            s.setCompanyContactNumber(null);
            s.setExportCountry(null);
            s.setAuctionHouseName(null);
            s.setLocation(null);
            s.setLotNumber(null);

            if (resolvedBuyerType == BuyerType.REGULAR_CUSTOMER) {
                s.setCustomerName(normalize(customerName));
                s.setContactNumber(normalize(contactNumber));
                s.setEmail(normalize(email));
            } else if (resolvedBuyerType == BuyerType.REGULAR_COMPANY) {
                s.setCompanyName(normalize(companyName));
                s.setCompanyAddress(normalize(companyAddress));
                s.setCompanyContactNumber(normalize(companyContactNumber));
            } else if (resolvedBuyerType == BuyerType.EXPORT) {
                s.setCompanyName(normalize(companyName));
                s.setExportCountry(normalize(exportCountry));
                s.setEmail(normalize(email));
            } else if (resolvedBuyerType == BuyerType.AUCTION) {
                s.setAuctionHouseName(normalize(auctionHouseName));
                s.setLocation(normalize(location));
                s.setLotNumber(normalize(lotNumber));
            }

            if (saleStatus != null)
                s.setSaleStatus(saleStatus);
            if (s.getVehicle() != null) {
                s.getVehicle().setSalePrice(salePrice);
                vehicleService.save(s.getVehicle());
            }
            saleService.save(s);
        });
        return "redirect:/manager/sales";
    }

    @GetMapping("/sales/invoice/{id}")
    public String invoice(@PathVariable("id") Long id, Model model) {
        Optional<Sale> saleOpt = saleService.findById(id);
        if (saleOpt.isPresent()) {
            model.addAttribute("sale", saleOpt.get());
            return "sales/invoice";
        }
        return "redirect:/manager/sales";
    }

    @GetMapping("/sales/invoice/{id}/send")
    public String markInvoiceSent(@PathVariable("id") Long id) {
        Optional<Sale> saleOpt = saleService.findById(id);
        if (saleOpt.isEmpty()) {
            return "redirect:/manager/sales";
        }

        Sale sale = saleOpt.get();
        if (sale.getBuyerType() == BuyerType.AUCTION) {
            return "redirect:/manager/sales/invoice/" + id + "?sendError=auction";
        }
        if (sale.getEmail() == null || sale.getEmail().isBlank()) {
            return "redirect:/manager/sales/invoice/" + id + "?sendError=missing_email";
        }

        InvoiceSendResult result = invoiceEmailService.sendInvoiceEmail(sale);
        if (!result.success()) {
            try {
                String encoded = java.net.URLEncoder.encode(result.errorCode(), java.nio.charset.StandardCharsets.UTF_8.toString());
                return "redirect:/manager/sales/invoice/" + id + "?sendError=" + encoded;
            } catch (java.io.UnsupportedEncodingException e) {
                return "redirect:/manager/sales/invoice/" + id + "?sendError=mail_error";
            }
        }

        sale.setInvoiceSent(true);
        sale.setInvoiceSentAt(LocalDateTime.now());
        sale.setInvoiceSentTo(result.recipient());
        saleService.save(sale);

        return "redirect:/manager/sales/invoice/" + id + "?sendSuccess=1";
    }

    @GetMapping("/sales/invoice/{id}/resend")
    public String resendInvoice(@PathVariable("id") Long id) {
        Optional<Sale> saleOpt = saleService.findById(id);
        if (saleOpt.isEmpty()) {
            return "redirect:/manager/sales";
        }

        Sale sale = saleOpt.get();
        if (sale.getBuyerType() == BuyerType.AUCTION) {
            return "redirect:/manager/sales/invoice/" + id + "?sendError=auction";
        }
        if (sale.getEmail() == null || sale.getEmail().isBlank()) {
            return "redirect:/manager/sales/invoice/" + id + "?sendError=missing_email";
        }

        InvoiceSendResult result = invoiceEmailService.sendInvoiceEmail(sale);
        if (!result.success()) {
            try {
                String encoded = java.net.URLEncoder.encode(result.errorCode(), java.nio.charset.StandardCharsets.UTF_8.toString());
                return "redirect:/manager/sales/invoice/" + id + "?sendError=" + encoded;
            } catch (java.io.UnsupportedEncodingException e) {
                return "redirect:/manager/sales/invoice/" + id + "?sendError=mail_error";
            }
        }

        sale.setInvoiceSent(true);
        sale.setInvoiceSentAt(LocalDateTime.now());
        sale.setInvoiceSentTo(result.recipient());
        saleService.save(sale);

        return "redirect:/manager/sales/invoice/" + id + "?resendSuccess=1";
    }

    @GetMapping("/sales/download")
    public ResponseEntity<byte[]> downloadSalesReport() {
        List<Sale> sales = saleService.findAll();
        byte[] pdf = pdfReportService.salesReport(sales);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"sales-report.pdf\"")
                .contentType(MediaType.APPLICATION_PDF)
                .body(pdf);
    }

    @GetMapping("/reports/download")
    public ResponseEntity<byte[]> downloadFullBusinessReport() {
        List<Sale> sales = saleService.findAll();
        List<Vehicle> vehicles = vehicleService.findAll();
        List<Repair> repairs = repairService.findActive();
        byte[] pdf = pdfReportService.fullBusinessReport(sales, vehicles, repairs);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"business-report.pdf\"")
                .contentType(MediaType.APPLICATION_PDF)
                .body(pdf);
    }

    @GetMapping("/sales/api/vehicle/{id}")
    @ResponseBody
    public String vehicleData(@PathVariable("id") Long id) {
        Optional<Vehicle> opt = vehicleService.findById(id);
        if (opt.isPresent()) {
            Vehicle v = opt.get();
            BigDecimal purchase = v.getPurchasePrice() != null ? v.getPurchasePrice() : BigDecimal.ZERO;
            BigDecimal repair = v.getRepairCost() != null ? v.getRepairCost() : BigDecimal.ZERO;
            return "{\"purchasePrice\":" + purchase + ",\"repairCost\":" + repair + ",\"totalInvestment\":"
                    + purchase.add(repair) + "}";
        }
        return "{}";
    }

    // =========================================================================
    // Download: Attendance Report PDF
    // =========================================================================
    // Attendance download endpoint removed for deployment.

}
