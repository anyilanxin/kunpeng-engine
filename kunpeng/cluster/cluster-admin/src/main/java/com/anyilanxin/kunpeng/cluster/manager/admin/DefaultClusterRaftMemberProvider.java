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
package com.anyilanxin.kunpeng.cluster.manager.admin;

import com.anyilanxin.kunpeng.cluster.cluster.ClusterMembershipService;
import com.anyilanxin.kunpeng.cluster.cluster.MemberId;
import com.anyilanxin.kunpeng.configuration.ZoneType;
import com.anyilanxin.kunpeng.scheduler.ConcurrencyControl;
import com.anyilanxin.kunpeng.scheduler.future.ActorFuture;
import java.time.Duration;
import java.util.Set;

/**
 * ClusterRaftMemberProvider 接口的默认实现，基于成员发现服务获取 BROKER 成员，数量不足时每 10 秒重试直至满足期望数量。
 *
 * @author zxuanhong
 * @since
 */
public class DefaultClusterRaftMemberProvider implements ClusterRaftMemberProvider {
  private final ClusterMembershipService membershipService;
  private final ConcurrencyControl actorControl;

  public DefaultClusterRaftMemberProvider(
      final ClusterMembershipService membershipService, final ConcurrencyControl actorControl) {
    this.membershipService = membershipService;
    this.actorControl = actorControl;
  }

  @Override
  public ActorFuture<Set<MemberId>> getMemberIds(final int expectedValue) {
    final ActorFuture<Set<MemberId>> future = actorControl.createFuture();
    getMemberIds(expectedValue, future);
    return future;
  }

  private void getMemberIds(final int expectedValue, final ActorFuture<Set<MemberId>> future) {
    final Set<MemberId> memberIds = membershipService.getMemberIds(ZoneType.BROKER.getType());
    if (memberIds.size() >= expectedValue) {
      future.complete(memberIds);
    } else {
      actorControl.schedule(Duration.ofSeconds(10), () -> getMemberIds(expectedValue, future));
    }
  }
}
