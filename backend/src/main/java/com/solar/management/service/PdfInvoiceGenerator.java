package com.solar.management.service;

import com.itextpdf.kernel.colors.Color;
import com.itextpdf.kernel.colors.DeviceRgb;
import com.itextpdf.kernel.font.PdfFont;
import com.itextpdf.kernel.font.PdfFontFactory;
import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.kernel.pdf.PdfWriter;
import com.itextpdf.layout.Document;
import com.itextpdf.layout.element.Cell;
import com.itextpdf.layout.element.Paragraph;
import com.itextpdf.layout.element.Table;
import com.itextpdf.layout.property.TextAlignment;
import com.itextpdf.layout.property.UnitValue;
import com.solar.management.entity.Invoice;
import com.solar.management.entity.WorkLog;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.io.FileOutputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Component
@Slf4j
public class PdfInvoiceGenerator {

    // Modern Color Palette
    private static final Color SLATE_700 = new DeviceRgb(51, 65, 85);        // Headers
    private static final Color TEAL_600 = new DeviceRgb(13, 148, 136);       // Labels/Accent
    private static final Color INDIGO_600 = new DeviceRgb(79, 70, 229);      // Secondary accent
    private static final Color GRAY_50 = new DeviceRgb(249, 250, 251);       // Light background
    private static final Color GRAY_800 = new DeviceRgb(31, 41, 55);         // Dark text
    private static final Color WHITE = new DeviceRgb(255, 255, 255);

    public void generatePdfInvoice(Invoice invoice, Path outputPath) throws IOException {
        try (PdfWriter writer = new PdfWriter(new FileOutputStream(outputPath.toFile()));
             PdfDocument pdf = new PdfDocument(writer);
             Document document = new Document(pdf)) {

            // Use Helvetica (similar to Inter - clean and modern)
            PdfFont regularFont = PdfFontFactory.createFont("Helvetica");
            PdfFont boldFont = PdfFontFactory.createFont("Helvetica-Bold");

            document.setFont(regularFont);
            document.setFontSize(9);
            document.setMargins(30, 30, 30, 30); // Smaller margins

            // Header Section
            addHeader(document, invoice, boldFont);

            // Technician and Bill-To Information
            addContactInfo(document, invoice, regularFont, boldFont);

            // Invoice Details
            addInvoiceDetails(document, invoice, regularFont, boldFont);

            // Work Logs Table
            addWorkLogsTable(document, invoice, regularFont, boldFont);

            // Totals and Bank Details
            addTotalsAndBankDetails(document, invoice, regularFont, boldFont);

            log.info("PDF invoice generated: {}", outputPath.getFileName());
        }
    }

    private void addHeader(Document document, Invoice invoice, PdfFont boldFont) {
        Paragraph header = new Paragraph("INVOICE")
                .setFont(boldFont)
                .setFontSize(20)
                .setFontColor(SLATE_700)
                .setTextAlignment(TextAlignment.CENTER)
                .setMarginBottom(10);
        document.add(header);
    }

    private void addContactInfo(Document document, Invoice invoice, PdfFont regularFont, PdfFont boldFont) {
        // Create a 2-column table for technician and bill-to info
        float[] columnWidths = {1, 1};
        Table table = new Table(UnitValue.createPercentArray(columnWidths))
                .useAllAvailableWidth()
                .setMarginBottom(8);

        // Technician Info (Left) - compact single-line format
        Cell techCell = new Cell()
                .setBorder(null)
                .setPadding(2)
                .add(new Paragraph("From:").setFont(boldFont).setFontSize(9).setFontColor(TEAL_600).setMarginBottom(2))
                .add(new Paragraph(invoice.getTechnicianName()).setFont(boldFont).setFontSize(10).setFontColor(GRAY_800).setMarginBottom(1))
                .add(new Paragraph(invoice.getTechnicianAddress() + " | ABN: " + invoice.getTechnicianABN()).setFont(regularFont).setFontSize(8).setFontColor(GRAY_800).setMarginBottom(1))
                .add(new Paragraph(invoice.getTechnicianEmail() + " | " + invoice.getTechnicianPhone()).setFont(regularFont).setFontSize(8).setFontColor(GRAY_800));

        // Bill To Info (Right) - compact single-line format
        Cell billToCell = new Cell()
                .setBorder(null)
                .setPadding(2)
                .setTextAlignment(TextAlignment.RIGHT)
                .add(new Paragraph("Bill To:").setFont(boldFont).setFontSize(9).setFontColor(TEAL_600).setMarginBottom(2))
                .add(new Paragraph(invoice.getBillToName()).setFont(boldFont).setFontSize(10).setFontColor(GRAY_800).setMarginBottom(1))
                .add(new Paragraph(invoice.getBillToAddress()).setFont(regularFont).setFontSize(8).setFontColor(GRAY_800).setMarginBottom(1))
                .add(new Paragraph(invoice.getBillToEmail() + " | " + invoice.getBillToPhone()).setFont(regularFont).setFontSize(8).setFontColor(GRAY_800));

        table.addCell(techCell);
        table.addCell(billToCell);
        document.add(table);
    }

    private void addInvoiceDetails(Document document, Invoice invoice, PdfFont regularFont, PdfFont boldFont) {
        float[] columnWidths = {1, 1, 1, 1};
        Table table = new Table(UnitValue.createPercentArray(columnWidths))
                .useAllAvailableWidth()
                .setMarginBottom(8);

        // Header row with modern teal background
        addDetailCell(table, "Invoice #", boldFont, TEAL_600, WHITE, true);
        addDetailCell(table, "Week #", boldFont, TEAL_600, WHITE, true);
        addDetailCell(table, "Invoice Date", boldFont, TEAL_600, WHITE, true);
        addDetailCell(table, "Period", boldFont, TEAL_600, WHITE, true);

        // Data row
        addDetailCell(table, invoice.getInvoiceNumber(), regularFont, WHITE, GRAY_800, false);
        addDetailCell(table, invoice.getWeekNumber().toString(), regularFont, WHITE, GRAY_800, false);
        addDetailCell(table, formatDate(invoice.getInvoiceDate()), regularFont, WHITE, GRAY_800, false);
        addDetailCell(table, formatDate(invoice.getPeriodStartDate()) + " - " + formatDate(invoice.getPeriodEndDate()),
                     regularFont, WHITE, GRAY_800, false);

        document.add(table);
    }

    private void addDetailCell(Table table, String text, PdfFont font, Color bgColor, Color textColor, boolean isHeader) {
        Cell cell = new Cell()
                .add(new Paragraph(text).setFont(font).setFontSize(isHeader ? 9 : 8).setFontColor(textColor))
                .setBackgroundColor(bgColor)
                .setPadding(4)
                .setTextAlignment(TextAlignment.CENTER);
        table.addCell(cell);
    }

    private void addWorkLogsTable(Document document, Invoice invoice, PdfFont regularFont, PdfFont boldFont) {
        // Add section title
        Paragraph workLogsTitle = new Paragraph("Work Performed")
                .setFont(boldFont)
                .setFontSize(11)
                .setFontColor(SLATE_700)
                .setMarginTop(5)
                .setMarginBottom(5);
        document.add(workLogsTitle);

        float[] columnWidths = {1.5f, 2.5f, 4.5f, 1.5f};
        Table table = new Table(UnitValue.createPercentArray(columnWidths))
                .useAllAvailableWidth()
                .setMarginBottom(8);

        // Header row with slate background
        addWorkLogHeaderCell(table, "Date", boldFont);
        addWorkLogHeaderCell(table, "Address", boldFont);
        addWorkLogHeaderCell(table, "Description", boldFont);
        addWorkLogHeaderCell(table, "Amount", boldFont);

        // Work log rows
        List<WorkLog> sortedLogs = invoice.getWorkLogs().stream()
                .sorted((a, b) -> a.getWorkDate().compareTo(b.getWorkDate()))
                .toList();

        boolean alternate = false;
        LocalDate currentDate = null;

        for (WorkLog workLog : sortedLogs) {
            Color rowColor = alternate ? GRAY_50 : WHITE;

            // Date (only show if different from previous)
            String dateStr = "";
            if (!workLog.getWorkDate().equals(currentDate)) {
                currentDate = workLog.getWorkDate();
                dateStr = formatDayDate(currentDate);
            }
            addWorkLogCell(table, dateStr, regularFont, rowColor);

            // Address
            addWorkLogCell(table, workLog.getJobAddress(), regularFont, rowColor);

            // Description with time and hours on same line
            String description = workLog.getWorkDescription() + " (" +
                    workLog.getStartTime() + "-" + workLog.getEndTime() + ", " +
                    workLog.getHoursWorked() + "hrs)";
            addWorkLogCell(table, description, regularFont, rowColor);

            // Amount
            addWorkLogCell(table, "$" + workLog.getTotalAmount().toString(), regularFont, rowColor);

            alternate = !alternate;
        }

        document.add(table);
    }

    private void addWorkLogHeaderCell(Table table, String text, PdfFont boldFont) {
        Cell cell = new Cell()
                .add(new Paragraph(text).setFont(boldFont).setFontSize(9).setFontColor(WHITE))
                .setBackgroundColor(SLATE_700)
                .setPadding(4)
                .setTextAlignment(TextAlignment.LEFT);
        table.addCell(cell);
    }

    private void addWorkLogCell(Table table, String text, PdfFont regularFont, Color bgColor) {
        Cell cell = new Cell()
                .add(new Paragraph(text).setFont(regularFont).setFontSize(8).setFontColor(GRAY_800))
                .setBackgroundColor(bgColor)
                .setPadding(3)
                .setTextAlignment(TextAlignment.LEFT);
        table.addCell(cell);
    }

    private void addTotalsAndBankDetails(Document document, Invoice invoice, PdfFont regularFont, PdfFont boldFont) {
        // Totals table (right-aligned)
        float[] totalsColumnWidths = {3, 1};
        Table totalsTable = new Table(UnitValue.createPercentArray(totalsColumnWidths))
                .setWidth(UnitValue.createPercentValue(50))
                .setHorizontalAlignment(com.itextpdf.layout.property.HorizontalAlignment.RIGHT)
                .setMarginTop(5)
                .setMarginBottom(8);

        // Subtotal
        addTotalRow(totalsTable, "Subtotal:", "$" + invoice.getSubtotal().toString(), regularFont, boldFont, false);

        // GST
        addTotalRow(totalsTable, "GST (" + invoice.getGstRate().multiply(new BigDecimal("100")).toString() + "%):",
                   "$" + invoice.getGstAmount().toString(), regularFont, boldFont, false);

        // Total
        addTotalRow(totalsTable, "Total Amount:", "$" + invoice.getTotalAmount().toString(), boldFont, boldFont, true);

        document.add(totalsTable);

        // Bank Details Section
        if (invoice.getBankName() != null) {
            Paragraph bankTitle = new Paragraph("Payment Details")
                    .setFont(boldFont)
                    .setFontSize(11)
                    .setFontColor(SLATE_700)
                    .setMarginTop(5)
                    .setMarginBottom(5);
            document.add(bankTitle);

            Table bankTable = new Table(UnitValue.createPercentArray(new float[]{1, 2}))
                    .setWidth(UnitValue.createPercentValue(50))
                    .setMarginBottom(5);

            addBankDetailRow(bankTable, "Bank:", invoice.getBankName(), regularFont, boldFont);
            addBankDetailRow(bankTable, "BSB:", invoice.getBsb(), regularFont, boldFont);
            addBankDetailRow(bankTable, "Account:", invoice.getAccountNumber(), regularFont, boldFont);
            addBankDetailRow(bankTable, "Account Name:", invoice.getTechnicianName(), regularFont, boldFont);

            document.add(bankTable);
        }
    }

    private void addTotalRow(Table table, String label, String amount, PdfFont labelFont, PdfFont amountFont, boolean isTotal) {
        Color bgColor = isTotal ? INDIGO_600 : WHITE;
        Color textColor = isTotal ? WHITE : GRAY_800;

        Cell labelCell = new Cell()
                .add(new Paragraph(label).setFont(labelFont).setFontSize(9).setFontColor(textColor))
                .setBackgroundColor(bgColor)
                .setPadding(5)
                .setTextAlignment(TextAlignment.RIGHT)
                .setBorder(null);

        Cell amountCell = new Cell()
                .add(new Paragraph(amount).setFont(amountFont).setFontSize(9).setFontColor(textColor))
                .setBackgroundColor(bgColor)
                .setPadding(5)
                .setTextAlignment(TextAlignment.RIGHT)
                .setBorder(null);

        table.addCell(labelCell);
        table.addCell(amountCell);
    }

    private void addBankDetailRow(Table table, String label, String value, PdfFont regularFont, PdfFont boldFont) {
        Cell labelCell = new Cell()
                .add(new Paragraph(label).setFont(boldFont).setFontSize(8).setFontColor(TEAL_600))
                .setBackgroundColor(GRAY_50)
                .setPadding(4)
                .setBorder(null);

        Cell valueCell = new Cell()
                .add(new Paragraph(value).setFont(regularFont).setFontSize(8).setFontColor(GRAY_800))
                .setBackgroundColor(WHITE)
                .setPadding(4)
                .setBorder(null);

        table.addCell(labelCell);
        table.addCell(valueCell);
    }

    private String formatDate(LocalDate date) {
        return date.format(DateTimeFormatter.ofPattern("dd/MM/yy"));
    }

    private String formatDayDate(LocalDate date) {
        return date.format(DateTimeFormatter.ofPattern("EEEE dd MMMM yyyy"));
    }
}
