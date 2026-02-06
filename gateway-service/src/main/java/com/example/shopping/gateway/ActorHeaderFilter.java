package com.example.shopping.gateway;

import java.util.List;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

@Component
public class ActorHeaderFilter implements GlobalFilter, Ordered {
  private final String rolesClaim;

  public ActorHeaderFilter(@Value("${auth.roles-claim:https://example.com/roles}") String rolesClaim) {
    this.rolesClaim = rolesClaim;
  }

  @Override
  public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
    return exchange.getPrincipal()
        .cast(Authentication.class)
        .flatMap(auth -> chain.filter(mutateExchange(exchange, auth)))
        .switchIfEmpty(Mono.defer(() -> chain.filter(mutateExchange(exchange, null))));
  }

  private ServerWebExchange mutateExchange(ServerWebExchange exchange, Authentication auth) {
    var request = exchange.getRequest();
    var builder = request.mutate();
    builder.header("X-Request-Path", request.getPath().value());
    builder.header("X-Request-Method", request.getMethod().name());

    if (auth != null && auth.getPrincipal() instanceof Jwt jwt) {
      String subject = jwt.getSubject();
      if (subject != null) {
        builder.header("X-Actor-Id", subject);
      }
      List<String> roles = jwt.getClaimAsStringList(rolesClaim);
      if (roles != null && !roles.isEmpty()) {
        builder.header("X-Actor-Roles", String.join(",", roles));
      }
      String email = jwt.getClaimAsString("email");
      if (email != null) {
        builder.header("X-Actor-Email", email);
      }
    }

    return exchange.mutate().request(builder.build()).build();
  }

  @Override
  public int getOrder() {
    return -1;
  }
}
