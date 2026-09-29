/*
 * Copyright © 2026 anyilanxin zxh(anyilanxin.com)
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
package com.anyilanxin.kunpeng.gateway.topology;

import com.anyilanxin.kunpeng.cluster.cluster.ClusterMembershipEvent;
import com.anyilanxin.kunpeng.cluster.cluster.ClusterMembershipEventListener;
import com.anyilanxin.kunpeng.cluster.cluster.ClusterMembershipService;
import com.anyilanxin.kunpeng.cluster.cluster.Member;
import com.anyilanxin.kunpeng.cluster.cluster.MemberId;
import com.anyilanxin.kunpeng.protocol.common.encoding.GatewayInfo;
import com.anyilanxin.kunpeng.scheduler.Actor;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * gateway 地址发现收集服务（gateway 单边）：监听成员事件，解析各网关成员属性中的 {@link GatewayInfo} 实体汇总 client 接入地址视图，供 client
 * 集群网关发现使用。只做收集，不管理——本网关地址的写入由网关装配侧完成。
 *
 * @author zxuanhong
 * @since 2026.9.0
 */
public final class GatewayTopologyManager extends Actor implements ClusterMembershipEventListener {
  private final ClusterMembershipService membershipService;

  /** 网关接入地址视图：gateway 成员 -> (host, port)；host 为空回落成员主机 */
  private final Map<MemberId, String[]> gatewayAddresses = new ConcurrentHashMap<>();

  public GatewayTopologyManager(final ClusterMembershipService membershipService) {
    this.membershipService = membershipService;
  }

  @Override
  protected void onActorStarted() {
    membershipService.addListener(this);
    for (final Member member : membershipService.getMembers()) {
      refreshMember(member);
    }
  }

  @Override
  protected void onActorClosing() {
    membershipService.removeListener(this);
  }

  @Override
  public void event(final ClusterMembershipEvent event) {
    actor.submit(
        () -> {
          switch (event.type()) {
            case MEMBER_ADDED, METADATA_CHANGED -> refreshMember(event.subject());
            case MEMBER_REMOVED -> gatewayAddresses.remove(event.subject().id());
            default -> {}
          }
        });
  }

  /** 解析成员属性中的 gateway 发现实体并刷新视图；未发布的成员视为非网关（无实体属性）。 */
  private void refreshMember(final Member member) {
    final GatewayInfo info = GatewayInfo.fromProperties(member.properties());
    if (info == null) {
      gatewayAddresses.remove(member.id());
      return;
    }
    final String host = info.getClientHost().isEmpty() ? member.host() : info.getClientHost();
    gatewayAddresses.put(member.id(), new String[] {host, String.valueOf(info.getClientPort())});
  }

  /** 当前集群全部网关的 client 接入地址（host:port 列表）。 */
  public List<String> getGateways() {
    return gatewayAddresses.values().stream().map(pair -> pair[0] + ":" + pair[1]).toList();
  }
}
