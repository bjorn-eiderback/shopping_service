package com.example.shopping.catalog;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.math.BigDecimal;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(ItemController.class)
class ItemControllerIntegrationTest {
  @Autowired
  private MockMvc mockMvc;

  @MockitoBean
  private ItemRepository repository;

  @MockitoBean
  private AuditClient auditClient;

  @Test
  void updateStockEndpointUpdatesAndReturnsItem() throws Exception {
    Item item = new Item();
    item.setId("item-1");
    item.setName("Widget");
    item.setPrice(BigDecimal.TEN);
    item.setStatus("ACTIVE");
    item.setStockQuantity(2);

    org.mockito.Mockito.when(repository.findById("item-1")).thenReturn(Optional.of(item));
    org.mockito.Mockito.when(repository.save(any(Item.class))).thenAnswer(invocation -> invocation.getArgument(0));
    org.mockito.Mockito.when(auditClient.request(
            eq("Item"), eq("item-1"), eq("UPDATE_STOCK"), any(), any(), any(), any(), any(), any()))
        .thenReturn(new AuditClient.AuditEventRequest(
            "catalog-service", "Item", "item-1", "UPDATE_STOCK", null, null, null, null, null, null, null));

    mockMvc.perform(patch("/api/catalog/item-1/stock")
            .contentType(MediaType.APPLICATION_JSON)
            .content("{\"stockQuantity\":12}"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.id").value("item-1"))
        .andExpect(jsonPath("$.stockQuantity").value(12));
  }
}
