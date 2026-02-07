package com.example.shopping.audit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class AuditControllerTest {
  @Mock
  private AuditEventRepository repository;

  private AuditController controller;

  @BeforeEach
  void setUp() {
    controller = new AuditController(repository);
  }

  @Test
  void createMapsRequestToEntity() {
    Instant now = Instant.now();
    AuditController.AuditEventRequest request = new AuditController.AuditEventRequest(
        "order-service", "Order", "42", "UPDATE", "actor", "apiAdmin", "/api/orders/42", "PUT", "before", "after", now);

    when(repository.save(org.mockito.ArgumentMatchers.any(AuditEvent.class)))
        .thenAnswer(invocation -> invocation.getArgument(0));

    AuditEvent saved = controller.create(request);

    assertThat(saved.getService()).isEqualTo("order-service");
    assertThat(saved.getEntityType()).isEqualTo("Order");
    assertThat(saved.getEntityId()).isEqualTo("42");
    assertThat(saved.getTimestamp()).isEqualTo(now);
  }

  @Test
  void listReturnsRepositoryData() {
    when(repository.findAll()).thenReturn(List.of(new AuditEvent(), new AuditEvent()));

    List<AuditEvent> result = controller.list();

    assertThat(result).hasSize(2);
  }
}
