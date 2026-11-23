/*
 * Copyright © 2026 anyilanxin zxh(anyilanxin@aliyun.com)
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */
package com.anyilanxin.kunpeng.gateway.webapp.security;

import com.anyilanxin.kunpeng.configuration.gateway.GatewayWebappCfg;
import com.anyilanxin.kunpeng.gateway.webapp.ConditionalOnWebappEnabled;
import java.nio.charset.StandardCharsets;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.ReactiveAuthenticationManager;
import org.springframework.security.authentication.UserDetailsRepositoryReactiveAuthenticationManager;
import org.springframework.security.config.annotation.web.reactive.EnableWebFluxSecurity;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.core.userdetails.ReactiveUserDetailsService;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.factory.PasswordEncoderFactories;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.server.SecurityWebFilterChain;
import org.springframework.security.web.server.context.WebSessionServerSecurityContextRepository;
import org.springframework.security.web.server.util.matcher.ServerWebExchangeMatchers;
import reactor.core.publisher.Mono;

/**
 * Web 控制台安全链：只作用于 {@code /webapp/**} 与 {@code /api/v1/webapp/**}，登录走 JSON 接口 + session cookie；REST
 * 业务 API 与其他端点均不在链内（现行为零变化）。
 *
 * <p>密码编码用 Spring 标准委托编码器（{noop}/{bcrypt} 前缀自描述）；未配置用户时用户服务查无账号，即不可登录。
 *
 * @author zxuanhong
 * @since 2026.9.0
 */
@Configuration(proxyBeanMethods = false)
@EnableWebFluxSecurity
@ConditionalOnWebappEnabled
public class WebappSecurityConfiguration {

  /** 控制台安全链：登录接口与静态壳放行，数据接口认证；未认证 API 请求回 401（SPA 据此转登录页） */
  @Bean
  SecurityWebFilterChain webappSecurityFilterChain(final ServerHttpSecurity http) {
    return http.securityMatcher(
            ServerWebExchangeMatchers.pathMatchers("/webapp/**", "/api/v1/webapp/**"))
        .authorizeExchange(
            exchanges ->
                exchanges
                    .pathMatchers("/api/v1/webapp/login", "/api/v1/webapp/me")
                    .permitAll()
                    .pathMatchers("/webapp/**")
                    .permitAll()
                    .anyExchange()
                    .authenticated())
        .securityContextRepository(new WebSessionServerSecurityContextRepository())
        .exceptionHandling(
            handling ->
                handling.authenticationEntryPoint(
                    (exchange, ex) -> unauthorized(exchange.getResponse())))
        .httpBasic(ServerHttpSecurity.HttpBasicSpec::disable)
        .formLogin(ServerHttpSecurity.FormLoginSpec::disable)
        .logout(ServerHttpSecurity.LogoutSpec::disable)
        .csrf(ServerHttpSecurity.CsrfSpec::disable)
        .build();
  }

  private static Mono<Void> unauthorized(
      final org.springframework.http.server.reactive.ServerHttpResponse response) {
    response.setStatusCode(HttpStatus.UNAUTHORIZED);
    response.getHeaders().setContentType(MediaType.APPLICATION_JSON);
    final byte[] body =
        "{\"code\":401,\"success\":false,\"message\":\"unauthorized\",\"data\":null,\"timestamp\":0}"
            .getBytes(StandardCharsets.UTF_8);
    return response.writeWith(Mono.just(response.bufferFactory().wrap(body)));
  }

  /** 用户明细服务：配置文件用户清单直读，缺号即抛（不可登录） */
  @Bean
  ReactiveUserDetailsService webappUserDetailsService(final GatewayWebappCfg properties) {
    return username ->
        Mono.justOrEmpty(
                properties.getUsers().stream()
                    .filter(user -> user.getUsername().equals(username))
                    .findFirst())
            .map(
                user ->
                    (UserDetails)
                        User.withUsername(user.getUsername())
                            .password(user.getPassword())
                            .roles(user.getRoles().toArray(String[]::new))
                            .build())
            .switchIfEmpty(Mono.error(new UsernameNotFoundException(username)));
  }

  /** 密码编码器：委托编码器，配置文件密码 {noop}/{bcrypt} 前缀自描述 */
  @Bean
  PasswordEncoder webappPasswordEncoder() {
    return PasswordEncoderFactories.createDelegatingPasswordEncoder();
  }

  /** 认证管理器：登录控制器与安全链共用 */
  @Bean
  ReactiveAuthenticationManager webappAuthenticationManager(
      final ReactiveUserDetailsService userDetailsService, final PasswordEncoder passwordEncoder) {
    final UserDetailsRepositoryReactiveAuthenticationManager manager =
        new UserDetailsRepositoryReactiveAuthenticationManager(userDetailsService);
    manager.setPasswordEncoder(passwordEncoder);
    return manager;
  }
}
