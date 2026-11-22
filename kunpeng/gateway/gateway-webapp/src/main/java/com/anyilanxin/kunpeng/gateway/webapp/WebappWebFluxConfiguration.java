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
package com.anyilanxin.kunpeng.gateway.webapp;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;
import org.springframework.http.MediaType;
import org.springframework.web.method.HandlerTypePredicate;
import org.springframework.web.reactive.config.PathMatchConfigurer;
import org.springframework.web.reactive.config.ResourceHandlerRegistry;
import org.springframework.web.reactive.config.WebFluxConfigurer;
import org.springframework.web.reactive.function.server.RouterFunction;
import org.springframework.web.reactive.function.server.RouterFunctions;
import org.springframework.web.reactive.function.server.ServerResponse;

/**
 * Web 控制台 WebFlux 配置：webapp 包 controller 挂 {@code /api} 前缀（与 REST 面同款版本化策略，URL 形如 {@code
 * /api/v1/webapp/...}）；SPA 构建产物挂 {@code /webapp/**}（hash 路由，无服务端 fallback 需要）。
 *
 * <p>WebFlux 资源处理器不自动补目录首页（无 MVC welcome page 机制），{@code /webapp} 与 {@code /webapp/} 由路由函数转发到
 * {@code /webapp/index.html}。
 *
 * @author zxuanhong
 * @since 2026.9.0
 */
@Configuration(proxyBeanMethods = false)
@ConditionalOnProperty(
    name = "kunpeng.gateway-webapp.enabled",
    havingValue = "true",
    matchIfMissing = true)
public class WebappWebFluxConfiguration implements WebFluxConfigurer {

  @Override
  public void configurePathMatching(final PathMatchConfigurer configurer) {
    configurer.addPathPrefix(
        "/api", HandlerTypePredicate.forBasePackage("com.anyilanxin.kunpeng.gateway.webapp"));
  }

  @Override
  public void addResourceHandlers(final ResourceHandlerRegistry registry) {
    registry
        .addResourceHandler("/webapp/**")
        .addResourceLocations("classpath:/static/webapp/")
        .resourceChain(true);
  }

  /** 控制台首页转发：{@code /webapp}、{@code /webapp/} → 静态壳 index.html */
  @Bean
  RouterFunction<ServerResponse> webappWelcomePage() {
    final ClassPathResource index = new ClassPathResource("static/webapp/index.html");
    return RouterFunctions.route()
        .GET(
            "/webapp",
            request -> ServerResponse.ok().contentType(MediaType.TEXT_HTML).bodyValue(index))
        .GET(
            "/webapp/",
            request -> ServerResponse.ok().contentType(MediaType.TEXT_HTML).bodyValue(index))
        .build();
  }
}
