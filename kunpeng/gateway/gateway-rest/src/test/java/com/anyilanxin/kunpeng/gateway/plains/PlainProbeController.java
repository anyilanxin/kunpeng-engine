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
package com.anyilanxin.kunpeng.gateway.plains;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

/**
 * 路由探针：模拟与版本化 REST 命名空间无关的无版本端点（actuator、springdoc 等形态）。
 *
 * <p><b>必须位于 {@code gateway.rest} 基包之外</b>（本包 gateway.plains）：一旦挪入 rest 包会被
 * {@code addPathPrefix} 挂上 /api 前缀，"前缀外端点照常路由"的断言即失效。不挂 /api 前缀、不参与版本协商；
 * 其中 {@code /v3/api-docs} 复刻 springdoc 默认路径——首段形如版本号，用于回归验证隔离正确性。
 *
 * @author zxuanhong
 * @since 2026.9.0
 */
@RestController
public class PlainProbeController {
  @GetMapping("/plain-probe")
  public Mono<String> plain() {
    return Mono.just("plain");
  }

  @GetMapping("/v3/api-docs")
  public Mono<String> apiDocs() {
    return Mono.just("apiDocs");
  }
}
