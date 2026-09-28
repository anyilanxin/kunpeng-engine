/*
 * Copyright © 2026 anyilanxin zxh (anyilanxin@aliyun.com)
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the Free Software Foundation as either version 3
 * of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */
package com.anyilanxin.kunpeng.modules.common.endpoints;

import static com.anyilanxin.kunpeng.protocol.common.ClusterCommonConstant.INITIAL_NODE_SOURCE;
import static com.anyilanxin.kunpeng.protocol.common.ClusterCommonConstant.NODE_SOURCE_PROPERTY_KEY;

import com.anyilanxin.kunpeng.cluster.cluster.AtomixCluster;
import com.anyilanxin.kunpeng.cluster.cluster.Member;
import com.anyilanxin.kunpeng.gateway.job.GatewayJobHub;
import com.fasterxml.jackson.annotation.JsonInclude;
import java.util.ArrayList;
import java.util.List;
import java.util.TreeMap;
import org.springframework.boot.actuate.endpoint.annotation.Endpoint;
import org.springframework.boot.actuate.endpoint.annotation.ReadOperation;
import org.springframework.stereotype.Component;

/**
 * job 流注册观测 Actuator 端点（/actuator/jobstreams），输出两层订阅视图——
 *
 * <p>{@code connections} 为连接视图：本节点网关的连接明细，携带所属节点标识（memberId / address / nodeSourceId）与按 jobType
 * 分组的订阅流（含 wire 快照不携带的容量、租户、变量偏好）。
 *
 * <p>{@code subscriptions} 为全量视图：broker 合并派发索引的同构镜像，每个 jobType
 * 下列出其全部消费会话并标注来源网关成员与地址。与连接视图逐条对账可发现同步滞后（连接有、全量无）或陈旧残留（全量有、连接无）。
 *
 * <p>网关未启用时连接视图为空类型列表；两份数据均为弱一致快照。数据来源由 {@link Service} 的各部署形态实现提供。
 *
 * @author zxuanhong
 * @since 2026.9.0
 */
@Component
@Endpoint(id = "jobstreams")
public class JobStreamEndpoint {

  private final Service service;

  public JobStreamEndpoint(final Service service) {
    this.service = service;
  }

  /** 返回本节点 job 流两层视图：gateway（本网关连接视图）+ subscriptions（全量视图），均按 jobType 分组 */
  @ReadOperation
  public JobStreamsView jobStreams() {
    return new JobStreamsView(service.gateway(), service.subscriptions());
  }

  /** job 流注册查询服务：由各部署形态（broker / gateway）提供实现 */
  public interface Service {

    /** 本网关连接视图：网关标识 + 连接明细（按 jobType 分组）；本节点未运行网关时返回 null（字段整体省略） */
    GatewayView gateway();

    /** 全量视图：broker 合并派发索引镜像（按 jobType 分组，会话标注来源网关）；未启用 broker 时为空数组 */
    List<SubscriptionType> subscriptions();
  }

  /** 本网关连接视图：网关标识（memberId 与 subscriptions 会话的 gateway 字段可直接 join）+ 连接明细 */
  public static GatewayView gatewayOf(final AtomixCluster atomixCluster, final GatewayJobHub hub) {
    final Member member = atomixCluster.getMembershipService().getLocalMember();
    return new GatewayView(
        member.id().id(), member.address().toString(), nodeSourceId(member), connectionsOf(hub));
  }

  /** 节点 sourceId：读成员广播属性，未初始化或解析失败时返回 0 兜底 */
  private static int nodeSourceId(final Member member) {
    final String value = member.properties().getProperty(NODE_SOURCE_PROPERTY_KEY);
    if (value == null) {
      return INITIAL_NODE_SOURCE;
    }
    try {
      return Integer.parseInt(value);
    } catch (final NumberFormatException e) {
      return INITIAL_NODE_SOURCE;
    }
  }

  /** 由网关侧 hub 明细构建连接类型视图（按 jobType 分组，会话按 ID 升序） */
  public static List<ConnectionType> connectionsOf(final GatewayJobHub hub) {
    final var sessionsByType = new TreeMap<String, List<ConnectionSession>>();
    hub.describe()
        .forEach(
            info ->
                sessionsByType
                    .computeIfAbsent(info.jobType(), key -> new ArrayList<>())
                    .add(
                        new ConnectionSession(
                            info.streamId(),
                            info.worker(),
                            info.capacity(),
                            info.tenantIds(),
                            info.fetchVariables())));
    return sessionsByType.entrySet().stream()
        .map(entry -> new ConnectionType(entry.getKey(), entry.getValue()))
        .toList();
  }

  /** 端点响应：两层订阅视图；本节点未运行网关时 gateway 字段整体省略（纯 broker 节点只有 subscriptions） */
  @JsonInclude(JsonInclude.Include.NON_NULL)
  public record JobStreamsView(GatewayView gateway, List<SubscriptionType> subscriptions) {}

  /** 本网关连接视图：网关标识（成员 ID + 地址 + sourceId）+ 按 jobType 分组的连接列表 */
  public record GatewayView(
      String memberId, String address, int nodeSourceId, List<ConnectionType> connections) {}

  /** 连接视图：本网关该 jobType 下的连接会话列表 */
  public record ConnectionType(String jobType, List<ConnectionSession> sessions) {}

  /** 一条连接到本网关的订阅流：会话 ID + worker + 容量 + 租户/变量偏好 */
  public record ConnectionSession(
      long sessionId,
      String worker,
      int capacity,
      List<String> tenantIds,
      List<String> fetchVariables) {}

  /** 全量视图：该 jobType 的全部消费会话（跨网关，镜像派发索引） */
  public record SubscriptionType(String jobType, List<SubscriptionSession> sessions) {}

  /** 一条订阅会话及其来源网关：网关成员 + 地址 + 稳定会话 ID + worker */
  public record SubscriptionSession(
      String gateway, String address, long sessionId, String worker) {}
}
