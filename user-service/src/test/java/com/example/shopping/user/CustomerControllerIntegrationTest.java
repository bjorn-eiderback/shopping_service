package com.example.shopping.user;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(CustomerController.class)
class CustomerControllerIntegrationTest {
  @Autowired
  private MockMvc mockMvc;

  @MockitoBean
  private CustomerRepository repository;

  @MockitoBean
  private AuditClient auditClient;

  @Test
  void getCustomerReturnsPayload() throws Exception {
    Customer customer = new Customer();
    customer.setId(10L);
    customer.setName("Alice");
    customer.setEmail("alice@example.com");
    customer.setActive(true);

    org.mockito.Mockito.when(repository.findById(10L)).thenReturn(Optional.of(customer));
    org.mockito.Mockito.when(auditClient.request(
            eq("Customer"), eq("10"), eq("READ"), any(), any(), any(), any(), any(), any()))
        .thenReturn(new AuditClient.AuditEventRequest(
            "user-service", "Customer", "10", "READ", null, null, null, null, null, null, null));

    mockMvc.perform(get("/api/users/10"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.id").value(10))
        .andExpect(jsonPath("$.name").value("Alice"))
        .andExpect(jsonPath("$.email").value("alice@example.com"));
  }
}
