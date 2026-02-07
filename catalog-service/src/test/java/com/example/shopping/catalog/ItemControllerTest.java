package com.example.shopping.catalog;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ItemControllerTest {
  @Mock
  private ItemRepository repository;

  @Mock
  private AuditClient auditClient;

  private ItemController controller;

  @BeforeEach
  void setUp() {
    controller = new ItemController(repository, auditClient);
  }

  @Test
  void updateStockUpdatesQuantityAndTimestamp() {
    Item existing = new Item();
    existing.setId("i1");
    existing.setName("Widget");
    existing.setPrice(BigDecimal.TEN);
    existing.setStatus("ACTIVE");
    existing.setStockQuantity(4);

    when(repository.findById("i1")).thenReturn(Optional.of(existing));
    when(repository.save(existing)).thenReturn(existing);
    when(auditClient.request(eq("Item"), eq("i1"), eq("UPDATE_STOCK"), any(), any(), any(), any(), any(), eq(existing)))
        .thenReturn(new AuditClient.AuditEventRequest(
            "catalog-service", "Item", "i1", "UPDATE_STOCK", null, null, null, null, null, null, null));

    Item result = controller.updateStock("i1", new ItemController.StockUpdateRequest(15), null, null, null, null);

    assertThat(result.getStockQuantity()).isEqualTo(15);
    assertThat(result.getLastUpdated()).isNotNull();
    verify(repository).save(existing);
  }
}
