package com.shopsphere.demo.service;

import com.shopsphere.demo.domain.InventoryReservationRequest;
import com.shopsphere.demo.domain.InventoryReservationResult;
import com.shopsphere.demo.domain.OrderItem;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.LockSupport;

@Service
public class InventoryService {

    private static final Logger log = LoggerFactory.getLogger(InventoryService.class);

    private final Map<String, AtomicInteger> inventory = new ConcurrentHashMap<>();
    private final Object globalMonitor = new Object();

    @PostConstruct
    public void init() {
        replenishAll();
    }

    public void replenishAll() {
        for (int i = 1; i <= 100; i++) {
            String id = "PROD-" + String.format("%03d", i);
            inventory.put(id, new AtomicInteger(100_000));
        }
    }

    public int getStock(String productId) {
        AtomicInteger stock = inventory.get(productId);
        return stock != null ? stock.get() : 0;
    }

    /**
     * Standard non-contended inventory reservation.
     */
    public InventoryReservationResult reserve(InventoryReservationRequest request) {
        List<String> reserved = new ArrayList<>();
        for (OrderItem item : request.items()) {
            AtomicInteger stock = inventory.computeIfAbsent(item.productId(), k -> new AtomicInteger(100_000));
            int current = stock.get();
            if (current >= item.quantity()) {
                stock.addAndGet(-item.quantity());
                reserved.add(item.productId());
            } else {
                return new InventoryReservationResult(false, request.orderId(), "Insufficient stock for product " + item.productId(), reserved);
            }
        }
        return new InventoryReservationResult(true, request.orderId(), "Reservation successful", reserved);
    }

    /**
     * Faulty reservation: Synchronizes on a single shared global monitor across all products,
     * performs excessive calculation and simulated verification delay inside the monitor.
     * Produces high JavaMonitorEnter contention.
     */
    public InventoryReservationResult reserveWithHeavyLock(InventoryReservationRequest request, long criticalSectionDelayMs) {
        synchronized (globalMonitor) {
            // Expensive calculation inside lock
            long checksum = 0;
            for (int i = 0; i < 50_000; i++) {
                checksum += (i * 31L) ^ request.orderId().hashCode();
            }

            if (criticalSectionDelayMs > 0) {
                LockSupport.parkNanos(java.util.concurrent.TimeUnit.MILLISECONDS.toNanos(criticalSectionDelayMs));
            }

            List<String> reserved = new ArrayList<>();
            for (OrderItem item : request.items()) {
                AtomicInteger stock = inventory.computeIfAbsent(item.productId(), k -> new AtomicInteger(100_000));
                if (stock.get() >= item.quantity()) {
                    stock.addAndGet(-item.quantity());
                    reserved.add(item.productId());
                } else {
                    return new InventoryReservationResult(false, request.orderId(), "Insufficient stock for product " + item.productId() + " (checksum=" + checksum + ")", reserved);
                }
            }
            return new InventoryReservationResult(true, request.orderId(), "Reservation successful (checksum=" + checksum + ")", reserved);
        }
    }

    /**
     * Corrected reservation: Uses atomic operations on individual products without global monitor.
     */
    public InventoryReservationResult reserveFineGrained(InventoryReservationRequest request) {
        List<String> reserved = new ArrayList<>();
        for (OrderItem item : request.items()) {
            AtomicInteger stock = inventory.computeIfAbsent(item.productId(), k -> new AtomicInteger(100_000));
            // Optimistic update
            int prev;
            int next;
            do {
                prev = stock.get();
                if (prev < item.quantity()) {
                    return new InventoryReservationResult(false, request.orderId(), "Insufficient stock for " + item.productId(), reserved);
                }
                next = prev - item.quantity();
            } while (!stock.compareAndSet(prev, next));

            reserved.add(item.productId());
        }
        return new InventoryReservationResult(true, request.orderId(), "Reservation successful", reserved);
    }
}
