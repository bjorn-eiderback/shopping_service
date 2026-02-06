package com.example.shopping.gateway;

import java.util.List;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.reactive.EnableWebFluxSecurity;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.DelegatingOAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2TokenValidatorResult;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtValidators;
import org.springframework.security.oauth2.jwt.NimbusReactiveJwtDecoder;
import org.springframework.security.oauth2.jwt.ReactiveJwtDecoders;
import org.springframework.security.oauth2.jwt.ReactiveJwtDecoder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.oauth2.server.resource.authentication.JwtGrantedAuthoritiesConverter;
import org.springframework.security.oauth2.server.resource.authentication.ReactiveJwtAuthenticationConverterAdapter;
import org.springframework.security.web.server.SecurityWebFilterChain;
import org.springframework.security.web.server.util.matcher.ServerWebExchangeMatchers;
import org.springframework.util.StringUtils;

@Configuration
@EnableWebFluxSecurity
public class SecurityConfig {
  private static final String ROLE_ADMIN = "ROLE_apiAdmin";
  private static final String ROLE_USER = "ROLE_apiUser";

  private final boolean authEnabled;
  private final String issuerUri;
  private final String audience;
  private final String rolesClaim;
  private final String rolePrefix;

  public SecurityConfig(
      @Value("${auth.enabled:false}") boolean authEnabled,
      @Value("${spring.security.oauth2.resourceserver.jwt.issuer-uri:}") String issuerUri,
      @Value("${auth.audience:}") String audience,
      @Value("${auth.roles-claim:https://example.com/roles}") String rolesClaim,
      @Value("${auth.role-prefix:ROLE_}") String rolePrefix) {
    this.authEnabled = authEnabled;
    this.issuerUri = issuerUri;
    this.audience = audience;
    this.rolesClaim = rolesClaim;
    this.rolePrefix = rolePrefix;
  }

  @Bean
  public SecurityWebFilterChain securityWebFilterChain(ServerHttpSecurity http) {
    if (!authEnabled) {
      return http.csrf(ServerHttpSecurity.CsrfSpec::disable)
          .authorizeExchange(exchanges -> exchanges.anyExchange().permitAll())
          .build();
    }

    return http.csrf(ServerHttpSecurity.CsrfSpec::disable)
        .authorizeExchange(exchanges -> exchanges
            .matchers(ServerWebExchangeMatchers.pathMatchers("/actuator/health", "/actuator/info"))
            .permitAll()
            .matchers(ServerWebExchangeMatchers.pathMatchers(
                "/swagger-ui/**",
                "/v3/api-docs/**",
                "/v3/api-docs.yaml"))
            .permitAll()
            .matchers(ServerWebExchangeMatchers.pathMatchers(HttpMethod.GET, "/api/**"))
            .hasAnyAuthority(ROLE_USER, ROLE_ADMIN)
            .matchers(ServerWebExchangeMatchers.pathMatchers(HttpMethod.POST, "/api/**"))
            .hasAuthority(ROLE_ADMIN)
            .matchers(ServerWebExchangeMatchers.pathMatchers(HttpMethod.PUT, "/api/**"))
            .hasAuthority(ROLE_ADMIN)
            .matchers(ServerWebExchangeMatchers.pathMatchers(HttpMethod.PATCH, "/api/**"))
            .hasAuthority(ROLE_ADMIN)
            .matchers(ServerWebExchangeMatchers.pathMatchers(HttpMethod.DELETE, "/api/**"))
            .hasAuthority(ROLE_ADMIN)
            .anyExchange().authenticated())
        .oauth2ResourceServer(oauth2 -> oauth2
            .jwt(jwt -> jwt.jwtAuthenticationConverter(jwtAuthenticationConverter())))
        .build();
  }

  @Bean
  public ReactiveJwtDecoder jwtDecoder() {
    if (!StringUtils.hasText(issuerUri)) {
      throw new IllegalStateException("AUTH0_ISSUER_URI must be set when auth.enabled=true");
    }
    NimbusReactiveJwtDecoder decoder =
        (NimbusReactiveJwtDecoder) ReactiveJwtDecoders.fromIssuerLocation(issuerUri);

    if (StringUtils.hasText(audience)) {
      OAuth2TokenValidator<Jwt> validator = JwtValidators.createDefaultWithIssuer(issuerUri);
      decoder.setJwtValidator(new DelegatingOAuth2TokenValidator<>(validator, audienceValidator()));
    }
    return decoder;
  }

  private OAuth2TokenValidator<Jwt> audienceValidator() {
    return token -> {
      List<String> audiences = token.getAudience();
      if (audiences != null && audiences.contains(audience)) {
        return OAuth2TokenValidatorResult.success();
      }
      OAuth2Error error = new OAuth2Error("invalid_token", "Missing required audience", null);
      return OAuth2TokenValidatorResult.failure(error);
    };
  }

  private ReactiveJwtAuthenticationConverterAdapter jwtAuthenticationConverter() {
    JwtGrantedAuthoritiesConverter authoritiesConverter = new JwtGrantedAuthoritiesConverter();
    authoritiesConverter.setAuthoritiesClaimName(rolesClaim);
    authoritiesConverter.setAuthorityPrefix(rolePrefix);

    JwtAuthenticationConverter converter = new JwtAuthenticationConverter();
    converter.setJwtGrantedAuthoritiesConverter(authoritiesConverter);
    return new ReactiveJwtAuthenticationConverterAdapter(converter);
  }
}
