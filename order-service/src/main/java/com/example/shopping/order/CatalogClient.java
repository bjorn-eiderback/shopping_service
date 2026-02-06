package com.example.shopping.order;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@FeignClient(name = "catalog-service")
public interface CatalogClient {
  @GetMapping("/api/catalog/{id}")
  CatalogItem getItem(@PathVariable("id") String id);
}
