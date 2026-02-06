package com.example.shopping.order;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/orders")
public class OrderController {
  private final OrderRepository repository;
  private final CatalogClient catalogClient;
  private final AuditClient auditClient;

  public OrderController(OrderRepository repository, CatalogClient catalogClient, AuditClient auditClient) {
    this.repository = repository;
    this.catalogClient = catalogClient;
    this.auditClient = auditClient;
  }

  @GetMapping
  public List<Order> list(
      @RequestHeader(name = "X-Actor-Id", required = false) String actorId,
      @RequestHeader(name = "X-Actor-Roles", required = false) String actorRoles,
      @RequestHeader(name = "X-Request-Path", required = false) String path,
      @RequestHeader(name = "X-Request-Method", required = false) String method) {
    List<Order> orders = repository.findAll();
    auditClient.emit(auditClient.request(
        "Order",
        null,
        "READ_LIST",
        actorId,
        actorRoles,
        path,
        method,
        null,
        orders));
    return orders;
  }

  @GetMapping("/{id}")
  public Order get(
      @PathVariable Long id,
      @RequestHeader(name = "X-Actor-Id", required = false) String actorId,
      @RequestHeader(name = "X-Actor-Roles", required = false) String actorRoles,
      @RequestHeader(name = "X-Request-Path", required = false) String path,
      @RequestHeader(name = "X-Request-Method", required = false) String method) {
    Order order = repository.findById(id).orElseThrow(() -> new OrderNotFoundException(id));
    auditClient.emit(auditClient.request(
        "Order",
        String.valueOf(id),
        "READ",
        actorId,
        actorRoles,
        path,
        method,
        null,
        order));
    return order;
  }

  @PostMapping
  @ResponseStatus(HttpStatus.CREATED)
  public Order create(
      @Valid @RequestBody Order order,
      @RequestHeader(name = "X-Actor-Id", required = false) String actorId,
      @RequestHeader(name = "X-Actor-Roles", required = false) String actorRoles,
      @RequestHeader(name = "X-Request-Path", required = false) String path,
      @RequestHeader(name = "X-Request-Method", required = false) String method) {
    order.setTotal(computeTotal(order.getItemIds()));
    applyStatus(order, OrderStatus.PENDING);
    Order saved = repository.save(order);
    auditClient.emit(auditClient.request(
        "Order",
        String.valueOf(saved.getId()),
        "CREATE",
        actorId,
        actorRoles,
        path,
        method,
        null,
        saved));
    return saved;
  }

  @PutMapping("/{id}")
  public Order update(
      @PathVariable Long id,
      @RequestBody OrderUpdateRequest request,
      @RequestHeader(name = "X-Actor-Id", required = false) String actorId,
      @RequestHeader(name = "X-Actor-Roles", required = false) String actorRoles,
      @RequestHeader(name = "X-Request-Path", required = false) String path,
      @RequestHeader(name = "X-Request-Method", required = false) String method) {
    Order existing = repository.findById(id).orElseThrow(() -> new OrderNotFoundException(id));
    Order before = snapshot(existing);
    if (request.userId() != null) {
      existing.setUserId(request.userId());
    }
    if (request.itemIds() != null) {
      existing.setItemIds(request.itemIds());
      existing.setTotal(computeTotal(request.itemIds()));
    }
    if (request.status() != null) {
      applyStatus(existing, request.status());
    }
    Order saved = repository.save(existing);
    auditClient.emit(auditClient.request(
        "Order",
        String.valueOf(id),
        "UPDATE",
        actorId,
        actorRoles,
        path,
        method,
        before,
        saved));
    return saved;
  }

  @PatchMapping("/{id}/status")
  public Order updateStatus(
      @PathVariable Long id,
      @Valid @RequestBody StatusUpdateRequest request,
      @RequestHeader(name = "X-Actor-Id", required = false) String actorId,
      @RequestHeader(name = "X-Actor-Roles", required = false) String actorRoles,
      @RequestHeader(name = "X-Request-Path", required = false) String path,
      @RequestHeader(name = "X-Request-Method", required = false) String method) {
    Order existing = repository.findById(id).orElseThrow(() -> new OrderNotFoundException(id));
    Order before = snapshot(existing);
    applyStatus(existing, request.status());
    Order saved = repository.save(existing);
    auditClient.emit(auditClient.request(
        "Order",
        String.valueOf(id),
        "UPDATE_STATUS",
        actorId,
        actorRoles,
        path,
        method,
        before,
        saved));
    return saved;
  }

  private BigDecimal computeTotal(List<String> itemIds) {
    if (itemIds == null || itemIds.isEmpty()) {
      return BigDecimal.ZERO;
    }
    return itemIds.stream()
        .map(catalogClient::getItem)
        .map(CatalogItem::price)
        .reduce(BigDecimal.ZERO, BigDecimal::add);
  }

  private void applyStatus(Order order, OrderStatus status) {
    validateTransition(order.getStatus(), status);
    order.setStatus(status);
    Instant now = Instant.now();
    switch (status) {
      case SHIPPED -> {
        if (order.getShippedAt() == null) {
          order.setShippedAt(now);
        }
      }
      case DELIVERED -> {
        if (order.getDeliveredAt() == null) {
          order.setDeliveredAt(now);
        }
      }
      case CANCELLED -> {
        if (order.getCancelledAt() == null) {
          order.setCancelledAt(now);
        }
      }
      default -> {
        // no-op for PENDING/PROCESSING
      }
    }
  }

  private void validateTransition(OrderStatus current, OrderStatus next) {
    if (current == null || current == next) {
      return;
    }
    switch (current) {
      case PENDING -> {
        if (next != OrderStatus.PROCESSING && next != OrderStatus.CANCELLED) {
          throw new OrderStatusTransitionException(current, next);
        }
      }
      case PROCESSING -> {
        if (next != OrderStatus.SHIPPED && next != OrderStatus.CANCELLED) {
          throw new OrderStatusTransitionException(current, next);
        }
      }
      case SHIPPED -> {
        if (next != OrderStatus.DELIVERED) {
          throw new OrderStatusTransitionException(current, next);
        }
      }
      case DELIVERED, CANCELLED -> throw new OrderStatusTransitionException(current, next);
      default -> throw new OrderStatusTransitionException(current, next);
    }
  }

  public record OrderUpdateRequest(String userId, List<String> itemIds, OrderStatus status) {}

  public record StatusUpdateRequest(@NotNull OrderStatus status) {}

  private Order snapshot(Order source) {
    Order copy = new Order();
    copy.setId(source.getId());
    copy.setUserId(source.getUserId());
    copy.setItemIds(source.getItemIds() == null ? null : List.copyOf(source.getItemIds()));
    copy.setTotal(source.getTotal());
    copy.setStatus(source.getStatus());
    copy.setCreatedAt(source.getCreatedAt());
    copy.setUpdatedAt(source.getUpdatedAt());
    copy.setShippedAt(source.getShippedAt());
    copy.setDeliveredAt(source.getDeliveredAt());
    copy.setCancelledAt(source.getCancelledAt());
    return copy;
  }
}
