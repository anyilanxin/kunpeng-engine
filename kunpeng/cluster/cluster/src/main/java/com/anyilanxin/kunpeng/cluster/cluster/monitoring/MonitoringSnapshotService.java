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
package com.anyilanxin.kunpeng.cluster.cluster.monitoring;

/**
 * 节点监控快照能力：broker 进程内读时采样并持有观察窗，经 {@link #SUBJECT} 集群消息应答远端拉取。
 *
 * <p>实现装配在 broker 进程（监控指标在 broker 侧 MeterRegistry）；Web 控制台所在网关进程为消费方——
 * 同进程（内嵌网关形态）直接调用，跨进程经集群消息单播。网关独立部署时网关进程不装配本接口。
 *
 * @author zxuanhong
 * @since 2026.9.0
 */
public interface MonitoringSnapshotService {

  /** 跨节点快照单播 subject（先例：gateway-load-query） */
  String SUBJECT = "webapp-monitoring-snapshot";

  /**
   * 取本节点完整快照（读时采样：距上次采样不足间隔时直接返回现有窗口）。
   *
   * @return nodeId 为本节点的完整观察窗
   */
  MonitoringSnapshotVo snapshot();
}
