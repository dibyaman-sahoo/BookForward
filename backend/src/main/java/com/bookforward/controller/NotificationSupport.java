package com.bookforward.controller;

import com.bookforward.dto.EngagementDtos.FeaturesDto;
import com.bookforward.payment.PaymentService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/** Small read-only helper exposing public feature flags. */
@Component
@RequiredArgsConstructor
class NotificationSupport {
    private final PaymentService payments;

    FeaturesDto features() { return new FeaturesDto(payments.isEnabled()); }
}
