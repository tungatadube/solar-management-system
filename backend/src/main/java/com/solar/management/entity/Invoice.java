package com.solar.management.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name = "invoices")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Invoice {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(nullable = false, unique = true)
    private String invoiceNumber;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "technician_id", nullable = false)
    private User technician; // Person who did the work
    
    @Column(nullable = false)
    private LocalDate invoiceDate;
    
    @Column(nullable = false)
    private LocalDate periodStartDate;
    
    @Column(nullable = false)
    private LocalDate periodEndDate;
    
    @Column(nullable = false)
    private Integer weekNumber;
    
    // Bill To Information (Company receiving the invoice)
    @Column(nullable = false)
    private String billToName; // e.g., "Nelvin Electrical"
    
    @Column(nullable = false)
    private String billToAddress;
    
    @Column
    private String billToPhone;
    
    @Column
    private String billToEmail;
    
    // Technician Information (Person billing)
    @Column(nullable = false)
    private String technicianName;
    
    @Column
    private String technicianABN;
    
    @Column(nullable = false)
    private String technicianAddress;
    
    @Column
    private String technicianEmail;
    
    @Column
    private String technicianPhone;
    
    // Bank Details
    @Column
    private String bankName;
    
    @Column
    private String bsb;
    
    @Column
    private String accountNumber;
    
    @OneToMany(mappedBy = "invoice", cascade = CascadeType.ALL)
    private Set<WorkLog> workLogs = new HashSet<>();
    
    @Column(nullable = false)
    private BigDecimal subtotal;
    
    @Column(nullable = false)
    private BigDecimal gstRate = BigDecimal.ZERO; // 0 or 0.10 for 10% GST
    
    @Column(nullable = false)
    private BigDecimal gstAmount;
    
    @Column(nullable = false)
    private BigDecimal totalAmount;
    
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private InvoiceStatus status;
    
    @Column
    private LocalDate paidDate;
    
    @Column
    private String fileUrl; // Path to generated Excel file
    
    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;
    
    @UpdateTimestamp
    @Column(nullable = false)
    private LocalDateTime updatedAt;
    
    public enum InvoiceStatus {
        DRAFT,
        SENT,
        PAID,
        OVERDUE,
        CANCELLED
    }
    
    @PrePersist
    @PreUpdate
    public void calculateTotals() {
        // Calculate from work logs if available
        if (workLogs != null && !workLogs.isEmpty()) {
            this.subtotal = workLogs.stream()
                    .map(WorkLog::getTotalAmount)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
        }
        
        if (subtotal != null && gstRate != null) {
            this.gstAmount = subtotal.multiply(gstRate);
            this.totalAmount = subtotal.add(gstAmount);
        }
    }
}
