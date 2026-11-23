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

import java.util.List;
import java.util.Map;

/**
 * 节点监控快照（JSON 载荷契约，跨集群消息传输）：节点内读时采样的完整观察窗。
 *
 * <p>summary 为最新单值（stat 卡），series 为完整时间线窗口；实现方（broker 进程）与消费方（Web 控制台网关侧） 各自做 JSON 编解码，字段名即契约。
 *
 * @author zxuanhong
 * @since 2026.9.0
 */
public record MonitoringSnapshotVo(
    String nodeId, long sampledAt, int windowSize, List<Stat> summary, List<Series> series) {

  /** 单点：采样墙钟毫秒 + 值 */
  public record Point(long t, double v) {}

  /** stat 卡单值 */
  public record Stat(String key, double value, Map<String, String> tags) {}

  /** 一条时间线（tags 为该 meter 的全量标签） */
  public record Series(String key, Map<String, String> tags, List<Point> points) {}
}
