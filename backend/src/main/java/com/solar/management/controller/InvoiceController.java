package com.solar.management.controller;

import com.solar.management.entity.Invoice;
import com.solar.management.service.InvoiceService;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/invoices")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class InvoiceController {
    
    private final InvoiceService invoiceService;
    
    /**
     * Generate a new invoice for a technician for a date range
     */
    @PostMapping("/generate")
    public ResponseEntity<Invoice> generateInvoice(
            @RequestParam Long technicianId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        
        Invoice invoice = invoiceService.generateInvoice(technicianId, startDate, endDate);
        return new ResponseEntity<>(invoice, HttpStatus.CREATED);
    }
    
    /**
     * Generate Excel file for an existing invoice
     */
    @PostMapping("/{id}/generate-excel")
    public ResponseEntity<String> generateExcelInvoice(@PathVariable Long id) {
        try {
            String filePath = invoiceService.generateExcelInvoice(id);
            return ResponseEntity.ok(filePath);
        } catch (IOException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Failed to generate Excel: " + e.getMessage());
        }
    }
    
    /**
     * Download invoice Excel file
     */
    @GetMapping("/{id}/download")
    public ResponseEntity<Resource> downloadInvoice(@PathVariable Long id) {
        try {
            Invoice invoice = invoiceService.getInvoiceById(id);
            
            if (invoice.getFileUrl() == null) {
                // Generate if not exists
                invoiceService.generateExcelInvoice(id);
                invoice = invoiceService.getInvoiceById(id);
            }
            
            Resource file = new FileSystemResource(Paths.get("./uploads", invoice.getFileUrl()));
            
            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION, 
                           "attachment; filename=\"" + file.getFilename() + "\"")
                    .contentType(MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                    .body(file);
                    
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
    
    /**
     * Get invoice by ID
     */
    @GetMapping("/{id}")
    public ResponseEntity<Invoice> getInvoiceById(@PathVariable Long id) {
        Invoice invoice = invoiceService.getInvoiceById(id);
        return ResponseEntity.ok(invoice);
    }
    
    /**
     * Get all invoices for a technician
     */
    @GetMapping("/technician/{technicianId}")
    public ResponseEntity<List<Invoice>> getInvoicesByTechnician(@PathVariable Long technicianId) {
        List<Invoice> invoices = invoiceService.getInvoicesByTechnician(technicianId);
        return ResponseEntity.ok(invoices);
    }
    
    /**
     * Update invoice details (bill to, bank info, etc.)
     */
    @PutMapping("/{id}")
    public ResponseEntity<Invoice> updateInvoice(@PathVariable Long id, @RequestBody Invoice invoiceDetails) {
        Invoice invoice = invoiceService.getInvoiceById(id);
        
        // Update editable fields
        if (invoiceDetails.getBillToName() != null) invoice.setBillToName(invoiceDetails.getBillToName());
        if (invoiceDetails.getBillToAddress() != null) invoice.setBillToAddress(invoiceDetails.getBillToAddress());
        if (invoiceDetails.getBillToPhone() != null) invoice.setBillToPhone(invoiceDetails.getBillToPhone());
        if (invoiceDetails.getBillToEmail() != null) invoice.setBillToEmail(invoiceDetails.getBillToEmail());
        if (invoiceDetails.getTechnicianABN() != null) invoice.setTechnicianABN(invoiceDetails.getTechnicianABN());
        if (invoiceDetails.getTechnicianAddress() != null) invoice.setTechnicianAddress(invoiceDetails.getTechnicianAddress());
        if (invoiceDetails.getBsb() != null) invoice.setBsb(invoiceDetails.getBsb());
        if (invoiceDetails.getAccountNumber() != null) invoice.setAccountNumber(invoiceDetails.getAccountNumber());
        if (invoiceDetails.getGstRate() != null) invoice.setGstRate(invoiceDetails.getGstRate());
        if (invoiceDetails.getStatus() != null) invoice.setStatus(invoiceDetails.getStatus());
        
        invoice.calculateTotals();
        
        return ResponseEntity.ok(invoice);
    }
}
