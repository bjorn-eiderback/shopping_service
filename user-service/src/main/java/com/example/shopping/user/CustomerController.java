package com.example.shopping.user;

import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/users")
public class CustomerController {
  private final CustomerRepository repository;
  private final AuditClient auditClient;

  public CustomerController(CustomerRepository repository, AuditClient auditClient) {
    this.repository = repository;
    this.auditClient = auditClient;
  }

  @GetMapping
  public List<Customer> list(
      @RequestHeader(name = "X-Actor-Id", required = false) String actorId,
      @RequestHeader(name = "X-Actor-Roles", required = false) String actorRoles,
      @RequestHeader(name = "X-Request-Path", required = false) String path,
      @RequestHeader(name = "X-Request-Method", required = false) String method) {
    List<Customer> customers = repository.findAll();
    auditClient.emit(auditClient.request(
        "Customer",
        null,
        "READ_LIST",
        actorId,
        actorRoles,
        path,
        method,
        null,
        customers));
    return customers;
  }

  @GetMapping("/{id}")
  public Customer get(
      @PathVariable Long id,
      @RequestHeader(name = "X-Actor-Id", required = false) String actorId,
      @RequestHeader(name = "X-Actor-Roles", required = false) String actorRoles,
      @RequestHeader(name = "X-Request-Path", required = false) String path,
      @RequestHeader(name = "X-Request-Method", required = false) String method) {
    Customer customer = repository.findById(id).orElseThrow();
    auditClient.emit(auditClient.request(
        "Customer",
        String.valueOf(id),
        "READ",
        actorId,
        actorRoles,
        path,
        method,
        null,
        customer));
    return customer;
  }

  @PostMapping
  @ResponseStatus(HttpStatus.CREATED)
  public Customer create(
      @Valid @RequestBody Customer customer,
      @RequestHeader(name = "X-Actor-Id", required = false) String actorId,
      @RequestHeader(name = "X-Actor-Roles", required = false) String actorRoles,
      @RequestHeader(name = "X-Request-Path", required = false) String path,
      @RequestHeader(name = "X-Request-Method", required = false) String method) {
    Customer saved = repository.save(customer);
    auditClient.emit(auditClient.request(
        "Customer",
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
}
