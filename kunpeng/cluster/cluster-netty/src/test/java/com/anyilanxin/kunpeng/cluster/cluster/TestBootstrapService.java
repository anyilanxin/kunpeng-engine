/*
 * Copyright © 2026 anyilanxin zxh (anyilanxin@aliyun.com)
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
package com.anyilanxin.kunpeng.cluster.cluster;

import com.anyilanxin.kunpeng.cluster.cluster.leaderfound.ClusterLeaderChangeListener;
import com.anyilanxin.kunpeng.cluster.cluster.leaderfound.ClusterLeaderFoundService;
import com.anyilanxin.kunpeng.cluster.cluster.messaging.MessagingService;
import com.anyilanxin.kunpeng.cluster.cluster.messaging.UnicastService;
import com.anyilanxin.kunpeng.cluster.utils.net.Address;
import java.util.concurrent.CompletableFuture;

/**
 * Test bootstrap service.
 *
 * <p>两参构造便于大多数只关心消息/单播的测试使用，leader-found 服务默认挂空实现； 需要自定义时使用全参构造。
 */
public record TestBootstrapService(
    MessagingService messagingService,
    UnicastService unicastService,
    ClusterLeaderFoundService leaderFoundService)
    implements BootstrapService {

  public TestBootstrapService(
      final MessagingService messagingService, final UnicastService unicastService) {
    this(messagingService, unicastService, new NoopLeaderFoundService());
  }

  @Override
  public MessagingService getMessagingService() {
    return messagingService;
  }

  @Override
  public UnicastService getUnicastService() {
    return unicastService;
  }

  @Override
  public ClusterLeaderFoundService getLeaderFoundService() {
    return leaderFoundService;
  }

  /** 空实现：忽略成员事件、无 leader 变更监听、无 leader 信息。 */
  static final class NoopLeaderFoundService implements ClusterLeaderFoundService {
    @Override
    public void addLeaderChangeListener(final ClusterLeaderChangeListener listener) {}

    @Override
    public void removeLeaderChangeListener(final ClusterLeaderChangeListener listener) {}

    @Override
    public CompletableFuture<MemberId> getAsyncLeader() {
      return CompletableFuture.completedFuture(null);
    }

    @Override
    public CompletableFuture<Address> getAsyncLeaderAddress() {
      return CompletableFuture.completedFuture(null);
    }

    @Override
    public MemberId getLeader() {
      return null;
    }

    @Override
    public Address getLeaderAddress() {
      return null;
    }

    @Override
    public void event(final ClusterMembershipEvent event) {}
  }
}
