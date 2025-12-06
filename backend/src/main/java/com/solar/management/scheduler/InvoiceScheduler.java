package com.solar.management.scheduler;

import com.solar.management.entity.Invoice;
import com.solar.management.service.InvoiceService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@EnableScheduling
@RequiredArgsConstructor
@Slf4j
public class InvoiceScheduler {

    private final InvoiceService invoiceService;

    /**
     * Automatically generate weekly invoices every Friday at 9:00 PM Adelaide time
     * Covers Monday-Friday work for all technicians
     */
    @Scheduled(cron = "0 0 21 ? * FRI", zone = "Australia/Adelaide")
    public void scheduleWeeklyInvoices() {
        log.info("Starting scheduled weekly invoice generation...");

        try {
            List<Invoice> generatedInvoices = invoiceService.generateWeeklyInvoices();

            log.info("Scheduled invoice generation completed successfully. Generated {} invoices",
                    generatedInvoices.size());

            // TODO: Send emails with generated invoices when email functionality is implemented

        } catch (Exception e) {
            log.error("Error during scheduled invoice generation", e);
        }
    }
}
