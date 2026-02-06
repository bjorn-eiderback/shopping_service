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
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/catalog")
public class ItemController {
  private final ItemRepository repository;
  private final AuditClient auditClient;

  public ItemController(ItemRepository repository, AuditClient auditClient) {
    this.repository = repository;
    this.auditClient = auditClient;
  }

  @GetMapping
  public List<Item> list(
      @RequestHeader(name = "X-Actor-Id", required = false) String actorId,
      @RequestHeader(name = "X-Actor-Roles", required = false) String actorRoles,
      @RequestHeader(name = "X-Request-Path", required = false) String path,
      @RequestHeader(name = "X-Request-Method", required = false) String method) {
    List<Item> items = repository.findAll();
    auditClient.emit(auditClient.request(
        "Item",
        null,
        "READ_LIST",
        actorId,
        actorRoles,
        path,
        method,
        null,
        items));
    return items;
  }

  @GetMapping("/{id}")
  public Item get(
      @PathVariable String id,
      @RequestHeader(name = "X-Actor-Id", required = false) String actorId,
      @RequestHeader(name = "X-Actor-Roles", required = false) String actorRoles,
      @RequestHeader(name = "X-Request-Path", required = false) String path,
      @RequestHeader(name = "X-Request-Method", required = false) String method) {
    Item item = repository.findById(id).orElseThrow();
    auditClient.emit(auditClient.request(
        "Item",
        id,
        "READ",
        actorId,
        actorRoles,
        path,
        method,
        null,
        item));
    return item;
  }

  @PostMapping
  @ResponseStatus(HttpStatus.CREATED)
  public Item create(
      @Valid @RequestBody Item item,
      @RequestHeader(name = "X-Actor-Id", required = false) String actorId,
      @RequestHeader(name = "X-Actor-Roles", required = false) String actorRoles,
      @RequestHeader(name = "X-Request-Path", required = false) String path,
      @RequestHeader(name = "X-Request-Method", required = false) String method) {
    item.setLastUpdated(Instant.now());
    Item saved = repository.save(item);
    auditClient.emit(auditClient.request(
        "Item",
        saved.getId(),
        "CREATE",
        actorId,
        actorRoles,
        path,
        method,
        null,
        saved));
    return saved;
  }

  @PutMapping("/{id}")
  public Item update(
      @PathVariable String id,
      @Valid @RequestBody Item item,
      @RequestHeader(name = "X-Actor-Id", required = false) String actorId,
      @RequestHeader(name = "X-Actor-Roles", required = false) String actorRoles,
      @RequestHeader(name = "X-Request-Path", required = false) String path,
      @RequestHeader(name = "X-Request-Method", required = false) String method) {
    Item before = repository.findById(id).orElseThrow();
    item.setId(id);
    item.setLastUpdated(Instant.now());
    Item saved = repository.save(item);
    auditClient.emit(auditClient.request(
        "Item",
        id,
        "UPDATE",
        actorId,
        actorRoles,
        path,
        method,
        before,
        saved));
    return saved;
  }

  @PatchMapping("/{id}/stock")
  public Item updateStock(
      @PathVariable String id,
      @Valid @RequestBody StockUpdateRequest request,
      @RequestHeader(name = "X-Actor-Id", required = false) String actorId,
      @RequestHeader(name = "X-Actor-Roles", required = false) String actorRoles,
      @RequestHeader(name = "X-Request-Path", required = false) String path,
      @RequestHeader(name = "X-Request-Method", required = false) String method) {
    Item existing = repository.findById(id).orElseThrow();
    Item before = new Item();
    before.setId(existing.getId());
    before.setName(existing.getName());
    before.setPrice(existing.getPrice());
    before.setStatus(existing.getStatus());
    before.setStockQuantity(existing.getStockQuantity());
    before.setLastUpdated(existing.getLastUpdated());
    before.setActive(existing.isActive());
    existing.setStockQuantity(request.stockQuantity());
    existing.setLastUpdated(Instant.now());
    Item saved = repository.save(existing);
    auditClient.emit(auditClient.request(
        "Item",
        id,
        "UPDATE_STOCK",
        actorId,
        actorRoles,
        path,
        method,
        before,
        saved));
    return saved;
  }

  public record StockUpdateRequest(@Min(0) int stockQuantity) {}
}
