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
package com.anyilanxin.kunpeng.modules.gateway;

import com.anyilanxin.kunpeng.cluster.cluster.AtomixCluster;
import com.anyilanxin.kunpeng.gateway.SpringGatewayBridge;
import com.anyilanxin.kunpeng.modules.common.endpoints.JobStreamEndpoint;
import com.anyilanxin.kunpeng.modules.common.endpoints.JobStreamEndpoint.GatewayView;
import com.anyilanxin.kunpeng.modules.common.endpoints.JobStreamEndpoint.SubscriptionType;
import java.util.List;
import org.springframework.stereotype.Component;

/**
 * 网关形态的 job 流注册查询：gateway 为本网关视图（网关标识 + 连接明细）， subscriptions 恒为空（全量视图在 broker 侧，注册数据经 SWIM 广播后于任意
 * broker 节点可查）。
 *
 * @author zxuanhong
 * @since 2026.9.0
 */
@Component
final class GatewayJobStreamService implements JobStreamEndpoint.Service {
  private final SpringGatewayBridge gatewayBridge;
  private final AtomixCluster atomixCluster;

  GatewayJobStreamService(
      final SpringGatewayBridge gatewayBridge, final AtomixCluster atomixCluster) {
    this.gatewayBridge = gatewayBridge;
    this.atomixCluster = atomixCluster;
  }

  @Override
  public GatewayView gateway() {
    return gatewayBridge
        .getJobHub()
        .map(hub -> JobStreamEndpoint.gatewayOf(atomixCluster, hub))
        .orElse(null);
  }

  @Override
  public List<SubscriptionType> subscriptions() {
    return List.of();
  }
}
