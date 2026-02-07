package com.example.shopping.gateway;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;
import org.springframework.test.web.reactive.server.WebTestClient;

@SpringBootTest(
    webEnvironment = SpringBootTest.WebEnvironment.MOCK,
    properties = {
      "spring.main.web-application-type=reactive",
      "spring.autoconfigure.exclude=org.springframework.boot.webmvc.autoconfigure.WebMvcAutoConfiguration,"
          + "org.springframework.boot.security.autoconfigure.servlet.SecurityAutoConfiguration,"
          + "org.springframework.boot.security.autoconfigure.servlet.SecurityFilterAutoConfiguration,"
          + "org.springframework.boot.jdbc.autoconfigure.DataSourceAutoConfiguration,"
          + "org.springframework.boot.jdbc.autoconfigure.DataSourceTransactionManagerAutoConfiguration,"
          + "org.springframework.boot.jdbc.autoconfigure.DataSourceInitializationAutoConfiguration,"
          + "org.springframework.boot.hibernate.autoconfigure.HibernateJpaAutoConfiguration",
      "management.health.defaults.enabled=false",
      "auth.provider=none",
      "auth.dev-basic.enabled=true",
      "spring.cloud.gateway.routes[0].id=noop",
      "spring.cloud.gateway.routes[0].uri=http://localhost:65535",
      "spring.cloud.gateway.routes[0].predicates[0]=Path=/api/**"
    })
class GatewaySecurityIntegrationTest {
  @Autowired
  private ApplicationContext applicationContext;

  private WebTestClient webTestClient() {
    return WebTestClient.bindToApplicationContext(applicationContext).build();
  }

  @Test
  void healthEndpointIsPublic() {
    webTestClient().get()
        .uri("/actuator/health")
        .exchange()
        .expectStatus()
        .isOk();
  }

  @Test
  void apiEndpointRequiresAuthentication() {
    webTestClient().get()
        .uri("/api/orders")
        .exchange()
        .expectStatus()
        .isUnauthorized();
  }
}
