package com.example.shopping.audit;

import static org.mockito.ArgumentMatchers.any;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(AuditController.class)
class AuditControllerIntegrationTest {
  @Autowired
  private MockMvc mockMvc;

  @MockitoBean
  private AuditEventRepository repository;

  @Test
  void createEventReturnsCreatedPayload() throws Exception {
    AuditEvent saved = new AuditEvent();
    saved.setService("order-service");
    saved.setEntityType("Order");
    saved.setEntityId("100");
    saved.setAction("CREATE");
    saved.setTimestamp(Instant.parse("2026-02-01T12:00:00Z"));

    org.mockito.Mockito.when(repository.save(any(AuditEvent.class))).thenReturn(saved);

    mockMvc.perform(post("/api/audit/events")
            .contentType(MediaType.APPLICATION_JSON)
            .content("""
                {
                  "service":"order-service",
                  "entityType":"Order",
                  "entityId":"100",
                  "action":"CREATE"
                }
                """))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.service").value("order-service"))
        .andExpect(jsonPath("$.entityType").value("Order"))
        .andExpect(jsonPath("$.entityId").value("100"));
  }

  @Test
  void listReturnsEvents() throws Exception {
    AuditEvent event = new AuditEvent();
    event.setService("catalog-service");
    event.setEntityType("Item");
    event.setEntityId("item-1");
    event.setAction("READ");
    event.setTimestamp(Instant.parse("2026-02-01T12:00:00Z"));

    org.mockito.Mockito.when(repository.findAll()).thenReturn(List.of(event));

    mockMvc.perform(get("/api/audit/events"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$[0].service").value("catalog-service"))
        .andExpect(jsonPath("$[0].entityType").value("Item"))
        .andExpect(jsonPath("$[0].action").value("READ"));
  }
}
