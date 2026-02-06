package com.example.shopping.order;

import jakarta.validation.Valid;
import java.math.BigDecimal;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
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
    return repository.findById(id).orElseThrow();
  }

  @PostMapping
  @ResponseStatus(HttpStatus.CREATED)
  public Order create(@Valid @RequestBody Order order) {
    BigDecimal total = order.getItemIds().stream()
        .map(catalogClient::getItem)
        .map(CatalogItem::price)
        .reduce(BigDecimal.ZERO, BigDecimal::add);
    order.setTotal(total);
    order.setStatus("PLACED");
    return repository.save(order);
  }
}
