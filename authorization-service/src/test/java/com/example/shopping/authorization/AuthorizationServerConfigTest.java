package com.example.shopping.authorization;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClient;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClientRepository;

class AuthorizationServerConfigTest {
  private final AuthorizationServerConfig config = new AuthorizationServerConfig();
  private final PasswordEncoder testEncoder = new PasswordEncoder() {
    @Override
    public String encode(CharSequence rawPassword) {
      return rawPassword.toString();
    }

    @Override
    public boolean matches(CharSequence rawPassword, String encodedPassword) {
      return rawPassword.toString().contentEquals(encodedPassword);
    }
  };

  @Test
  void registersAdminAndUserClients() {
    RegisteredClientRepository repository = config.registeredClientRepository(testEncoder);

    RegisteredClient admin = repository.findByClientId("admin-client");
    RegisteredClient user = repository.findByClientId("user-client");

    assertThat(admin).isNotNull();
    assertThat(admin.getScopes()).contains("apiAdmin", "apiUser");
    assertThat(user).isNotNull();
    assertThat(user.getScopes()).containsExactly("apiUser");
  }
}
