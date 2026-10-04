package com.bookforward.payment;

import com.bookforward.entity.PaymentStatus;
import java.math.BigDecimal;
import java.util.UUID;
import org.springframework.stereotype.Component;

/** Default adapter: payment is settled in person at handover, so the record stays PENDING. */
@Component
public class OfflinePaymentProvider implements PaymentProvider {
    @Override public String name() { return "offline"; }

    @Override
    public PaymentResult initiate(String orderReference, BigDecimal amount, String currency) {
        return new PaymentResult("OFFLINE-" + UUID.randomUUID(), PaymentStatus.PENDING);
    }
}
