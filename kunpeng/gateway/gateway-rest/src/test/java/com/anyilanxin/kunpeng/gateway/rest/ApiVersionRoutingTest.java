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
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package com.anyilanxin.kunpeng.gateway.rest;

import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.net.URI;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.buffer.DefaultDataBufferFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseCookie;
import org.springframework.http.codec.ServerCodecConfigurer;
import org.springframework.http.server.RequestPath;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.accept.InvalidApiVersionException;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.reactive.config.EnableWebFlux;
import org.springframework.web.reactive.result.method.annotation.RequestMappingHandlerMapping;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.adapter.DefaultServerWebExchange;
import org.springframework.web.server.i18n.AcceptHeaderLocaleContextResolver;
import org.springframework.web.server.session.DefaultWebSessionManager;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * REST 版本化路由验证（Spring Framework 7 原生 API 版本化）。
 *
 * <p>验证正确用法的完整契约：controller 以 {@code /{version}/xxx} URI 变量声明版本槽位并挂
 * {@code /api} 前缀，版本取段下标 1；类级 {@code version = "v1"} 为默认，方法级更高版本可并存
 * （接口演进形态）；固定版本等值匹配，请求版本不等即 400；前缀外的无版本端点（actuator、
 * springdoc 的 {@code /v3/api-docs}）不受版本协商影响。
 *
 * @author zxuanhong
 * @since 2026.9.0
 */
class ApiVersionRoutingTest {
  private static AnnotationConfigApplicationContext context;
  private static RequestMappingHandlerMapping handlerMapping;

  @BeforeAll
  static void setUp() {
    context = new AnnotationConfigApplicationContext();
    context.register(
        EnableWebFluxConfig.class,
        WebFluxConfiguration.class,
        EvolutionProbes.EvolutionController.class,
        com.anyilanxin.kunpeng.gateway.plains.PlainProbeController.class);
    context.refresh();
    handlerMapping = context.getBean(RequestMappingHandlerMapping.class);
  }

  @AfterAll
  static void tearDown() {
    context.close();
  }

  @Test
  void exactDeclaredVersionRoutes() {
    assertHandler("GET", "/api/v1/probes/stable", "stable");
    assertHandler("GET", "/api/v1/probes/changed", "changedV1");
  }

  @Test
  void methodLevelHigherVersionCoexistsWithV1() {
    assertHandler("GET", "/api/v2/probes/changed", "changedV2");
    assertHandler("GET", "/api/v1/probes/changed", "changedV1");
  }

  @Test
  void unversionedEndpointsOutsideApiPrefixStillRoute() {
    // 前缀外的无版本端点必须照常命中（actuator 形态）
    assertHandler("GET", "/plain-probe", "plain");
    // springdoc 默认路径 /v3/api-docs：首段形如版本号，但不在 /api 前缀内，不得被版本协商误伤
    assertHandler("GET", "/v3/api-docs", "apiDocs");
  }

  @Test
  void requestOfUnsupportedVersionIsRejected() {
    // stable 仅声明固定 v1：v2 请求按等值匹配语义拒绝（NotAcceptableApiVersionException → 400）
    assertRejected("GET", "/api/v2/probes/stable");
    // v3 未在任何 mapping 声明：版本校验直接拒绝（InvalidApiVersionException → 400）
    assertRejected("GET", "/api/v3/probes/stable");
  }

  private void assertHandler(final String method, final String path, final String expectedMethod) {
    final HandlerMethod handler = handler(method, path);
    assertTrue(
        handler != null && expectedMethod.equals(handler.getMethod().getName()),
        path + " 必须命中 " + expectedMethod + "，实际: "
            + (handler == null ? "null" : handler.getMethod().getName()));
  }

  private void assertRejected(final String method, final String path) {
    try {
      assertNull(handler(method, path), path + " 未支持的版本不得命中任何 mapping");
    } catch (final InvalidApiVersionException e) {
      // NotAcceptableApiVersionException 为其子类：语义化拒绝，对应 400
    }
  }

  private HandlerMethod handler(final String method, final String path) {
    return (HandlerMethod) handlerMapping.getHandler(exchange(method, path)).block();
  }

  private ServerWebExchange exchange(final String method, final String path) {
    return new DefaultServerWebExchange(
        request(method, path),
        new MatchingOnlyResponse(),
        new DefaultWebSessionManager(),
        ServerCodecConfigurer.create(),
        new AcceptHeaderLocaleContextResolver(),
        context);
  }

  private static ServerHttpRequest request(final String method, final String path) {
    final URI uri = URI.create("http://localhost" + path);
    return new ServerHttpRequest() {
      @Override
      public String getId() {
        return "routing-test-request";
      }

      @Override
      public URI getURI() {
        return uri;
      }

      @Override
      public RequestPath getPath() {
        return RequestPath.parse(uri, "");
      }

      @Override
      public MultiValueMap<String, String> getQueryParams() {
        return new LinkedMultiValueMap<>();
      }

      @Override
      public HttpMethod getMethod() {
        return HttpMethod.valueOf(method);
      }

      @Override
      public java.util.Map<String, Object> getAttributes() {
        return java.util.Map.of();
      }

      @Override
      public Flux<org.springframework.core.io.buffer.DataBuffer> getBody() {
        return Flux.empty();
      }

      @Override
      public HttpHeaders getHeaders() {
        return new HttpHeaders();
      }

      @Override
      public MultiValueMap<String, org.springframework.http.HttpCookie> getCookies() {
        return new LinkedMultiValueMap<>();
      }
    };
  }

  /** 仅供路由匹配用的响应实现：匹配过程不写响应体。 */
  private static final class MatchingOnlyResponse implements ServerHttpResponse {
    @Override
    public boolean isCommitted() {
      return false;
    }

    @Override
    public void beforeCommit(final java.util.function.Supplier<? extends Mono<Void>> action) {
      // 路由匹配不提交响应
    }

    @Override
    public boolean setStatusCode(final org.springframework.http.HttpStatusCode status) {
      return true;
    }

    @Override
    public org.springframework.http.HttpStatusCode getStatusCode() {
      return null;
    }

    @Override
    public HttpHeaders getHeaders() {
      return new HttpHeaders();
    }

    @Override
    public MultiValueMap<String, ResponseCookie> getCookies() {
      return new LinkedMultiValueMap<>();
    }

    @Override
    public void addCookie(final ResponseCookie cookie) {
      // 路由匹配不写 cookie
    }

    @Override
    public Mono<Void> writeWith(
        final org.reactivestreams.Publisher<? extends org.springframework.core.io.buffer.DataBuffer>
            body) {
      return Mono.empty();
    }

    @Override
    public Mono<Void> writeAndFlushWith(
        final org.reactivestreams.Publisher<
                ? extends org.reactivestreams.Publisher<
                    ? extends org.springframework.core.io.buffer.DataBuffer>>
            bodyWithFlushHandlers) {
      return Mono.empty();
    }

    @Override
    public Mono<Void> setComplete() {
      return Mono.empty();
    }

    @Override
    public DefaultDataBufferFactory bufferFactory() {
      return new DefaultDataBufferFactory();
    }
  }

  /** 探针：类级默认 v1 + 同一路径方法级 v2 并存（接口演进的落地形态）。 */
  static final class EvolutionProbes {
    @RestController
    @RequestMapping(value = "/{version}/probes", version = "v1")
    static class EvolutionController {
      @GetMapping("/stable")
      Mono<String> stable() {
        return Mono.just("stable");
      }

      @GetMapping(path = "/changed", version = "v1")
      Mono<String> changedV1() {
        return Mono.just("changedV1");
      }

      @GetMapping(path = "/changed", version = "v2")
      Mono<String> changedV2() {
        return Mono.just("changedV2");
      }
    }
  }

  @Configuration
  @EnableWebFlux
  static class EnableWebFluxConfig {}
}
