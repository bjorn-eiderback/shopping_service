package com.example.shopping.catalog;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import java.time.Instant;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/catalog")
public class ItemController {
  private final ItemRepository repository;

  public ItemController(ItemRepository repository) {
    this.repository = repository;
  }

  @GetMapping
  public List<Item> list() {
    return repository.findAll();
  }

  @GetMapping("/{id}")
  public Item get(@PathVariable String id) {
    return repository.findById(id).orElseThrow();
  }

  @PostMapping
  @ResponseStatus(HttpStatus.CREATED)
  public Item create(@Valid @RequestBody Item item) {
    item.setLastUpdated(Instant.now());
    return repository.save(item);
  }

  @PutMapping("/{id}")
  public Item update(@PathVariable String id, @Valid @RequestBody Item item) {
    item.setId(id);
    item.setLastUpdated(Instant.now());
    return repository.save(item);
  }

  @PatchMapping("/{id}/stock")
  public Item updateStock(@PathVariable String id, @Valid @RequestBody StockUpdateRequest request) {
    Item existing = repository.findById(id).orElseThrow();
    existing.setStockQuantity(request.stockQuantity());
    existing.setLastUpdated(Instant.now());
    return repository.save(existing);
  }

  public record StockUpdateRequest(@Min(0) int stockQuantity) {}
}
