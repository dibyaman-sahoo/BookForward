package com.bookforward.payment;

import com.bookforward.entity.PaymentStatus;
import java.math.BigDecimal;

/** Provider-neutral payment boundary. No gateway-specific types may leak outside adapters. */
public interface PaymentProvider {
    String name();
    PaymentResult initiate(String orderReference, BigDecimal amount, String currency);

    record PaymentResult(String providerReference, PaymentStatus status) {}
}
