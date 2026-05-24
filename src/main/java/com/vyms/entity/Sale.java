package com.vyms.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "sale")
public class Sale {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne
    @JoinColumn(name = "vehicle_id")
    private Vehicle vehicle;

    @ManyToOne
    @JoinColumn(name = "seller_id")
    private User seller;

    private BigDecimal salePrice;
    private LocalDate saleDate;

    @Enumerated(EnumType.STRING)
    @Column(name = "buyer_type", length = 20)
    private BuyerType buyerType;

    // Customer information
    private String customerName;
    private String contactNumber;
    private String email;

    // Export specific
    private String companyName;
    private String exportCountry;

    // Auction specific
    private String auctionHouseName;
    private String location;
    private String lotNumber;

    // DRAFT or FINALIZED
    private String saleStatus;

    // Snapshotted cost at time of sale (purchasePrice + repairCost)
    private BigDecimal totalCost;

    // Invoice send tracking
    private Boolean invoiceSent = Boolean.FALSE;
    private LocalDateTime invoiceSentAt;
    private String invoiceSentTo;

    // Getters and Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Vehicle getVehicle() {
        return vehicle;
    }

    public void setVehicle(Vehicle vehicle) {
        this.vehicle = vehicle;
    }

    public User getSeller() {
        return seller;
    }

    public void setSeller(User seller) {
        this.seller = seller;
    }

    public BigDecimal getSalePrice() {
        return salePrice;
    }

    public void setSalePrice(BigDecimal salePrice) {
        this.salePrice = salePrice;
    }

    public LocalDate getSaleDate() {
        return saleDate;
    }

    public void setSaleDate(LocalDate saleDate) {
        this.saleDate = saleDate;
    }

    public BuyerType getBuyerType() {
        return buyerType;
    }

    public void setBuyerType(BuyerType buyerType) {
        this.buyerType = buyerType;
    }

    public String getCustomerName() {
        return customerName;
    }

    public void setCustomerName(String customerName) {
        this.customerName = customerName;
    }

    public String getContactNumber() {
        return contactNumber;
    }

    public void setContactNumber(String contactNumber) {
        this.contactNumber = contactNumber;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getSaleStatus() {
        return saleStatus;
    }

    public void setSaleStatus(String saleStatus) {
        this.saleStatus = saleStatus;
    }

    public BigDecimal getTotalCost() {
        return totalCost;
    }

    public void setTotalCost(BigDecimal totalCost) {
        this.totalCost = totalCost;
    }

    public String getCompanyName() { return companyName; }
    public void setCompanyName(String companyName) { this.companyName = companyName; }

    public String getExportCountry() { return exportCountry; }
    public void setExportCountry(String exportCountry) { this.exportCountry = exportCountry; }

    public String getAuctionHouseName() { return auctionHouseName; }
    public void setAuctionHouseName(String auctionHouseName) { this.auctionHouseName = auctionHouseName; }

    public String getLocation() { return location; }
    public void setLocation(String location) { this.location = location; }

    public String getLotNumber() { return lotNumber; }
    public void setLotNumber(String lotNumber) { this.lotNumber = lotNumber; }

    public Boolean getInvoiceSent() {
        return invoiceSent;
    }

    public void setInvoiceSent(Boolean invoiceSent) {
        this.invoiceSent = invoiceSent;
    }

    public LocalDateTime getInvoiceSentAt() {
        return invoiceSentAt;
    }

    public void setInvoiceSentAt(LocalDateTime invoiceSentAt) {
        this.invoiceSentAt = invoiceSentAt;
    }

    public String getInvoiceSentTo() {
        return invoiceSentTo;
    }

    public void setInvoiceSentTo(String invoiceSentTo) {
        this.invoiceSentTo = invoiceSentTo;
    }

    // Computed: profit = salePrice - snapshotted totalCost (falls back to live
    // vehicle cost)
    public BigDecimal getProfit() {
        if (salePrice == null)
            return BigDecimal.ZERO;
        if (totalCost != null)
            return salePrice.subtract(totalCost);
        if (vehicle == null)
            return BigDecimal.ZERO;
        BigDecimal purchase = vehicle.getPurchasePrice() != null ? vehicle.getPurchasePrice() : BigDecimal.ZERO;
        BigDecimal repair = vehicle.getRepairCost() != null ? vehicle.getRepairCost() : BigDecimal.ZERO;
        return salePrice.subtract(purchase.add(repair));
    }
}
