package com.example.shopping.catalog;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Instant;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

@Service
public class AuditClient {
  private static final Logger log = LoggerFactory.getLogger(AuditClient.class);

  private final RestClient restClient;
  private final ObjectMapper objectMapper;
  private final String serviceName;

  public AuditClient(
      RestClient.Builder restClientBuilder,
      ObjectMapper objectMapper,
      @Value("${spring.application.name}") String serviceName,
      @Value("${spring.audit.base-url}") String baseUrl) {
    this.restClient = restClientBuilder.baseUrl(baseUrl).build();
    this.objectMapper = objectMapper;
    this.serviceName = serviceName;
  }

  public void emit(AuditEventRequest request) {
    try {
      restClient.post()
          .uri("/api/audit/events")
          .body(request)
          .retrieve()
          .toBodilessEntity();
    } catch (Exception ex) {
      log.warn("Failed to emit audit event", ex);
    }
  }

  public String toJson(Object value) {
    if (value == null) {
      return null;
    }
    try {
      return objectMapper.writeValueAsString(value);
    } catch (JsonProcessingException ex) {
      log.warn("Failed to serialize audit payload", ex);
      return null;
    }
  }

  public AuditEventRequest request(String entityType, String entityId, String action,
                                   String actorId, String actorRoles, String path, String method,
                                   Object before, Object after) {
    return new AuditEventRequest(
        serviceName,
        entityType,
        entityId,
        action,
        actorId,
        actorRoles,
        path,
        method,
        toJson(before),
        toJson(after),
        Instant.now());
  }

  public record AuditEventRequest(
      String service,
      String entityType,
      String entityId,
      String action,
      String actorId,
      String actorRoles,
      String path,
      String method,
      String beforeJson,
      String afterJson,
      Instant timestamp
  ) {}
}
