package com.vyms.service;

import com.lowagie.text.*;
import com.lowagie.text.Font;
import com.lowagie.text.Rectangle;
import com.lowagie.text.pdf.*;

import com.vyms.entity.Repair;
import com.vyms.entity.Sale;
import com.vyms.entity.Vehicle;
import org.springframework.stereotype.Service;

import java.awt.*;
import java.io.ByteArrayOutputStream;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Service
public class PdfReportService {

    // ─── Colours & Fonts ─────────────────────────────────────────────────────
    private static final Color PRIMARY   = new Color(0x6366f1);
    private static final Color DARK      = new Color(0x1e293b);
    private static final Color MUTED     = new Color(0x64748b);
    private static final Color ROW_ALT   = new Color(0xf8fafc);
    private static final Color GREEN     = new Color(0x10b981);
    private static final Color RED       = new Color(0xef4444);
    private static final Color AMBER     = new Color(0xd97706);
    private static final Color HEADER_BG = new Color(0x6366f1);

    private static final Font TITLE_FONT    = FontFactory.getFont(FontFactory.HELVETICA_BOLD,  20, PRIMARY);
    private static final Font SUB_FONT      = FontFactory.getFont(FontFactory.HELVETICA,       10, MUTED);
    private static final Font SECTION_FONT  = FontFactory.getFont(FontFactory.HELVETICA_BOLD,  12, DARK);
    private static final Font TH_FONT       = FontFactory.getFont(FontFactory.HELVETICA_BOLD,  9, Color.WHITE);
    private static final Font TD_FONT       = FontFactory.getFont(FontFactory.HELVETICA,        9, DARK);
    private static final Font TD_BOLD       = FontFactory.getFont(FontFactory.HELVETICA_BOLD,   9, DARK);
    private static final Font SMALL_MUTED   = FontFactory.getFont(FontFactory.HELVETICA,        8, MUTED);

    // =========================================================================
    // 1. Full Business Report (Admin)
    // =========================================================================
    public byte[] fullBusinessReport(List<Sale> sales, List<Vehicle> vehicles,
                                     List<Repair> repairs) {
        try (ByteArrayOutputStream bos = new ByteArrayOutputStream()) {
            Document doc = new Document(PageSize.A4.rotate(), 36, 36, 48, 36);
            PdfWriter.getInstance(doc, bos);
            doc.open();

            addHeader(doc, "Full Business Report", "Generated on " + LocalDate.now());

            // ── Business Summary ──
            BigDecimal totalRevenue  = sales.stream().map(s -> s.getSalePrice() != null ? s.getSalePrice() : BigDecimal.ZERO).reduce(BigDecimal.ZERO, BigDecimal::add);
            BigDecimal totalExpenses = sales.stream().map(s -> s.getTotalCost() != null ? s.getTotalCost() : BigDecimal.ZERO).reduce(BigDecimal.ZERO, BigDecimal::add);
            BigDecimal totalProfit   = sales.stream().map(Sale::getProfit).reduce(BigDecimal.ZERO, BigDecimal::add);
            BigDecimal repairTotal   = repairs.stream().map(r -> r.getCost() != null ? r.getCost() : BigDecimal.ZERO).reduce(BigDecimal.ZERO, BigDecimal::add);
            long soldVehicles        = vehicles.stream().filter(v -> "SOLD".equalsIgnoreCase(v.getStatus())).count();

            doc.add(sectionTitle("Business Summary"));
            PdfPTable summary = newTable(new float[]{25,25,25,25});
            addTh(summary, "Total Revenue", "Total Expenses", "Total Profit", "Total Repair Cost");
            addTdRow(summary, Color.WHITE, "¥" + fmt(totalRevenue), "¥" + fmt(totalExpenses), "¥" + fmt(totalProfit), "¥" + fmt(repairTotal));
            doc.add(summary);
            doc.add(Chunk.NEWLINE);

            PdfPTable summary2 = newTable(new float[]{25,25,25,25});
            addTh(summary2, "Total Vehicles", "Sold", "Unsold", "Report Date");
            addTdRow(summary2, Color.WHITE,
                    String.valueOf(vehicles.size()),
                    String.valueOf(soldVehicles),
                    String.valueOf(vehicles.size() - soldVehicles),
                    LocalDate.now().toString());
            doc.add(summary2);
            doc.add(Chunk.NEWLINE);

            // ── Sales Table ──
            doc.add(sectionTitle("Sales Transactions (" + sales.size() + ")"));
            PdfPTable salesTable = newTable(new float[]{20, 15, 12, 12, 12, 15, 14});
            addTh(salesTable, "Vehicle", "Buyer Type", "Customer", "Sale Date", "Total Cost", "Sale Price", "Profit");
            boolean alt = false;
            for (Sale s : sales) {
                Color bg = alt ? ROW_ALT : Color.WHITE;
                String veh   = s.getVehicle() != null ? s.getVehicle().getVehicleModel() : "—";
                String buyer = s.getBuyerType() != null ? s.getBuyerType().name().replace("_", " ") : "—";
                String cust  = s.getCustomerName() != null ? s.getCustomerName() : (s.getCompanyName() != null ? s.getCompanyName() : "—");
                addTdRow(salesTable, bg, veh, buyer, cust,
                        s.getSaleDate() != null ? s.getSaleDate().toString() : "—",
                        "¥" + fmt(s.getTotalCost()),
                        "¥" + fmt(s.getSalePrice()),
                        "¥" + fmt(s.getProfit()));
                alt = !alt;
            }
            if (sales.isEmpty()) addEmptyRow(salesTable, 7);
            doc.add(salesTable);
            doc.add(Chunk.NEWLINE);

            // ── Repair Table ──
            doc.add(sectionTitle("Repair & Maintenance (" + repairs.size() + ")"));
            PdfPTable repTable = newTable(new float[]{22, 30, 15, 15, 18});
            addTh(repTable, "Vehicle", "Description", "Type", "Date", "Cost");
            alt = false;
            for (Repair r : repairs) {
                Color bg = alt ? ROW_ALT : Color.WHITE;
                String veh = r.getVehicle() != null ? r.getVehicle().getVehicleModel() : "—";
                addTdRow(repTable, bg, veh,
                        safe(r.getDescription()),
                        safe(r.getRepairType()),
                        r.getRepairDate() != null ? r.getRepairDate().toString() : "—",
                        "¥" + fmt(r.getCost()));
                alt = !alt;
            }
            if (repairs.isEmpty()) addEmptyRow(repTable, 5);
            doc.add(repTable);

            addFooter(doc);
            doc.close();
            return bos.toByteArray();
        } catch (Exception e) {
            throw new RuntimeException("Failed to generate full business PDF", e);
        }
    }


    // =========================================================================
    // 3. Repair Report (Mechanic)
    // =========================================================================
    public byte[] repairReport(List<Repair> repairs) {
        try (ByteArrayOutputStream bos = new ByteArrayOutputStream()) {
            Document doc = new Document(PageSize.A4, 36, 36, 48, 36);
            PdfWriter.getInstance(doc, bos);
            doc.open();

            addHeader(doc, "Repair & Maintenance Report", "Generated on " + LocalDate.now());

            BigDecimal total    = repairs.stream().map(r -> r.getCost() != null ? r.getCost() : BigDecimal.ZERO).reduce(BigDecimal.ZERO, BigDecimal::add);
            BigDecimal internal = repairs.stream().filter(r -> "INTERNAL".equalsIgnoreCase(r.getRepairType())).map(r -> r.getCost() != null ? r.getCost() : BigDecimal.ZERO).reduce(BigDecimal.ZERO, BigDecimal::add);
            BigDecimal external = repairs.stream().filter(r -> "EXTERNAL".equalsIgnoreCase(r.getRepairType())).map(r -> r.getCost() != null ? r.getCost() : BigDecimal.ZERO).reduce(BigDecimal.ZERO, BigDecimal::add);

            addSummaryPair(doc, "Total Repair Cost", "¥" + fmt(total), "Total Records", String.valueOf(repairs.size()));
            addSummaryPair(doc, "Internal Cost",     "¥" + fmt(internal), "External Cost", "¥" + fmt(external));

            PdfPTable table = newTable(new float[]{22, 35, 15, 14, 14});
            addTh(table, "Vehicle", "Description", "Type", "Date", "Cost");

            boolean alt = false;
            for (Repair r : repairs) {
                Color bg  = alt ? ROW_ALT : Color.WHITE;
                String veh = r.getVehicle() != null ? r.getVehicle().getVehicleModel() : "—";
                addTdRow(table, bg, veh, safe(r.getDescription()), safe(r.getRepairType()),
                        r.getRepairDate() != null ? r.getRepairDate().toString() : "—",
                        "¥" + fmt(r.getCost()));
                alt = !alt;
            }
            if (repairs.isEmpty()) addEmptyRow(table, 5);
            doc.add(table);

            addFooter(doc);
            doc.close();
            return bos.toByteArray();
        } catch (Exception e) {
            throw new RuntimeException("Failed to generate repair PDF", e);
        }
    }

    // =========================================================================
    // 5. Sales Report (Sales Dashboard)
    // =========================================================================
    public byte[] salesReport(List<Sale> sales) {
        try (ByteArrayOutputStream bos = new ByteArrayOutputStream()) {
            Document doc = new Document(PageSize.A4.rotate(), 36, 36, 48, 36);
            PdfWriter.getInstance(doc, bos);
            doc.open();

            addHeader(doc, "Sales Report", "Generated on " + LocalDate.now());

            BigDecimal totalRevenue = sales.stream().map(s -> s.getSalePrice() != null ? s.getSalePrice() : BigDecimal.ZERO).reduce(BigDecimal.ZERO, BigDecimal::add);
            BigDecimal totalProfit  = sales.stream().map(Sale::getProfit).reduce(BigDecimal.ZERO, BigDecimal::add);

            addSummaryPair(doc, "Total Sales", String.valueOf(sales.size()), "Total Revenue", "¥" + fmt(totalRevenue));
            addSummaryPair(doc, "Total Profit", "¥" + fmt(totalProfit), "Avg Sale Price",
                    sales.isEmpty() ? "—" : "¥" + fmt(totalRevenue.divide(BigDecimal.valueOf(sales.size()), 0, java.math.RoundingMode.HALF_UP)));

            PdfPTable table = newTable(new float[]{18, 15, 18, 12, 14, 14, 10});
            addTh(table, "Vehicle", "Buyer Type", "Customer / Company", "Date", "Cost", "Sale Price", "Profit");

            boolean alt = false;
            for (Sale s : sales) {
                Color bg = alt ? ROW_ALT : Color.WHITE;
                String veh   = s.getVehicle() != null ? s.getVehicle().getVehicleModel() : "—";
                String buyer = s.getBuyerType() != null ? s.getBuyerType().name().replace("_", " ") : "—";
                String cust  = firstNonNull(s.getCustomerName(), s.getCompanyName(), s.getAuctionHouseName(), "—");
                addTdRow(table, bg, veh, buyer, cust,
                        s.getSaleDate() != null ? s.getSaleDate().toString() : "—",
                        "¥" + fmt(s.getTotalCost()),
                        "¥" + fmt(s.getSalePrice()),
                        "¥" + fmt(s.getProfit()));
                alt = !alt;
            }
            if (sales.isEmpty()) addEmptyRow(table, 7);
            doc.add(table);

            addFooter(doc);
            doc.close();
            return bos.toByteArray();
        } catch (Exception e) {
            throw new RuntimeException("Failed to generate sales PDF", e);
        }
    }

    // =========================================================================
    // 6. Inventory Stock Report (Inventory Role)
    // =========================================================================
    public byte[] inventoryStockReport(List<Vehicle> vehicles) {
        try (ByteArrayOutputStream bos = new ByteArrayOutputStream()) {
            Document doc = new Document(PageSize.A4.rotate(), 36, 36, 48, 36);
            PdfWriter.getInstance(doc, bos);
            doc.open();

            addHeader(doc, "Inventory Stock Report", "Generated on " + LocalDate.now());

            // ── Summary Stats ──
            long total   = vehicles.size();
            long unsold  = vehicles.stream().filter(v -> !"SOLD".equalsIgnoreCase(v.getStatus())).count();
            long sold    = total - unsold;

            BigDecimal totalInvestment = vehicles.stream()
                .filter(v -> !"SOLD".equalsIgnoreCase(v.getStatus()))
                .map(v -> {
                    BigDecimal p = v.getPurchasePrice() != null ? v.getPurchasePrice() : BigDecimal.ZERO;
                    BigDecimal r = v.getRepairCost()    != null ? v.getRepairCost()    : BigDecimal.ZERO;
                    return p.add(r);
                })
                .reduce(BigDecimal.ZERO, BigDecimal::add);

            BigDecimal potentialRevenue = vehicles.stream()
                .filter(v -> !"SOLD".equalsIgnoreCase(v.getStatus()))
                .map(v -> v.getSalePrice() != null ? v.getSalePrice() : BigDecimal.ZERO)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

            BigDecimal soldRevenue = vehicles.stream()
                .filter(v -> "SOLD".equalsIgnoreCase(v.getStatus()))
                .map(v -> v.getSalePrice() != null ? v.getSalePrice() : BigDecimal.ZERO)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

            addSummaryPair(doc, "Total Vehicles",   String.valueOf(total),   "In Stock (Unsold)", String.valueOf(unsold));
            addSummaryPair(doc, "Sold Vehicles",    String.valueOf(sold),    "Total Investment (Unsold Stock)", "¥" + fmt(totalInvestment));
            addSummaryPair(doc, "Potential Revenue (Unsold)", "¥" + fmt(potentialRevenue), "Revenue Realised (Sold)", "¥" + fmt(soldRevenue));

            // ── Vehicle Table ──
            doc.add(sectionTitle("Vehicle Stock List (" + total + " vehicles)"));
            PdfPTable table = newTable(new float[]{20, 16, 15, 14, 12, 14, 9});
            addTh(table, "Model", "Chassis No.", "License Plate", "Purchase Price", "Repair Cost", "Sale Price", "Status");

            boolean alt = false;
            for (Vehicle v : vehicles) {
                Color bg = alt ? ROW_ALT : Color.WHITE;
                String status = v.getStatus() != null ? v.getStatus() : "UNSOLD";
                Color sc = "SOLD".equalsIgnoreCase(status) ? GREEN : AMBER;
                PdfPCell modelCell    = tdCell(safe(v.getVehicleModel()), bg);
                PdfPCell chassisCell  = tdCell(safe(v.getChassisNumber()), bg);
                PdfPCell plateCell    = tdCell(safe(v.getLicensePlate()), bg);
                PdfPCell purchaseCell = tdCell("¥" + fmt(v.getPurchasePrice()), bg);
                PdfPCell repairCell   = tdCell("¥" + fmt(v.getRepairCost()), bg);
                PdfPCell salePriceCell = tdCell(v.getSalePrice() != null ? "¥" + fmt(v.getSalePrice()) : "--", bg);
                PdfPCell statusCell   = coloredTdCell(status, bg, sc);
                table.addCell(modelCell);
                table.addCell(chassisCell);
                table.addCell(plateCell);
                table.addCell(purchaseCell);
                table.addCell(repairCell);
                table.addCell(salePriceCell);
                table.addCell(statusCell);
                alt = !alt;
            }
            if (vehicles.isEmpty()) addEmptyRow(table, 7);
            doc.add(table);

            addFooter(doc);
            doc.close();
            return bos.toByteArray();
        } catch (Exception e) {
            throw new RuntimeException("Failed to generate inventory stock PDF", e);
        }
    }

    // =========================================================================
    // Helpers
    // =========================================================================

    private void addHeader(Document doc, String title, String subtitle) throws DocumentException {
        Paragraph t = new Paragraph(title, TITLE_FONT);
        t.setAlignment(Element.ALIGN_LEFT);
        t.setSpacingAfter(2);
        doc.add(t);

        Paragraph s = new Paragraph("Vehicle Yard Management System  |  " + subtitle, SUB_FONT);
        s.setAlignment(Element.ALIGN_LEFT);
        s.setSpacingAfter(14);
        doc.add(s);

        // Divider line
        PdfPTable line = new PdfPTable(1);
        line.setWidthPercentage(100);
        PdfPCell lc = new PdfPCell();
        lc.setBorderWidthBottom(1.5f);
        lc.setBorderColorBottom(PRIMARY);
        lc.setBorderWidthTop(0); lc.setBorderWidthLeft(0); lc.setBorderWidthRight(0);
        lc.setFixedHeight(4);
        line.addCell(lc);
        doc.add(line);
        doc.add(Chunk.NEWLINE);
    }

    private void addSummaryPair(Document doc, String k1, String v1, String k2, String v2) throws DocumentException {
        PdfPTable t = new PdfPTable(4);
        t.setWidthPercentage(100);
        t.setSpacingAfter(8);
        PdfPCell kc1 = new PdfPCell(new Phrase(k1, SMALL_MUTED)); styleKCell(kc1);
        PdfPCell vc1 = new PdfPCell(new Phrase(v1, TD_BOLD));      styleVCell(vc1);
        PdfPCell kc2 = new PdfPCell(new Phrase(k2, SMALL_MUTED)); styleKCell(kc2);
        PdfPCell vc2 = new PdfPCell(new Phrase(v2, TD_BOLD));      styleVCell(vc2);
        t.addCell(kc1); t.addCell(vc1); t.addCell(kc2); t.addCell(vc2);
        doc.add(t);
    }

    private Paragraph sectionTitle(String text) {
        Paragraph p = new Paragraph(text, SECTION_FONT);
        p.setSpacingBefore(12);
        p.setSpacingAfter(6);
        return p;
    }

    private PdfPTable newTable(float[] widths) throws DocumentException {
        PdfPTable t = new PdfPTable(widths.length);
        t.setWidthPercentage(100);
        t.setWidths(widths);
        t.setSpacingAfter(4);
        t.setHeaderRows(1);
        return t;
    }

    private void addTh(PdfPTable table, String... headers) {
        for (String h : headers) {
            PdfPCell c = new PdfPCell(new Phrase(h, TH_FONT));
            c.setBackgroundColor(HEADER_BG);
            c.setPadding(6);
            c.setBorder(Rectangle.NO_BORDER);
            c.setHorizontalAlignment(Element.ALIGN_LEFT);
            table.addCell(c);
        }
    }

    private void addTdRow(PdfPTable table, Color bg, String... values) {
        for (String v : values) {
            table.addCell(tdCell(v, bg));
        }
    }

    private PdfPCell tdCell(String value, Color bg) {
        PdfPCell c = new PdfPCell(new Phrase(value != null ? value : "—", TD_FONT));
        c.setBackgroundColor(bg);
        c.setPadding(5);
        c.setBorderColor(new Color(0xe2e8f0));
        c.setBorderWidth(0.5f);
        return c;
    }

    private PdfPCell coloredTdCell(String value, Color bg, Color textColor) {
        Font f = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 9, textColor);
        PdfPCell c = new PdfPCell(new Phrase(value != null ? value : "—", f));
        c.setBackgroundColor(bg);
        c.setPadding(5);
        c.setBorderColor(new Color(0xe2e8f0));
        c.setBorderWidth(0.5f);
        return c;
    }

    private void addEmptyRow(PdfPTable table, int cols) {
        PdfPCell c = new PdfPCell(new Phrase("No records found.", SMALL_MUTED));
        c.setColspan(cols);
        c.setHorizontalAlignment(Element.ALIGN_CENTER);
        c.setPadding(12);
        c.setBorder(Rectangle.NO_BORDER);
        table.addCell(c);
    }

    private void styleKCell(PdfPCell c) {
        c.setPadding(6);
        c.setBackgroundColor(new Color(0xf8fafc));
        c.setBorderColor(new Color(0xe2e8f0));
        c.setBorderWidth(0.5f);
    }

    private void styleVCell(PdfPCell c) {
        c.setPadding(6);
        c.setBackgroundColor(Color.WHITE);
        c.setBorderColor(new Color(0xe2e8f0));
        c.setBorderWidth(0.5f);
    }

    private void addFooter(Document doc) throws DocumentException {
        doc.add(Chunk.NEWLINE);
        Paragraph footer = new Paragraph("VYMS — Vehicle Yard Management System  |  Report generated: " + java.time.LocalDateTime.now().toString().substring(0, 19), SMALL_MUTED);
        footer.setAlignment(Element.ALIGN_RIGHT);
        doc.add(footer);
    }

    private String fmt(BigDecimal v) {
        return v != null ? String.format("%,.0f", v) : "0";
    }

    private String safe(String v) { return v != null ? v : "—"; }

    private String firstNonNull(String... vals) {
        for (String v : vals) if (v != null && !v.isBlank()) return v;
        return "—";
    }
}
