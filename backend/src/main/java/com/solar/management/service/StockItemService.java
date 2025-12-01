package com.solar.management.service;

import com.solar.management.entity.StockItem;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Optional;

public interface StockItemService {
    List<StockItem> getAllStockItems();
    Optional<StockItem> getStockItemById(Long id);
    Optional<StockItem> getStockItemBySku(String sku);
    List<StockItem> getStockItemsByCategory(StockItem.StockCategory category);
    List<StockItem> getLowStockItems();
    Page<StockItem> searchStockItems(String keyword, Pageable pageable);
    StockItem createStockItem(StockItem stockItem);
    StockItem updateStockItem(Long id, StockItem stockItem);
    void deleteStockItem(Long id);
}
