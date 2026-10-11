package com.shopsphere.demo.controller;

import com.shopsphere.demo.domain.InventoryReservationRequest;
import com.shopsphere.demo.domain.InventoryReservationResult;
import com.shopsphere.demo.service.InventoryService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/inventory")
public class InventoryController {

    private final InventoryService inventoryService;

    public InventoryController(InventoryService inventoryService) {
        this.inventoryService = inventoryService;
    }

    @GetMapping("/{productId}")
    public ResponseEntity<Map<String, Object>> getStock(@PathVariable String productId) {
        int stock = inventoryService.getStock(productId);
        return ResponseEntity.ok(Map.of("productId", productId, "stock", stock));
    }

    @PostMapping("/reserve")
    public ResponseEntity<InventoryReservationResult> reserve(@RequestBody InventoryReservationRequest request) {
        InventoryReservationResult result = inventoryService.reserve(request);
        return ResponseEntity.ok(result);
    }

    @PostMapping("/replenish")
    public ResponseEntity<Map<String, String>> replenish() {
        inventoryService.replenishAll();
        return ResponseEntity.ok(Map.of("status", "Inventory replenished successfully"));
    }
}
