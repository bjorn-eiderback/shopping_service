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
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/orders")
public class OrderController {
  private final OrderRepository repository;
  private final CatalogClient catalogClient;

  public OrderController(OrderRepository repository, CatalogClient catalogClient) {
    this.repository = repository;
    this.catalogClient = catalogClient;
  }

  @GetMapping
  public List<Order> list() {
    return repository.findAll();
  }

  @GetMapping("/{id}")
  public Order get(@PathVariable Long id) {
    return repository.findById(id).orElseThrow(() -> new OrderNotFoundException(id));
  }

  @PostMapping
  @ResponseStatus(HttpStatus.CREATED)
  public Order create(@Valid @RequestBody Order order) {
    order.setTotal(computeTotal(order.getItemIds()));
    applyStatus(order, OrderStatus.PENDING);
    return repository.save(order);
  }

  @PutMapping("/{id}")
  public Order update(@PathVariable Long id, @RequestBody OrderUpdateRequest request) {
    Order existing = repository.findById(id).orElseThrow(() -> new OrderNotFoundException(id));
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
    return repository.save(existing);
  }

  @PatchMapping("/{id}/status")
  public Order updateStatus(@PathVariable Long id, @Valid @RequestBody StatusUpdateRequest request) {
    Order existing = repository.findById(id).orElseThrow(() -> new OrderNotFoundException(id));
    applyStatus(existing, request.status());
    return repository.save(existing);
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
}
