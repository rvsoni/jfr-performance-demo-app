package com.shopsphere.demo.service;

import com.shopsphere.demo.domain.OrderItem;
import com.shopsphere.demo.domain.PriceBreakdown;
import com.shopsphere.demo.domain.PricingRequest;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;

@Service
public class PricingService {

    public PriceBreakdown calculatePrice(PricingRequest request) {
        BigDecimal subtotal = BigDecimal.ZERO;
        for (OrderItem item : request.items()) {
            subtotal = subtotal.add(item.unitPrice().multiply(BigDecimal.valueOf(item.quantity())));
        }

        List<String> appliedRules = new ArrayList<>();

        // Tier discount
        BigDecimal tierDiscount = BigDecimal.ZERO;
        if ("PLATINUM".equalsIgnoreCase(request.membershipTier())) {
            tierDiscount = subtotal.multiply(BigDecimal.valueOf(0.15));
            appliedRules.add("15% Platinum Membership Discount");
        } else if ("GOLD".equalsIgnoreCase(request.membershipTier())) {
            tierDiscount = subtotal.multiply(BigDecimal.valueOf(0.10));
            appliedRules.add("10% Gold Membership Discount");
        } else if ("SILVER".equalsIgnoreCase(request.membershipTier())) {
            tierDiscount = subtotal.multiply(BigDecimal.valueOf(0.05));
            appliedRules.add("5% Silver Membership Discount");
        }

        // Promo discount
        BigDecimal promoDiscount = BigDecimal.ZERO;
        if ("SAVE20".equalsIgnoreCase(request.promoCode())) {
            promoDiscount = subtotal.multiply(BigDecimal.valueOf(0.20));
            appliedRules.add("Promo SAVE20 Applied");
        } else if ("WELCOME10".equalsIgnoreCase(request.promoCode())) {
            promoDiscount = subtotal.multiply(BigDecimal.valueOf(0.10));
            appliedRules.add("Promo WELCOME10 Applied");
        }

        BigDecimal discount = tierDiscount.add(promoDiscount).setScale(2, RoundingMode.HALF_UP);
        BigDecimal discountedSubtotal = subtotal.subtract(discount).max(BigDecimal.ZERO);

        // Tax (e.g. 8.25%)
        BigDecimal tax = discountedSubtotal.multiply(BigDecimal.valueOf(0.0825)).setScale(2, RoundingMode.HALF_UP);

        // Shipping
        BigDecimal shipping = subtotal.compareTo(BigDecimal.valueOf(100)) >= 0
                ? BigDecimal.ZERO
                : BigDecimal.valueOf(9.99);
        if (shipping.compareTo(BigDecimal.ZERO) == 0) {
            appliedRules.add("Free Shipping (Orders over $100)");
        }

        BigDecimal total = discountedSubtotal.add(tax).add(shipping).setScale(2, RoundingMode.HALF_UP);

        return new PriceBreakdown(
                subtotal.setScale(2, RoundingMode.HALF_UP),
                tax,
                shipping,
                discount,
                total,
                appliedRules
        );
    }
}
