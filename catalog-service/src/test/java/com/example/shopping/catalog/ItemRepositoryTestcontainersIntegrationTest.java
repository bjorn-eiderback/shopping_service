package com.example.shopping.catalog;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Optional;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.testcontainers.containers.MongoDBContainer;

@SpringBootTest(
    webEnvironment = SpringBootTest.WebEnvironment.NONE,
    properties = {
      "spring.main.web-application-type=none",
      "spring.cloud.gateway.server.webflux.enabled=false",
      "spring.cloud.gateway.server.webmvc.enabled=false",
      "spring.autoconfigure.exclude=org.springframework.cloud.gateway.config.GatewayAutoConfiguration,"
          + "org.springframework.cloud.gateway.config.GatewayClassPathWarningAutoConfiguration,"
          + "org.springframework.boot.jdbc.autoconfigure.DataSourceAutoConfiguration,"
          + "org.springframework.boot.jdbc.autoconfigure.DataSourceTransactionManagerAutoConfiguration,"
          + "org.springframework.boot.jdbc.autoconfigure.DataSourceInitializationAutoConfiguration,"
          + "org.springframework.boot.hibernate.autoconfigure.HibernateJpaAutoConfiguration",
      "management.health.defaults.enabled=false"
    })
@Tag("testcontainers")
class ItemRepositoryTestcontainersIntegrationTest {
  static MongoDBContainer mongo = new MongoDBContainer("mongo:7.0");

  @DynamicPropertySource
  static void configureMongo(DynamicPropertyRegistry registry) {
    if (!mongo.isRunning()) {
      mongo.start();
    }
    registry.add("spring.mongodb.uri", mongo::getConnectionString);
    registry.add("spring.data.mongodb.uri", mongo::getConnectionString);
    registry.add("spring.mongodb.database", () -> "catalog_test");
    registry.add("spring.data.mongodb.database", () -> "catalog_test");
  }

  @Autowired
  private ItemRepository itemRepository;

  @MockitoBean
  private AuditClient auditClient;

  @Test
  void savesAndLoadsItem() {
    Item item = new Item();
    item.setName("Widget");
    item.setPrice(new BigDecimal("9.99"));
    item.setStatus("ACTIVE");
    item.setStockQuantity(7);
    item.setLastUpdated(Instant.now());
    item.setActive(true);

    Item saved = itemRepository.save(item);
    Optional<Item> found = itemRepository.findById(saved.getId());

    assertThat(found).isPresent();
    assertThat(found.get().getName()).isEqualTo("Widget");
    assertThat(found.get().getPrice()).isEqualByComparingTo("9.99");
    assertThat(found.get().getStockQuantity()).isEqualTo(7);
    assertThat(found.get().getStatus()).isEqualTo("ACTIVE");
  }
}
