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
package com.anyilanxin.kunpeng.gateway.rest.controller;

import com.anyilanxin.kunpeng.gateway.rest.core.Result;
import com.anyilanxin.kunpeng.gateway.rest.service.ClusterService;
import com.anyilanxin.kunpeng.gateway.rest.service.vo.ClusterTopologyVo;
import com.anyilanxin.kunpeng.gateway.rest.service.vo.GatewayDiscoveryVo;
import com.anyilanxin.kunpeng.gateway.rest.service.vo.GatewayLoadVo;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

/**
 * 集群管理查询接口。
 *
 * @author zxuanhong
 * @since 2026.9.0
 */
@RestController
@RequestMapping(
    value = "/{version}/cluster",
    version = "v1",
    produces = MediaType.APPLICATION_JSON_VALUE)
@Tag(name = "集群管理", description = "拓扑快照、网关发现与负载查询")
public class ClusterController {
  private final ClusterService clusterService;

  public ClusterController(final ClusterService clusterService) {
    this.clusterService = clusterService;
  }

  @GetMapping("/topology")
  @Operation(summary = "集群拓扑快照")
  public Mono<Result<ClusterTopologyVo>> topology() {
    return clusterService.topology().map(Result::ok);
  }

  @GetMapping("/gateways")
  @Operation(summary = "集群网关发现")
  public Mono<Result<GatewayDiscoveryVo>> gateways() {
    return clusterService.gateways().map(Result::ok);
  }

  @GetMapping("/gateway-load")
  @Operation(summary = "网关负载查询")
  public Mono<Result<GatewayLoadVo>> gatewayLoad(@RequestParam final String targetGrpcAddress) {
    return clusterService.gatewayLoad(targetGrpcAddress).map(Result::ok);
  }
}
