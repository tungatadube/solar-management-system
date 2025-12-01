package com.solar.management.service.impl;

import com.solar.management.entity.StockItem;
import com.solar.management.repository.StockItemRepository;
import com.solar.management.service.StockItemService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Transactional
public class StockItemServiceImpl implements StockItemService {

    private final StockItemRepository stockItemRepository;

    @Override
    @Transactional(readOnly = true)
    public List<StockItem> getAllStockItems() {
        return stockItemRepository.findAll();
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<StockItem> getStockItemById(Long id) {
        return stockItemRepository.findById(id);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<StockItem> getStockItemBySku(String sku) {
        return stockItemRepository.findBySku(sku);
    }

    @Override
    @Transactional(readOnly = true)
    public List<StockItem> getStockItemsByCategory(StockItem.StockCategory category) {
        return stockItemRepository.findByCategory(category);
    }

    @Override
    @Transactional(readOnly = true)
    public List<StockItem> getLowStockItems() {
        return stockItemRepository.findLowStockItems();
    }

    @Override
    @Transactional(readOnly = true)
    public Page<StockItem> searchStockItems(String keyword, Pageable pageable) {
        return stockItemRepository.searchStockItems(keyword, pageable);
    }

    @Override
    public StockItem createStockItem(StockItem stockItem) {
        return stockItemRepository.save(stockItem);
    }

    @Override
    public StockItem updateStockItem(Long id, StockItem stockItem) {
        return stockItemRepository.findById(id)
                .map(existing -> {
                    existing.setSku(stockItem.getSku());
                    existing.setName(stockItem.getName());
                    existing.setDescription(stockItem.getDescription());
                    existing.setCategory(stockItem.getCategory());
                    existing.setUnit(stockItem.getUnit());
                    existing.setUnitPrice(stockItem.getUnitPrice());
                    existing.setMinimumQuantity(stockItem.getMinimumQuantity());
                    existing.setReorderLevel(stockItem.getReorderLevel());
                    existing.setBarcode(stockItem.getBarcode());
                    existing.setImageUrl(stockItem.getImageUrl());
                    return stockItemRepository.save(existing);
                })
                .orElseThrow(() -> new RuntimeException("StockItem not found with id: " + id));
    }

    @Override
    public void deleteStockItem(Long id) {
        stockItemRepository.deleteById(id);
    }
}
