package com.example.shopping.gateway;

import java.util.List;
import java.util.Locale;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.reactive.EnableWebFluxSecurity;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.core.userdetails.MapReactiveUserDetailsService;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.factory.PasswordEncoderFactories;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.core.DelegatingOAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2Error;
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

  private final String provider;
  private final boolean authEnabled;
  private final boolean devBasicEnabled;
  private final String devAdminUsername;
  private final String devAdminPassword;
  private final String devUserUsername;
  private final String devUserPassword;
  private final String auth0IssuerUri;
  private final String auth0Audience;
  private final String localIssuerUri;
  private final String localAudience;
  private final String rolesClaim;
  private final String rolePrefix;

  public SecurityConfig(
      @Value("${auth.provider:none}") String provider,
      @Value("${auth.enabled:false}") boolean authEnabled,
      @Value("${auth.dev-basic.enabled:false}") boolean devBasicEnabled,
      @Value("${auth.dev-basic.admin-username:adminAPI}") String devAdminUsername,
      @Value("${auth.dev-basic.admin-password:admin}") String devAdminPassword,
      @Value("${auth.dev-basic.user-username:userAPI}") String devUserUsername,
      @Value("${auth.dev-basic.user-password:user}") String devUserPassword,
      @Value("${auth.auth0.issuer-uri:}") String auth0IssuerUri,
      @Value("${auth.auth0.audience:}") String auth0Audience,
      @Value("${auth.local.issuer-uri:http://localhost:9000}") String localIssuerUri,
      @Value("${auth.local.audience:shopping-api}") String localAudience,
      @Value("${auth.roles-claim:https://example.com/roles}") String rolesClaim,
      @Value("${auth.role-prefix:ROLE_}") String rolePrefix) {
    this.provider = provider;
    this.authEnabled = authEnabled;
    this.devBasicEnabled = devBasicEnabled;
    this.devAdminUsername = devAdminUsername;
    this.devAdminPassword = devAdminPassword;
    this.devUserUsername = devUserUsername;
    this.devUserPassword = devUserPassword;
    this.auth0IssuerUri = auth0IssuerUri;
    this.auth0Audience = auth0Audience;
    this.localIssuerUri = localIssuerUri;
    this.localAudience = localAudience;
    this.rolesClaim = rolesClaim;
    this.rolePrefix = rolePrefix;
  }

  @Bean
  public SecurityWebFilterChain securityWebFilterChain(ServerHttpSecurity http) {
    String mode = effectiveProvider();

    if ("none".equals(mode)) {
      if (devBasicEnabled) {
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
            .httpBasic(spec -> {})
            .build();
      }

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
            .jwt(jwt -> jwt
                .jwtDecoder(jwtDecoderForMode(mode))
                .jwtAuthenticationConverter(jwtAuthenticationConverter())))
        .build();
  }

  @Bean
  @ConditionalOnProperty(name = "auth.dev-basic.enabled", havingValue = "true")
  public MapReactiveUserDetailsService userDetailsService(PasswordEncoder passwordEncoder) {
    UserDetails admin = User.withUsername(devAdminUsername)
        .password(passwordEncoder.encode(devAdminPassword))
        .roles("apiAdmin")
        .build();
    UserDetails user = User.withUsername(devUserUsername)
        .password(passwordEncoder.encode(devUserPassword))
        .roles("apiUser")
        .build();
    return new MapReactiveUserDetailsService(admin, user);
  }

  @Bean
  @ConditionalOnProperty(name = "auth.dev-basic.enabled", havingValue = "true")
  public PasswordEncoder passwordEncoder() {
    return PasswordEncoderFactories.createDelegatingPasswordEncoder();
  }

  private String effectiveProvider() {
    if (StringUtils.hasText(provider)) {
      String normalized = provider.toLowerCase(Locale.ROOT);
      if ("auth0".equals(normalized) || "local".equals(normalized) || "none".equals(normalized)) {
        return normalized;
      }
    }
    return authEnabled ? "auth0" : "none";
  }

  private ReactiveJwtDecoder jwtDecoderForMode(String mode) {
    String issuer = "local".equals(mode) ? localIssuerUri : auth0IssuerUri;
    String audience = "local".equals(mode) ? localAudience : auth0Audience;

    if (!StringUtils.hasText(issuer)) {
      throw new IllegalStateException("Issuer URI must be configured for auth provider: " + mode);
    }

    NimbusReactiveJwtDecoder decoder =
        (NimbusReactiveJwtDecoder) ReactiveJwtDecoders.fromIssuerLocation(issuer);

    OAuth2TokenValidator<Jwt> issuerValidator = JwtValidators.createDefaultWithIssuer(issuer);
    if (StringUtils.hasText(audience)) {
      decoder.setJwtValidator(new DelegatingOAuth2TokenValidator<>(issuerValidator, audienceValidator(audience)));
    } else {
      decoder.setJwtValidator(issuerValidator);
    }
    return decoder;
  }

  private OAuth2TokenValidator<Jwt> audienceValidator(String audience) {
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
