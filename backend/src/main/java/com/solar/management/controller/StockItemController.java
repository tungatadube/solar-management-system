package com.solar.management.controller;

import com.solar.management.entity.StockItem;
import com.solar.management.service.StockItemService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/stock")
@RequiredArgsConstructor
@CrossOrigin(origins = {"http://localhost:3000", "http://localhost"})
public class StockItemController {

    private final StockItemService stockItemService;

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'TECHNICIAN')")
    public ResponseEntity<List<StockItem>> getAllStockItems() {
        List<StockItem> stockItems = stockItemService.getAllStockItems();
        return ResponseEntity.ok(stockItems);
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'TECHNICIAN')")
    public ResponseEntity<StockItem> getStockItemById(@PathVariable Long id) {
        return stockItemService.getStockItemById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/sku/{sku}")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'TECHNICIAN')")
    public ResponseEntity<StockItem> getStockItemBySku(@PathVariable String sku) {
        return stockItemService.getStockItemBySku(sku)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/category/{category}")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'TECHNICIAN')")
    public ResponseEntity<List<StockItem>> getStockItemsByCategory(
            @PathVariable StockItem.StockCategory category) {
        List<StockItem> stockItems = stockItemService.getStockItemsByCategory(category);
        return ResponseEntity.ok(stockItems);
    }

    @GetMapping("/low-stock")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'TECHNICIAN')")
    public ResponseEntity<List<StockItem>> getLowStockItems() {
        List<StockItem> lowStockItems = stockItemService.getLowStockItems();
        return ResponseEntity.ok(lowStockItems);
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    public ResponseEntity<StockItem> createStockItem(@RequestBody StockItem stockItem) {
        StockItem created = stockItemService.createStockItem(stockItem);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    public ResponseEntity<StockItem> updateStockItem(
            @PathVariable Long id,
            @RequestBody StockItem stockItem) {
        try {
            StockItem updated = stockItemService.updateStockItem(id, stockItem);
            return ResponseEntity.ok(updated);
        } catch (RuntimeException e) {
            return ResponseEntity.notFound().build();
        }
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> deleteStockItem(@PathVariable Long id) {
        stockItemService.deleteStockItem(id);
        return ResponseEntity.noContent().build();
    }
}
