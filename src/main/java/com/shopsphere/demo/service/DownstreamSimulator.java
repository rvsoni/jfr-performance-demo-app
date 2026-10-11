package com.shopsphere.demo.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.LockSupport;

@Service
public class DownstreamSimulator {

    private static final Logger log = LoggerFactory.getLogger(DownstreamSimulator.class);

    /**
     * Simulates a downstream HTTP/network call with a delay.
     * Uses LockSupport.parkNanos for precise bounded waiting.
     */
    public boolean callPaymentGateway(String customerId, double amount, long delayMs) {
        if (delayMs > 0) {
            LockSupport.parkNanos(TimeUnit.MILLISECONDS.toNanos(delayMs));
        }
        return true;
    }

    public boolean checkFraudRisk(String customerId, double amount, long delayMs) {
        if (delayMs > 0) {
            LockSupport.parkNanos(TimeUnit.MILLISECONDS.toNanos(delayMs));
        }
        return true;
    }

    public boolean notifyWarehouse(String orderId, long delayMs) {
        if (delayMs > 0) {
            LockSupport.parkNanos(TimeUnit.MILLISECONDS.toNanos(delayMs));
        }
        return true;
    }
}
