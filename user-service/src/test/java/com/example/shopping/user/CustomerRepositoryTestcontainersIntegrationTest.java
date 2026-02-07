package com.example.shopping.user;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Optional;
import org.junit.jupiter.api.Tag;
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
      "spring.cloud.gateway.server.webflux.enabled=false",
      "spring.cloud.gateway.server.webmvc.enabled=false",
      "spring.autoconfigure.exclude=org.springframework.cloud.gateway.config.GatewayAutoConfiguration,"
          + "org.springframework.cloud.gateway.config.GatewayClassPathWarningAutoConfiguration",
      "management.health.defaults.enabled=false",
      "spring.jpa.hibernate.ddl-auto=create-drop"
    })
@Testcontainers
@Tag("testcontainers")
class CustomerRepositoryTestcontainersIntegrationTest {
  @Container
  static PostgreSQLContainer<?> postgres =
      new PostgreSQLContainer<>("postgres:16-alpine")
          .withDatabaseName("users_test")
          .withUsername("users")
          .withPassword("users");

  @DynamicPropertySource
  static void configureDatasource(DynamicPropertyRegistry registry) {
    registry.add("spring.datasource.url", postgres::getJdbcUrl);
    registry.add("spring.datasource.username", postgres::getUsername);
    registry.add("spring.datasource.password", postgres::getPassword);
  }

  @Autowired
  private CustomerRepository customerRepository;

  @MockitoBean
  private AuditClient auditClient;

  @Test
  @Transactional
  void savesAndFindsCustomerByEmail() {
    Customer customer = new Customer();
    customer.setName("Alice");
    customer.setEmail("alice@example.com");
    customer.setActive(true);

    Customer saved = customerRepository.save(customer);
    Optional<Customer> found = customerRepository.findByEmail("alice@example.com");

    assertThat(saved.getId()).isNotNull();
    assertThat(found).isPresent();
    assertThat(found.get().getName()).isEqualTo("Alice");
    assertThat(found.get().isActive()).isTrue();
  }
}
