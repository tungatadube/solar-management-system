package com.solar.management.repository;

import com.solar.management.entity.Invoice;
import com.solar.management.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface InvoiceRepository extends JpaRepository<Invoice, Long> {
    
    Optional<Invoice> findByInvoiceNumber(String invoiceNumber);
    
    List<Invoice> findByTechnician(User technician);
    
    List<Invoice> findByStatus(Invoice.InvoiceStatus status);
    
    @Query("SELECT i FROM Invoice i WHERE i.technician = :technician AND i.invoiceDate BETWEEN :startDate AND :endDate")
    List<Invoice> findByTechnicianAndDateRange(@Param("technician") User technician,
                                               @Param("startDate") LocalDate startDate,
                                               @Param("endDate") LocalDate endDate);
    
    @Query("SELECT i FROM Invoice i WHERE i.technician = :technician ORDER BY i.invoiceDate DESC")
    List<Invoice> findByTechnicianOrderByDateDesc(@Param("technician") User technician);
    
    @Query("SELECT MAX(i.invoiceNumber) FROM Invoice i WHERE i.technician = :technician")
    String findLatestInvoiceNumberByTechnician(@Param("technician") User technician);

    @Query("SELECT MAX(i.invoiceNumber) FROM Invoice i")
    String findLatestInvoiceNumber();
}
