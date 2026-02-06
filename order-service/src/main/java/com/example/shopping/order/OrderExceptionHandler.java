package com.example.shopping.order;

import java.time.Instant;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;

@RestControllerAdvice
public class OrderExceptionHandler {
  @ExceptionHandler(OrderNotFoundException.class)
  public ResponseEntity<ErrorResponse> handleNotFound(OrderNotFoundException ex) {
    return ResponseEntity.status(HttpStatus.NOT_FOUND)
        .body(new ErrorResponse("ORDER_NOT_FOUND", ex.getMessage()));
  }

  @ExceptionHandler(OrderStatusTransitionException.class)
  public ResponseEntity<ErrorResponse> handleInvalidTransition(OrderStatusTransitionException ex) {
    return ResponseEntity.status(HttpStatus.CONFLICT)
        .body(new ErrorResponse("ORDER_INVALID_STATUS_TRANSITION", ex.getMessage()));
  }

  @ExceptionHandler({MethodArgumentNotValidException.class, HttpMessageNotReadableException.class})
  public ResponseEntity<ErrorResponse> handleBadRequest(Exception ex) {
    return ResponseEntity.status(HttpStatus.BAD_REQUEST)
        .body(new ErrorResponse("ORDER_BAD_REQUEST", ex.getMessage()));
  }

  public record ErrorResponse(String code, String message, Instant timestamp) {
    public ErrorResponse(String code, String message) {
      this(code, message, Instant.now());
    }
  }
}
