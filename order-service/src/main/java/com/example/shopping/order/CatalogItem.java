package com.example.shopping.order;

import java.math.BigDecimal;

public record CatalogItem(String id, String name, BigDecimal price, String status) {}
