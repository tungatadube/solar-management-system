package com.solar.management.service;

import com.solar.management.entity.Invoice;
import com.solar.management.entity.User;
import com.solar.management.entity.WorkLog;
import com.solar.management.repository.InvoiceRepository;
import com.solar.management.repository.UserRepository;
import com.solar.management.repository.WorkLogRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.temporal.WeekFields;
import java.util.List;
import java.util.Locale;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class InvoiceService {

    private final InvoiceRepository invoiceRepository;
    private final WorkLogRepository workLogRepository;
    private final UserRepository userRepository;
    private final ParameterService parameterService;
    private final PdfInvoiceGenerator pdfInvoiceGenerator;

    @Value("${file.upload-dir:./uploads}")
    private String uploadDir;
    
    /**
     * Generate invoice for a technician for a specific date range
     */
    public Invoice generateInvoice(Long technicianId, LocalDate startDate, LocalDate endDate) {
        User technician = userRepository.findById(technicianId)
                .orElseThrow(() -> new RuntimeException("Technician not found"));
        
        // Get all uninvoiced work logs for this period
        List<WorkLog> workLogs = workLogRepository.findUninvoicedWorkByUserAndDateRange(
                technician, startDate, endDate);
        
        if (workLogs.isEmpty()) {
            throw new RuntimeException("No uninvoiced work found for this period");
        }
        
        // Generate invoice number
        String invoiceNumber = generateInvoiceNumber(technician, startDate);
        
        // Calculate week number
        WeekFields weekFields = WeekFields.of(Locale.getDefault());
        int weekNumber = startDate.get(weekFields.weekOfWeekBasedYear());
        
        // Create invoice
        Invoice invoice = Invoice.builder()
                .invoiceNumber(invoiceNumber)
                .technician(technician)
                .invoiceDate(LocalDate.now())
                .periodStartDate(startDate)
                .periodEndDate(endDate)
                .weekNumber(weekNumber)
                .billToName(parameterService.getCompanyName())
                .billToAddress(parameterService.getCompanyAddress())
                .billToEmail(parameterService.getCompanyEmail())
                .billToPhone(parameterService.getCompanyPhone())
                .technicianName(technician.getFirstName() + " " + technician.getLastName())
                .technicianABN(technician.getAbn())
                .technicianAddress(technician.getAddress() != null ? technician.getAddress() : "")
                .technicianEmail(technician.getEmail())
                .technicianPhone(technician.getPhoneNumber())
                .bankName(technician.getAccount() != null ? technician.getAccount().getBankName() : null)
                .bsb(technician.getAccount() != null ? technician.getAccount().getBsb() : null)
                .accountNumber(technician.getAccount() != null ? technician.getAccount().getAccountNumber() : null)
                .status(Invoice.InvoiceStatus.DRAFT)
                .gstRate(BigDecimal.ZERO)
                .build();
        
        // Mark work logs as invoiced and associate with invoice
        workLogs.forEach(workLog -> {
            workLog.setInvoiced(true);
            workLog.setInvoice(invoice);
        });
        
        invoice.setWorkLogs(new java.util.HashSet<>(workLogs));
        invoice.calculateTotals();
        
        Invoice savedInvoice = invoiceRepository.save(invoice);
        
        log.info("Invoice {} generated for technician {} with {} work logs",
                invoiceNumber, technician.getUsername(), workLogs.size());
        
        return savedInvoice;
    }
    
    /**
     * Generate PDF invoice file
     */
    public String generateExcelInvoice(Long invoiceId) throws IOException {
        Invoice invoice = invoiceRepository.findById(invoiceId)
                .orElseThrow(() -> new RuntimeException("Invoice not found"));

        // Generate PDF file name
        String fileName = "INV-" + formatDate(invoice.getInvoiceDate()) +
                         "-Week-" + invoice.getWeekNumber() +
                         "-Invoice-" + invoice.getTechnicianName().replace(" ", "-") + ".pdf";

        Path outputPath = Paths.get(uploadDir, "invoices", fileName);
        Files.createDirectories(outputPath.getParent());

        // Generate PDF using PdfInvoiceGenerator
        pdfInvoiceGenerator.generatePdfInvoice(invoice, outputPath);

        // Update invoice with file path
        invoice.setFileUrl("invoices/" + fileName);
        invoiceRepository.save(invoice);

        log.info("PDF invoice generated: {}", fileName);

        return "invoices/" + fileName;
    }
    
    private String generateInvoiceNumber(User technician, LocalDate date) {
        String lastInvoiceNumber = invoiceRepository.findLatestInvoiceNumber();
        int nextNumber = 1;

        if (lastInvoiceNumber != null) {
            try {
                nextNumber = Integer.parseInt(lastInvoiceNumber.replaceAll("\\D+", "")) + 1;
            } catch (NumberFormatException e) {
                log.warn("Could not parse last invoice number: {}", lastInvoiceNumber);
            }
        }

        return String.format("%02d", nextNumber);
    }
    
    private String formatDate(LocalDate date) {
        return date.format(DateTimeFormatter.ofPattern("dd/MM/yy"));
    }
    
    private String formatDayDate(LocalDate date) {
        return date.format(DateTimeFormatter.ofPattern("EEEE dd MMMM yyyy"));
    }
    
    public Invoice getInvoiceById(Long id) {
        return invoiceRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Invoice not found"));
    }
    
    public List<Invoice> getInvoicesByTechnician(Long technicianId) {
        User technician = userRepository.findById(technicianId)
                .orElseThrow(() -> new RuntimeException("Technician not found"));
        return invoiceRepository.findByTechnicianOrderByDateDesc(technician);
    }

    /**
     * Generate weekly invoices for all technicians
     * Looks at completed jobs for Mon-Fri of current week
     */
    public List<Invoice> generateWeeklyInvoices() {
        // Get Monday and Friday of current week
        LocalDate today = LocalDate.now();
        LocalDate monday = today.with(java.time.DayOfWeek.MONDAY);
        LocalDate friday = today.with(java.time.DayOfWeek.FRIDAY);

        log.info("Generating weekly invoices for period {} to {}", monday, friday);

        List<Invoice> generatedInvoices = new java.util.ArrayList<>();

        // Get all active technicians and managers
        List<User> technicians = userRepository.findAll().stream()
                .filter(u -> u.getRole() == com.solar.management.entity.User.UserRole.TECHNICIAN ||
                            u.getRole() == com.solar.management.entity.User.UserRole.MANAGER)
                .toList();

        for (User technician : technicians) {
            try {
                // Try to generate invoice for this technician
                Invoice invoice = generateInvoice(technician.getId(), monday, friday);

                // Generate Excel file
                String filePath = generateExcelInvoice(invoice.getId());

                generatedInvoices.add(invoice);
                log.info("Generated invoice {} for technician {} - file: {}",
                        invoice.getInvoiceNumber(),
                        technician.getUsername(),
                        filePath);

            } catch (Exception e) {
                // Log but continue with other technicians
                log.warn("Could not generate invoice for technician {}: {}",
                        technician.getUsername(), e.getMessage());
            }
        }

        log.info("Weekly invoice generation complete. Generated {} invoices", generatedInvoices.size());
        return generatedInvoices;
    }

    /**
     * Get current week date range (Monday to Friday)
     */
    public Map<String, LocalDate> getCurrentWeekRange() {
        LocalDate today = LocalDate.now();
        LocalDate monday = today.with(java.time.DayOfWeek.MONDAY);
        LocalDate friday = today.with(java.time.DayOfWeek.FRIDAY);

        return Map.of(
            "startDate", monday,
            "endDate", friday
        );
    }
}
