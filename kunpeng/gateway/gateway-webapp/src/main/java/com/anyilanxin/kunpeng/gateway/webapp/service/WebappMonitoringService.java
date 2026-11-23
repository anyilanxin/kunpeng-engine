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
package com.anyilanxin.kunpeng.gateway.webapp.service;

import com.anyilanxin.kunpeng.cluster.cluster.AtomixCluster;
import com.anyilanxin.kunpeng.cluster.cluster.Member;
import com.anyilanxin.kunpeng.cluster.cluster.MemberId;
import com.anyilanxin.kunpeng.cluster.cluster.monitoring.MonitoringSnapshotService;
import com.anyilanxin.kunpeng.cluster.cluster.monitoring.MonitoringSnapshotVo;
import com.anyilanxin.kunpeng.configuration.ZoneType;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Duration;
import java.util.function.Function;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Service;

/**
 * 集群大屏指标转发：数据来自 broker 节点（指标真身在 broker 进程 MeterRegistry），本服务只按节点 id 取快照—— 同进程（内嵌网关形态，目标即本进程
 * broker）直读本地 {@link MonitoringSnapshotService}，其余经集群消息单播拉取 （网关独立部署时全部走单播）。没有"本节点"视角：网关进程自身不是监控数据源。
 *
 * <p>远端缓冲由观看者轮询驱动滚动（读时采样，无人观看零成本）；broker 成员不可达或未注册处理器时直接报错—— 事实呈现，不做兜底。
 *
 * @author zxuanhong
 * @since 2026.9.0
 */
@Service
public class WebappMonitoringService {

  /** 远端快照单播超时 */
  private static final Duration SNAPSHOT_TIMEOUT = Duration.ofSeconds(8);

  /** broker 成员 id 前缀（member id 形如 zone@nodeId） */
  private static final String BROKER_PREFIX = ZoneType.BROKER.getType() + "@";

  /** 消息载荷 JSON 解码：关未知字段失败以容忍新旧版本混布（同 SBE decode-unknown 策略） */
  private static final ObjectMapper JSON =
      new ObjectMapper().disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);

  private final AtomixCluster atomixCluster;
  private final ObjectProvider<MonitoringSnapshotService> localSnapshot;

  public WebappMonitoringService(
      final AtomixCluster atomixCluster,
      final ObjectProvider<MonitoringSnapshotService> localSnapshot) {
    this.atomixCluster = atomixCluster;
    this.localSnapshot = localSnapshot;
  }

  /**
   * 按节点 id 取监控快照（node 必填，必须是 broker 成员）。
   *
   * @param nodeId broker 成员 id（形如 broker@node01）
   * @return 该 broker 的完整观察窗
   */
  public MonitoringSnapshotVo sampleOf(final String nodeId) {
    if (nodeId == null || nodeId.isBlank()) {
      throw new IllegalArgumentException("node 必填：监控数据来自 broker 节点");
    }
    if (!nodeId.startsWith(BROKER_PREFIX)) {
      throw new IllegalStateException("监控数据来自 broker 节点: " + nodeId);
    }
    final Member member = atomixCluster.getMembershipService().getMember(MemberId.from(nodeId));
    if (member == null) {
      throw new IllegalStateException("节点不在成员清单: " + nodeId);
    }
    if (!member.isReachable()) {
      throw new IllegalStateException("节点不可达: " + nodeId);
    }
    if (nodeId.equals(localNodeId())) {
      // 同进程 broker（内嵌网关形态）：本进程必装配快照服务，缺失即装配异常直接抛
      return localSnapshot.getObject().snapshot();
    }
    return atomixCluster
        .getCommunicationService()
        .send(
            MonitoringSnapshotService.SUBJECT,
            new byte[0],
            Function.identity(),
            WebappMonitoringService::decode,
            member.id(),
            SNAPSHOT_TIMEOUT)
        .join();
  }

  private String localNodeId() {
    return atomixCluster.getMembershipService().getLocalMember().id().id();
  }

  private static MonitoringSnapshotVo decode(final byte[] payload) {
    try {
      return JSON.readValue(payload, MonitoringSnapshotVo.class);
    } catch (final java.io.IOException e) {
      throw new IllegalStateException("监控快照反序列化失败", e);
    }
  }
}
