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
import com.anyilanxin.kunpeng.gateway.webapp.service.WebappConsoleService;
import com.anyilanxin.kunpeng.gateway.webapp.service.WebappConsoleService.DispatchVo;
import com.anyilanxin.kunpeng.gateway.webapp.service.WebappConsoleService.GatewayLoadVo;
import com.anyilanxin.kunpeng.gateway.webapp.service.WebappConsoleService.GatewayVo;
import com.anyilanxin.kunpeng.gateway.webapp.service.WebappConsoleService.OverviewVo;
import com.anyilanxin.kunpeng.gateway.webapp.service.WebappConsoleService.PartitionVo;
import com.anyilanxin.kunpeng.gateway.webapp.service.WebappConsoleService.TopologyVo;
import java.util.List;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

/**
 * 控制台数据接口：总览/拓扑/分区详情/网关/网关负载/job 流/调度查询，全部只读。
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
public class WebappConsoleController {
  private final WebappConsoleService consoleService;

  public WebappConsoleController(final WebappConsoleService consoleService) {
    this.consoleService = consoleService;
  }

  /** 集群总览关键数字 */
  @GetMapping("/overview")
  public Mono<WebappResult<OverviewVo>> overview() {
    return Mono.fromSupplier(consoleService::overview).map(WebappResult::ok);
  }

  /** 拓扑矩阵：broker×分区 + 分区详情列表 */
  @GetMapping("/topology")
  public Mono<WebappResult<TopologyVo>> topology() {
    return Mono.fromSupplier(consoleService::topology).map(WebappResult::ok);
  }

  /** 分区详情：{group} 为 raft 组名（admin-partition / raft-partition） */
  @GetMapping("/partitions/{group}/{id}")
  public Mono<WebappResult<PartitionVo>> partition(
      @PathVariable final String group, @PathVariable final int id) {
    return Mono.fromSupplier(() -> consoleService.partition(group, id)).map(WebappResult::ok);
  }

  /** 网关发现列表 */
  @GetMapping("/gateways")
  public Mono<WebappResult<List<GatewayVo>>> gateways() {
    return Mono.fromSupplier(consoleService::gateways).map(WebappResult::ok);
  }

  /** 网关负载查询 */
  @GetMapping("/gateways/load")
  public Mono<WebappResult<GatewayLoadVo>> gatewayLoad(@RequestParam final String grpcAddress) {
    return Mono.fromFuture(consoleService.gatewayLoad(grpcAddress)).map(WebappResult::ok);
  }

  /** 本网关 job 流合并视图（actuator jobstreams 对等形状：gateway 连接 + 订阅关系） */
  @GetMapping("/jobs/streams")
  public Mono<WebappResult<WebappConsoleService.JobStreamsVo>> jobStreams() {
    return Mono.fromSupplier(consoleService::jobStreams).map(WebappResult::ok);
  }

  /** 参考控制台模型：broker 节点与分区副本快照 */
  @GetMapping("/cluster/brokers")
  public Mono<WebappResult<List<WebappConsoleService.BrokerSnapshotVo>>> brokerSnapshots() {
    return Mono.fromSupplier(consoleService::brokerSnapshots).map(WebappResult::ok);
  }

  /** 参考控制台模型：本网关 Worker 连接与订阅关系快照 */
  @GetMapping("/cluster/workers")
  public Mono<WebappResult<List<WebappConsoleService.JobWorkerSnapshotVo>>> workerSnapshots() {
    return Mono.fromSupplier(consoleService::workerSnapshots).map(WebappResult::ok);
  }

  /** 调度查询聚合：业务面元数据+最新计划（含执行明细）+ 管理面元数据+计划 */
  @GetMapping("/dispatch")
  public Mono<WebappResult<DispatchVo>> dispatch() {
    return Mono.fromSupplier(consoleService::dispatch).map(WebappResult::ok);
  }

  /** 业务集群重新负载：apply=false 仅生成调度计划预览 */
  @PostMapping("/dispatch/balance")
  public Mono<WebappResult<WebappConsoleService.DispatchOpVo>> dispatchBalance(
      @RequestBody final BalanceRequest request) {
    return Mono.fromFuture(() -> consoleService.dispatchBalance(request.apply()))
        .map(WebappResult::ok);
  }

  /** 修改业务集群副本因子：apply=false 仅生成调度计划预览 */
  @PostMapping("/dispatch/replication")
  public Mono<WebappResult<WebappConsoleService.DispatchOpVo>> dispatchChangeReplication(
      @RequestBody final ReplicationRequest request) {
    return Mono.fromFuture(
            () ->
                consoleService.dispatchChangeReplication(
                    request.expectReplicationFactor(), request.apply()))
        .map(WebappResult::ok);
  }

  /** 修改业务集群分区数量：apply=false 仅生成调度计划预览 */
  @PostMapping("/dispatch/partitions")
  public Mono<WebappResult<WebappConsoleService.DispatchOpVo>> dispatchChangePartitions(
      @RequestBody final PartitionsRequest request) {
    return Mono.fromFuture(
            () ->
                consoleService.dispatchChangePartitions(
                    request.expectPartitionsCount(), request.apply()))
        .map(WebappResult::ok);
  }

  /** 重新负载请求体 */
  public record BalanceRequest(boolean apply) {}

  /** 副本因子变更请求体 */
  public record ReplicationRequest(int expectReplicationFactor, boolean apply) {}

  /** 分区数量变更请求体 */
  public record PartitionsRequest(int expectPartitionsCount, boolean apply) {}
}
