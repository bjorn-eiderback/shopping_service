package com.example.shopping.order;

public class OrderStatusTransitionException extends RuntimeException {
  public OrderStatusTransitionException(OrderStatus current, OrderStatus next) {
    super("Invalid status transition: " + current + " -> " + next);
  }
}
