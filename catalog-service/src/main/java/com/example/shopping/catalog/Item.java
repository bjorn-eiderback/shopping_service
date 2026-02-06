package com.example.shopping.catalog;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import java.time.Instant;
import java.math.BigDecimal;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

@Document("items")
@Getter
@Setter
@NoArgsConstructor
public class Item {
  @Id
  private String id;

  @NotBlank
  private String name;

  @Min(0)
  private BigDecimal price;

  @NotBlank
  private String status;

  @Min(0)
  private int stockQuantity = 0;

  private Instant lastUpdated;

  private boolean active = true;
}
