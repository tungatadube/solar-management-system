package com.solar.management.repository;

import com.solar.management.entity.StockItem;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface StockItemRepository extends JpaRepository<StockItem, Long> {
    Optional<StockItem> findBySku(String sku);
    List<StockItem> findByCategory(StockItem.StockCategory category);

    @Query("SELECT s FROM StockItem s WHERE " +
           "LOWER(s.name) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
           "LOWER(s.description) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
           "LOWER(s.sku) LIKE LOWER(CONCAT('%', :keyword, '%'))")
    Page<StockItem> searchStockItems(@Param("keyword") String keyword, Pageable pageable);

    @Query("SELECT s FROM StockItem s JOIN s.stockLocations sl " +
           "GROUP BY s HAVING SUM(sl.quantity) <= s.minimumQuantity")
    List<StockItem> findLowStockItems();
}
