package com.example.shopping.audit;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import java.time.Instant;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/audit")
public class AuditController {
  private final AuditEventRepository repository;

  public AuditController(AuditEventRepository repository) {
    this.repository = repository;
  }

  @PostMapping("/events")
  @ResponseStatus(HttpStatus.CREATED)
  public AuditEvent create(@Valid @RequestBody AuditEventRequest request) {
    AuditEvent event = new AuditEvent();
    event.setService(request.service());
    event.setEntityType(request.entityType());
    event.setEntityId(request.entityId());
    event.setAction(request.action());
    event.setActorId(request.actorId());
    event.setActorRoles(request.actorRoles());
    event.setPath(request.path());
    event.setMethod(request.method());
    event.setBeforeJson(request.beforeJson());
    event.setAfterJson(request.afterJson());
    event.setTimestamp(request.timestamp() == null ? Instant.now() : request.timestamp());
    return repository.save(event);
  }

  @GetMapping("/events")
  public List<AuditEvent> list() {
    return repository.findAll();
  }

  public record AuditEventRequest(
      @NotBlank String service,
      @NotBlank String entityType,
      String entityId,
      @NotBlank String action,
      String actorId,
      String actorRoles,
      String path,
      String method,
      String beforeJson,
      String afterJson,
      Instant timestamp
  ) {}
}
