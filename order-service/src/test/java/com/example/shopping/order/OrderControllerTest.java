package com.example.shopping.order;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class OrderControllerTest {
  @Mock
  private OrderRepository repository;

  @Mock
  private CatalogClient catalogClient;

  @Mock
  private AuditClient auditClient;

  private OrderController controller;

  @BeforeEach
  void setUp() {
    controller = new OrderController(repository, catalogClient, auditClient);
  }

  @Test
  void createComputesTotalAndSetsPendingStatus() {
    Order order = new Order();
    order.setUserId("u1");
    order.setItemIds(List.of("item-1", "item-2"));

    when(catalogClient.getItem("item-1")).thenReturn(new CatalogItem("item-1", "A", new BigDecimal("10.00"), "ACTIVE"));
    when(catalogClient.getItem("item-2")).thenReturn(new CatalogItem("item-2", "B", new BigDecimal("2.50"), "ACTIVE"));
    when(repository.save(order)).thenAnswer(invocation -> {
      Order saved = invocation.getArgument(0);
      saved.setId(9L);
      return saved;
    });
    when(auditClient.request(eq("Order"), eq("9"), eq("CREATE"), any(), any(), any(), any(), eq(null), any()))
        .thenReturn(new AuditClient.AuditEventRequest(
            "order-service", "Order", "9", "CREATE", null, null, null, null, null, null, null));

    Order saved = controller.create(order, null, null, null, null);

    assertThat(saved.getTotal()).isEqualByComparingTo("12.50");
    assertThat(saved.getStatus()).isEqualTo(OrderStatus.PENDING);
  }

  @Test
  void updateStatusRejectsInvalidTransition() {
    Order existing = new Order();
    existing.setId(5L);
    existing.setStatus(OrderStatus.SHIPPED);

    when(repository.findById(5L)).thenReturn(Optional.of(existing));

    assertThatThrownBy(() -> controller.updateStatus(
        5L,
        new OrderController.StatusUpdateRequest(OrderStatus.PENDING),
        null,
        null,
        null,
        null))
        .isInstanceOf(OrderStatusTransitionException.class);
  }
}
