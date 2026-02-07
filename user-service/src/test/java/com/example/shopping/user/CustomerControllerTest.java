package com.example.shopping.user;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class CustomerControllerTest {
  @Mock
  private CustomerRepository repository;

  @Mock
  private AuditClient auditClient;

  private CustomerController controller;

  @BeforeEach
  void setUp() {
    controller = new CustomerController(repository, auditClient);
  }

  @Test
  void getReturnsCustomerAndEmitsAuditEvent() {
    Customer customer = new Customer();
    customer.setId(7L);
    customer.setName("Alice");

    AuditClient.AuditEventRequest event = new AuditClient.AuditEventRequest(
        "user-service", "Customer", "7", "READ", "actor", "apiUser", "/api/users/7", "GET", null, null, null);

    when(repository.findById(7L)).thenReturn(Optional.of(customer));
    when(auditClient.request(
        eq("Customer"), eq("7"), eq("READ"), eq("actor"), eq("apiUser"),
        eq("/api/users/7"), eq("GET"), eq(null), eq(customer)))
        .thenReturn(event);

    Customer result = controller.get(7L, "actor", "apiUser", "/api/users/7", "GET");

    assertThat(result).isSameAs(customer);
    verify(auditClient).emit(event);
  }

  @Test
  void listReturnsAllCustomers() {
    Customer c1 = new Customer();
    c1.setId(1L);
    Customer c2 = new Customer();
    c2.setId(2L);

    when(repository.findAll()).thenReturn(List.of(c1, c2));
    when(auditClient.request(any(), any(), any(), any(), any(), any(), any(), any(), any()))
        .thenReturn(new AuditClient.AuditEventRequest(
            "user-service", "Customer", null, "READ_LIST", null, null, null, null, null, null, null));

    List<Customer> result = controller.list(null, null, null, null);

    assertThat(result).hasSize(2);
  }
}
