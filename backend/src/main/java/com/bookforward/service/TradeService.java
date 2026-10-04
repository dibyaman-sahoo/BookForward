package com.bookforward.service;

import com.bookforward.dto.TradeDtos.*;
import com.bookforward.entity.*;
import com.bookforward.exception.ApiException;
import com.bookforward.mapper.ListingMapper;
import com.bookforward.notification.NotificationService;
import com.bookforward.repository.*;
import java.util.*;
import java.util.stream.Stream;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Purchase-request and order lifecycle. Listing rows are locked while state changes. */
@Service
@RequiredArgsConstructor
public class TradeService {
    private final PurchaseRequestRepository requests;
    private final OrderRepository orders;
    private final OrderStatusHistoryRepository history;
    private final ReviewRepository reviews;
    private final ListingRepository listings;
    private final UserRepository users;
    private final NotificationService notifications;
    private final ChatService chat;
    private final ListingMapper mapper;

    @Transactional
    public RequestDto createRequest(UUID buyerId, CreateRequestDto r) {
        Listing l = listings.findById(r.listingId()).orElseThrow(() -> ApiException.notFound("Listing"));
        if (l.getSeller().getId().equals(buyerId)) {
            throw ApiException.badRequest("OWN_LISTING", "You cannot request your own listing");
        }
        if (l.getStatus() != ListingStatus.ACTIVE) {
            throw ApiException.conflict("LISTING_UNAVAILABLE", "This book is no longer available");
        }
        if (requests.existsByListingIdAndBuyerIdAndStatus(l.getId(), buyerId, RequestStatus.PENDING)) {
            throw ApiException.conflict("REQUEST_EXISTS", "You already have a pending request for this book");
        }
        User buyer = users.getReferenceById(buyerId);
        PurchaseRequest pr = new PurchaseRequest();
        pr.setListing(l);
        pr.setBuyer(buyer);
        pr.setSeller(l.getSeller());
        pr.setStatus(RequestStatus.PENDING);
        pr.setMessage(r.message() == null || r.message().isBlank() ? null : r.message().trim());
        pr = requests.save(pr);

        Conversation c = chat.getOrCreate(l, buyerId, l.getSeller().getId());
        String intro = "Purchase request for \"" + l.getTitle() + "\"" + (pr.getMessage() == null ? "" : ": " + pr.getMessage());
        chat.post(c.getId(), buyerId, intro.length() > 2000 ? intro.substring(0, 2000) : intro, MessageType.SYSTEM);
        notifications.notify(l.getSeller(), NotificationType.REQUEST_RECEIVED, "New purchase request",
                buyer.getDisplayName() + " wants \"" + l.getTitle() + "\"", "#/dashboard?tab=received");
        return toDto(pr, null);
    }

    @Transactional
    public RequestDto updateRequest(UUID actorId, UUID requestId, RequestStatusUpdate u) {
        PurchaseRequest pr = requests.findById(requestId).orElseThrow(() -> ApiException.notFound("Request"));
        boolean isSeller = pr.getSeller().getId().equals(actorId);
        boolean isBuyer = pr.getBuyer().getId().equals(actorId);
        if (!isSeller && !isBuyer) throw ApiException.notFound("Request");
        if (pr.getStatus() != RequestStatus.PENDING) {
            throw ApiException.conflict("INVALID_TRANSITION", "This request is already " + pr.getStatus().name().toLowerCase());
        }
        boolean allowed = switch (u.status()) {
            case ACCEPTED, REJECTED -> isSeller;
            case CANCELLED -> isBuyer;
            default -> false;
        };
        if (!allowed) throw ApiException.forbidden("You cannot set this status on the request");

        PurchaseOrder order = null;
        User other = isSeller ? pr.getBuyer() : pr.getSeller();
        if (u.status() == RequestStatus.ACCEPTED) {
            Listing l = listings.findForUpdate(pr.getListing().getId()).orElseThrow(() -> ApiException.notFound("Listing"));
            if (l.getStatus() != ListingStatus.ACTIVE) {
                throw ApiException.conflict("LISTING_UNAVAILABLE", "This listing is no longer available");
            }
            l.setStatus(ListingStatus.RESERVED);
            pr.setStatus(RequestStatus.ACCEPTED);
            order = new PurchaseOrder();
            order.setPurchaseRequest(pr);
            order.setListing(l);
            order.setBuyer(pr.getBuyer());
            order.setSeller(pr.getSeller());
            order.setStatus(OrderStatus.CONFIRMED);
            order.setTotalAmount(l.getPrice());
            order.setCurrency("INR");
            order = orders.save(order);
            record(order, null, OrderStatus.CONFIRMED, actorId, "Request accepted");
            for (PurchaseRequest rival : requests.findByListingIdAndStatus(l.getId(), RequestStatus.PENDING)) {
                if (!rival.getId().equals(pr.getId())) {
                    rival.setStatus(RequestStatus.REJECTED);
                    notifications.notify(rival.getBuyer(), NotificationType.REQUEST_UPDATED, "Request not accepted",
                            "\"" + l.getTitle() + "\" was sold to another buyer.", "#/dashboard?tab=sent");
                }
            }
        } else {
            pr.setStatus(u.status());
        }
        String verb = u.status().name().toLowerCase();
        notifications.notify(other, NotificationType.REQUEST_UPDATED, "Request " + verb,
                "Your request for \"" + pr.getListing().getTitle() + "\" was " + verb + ".", "#/dashboard?tab=" + (isSeller ? "sent" : "received"));
        return toDto(pr, order);
    }

    @Transactional(readOnly = true)
    public List<RequestDto> mine(UUID userId, String role) {
        List<PurchaseRequest> list = "seller".equalsIgnoreCase(role)
                ? requests.findBySellerIdOrderByCreatedAtDesc(userId)
                : requests.findByBuyerIdOrderByCreatedAtDesc(userId);
        Map<UUID, String> covers = mapper.coverUrls(list.stream().map(r -> r.getListing().getId()).toList());
        return list.stream().map(r -> toDto(r, orders.findByPurchaseRequestId(r.getId()).orElse(null), covers.get(r.getListing().getId()))).toList();
    }

    @Transactional(readOnly = true)
    public List<OrderDto> orders(UUID userId) {
        List<PurchaseOrder> list = orders.findByBuyerIdOrSellerIdOrderByCreatedAtDesc(userId, userId);
        Map<UUID, String> covers = mapper.coverUrls(list.stream().map(o -> o.getListing().getId()).toList());
        return list.stream().map(o -> toDto(o, covers.get(o.getListing().getId()))).toList();
    }

    @Transactional(readOnly = true)
    public OrderDto order(UUID userId, UUID orderId) {
        PurchaseOrder o = visibleOrder(userId, orderId);
        return toDto(o, mapper.coverUrls(List.of(o.getListing().getId())).get(o.getListing().getId()));
    }

    @Transactional
    public OrderDto updateOrder(UUID actorId, UUID orderId, OrderStatusUpdate u) {
        PurchaseOrder o = visibleOrder(actorId, orderId);
        boolean isBuyer = o.getBuyer().getId().equals(actorId);
        boolean isSeller = o.getSeller().getId().equals(actorId);
        if (!OrderStateMachine.canTransition(o.getStatus(), u.status())) {
            throw ApiException.conflict("INVALID_TRANSITION", "Cannot move an order from " + o.getStatus() + " to " + u.status());
        }
        if (!OrderStateMachine.actorAllowed(u.status(), isBuyer, isSeller)) {
            throw ApiException.forbidden("You are not allowed to make this change");
        }
        if (u.status() == OrderStatus.CANCELLED && (u.reason() == null || u.reason().isBlank())) {
            throw ApiException.badRequest("REASON_REQUIRED", "Please give a reason for cancelling");
        }
        Listing l = listings.findForUpdate(o.getListing().getId()).orElseThrow(() -> ApiException.notFound("Listing"));
        OrderStatus old = o.getStatus();
        o.setStatus(u.status());
        if (u.status() == OrderStatus.COMPLETED) {
            l.setStatus(ListingStatus.SOLD);
        } else if (u.status() == OrderStatus.CANCELLED && l.getStatus() == ListingStatus.RESERVED) {
            l.setStatus(ListingStatus.ACTIVE);
        }
        record(o, old, u.status(), actorId, u.reason());
        User other = isBuyer ? o.getSeller() : o.getBuyer();
        notifications.notify(other, NotificationType.ORDER_UPDATED, "Order " + u.status().name().toLowerCase(),
                "Order for \"" + l.getTitle() + "\" is now " + u.status().name().toLowerCase() + ".", "#/dashboard?tab=orders");
        return toDto(o, null);
    }

    private PurchaseOrder visibleOrder(UUID userId, UUID orderId) {
        PurchaseOrder o = orders.findById(orderId).orElseThrow(() -> ApiException.notFound("Order"));
        if (!o.getBuyer().getId().equals(userId) && !o.getSeller().getId().equals(userId)) {
            throw ApiException.notFound("Order");
        }
        return o;
    }

    private void record(PurchaseOrder o, OrderStatus from, OrderStatus to, UUID actor, String reason) {
        OrderStatusHistory h = new OrderStatusHistory();
        h.setOrder(o);
        h.setOldStatus(from == null ? null : from.name());
        h.setNewStatus(to.name());
        h.setActor(users.getReferenceById(actor));
        h.setReason(reason);
        history.save(h);
    }

    private RequestDto toDto(PurchaseRequest r, PurchaseOrder o) {
        return toDto(r, o, mapper.coverUrls(List.of(r.getListing().getId())).get(r.getListing().getId()));
    }

    private RequestDto toDto(PurchaseRequest r, PurchaseOrder o, String cover) {
        return new RequestDto(r.getId(), r.getListing().getId(), r.getListing().getTitle(), cover,
                new PartyDto(r.getBuyer().getId(), r.getBuyer().getDisplayName()),
                new PartyDto(r.getSeller().getId(), r.getSeller().getDisplayName()),
                r.getStatus(), r.getMessage(), o == null ? null : o.getId(), r.getCreatedAt());
    }

    private OrderDto toDto(PurchaseOrder o, String cover) {
        List<HistoryDto> h = history.findByOrderIdOrderByCreatedAtAsc(o.getId()).stream()
                .map(x -> new HistoryDto(x.getOldStatus(), x.getNewStatus(), x.getActor().getDisplayName(), x.getReason(), x.getCreatedAt())).toList();
        return new OrderDto(o.getId(), o.getListing().getId(), o.getListing().getTitle(), cover,
                new PartyDto(o.getBuyer().getId(), o.getBuyer().getDisplayName()),
                new PartyDto(o.getSeller().getId(), o.getSeller().getDisplayName()),
                o.getStatus(), o.getTotalAmount(), o.getCurrency(),
                reviews.existsByOrderIdAndReviewerId(o.getId(), o.getBuyer().getId()), h, o.getCreatedAt());
    }
}
