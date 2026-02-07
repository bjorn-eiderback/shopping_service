package com.example.shopping.order;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest({OrderController.class, OrderExceptionHandler.class})
class OrderControllerIntegrationTest {
  @Autowired
  private MockMvc mockMvc;

  @MockitoBean
  private OrderRepository repository;

  @MockitoBean
  private CatalogClient catalogClient;

  @MockitoBean
  private AuditClient auditClient;

  @Test
  void invalidStatusTransitionReturnsConflict() throws Exception {
    Order order = new Order();
    order.setId(77L);
    order.setStatus(OrderStatus.SHIPPED);

    org.mockito.Mockito.when(repository.findById(77L)).thenReturn(Optional.of(order));

    mockMvc.perform(patch("/api/orders/77/status")
            .contentType(MediaType.APPLICATION_JSON)
            .content("{\"status\":\"PENDING\"}"))
        .andExpect(status().isConflict())
        .andExpect(jsonPath("$.code").value("ORDER_INVALID_STATUS_TRANSITION"));
  }
}
