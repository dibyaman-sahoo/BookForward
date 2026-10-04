package com.bookforward.payment;

import com.bookforward.config.AppProperties;
import com.bookforward.dto.TradeDtos.PaymentDto;
import com.bookforward.entity.*;
import com.bookforward.exception.ApiException;
import com.bookforward.repository.*;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class PaymentService {
    private final AppProperties props;
    private final Map<String, PaymentProvider> providers;
    private final OrderRepository orders;
    private final PaymentRepository payments;

    public PaymentService(AppProperties props, List<PaymentProvider> providers, OrderRepository orders, PaymentRepository payments) {
        this.props = props;
        this.providers = providers.stream().collect(Collectors.toMap(PaymentProvider::name, Function.identity()));
        this.orders = orders;
        this.payments = payments;
    }

    public boolean isEnabled() { return props.payment().enabled(); }

    @Transactional
    public PaymentDto initiate(UUID orderId, UUID userId) {
        if (!isEnabled()) {
            throw new ApiException(HttpStatus.SERVICE_UNAVAILABLE, "PAYMENT_DISABLED", "Online payment is not enabled on this deployment");
        }
        PurchaseOrder o = orders.findById(orderId).orElseThrow(() -> ApiException.notFound("Order"));
        if (!o.getBuyer().getId().equals(userId)) {
            throw ApiException.forbidden("Only the buyer can start a payment");
        }
        if (o.getStatus() == OrderStatus.COMPLETED || o.getStatus() == OrderStatus.CANCELLED) {
            throw ApiException.conflict("ORDER_CLOSED", "This order is already closed");
        }
        PaymentProvider provider = providers.get(props.payment().provider());
        if (provider == null) {
            throw new ApiException(HttpStatus.SERVICE_UNAVAILABLE, "PAYMENT_PROVIDER_MISSING", "Payment provider is not configured");
        }
        var result = provider.initiate(o.getId().toString(), o.getTotalAmount(), o.getCurrency());
        Payment p = new Payment();
        p.setOrder(o);
        p.setProvider(provider.name());
        p.setProviderReference(result.providerReference());
        p.setStatus(result.status());
        p.setAmount(o.getTotalAmount());
        p.setCurrency(o.getCurrency());
        return toDto(payments.save(p));
    }

    @Transactional(readOnly = true)
    public List<PaymentDto> list(UUID orderId, UUID userId) {
        PurchaseOrder o = orders.findById(orderId).orElseThrow(() -> ApiException.notFound("Order"));
        if (!o.getBuyer().getId().equals(userId) && !o.getSeller().getId().equals(userId)) {
            throw ApiException.notFound("Order");
        }
        return payments.findByOrderIdOrderByCreatedAtDesc(orderId).stream().map(this::toDto).toList();
    }

    private PaymentDto toDto(Payment p) {
        return new PaymentDto(p.getId(), p.getProvider(), p.getProviderReference(), p.getStatus(), p.getAmount(), p.getCurrency());
    }
}
