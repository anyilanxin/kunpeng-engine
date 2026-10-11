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

import org.springframework.context.annotation.Configuration;
import org.springframework.http.server.PathContainer;
import org.springframework.http.server.RequestPath;
import org.springframework.web.method.HandlerTypePredicate;
import org.springframework.web.reactive.config.ApiVersionConfigurer;
import org.springframework.web.reactive.config.PathMatchConfigurer;
import org.springframework.web.reactive.config.WebFluxConfigurer;

/**
 * WebFlux 配置：REST 层路径收口与 API 版本化策略（Spring Framework 7 原生）。
 *
 * <p>REST controller 统一挂 {@code /api} 前缀，实际 URL 形如 {@code /api/v1/jobs}； 版本取 {@code /api} 后的首段（段下标
 * 1）：controller 以 {@code /{version}/xxx} URI 变量声明版本槽位， 以 {@code @RequestMapping(version = "v1")}
 * 声明版本（"v1" 与 "1" 等价，语义化比较）， 接口演进时在方法级声明更高版本即可与旧版本并存。
 *
 * <p>{@code /api} 前缀承担命名空间隔离：actuator、swagger（{@code /v3/api-docs} 的首段形如版本号）、 swagger-ui
 * 等无版本端点不在前缀内，不参与版本协商，也不受版本校验影响。
 *
 * <p>配置默认版本而非 {@code setVersionRequired(false)}：Spring Framework 7.0 中无版本请求在 无默认版本时产生空 Mono，会短路
 * {@code getHandler} 导致前缀外端点整体 404；默认版本仅作为 持有者占位，版本化端点的版本始终由路径段显式给出。
 *
 * @author zxuanhong
 * @since 2026.9.0
 */
@Configuration
public class WebFluxConfiguration implements WebFluxConfigurer {
  @Override
  public void configurePathMatching(final PathMatchConfigurer configurer) {
    configurer.addPathPrefix(
        "/api", HandlerTypePredicate.forBasePackage("com.anyilanxin.kunpeng.gateway.rest"));
  }

  @Override
  public void configureApiVersioning(final ApiVersionConfigurer configurer) {
    configurer
        .usePathSegment(1, WebFluxConfiguration::looksVersioned)
        .setDefaultVersion("v1")
        .detectSupportedVersions(true);
  }

  /** 版本路径判定：第 1 段（下标 0 为 /api 前缀）以 v 开头（v1/v2/...）才进入版本协商 */
  static boolean looksVersioned(final RequestPath path) {
    return path.elements().stream()
        .filter(PathContainer.PathSegment.class::isInstance)
        .map(PathContainer.Element::value)
        .skip(1)
        .findFirst()
        .map(second -> second.startsWith("v"))
        .orElse(false);
  }
}
