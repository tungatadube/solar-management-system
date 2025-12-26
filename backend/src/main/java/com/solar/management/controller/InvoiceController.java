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
import org.springframework.security.access.prepost.PreAuthorize;
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
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Invoice> generateInvoice(
            @RequestParam Long technicianId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {

        Invoice invoice = invoiceService.generateInvoiceWithAuth(technicianId, startDate, endDate);
        return new ResponseEntity<>(invoice, HttpStatus.CREATED);
    }
    
    /**
     * Generate PDF file for an existing invoice
     */
    @PostMapping("/{id}/generate-excel")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<String> generateExcelInvoice(@PathVariable Long id) {
        try {
            String filePath = invoiceService.generateExcelInvoiceWithAuth(id);
            return ResponseEntity.ok(filePath);
        } catch (IOException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Failed to generate PDF: " + e.getMessage());
        }
    }

    /**
     * Download invoice PDF file
     */
    @GetMapping("/{id}/download")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Resource> downloadInvoice(@PathVariable Long id) {
        try {
            Invoice invoice = invoiceService.getInvoiceByIdWithAuth(id);

            if (invoice.getFileUrl() == null) {
                // Generate if not exists
                invoiceService.generateExcelInvoiceWithAuth(id);
                invoice = invoiceService.getInvoiceByIdWithAuth(id);
            }

            Resource file = new FileSystemResource(Paths.get("./uploads", invoice.getFileUrl()));

            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION,
                           "attachment; filename=\"" + file.getFilename() + "\"")
                    .contentType(MediaType.APPLICATION_PDF)
                    .body(file);

        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
    
    /**
     * Get invoice by ID
     */
    @GetMapping("/{id}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Invoice> getInvoiceById(@PathVariable Long id) {
        Invoice invoice = invoiceService.getInvoiceByIdWithAuth(id);
        return ResponseEntity.ok(invoice);
    }
    
    /**
     * Get all invoices for a technician
     */
    @GetMapping("/technician/{technicianId}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<Invoice>> getInvoicesByTechnician(@PathVariable Long technicianId) {
        List<Invoice> invoices = invoiceService.getInvoicesByTechnicianWithAuth(technicianId);
        return ResponseEntity.ok(invoices);
    }
    
    /**
     * Update invoice details (bill to, bank info, etc.)
     */
    @PutMapping("/{id}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Invoice> updateInvoice(@PathVariable Long id, @RequestBody Invoice invoiceDetails) {
        Invoice invoice = invoiceService.getInvoiceByIdWithAuth(id);

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
