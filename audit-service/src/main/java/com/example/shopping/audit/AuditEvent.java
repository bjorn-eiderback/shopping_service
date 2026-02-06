package com.example.shopping.audit;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Lob;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import java.time.Instant;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "audit_events")
@Getter
@Setter
@NoArgsConstructor
public class AuditEvent {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(nullable = false)
  private String service;

  @Column(nullable = false)
  private String entityType;

  private String entityId;

  @Column(nullable = false)
  private String action;

  private String actorId;

  private String actorRoles;

  private String path;

  private String method;

  @Lob
  private String beforeJson;

  @Lob
  private String afterJson;

  @Column(nullable = false)
  private Instant timestamp;

  @PrePersist
  void onCreate() {
    if (timestamp == null) {
      timestamp = Instant.now();
    }
  }
}
