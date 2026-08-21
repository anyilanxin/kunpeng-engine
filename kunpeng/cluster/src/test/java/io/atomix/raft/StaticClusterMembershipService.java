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
package io.atomix.raft;

import io.atomix.cluster.ClusterMembershipEventListener;
import io.atomix.cluster.ClusterMembershipService;
import io.atomix.cluster.Member;
import io.atomix.cluster.MemberId;
import io.atomix.utils.net.Address;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * 测试辅助：成员列表固定不变的 {@link ClusterMembershipService} 实现。
 *
 * <p>本节点与远端节点均使用本地回环地址构造，成员视图创建后不再变化，事件监听为空实现。
 */
final class StaticClusterMembershipService implements ClusterMembershipService {

  private final Member self;
  private final Map<MemberId, Member> clusterView;

  private StaticClusterMembershipService(final Member self, final Map<MemberId, Member> clusterView) {
    this.self = self;
    this.clusterView = clusterView;
  }

  /**
   * 以 localId 为本节点、remoteIds 为其余节点，构造一份静态成员视图。
   *
   * @param localId 本节点编号
   * @param remoteIds 其余节点编号
   * @return 固定成员视图的成员服务
   */
  static ClusterMembershipService of(final MemberId localId, final MemberId... remoteIds) {
    final var localMember = Member.member(localId, Address.local());
    final var others =
        Stream.of(remoteIds)
            .map(id -> Member.member(id, Address.local()))
            .collect(Collectors.toMap(Member::id, Function.identity()));
    others.put(localId, localMember);
    return new StaticClusterMembershipService(localMember, others);
  }

  @Override
  public Member getLocalMember() {
    return self;
  }

  @Override
  public Set<Member> getMembers() {
    return Set.copyOf(clusterView.values());
  }

  @Override
  public Member getMember(final MemberId memberId) {
    return clusterView.get(memberId);
  }

  /** 成员视图静态，无事件可发，监听注册为空操作。 */
  @Override
  public void addListener(final ClusterMembershipEventListener listener) {}

  @Override
  public void removeListener(final ClusterMembershipEventListener listener) {}
}
