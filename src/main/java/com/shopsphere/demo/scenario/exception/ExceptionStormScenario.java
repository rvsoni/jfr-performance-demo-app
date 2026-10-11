package com.shopsphere.demo.scenario.exception;

import com.shopsphere.demo.scenario.BaseScenario;
import org.springframework.stereotype.Component;

import java.util.concurrent.atomic.LongAdder;

@Component
public class ExceptionStormScenario extends BaseScenario {

    public static final String ID = "exception-storm";

    private final LongAdder exceptionsThrown = new LongAdder();

    public ExceptionStormScenario() {
        super(
                ID,
                "Exception Storm from Control Flow Misuse",
                "Demonstrates CPU and allocation overhead caused by throwing and catching exceptions for standard business validations.",
                "jdk.JavaExceptionThrow events and stack trace generation overhead in application methods"
        );
    }

    public static class OrderValidationException extends Exception {
        public OrderValidationException(String message) {
            super(message);
        }
    }

    public record ValidationResult(boolean valid, String message) {
    }

    @Override
    protected void executeFaulty(int stepIndex) {
        // Repeatedly throws and catches exceptions for normal business checks
        for (int i = 0; i < 50; i++) {
            try {
                validateItemWithException(stepIndex, i);
            } catch (OrderValidationException e) {
                exceptionsThrown.increment();
                // Handled in catch block
            }
        }
    }

    @Override
    protected void executeCorrected(int stepIndex) {
        // Fast boolean / result check without generating stack traces
        for (int i = 0; i < 50; i++) {
            ValidationResult result = validateItemExplicit(stepIndex, i);
            if (!result.valid()) {
                // Handled cleanly
            }
        }
    }

    private void validateItemWithException(int stepIndex, int itemIndex) throws OrderValidationException {
        if ((stepIndex + itemIndex) % 2 == 0) {
            throw new OrderValidationException("Validation condition failed for item: " + itemIndex);
        }
    }

    private ValidationResult validateItemExplicit(int stepIndex, int itemIndex) {
        if ((stepIndex + itemIndex) % 2 == 0) {
            return new ValidationResult(false, "Validation condition failed for item: " + itemIndex);
        }
        return new ValidationResult(true, "OK");
    }

    @Override
    public java.util.Map<String, Object> getCustomMetrics() {
        return java.util.Map.of("totalExceptionsThrown", exceptionsThrown.sum());
    }
}
