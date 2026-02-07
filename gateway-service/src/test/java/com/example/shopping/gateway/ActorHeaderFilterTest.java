package com.example.shopping.gateway;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;
import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

@ExtendWith(MockitoExtension.class)
class ActorHeaderFilterTest {
  @Mock
  private GatewayFilterChain filterChain;

  @Test
  void addsActorHeadersWhenJwtPrincipalExists() {
    ActorHeaderFilter filter = new ActorHeaderFilter("https://example.com/roles");

    Jwt jwt = Jwt.withTokenValue("token")
        .subject("user-1")
        .issuedAt(Instant.now())
        .expiresAt(Instant.now().plusSeconds(300))
        .header("alg", "none")
        .claim("https://example.com/roles", List.of("apiAdmin"))
        .claim("email", "user@example.com")
        .build();

    TestingAuthenticationToken auth = new TestingAuthenticationToken(jwt, null);
    MockServerWebExchange exchange = MockServerWebExchange.from(MockServerHttpRequest.get("/api/orders").build());
    ServerWebExchange withPrincipal = exchange.mutate().principal(Mono.just(auth)).build();

    when(filterChain.filter(any(ServerWebExchange.class))).thenReturn(Mono.empty());

    filter.filter(withPrincipal, filterChain).block();

    ArgumentCaptor<ServerWebExchange> captor = ArgumentCaptor.forClass(ServerWebExchange.class);
    verify(filterChain).filter(captor.capture());

    ServerHttpRequest request = captor.getValue().getRequest();
    assertThat(request.getHeaders().getFirst("X-Actor-Id")).isEqualTo("user-1");
    assertThat(request.getHeaders().getFirst("X-Actor-Roles")).isEqualTo("apiAdmin");
    assertThat(request.getHeaders().getFirst("X-Actor-Email")).isEqualTo("user@example.com");
    assertThat(request.getHeaders().getFirst("X-Request-Path")).isEqualTo("/api/orders");
    assertThat(request.getHeaders().getFirst("X-Request-Method")).isEqualTo("GET");
  }
}
