package com.example.shopping.order;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Tag;
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.transaction.annotation.Transactional;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

@SpringBootTest(
    webEnvironment = SpringBootTest.WebEnvironment.NONE,
    properties = {
      "spring.main.web-application-type=none",
      "spring.cloud.kubernetes.enabled=false",
      "spring.cloud.discovery.enabled=false",
      "spring.cloud.gateway.server.webflux.enabled=false",
      "spring.cloud.gateway.server.webmvc.enabled=false",
      "spring.autoconfigure.exclude=org.springframework.cloud.gateway.config.GatewayAutoConfiguration,"
          + "org.springframework.cloud.gateway.config.GatewayClassPathWarningAutoConfiguration",
      "management.health.defaults.enabled=false",
      "spring.jpa.hibernate.ddl-auto=create-drop"
    })
@Testcontainers
@Tag("testcontainers")
class OrderRepositoryTestcontainersIntegrationTest {
  @Container
  static PostgreSQLContainer<?> postgres =
      new PostgreSQLContainer<>("postgres:16-alpine")
          .withDatabaseName("orders_test")
          .withUsername("orders")
          .withPassword("orders");

  @DynamicPropertySource
  static void configureDatasource(DynamicPropertyRegistry registry) {
    registry.add("spring.datasource.url", postgres::getJdbcUrl);
    registry.add("spring.datasource.username", postgres::getUsername);
    registry.add("spring.datasource.password", postgres::getPassword);
  }

  @Autowired
  private OrderRepository orderRepository;

  @MockitoBean
  private AuditClient auditClient;

  @MockitoBean
  private CatalogClient catalogClient;

  @Test
  @Transactional
  void savesAndLoadsOrderWithItemsAndStatus() {
    Order order = new Order();
    order.setUserId("user-1");
    order.setItemIds(List.of("item-1", "item-2"));
    order.setTotal(new BigDecimal("15.50"));
    order.setStatus(OrderStatus.PROCESSING);

    Order saved = orderRepository.save(order);
    Optional<Order> reloaded = orderRepository.findById(saved.getId());

    assertThat(reloaded).isPresent();
    assertThat(reloaded.get().getUserId()).isEqualTo("user-1");
    assertThat(reloaded.get().getItemIds()).containsExactly("item-1", "item-2");
    assertThat(reloaded.get().getStatus()).isEqualTo(OrderStatus.PROCESSING);
    assertThat(reloaded.get().getCreatedAt()).isNotNull();
    assertThat(reloaded.get().getUpdatedAt()).isNotNull();
  }
}
