package com.bookforward.controller;

import com.bookforward.dto.*;
import com.bookforward.dto.AuthDtos.*;
import com.bookforward.dto.ListingDtos.*;
import com.bookforward.dto.TradeDtos.*;
import com.bookforward.dto.ChatDtos.*;
import com.bookforward.dto.EngagementDtos.*;
import com.bookforward.dto.AdminDtos.*;
import com.bookforward.entity.*;
import com.bookforward.security.UserPrincipal;
import com.bookforward.service.*;
import jakarta.validation.Valid;
import java.util.*;
import lombok.RequiredArgsConstructor;
import org.springframework.http.*;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import com.bookforward.payment.PaymentService;

@RestController
@RequiredArgsConstructor
public class TradeController {
    private final TradeService trade;
    private final PaymentService payments;
    private final ReviewService reviews;

    @PostMapping("/api/requests")
    @ResponseStatus(HttpStatus.CREATED)
    public RequestDto create(@AuthenticationPrincipal UserPrincipal p, @Valid @RequestBody CreateRequestDto r) { return trade.createRequest(p.getId(), r); }

    @GetMapping("/api/requests/mine")
    public List<RequestDto> mine(@AuthenticationPrincipal UserPrincipal p, @RequestParam(defaultValue = "buyer") String role) { return trade.mine(p.getId(), role); }

    @PatchMapping("/api/requests/{id}/status")
    public RequestDto updateRequest(@PathVariable UUID id, @AuthenticationPrincipal UserPrincipal p, @Valid @RequestBody RequestStatusUpdate u) { return trade.updateRequest(p.getId(), id, u); }

    @GetMapping("/api/orders")
    public List<OrderDto> orders(@AuthenticationPrincipal UserPrincipal p) { return trade.orders(p.getId()); }

    @GetMapping("/api/orders/{id}")
    public OrderDto order(@PathVariable UUID id, @AuthenticationPrincipal UserPrincipal p) { return trade.order(p.getId(), id); }

    @PatchMapping("/api/orders/{id}/status")
    public OrderDto updateOrder(@PathVariable UUID id, @AuthenticationPrincipal UserPrincipal p, @Valid @RequestBody OrderStatusUpdate u) { return trade.updateOrder(p.getId(), id, u); }

    @PostMapping("/api/orders/{id}/payments")
    @ResponseStatus(HttpStatus.CREATED)
    public PaymentDto pay(@PathVariable UUID id, @AuthenticationPrincipal UserPrincipal p) { return payments.initiate(id, p.getId()); }

    @GetMapping("/api/orders/{id}/payments")
    public List<PaymentDto> payments(@PathVariable UUID id, @AuthenticationPrincipal UserPrincipal p) { return payments.list(id, p.getId()); }

    @PostMapping("/api/reviews")
    @ResponseStatus(HttpStatus.CREATED)
    public ReviewDto review(@AuthenticationPrincipal UserPrincipal p, @Valid @RequestBody ReviewRequest r) { return reviews.create(p.getId(), r); }
}
