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
package com.anyilanxin.kunpeng.modules.broker;

import com.anyilanxin.kunpeng.broker.Broker;
import com.anyilanxin.kunpeng.broker.client.jobstream.JobStreamCoordinator.PushPoint;
import com.anyilanxin.kunpeng.cluster.cluster.AtomixCluster;
import com.anyilanxin.kunpeng.modules.common.endpoints.JobStreamEndpoint;
import com.anyilanxin.kunpeng.modules.common.endpoints.JobStreamEndpoint.GatewayView;
import com.anyilanxin.kunpeng.modules.common.endpoints.JobStreamEndpoint.SubscriptionSession;
import com.anyilanxin.kunpeng.modules.common.endpoints.JobStreamEndpoint.SubscriptionType;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.springframework.stereotype.Component;

/**
 * broker 形态的 job 流注册查询：gateway 取内嵌网关的连接视图（未启用内嵌网关时为 null，字段整体省略）， subscriptions 为合并派发索引镜像（按 jobType
 * 分组、会话标注来源网关），并经成员服务补出来源网关地址。
 *
 * @author zxuanhong
 * @since 2026.9.0
 */
@Component
final class BrokerJobStreamService implements JobStreamEndpoint.Service {
  private final Broker broker;
  private final AtomixCluster atomixCluster;

  BrokerJobStreamService(final Broker broker, final AtomixCluster atomixCluster) {
    this.broker = broker;
    this.atomixCluster = atomixCluster;
  }

  @Override
  public GatewayView gateway() {
    return broker
        .getEmbeddedGatewayJobHub()
        .map(hub -> JobStreamEndpoint.gatewayOf(atomixCluster, hub))
        .orElse(null);
  }

  @Override
  public List<SubscriptionType> subscriptions() {
    return broker
        .getJobStreamDispatcher()
        .map(dispatcher -> subscriptions(dispatcher.coordinator().mergedByType()))
        .orElseGet(List::of);
  }

  private List<SubscriptionType> subscriptions(final Map<String, List<PushPoint>> mergedByType) {
    final Map<String, String> addresses = gatewayAddresses();
    return mergedByType.entrySet().stream()
        .map(
            entry ->
                new SubscriptionType(
                    entry.getKey(),
                    entry.getValue().stream()
                        .map(
                            point ->
                                new SubscriptionSession(
                                    point.gatewayMemberId(),
                                    addresses.getOrDefault(point.gatewayMemberId(), ""),
                                    point.sessionId(),
                                    point.worker()))
                        .sorted(
                            Comparator.comparing(SubscriptionSession::gateway)
                                .thenComparingLong(SubscriptionSession::sessionId))
                        .toList()))
        .sorted(Comparator.comparing(SubscriptionType::jobType))
        .toList();
  }

  /** 成员服务中的网关地址表；网关刚离线、桶尚未摘除的窗口期查不到地址，置空串 */
  private Map<String, String> gatewayAddresses() {
    return atomixCluster.getMembershipService().getMembers().stream()
        .collect(
            Collectors.toMap(
                member -> member.id().id(), member -> member.address().toString(), (a, b) -> a));
  }
}
