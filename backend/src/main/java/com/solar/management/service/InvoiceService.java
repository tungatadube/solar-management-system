package com.solar.management.service;

import com.solar.management.entity.Invoice;
import com.solar.management.entity.User;
import com.solar.management.entity.WorkLog;
import com.solar.management.repository.InvoiceRepository;
import com.solar.management.repository.UserRepository;
import com.solar.management.repository.WorkLogRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.FileOutputStream;
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

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class InvoiceService {
    
    private final InvoiceRepository invoiceRepository;
    private final WorkLogRepository workLogRepository;
    private final UserRepository userRepository;
    
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
                .technicianName(technician.getFirstName() + " " + technician.getLastName())
                .technicianEmail(technician.getEmail())
                .technicianPhone(technician.getPhoneNumber())
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
     * Generate Excel file matching the uploaded format
     */
    public String generateExcelInvoice(Long invoiceId) throws IOException {
        Invoice invoice = invoiceRepository.findById(invoiceId)
                .orElseThrow(() -> new RuntimeException("Invoice not found"));
        
        Workbook workbook = new XSSFWorkbook();
        Sheet sheet = workbook.createSheet("Invoice");
        
        // Create cell styles
        CellStyle headerStyle = createHeaderStyle(workbook);
        CellStyle boldStyle = createBoldStyle(workbook);
        CellStyle currencyStyle = createCurrencyStyle(workbook);
        
        int rowNum = 1; // Start from row 2 (0-indexed row 1)
        
        // Row 2: Name and ABN
        Row row = sheet.createRow(rowNum++);
        createCell(row, 1, "Name", boldStyle);
        createCell(row, 2, invoice.getTechnicianName(), null);
        createCell(row, 3, "ABN", boldStyle);
        createCell(row, 4, invoice.getTechnicianABN(), null);
        
        // Row 3: Address and Email
        row = sheet.createRow(rowNum++);
        createCell(row, 1, "Address:", boldStyle);
        createCell(row, 2, invoice.getTechnicianAddress(), null);
        createCell(row, 3, "E Mail", boldStyle);
        createCell(row, 4, invoice.getTechnicianEmail(), null);
        
        // Row 4: City and Phone
        row = sheet.createRow(rowNum++);
        // Skip address line 2 for now
        createCell(row, 3, "Phone Number :", boldStyle);
        createCell(row, 4, invoice.getTechnicianPhone(), null);
        
        rowNum++; // Empty row
        
        // Row 6: Bill To info
        row = sheet.createRow(rowNum++);
        createCell(row, 1, "Bill To:", boldStyle);
        createCell(row, 2, invoice.getBillToName(), null);
        createCell(row, 3, "Phone:   " + invoice.getBillToPhone(), null);
        createCell(row, 4, "Invoice #:" + invoice.getInvoiceNumber(), boldStyle);
        
        // Row 7: Bill To Address
        row = sheet.createRow(rowNum++);
        createCell(row, 1, "Address:", boldStyle);
        createCell(row, 2, invoice.getBillToAddress(), null);
        createCell(row, 3, "Email:   " + invoice.getBillToEmail(), null);
        createCell(row, 4, "Week Number:" + invoice.getWeekNumber(), boldStyle);
        
        rowNum++; // Empty row
        
        // Row 9: Invoice period
        row = sheet.createRow(rowNum++);
        createCell(row, 1, "Invoice For:", boldStyle);
        String period = formatDate(invoice.getPeriodStartDate()) + "   -   " + 
                       formatDate(invoice.getPeriodEndDate());
        createCell(row, 2, period, null);
        createCell(row, 4, "Invoice Date:  " + formatDate(invoice.getInvoiceDate()), boldStyle);
        
        // Row 10: Headers
        row = sheet.createRow(rowNum++);
        createCell(row, 1, "Date", headerStyle);
        createCell(row, 2, "Address", headerStyle);
        createCell(row, 3, "Description", headerStyle);
        createCell(row, 4, "Price", headerStyle);
        
        // Work logs grouped by date
        List<WorkLog> sortedLogs = invoice.getWorkLogs().stream()
                .sorted((a, b) -> a.getWorkDate().compareTo(b.getWorkDate()))
                .toList();
        
        LocalDate currentDate = null;
        for (WorkLog workLog : sortedLogs) {
            row = sheet.createRow(rowNum++);
            
            // Only show date on first entry of each day
            if (!workLog.getWorkDate().equals(currentDate)) {
                currentDate = workLog.getWorkDate();
                createCell(row, 1, formatDayDate(currentDate), null);
            }
            
            createCell(row, 2, workLog.getJobAddress(), null);
            
            // Build description with time and hours
            String description = workLog.getWorkDescription() + " (" + 
                    workLog.getStartTime() + " - " + workLog.getEndTime() + ") " +
                    workLog.getHoursWorked() + " hrs";
            createCell(row, 3, description, null);
            
            Cell priceCell = createCell(row, 4, "", currencyStyle);
            priceCell.setCellValue(workLog.getTotalAmount().doubleValue());
        }
        
        rowNum += 5; // Skip rows
        
        // Totals
        row = sheet.createRow(rowNum++);
        createCell(row, 4, "Invoice Subtotal", boldStyle);
        Cell subtotalCell = createCell(row, 5, "", currencyStyle);
        subtotalCell.setCellValue(invoice.getSubtotal().doubleValue());
        
        row = sheet.createRow(rowNum++);
        createCell(row, 4, "GST ( If Registered )", boldStyle);
        Cell gstRateCell = createCell(row, 5, "", null);
        gstRateCell.setCellValue(invoice.getGstRate().doubleValue());
        
        row = sheet.createRow(rowNum++);
        createCell(row, 4, "GST Amount", boldStyle);
        Cell gstAmountCell = createCell(row, 5, "", currencyStyle);
        gstAmountCell.setCellValue(invoice.getGstAmount().doubleValue());
        
        // Bank details
        row = sheet.createRow(rowNum++);
        createCell(row, 1, "Bank Details: " + invoice.getTechnicianName(), boldStyle);
        
        row = sheet.createRow(rowNum++);
        createCell(row, 1, "BSB: " + (invoice.getBsb() != null ? invoice.getBsb() : ""), null);
        createCell(row, 4, "Deposit To Be Paid", boldStyle);
        Cell totalCell = createCell(row, 5, "", currencyStyle);
        totalCell.setCellValue(invoice.getTotalAmount().doubleValue());
        
        // Auto-size columns
        for (int i = 1; i <= 5; i++) {
            sheet.autoSizeColumn(i);
        }
        
        // Save file
        String fileName = "INV-" + formatDate(invoice.getInvoiceDate()) + 
                         "-Week-" + invoice.getWeekNumber() + 
                         "-Invoice-" + invoice.getTechnicianName().replace(" ", "-") + ".xlsx";
        
        Path outputPath = Paths.get(uploadDir, "invoices", fileName);
        Files.createDirectories(outputPath.getParent());
        
        try (FileOutputStream outputStream = new FileOutputStream(outputPath.toFile())) {
            workbook.write(outputStream);
        }
        
        workbook.close();
        
        // Update invoice with file path
        invoice.setFileUrl("invoices/" + fileName);
        invoiceRepository.save(invoice);
        
        log.info("Excel invoice generated: {}", fileName);
        
        return "invoices/" + fileName;
    }
    
    private String generateInvoiceNumber(User technician, LocalDate date) {
        String lastInvoiceNumber = invoiceRepository.findLatestInvoiceNumberByTechnician(technician);
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
    
    private Cell createCell(Row row, int column, String value, CellStyle style) {
        Cell cell = row.createCell(column);
        if (value != null) {
            cell.setCellValue(value);
        }
        if (style != null) {
            cell.setCellStyle(style);
        }
        return cell;
    }
    
    private CellStyle createHeaderStyle(Workbook workbook) {
        CellStyle style = workbook.createCellStyle();
        Font font = workbook.createFont();
        font.setBold(true);
        style.setFont(font);
        style.setAlignment(HorizontalAlignment.CENTER);
        return style;
    }
    
    private CellStyle createBoldStyle(Workbook workbook) {
        CellStyle style = workbook.createCellStyle();
        Font font = workbook.createFont();
        font.setBold(true);
        style.setFont(font);
        return style;
    }
    
    private CellStyle createCurrencyStyle(Workbook workbook) {
        CellStyle style = workbook.createCellStyle();
        style.setDataFormat(workbook.createDataFormat().getFormat("$#,##0.00"));
        return style;
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
}
