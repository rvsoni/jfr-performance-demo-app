package com.shopsphere.demo.service;

import com.shopsphere.demo.domain.Product;
import jakarta.annotation.PostConstruct;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class CatalogService {

    private final Map<String, Product> productMap = new ConcurrentHashMap<>();

    @PostConstruct
    public void init() {
        populateCatalog();
    }

    public void populateCatalog() {
        productMap.clear();
        String[] categories = {"Electronics", "Apparel", "Home & Kitchen", "Books", "Sports", "Beauty"};
        for (int i = 1; i <= 100; i++) {
            String id = "PROD-" + String.format("%03d", i);
            String category = categories[i % categories.length];
            String name = category + " Item " + i;
            BigDecimal price = BigDecimal.valueOf(10 + (i * 3.5)).setScale(2, java.math.RoundingMode.HALF_UP);
            int stock = 1000 + (i * 50);
            productMap.put(id, new Product(id, name, category, price, stock));
        }
    }

    public List<Product> getAllProducts() {
        return new ArrayList<>(productMap.values());
    }

    public Optional<Product> getProduct(String id) {
        return Optional.ofNullable(productMap.get(id));
    }

    public Product getProductOrThrow(String id) {
        Product p = productMap.get(id);
        if (p == null) {
            throw new IllegalArgumentException("Product not found: " + id);
        }
        return p;
    }
}
