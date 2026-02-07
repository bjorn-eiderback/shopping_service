package com.example.shopping.authorization;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClientRepository;
import org.springframework.security.oauth2.server.authorization.settings.AuthorizationServerSettings;

@SpringBootTest(
    webEnvironment = SpringBootTest.WebEnvironment.MOCK,
    properties = {
      "spring.main.web-application-type=servlet",
      "spring.cloud.gateway.server.webflux.enabled=false",
      "spring.cloud.gateway.server.webmvc.enabled=false",
      "spring.autoconfigure.exclude=org.springframework.boot.jdbc.autoconfigure.DataSourceAutoConfiguration,"
          + "org.springframework.boot.jdbc.autoconfigure.DataSourceTransactionManagerAutoConfiguration,"
          + "org.springframework.boot.jdbc.autoconfigure.DataSourceInitializationAutoConfiguration,"
          + "org.springframework.boot.hibernate.autoconfigure.HibernateJpaAutoConfiguration,"
          + "org.springframework.cloud.gateway.config.GatewayAutoConfiguration,"
          + "org.springframework.cloud.gateway.config.GatewayClassPathWarningAutoConfiguration"
    })
class AuthorizationServiceIntegrationTest {
  @Autowired
  private RegisteredClientRepository registeredClientRepository;

  @Autowired
  private AuthorizationServerSettings authorizationServerSettings;

  @Test
  void contextExposesConfiguredClientsAndIssuer() {
    assertThat(registeredClientRepository.findByClientId("admin-client")).isNotNull();
    assertThat(registeredClientRepository.findByClientId("user-client")).isNotNull();
    assertThat(authorizationServerSettings.getIssuer()).isEqualTo("http://localhost:9000");
  }
}
