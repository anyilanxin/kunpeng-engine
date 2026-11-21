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
package com.anyilanxin.kunpeng.gateway.webapp.controller;

import com.anyilanxin.kunpeng.gateway.webapp.core.WebappResult;
import java.util.List;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.ReactiveAuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.ReactiveSecurityContextHolder;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextImpl;
import org.springframework.security.web.server.context.ServerSecurityContextRepository;
import org.springframework.security.web.server.context.WebSessionServerSecurityContextRepository;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

/**
 * 控制台认证接口：JSON 登录（session cookie）、登出（会话失效）、当前用户。
 *
 * @author zxuanhong
 * @since 2026.9.0
 */
@RestController
@RequestMapping(
    value = "/{version}/webapp",
    version = "v1",
    produces = MediaType.APPLICATION_JSON_VALUE)
@ConditionalOnProperty(
    name = "kunpeng.gateway-webapp.enabled",
    havingValue = "true",
    matchIfMissing = true)
public class WebappAuthController {
  private final ReactiveAuthenticationManager authenticationManager;
  private final ServerSecurityContextRepository securityContextRepository =
      new WebSessionServerSecurityContextRepository();

  public WebappAuthController(final ReactiveAuthenticationManager authenticationManager) {
    this.authenticationManager = authenticationManager;
  }

  /** 登录请求体 */
  public record LoginRequest(String username, String password) {}

  /** 当前用户信息 */
  public record MeResponse(String username, List<String> roles) {}

  /** 登录：认证通过后安全上下文写入 session（cookie 会话），认证失败（账号不存在/密码错误）回 401 信封 */
  @PostMapping(value = "/login", consumes = MediaType.APPLICATION_JSON_VALUE)
  public Mono<WebappResult<MeResponse>> login(
      @RequestBody final LoginRequest request, final ServerWebExchange exchange) {
    return authenticationManager
        .authenticate(
            UsernamePasswordAuthenticationToken.unauthenticated(
                request.username(), request.password()))
        .flatMap(
            authentication ->
                securityContextRepository
                    .save(exchange, new SecurityContextImpl(authentication))
                    .thenReturn(me(authentication)))
        .map(WebappResult::ok)
        .onErrorResume(
            org.springframework.security.core.AuthenticationException.class,
            e -> Mono.just(WebappResult.fail(401, "用户名或密码错误")));
  }

  /** 登出：session 失效即全端下线（服务器会话无 token） */
  @PostMapping("/logout")
  public Mono<WebappResult<Void>> logout(final ServerWebExchange exchange) {
    return exchange
        .getSession()
        .doOnNext(session -> session.invalidate())
        .thenReturn(WebappResult.ok());
  }

  /** 当前用户：已登录回用户信息，未登录回 401 信封（SPA 轮询探活） */
  @GetMapping("/me")
  public Mono<WebappResult<MeResponse>> me() {
    return ReactiveSecurityContextHolder.getContext()
        .map(SecurityContext::getAuthentication)
        .filter(Authentication::isAuthenticated)
        .map(WebappAuthController::me)
        .map(WebappResult::ok)
        .defaultIfEmpty(WebappResult.fail(401, "未登录"));
  }

  private static MeResponse me(final Authentication authentication) {
    return new MeResponse(
        authentication.getName(),
        authentication.getAuthorities().stream().map(GrantedAuthority::getAuthority).toList());
  }
}
